// Builds three.js meshes from Warfront unit specs using Minecraft's own cube UV and
// part-transform rules, so the preview matches what the game renders.
(function (global) {
  function cubeGeometry(b, texW, texH) {
    const g0 = b.grow || 0;   // CubeDeformation: inflate the box without changing its UVs
    let [x0, y0, z0] = b.origin;
    const [w, h, d] = b.size;
    let x1 = x0 + w + g0;
    x0 -= g0;
    const y1 = y0 + h + g0, z1 = z0 + d + g0;
    y0 -= g0; z0 -= g0;
    if (b.mirror) { const t = x1; x1 = x0; x0 = t; }   // ModelPart.Cube swaps x for mirrored boxes
    const [u, v] = b.uv;
    const W = Math.ceil(w), H = Math.ceil(h), D = Math.ceil(d);
    const f4 = u, f5 = u + D, f6 = u + D + W, f7 = u + D + W + W, f8 = u + D + W + D, f9 = u + D + W + D + W;
    const f10 = v, f11 = v + D, f12 = v + D + H;
    // Vertices as named in ModelPart.Cube.
    const V7 = [x0, y0, z0], V = [x1, y0, z0], V1 = [x1, y1, z0], V2 = [x0, y1, z0];
    const V3 = [x0, y0, z1], V4 = [x1, y0, z1], V5 = [x1, y1, z1], V6 = [x0, y1, z1];
    let polys = [
      [[V4, V3, V7, V], f5, f10, f6, f11],
      [[V1, V2, V6, V5], f6, f11, f7, f10],
      [[V7, V3, V6, V2], f4, f11, f5, f12],
      [[V, V7, V2, V1], f5, f11, f6, f12],
      [[V4, V, V1, V5], f6, f11, f8, f12],
      [[V3, V4, V5, V6], f8, f11, f9, f12],
    ];
    const pos = [], uvs = [], idx = [];
    for (const [verts, u1, v1, u2, v2] of polys) {
      let vs = verts.slice();
      let uv = [[u2, v1], [u1, v1], [u1, v2], [u2, v2]];
      if (b.mirror) { vs.reverse(); uv.reverse(); }
      const base = pos.length / 3;
      for (let i = 0; i < 4; i++) {
        pos.push(vs[i][0], vs[i][1], vs[i][2]);
        uvs.push(uv[i][0] / texW, uv[i][1] / texH);
      }
      idx.push(base, base + 1, base + 2, base, base + 2, base + 3);
    }
    const g = new THREE.BufferGeometry();
    g.setAttribute('position', new THREE.Float32BufferAttribute(pos, 3));
    g.setAttribute('uv', new THREE.Float32BufferAttribute(uvs, 2));
    g.setIndex(idx);
    g.computeVertexNormals();
    return g;
  }

  function buildPart(p, material, texW, texH, registry, glowMat) {
    const group = new THREE.Group();
    group.name = p.name;
    group.position.set(p.pivot[0], p.pivot[1], p.pivot[2]);
    group.rotation.set(p.rot[0], p.rot[1], p.rot[2], 'ZYX');
    group.userData.baseRot = p.rot.slice();
    for (const b of p.boxes) {
      const geo = cubeGeometry(b, texW, texH);
      group.add(new THREE.Mesh(geo, material));
      if (glowMat) group.add(new THREE.Mesh(geo, glowMat));
    }
    for (const c of p.children) group.add(buildPart(c, material, texW, texH, registry, glowMat));
    registry[p.name] = group;
    return group;
  }

  function prep(t) { t.magFilter = THREE.NearestFilter; t.minFilter = THREE.NearestFilter; t.flipY = false; return t; }

  function buildModel(spec, texture, glowTexture) {
    prep(texture);
    const material = new THREE.MeshLambertMaterial({ map: texture, side: THREE.DoubleSide, alphaTest: 0.1 });
    // Emissive pass, like RenderType.eyes: full-bright, additive.
    const glowMat = glowTexture ? new THREE.MeshBasicMaterial({ map: prep(glowTexture), side: THREE.DoubleSide,
      transparent: true, blending: THREE.AdditiveBlending, depthWrite: false, polygonOffset: true,
      polygonOffsetFactor: -1 }) : null;
    const registry = {};
    const inner = new THREE.Group();
    for (const p of spec.parts) inner.add(buildPart(p, material, spec.tex[0], spec.tex[1], registry, glowMat));
    // LivingEntityRenderer flips the model (scale -1,-1,1) and lifts it 1.501 blocks.
    inner.scale.set(-1, -1, 1);
    inner.position.y = 24.016;
    const outer = new THREE.Group();
    outer.add(inner);
    outer.userData.parts = registry;
    return outer;
  }

  global.MCModel = { buildModel };
})(window);
