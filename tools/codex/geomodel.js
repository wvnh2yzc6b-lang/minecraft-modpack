// Builds three.js objects from GeckoLib (Bedrock) .geo.json files and plays their .animation.json clips, using the
// same rules GeckoLib applies when it bakes and renders a model: x mirrored, rotations about x and y negated,
// bones rotated about their pivot in Z, Y, X order, per-face UV. So a preview here matches the game.
(function (global) {
  const D2R = Math.PI / 180;
  function prep(t) { t.magFilter = THREE.NearestFilter; t.minFilter = THREE.NearestFilter; t.flipY = false; return t; }

  // Corners of each face as seen from outside: top-left, top-right, bottom-right, bottom-left (model space).
  function corners(side, x0, y0, z0, x1, y1, z1) {
    switch (side) {
      case 'north': return [[x1, y1, z0], [x0, y1, z0], [x0, y0, z0], [x1, y0, z0]];
      case 'south': return [[x0, y1, z1], [x1, y1, z1], [x1, y0, z1], [x0, y0, z1]];
      case 'east':  return [[x0, y1, z0], [x0, y1, z1], [x0, y0, z1], [x0, y0, z0]];   // file east is model -x
      case 'west':  return [[x1, y1, z1], [x1, y1, z0], [x1, y0, z0], [x1, y0, z1]];
      case 'up':    return [[x1, y1, z0], [x0, y1, z0], [x0, y1, z1], [x1, y1, z1]];
      default:      return [[x1, y0, z1], [x0, y0, z1], [x0, y0, z0], [x1, y0, z0]];
    }
  }

  function cubeGeometry(c, tw, th) {
    const inf = c.inflate || 0;
    const x0 = -(c.origin[0] + c.size[0]) - inf, y0 = c.origin[1] - inf, z0 = c.origin[2] - inf;
    const x1 = x0 + c.size[0] + 2 * inf, y1 = y0 + c.size[1] + 2 * inf, z1 = z0 + c.size[2] + 2 * inf;
    const pos = [], uvs = [], idx = [];
    for (const side in c.uv) {
      const f = c.uv[side], [u, v] = f.uv, [w, h] = f.uv_size;
      const q = corners(side, x0, y0, z0, x1, y1, z1);
      const uv = [[u, v], [u + w, v], [u + w, v + h], [u, v + h]];
      const base = pos.length / 3;
      for (let i = 0; i < 4; i++) { pos.push(...q[i]); uvs.push(uv[i][0] / tw, uv[i][1] / th); }
      idx.push(base, base + 2, base + 1, base, base + 3, base + 2);
    }
    const g = new THREE.BufferGeometry();
    g.setAttribute('position', new THREE.Float32BufferAttribute(pos, 3));
    g.setAttribute('uv', new THREE.Float32BufferAttribute(uvs, 2));
    g.setIndex(idx);
    g.computeVertexNormals();
    return g;
  }

  // A pivoted rotation: outer group sits at the pivot and rotates, inner group moves back so contents keep
  // their absolute coordinates.
  function pivoted(pivot, rot) {
    const outer = new THREE.Group(), inner = new THREE.Group();
    outer.position.set(-pivot[0], pivot[1], pivot[2]);
    const r = rot ? [-rot[0] * D2R, -rot[1] * D2R, rot[2] * D2R] : [0, 0, 0];
    outer.rotation.set(r[0], r[1], r[2], 'ZYX');
    inner.position.set(pivot[0], -pivot[1], -pivot[2]);
    outer.add(inner);
    return { outer, inner, rest: { pos: outer.position.clone(), rot: r } };
  }

  function build(geo, texture, glowTexture) {
    const g = geo['minecraft:geometry'][0], tw = g.description.texture_width, th = g.description.texture_height;
    prep(texture);
    const mat = new THREE.MeshLambertMaterial({ map: texture, side: THREE.DoubleSide, transparent: true, alphaTest: 0.04 });
    const glowMat = glowTexture ? new THREE.MeshBasicMaterial({ map: prep(glowTexture), side: THREE.FrontSide, transparent: true,
      blending: THREE.AdditiveBlending, depthWrite: false, polygonOffset: true, polygonOffsetFactor: -1 }) : null;
    const root = new THREE.Group(), bones = {};
    for (const b of g.bones) {
      const p = pivoted(b.pivot || [0, 0, 0], b.rotation);
      bones[b.name] = p;
      (b.parent && bones[b.parent] ? bones[b.parent].inner : root).add(p.outer);
      for (const c of b.cubes || []) {
        const geom = cubeGeometry(c, tw, th);
        let holder = p.inner;
        if (c.rotation) { const cp = pivoted(c.pivot || [0, 0, 0], c.rotation); p.inner.add(cp.outer); holder = cp.inner; }
        holder.add(new THREE.Mesh(geom, mat));
        if (glowMat) holder.add(new THREE.Mesh(geom, glowMat));
      }
    }
    root.userData.bones = bones;
    return root;
  }

  function sample(track, t) {
    const keys = Object.keys(track).map(Number).sort((a, b) => a - b);
    if (t <= keys[0]) return track[String(keys[0])] || track[keys[0].toString()];
    for (let i = 1; i < keys.length; i++) {
      if (t <= keys[i]) {
        const a = keys[i - 1], b = keys[i], f = (t - a) / (b - a), va = track[fmt(a)], vb = track[fmt(b)];
        return [0, 1, 2].map(j => va[j] + (vb[j] - va[j]) * f);
      }
    }
    return track[fmt(keys[keys.length - 1])];
  }
  function fmt(n) { return String(n); }

  // Poses the model at time t (seconds) of an animation clip; clip = null puts it back at rest.
  function pose(root, clip, t) {
    const bones = root.userData.bones;
    for (const name in bones) {
      const b = bones[name];
      b.outer.position.copy(b.rest.pos);
      b.outer.rotation.set(b.rest.rot[0], b.rest.rot[1], b.rest.rot[2], 'ZYX');
    }
    if (!clip) return;
    const len = clip.animation_length || 1, time = clip.loop === false ? Math.min(t, len) : t % len;
    for (const name in clip.bones) {
      const b = bones[name]; if (!b) continue;
      const ch = clip.bones[name];
      if (ch.rotation) {
        const r = sample(ch.rotation, time);
        b.outer.rotation.set(b.rest.rot[0] - r[0] * D2R, b.rest.rot[1] - r[1] * D2R, b.rest.rot[2] + r[2] * D2R, 'ZYX');
      }
      if (ch.position) {
        const p = sample(ch.position, time);
        b.outer.position.set(b.rest.pos.x - p[0], b.rest.pos.y + p[1], b.rest.pos.z + p[2]);
      }
    }
  }

  global.GeoModel = { build, pose };
})(typeof window !== 'undefined' ? window : globalThis);
