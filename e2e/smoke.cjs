/* Runs only against the isolated CI/local review server; uses a new test account. */
const {chromium} = require('playwright');
const assert = require('node:assert/strict');
const fs = require('node:fs');
const base = process.env.BREWLAB_TEST_URL || 'http://127.0.0.1:8080';
const routes = ['inicio','cafes','equipos','recetas','preparacion','catacion','analisis','experimentos','ajustes'];
(async () => {
  fs.mkdirSync('test-results', {recursive:true});
  let ready = false;
  for (let attempt=0; attempt<90; attempt++) {
    try { const r=await fetch(base+'/auth/login'); if(r.ok){ready=true;break;} } catch {}
    await new Promise(resolve=>setTimeout(resolve,2000));
  }
  assert(ready, 'Tomcat did not start within three minutes');
  const browser=await chromium.launch();
  const page=await browser.newPage({viewport:{width:1440,height:1000}});
  const errors=[];
  page.on('pageerror',error=>errors.push(error.message));
  async function open(path) {
    const response=await page.goto(base+path);
    assert(response.ok(), path+' HTTP '+response.status());
    assert(!/Exception Report|JasperException/.test(await page.locator('body').innerText()),path+' JSP error');
  }
  async function submit(action) {
    const form=page.locator('form').filter({has:page.locator(`input[name="action"][value="${action}"]`)}).first();
    await Promise.all([page.waitForNavigation(),form.locator('button[type="submit"]').first().click()]);
  }
  try {
    await open('/auth/registro');
    await page.locator('[name="displayName"]').fill('Revisión de interfaz');
    await page.locator('[name="email"]').fill('review-'+Date.now()+'@example.com');
    await page.locator('[name="password"]').fill('Review-only-9274!');
    await Promise.all([page.waitForURL('**/app/inicio**'),page.getByRole('button',{name:'Crear cuenta e iniciar'}).click()]);
    assert(await page.getByText('Sin sesión abierta',{exact:true}).isVisible());
    await page.screenshot({path:'test-results/dashboard-empty.png',fullPage:true});
    for(const route of routes) await open('/app/'+route);
    // Existing explicit [DEMO] action exercises populated branches and charts.
    await open('/app/ajustes');
    await submit('seedDemo');
    for(const width of [1440,768,390,320]) {
      await page.setViewportSize({width,height:1000});
      for(const route of routes) {
        await open('/app/'+route);
        assert.equal(await page.locator('h1').count(),1,route+' must have one h1');
        assert(await page.locator('.lab-nav [aria-current="page"]').count() === (route==='ajustes'?0:1),route+' active nav');
        const overflow=await page.evaluate(()=>document.documentElement.scrollWidth>innerWidth+1);
        assert(!overflow,route+' overflow at '+width);
        await page.screenshot({path:`test-results/${route}-${width}.png`,fullPage:true});
      }
    }
    await page.setViewportSize({width:1440,height:1000});
    await open('/app/cafes');
    await page.locator('.sample-file a[href*="/lote?id="]').first().click();
    assert.equal(await page.locator('#form-batch').count(),1);
    await open('/app/recetas');
    await page.locator('.protocol-entry a[href*="/detalle?id="]').first().click();
    assert.equal(await page.locator('#pasos-receta').count(),1);
    // Start, pause, reload and resume the existing timestamp-based timer.
    await open('/app/preparacion?new=true');
    await submit('createSession');
    assert.equal(await page.locator('#brew-session-stage').getAttribute('data-status'),'ACTIVE');
    await submit('pause');
    const paused=await page.locator('#brew-timer-display').innerText();
    await page.reload();
    assert.equal((await page.locator('#brew-timer-display').innerText()).trim(),paused.trim());
    await submit('resume');
    assert.equal(await page.locator('#brew-session-stage').getAttribute('data-status'),'ACTIVE');
    await open('/app/inicio');
    assert(await page.getByRole('link',{name:'Abrir sesión'}).isVisible());
    await page.screenshot({path:'test-results/dashboard-active.png',fullPage:true});
    await open('/app/catacion');
    await page.locator('#flavor-search-input').fill('jazmín');
    assert((await page.locator('.descriptor-row:visible').count())>0);
    await page.locator('#flavor-clear-filter-btn').click();
    await page.locator('.wheel-sector').first().focus();
    await page.keyboard.press('Enter');
    await page.locator('#wheel-reset-center').focus();
    await page.keyboard.press('Enter');
    // Explicitly compare two seeded sessions: verifies populated SVG and table.
    await open('/app/analisis');
    const boxes=page.locator('#comparison-sessions input[type="checkbox"]');
    assert((await boxes.count())>=2);
    await boxes.nth(0).check(); await boxes.nth(1).check();
    await Promise.all([page.waitForNavigation(),page.locator('#comparison-sessions button[type="submit"]').click()]);
    assert(await page.locator('svg[role="img"]').isVisible());
    await page.screenshot({path:'test-results/comparison.png',fullPage:true});
    await page.emulateMedia({reducedMotion:'reduce'});
    await open('/app/inicio');
    assert.equal(await page.locator('.btn-copper').evaluate(el=>getComputedStyle(el).transform),'none');
    await page.locator('.lab-account summary').click();
    await Promise.all([page.waitForURL('**/auth/login**'),page.getByRole('button',{name:'Cerrar sesión',exact:true}).click()]);
    assert.equal(errors.length,0,errors.join('\n'));
    console.log('PASS: JSP routes, archive/detail/protocol, responsive 320–1440, timer pause/resume, sensory keyboard/search, comparison, logout.');
  } catch(error) {
    await page.screenshot({path:'test-results/failure.png',fullPage:true});
    fs.writeFileSync('test-results/failure.txt',await page.locator('body').innerText());
    throw error;
  } finally { await browser.close(); }
})().catch(error=>{console.error(error);process.exitCode=1;});
