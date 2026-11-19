import test from 'node:test';
import assert from 'node:assert/strict';
import { readFile } from 'node:fs/promises';
import { RecipeEvaluator,simulateProfile,syntheticScenario } from './recipe-runtime.js';
import { importDecent } from './decent-import.js';
import {configureYield,validateExtensions} from './recipe-contract.js';
import {validateProfile,targetValue} from './editor-core.js';
const cases=JSON.parse(await readFile(new URL('../../shared/domain/device/src/jvmTest/resources/runtime-contract.json',import.meta.url)));
test('browser evaluator conforms to shared Kotlin execution traces',()=>{
 for(const c of cases){const engine=new RecipeEvaluator(c.profile);c.samples.forEach((sample,i)=>{const out=engine.tick(sample.elapsedMillis,sample);assert.ok(Math.abs(targetValue(out.target)-c.expected[i])<0.0001,c.name);assert.ok(['pressure','flow'].includes(out.controlTargets.controller));assert.equal(out.controlTargets.controller==='pressure'?out.controlTargets.flowMillilitresPerSecond:out.controlTargets.pressureBar,0);if(c.expectedControllers)assert.equal(out.controlTargets.controller,c.expectedControllers[i]);engine.acknowledgeApplied({...out.target,...(out.target.type==='flow'?{millilitresPerSecond:Math.round(targetValue(out.target)*10)/10}:{bar:Math.round(targetValue(out.target)*10)/10})});});}
});
test('Decent originals and independent yield survive import',async()=>{
 const source=JSON.parse(await readFile(new URL('./recipes/decent/cremina.json',import.meta.url)));
 const record=await importDecent(source,{adaptation:'pressure-only'});assert.equal(record.profile.program.phases.length,5);assert.deepEqual(record.profile.program.recipe.source.original,source);assert.equal(record.profile.finishCondition,null);
 const configured=configureYield(record,{doseG:15,ratio:2});assert.equal(configured.profile.finishCondition.target,30);assert.deepEqual(configured.profile.program.phases,record.profile.program.phases);assert.deepEqual(validateProfile(configured.profile),[]);
});
test('Adaptive explicit handoff retains source frame and limits',async()=>{
 const source=JSON.parse(await readFile(new URL('./recipes/decent/best_practice.json',import.meta.url)));
 const record=await importDecent(source,{adaptation:'adaptive-capture'});assert.equal(source.steps.length,7);assert.equal(record.profile.program.phases.length,6);assert.equal(record.profile.program.phases.at(-1).ramp.source.kind,'CapturedValue');assert.equal(record.profile.program.phases.at(-1).secondaryLimit.ceiling,9.5);assert.deepEqual(validateProfile(record.profile),[]);
 const a=simulateProfile(record.profile,syntheticScenario({flow:1.3})),b=simulateProfile(record.profile,syntheticScenario({flow:2.2}));assert.notEqual(a.captures.at(-1)?.value,b.captures.at(-1)?.value);assert.ok(a.trace.at(-1).finish);
});
test('deadline, delayed acknowledgements and stale data are inspectable stops',()=>{
 const profile=cases[2].profile;const p=structuredClone(profile);p.program.phases[0].secondaryLimit=null;p.program.phases[0].maximumDurationMillis=60000;
 const result=simulateProfile(p,syntheticScenario());assert.equal(result.trace.at(-1).finish,'profile_timeout');
 const stale=simulateProfile(p,syntheticScenario({staleAfterMillis:1000}));assert.equal(stale.trace.at(-1).finish,'telemetry_timeout');
 assert.ok(validateExtensions({...p.program,version:1}).length===0);
});
test('delayed quantized acknowledgements determine the next ramp start; missing acknowledgements stop',()=>{
 const c=cases[0],scenario={samples:c.samples,durationMillis:2000,stepMillis:100};
 const handoff=result=>result.trace.find(x=>x.phaseId==='decline');
 const fast=simulateProfile(c.profile,scenario),slow=simulateProfile(c.profile,{...scenario,acknowledgementDelayMillis:600});
 assert.equal(targetValue(handoff(fast).target),3.5);assert.equal(targetValue(handoff(slow).target),3.2);
 const missing=simulateProfile(c.profile,{...scenario,acknowledgementDelayMillis:2000});assert.match(missing.trace.at(-1).finish,/target source/);
 const disconnected=simulateProfile(c.profile,{...scenario,disconnectedAtMillis:500});assert.equal(disconnected.trace.at(-1).finish,'device_disconnected');
});
