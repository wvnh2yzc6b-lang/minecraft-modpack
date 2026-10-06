// Line-for-line port of com.warfront.client.WingAnimator, so previews move exactly like the game.
(function (g) {
  const TUCK_X = 0.77, TUCK_Y = 1.26, TUCK_Z = 0.7, TUCK_OUTER_Z = -2.95;
  const OPEN_X = 0.1, OPEN_Y = 0.2, OPEN_Z = 0.35;
  const clamp = (v, a, b) => Math.min(b, Math.max(a, v)), lerp = (t, a, b) => a + (b - a) * t;
  const approach = (c, t, rate, dt) => c + (t - c) * (1 - Math.pow(1 - rate, dt));
  function newState() { return { spread: 0, amp: 0, freq: 0, phase: 0, glide: 0, cloak: 0, lastAge: NaN }; }
  function update(s, inp) {
    const dt = isNaN(s.lastAge) ? 0 : clamp(inp.age - s.lastAge, 0, 4);
    s.lastAge = inp.age;
    let spreadT, ampT, freqT, glideT;
    if (inp.flying) {
      let effort = clamp(0.55 + inp.climb * 3 - Math.max(0, inp.speed - 1.2) * 0.6, 0.1, 1);
      effort = Math.max(effort, 1 - inp.flyTicks / 25);
      spreadT = 1; ampT = effort; freqT = 0.24 + 0.2 * effort; glideT = 1 - effort;
    } else if (inp.airborne) { spreadT = 0.45; ampT = 0.45; freqT = 0.62; glideT = 0; }
    else { spreadT = 0; ampT = 0; freqT = 0.3; glideT = 0; }
    if (dt === 0 && s.freq === 0) { s.spread = spreadT; s.amp = ampT; s.freq = freqT; s.glide = glideT; }
    s.spread = approach(s.spread, spreadT, spreadT > s.spread ? 0.22 : 0.14, dt);
    s.amp = approach(s.amp, ampT, 0.1, dt);
    s.freq = approach(s.freq, freqT, 0.08, dt);
    s.glide = approach(s.glide, glideT, 0.06, dt);
    s.phase = (s.phase + s.freq * dt) % (Math.PI * 2);
    const psi = s.phase + 0.4 * (1 - Math.cos(s.phase));
    const lift = Math.cos(psi), stroke = Math.sin(psi);
    const sp = s.spread, a = s.amp * sp;
    let elbow = sp * sp * (3 - 2 * sp); elbow = elbow * elbow;
    const o = {};
    o.x = lerp(sp, TUCK_X, OPEN_X) + a * 0.1 * stroke;
    o.y = lerp(sp, TUCK_Y, OPEN_Y) + a * 0.16 * stroke + s.glide * sp * 0.3;
    o.z = lerp(sp, TUCK_Z, OPEN_Z) + a * 0.42 * lift - s.glide * sp * 0.08;
    const tip = stroke > 0 ? 0.28 * stroke : 0.85 * stroke;
    o.outerZ = lerp(elbow, TUCK_OUTER_Z, 0) + a * tip;
    const rest = 1 - sp;
    o.z += rest * (Math.sin(inp.age * 0.08) * 0.03 + inp.limbSwingAmount * 0.06 * Math.sin(inp.age * 0.6));
    const cloakT = inp.flying
      ? 0.18 + a * 0.07 * Math.sin(psi - 1.1) + Math.min(1, inp.speed) * 0.03 * Math.sin(inp.age * 1.7)
      : 0.1 + inp.limbSwingAmount * 0.7 + (inp.sprinting ? 0.3 : 0) + Math.sin(inp.age * 0.05) * 0.03;
    s.cloak = dt === 0 && s.cloak === 0 ? cloakT : approach(s.cloak, cloakT, 0.35, dt);
    o.cloakX = s.cloak;
    return o;
  }
  g.WingAnimator = { newState, update };
})(window);
