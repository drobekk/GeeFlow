import { test, expect } from '@playwright/test';
const phase=(id,bar,duration=5000)=>({id,name:id,control:{type:'pressure',bar,location:'Pump'},maximumDurationMillis:duration,minimumDurationMillis:0,ramp:{style:'Instant',durationMillis:0,start:'PreviousTarget'},exitConditions:[],conditionOperator:'Or'});
const profile=()=>({id:'550e8400-e29b-41d4-a716-446655440000',revision:1,favorite:false,deleted:false,profile:{name:'Browser test',description:'',finishCondition:{type:'weight',target:36},autoLinkOpen:false,singleDoseSettings:{enabled:false,grindingSize:300,grindingSpeed:500},program:{type:'phases',version:1,phases:[phase('a',3),{...phase('b',9,10000),ramp:{style:'Linear',durationMillis:5000,start:'PreviousTarget'}}]}}});
async function connect(page,record=profile()) {
 const errors=[];page.on('pageerror',e=>errors.push(e.message));
 const catalog={schemaVersion:1,revision:1,profiles:[record]};
 await page.route('**/api/v1/catalog',async route=>{
  if(route.request().method()==='PUT'){const body=route.request().postDataJSON();body.revision++;await route.fulfill({json:body});}
  else await route.fulfill({json:catalog});
 });
 await page.goto('/profile-library-sync/');await page.getByLabel('Library key',{exact:true}).fill('browser-test-token');await page.getByRole('button',{name:'Connect',exact:true}).click();await page.getByRole('button',{name:'Browser test',exact:true}).click();
 return errors;
}
test('ramp hold geometry, point insertion, undo, draft recovery and explicit save',async({page})=>{
 const errors=await connect(page);const line=page.locator('#pressure-chart .chart-path').nth(1);expect((await line.getAttribute('d')).split('L')).toHaveLength(3);
 const hit=page.locator('#pressure-chart .chart-hit').nth(1);await hit.click({position:{x:15,y:15},force:true});
 await page.locator('[data-path="profile.name"]').fill('New draft');await page.getByRole('button',{name:'Undo',exact:true}).click();await expect(page.locator('[data-path="profile.name"]')).toHaveValue('Browser test');
 await page.locator('[data-path="profile.name"]').fill('Recovered');await page.reload();await page.getByLabel('Library key',{exact:true}).fill('browser-test-token');await page.getByRole('button',{name:'Connect',exact:true}).click();await page.getByRole('button',{name:'Restore draft'}).click();await expect(page.locator('[data-path="profile.name"]')).toHaveValue('Recovered');
 const storage=await page.evaluate(()=>localStorage.getItem('geeflow.profileDraft.v1'));expect(storage).not.toContain('browser-test-token');
 await page.getByRole('button',{name:'Save',exact:true}).click();await expect(page.locator('#editor-status')).toContainText('Saved');expect(errors).toEqual([]);
});
test('both recorded charts drag, sample zero anchor, pointer cancellation and keyboard',async({page})=>{
 const r=profile();r.profile.program={type:'recording',version:1,recording:{controlMode:'Flow',samples:[{elapsedMillis:0,data:{pressure:3,flowRate:2,volume:0,weight:0,weightRate:0}},{elapsedMillis:2000,data:{pressure:6,flowRate:4,volume:4,weight:3,weightRate:1}}]}};
 const errors=await connect(page,r);
 for(const chart of ['pressure','flow']){
  const point=page.locator(`#${chart}-chart [data-action="drag-sample-point"][data-index="0"]`);await point.scrollIntoViewIfNeeded();const box=await point.boundingBox();await page.mouse.move(box.x+box.width/2,box.y+box.height/2);await page.mouse.down();await page.mouse.move(box.x+box.width/2,box.y-10,{steps:5});await page.mouse.up();
 }
 await expect(page.locator('#inspector [data-sample-time="0"]')).toHaveValue('0');
 const value=await page.locator('#inspector [data-sample-index="0"][data-sample-key="flowRate"]').inputValue();expect(Number(value)).toBeGreaterThan(2);
 await page.locator('#flow-chart [data-index="1"]').focus();await page.keyboard.press('ArrowUp');expect(errors).toEqual([]);
});
test('phase operations preserve rules and conflict requires choices',async({page})=>{
 const errors=await connect(page);await page.locator('#inspector summary[aria-label="Phase actions"]').click();await page.locator('#inspector').getByRole('button',{name:'Duplicate',exact:true}).click();await expect(page.locator('.phase-row')).toHaveCount(3);
 await page.getByRole('button',{name:'Undo',exact:true}).click();await expect(page.locator('.phase-row')).toHaveCount(2);
 await page.locator('[data-path="profile.name"]').fill('local');const remote=profile();remote.profile.name='remote';let revision=2;
 await page.route('**/api/v1/catalog',async route=>{if(route.request().method()==='PUT')await route.fulfill({status:409,json:{error:'revision_conflict',catalog:{schemaVersion:1,revision,profiles:[remote]}}});else await route.fulfill({json:{schemaVersion:1,revision,profiles:[remote]}});});
 await page.getByRole('button',{name:'Save',exact:true}).click();await expect(page.locator('#save')).toBeDisabled();await page.locator('#use-latest').click();await expect(page.locator('[data-merge-index]')).toHaveCount(1);await page.locator('[data-merge-index]').selectOption('remote');await page.getByRole('button',{name:'Apply choices'}).click();await expect(page.locator('[data-path="profile.name"]')).toHaveValue('remote');expect(errors).toEqual([]);
});

test('Cremina import configures yield independently; Adaptive simulation exposes captured flow',async({page})=>{
 await page.setViewportSize({width:1440,height:1000});const errors=await connect(page);page.on('dialog',dialog=>dialog.accept());
 await page.locator('.sidebar-footer [data-open-dialog="import-dialog"]').click();await page.locator('#load-cremina').click();
 await expect(page.locator('.phase-row')).toHaveCount(5);await page.locator('#doseG').fill('15');await page.locator('#doseG').press('Tab');await page.locator('#ratio-toggle').click();await page.locator('#ratio').fill('2');await page.locator('#ratio').press('Tab');await page.locator('#ratio-dialog [data-close-dialog]').click();
 await expect(page.locator('#targetYieldG')).toHaveValue('30');
 if(process.env.EDITOR_EVIDENCE_DIR){await page.locator('.phase-row').nth(1).click();await page.screenshot({path:`${process.env.EDITOR_EVIDENCE_DIR}/cremina-workspace.png`,fullPage:true});}
 await page.locator('#preview').click();await expect(page.locator('#simulation-summary')).toContainText('s ·');await page.locator('#preview-panel [data-close-dialog]').click();
 await page.locator('.sidebar-footer [data-open-dialog="import-dialog"]').click();await page.locator('#load-adaptive').click();await expect(page.locator('.phase-row')).toHaveCount(6);
 await page.locator('.phase-row').last().click();await expect(page.locator('[data-product="start"]')).toHaveValue(/^end:/);
 const invalid=await page.locator('#editor input:invalid').evaluateAll(nodes=>nodes.map(n=>({value:n.value,path:n.dataset.v2Path||n.dataset.phasePath||n.id})));expect(invalid).toEqual([]);
 const saved=page.waitForRequest(r=>r.method()==='PUT'&&r.url().endsWith('/catalog'));
 await page.getByRole('button',{name:'Save',exact:true}).click();const payload=(await saved).postDataJSON();expect(payload.schemaVersion).toBe(2);expect(payload.profiles.at(-1).profile.program.recipe.source.original.steps).toHaveLength(7);await expect(page.locator('#editor-status')).toContainText('Saved');
 await page.locator('#preview').click();await expect(page.locator('#simulation-trace')).toContainText('extractionStartFlow');await expect(page.locator('#compatibility')).toContainText('Preview only');await page.locator('#preview-panel [data-close-dialog]').click();
 if(process.env.EDITOR_EVIDENCE_DIR){await page.locator('.chart-card').screenshot({path:`${process.env.EDITOR_EVIDENCE_DIR}/adaptive-charts.png`});await page.locator('#inspector').screenshot({path:`${process.env.EDITOR_EVIDENCE_DIR}/adaptive-inspector.png`});await page.screenshot({path:`${process.env.EDITOR_EVIDENCE_DIR}/adaptive-workspace.png`,fullPage:true});}
 expect(errors).toEqual([]);
});

test.describe('tablet gestures',()=>{
 test.use({hasTouch:true,viewport:{width:800,height:1100}});
 test('touch cancellation restores sample and committed drag changes only its value',async({page})=>{
  const r=profile();r.profile.program={type:'recording',version:1,recording:{controlMode:'Flow',samples:[{elapsedMillis:0,data:{pressure:3,flowRate:2,volume:0,weight:0,weightRate:0}},{elapsedMillis:2000,data:{pressure:6,flowRate:4,volume:4,weight:3,weightRate:1}}]}};
  const errors=await connect(page,r);const cdp=await page.context().newCDPSession(page);
  async function touch(cancel){const point=page.locator('#flow-chart [data-index="0"]');await point.scrollIntoViewIfNeeded();const b=await point.boundingBox();const x=b.x+b.width/2,y=b.y+b.height/2;await cdp.send('Input.dispatchTouchEvent',{type:'touchStart',touchPoints:[{x,y,id:1}]});for(let i=1;i<=4;i++)await cdp.send('Input.dispatchTouchEvent',{type:'touchMove',touchPoints:[{x,y:y-i*4,id:1}]});await cdp.send('Input.dispatchTouchEvent',{type:cancel?'touchCancel':'touchEnd',touchPoints:[]});}
  await touch(true);await expect(page.locator('#inspector [data-sample-index="0"][data-sample-key="flowRate"]')).toHaveValue('2');expect(await page.evaluate(()=>localStorage.getItem('geeflow.profileDraft.v1'))).toBeNull();
  await touch(false);expect(Number(await page.locator('#inspector [data-sample-index="0"][data-sample-key="flowRate"]').inputValue())).toBeGreaterThan(2);await expect(page.locator('#inspector [data-sample-time="0"]')).toHaveValue('0');if(process.env.EDITOR_EVIDENCE_DIR) await page.locator('.chart-card').screenshot({path:`${process.env.EDITOR_EVIDENCE_DIR}/tablet-recording-charts.png`});
  expect(errors).toEqual([]);
 });
});

test('point popover edits actual values and phase edges shift later phases without changing targets',async({page})=>{
 const errors=await connect(page);
 const point=page.locator('#pressure-chart [data-action="drag-phase-point"][data-index="0"]');await point.click();await expect(page.locator('#point-editor')).toBeVisible();
 await page.locator('[data-point-field="value"]').fill('4');await page.locator('[data-point-field="value"]').press('Tab');await page.locator('#close-point').click();
 const boundary=page.locator('#pressure-chart [data-action="drag-boundary"][data-index="0"]');await boundary.focus();await page.keyboard.press('Enter');await expect(page.locator('[data-point-field="value"]')).toHaveCount(0);
 await page.locator('[data-point-field="time"]').fill('6');await page.locator('[data-point-field="time"]').press('Tab');await page.locator('#close-point').click();
 const saved=page.waitForRequest(r=>r.method()==='PUT');await page.locator('#save').click();const phases=(await saved).postDataJSON().profiles[0].profile.program.phases;
 expect(phases[0].control.bar).toBe(4);expect(phases[0].maximumDurationMillis).toBe(6000);expect(phases[1].maximumDurationMillis).toBe(10000);expect(phases[1].ramp.durationMillis).toBe(5000);expect(errors).toEqual([]);
});

test('adaptive start selection creates its capture automatically and keeps phase settings',async({page})=>{
 const errors=await connect(page);await page.locator('.phase-row').last().click();
 await page.locator('[data-product="start"]').selectOption('end:a');await expect(page.locator('[data-product="start"]')).toHaveValue('end:a');
 await expect(page.locator('#technical-inspector')).not.toBeVisible();await expect(page.locator('#sim-ack')).not.toBeVisible();
 const saved=page.waitForRequest(r=>r.method()==='PUT');await page.locator('#save').click();const program=(await saved).postDataJSON().profiles[0].profile.program;
 expect(program.version).toBe(2);expect(program.phases[0].exitCaptures).toHaveLength(1);expect(program.phases[1].ramp.source.capture).toBe(program.phases[0].exitCaptures[0].name);expect(program.phases[1].control.bar).toBe(9);expect(program.phases[1].maximumDurationMillis).toBe(10000);expect(errors).toEqual([]);
});

for(const viewport of [{width:1440,height:1000},{width:390,height:844}])test(`compact workspace fits ${viewport.width}px without technical controls`,async({page})=>{
 await page.setViewportSize(viewport);const errors=await connect(page);
 await expect(page.locator('#access-card')).not.toBeVisible();await expect(page.locator('#drag-mode')).not.toBeVisible();await expect(page.locator('#machine-model')).not.toBeVisible();
 const overflow=await page.evaluate(()=>document.documentElement.scrollWidth>innerWidth);expect(overflow).toBe(false);
 expect(await page.locator('#editor input:visible').count()).toBeLessThanOrEqual(7);
 if(process.env.EDITOR_EVIDENCE_DIR)await page.screenshot({path:`${process.env.EDITOR_EVIDENCE_DIR}/workspace-${viewport.width}.png`,fullPage:true});
 expect(errors).toEqual([]);
});

test('pressure controller keeps flow target zero and adjusts only pressure from a flow range',async({page})=>{
 const errors=await connect(page);
 await page.locator('[data-product="goal"]').fill('9');await page.locator('[data-product="goal"]').press('Tab');
 expect(await page.locator('#inspector [data-phase-path="control.type"] option').evaluateAll(nodes=>nodes.map(n=>n.value))).toEqual(['pressure','flow']);
 await page.locator('#inspector .more-options > summary').click();await page.locator('#inspector').getByRole('button',{name:'Add flow feedback',exact:true}).click();
 await page.locator('[data-product="feedback-mode"]').selectOption('range');await page.locator('[data-product="limit"]').fill('2.5');await page.locator('[data-product="limit"]').press('Tab');
 await page.locator('[data-product="range-minimum"]').fill('1.5');await page.locator('[data-product="range-minimum"]').press('Tab');
 await expect(page.locator('#flow-chart [data-feedback="range"]')).toHaveCount(1);await expect(page.locator('#flow-chart [data-inactive-target="flow"]')).toHaveCount(2);
 const saved=page.waitForRequest(r=>r.method()==='PUT');await page.locator('#save').click();const program=(await saved).postDataJSON().profiles[0].profile.program;
 expect(program.phases[0].control).toEqual({type:'pressure',bar:9,location:'Pump'});expect(program.phases[0].secondaryLimit.minimum).toBe(1.5);expect(program.phases[0].secondaryLimit.ceiling).toBe(2.5);
 const flows=[2,3,2,1,2.5],scenario={durationMillis:2000,stepMillis:500,samples:flows.map((flow,i)=>({elapsedMillis:i*500,values:{PumpPressure:9,PumpFlow:flow,PumpedVolume:0,CupWeight:0}}))};
 await page.locator('#sensor-trace').setInputFiles({name:'range-input.json',mimeType:'application/json',buffer:Buffer.from(JSON.stringify(scenario))});
 await page.locator('#preview').click();
 const download=page.waitForEvent('download');await page.locator('#export-trace').click();const stream=await (await download).createReadStream();const chunks=[];for await(const chunk of stream)chunks.push(chunk);const result=JSON.parse(Buffer.concat(chunks).toString());
 expect(result.trace.map(x=>x.controlTargets.controller)).toEqual(flows.map(()=> 'pressure'));
 expect(result.trace.map(x=>x.controlTargets.flowMillilitresPerSecond)).toEqual(flows.map(()=>0));
 expect(result.trace.map(x=>x.measurements.PumpFlow)).toEqual(flows);
 expect(result.trace.map(x=>x.controlTargets.pressureBar)).toEqual([9,8.5,8.5,8.625,8.625]);
 await expect(page.locator('#flow-chart svg > path[stroke="#075f81"]')).toHaveCount(0);
 if(process.env.EDITOR_EVIDENCE_DIR){await page.setViewportSize({width:1440,height:1000});await page.screenshot({path:`${process.env.EDITOR_EVIDENCE_DIR}/pressure-flow-feedback.png`,fullPage:true});}
 expect(errors).toEqual([]);
});

test('legacy leading wait becomes a zero target with the following controller selected',async({page})=>{
 const r=profile();r.profile.program.phases=[{...phase('Wait',0,1000),control:{type:'pump_pause'}},{...phase('Extract',0,5000),control:{type:'flow',millilitresPerSecond:2}}];
 const errors=await connect(page,r);
 await expect(page.locator('#inspector [data-phase-path="control.type"]')).toHaveValue('flow');await expect(page.locator('[data-product="goal"]')).toHaveValue('0');
 await expect(page.locator('#pressure-chart [data-inactive-target="pressure"]')).toHaveCount(2);
 const saved=page.waitForRequest(r=>r.method()==='PUT');await page.locator('#save').click();const phases=(await saved).postDataJSON().profiles[0].profile.program.phases;
 expect(phases[0].control).toEqual({type:'flow',millilitresPerSecond:0});expect(phases[0].maximumDurationMillis).toBe(1000);expect(errors).toEqual([]);
});
