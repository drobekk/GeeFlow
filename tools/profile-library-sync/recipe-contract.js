const finite = (v, min = 0, max = Infinity) => typeof v === 'number' && Number.isFinite(v) && v >= min && v <= max;
const metrics = ['PumpPressure','GroupPressure','BoilerPressure','PumpFlow','PumpedVolume','CupWeight'];
export function validateExtensions(program) {
  const errors = [];
  if (!program || ![1,2].includes(program.version ?? 1)) return ['Unsupported program version.'];
  const v2 = program.version === 2;
  const allowed = ['type','version','phases','execution','recipe','recording','originalRecording','replayPolicy'];
  if (Object.keys(program).some(k => !allowed.includes(k))) errors.push('Unknown execution fields are unsupported.');
  const deadline = program.execution?.maximumTotalDurationMillis ?? 40000;
  if (!Number.isSafeInteger(deadline) || deadline < 1 || deadline > 40000) errors.push('Live total deadline must be at most 40 seconds.');
  if (!v2 && deadline !== 40000) errors.push('A custom deadline requires program v2.');
  if (!v2 && (program.recipe || program.originalRecording || program.replayPolicy && program.replayPolicy !== 'Legacy')) errors.push('Recipe extensions require program v2.');
  if (program.replayPolicy && !['Legacy','Hold','Linear'].includes(program.replayPolicy)) errors.push('Unsupported replay policy.');
  const captures = new Set();
  function source(s) {
    if (!s) return;
    if (!['Constant','MeasurementAtEntry','CapturedValue','PreviousGoal','PreviousApplied'].includes(s.kind)) errors.push('Unknown target source.');
    if (s.kind === 'Constant' && !finite(s.value)) errors.push('Invalid source constant.');
    if (s.kind === 'MeasurementAtEntry' && !metrics.includes(s.metric)) errors.push('Invalid source metric.');
    if (s.kind === 'CapturedValue' && !captures.has(s.capture)) errors.push(`Capture must precede use: ${s.capture}.`);
  }
  function actions(list = []) {
    for (const action of list) {
      if (!action.name?.trim() || captures.has(action.name)) errors.push('Capture names must be nonempty and unique.');
      if (!metrics.includes(action.metric) || !['Latest','Mean'].includes(action.filter || 'Mean')) errors.push('Invalid capture source/filter.');
      if (!Number.isSafeInteger(action.windowMillis ?? 500) || !finite(action.windowMillis ?? 500,0,10000) || !Number.isSafeInteger(action.maximumAgeMillis ?? 500) || !finite(action.maximumAgeMillis ?? 500,1,10000)) errors.push('Invalid capture timing.');
      if (!finite(action.minimum ?? 0) || !finite(action.maximum ?? 12, action.minimum ?? 0) || action.fallback != null && !finite(action.fallback,action.minimum ?? 0,action.maximum ?? 12)) errors.push('Invalid capture bounds/fallback.');
      captures.add(action.name);
    }
  }
  for (const phase of program.phases || []) {
    const extended = phase.targetSource || phase.ramp?.source || phase.entryCaptures?.length || phase.exitCaptures?.length || phase.secondaryLimit || phase.temperatureC != null || phase.stabilityDurationMillis;
    if (!v2 && extended) errors.push('Conditional targets, captures, limits and temperature require v2.');
    actions(phase.entryCaptures); source(phase.targetSource); source(phase.ramp?.source); actions(phase.exitCaptures);
    if (!Number.isSafeInteger(phase.stabilityDurationMillis || 0) || !finite(phase.stabilityDurationMillis || 0,0,phase.maximumDurationMillis)) errors.push('Invalid condition stability duration.');
    if (phase.temperatureC != null && !finite(phase.temperatureC,0,130)) errors.push('Invalid phase temperature.');
    const l = phase.secondaryLimit;
    if (l) {
      if (!((phase.control.type === 'flow' && l.metric === 'PumpPressure') || (phase.control.type === 'pressure' && l.metric === 'PumpFlow'))) errors.push('Secondary limit must use the opposing metric.');
      if (l.minimum != null && (!finite(l.minimum) || l.minimum >= l.ceiling)) errors.push('Range minimum must be below its maximum.');
      if (!finite(l.ceiling,Number.MIN_VALUE) || !finite(l.hysteresis ?? 0.2,0,l.ceiling) || (l.hysteresis ?? 0.2) >= l.ceiling || !finite(l.reductionPerSecond ?? 1,Number.MIN_VALUE) || !finite(l.recoveryPerSecond ?? 0.25) || !finite(l.overshootAllowance ?? 1) || !Number.isSafeInteger(l.tripDurationMillis ?? 1000) || !finite(l.tripDurationMillis ?? 1000,1,10000)) errors.push('Invalid secondary limit response.');
    }
  }
  const recipe = program.recipe;
  if (recipe) {
    if (!recipe.source?.original || !/^[a-f0-9]{64}$/i.test(recipe.source.sha256 || '') || !recipe.templateId || !Array.isArray(recipe.templatePhases)) errors.push('Recipe source, hash and template phases required.');
    if (recipe.settings && Object.values(recipe.settings).some(v=>v!=null&&!finite(v,Number.MIN_VALUE))) errors.push('Dose, ratio and yield must be positive.');
  }
  return errors;
}
export function upgradeProgram(program) {
  return { ...structuredClone(program), version: 2, execution: { maximumTotalDurationMillis: 40000, ...program.execution } };
}
export function preserveRecording(program) {
  const result = upgradeProgram(program);
  result.originalRecording ??= structuredClone(program.recording);
  result.replayPolicy ??= 'Legacy';
  return result;
}
export function configureYield(record, settings) {
  const next = structuredClone(record);
  if (!next.profile.program.recipe) throw new Error('A recipe template is required.');
  const value = settings.targetYieldG ?? (settings.doseG != null && settings.ratio != null ? settings.doseG * settings.ratio : null);
  if (!finite(value,Number.MIN_VALUE)) throw new Error('Choose a positive target yield or dose and ratio.');
  next.profile.program.recipe.settings = structuredClone(settings);
  next.profile.finishCondition = { type: 'weight', target: value };
  return next;
}
