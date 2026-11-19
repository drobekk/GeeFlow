import { createBlankRecord, deepCopy } from './editor-core.js';
import { configureYield } from './recipe-contract.js';

/** Source-preserving, explicitly labelled translation; never claims DE1 firmware equivalence. */
export async function importDecent(original, { id, url = '', adaptation = 'preserve', settings = {} } = {}) {
  if (!original || !Array.isArray(original.steps) || !original.steps.length || String(original.version) !== '2') throw new Error('Expected Decent JSON v2 with steps.');
  if (!['preserve','pressure-only','adaptive-capture'].includes(adaptation)) throw new Error('Unsupported import adaptation.');
  const canonical = JSON.stringify(original);
  const digest = await crypto.subtle.digest('SHA-256', new TextEncoder().encode(canonical));
  const sha256 = [...new Uint8Array(digest)].map(x=>x.toString(16).padStart(2,'0')).join('');
  const record = createBlankRecord(id || crypto.randomUUID());
  const diagnostics = ['Decent smooth transitions are translated as linear phase ramps. Firmware equivalence is unverified.', 'Source temperature and limiter data are preserved. Live support requires device verification.'];
  const phases = [];
  let previousZeroFrame = null;
  function numeric(value, field, fallback = null) {
    if ((value === '' || value == null) && fallback != null) return fallback;
    const n = Number(value); if (!Number.isFinite(n) || n < 0) throw new Error(`Invalid ${field}`); return n;
  }
  original.steps.forEach((step,index)=>{
    const ms = Math.round(numeric(step.seconds,'seconds') * 1000);
    if (!['pressure','flow'].includes(step.pump)) throw new Error(`Unsupported pump mode at source frame ${index+1}`);
    if (ms === 0) {
      diagnostics.push(`Source frame ${index+1} (${step.name || ''}) is a zero-duration transition, retained in the original source.`);
      previousZeroFrame = { step, index };
      return;
    }
    const mode = step.pump;
    const control = mode === 'pressure' ? {type:'pressure',bar:numeric(step.pressure,'pressure'),location:'Pump'} : {type:'flow',millilitresPerSecond:numeric(step.flow,'flow')};
    const phase = {id:`decent-${index+1}`,name:step.name || `Stage ${index+1}`,control,maximumDurationMillis:ms,minimumDurationMillis:0,
      ramp:{style:step.transition === 'smooth' ? 'Linear':'Instant',durationMillis:step.transition === 'smooth' ? ms:0,start:'PreviousTarget'},exitConditions:[],conditionOperator:'Or',temperatureC:numeric(step.temperature,'temperature')};
    if (step.exit && Number(step.exit.value)>0) {
      if (!['pressure','flow'].includes(step.exit.type) || !['over','under'].includes(step.exit.condition)) throw new Error(`Unsupported exit rule at source frame ${index+1}`);
      phase.exitConditions.push({metric:step.exit.type==='pressure'?'PumpPressure':'PumpFlow',comparison:step.exit.condition==='over'?'StrictlyAbove':'StrictlyBelow',threshold:numeric(step.exit.value,'exit threshold'),scope:'Total'});
    }
    for (const key of ['weight','volume']) if (Number(step[key])>0) diagnostics.push(`Source frame ${index+1} ${key}=${step[key]} is retained as source metadata; its DE1 semantics are not silently converted.`);
    const ceiling = Number(step.limiter?.value || 0);
    if (ceiling > 0) phase.secondaryLimit = {metric:mode==='flow'?'PumpPressure':'PumpFlow',ceiling,hysteresis:Math.min(0.2,ceiling/2),reductionPerSecond:1,recoveryPerSecond:0.25,overshootAllowance:1,tripDurationMillis:1000};
    if (ceiling > 0) diagnostics.push(`Source limiter.range=${step.limiter?.range || ''} is preserved. GeeFlow response parameters are separate simulation defaults, not a DE1 conversion.`);
    if (adaptation === 'adaptive-capture' && previousZeroFrame) {
      if (mode!=='flow' || phases.at(-1)?.control.type!=='pressure' || previousZeroFrame.step.pump!=='flow') throw new Error('Measured-flow handoff requires pressure → transition → flow.');
      phases.at(-1).exitCaptures=[{name:'extractionStartFlow',metric:'PumpFlow',windowMillis:500,maximumAgeMillis:500,filter:'Mean',minimum:0,maximum:12}];
      phase.ramp.source={kind:'CapturedValue',capture:'extractionStartFlow'};
      diagnostics.push('Explicit adaptation: capture filtered pump flow at pressurization exit and use it as extraction ramp start. Source transition frame was not executed as a fixed-flow reset.');
    } else if (previousZeroFrame) {
      diagnostics.push('Unresolved zero-duration transition prevents live equivalence. Select the explicit measured-flow adaptation for simulation.');
      // Keep the unresolved intent represented as unsupported thermal/controller metadata; no live claim.
    }
    if (adaptation === 'pressure-only') {
      if (mode !== 'pressure' || phase.secondaryLimit) throw new Error('Pressure-only adaptation cannot drop a required flow controller or limiter.');
      delete phase.temperatureC;
    }
    previousZeroFrame = null;
    phases.push(phase);
  });
  if (previousZeroFrame) throw new Error('Trailing zero-duration action cannot be translated without an explicit following phase.');
  if (!phases.length) throw new Error('No executable phases.');
  record.profile.name = original.title || 'Imported profile';
  record.profile.description = original.notes || '';
  record.profile.program = {type:'phases',version:2,execution:{maximumTotalDurationMillis:40000},phases,
    recipe:{templateId:`decent-${sha256.slice(0,16)}`,source:{format:'decent-json-v2',title:original.title || '',author:original.author || '',url,sha256,original:deepCopy(original)},templatePhases:deepCopy(phases),settings:{},diagnostics,adaptation,unresolvedSemantics:adaptation==='preserve'?['Source firmware transitions have not been explicitly adapted for this machine.']:[]}};
  const sourceWeight=Number(original.target_weight || 0);
  if (settings.targetYieldG != null || settings.doseG != null && settings.ratio != null) return configureYield(record,settings);
  if (sourceWeight>0) return configureYield(record,{targetYieldG:sourceWeight});
  diagnostics.push('Source output goal is unset. Choose a positive independent shot yield before execution.');
  return record;
}
