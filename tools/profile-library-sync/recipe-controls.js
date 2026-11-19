import { deepCopy, phaseWithTarget, targetValue } from './editor-core.js';
import { upgradeProgram } from './recipe-contract.js';

export const metricLabels = {PumpPressure:'Pressure',GroupPressure:'Group pressure',BoilerPressure:'Boiler pressure',PumpFlow:'Flow',PumpedVolume:'Volume',CupWeight:'Weight',PhaseTime:'Time'};
export const comparisons = {Above:'reaches',Below:'falls to',StrictlyAbove:'exceeds',StrictlyBelow:'falls below'};
const metricFor = phase => phase.control.type === 'flow' ? 'PumpFlow' : ({Pump:'PumpPressure',Group:'GroupPressure',Boiler:'BoilerPressure'}[phase.control.location || 'Pump']);
const unitFor = phase => phase.control.type === 'flow' ? 'ml/s' : 'bar';
const nameFor = (phase,index) => phase.name || `Phase ${index+1}`;

export function startOptions(program,index) {
  const phase=program.phases[index],metric=metricFor(phase),source=phase.ramp?.source;
  const options=[{value:'previous',label:'Previous phase target'},{value:'measured',label:`${metricLabels[metric]} at phase start`},{value:'fixed',label:'Fixed value'}];
  program.phases.slice(0,index).forEach((p,i)=>options.push({value:`end:${p.id}`,label:`${metricLabels[metric]} when ${nameFor(p,i)} ends`}));
  let selected=phase.ramp?.start==='CurrentMeasurement'?'measured':'previous';
  if(source){
    if(source.kind==='Constant')selected='fixed';
    else if(source.kind==='MeasurementAtEntry'&&source.metric===metric)selected='measured';
    else if(source.kind==='CapturedValue'){
      const earlier=program.phases.slice(0,index).find(p=>(p.exitCaptures||[]).some(c=>c.name===source.capture&&c.metric===metric));
      selected=earlier?`end:${earlier.id}`:'custom';
    }else selected='custom';
    if(selected==='custom') options.push({value:'custom',label:source.kind==='PreviousApplied'?'Last target sent':source.kind==='PreviousGoal'?'Previous phase goal':'Saved custom start'});
  }
  return {options,selected,unit:unitFor(phase)};
}

/** Existing captures and custom filter settings are retained, including when a reference is changed. */
export function changeStart(program,index,choice) {
  let next=deepCopy(program),phase=next.phases[index];
  if(choice==='custom'||choice===startOptions(program,index).selected)return next;
  if(choice==='previous'||choice==='measured'){
    // Preserve a pre-existing typed measurement source when the same choice is selected.
    if(choice==='measured'&&phase.ramp.source?.kind==='MeasurementAtEntry'&&phase.ramp.source.metric===metricFor(phase))return next;
    phase.ramp.source=null;phase.ramp.start=choice==='previous'?'PreviousTarget':'CurrentMeasurement';return next;
  }
  next=upgradeProgram(next);phase=next.phases[index];
  if(choice==='fixed'){phase.ramp.source={kind:'Constant',value:phase.ramp.source?.kind==='Constant'?phase.ramp.source.value:targetValue(phase.control)};return next;}
  if(!choice.startsWith('end:'))throw new Error('Choose a starting value.');
  const earlier=next.phases.slice(0,index).find(p=>p.id===choice.slice(4));
  if(!earlier)throw new Error('Choose an earlier phase.');
  const metric=metricFor(phase);
  let capture=(earlier.exitCaptures||[]).find(c=>c.metric===metric&&c.name===phase.ramp.source?.capture)||(earlier.exitCaptures||[]).find(c=>c.metric===metric);
  if(!capture){
    const used=new Set(next.phases.flatMap(p=>[...(p.entryCaptures||[]),...(p.exitCaptures||[])].map(c=>c.name)));
    let name=`end_${metric}_${earlier.id}`.replace(/[^a-zA-Z0-9_]/g,'_');while(used.has(name))name+='_';
    capture={name,metric,filter:'Mean',windowMillis:500,maximumAgeMillis:500,minimum:0,maximum:12};
    earlier.exitCaptures??=[];earlier.exitCaptures.push(capture);
  }
  phase.ramp.source={kind:'CapturedValue',capture:capture.name};return next;
}

export function changeTransition(program,index,transition) {
  const next=deepCopy(program),phase=next.phases[index];
  if(transition==='instant') {phase.ramp.style='Instant';phase.ramp.durationMillis=0;phase.ramp.source=null;}
  else if(phase.ramp.style==='Instant'){phase.ramp.style='Linear';phase.ramp.durationMillis=phase.maximumDurationMillis;}
  return next;
}

export function setPhaseGoal(program,index,value) {
  const next=deepCopy(program);next.phases[index]=phaseWithTarget(next.phases[index],value);return next;
}

export function changeLimit(program,index,value) {
  const next=value==null?deepCopy(program):upgradeProgram(program),phase=next.phases[index];
  if(value==null){phase.secondaryLimit=null;return next;}
  if(!Number.isFinite(value)||value<=0)throw new Error('Enter a positive maximum.');
  if(phase.secondaryLimit?.minimum!=null&&phase.secondaryLimit.minimum>=value)throw new Error('Maximum must exceed the minimum.');
  if(phase.control.type==='pump_pause')throw new Error('A pause has no pressure or flow limit.');
  phase.secondaryLimit={metric:phase.control.type==='flow'?'PumpPressure':'PumpFlow',ceiling:value,hysteresis:Math.min(0.2,value/2),reductionPerSecond:1,recoveryPerSecond:0.25,overshootAllowance:1,tripDurationMillis:1000,...phase.secondaryLimit,ceiling:value};
  return next;
}

export function changeFeedbackMode(program,index,mode) {
  if(!['off','limit','range'].includes(mode))throw new Error('Choose a feedback mode.');
  if(mode==='off')return changeLimit(program,index,null);
  const phase=program.phases[index],next=changeLimit(program,index,phase.secondaryLimit?.ceiling??(phase.control.type==='flow'?9:3.5));
  next.phases[index].secondaryLimit.minimum=mode==='range'?(phase.secondaryLimit?.minimum??0):null;
  return next;
}
export function changeRangeMinimum(program,index,minimum) {
  const next=upgradeProgram(program),limit=next.phases[index].secondaryLimit;
  if(!limit||!Number.isFinite(minimum)||minimum<0||minimum>=limit.ceiling)throw new Error('Minimum must be below the maximum.');
  limit.minimum=minimum;return next;
}
