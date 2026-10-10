// Renders gallery pictures of Warfront's generated models with the game's own cube and UV rules (mcmodel.js).
// node render_gallery.js <jobs.json> <outDir> <three.min.js path>
// Each job: {id, layers: [{model, tex, glow?}], view?: "three"|"side"|"front"}. Writes <outDir>/<id>.png (transparent).
const { chromium } = require('/opt/node22/lib/node_modules/playwright');
const fs = require('fs'), path = require('path');
const [jobsFile, outDir, threePath] = process.argv.slice(2);
const ROOT = path.resolve(__dirname, '../..');
const specs = Object.fromEntries(JSON.parse(fs.readFileSync(ROOT + '/mods/warfront/build/unit-models.json')).map(m => [m.id, m]));
const jobs = JSON.parse(fs.readFileSync(jobsFile));
const uri = f => 'data:image/png;base64,' + fs.readFileSync(f).toString('base64');
fs.mkdirSync(outDir, { recursive: true });
(async () => {
  const browser = await chromium.launch({ args: ['--use-gl=swiftshader', '--enable-unsafe-swiftshader'] });
  const page = await browser.newPage({ viewport: { width: 360, height: 360 } });
  await page.setContent(`<html><body style="margin:0"><script>${fs.readFileSync(threePath)}</script>
    <script>${fs.readFileSync(__dirname + '/mcmodel.js')}</script></body></html>`);
  await page.evaluate(() => {
    window.R = new THREE.WebGLRenderer({ antialias: true, alpha: true, preserveDrawingBuffer: true });
    R.setSize(360, 360); R.setClearColor(0x000000, 0); document.body.appendChild(R.domElement);
  });
  for (const job of jobs) {
    const layers = job.layers.map(l => ({ spec: specs[l.model], tex: uri(l.tex), glow: l.glow && fs.existsSync(l.glow) ? uri(l.glow) : null }));
    if (layers.some(l => !l.spec)) { console.error('missing model in', job.id); continue; }
    const png = await page.evaluate(async ({ layers, view }) => {
      const load = src => new Promise(r => new THREE.TextureLoader().load(src, r));
      const scene = new THREE.Scene();
      scene.add(new THREE.AmbientLight(0xffffff, 0.8));
      const dl = new THREE.DirectionalLight(0xffffff, 0.75); dl.position.set(-30, 60, -50); scene.add(dl);
      for (const l of layers) scene.add(MCModel.buildModel(l.spec, await load(l.tex), l.glow ? await load(l.glow) : null));
      const box = new THREE.Box3().setFromObject(scene);
      const c = box.getCenter(new THREE.Vector3()), s = box.getSize(new THREE.Vector3());
      const dir = { three: [0.72, 0.42, -0.72], side: [1, 0.15, 0], front: [0, 0.15, -1] }[view || 'three'];
      const cam = new THREE.OrthographicCamera(-1, 1, 1, -1, 0.1, 2000);
      cam.position.set(c.x + dir[0] * 300, c.y + dir[1] * 300, c.z + dir[2] * 300); cam.lookAt(c);
      cam.updateMatrixWorld();
      // Fit: project the box corners into view space and size the frustum to them.
      let minX = 1e9, maxX = -1e9, minY = 1e9, maxY = -1e9;
      for (const x of [box.min.x, box.max.x]) for (const y of [box.min.y, box.max.y]) for (const z of [box.min.z, box.max.z]) {
        const v = new THREE.Vector3(x, y, z).applyMatrix4(cam.matrixWorldInverse);
        minX = Math.min(minX, v.x); maxX = Math.max(maxX, v.x); minY = Math.min(minY, v.y); maxY = Math.max(maxY, v.y);
      }
      const half = Math.max(maxX - minX, maxY - minY) / 2 * 1.06, mx = (minX + maxX) / 2, my = (minY + maxY) / 2;
      Object.assign(cam, { left: mx - half, right: mx + half, top: my + half, bottom: my - half }); cam.updateProjectionMatrix();
      R.clear(); R.render(scene, cam);
      return R.domElement.toDataURL('image/png');
    }, { layers, view: job.view });
    fs.writeFileSync(path.join(outDir, job.id + '.png'), Buffer.from(png.split(',')[1], 'base64'));
  }
  await browser.close();
  console.log('rendered', jobs.length);
})();
