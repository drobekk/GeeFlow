import test from 'node:test';
import assert from 'node:assert/strict';
import { DraftHistory, mergeRecords, resolveMerge, operatePhase } from './editor-workflow.js';
import { movePhasePoint, createBlankRecord, phaseCurvePoints, projectPhases, insertPhasePoint, moveRecordingSample, recordingReplayPoints, assessCompatibility } from './editor-core.js';
const phase = (id, max = 10000) => ({ id, control: { type:'pressure', bar:9 }, maximumDurationMillis:max, minimumDurationMillis:0, ramp:{style:'Linear',durationMillis:5000,start:'PreviousTarget'},exitConditions:[] });
test('ramp completion and hold vertices share insertion geometry',()=>{
 const p={type:'phases',version:1,phases:[{...phase('first'),control:{type:'pressure',bar:3},ramp:{style:'Instant',durationMillis:0}},phase('ramp')]};
 assert.deepEqual(phaseCurvePoints(projectPhases(p)[1]),[[10000,3],[15000,9],[20000,9]]);
 const u=insertPhasePoint(p,1,12500,6,()=> 'new'); assert.equal(u.phases[1].control.bar,6);
});
test('unknown measured and feedback starts are not drawn as zero',()=>{
 const p={phases:[phase('a')]}; assert.deepEqual(phaseCurvePoints(projectPhases(p)[0]),[]);
 assert.equal(phaseCurvePoints(projectPhases(p,{measurements:{pressure:2}})[0])[0][1],2);
 p.phases[0].adaptiveFlowProbe={}; assert.deepEqual(phaseCurvePoints(projectPhases(p,{measurements:{pressure:2}})[0]),[]);
});
test('eased insert requires explicit conversion',()=>{
 const p={phases:[{...phase('a'),ramp:{style:'EaseIn',durationMillis:10000,start:'CurrentMeasurement'}}]};
 assert.throws(()=>insertPhasePoint(p,0,5000,3,()=> 'new',{pressure:0}),/Convert to Linear/);
});
test('sample zero value editable without moving anchor; replay preview is stepped',()=>{
 const r={controlMode:'Flow',samples:[{elapsedMillis:0,data:{flowRate:1,pressure:2}},{elapsedMillis:1000,data:{flowRate:3,pressure:5}}]};
 assert.equal(moveRecordingSample(r,0,{elapsedMillis:0,channel:'flowRate',value:4}).samples[0].data.flowRate,4);
 assert.deepEqual(recordingReplayPoints(r),[[0,1],[1000,1],[1000,3]]);
 assert.equal(recordingReplayPoints(r,'native').length,3);
});
test('native capacity uses resampled duration, not raw count',()=>{
 const p=createBlankRecord('id').profile;p.finishCondition={type:'weight',target:36};p.program={type:'recording',recording:{controlMode:'Pressure',samples:[{elapsedMillis:0,data:{pressure:1}},{elapsedMillis:83000,data:{pressure:3}}]}};
 assert.equal(assessCompatibility(p).route,'app');assert.equal(assessCompatibility(p).nativeCount,167);
});
test('history groups edits and restores complete records',()=>{
 const r=createBlankRecord('id'); const h=new DraftHistory(r);r.profile.name='a';h.record(r,'name');r.profile.name='ab';h.record(r,'name');
 assert.equal(h.undo().profile.name,'');assert.equal(h.redo().profile.name,'ab');
});
test('three-way merge preserves independent phase edits and detects overlapping/reorder changes',()=>{
 const b=createBlankRecord('id');b.profile.program.phases=[phase('a'),phase('b')]; const l=structuredClone(b),r=structuredClone(b);
 l.profile.program.phases[0].name='local';r.profile.program.phases[1].name='remote';
 let m=mergeRecords(b,l,r); assert.equal(m.conflicts.length,0); assert.equal(m.result.profile.program.phases[1].name,'remote');
 r.profile.program.phases[0].name='overlap';m=mergeRecords(b,l,r); assert.equal(m.conflicts.length,1);assert.throws(()=>resolveMerge(m,[]));assert.equal(resolveMerge(m,['remote']).profile.program.phases[0].name,'overlap');
 const moved=operatePhase(b.profile.program,1,'up',()=> 'new'); assert.equal(moved.phases[0].id,'b');
});

test('portable out-of-range replay targets can be stored but are unsupported for execution',()=>{
 const p=createBlankRecord('id').profile;p.finishCondition={type:'volume',target:36};p.program={type:'recording',version:1,recording:{controlMode:'Pressure',samples:[{elapsedMillis:0,data:{pressure:14}}]}};
 assert.equal(assessCompatibility(p).route,'unsupported');
});

test('moving a constant-source goal edits the actual source, preserving the legacy control',()=>{
 const p={type:'phases',version:2,phases:[{...phase('a'),targetSource:{kind:'Constant',value:4}}]};
 const updated=movePhasePoint(p,0,{value:5});assert.equal(updated.phases[0].targetSource.value,5);assert.equal(updated.phases[0].control.bar,9);
 p.phases[0].targetSource={kind:'CapturedValue',capture:'x'};assert.throws(()=>movePhasePoint(p,0,{value:5}),/computed target/);
});
