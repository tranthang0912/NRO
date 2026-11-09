'use strict';

/* Render the original map grid with 24px resource strips. Early tilesets missing
   from the supplied client use terrain from the closest available original strip. */
window.ClassicScene = (() => {
  const tileSize = 24;
  const nativeSets = new Set([14, 16, 17, 18, 19, 20, 21, 24, 25, 26, 27, 28, 29, 30, 31, 32, 33]);
  const invisibleTiles = { 1: [39], 4: [19], 5: [38], 9: [26] };
  const scenes = {
    coast: { color: '#74c9f4', sky: 'b03-2', clouds: 'b112-1', water: true },
    forest: { color: '#9adbf5', sky: 'b03-2', clouds: 'b112-1', horizon: 'b80-2' },
    namek: { color: '#7ce3b7', sky: 'b03-4', clouds: 'b112-2', horizon: 'b02-4' },
    xayda: { color: '#edbf94', sky: 'b03-3', horizon: 'b01-2' },
    night: { color: '#23265d', sky: 'b203', horizon: 'b202' },
    pink: { color: '#ebbbcb', sky: 'b163', horizon: 'b162' },
    snow: { color: '#9ed3f1', sky: 'b03-2', clouds: 'b00-5', horizon: 'b82-4' },
    clouds: { color: '#8ec9f6', sky: 'b03-2', clouds: 'b00-5' }
  };

  function style(map) {
    if (!map) return scenes.coast;
    if ([45, 46, 48, 49, 50, 51].includes(map.id)) return scenes.clouds;
    if ([29, 33].includes(map.tileId)) return scenes.night;
    if (map.tileId === 31) return scenes.pink;
    if ([14, 16, 24, 25].includes(map.tileId)) return scenes.snow;
    if (map.planet === 1) return scenes.namek;
    if (map.planet === 2) return scenes.xayda;
    if ([4, 18, 20].includes(map.tileId)) return scenes.forest;
    return scenes.coast;
  }

  const resource = (image, name) => image('/assets/res/x1/' + name);
  const loaded = picture => picture?.complete && picture.naturalWidth > 0;

  function repeat(ctx, picture, width, y, offset, h = picture.naturalHeight) {
    if (!loaded(picture)) return;
    const w = picture.naturalWidth;
    for (let x = -((offset % w + w) % w); x < width; x += w) {
      ctx.drawImage(picture, Math.floor(x), Math.round(y), w, h);
    }
  }

  function background(ctx, image, map, width, height, cx, cy) {
    const scene = style(map);
    ctx.fillStyle = scene.color;
    ctx.fillRect(0, 0, width, height);
    const sky = resource(image, scene.sky);
    if (loaded(sky)) ctx.drawImage(sky, 0, 0, width, height);
    if (scene.clouds) repeat(ctx, resource(image, scene.clouds), width, height * .16 - cy * .08, cx * .17);
    const floor = (map?.floor ?? height * .74) - cy;
    if (scene.horizon) {
      const horizon = resource(image, scene.horizon);
      repeat(ctx, horizon, width, Math.max(height * .35, floor - (horizon.naturalHeight || 80) * .75), cx * .31);
    }
    if (scene.water) {
      const top = Math.max(height * .44, floor - 49);
      const sea = ctx.createLinearGradient(0, top, 0, top + 126);
      sea.addColorStop(0, '#46bff2');
      sea.addColorStop(.6, '#6bdaf2');
      sea.addColorStop(1, '#76ddf5');
      ctx.fillStyle = sea;
      ctx.fillRect(0, top, width, Math.max(0, height - top));
      const water = resource(image, 'wts');
      if (loaded(water)) {
        ctx.globalAlpha = .45;
        repeat(ctx, water, width, top, cx * .25, 24);
        ctx.globalAlpha = 1;
      }
    }
  }

  function prepare(map) {
    const invisible = new Set(invisibleTiles[map.tileId] || []);
    const solid = (x, y) => {
      const tile = map.tiles[y]?.[x];
      return tile > 0 && !invisible.has(tile);
    };
    const floors = [];
    const native = nativeSets.has(map.tileId);
    const set = native ? map.tileId : map.planet === 1 ? 20 : map.planet === 2 ? 19 : 30;
    const tileRows = map.tiles.map((row, y) => row.map((id, x) => {
      if (!solid(x, y)) return 0;
      if (!solid(x, y - 1) && y > 2) floors.push(y * tileSize);
      if (native) return id;
      const top = !solid(x, y - 1);
      const left = !solid(x - 1, y);
      const right = !solid(x + 1, y);
      if (set === 20) return top ? 1 + (id % 4) : 6 + (id % 6);
      if (set === 19) return top ? 1 + (id % 5) : 6 + (id % 5);
      if (top) return left ? 1 : right ? 8 : 2 + (id % 6);
      return left ? 9 : right ? 16 : [10, 13, 13, 14, 13, 11][id % 6];
    }));
    floors.sort((a, b) => a - b);
    // The early Earth maps place their grassy cap one row above the walk line.
    const groundOffset = map.tileId === 1 ? tileSize : 0;
    const floor = (floors[Math.floor(floors.length / 2)] || map.height * .72) + groundOffset;
    return { ...map, tileRows, tileSet: set, floor, groundOffset };
  }

  function terrain(ctx, image, map, width, height, cx, cy) {
    const strip = resource(image, map?.tileSet || 30);
    if (!loaded(strip)) return;
    if (!map) {
      const y = Math.floor(height * .74);
      for (let x = 0; x < width; x += tileSize) {
        ctx.drawImage(strip, 0, 24 * (2 + x / tileSize % 4), 24, 24, x, y, 24, 24);
        for (let yy = y + 24; yy < height; yy += 24) ctx.drawImage(strip, 0, 24 * 11, 24, 24, x, yy, 24, 24);
      }
      return;
    }
    const firstX = Math.max(0, Math.floor(cx / tileSize));
    const groundY = cy - map.groundOffset;
    const firstY = Math.max(0, Math.floor(groundY / tileSize));
    const lastY = Math.min(map.tileRows.length, Math.ceil((groundY + height) / tileSize));
    for (let y = firstY; y < lastY; y++) {
      const row = map.tileRows[y];
      const lastX = Math.min(row.length, Math.ceil((cx + width) / tileSize));
      for (let x = firstX; x < lastX; x++) {
        const id = row[x];
        if (!id || id * tileSize > strip.naturalHeight) continue;
        ctx.drawImage(strip, 0, (id - 1) * tileSize, tileSize, tileSize,
          Math.round(x * tileSize - cx), Math.round(y * tileSize - groundY), tileSize, tileSize);
      }
    }
  }

  function decorations(ctx, image, map, width, height, cx, cy, foreground) {
    for (const item of map?.decorations || []) {
      if ((item.layer >= 4) !== foreground) continue;
      if (item.x - cx > width + 100 || item.x - cx < -300) continue;
      const picture = image('/assets/item_bg_temp/x1/' + item.image + '.png');
      if (!loaded(picture)) continue;
      const x = item.x - cx - (foreground ? picture.naturalWidth / 2 : 0);
      const bottomAnchor = foreground || (item.layer === 3 && picture.naturalHeight > 64);
      const y = item.y - cy - (bottomAnchor ? picture.naturalHeight : 0);
      if (y > height || y + picture.naturalHeight < 0) continue;
      ctx.drawImage(picture, Math.round(x), Math.round(y));
    }
  }

  return { prepare, background, terrain, decorations };
})();
