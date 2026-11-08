'use strict';
(() => {
  const $ = id => document.getElementById(id);
  const canvas = $('world'), ctx = canvas.getContext('2d', { alpha: false });
  const images = new Map(), actors = new Map(), effects = [], bubbles = new Map();
  let socket, joined = false, register = false, catalog = { parts: {}, npcs: {}, items: {} };
  let map = null, state = null, stats = null, inventory = null, ownId = 0, selected = null, selectedSkill = 0;
  let currentPanel = '', bagTab = 'bag', lastState = 0, lastFrame = performance.now(), frameTime = 0;
  let width = 960, height = 540, cameraX = 0, cameraY = 0, scale = 2, heartbeat, inputTimer, loginTimer;
  let reconnectTimer, retryCount = 0, intentionalDisconnect = false;
  const input = { x: 0, y: 0, jump: false }, keys = new Set(), touchKeys = new Set();
  const number = n => new Intl.NumberFormat('vi-VN').format(n || 0);
  const distance = (a, b) => Math.hypot(a.x - b.x, a.y - b.y);
  const element = (tag, text, className) => { const e = document.createElement(tag); if (text !== undefined) e.textContent = text; if (className) e.className = className; return e; };
  const button = (text, fn, className) => { const e = element('button', text, className); e.type = 'button'; e.addEventListener('click', fn); return e; };
  const send = data => { if (socket?.readyState === WebSocket.OPEN) socket.send(JSON.stringify(data)); };
  const iconUrl = id => `/assets/icon/x1/${id}.png`;
  function img(url) {
    if (!images.has(url)) { const i = new Image(); i.src = url; i.addEventListener('error', () => { i.failed = true; }); images.set(url, i); }
    return images.get(url);
  }
  function toast(text) {
    const e = element('div', text, 'toast'); $('toasts').append(e); setTimeout(() => e.remove(), 4700);
    while ($('toasts').children.length > 4) $('toasts').firstChild.remove();
  }
  function authReady() { clearTimeout(loginTimer); $('authSubmit').disabled = false; $('authSubmit').textContent = register ? 'Tạo nhân vật và vào game →' : 'Vào thế giới →'; }
  function connect() {
    clearTimeout(reconnectTimer);intentionalDisconnect=false;
    clearInterval(heartbeat); clearInterval(inputTimer);
    $('connectionText').textContent = 'Đang kết nối'; $('connectionDot').classList.remove('on');
    socket = new WebSocket(`${location.protocol === 'https:' ? 'wss:' : 'ws:'}//${location.host}/ws`);
    socket.addEventListener('open', () => {
      $('connectionText').textContent = 'Máy chủ trực tuyến'; $('connectionDot').classList.add('on');
      $('reconnect').hidden = true; authReady();
      heartbeat = setInterval(() => send({ type: 'ping' }), 15000);
      inputTimer = setInterval(() => { if (joined) sendMovement(); }, 100);
    });
    socket.addEventListener('message', event => {
      try { receive(JSON.parse(event.data)); } catch (e) { console.error('Không đọc được dữ liệu game', e); }
    });
    socket.addEventListener('close', () => {
      clearInterval(heartbeat); clearInterval(inputTimer); stopMovement(); authReady();
      $('connectionText').textContent = 'Mất kết nối'; $('connectionDot').classList.remove('on');
      if (intentionalDisconnect) return;
      if (joined || sessionStorage.getItem('nro-resume')) {
        $('reconnect').hidden = false; joined = false;
        if(retryCount<6){
          const delay=Math.min(10000,1000*2**retryCount++);
          $('reconnectText').textContent='Đang tự kết nối lại. Nhân vật được giữ chỗ trong 45 giây.';
          reconnectTimer=setTimeout(connect,delay);
        }else $('reconnectText').textContent='Mạng chưa ổn định. Bạn có thể kết nối lại hoặc đăng nhập để tiếp tục.';
      }
      else $('authError').textContent = 'Máy chủ đã ngắt kết nối. Bấm Vào thế giới để kết nối lại.';
    });
    socket.addEventListener('error', () => { $('connectionText').textContent = 'Không kết nối được'; authReady(); });
  }
  function receive(data) {
    switch (data.type) {
      case 'hello':
        $('onlineCount').textContent = `${data.online ?? state?.online ?? 0}/${data.maxPlayers}`;
        if(sessionStorage.getItem('nro-resume')&&!joined)send({type:'resume',token:sessionStorage.getItem('nro-resume')});
        break;
      case 'joined':
        joined = true; ownId = data.id; $('playerName').textContent = data.name;
        retryCount=0;clearTimeout(reconnectTimer);if(data.token)sessionStorage.setItem('nro-resume',data.token);
        $('welcome').hidden = true; $('gameUI').hidden = false; document.body.classList.add('playing');
        $('reconnect').hidden = true; $('password').value = ''; $('authError').textContent = ''; authReady();
        localStorage.setItem('nro-username', $('username').value); resize(); break;
      case 'resumeFailed':
        sessionStorage.removeItem('nro-resume');joined=false;
        $('gameUI').hidden=true;$('welcome').hidden=false;$('reconnect').hidden=true;
        document.body.classList.remove('playing');state=map=null;actors.clear();
        $('authError').textContent='Phiên cũ đã kết thúc. Đăng nhập lại để tiếp tục nhân vật đã lưu.';
        break;
      case 'notice':
        if (!joined) { $('authError').textContent = clean(data.text); authReady(); } else toast(clean(data.text)); break;
      case 'map':
        map = data; selected = null; actors.clear();
        $('mapName').replaceChildren(document.createTextNode(data.name), element('small', 'Khu 0 · Thế giới chung'));
        $('npcDialog').close(); break;
      case 'state': {
        state = data; lastState = performance.now();
        $('onlineCount').textContent = `${data.online}/5`;
        const present = new Set();
        for (const a of data.players) { present.add(a.id); const old = actors.get(a.id); actors.set(a.id, { ...a, px: old?.px ?? a.x, py: old?.py ?? a.y, moving: old ? old.x !== a.x : false }); }
        for (const id of actors.keys()) if (!present.has(id)) actors.delete(id);
        if (selected?.kind === 'mob') { const m = state.mobs.find(m => m.id === selected.id); if (!m || m.dead) selected = null; }
        if (selected?.kind === 'player' && !actors.has(selected.id)) selected = null;
        $('revive').hidden = !actors.get(ownId)?.dead;
        updateContext(); break;
      }
      case 'stats':
        stats = data; updateHUD(); updateSkills();
        if (currentPanel && ['character', 'task', 'social'].includes(currentPanel)) renderPanel(); break;
      case 'inventory': inventory = data; if (currentPanel === 'inventory') renderPanel(); break;
      case 'chat': {
        const row = element('div'); row.append(element('b', `${data.name}: `), document.createTextNode(data.text)); $('chatLog').append(row);
        while ($('chatLog').children.length > 40) $('chatLog').firstChild.remove(); $('chatLog').scrollTop = $('chatLog').scrollHeight;
        bubbles.set(data.id, { text: data.text, until: performance.now() + 4500 }); break;
      }
      case 'menu': openMenu(data); break;
      case 'input': openInput(data); break;
      case 'confirmItem': openDialog(clean(data.text), [button('Đồng ý', () => { $('npcDialog').close(); send({ type: 'confirmItem' }); }), button('Hủy', () => $('npcDialog').close())]); break;
      case 'shop': renderShop(data); break;
      case 'combine': renderCombine(data);break;
      case 'clans': renderClans(data);break;
      case 'confirmSell': openDialog(clean(data.text),[button('Bán vật phẩm',()=>{$('npcDialog').close();send({type:'confirmSell'});}),button('Hủy',()=>$('npcDialog').close())]);break;
      case 'travel': openDialog('Chọn nơi muốn đến',data.maps.map(m=>button(`${m.name} · ${m.planet}`,()=>{$('npcDialog').close();send({type:'travel',index:m.index});})));break;
      case 'attack': effects.push({ ...data, until: performance.now() + 400 }); break;
      case 'tradeState': renderTrade(data); break;
      case 'tradeOpen': send({type:'tradeView'});break;
      case 'tradePeerLocked': send({type:'tradeView'});break;
      case 'tradeClosed':if(currentPanel==='trade')closePanel();toast('Giao dịch đã kết thúc.');send({type:'inventory'});break;
      case 'tradeInvite':openDialog(`${actors.get(data.peer)?.name||'Người chơi'} muốn giao dịch với bạn.`,[button('Đồng ý',()=>{$('npcDialog').close();send({type:'trade',action:1,peer:data.peer});}),button('Từ chối',()=>$('npcDialog').close())]);break;
    }
  }
  const clean = text => String(text ?? '').replace(/\|\d+\|/g, '').replace(/\b(?:https?:\/\/)?(?:nro1999\.online|nroblackgoku\.click)\S*/gi, '');
  function updateHUD() {
    if (!stats) return;
    $('hpBar').style.width = `${Math.max(0, Math.min(100, stats.hp / Math.max(1, stats.maxHp) * 100))}%`;
    $('mpBar').style.width = `${Math.max(0, Math.min(100, stats.mp / Math.max(1, stats.maxMp) * 100))}%`;
    $('hpText').textContent = `${number(stats.hp)} / ${number(stats.maxHp)}`; $('mpText').textContent = `${number(stats.mp)} / ${number(stats.maxMp)}`;
    $('powerText').textContent = `Sức mạnh ${number(stats.power)}`;
  }
  let skillSignature = '';
  function updateSkills() {
    const skills = stats?.skills.filter(s => s.level > 0) || [], signature = JSON.stringify(skills.map(s => [s.id, s.level]));
    if (signature !== skillSignature) {
      skillSignature = signature; $('skills').replaceChildren();
      if (!skills.some(s => s.id === selectedSkill)) selectedSkill = skills[0]?.id || 0;
      skills.slice(0, 9).forEach((s, index) => {
        const b = button('', () => { selectedSkill = s.id; updateSkills(); attack(); }, 'skill');
        b.dataset.skill = s.id; b.title = `${s.name} cấp ${s.level} · phím ${index + 1}`; b.setAttribute('aria-label', b.title);
        const i = element('img'); i.src = iconUrl(s.icon); i.alt = s.name; b.append(i, element('small', index + 1), element('span', '', 'cooldown')); $('skills').append(b);
      });
    }
    document.querySelectorAll('.skill').forEach(b => b.classList.toggle('active', +b.dataset.skill === selectedSkill));
  }
  function openDialog(text, choices) { stopMovement(); $('npcText').textContent = text; $('npcChoices').replaceChildren(...choices); if (!$('npcDialog').open) $('npcDialog').showModal(); }
  function openMenu(data) {
    openDialog(clean(data.text), data.choices.map((choice, index) => button(clean(choice), () => { $('npcDialog').close(); send({ type: 'menu', id: data.npc, choice: index }); })));
  }
  function openInput(data) {
    stopMovement(); $('npcText').textContent = clean(data.title); const form = element('form');
    for (const field of data.fields) { const label = element('label', field.label); const i = element('input'); i.required = true; i.maxLength = 160; i.type = field.kind === 2 ? 'password' : 'text'; label.append(i); form.append(label); }
    const submit = element('button', 'Xác nhận', 'primary'); submit.type = 'submit'; form.append(submit);
    form.addEventListener('submit', e => { e.preventDefault(); const values = [...form.querySelectorAll('input')].map(i => i.value); $('npcDialog').close(); send({ type: 'input', values }); });
    $('npcChoices').replaceChildren(form); if (!$('npcDialog').open) $('npcDialog').showModal();
  }
  function togglePanel(name) {
    if (currentPanel === name && !$('panel').hidden) { closePanel(); return; }
    currentPanel = name; $('panel').hidden = false; stopMovement();
    if (name === 'inventory') send({ type: 'inventory' }); renderPanel();
  }
  function closePanel() { currentPanel = ''; $('panel').hidden = true; }
  function statRow(label, value, action) { const e = element('div', '', 'stat-row'); e.append(element('span', label), element('b', value)); if (action) e.append(action); return e; }
  function renderPanel() {
    const root = $('panelContent'); root.replaceChildren();
    const titles = { inventory: 'Hành trang', character: 'Nhân vật', task: 'Nhiệm vụ', social: 'Đồng đội', help: 'Hướng dẫn' }; $('panelTitle').textContent = titles[currentPanel] || 'Game';
    if (currentPanel === 'inventory') {
      root.append(element('p', `Vàng ${number(stats?.gold)} · Ngọc ${number(stats?.gem)} · Hồng ngọc ${number(stats?.ruby)}`));
      const tabs = element('div', '', 'panel-tabs'); for (const [key, label] of [['bag', 'Túi đồ'], ['body', 'Trang bị'], ['box', 'Rương']]) tabs.append(button(label, () => { bagTab = key; renderPanel(); }, bagTab === key ? 'active' : '')); root.append(tabs);
      if (bagTab === 'box') root.append(element('p', 'Về nhà để cất và lấy vật phẩm.'));
      const list = inventory?.[bagTab] || [], nonempty = list.filter(i => i.id >= 0);
      if (!nonempty.length) root.append(element('p', 'Chưa có vật phẩm.'));
      for (const item of nonempty) {
        const row = element('div', '', 'item-row'), image = element('img'); image.src = iconUrl(item.icon); image.alt = '';
        const info = element('div', '', 'item-info'); info.append(element('b', `${item.name}${item.quantity > 1 ? ` ×${item.quantity}` : ''}`)); item.options.forEach(o => info.append(element('small', o)));
        const actions = element('div');
        if (bagTab === 'bag') {
          if ([0, 1, 2, 3, 4, 5, 11, 12, 32].includes(item.type)) actions.append(button('Mặc', () => send({ type: 'equip', index: item.index })));
          else actions.append(button('Dùng', () => send({ type: 'use', index: item.index })));
          actions.append(button('Cất', () => send({ type: 'store', index: item.index })),button('Bán',()=>send({type:'sell',index:item.index})));
        } else actions.append(button(bagTab === 'body' ? 'Tháo' : 'Lấy', () => send({ type: bagTab === 'body' ? 'unequip' : 'withdraw', index: item.index })));
        row.append(image, info, actions); root.append(row);
      }
    } else if (currentPanel === 'character') {
      root.append(statRow('Sức mạnh', number(stats?.power)), statRow('Tiềm năng', number(stats?.potential)));
      [['HP tối đa', stats?.maxHp, 0], ['KI tối đa', stats?.maxMp, 1], ['Sức đánh', stats?.damage, 2], ['Giáp', stats?.defense, 3], ['Chí mạng', stats?.crit || 0, 4]].forEach(([label, value, stat]) => root.append(statRow(label, number(value), button('+', () => send({ type: 'upgrade', stat, amount: 1 }), 'panel-action'))));
      root.append(element('h3', 'Kỹ năng'));
      stats?.skills.forEach(s => root.append(statRow(s.name, s.level > 0 ? `Cấp ${s.level}` : 'Chưa học')));
      root.append(element('p', 'Gặp sư phụ trên hành tinh của bạn để học và nâng cấp kỹ năng.'));
    } else if (currentPanel === 'task') {
      const t = stats?.task;
      if (!t) { root.append(element('p', 'Gặp NPC trong làng để nhận nhiệm vụ.')); return; }
      root.append(element('h3', t.name), element('p', clean(t.detail)));
      t.subtasks?.forEach((sub, index) => { const line = element('p', `${index < t.index ? '✓' : index === t.index ? '→' : '·'} ${sub.name}${sub.maxCount > 0 ? ` (${sub.count}/${sub.maxCount})` : ''}`); if (index === t.index) line.style.color = '#ffce85'; root.append(line); });
      root.append(button('Về nhà gặp ông', () => send({ type: 'home' }), 'panel-action'));
    } else if (currentPanel === 'social') {
      root.append(element('h3', `Cùng bản đồ · ${state?.players.filter(p => !p.boss).length || 0} người`));
      state?.players.filter(p => !p.boss).forEach(p => root.append(statRow(`${p.id === ownId ? '● ' : '○ '}${p.name}`, `HP ${number(p.hp)}`,p.id!==ownId?button('Giao dịch',()=>send({type:'trade',action:0,peer:p.id}),'panel-action'):null)));
      root.append(button('Tập hợp về Làng Aru', () => send({ type: 'rally' }), 'panel-action'));
      root.append(button('Bang hội',()=>send({type:'clans'}),'panel-action'));
      if (stats?.clan) root.append(element('h3', stats.clan.name)); else root.append(element('p', 'Gặp NPC quy lão để tạo hoặc tham gia bang hội.'));
      if (stats?.pet) {
        root.append(element('h3', `Đệ tử · ${stats.pet.name}`));
        ['Đi theo', 'Bảo vệ', 'Tấn công', 'Về nhà', 'Hợp thể'].forEach((text, status) => root.append(button(text, () => send({ type: 'petStatus', status }), 'panel-action')));
      }
    } else {
      const lines = [['Di chuyển', 'WASD hoặc phím mũi tên. Giữ W/↑ để bay và tiêu hao KI.'], ['Nhảy', 'Space hoặc nút ↥ trên điện thoại.'], ['Tấn công', 'Chọn quái rồi nhấn J hoặc ĐÁNH. Phím 1–9 chọn kỹ năng.'], ['Tương tác', 'Đến gần NPC hoặc lối đi rồi nhấn E.'], ['Nhặt vật phẩm', 'Đến gần vật phẩm rồi nhấn F.'], ['Hồi phục', 'Nhấn R hoặc ĐẬU để ăn đậu trong túi.'], ['Gặp bạn bè', 'Tất cả bấm Về làng, ở chung khu 0.'], ['Trò chuyện', 'Nhấn Enter, nhập tin nhắn và gửi.'], ['Lưu tiến trình', 'Tự lưu định kỳ và khi thoát. Dùng nút Thoát để lưu ngay.']];
      for (const [title, text] of lines) root.append(element('h3', title), element('p', text));
    }
  }
  function renderShop(data) {
    currentPanel = 'shop'; $('panel').hidden = false; $('panelTitle').textContent = 'Cửa hàng';
    const root = $('panelContent'); root.replaceChildren();
    root.append(element('p', `Vàng ${number(stats?.gold)} · Ngọc ${number(stats?.gem)}`));
    for (const tab of data.tabs) {
      root.append(element('h3', clean(tab.name)));
      for (const item of tab.items) {
        const info = catalog.items[item.id] || { name: `Vật phẩm ${item.id}`, icon: 0 };
        const row = element('div', '', 'item-row'), picture = element('img'); picture.src = iconUrl(info.icon); picture.alt = '';
        const text = element('div', '', 'item-info'); text.append(element('b', info.name), element('small', item.gold ? `${number(item.gold)} vàng` : `${number(item.gem)} ngọc`));
        row.append(picture, text, button('Mua', () => send({ type: 'buy', id: item.id }))); root.append(row);
      }
    }
  }
  function renderTrade(data) {
    currentPanel='trade';$('panel').hidden=false;$('panelTitle').textContent=`Giao dịch · ${actors.get(data.peer)?.name||'Đồng đội'}`;
    const root=$('panelContent');root.replaceChildren();
    root.append(element('h3',`Bạn đưa: ${number(data.gold)} vàng`));
    data.ownItems.forEach(i=>root.append(element('p',`${i.name} ×${i.quantity}`)));
    if(!data.locked){
      const goldForm=element('form'),gold=element('input');gold.type='number';gold.min=0;gold.max=stats?.gold||0;gold.value=data.gold;gold.placeholder='Số vàng muốn đưa';gold.setAttribute('aria-label','Số vàng giao dịch');
      const add=element('button','Đặt số vàng','panel-action');add.type='submit';goldForm.append(gold,add);goldForm.addEventListener('submit',e=>{e.preventDefault();send({type:'trade',action:2,index:-1,quantity:+gold.value});});root.append(goldForm);
      for(const item of data.bag){const row=element('div','', 'item-row'),info=element('span',`${item.name} ×${item.quantity}`);const qty=element('input');qty.type='number';qty.min=1;qty.max=Math.min(9999,item.quantity);qty.value=1;qty.style.width='65px';row.append(info,qty,button('Thêm',()=>send({type:'trade',action:2,index:item.index,quantity:+qty.value})));root.append(row);}
    }
    root.append(element('h3',data.peerLocked?`Đối phương đưa: ${number(data.peerGold)} vàng`:'Đợi đối phương khóa giao dịch'));
    data.peerItems.forEach(i=>root.append(element('p',`${i.name} ×${i.quantity}`)));
    const lock=button(data.locked?'Đã khóa':'Khóa giao dịch',()=>send({type:'trade',action:5}),'panel-action');lock.disabled=data.locked;
    const confirm=button('Xác nhận trao đổi',()=>{send({type:'trade',action:7});confirm.disabled=true;},'panel-action');confirm.disabled=!data.locked||!data.peerLocked;
    root.append(lock,confirm,button('Hủy giao dịch',()=>send({type:'trade',action:3}),'panel-action'));
  }
  function renderCombine(data){
    currentPanel='combine';$('panel').hidden=false;$('panelTitle').textContent=clean(data.title)||'Nâng cấp';
    const root=$('panelContent');root.replaceChildren(element('p',clean(data.text)));
    const form=element('form');for(const item of data.bag.filter(i=>i.id>=0)){
      const label=element('label',`${item.name} ×${item.quantity}`),check=element('input');check.type='checkbox';check.value=item.index;check.style.width='auto';check.style.minHeight='20px';label.prepend(check);form.append(label);
    }
    const submit=element('button','Xem công thức','primary');submit.type='submit';form.append(submit);
    form.addEventListener('submit',e=>{e.preventDefault();const indices=[...form.querySelectorAll('input:checked')].map(i=>+i.value);send({type:'combine',indices});});root.append(form);
  }
  function renderClans(data){
    currentPanel='clans';$('panel').hidden=false;$('panelTitle').textContent='Bang hội';const root=$('panelContent');root.replaceChildren();
    root.append(button('Làm mới',()=>send({type:'clans'}),'panel-action'));
    if(data.ownClan<0){
      const form=element('form'),input=element('input');input.placeholder='Tên bang mới';input.minLength=3;input.maxLength=25;input.required=true;
      const submit=element('button','Tạo bang (tính phí vàng/ngọc trong game)','panel-action');submit.type='submit';form.append(input,submit);form.addEventListener('submit',e=>{e.preventDefault();send({type:'clanCreate',name:input.value});});root.append(form);
      for(const clan of data.list){const row=element('div','', 'stat-row');row.append(element('span',`${clan.name} · ${clan.members}/${clan.max}`),button('Xin vào',()=>send({type:'clanJoinRequest',id:clan.id}),'panel-action'));root.append(row);}
    }else{
      for(const member of data.members)root.append(statRow(member.name,member.role===0?'Bang chủ':member.role===1?'Phó bang':number(member.power)));
      root.append(element('h3','Tin nhắn bang'));
      for(const m of data.messages){const row=element('p',`${m.name}: ${clean(m.text)}`);if(data.canApprove&&m.type===2)row.append(button('Duyệt',()=>send({type:'clanApprove',message:m.id}),'panel-action'));root.append(row);}
      const form=element('form'),input=element('input');input.placeholder='Trò chuyện trong bang';input.maxLength=120;const submit=element('button','Gửi','panel-action');submit.type='submit';form.append(input,submit);form.addEventListener('submit',e=>{e.preventDefault();send({type:'clanChat',text:input.value});input.value='';});root.append(form,button('Rời bang',()=>send({type:'clanLeave'}),'panel-action'));
    }
  }
  function own() { return actors.get(ownId); }
  function nearest(type) {
    const me = own(); if (!me) return null;
    const list = type === 'npc' ? map?.npcs : type === 'portal' ? map?.portals : type === 'drop' ? state?.drops : state?.mobs.filter(m => !m.dead);
    return list?.reduce((a, b) => !a || distance(me, b) < distance(me, a) ? b : a, null);
  }
  function updateContext() {
    const root = $('context'); root.replaceChildren(); const me = own(); if (!me || me.dead) { root.hidden = true; return; }
    const npc = nearest('npc'), portal = nearest('portal'), drop = nearest('drop');
    if (npc && distance(me, npc) <= 175) root.append(button(`${npc.name} · E`, () => send({ type: 'npc', id: npc.id })));
    if (portal && Math.abs(me.x - portal.x) <= 135) root.append(button(`${portal.name} →`, () => send({ type: 'portal', index: portal.index })));
    if (drop && distance(me, drop) < 90) root.append(button('Nhặt · F', () => send({ type: 'pickup', id: drop.id })));
    root.hidden = !root.children.length;
    const target = selected?.kind === 'mob' ? state.mobs.find(m => m.id === selected.id) : selected?.kind === 'player' ? actors.get(selected.id) : null;
    $('target').hidden = !target;
    if (target) $('target').replaceChildren(document.createTextNode(target.name), element('small', `${number(target.hp)} / ${number(target.maxHp)}`));
  }
  function interact() {
    const me = own(); if (!me) return;
    const npc = nearest('npc'), portal = nearest('portal');
    if (npc && distance(me, npc) < 175) send({ type: 'npc', id: npc.id });
    else if (portal && Math.abs(me.x - portal.x) < 135) send({ type: 'portal', index: portal.index });
  }
  function attack() {
    if (!joined || $('npcDialog').open || !stats) return;
    let target = selected;
    if (!target) { const mob = nearest('mob'); if (mob) { target = { kind: 'mob', id: mob.id }; selected = target; } }
    const request = { type: 'attack', skill: selectedSkill }; if (target) request[target.kind] = target.id; send(request);
  }
  function sendMovement() {
    if (!joined) return;
    const blocked = $('npcDialog').open || document.activeElement?.tagName === 'INPUT';
    const left = keys.has('a') || keys.has('arrowleft') || touchKeys.has('left'), right = keys.has('d') || keys.has('arrowright') || touchKeys.has('right');
    const up = keys.has('w') || keys.has('arrowup') || touchKeys.has('up'), down = keys.has('s') || keys.has('arrowdown') || touchKeys.has('down');
    input.x = blocked ? 0 : Number(right) - Number(left); input.y = blocked ? 0 : Number(down) - Number(up);
    send({ type: 'move', x: input.x, y: input.y, jump: blocked ? false : input.jump }); input.jump = false;
  }
  function stopMovement() { keys.clear(); touchKeys.clear(); input.x = input.y = 0; input.jump = false; if (joined) send({ type: 'move', x: 0, y: 0, jump: false }); }
  function resize() {
    const dpr = Math.min(devicePixelRatio || 1, 2); canvas.width = Math.round(innerWidth * dpr); canvas.height = Math.round(innerHeight * dpr);
    scale = innerWidth < 600 ? 1.5 : innerWidth < 1100 ? 1.8 : 2.2; width = innerWidth / scale; height = innerHeight / scale;
    ctx.setTransform(dpr * scale, 0, 0, dpr * scale, 0, 0); ctx.imageSmoothingEnabled = false;
  }
  function label(text, x, y, color = '#f0f6d5', size = 8) {
    ctx.font = `600 ${size}px Segoe UI,Arial`; ctx.textAlign = 'center'; ctx.lineWidth = 2.5; ctx.strokeStyle = '#1c382b'; ctx.strokeText(text, x, y); ctx.fillStyle = color; ctx.fillText(text, x, y);
  }
  function background(time) {
    const namek = map && [1, 2, 3].includes(map.tileId); const sky = ctx.createLinearGradient(0, 0, 0, height);
    sky.addColorStop(0, namek ? '#87bba1' : '#81bec2'); sky.addColorStop(0.7, namek ? '#bfdbac' : '#d3e2b5'); sky.addColorStop(1, '#9bbd86'); ctx.fillStyle = sky; ctx.fillRect(0, 0, width, height);
    ctx.fillStyle = '#f7e8b0'; ctx.beginPath(); ctx.arc(width * .68 - cameraX * .02, height * .21, 26, 0, Math.PI * 2); ctx.fill();
    for (let layer = 0; layer < 3; layer++) {
      const base = height * (.48 + layer * .12); ctx.fillStyle = ['#83b5a0', '#669c83', '#467b61'][layer]; ctx.beginPath(); ctx.moveTo(0, height);
      const offset = cameraX * (.08 + layer * .06);
      for (let x = -100; x < width + 150; x += 12) { const y = base - (Math.sin((x + offset) / (100 + 35 * layer)) * 27 + Math.sin((x + offset) / 47) * 13); ctx.lineTo(x, y); }
      ctx.lineTo(width, height); ctx.fill();
    }
    const cloud = img('/assets/res/x1/b00-1');
    if (cloud.complete && cloud.naturalWidth) { ctx.globalAlpha = .45; for (let x = -200; x < width + 200; x += 168) ctx.drawImage(cloud, x - (cameraX * .1 % 168), height * .48, 168, 80); ctx.globalAlpha = 1; }
    for (let i = 0; i < 7; i++) { const x = ((i * 173 + time * .006) % (width + 80)) - 40; const y = height * .2 + (i % 3) * 28; ctx.fillStyle = '#f8f5d940'; ctx.fillRect(x, y, 38, 4); ctx.fillRect(x + 9, y - 5, 20, 5); }
  }
  function ground() {
    if (!map?.tiles) {
      const gy = height * .78; ctx.fillStyle = '#536b39'; ctx.fillRect(0, gy, width, height); ctx.fillStyle = '#93b25b'; ctx.fillRect(0, gy, width, 6);
      for (let i = 0; i < width; i += 48) { ctx.fillStyle = i % 96 ? '#758d49' : '#859d51'; ctx.fillRect(i, gy + 2, 21, 2); }
      // A quiet training scene behind the welcome screen, using original artwork.
      drawActor({ x: width * .34, y: gy, head: 64, body: 1, leg: 2, facing: 1, name: '', id: -10 }, true);
      return;
    }
    const tops = new Set(map.top), startX = Math.max(0, Math.floor(cameraX / 24)), endX = Math.min(map.tiles[0].length, Math.ceil((cameraX + width) / 24));
    const startY = Math.max(0, Math.floor(cameraY / 24)), endY = Math.min(map.tiles.length, Math.ceil((cameraY + height) / 24));
    for (let y = startY; y < endY; y++) for (let x = startX; x < endX; x++) {
      const tile = map.tiles[y][x]; if (!tile) continue;
      const px = x * 24 - cameraX, py = y * 24 - cameraY, top = tops.has(tile);
      ctx.fillStyle = top ? '#6e8b43' : '#7d7551'; ctx.fillRect(px, py, 24, 24);
      if (top) { ctx.fillStyle = '#a0b858'; ctx.fillRect(px, py, 24, 4); ctx.fillStyle = '#c3cc75'; ctx.fillRect(px + ((x * 3) % 7), py, 5, 2); ctx.fillStyle = '#526537'; ctx.fillRect(px, py + 6, 24, 2); }
      else { ctx.fillStyle = '#918260'; ctx.fillRect(px + 3, py + 5, 5, 3); ctx.fillRect(px + 14, py + 17, 6, 2); ctx.fillStyle = '#6b6447'; ctx.fillRect(px + 17, py + 4, 3, 4); }
    }
    // Portals remain attached to the original map waypoints.
    for (const p of map.portals) {
      const x = p.x - cameraX, y = Math.min(p.y, map.height - 48) - cameraY;
      if (x < -100 || x > width + 100) continue;
      ctx.fillStyle = '#ffde8c'; ctx.globalAlpha = .8; ctx.beginPath(); ctx.moveTo(x - 7, y - 25); ctx.lineTo(x + 7, y - 25); ctx.lineTo(x, y - 17); ctx.fill(); ctx.globalAlpha = 1;
      label(p.name, x, y - 34, '#fff1b4', 7);
    }
  }
  function drawPart(id, frame, x, bottom, mirror) {
    const parts = catalog.parts[id]; if (!parts?.length) return false;
    const part = parts[Math.min(frame, parts.length - 1)] || parts[0], image = img(iconUrl(part[0]));
    if (!image.complete || !image.naturalWidth) return false;
    ctx.save(); ctx.translate(Math.round(x), Math.round(bottom)); if (mirror < 0) ctx.scale(-1, 1);
    ctx.drawImage(image, -Math.floor(image.naturalWidth / 2) + (part[1] || 0), -image.naturalHeight + (part[2] || 0)); ctx.restore(); return true;
  }
  function drawActor(a, demo = false) {
    const x = (demo ? a.x : a.px ?? a.x) - (demo ? 0 : cameraX), y = (demo ? a.y : a.py ?? a.y) - (demo ? 0 : cameraY);
    if (x < -80 || x > width + 80 || y < -80 || y > height + 80) return;
    const walking = a.moving && !a.dead, frame = walking ? 1 + Math.floor(frameTime / 100) % 5 : 0, bob = walking ? Math.floor(frameTime / 90) % 2 : 0;
    ctx.fillStyle = '#223b3b44'; ctx.beginPath(); ctx.ellipse(x, y, 12, 3, 0, 0, Math.PI * 2); ctx.fill();
    ctx.save(); if (a.dead) { ctx.globalAlpha = .5; ctx.translate(x, y); ctx.rotate(-Math.PI / 2); ctx.translate(-x, -y); }
    drawPart(a.leg, frame, x, y, a.facing); drawPart(a.body, frame, x, y - 11 - bob, a.facing); const head = drawPart(a.head, 0, x, y - 23 - bob, a.facing);
    if (!head) { ctx.fillStyle = a.gender === 1 ? '#81b66b' : '#edc28d'; ctx.fillRect(x - 6, y - 40, 12, 14); ctx.fillStyle = a.boss ? '#864660' : '#eab564'; ctx.fillRect(x - 7, y - 25, 14, 13); }
    ctx.restore();
    if (a.name) label(a.name, x, y - 54, a.id === ownId ? '#ffe09d' : a.boss ? '#ffb59d' : '#f0f6d5', 8);
    if (a.id === ownId) { ctx.fillStyle = '#ffce73'; ctx.fillRect(x - 2, y + 5, 4, 2); }
    const bubble = bubbles.get(a.id); if (bubble && bubble.until > frameTime) {
      const text = bubble.text.length > 35 ? bubble.text.slice(0, 35) + '…' : bubble.text;
      ctx.font = '8px Segoe UI'; const w = ctx.measureText(text).width + 12; ctx.fillStyle = '#f2efda'; ctx.fillRect(x - w / 2, y - 79, w, 15); ctx.fillStyle = '#203a29'; ctx.textAlign = 'center'; ctx.fillText(text, x, y - 68);
    }
  }
  function drawMob(m) {
    if (m.dead) return; const x = m.x - cameraX, y = m.y - cameraY; if (x < -50 || x > width + 50) return;
    const sheet = img(`/assets/web-mob/${m.template}.png`);
    if (sheet.complete && sheet.naturalWidth) {
      const template=catalog.mobFrames?.[m.template];
      if(template?.frames?.length){
        const frame=template.frames[Math.floor(frameTime/260)%Math.min(2,template.frames.length)];
        for(const p of frame){const piece=template.pieces.find(i=>i.id===p.piece);if(piece)ctx.drawImage(sheet,piece.x,piece.y,piece.w,piece.h,Math.round(x+p.x),Math.round(y+p.y),piece.w,piece.h);}
      }else {const sw = Math.min(36, sheet.naturalWidth), sh = Math.min(36, sheet.naturalHeight); ctx.drawImage(sheet, 0, 0, sw, sh, x - sw / 2, y - sh, sw, sh);}
    }
    else { ctx.fillStyle = '#c3af53'; ctx.beginPath(); ctx.ellipse(x, y - 10, 12, 9, 0, 0, Math.PI * 2); ctx.fill(); }
    label(m.name, x, y - 45, '#e7e7ad', 7);
    if (m.hp < m.maxHp || selected?.kind === 'mob' && selected.id === m.id) { ctx.fillStyle = '#2b3e2a'; ctx.fillRect(x - 16, y - 40, 32, 3); ctx.fillStyle = '#d18460'; ctx.fillRect(x - 16, y - 40, 32 * Math.max(0, m.hp / m.maxHp), 3); }
    if (selected?.kind === 'mob' && selected.id === m.id) { ctx.strokeStyle = '#ffe29d'; ctx.lineWidth = 1; ctx.strokeRect(x - 19, y - 35, 38, 36); }
  }
  function decorations(front){
    for(const d of map?.decorations||[]){
      if((d.layer>=4)!==front)continue;
      const image=img(`/assets/item_bg_temp/x1/${d.image}.png`);
      if(image.complete&&image.naturalWidth){const x=d.x-cameraX-image.naturalWidth/2,y=d.y-cameraY-image.naturalHeight;if(x+image.naturalWidth>-20&&x<width+20)ctx.drawImage(image,Math.round(x),Math.round(y));}
    }
  }
  function draw(time) {
    frameTime = time; const dt = Math.min(70, time - lastFrame); lastFrame = time;
    for (const a of actors.values()) { a.px += (a.x - a.px) * Math.min(1, dt / 75); a.py += (a.y - a.py) * Math.min(1, dt / 75); }
    const me = own();
    if (me && map) { cameraX += (Math.max(0, Math.min(map.width - width, me.px - width / 2)) - cameraX) * .14; cameraY += (Math.max(0, Math.min(map.height - height, me.py - height * .58)) - cameraY) * .14; }
    else { cameraX = cameraY = 0; }
    background(time); ground();decorations(false);decorations(true);
    if (joined && map && state) {
      for (const n of map.npcs) drawActor({ ...n, id: `npc${n.id}`, facing: 1 });
      for (const m of state.mobs) drawMob(m);
      for (const d of state.drops) { const image = img(iconUrl(d.icon)); if (image.complete && image.naturalWidth) ctx.drawImage(image, d.x - cameraX - image.naturalWidth / 2, d.y - cameraY - image.naturalHeight - 3 + Math.sin(time / 220) * 2); }
      [...actors.values()].sort((a, b) => a.y - b.y).forEach(a => drawActor(a));
      for (let i = effects.length - 1; i >= 0; i--) {
        const e = effects[i]; if (time > e.until) { effects.splice(i, 1); continue; }
        const progress = 1 - (e.until - time) / 400, x = e.x + (e.targetX - e.x) * progress - cameraX, y = e.y + (e.targetY - e.y) * progress - cameraY - 18;
        ctx.fillStyle = '#fff8c9'; ctx.shadowColor = '#ffc95f'; ctx.shadowBlur = 9; ctx.beginPath(); ctx.arc(x, y, 3 + 3 * Math.sin(progress * Math.PI), 0, Math.PI * 2); ctx.fill(); ctx.shadowBlur = 0;
      }
      const ac = $('avatar').getContext('2d'); ac.clearRect(0, 0, 58, 58);
      const head = catalog.parts[me?.head]?.[0]; if (head) { const image = img(iconUrl(head[0])); if (image.complete && image.naturalWidth) { ac.imageSmoothingEnabled = false; ac.drawImage(image, 29 - image.naturalWidth, 28 - image.naturalHeight, image.naturalWidth * 2, image.naturalHeight * 2); } }
      document.querySelectorAll('.skill').forEach(b => { const skill = stats?.skills.find(s => s.id === +b.dataset.skill); if (skill) b.querySelector('.cooldown').style.height = `${Math.max(0, Math.min(100, (skill.cooldown - (Date.now() - skill.lastUse)) / Math.max(1, skill.cooldown) * 100))}%`; });
    }
    requestAnimationFrame(draw);
  }
  canvas.addEventListener('pointerdown', e => {
    if (!joined || !state || !map) return;
    const rect = canvas.getBoundingClientRect(), x = (e.clientX - rect.left) / scale + cameraX, y = (e.clientY - rect.top) / scale + cameraY;
    const hitMob = state.mobs.filter(m => !m.dead).find(m => Math.abs(m.x - x) < 30 && y > m.y - 55 && y < m.y + 18);
    if (hitMob) { selected = { kind: 'mob', id: hitMob.id }; updateContext(); return; }
    const hitNpc = map.npcs.find(n => Math.abs(n.x - x) < 30 && y > n.y - 70 && y < n.y + 10);
    if (hitNpc) { send({ type: 'npc', id: hitNpc.id }); return; }
    const hitDrop = state.drops.find(d => Math.abs(d.x - x) < 20 && Math.abs(d.y - y) < 25);
    if (hitDrop) { send({ type: 'pickup', id: hitDrop.id }); return; }
    const hitPlayer = [...actors.values()].find(p => p.id !== ownId && Math.abs(p.x - x) < 25 && y > p.y - 60 && y < p.y + 10);
    selected = hitPlayer ? { kind: 'player', id: hitPlayer.id } : null; updateContext();
  });
  document.addEventListener('keydown', e => {
    if (!joined || e.target.matches('input,select,textarea') || $('npcDialog').open) return;
    const key = e.key.toLowerCase();
    if (['arrowleft', 'arrowright', 'arrowup', 'arrowdown', ' ', 'enter'].includes(key)) e.preventDefault();
    keys.add(key);
    if (e.repeat) return;
    if (key === ' ') input.jump = true;
    else if (key === 'j') attack();
    else if (key === 'e') interact();
    else if (key === 'f') { const drop = nearest('drop'); if (drop) send({ type: 'pickup', id: drop.id }); }
    else if (key === 'r') send({ type: 'pea' });
    else if (key === 'i') togglePanel('inventory');
    else if (key === 'escape') closePanel();
    else if (key === 'enter') { stopMovement(); $('chatInput').focus(); }
    else if (/^[1-9]$/.test(key)) { const skill = stats?.skills.filter(s => s.level > 0)[+key - 1]; if (skill) { selectedSkill = skill.id; updateSkills(); } }
    sendMovement();
  });
  document.addEventListener('keyup', e => { keys.delete(e.key.toLowerCase()); if (joined) sendMovement(); });
  window.addEventListener('blur', stopMovement); document.addEventListener('visibilitychange', () => { if (document.hidden) stopMovement(); });
  document.querySelectorAll('[data-dir]').forEach(b => {
    b.addEventListener('pointerdown', e => { e.preventDefault(); b.setPointerCapture(e.pointerId); touchKeys.add(b.dataset.dir); sendMovement(); });
    for (const event of ['pointerup', 'pointercancel', 'lostpointercapture']) b.addEventListener(event, () => { touchKeys.delete(b.dataset.dir); sendMovement(); });
  });
  $('touchJump').addEventListener('pointerdown', e => { e.preventDefault(); input.jump = true; sendMovement(); });
  $('touchAttack').addEventListener('click', attack); $('touchInteract').addEventListener('click', interact); $('touchPea').addEventListener('click', () => send({ type: 'pea' }));
  document.querySelectorAll('[data-panel]').forEach(b => b.addEventListener('click', () => togglePanel(b.dataset.panel)));
  $('closePanel').addEventListener('click', closePanel);
  $('rally').addEventListener('click', () => send({ type: 'rally' })); $('home').addEventListener('click', () => send({ type: 'home' }));
  $('revive').addEventListener('click', () => send({ type: 'revive' }));
  $('logout').addEventListener('click', () => {intentionalDisconnect=true;sessionStorage.removeItem('nro-resume');clearTimeout(reconnectTimer);send({ type: 'logout' }); joined = false; $('gameUI').hidden = true; $('welcome').hidden = false; document.body.classList.remove('playing'); state = map = stats = null; actors.clear(); closePanel(); setTimeout(connect, 300); });
  $('reconnectButton').addEventListener('click', () => { $('reconnect').hidden = true; $('gameUI').hidden = true; $('welcome').hidden = false; document.body.classList.remove('playing'); state = map = null; actors.clear(); connect(); });
  $('fullscreen').addEventListener('click', () => { const operation = document.fullscreenElement ? document.exitFullscreen() : document.documentElement.requestFullscreen(); operation?.catch(() => toast('Trình duyệt chưa hỗ trợ toàn màn hình.')); });
  function authTab(value) { register = value; $('registerFields').hidden = !value; $('characterName').required = value; $('password').autocomplete = value ? 'new-password' : 'current-password'; $('loginTab').classList.toggle('active', !value); $('registerTab').classList.toggle('active', value); $('loginTab').setAttribute('aria-selected', !value); $('registerTab').setAttribute('aria-selected', value); $('authError').textContent = ''; authReady(); }
  $('loginTab').addEventListener('click', () => authTab(false)); $('registerTab').addEventListener('click', () => authTab(true));
  $('authForm').addEventListener('submit', e => {
    e.preventDefault(); $('authError').textContent = '';
    if (socket?.readyState !== WebSocket.OPEN) { connect(); $('authError').textContent = 'Đang kết nối lại, hãy bấm vào game khi máy chủ trực tuyến.'; return; }
    $('authSubmit').disabled = true; $('authSubmit').textContent = 'Đang vào thế giới…';
    send({ type: register ? 'register' : 'login', username: $('username').value.trim(), password: $('password').value, name: $('characterName').value, gender: +$('gender').value, invite: $('invite').value });
    loginTimer = setTimeout(() => { authReady(); $('authError').textContent = 'Máy chủ đang xử lý. Nếu chưa vào được, hãy thử lại.'; }, 20000);
  });
  $('chatForm').addEventListener('submit', e => { e.preventDefault(); const text = $('chatInput').value.trim(); if (text) send({ type: 'chat', text }); $('chatInput').value = ''; $('chatInput').blur(); });
  $('username').value = localStorage.getItem('nro-username') || '';
  fetch('/api/status').then(r => { if (!r.ok) throw Error('status'); return r.json(); }).then(s => { $('onlineCount').textContent = `${s.online}/${s.maxPlayers}`; $('inviteField').hidden = !s.inviteRequired; }).catch(() => { $('connectionText').textContent = 'Đang khởi động'; });
  fetch('/api/catalog').then(r => { if (!r.ok) throw Error('catalog'); return r.json(); }).then(c => { catalog = c; }).catch(() => toast('Chưa tải được dữ liệu hình ảnh. Hãy tải lại trang.'));
  window.addEventListener('resize', resize); resize(); connect(); requestAnimationFrame(draw);
})();
