import { activeControl, controlTargets, rampFraction, targetValue, withTarget } from './editor-core.js';
const metricOf = c => c.type === 'flow' ? 'PumpFlow' : c.type === 'pressure' ? ({Pump:'PumpPressure',Group:'GroupPressure',Boiler:'BoilerPressure'}[c.location || 'Pump']) : null;
const compare=(v,c)=>({Above:()=>v>=c.threshold,Below:()=>v<=c.threshold,StrictlyAbove:()=>v>c.threshold,StrictlyBelow:()=>v<c.threshold}[c.comparison]?.() ?? false);
export class RecipeEvaluator {
  constructor(profile,{simulation=false}={}) {
    this.profile=profile;this.simulation=simulation;this.index=0;this.entered=false;this.previousGoal=null;this.previousLegacy=null;this.applied=null;this.lastController=null;this.captured={};this.transitions=[];this.captureEvents=[];this.decisions=[];this.history=[];this.lastTime=0;this.finished=null;
  }
  acknowledgeApplied(target){if(!this.finished)this.applied=structuredClone(activeControl(target,this.applied||this.lastController));}
  fresh(metric,t,data,maxAge=500){const at=data.sampledAtMillis?.[metric],v=data.values[metric];if(at==null||at>t||t-at>maxAge||!Number.isFinite(v)||v<0)throw new Error(`Invalid or stale measurement: ${metric}`);return v;}
  capture(actions,t,data){for(const a of actions||[]){let value,fallbackUsed=false;try{this.fresh(a.metric,t,data,a.maximumAgeMillis??500);const seen=new Set();const values=this.history.filter(x=>t-x.t<=(a.windowMillis??500)).filter(x=>{const at=x.data.sampledAtMillis?.[a.metric];if(at==null||at>t||t-at>(a.maximumAgeMillis??500)||seen.has(at))return false;seen.add(at);return Number.isFinite(x.data.values[a.metric]);}).map(x=>x.data.values[a.metric]);value=(a.filter||'Mean')==='Mean'?values.reduce((x,y)=>x+y,0)/values.length:this.fresh(a.metric,t,data,a.maximumAgeMillis??500);if(!Number.isFinite(value)||value<(a.minimum??0)||value>(a.maximum??12))throw new Error('Out of bounds');}catch(error){if(a.fallback==null)throw new Error(`Invalid capture: ${a.name}`);value=a.fallback;fallbackUsed=true;}this.captured[a.name]=value;this.captureEvents.push({elapsedMillis:t,name:a.name,metric:a.metric,value,sampledAtMillis:data.sampledAtMillis?.[a.metric]??null,fallbackUsed});}}
  resolve(source,phase,t,data){let v;switch(source.kind){case'Constant':v=source.value;break;case'MeasurementAtEntry':v=this.fresh(source.metric,t,data);break;case'CapturedValue':v=this.captured[source.capture];break;case'PreviousGoal':if(metricOf(this.previousGoal||{})===metricOf(phase.control))v=targetValue(this.previousGoal);break;case'PreviousApplied':if(metricOf(this.applied||{})===metricOf(phase.control))v=targetValue(this.applied);break;}if(!Number.isFinite(v)||v<0||v>12)throw new Error('Missing or out-of-range target source');return v;}
  tick(t,data){
    if(this.finished)return{finish:this.finished};if(t<this.lastTime)throw new Error('Execution time must be monotonic');
    const p=this.profile.program;if(!this.simulation&&t>=Math.min(40000,p.execution?.maximumTotalDurationMillis??40000))return this.finish('profile_timeout');
    this.history.push({t,data:structuredClone(data)});this.history=this.history.filter(x=>t-x.t<=10000);
    if(p.version===2&&this.profile.finishCondition?.type==='weight')this.fresh('CupWeight',t,data);
    const goal=this.profile.finishCondition;const m=goal?.type==='weight'?'CupWeight':'PumpedVolume';if(goal&&data.values[m]>=goal.target)return this.finish('global_goal');
    if(p.type==='recording'){
      const r=p.recording,end=r.samples.at(-1).elapsedMillis;if(t>=Math.max(30000,end*2))return this.finish('recording_timeout');if(!goal&&t>=end)return this.finish('program_complete');
      const a=r.samples.findLast(s=>s.elapsedMillis<=t)||r.samples[0],b=r.samples.find(s=>s.elapsedMillis>t),key=r.controlMode==='Flow'?'flowRate':'pressure';let v=a.data[key];
      if(p.version===2&&p.replayPolicy==='Linear'&&b)v+=(b.data[key]-v)*(t-a.elapsedMillis)/(b.elapsedMillis-a.elapsedMillis);
      this.lastTime=t;return this.output(r.controlMode==='Flow'?{type:'flow',millilitresPerSecond:v}:{type:'pressure',bar:v,location:'Pump'});
    }
    while(this.index<p.phases.length){
      const phase=p.phases[this.index];if(!this.entered){this.enteredAt=t;this.capture(phase.entryCaptures,t,data);this.goal=phase.targetSource?this.resolve(phase.targetSource,phase,t,data):targetValue(phase.control);
        const metric=metricOf(phase.control);this.from=phase.ramp?.source?this.resolve(phase.ramp.source,phase,t,data):(phase.ramp?.style||'Instant')==='Instant'?targetValue(phase.control):phase.ramp?.start==='PreviousTarget'&&metricOf(this.previousLegacy||{})===metric?targetValue(this.previousLegacy):p.version===2?this.fresh(metric,t,data):data.values[metric];
        if(!Number.isFinite(this.from))throw new Error('Missing ramp start measurement');this.entered=true;this.matchSince=null;this.limitValue=null;this.limitExceededAt=null;this.probeMaximum=null;this.probeHold=null;this.volume=data.values.PumpedVolume;this.weight=data.values.CupWeight;
      }
      const dt=t-this.enteredAt,probe=phase.adaptiveFlowProbe,pressure=data.values.PumpPressure,flow=data.values.PumpFlow;
      if(probe&&dt<probe.samplingDurationMillis&&pressure<probe.pressureCeilingBar&&Number.isFinite(flow))this.probeMaximum=Math.max(this.probeMaximum??flow,flow);
      let matched=null;const matches=c=>{const measured=c.metric==='PhaseTime'?null:p.version===2?this.fresh(c.metric,t,data):data.values[c.metric];const v=c.metric==='PhaseTime'?dt/1000:c.scope==='Step'?measured-({PumpedVolume:this.volume,CupWeight:this.weight}[c.metric]):measured;const ok=Number.isFinite(v)&&v>=0&&compare(v,c);if(ok)matched=c;return ok;};
      const conditions=phase.exitConditions||[],measurements=conditions.filter(c=>c.metric!=='PhaseTime');const raw=dt>=(phase.minimumDurationMillis||0)&&((phase.conditionOperator||'Or')==='Or'?conditions.some(matches):conditions.filter(c=>c.metric==='PhaseTime').some(matches)||(measurements.length>0&&measurements.every(matches)));
      if(raw){this.matchSince??=t;}else this.matchSince=null;const timeDeadlineMet=dt>=(phase.minimumDurationMillis||0)&&conditions.filter(c=>c.metric==='PhaseTime').some(matches);const met=timeDeadlineMet||(raw&&t-this.matchSince>=(phase.stabilityDurationMillis||0));
      const reason=dt>=phase.maximumDurationMillis?'MaximumDuration':met?'ConditionMatched':null;
      if(reason){this.transitions.push({elapsedMillis:t,phaseId:phase.id,reason,condition:reason==='ConditionMatched'?matched:null});this.capture(phase.exitCaptures,t,data);this.previousGoal=withTarget(phase.control,this.goal);this.previousLegacy=phase.control;this.index++;this.entered=false;continue;}
      let value=this.from+(this.goal-this.from)*rampFraction(phase.ramp,dt);
      if(probe){if(dt<probe.samplingDurationMillis){if(pressure>=probe.pressureCeilingBar)value=Math.min(value,this.probeMaximum??this.from);}else{this.probeHold??=this.probeMaximum??this.from;if(pressure>=probe.pressureCeilingBar&&Number.isFinite(flow))this.probeHold=Math.min(this.probeHold,Math.max(0,flow-(probe.flowBackoffMillilitresPerSecond??0.1)));value=this.probeHold;}}
      const requested=activeControl(withTarget(phase.control,value),this.applied||this.lastController||(this.previousLegacy?.type!=='pump_pause'?this.previousLegacy:null)||p.phases.find(p=>p.control.type!=='pump_pause')?.control),limit=phase.secondaryLimit;
      if(limit){const measured=this.fresh(limit.metric,t,data),seconds=Math.min(500,Math.max(0,t-this.lastTime))/1000;const previous=this.limitValue??Math.min(value,metricOf(this.applied||{})===metricOf(phase.control)?targetValue(this.applied):this.from);
        value=(limit.minimum!=null?measured>limit.ceiling:measured>=limit.ceiling)?Math.min(value,Math.max(0,previous-(limit.reductionPerSecond??1)*seconds)):measured<(limit.minimum??(limit.ceiling-(limit.hysteresis??0.2)))?Math.min(value,previous+(limit.recoveryPerSecond??0.25)*seconds):Math.min(value,previous);value=Math.max(0,Math.min(12,value));this.limitValue=value;
        if(measured>limit.ceiling+(limit.overshootAllowance??1)){this.limitExceededAt??=t;if(t-this.limitExceededAt>=(limit.tripDurationMillis??1000))return this.finish('secondary_limit_fault');}else this.limitExceededAt=null;
      }
      const target=activeControl(withTarget(phase.control,value),this.applied||this.lastController||(this.previousLegacy?.type!=='pump_pause'?this.previousLegacy:null)||p.phases.find(p=>p.control.type!=='pump_pause')?.control);this.decisions.push({elapsedMillis:t,phaseId:phase.id,requested,limited:target,previousApplied:this.applied});this.lastTime=t;return this.output(target,phase.id);
    }
    return this.finish('program_complete');
  }
  output(target,phaseId=null){const active=activeControl(target,this.applied||this.lastController||this.previousLegacy);this.lastController=active;return{target:active,phaseId,controlTargets:controlTargets(active)};}
  finish(reason){this.finished=reason;return{finish:reason};}
}
export function simulateProfile(profile,scenario,{fullTimeline=false}={}) {
  const duration=scenario.durationMillis??120000,step=scenario.stepMillis??100;
  if(!Number.isSafeInteger(step)||step<20||duration<0||duration>180000||duration/step>10000)throw new Error('Simulation duration/step is out of bounds');
  const input=scenario.samples||[];if(!input.length||input[0].elapsedMillis!==0||input.some((s,i)=>i&&s.elapsedMillis<=input[i-1].elapsedMillis))throw new Error('Sensor trace must start at zero and increase');
  const engine=new RecipeEvaluator(profile,{simulation:fullTimeline}),trace=[],pending=[];let sampleIndex=0;
  for(let t=0;t<=duration;t+=step){
    if(scenario.disconnectedAtMillis!=null&&t>=scenario.disconnectedAtMillis){trace.push({elapsedMillis:t,finish:'device_disconnected'});break;}
    while(sampleIndex+1<input.length&&input[sampleIndex+1].elapsedMillis<=t)sampleIndex++;
    const sample=input[sampleIndex],data={values:sample.values,sampledAtMillis:sample.sampledAtMillis||Object.fromEntries(Object.keys(sample.values).map(k=>[k,sample.elapsedMillis]))};
    if(t-sample.elapsedMillis>=2000){trace.push({elapsedMillis:t,finish:'telemetry_timeout'});break;}
    while(pending.length&&pending[0].at<=t)engine.acknowledgeApplied(pending.shift().target);
    try{const output=engine.tick(t,data);trace.push({elapsedMillis:t,...output,measurements:data.values});if(output.finish)break;
      if(output.target){const c=output.target,quantized=withTarget(c,Math.round(targetValue(c)*10)/10);pending.push({at:t+(scenario.acknowledgementDelayMillis||0),target:quantized});}
    }catch(error){trace.push({elapsedMillis:t,finish:error.message});break;}
  }
  return {trace,transitions:engine.transitions,captures:engine.captureEvents,decisions:engine.decisions,simulationOnly:true,fullTimeline};
}
export function syntheticScenario({flow=2,noise=0,staleAfterMillis=Infinity}={}) {
  const samples=[];for(let t=0;t<=120000&&t<=staleAfterMillis;t+=100){const pressure=Math.min(9,t/1800),f=(t<5500?8:flow)+noise*Math.sin(t/113);samples.push({elapsedMillis:t,values:{PumpPressure:Math.max(0,pressure+noise*Math.cos(t/157)),PumpFlow:Math.max(0,f),PumpedVolume:t/1000*flow,CupWeight:Math.max(0,t/1000-2)*flow*0.8}});}
  return{durationMillis:120000,stepMillis:100,samples};
}
