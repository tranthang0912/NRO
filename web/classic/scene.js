'use strict';

/* Original map cells, collision classifications and resource pixels. */
window.ClassicScene = (() => {
  const tileSize = 24;
  let tiles = {}, backgrounds = {};
  const manifest = (path, label) => fetch('/classic/assets/' + path + '/manifest.json').then(response => {
    if (!response.ok) throw Error('Không tải được ' + label + ' gốc.');
    return response.json();
  });
  const ready = Promise.all([manifest('tiles', 'địa hình'), manifest('backgrounds', 'cảnh nền')])
    .then(([terrain, scenes]) => { tiles = terrain; backgrounds = scenes; });

  const loaded = picture => picture.complete && picture.naturalWidth > 0;
  const rgb = color => '#' + (color & 0xffffff).toString(16).padStart(6, '0');
  const noColor = color => (color | 0) === -1;

  function layoutValue(value, width, height) {
    if (typeof value === 'number') return value;
    // Offsets measured in the Windows client's logical, unscaled viewport.
    const [base, w, halfW, thirdW, h, twoThirdsH] = value.layout;
    return base + w * width + halfW * Math.floor(width / 2) + thirdW * Math.floor(width / 3)
      + h * height + twoThirdsH * Math.floor(height * 2 / 3);
  }

  function background(ctx, image, map, width, height, cx, cy) {
    const key = map ? `${map.bgId}:${map.bgType}` : '0:0';
    const scene = backgrounds[key + ':' + map?.id] || backgrounds[key];
    ctx.fillStyle = scene ? rgb(scene[3]) : '#74c9f4';
    ctx.fillRect(0, 0, width, height);
    if (!scene) return;

    const [pictures, xShifts, yPositions, , paint] = scene;
    const x = Math.floor(cx), y = Math.min(Math.floor(cy), Math.floor(height));
    const position = value => layoutValue(value, Math.floor(width), Math.floor(height));
    const getImage = name => image('/classic/assets/backgrounds/' + name + '.png');
    for (const [kind, ...args] of paint) {
      if (kind === 'layer') {
        const [layer, yShift, top, bottom] = args;
        const name = pictures[layer];
        if (!name) continue;
        const picture = getImage(name);
        if (!loaded(picture)) continue;
        const w = picture.naturalWidth / 2, h = picture.naturalHeight / 2;
        const shift = xShifts[layer];
        const left = shift ? -((x >> shift) % w) : 0;
        const topY = position(yPositions[layer]) - (yShift > 0 ? y >> yShift : 0);
        if (!noColor(top) && topY > 0) {
          ctx.fillStyle = rgb(top);
          ctx.fillRect(0, 0, width, topY);
        }
        for (let dx = left; dx < width; dx += w) ctx.drawImage(picture, dx, topY, w, h);
        if (!noColor(bottom) && topY + h < height) {
          ctx.fillStyle = rgb(bottom);
          ctx.fillRect(0, topY + h, width, height - topY - h);
        }
      } else if (kind === 'image') {
        const [name, left, top, anchor] = args;
        const picture = getImage(name);
        if (!loaded(picture)) continue;
        const w = picture.naturalWidth / 2, h = picture.naturalHeight / 2;
        ctx.drawImage(picture, position(left) - ((anchor & 1) ? Math.floor(w / 2) : 0),
          position(top) - ((anchor & 32) ? h : 0), w, h);
      } else if (kind === 'fill') {
        const [color, left, top, w, h, shift] = args;
        ctx.fillStyle = rgb(color);
        ctx.fillRect(position(left), position(top) - (shift > 0 ? y >> shift : 0), position(w), position(h));
      }
    }
  }

  function terrain(ctx, image, map, width, height, cx, cy) {
    if (!map) return;
    const info = tiles[map.tileId];
    const atlas = image('/classic/assets/tiles/' + map.tileId + '.png');
    if (!info || !loaded(atlas)) return;
    const firstX = Math.max(0, Math.floor(cx / tileSize));
    const firstY = Math.max(0, Math.floor(cy / tileSize));
    const lastY = Math.min(map.tiles.length, Math.ceil((cy + height) / tileSize));
    for (let y = firstY; y < lastY; y++) {
      const row = map.tiles[y];
      const lastX = Math.min(row.length, Math.ceil((cx + width) / tileSize));
      for (let x = firstX; x < lastX; x++) {
        // Native paintTilemap repeats the inner column at each map edge.
        const id = row[x === 0 ? 1 : x === row.length - 1 ? x - 1 : x];
        const flags = info.flags[id] || 0;
        if (!id || id > info.count || flags & 256 || map.tileId === 13) continue;
        const index = id - 1;
        ctx.drawImage(atlas, index % 8 * 48, Math.floor(index / 8) * 48, 48, 48,
          Math.round(x * tileSize - cx), Math.round(y * tileSize - cy), tileSize, tileSize);
      }
    }
  }

  function decorations(ctx, image, map, width, height, cx, cy, foreground) {
    for (const item of map?.decorations || []) {
      if ((item.layer >= 4) !== foreground) continue;
      if (item.x - cx > width + 100 || item.x - cx < -300) continue;
      const picture = image('/assets/item_bg_temp/x1/' + item.image + '.png');
      if (!loaded(picture)) continue;
      const x = item.x - cx;
      const y = item.y - cy;
      if (y > height || y + picture.naturalHeight < 0) continue;
      ctx.drawImage(picture, Math.round(x), Math.round(y));
    }
  }

  return { ready, background, terrain, decorations };
})();
