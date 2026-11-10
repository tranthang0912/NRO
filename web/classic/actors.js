'use strict';

/* CharInfo from Mod_Local/GameAssembly.dll. Each part uses the original
   [spriteFrame, xOffset, heightAboveBaseline] and SQL part dx/dy. */
window.ClassicActors = (() => {
  let poses = [];
  const ready = fetch('/classic/assets/character-poses.json').then(response => {
    if (!response.ok) throw Error('Không tải được bảng tạo dáng nhân vật gốc.');
    return response.json();
  }).then(data => { poses = data; });

  function frame(actor, time) {
    if (typeof actor.id === 'string') return 0;
    if (actor.vertical < 0) return 7;
    if (actor.vertical > 0) return 12;
    const tick = Math.floor(time / 50);
    return actor.moving ? 2 + Math.floor(tick % 10 / 2) : tick % 15 < 5 ? 0 : 1;
  }

  function draw(ctx, image, parts, actor, time) {
    const pose = poses[frame(actor, time)];
    if (!pose) return -36;
    let top = -30;
    // paintCharBody draws the head, legs and torso in this order.
    [actor.head, actor.leg, actor.body].forEach((id, index) => {
      const [spriteFrame, offsetX, offsetY] = pose[index];
      const part = parts[id]?.[spriteFrame];
      if (!part || part[0] <= 0) return;
      const picture = image(`/assets/icon/x1/${part[0]}.png`);
      if (!picture.complete || !picture.naturalWidth) return;
      const x = offsetX + part[1], y = part[2] - offsetY;
      ctx.drawImage(picture, x, y);
      top = Math.min(top, y);
    });
    return top;
  }
  return { ready, frame, draw };
})();
