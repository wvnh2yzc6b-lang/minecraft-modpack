// Codex model viewer. Geometry and textures come from tools/units.py output, built with the
// game's own cube UV and part-transform rules (mcmodel.js). Idle motion mirrors ImpModel; the
// demon player's wings run WingAnimator, the same code as DemonPlayerLayer.
(function () {
  const stage = document.getElementById('stage');
  const fallback = document.getElementById('fallback');
  const about = document.getElementById('about');
  const MODELS = [
    { id: 'imp_impaler', label: 'Imp Impaler', about: '<b>Imp Impaler</b> · spearman. Studded harness and bone spikes.' },
    { id: 'imp_firecaster', label: 'Imp Firecaster', about: '<b>Imp Firecaster</b> · archer. Rune mantle and a floating ember orb.' },
    { id: 'hive_lancer', label: 'Lancer-Drone', about: '<b>Lancer-Drone</b> · Hive spearman. Four arms, a double-bladed polearm and a throwing blade.', hive: true },
    { id: 'hive_beast', label: 'Deepmaw', about: '<b>Deepmaw</b> · Hive war beast. A towering burrower with a spiked frill and one huge hooked claw.', hive: true },
    { id: 'workers', label: 'Workers', about: '<b>Workers and guards</b> · posted roles, dressed by race. Plain work clothes in each race\'s colors.' },
    { id: 'demon_player', label: 'Demon player', about: '<b>Demon player</b> · what a player of the demon race looks like to everyone on the server.' },
  ];
  const PALETTES = [{ id: 'demon', label: 'Player demons' }, { id: 'burning_horde', label: 'Burning Horde' }];
  const HIVE_PALETTES = [{ id: 'hive', label: 'Player Hive' }, { id: 'the_swarm', label: 'The Swarm' }];
  const ROLES = [{ id: 'farmer', label: 'Farmer' }, { id: 'builder', label: 'Builder' }, { id: 'guard', label: 'Guard' }];
  const RACES = ['human', 'elf', 'dwarf', 'orc', 'angel', 'hive', 'demon'].map(id => ({ id, label: id[0].toUpperCase() + id.slice(1) }));
  const NAMES = CODEX.workerNames || {};
  const POSES = [{ id: 'ground', label: 'Folded' }, { id: 'jump', label: 'Jumping' }, { id: 'fly', label: 'Flying' }];
  const reduce = window.matchMedia && window.matchMedia('(prefers-reduced-motion: reduce)').matches;
  let pick = { model: 'imp_impaler', palette: 'demon', pose: 'fly', role: 'farmer', race: 'human' };
  try { const saved = JSON.parse(localStorage.getItem('codex-viewer') || 'null'); if (saved) pick = Object.assign(pick, saved); } catch (e) { }

  function seg(el, items, key, onPick) {
    el.innerHTML = '';
    for (const it of items) {
      const b = document.createElement('button');
      b.type = 'button'; b.textContent = it.label; b.id = 'pick-' + key + '-' + it.id;
      b.setAttribute('aria-pressed', pick[key] === it.id ? 'true' : 'false');
      b.addEventListener('click', () => {
        pick[key] = it.id;
        for (const c of el.children) c.setAttribute('aria-pressed', c === b ? 'true' : 'false');
        try { localStorage.setItem('codex-viewer', JSON.stringify(pick)); } catch (e) { }
        onPick();
      });
      el.appendChild(b);
    }
  }

  if (typeof THREE === 'undefined') { fallback.hidden = false; fallback.textContent = 'The 3D viewer could not load its graphics library. The renders below show the same models.'; return; }
  let renderer;
  try { renderer = new THREE.WebGLRenderer({ antialias: true, alpha: true }); } catch (e) { fallback.hidden = false; return; }
  renderer.setPixelRatio(Math.min(2, window.devicePixelRatio || 1));
  stage.insertBefore(renderer.domElement, stage.firstChild);
  const scene = new THREE.Scene();
  scene.add(new THREE.HemisphereLight(0xfff4ee, 0x3a2420, 1.05));
  const key = new THREE.DirectionalLight(0xfff0e6, 0.95); key.position.set(-30, 50, -40); scene.add(key);
  const rim = new THREE.DirectionalLight(0xff3a1a, 0.55); rim.position.set(40, 20, 50); scene.add(rim);
  const cam = new THREE.PerspectiveCamera(30, 4 / 3, 1, 2000);
  const turntable = new THREE.Group(); scene.add(turntable);
  const loader = new THREE.TextureLoader();
  const tex = id => CODEX.tex[id] ? loader.load(CODEX.tex[id]) : null;

  let current = null, wingState = null, yaw = 0.6, pitch = 0.12, dragging = false, lastX = 0, lastY = 0, idleSpin = !reduce;

  function build() {
    turntable.clear();
    const m = MODELS.find(x => x.id === pick.model) || MODELS[0];
    about.innerHTML = m.about;
    document.getElementById('palette-set').hidden = m.id === 'demon_player' || m.id === 'workers';
    const pals = m.hive ? HIVE_PALETTES : PALETTES;
    if (!pals.some(p => p.id === pick.palette)) pick.palette = pals[0].id;
    seg(document.getElementById('pick-palette'), pals, 'palette', build);
    document.getElementById('pose-set').hidden = m.id !== 'demon_player';
    document.getElementById('role-set').hidden = m.id !== 'workers';
    document.getElementById('race-set').hidden = m.id !== 'workers';
    const holder = new THREE.Group();
    if (m.id === 'workers') {
      const name = (NAMES[pick.race] || {})[pick.role];
      about.innerHTML = '<b>' + (name || 'Worker') + '</b> · ' + pick.race + ' ' + pick.role + '.';
      const id = 'gear_' + pick.role + '_' + pick.race;
      holder.add(MCModel.buildModel(CODEX.models.player_base, tex('skin/' + pick.race), null));
      const g = MCModel.buildModel(CODEX.models[id], tex(pick.race + '/' + id), tex(pick.race + '/' + id + '_glow'));
      holder.add(g);
      current = { kind: 'worker', P: g.userData.parts };
    } else if (m.id === 'demon_player') {
      const base = MCModel.buildModel(CODEX.models.player_base, tex('player/demon_skin'), tex('player/demon_skin_glow'));
      const extras = MCModel.buildModel(CODEX.models.demon_player, tex('player/demon_extras'), tex('player/demon_extras_glow'));
      const body = new THREE.Group(); base.position.y = extras.position.y = -12; body.position.y = 12;
      body.add(base); body.add(extras); holder.add(body);
      current = { kind: 'demon', body, P: extras.userData.parts, B: base.userData.parts };
      wingState = WingAnimator.newState();
    } else {
      const g = MCModel.buildModel(CODEX.models[m.id], tex(pick.palette + '/' + m.id), tex(pick.palette + '/' + m.id + '_glow'));
      g.scale.setScalar(m.id === 'hive_beast' ? 0.75 : m.hive ? 1.0 : 1.25);   // imps are 0.7x in game, the beast 1.75x
      holder.add(g);
      current = { kind: 'imp', P: g.userData.parts };
    }
    turntable.add(holder);
  }

  function animate(t) {
    if (!current) return;
    const P = current.P;
    if (current.kind === 'worker') return;
    if (current.kind === 'imp') {
      const base = n => P[n] && P[n].userData.baseRot;
      const flap = Math.sin(t * 0.18) * 0.08;
      if (P.wing_r) P.wing_r.rotation.z = base('wing_r')[2] + flap;
      if (P.wing_l) P.wing_l.rotation.z = base('wing_l')[2] - flap;
      for (let i = 1; i <= 6; i++) { const s = P['tail_' + i]; if (s) s.rotation.y = Math.sin(t * 0.07 - (i - 1) * 0.6) * 0.12; }
      if (P.ember_orb) { P.ember_orb.position.y = 3.5 + Math.sin(t * 0.15) * 0.6; P.ember_orb.rotation.y = t * 0.08; }
      return;
    }
    // Demon player: a looping scenario per pose, fed through the game's wing animator.
    const flying = pick.pose === 'fly', airborne = pick.pose === 'jump';
    const climb = flying ? 0.05 + Math.sin(t * 0.03) * 0.09 : 0;
    const o = WingAnimator.update(wingState, { flying, airborne, limbSwingAmount: 0, sprinting: false,
      climb, speed: flying ? 0.9 : 0, flyTicks: flying ? 200 : 0, age: t });
    P.wing_r.rotation.set(o.x, o.y, o.z, 'ZYX'); P.wing_l.rotation.set(o.x, -o.y, -o.z, 'ZYX');
    P.wing_r_outer.rotation.set(0, 0, o.outerZ, 'ZYX'); P.wing_l_outer.rotation.set(0, 0, -o.outerZ, 'ZYX');
    P.cloak.rotation.set(o.cloakX, 0, 0, 'ZYX');
    const lie = flying ? -(90 - 8) * Math.PI / 180 : 0;
    current.body.rotation.x += (lie - current.body.rotation.x) * 0.08;
    const headX = flying ? -Math.PI / 4 : 0;
    for (const M of [current.B, P]) { if (M.head) M.head.rotation.x = headX; if (M.hat) M.hat.rotation.x = headX; }
    current.body.position.y = 12 + (flying ? 14 + Math.sin(t * 0.1) * 1.2 : airborne ? 6 : 0);
  }

  function resize() {
    const w = stage.clientWidth, h = stage.clientHeight;
    renderer.setSize(w, h, false); cam.aspect = w / h; cam.updateProjectionMatrix();
  }
  window.addEventListener('resize', resize);
  stage.addEventListener('pointerdown', e => { dragging = true; idleSpin = false; lastX = e.clientX; lastY = e.clientY; stage.setPointerCapture(e.pointerId); });
  stage.addEventListener('pointermove', e => {
    if (!dragging) return;
    yaw += (e.clientX - lastX) * 0.01; pitch = Math.max(-0.4, Math.min(0.7, pitch + (e.clientY - lastY) * 0.006));
    lastX = e.clientX; lastY = e.clientY;
  });
  stage.addEventListener('pointerup', () => { dragging = false; });

  seg(document.getElementById('pick-model'), MODELS, 'model', build);
  seg(document.getElementById('pick-palette'), PALETTES, 'palette', build);
  seg(document.getElementById('pick-pose'), POSES, 'pose', () => { });
  seg(document.getElementById('pick-role'), ROLES, 'role', build);
  seg(document.getElementById('pick-race'), RACES, 'race', build);
  build(); resize();
  const t0 = performance.now();
  (function loop(now) {
    const t = (now - t0) / 50;   // game ticks
    if (idleSpin) yaw += 0.004;
    animate(reduce ? 0 : t);
    turntable.rotation.y = yaw;
    const r = current && current.kind === 'demon' && pick.pose === 'fly' ? 128 : 112;
    const focusY = current && current.kind === 'demon' ? (pick.pose === 'fly' ? 26 : 18) : 20;
    const lift = current && current.kind === 'demon' && pick.pose === 'fly' ? 0.45 : 0;
    cam.position.set(0, focusY + Math.sin(pitch + lift) * r, -Math.cos(pitch + lift) * r);
    cam.lookAt(0, focusY, 0);
    renderer.render(scene, cam);
    requestAnimationFrame(loop);
  })(t0);
})();
