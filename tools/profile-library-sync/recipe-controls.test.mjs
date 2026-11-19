import test from 'node:test';
import assert from 'node:assert/strict';
import {changeStart,startOptions,changeTransition,changeLimit,setPhaseGoal,changeFeedbackMode,changeRangeMinimum} from './recipe-controls.js';
import {validateProgram, controlTargets, normalizeRecord, createBlankRecord} from './editor-core.js';
const phase=(id,type='pressure')=>({id,name:id,control:type==='flow'?{type:'flow',millilitresPerSecond:3.5}:{type:'pressure',bar:6,location:'Pump'},maximumDurationMillis:10000,minimumDurationMillis:0,ramp:{style:'Linear',durationMillis:10000,start:'PreviousTarget'},exitConditions:[]});
test('earlier-phase start creates a valid capture without changing targets or timing',()=>{
 const p={type:'phases',version:1,phases:[phase('Pressurize'),phase('Extract','flow')]};
 const next=changeStart(p,1,'end:Pressurize');assert.equal(next.version,2);const capture=next.phases[0].exitCaptures[0];
 assert.equal(capture.metric,'PumpFlow');assert.equal(next.phases[1].ramp.source.capture,capture.name);assert.equal(next.phases[1].control.millilitresPerSecond,3.5);assert.equal(next.phases[1].maximumDurationMillis,10000);assert.deepEqual(validateProgram(next),[]);assert.equal(p.phases[0].exitCaptures,undefined);
 assert.equal(startOptions(next,1).options.find(o=>o.value==='end:Pressurize').label,'Flow when Pressurize ends');
 assert.throws(()=>changeStart(p,0,'end:Extract'),/earlier/);
});
test('existing named captures and custom filter settings survive simple-source editing',()=>{
 const p={type:'phases',version:2,phases:[phase('p'),phase('f','flow')]};
 p.phases[0].exitCaptures=[{name:'first',metric:'PumpFlow',windowMillis:300},{name:'custom',metric:'PumpFlow',filter:'Latest',windowMillis:750,maximumAgeMillis:350,minimum:0.5,maximum:9,fallback:1.2}];p.phases[1].ramp.source={kind:'CapturedValue',capture:'custom'};
 assert.equal(startOptions(p,1).selected,'end:p');assert.deepEqual(changeStart(p,1,'end:p'),p);
 const next=changeStart(p,1,'fixed');assert.deepEqual(next.phases[0].exitCaptures,p.phases[0].exitCaptures);assert.equal(next.phases[1].ramp.source.kind,'Constant');
});
test('unrecognized custom start is preserved and is clearly identified',()=>{
 const p={type:'phases',version:2,phases:[phase('a'),phase('b')]};p.phases[1].ramp.source={kind:'PreviousApplied'};
 assert.equal(startOptions(p,1).selected,'custom');assert.deepEqual(changeStart(p,1,'custom'),p);
});
test('simple transition and limit controls preserve unrelated recipe settings',()=>{
 const p={type:'phases',version:2,phases:[phase('a')]};p.phases[0].ramp.style='EaseOut';p.phases[0].ramp.source={kind:'Constant',value:2};p.phases[0].entryCaptures=[{name:'x',metric:'PumpFlow'}];
 assert.deepEqual(changeTransition(p,0,'ramp'),p);
 const instant=changeTransition(p,0,'instant');assert.equal(instant.phases[0].ramp.durationMillis,0);assert.equal(instant.phases[0].ramp.source,null);assert.deepEqual(instant.phases[0].entryCaptures,p.phases[0].entryCaptures);
 const limited=changeLimit(p,0,3);limited.phases[0].secondaryLimit.recoveryPerSecond=0.8;
 assert.equal(changeLimit(limited,0,4).phases[0].secondaryLimit.recoveryPerSecond,0.8);assert.equal(changeLimit(limited,0,null).phases[0].secondaryLimit,null);
 p.phases[0].targetSource={kind:'Constant',value:8};assert.equal(setPhaseGoal(p,0,9).phases[0].targetSource.value,9);
});

test('one selected controller, zero inactive target, and no dual control fields',()=>{
 assert.deepEqual(controlTargets({type:'pressure',bar:9}),{controller:'pressure',pressureBar:9,flowMillilitresPerSecond:0});
 assert.deepEqual(controlTargets({type:'flow',millilitresPerSecond:0}),{controller:'flow',pressureBar:0,flowMillilitresPerSecond:0});
 const p={type:'phases',version:1,phases:[phase('p')]};p.phases[0].control.millilitresPerSecond=2;assert.match(validateProgram(p).join(' '),/one controller/);
 const record=createBlankRecord('test');record.profile.program.phases=[phase('f','flow'),{...phase('wait'),control:{type:'pump_pause'},ramp:{style:'Instant',durationMillis:0,start:'PreviousTarget'}}];
 const normalized=normalizeRecord(record);assert.deepEqual(normalized.profile.program.phases[1].control,{type:'flow',millilitresPerSecond:0});assert.equal(record.profile.program.phases[1].control.type,'pump_pause');
});
test('range feedback preserves the controller and custom response settings',()=>{
 const p={type:'phases',version:2,phases:[phase('p')]};let next=changeFeedbackMode(p,0,'range');next=changeLimit(next,0,2.5);next=changeRangeMinimum(next,0,1.5);
 next.phases[0].secondaryLimit.recoveryPerSecond=0.75;
 assert.equal(next.phases[0].control.type,'pressure');assert.equal(next.phases[0].secondaryLimit.metric,'PumpFlow');assert.deepEqual(validateProgram(next),[]);
 assert.throws(()=>changeRangeMinimum(next,0,3),/below/);assert.throws(()=>changeLimit(next,0,1),/exceed/);
 const soft=changeFeedbackMode(next,0,'limit');assert.equal(soft.phases[0].secondaryLimit.minimum,null);assert.equal(soft.phases[0].secondaryLimit.recoveryPerSecond,0.75);
});
