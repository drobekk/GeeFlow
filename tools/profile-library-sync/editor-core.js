import { validateExtensions } from "./recipe-contract.js";
export const CONTROL_TYPES = ["pressure", "flow"];
export const RAMP_STYLES = ["Instant", "Linear", "EaseIn", "EaseOut", "EaseInOut"];
export const RAMP_STARTS = ["PreviousTarget", "CurrentMeasurement"];
export const CONDITION_METRICS = [
  "PumpPressure", "GroupPressure", "BoilerPressure", "PumpFlow", "PumpedVolume", "CupWeight", "PhaseTime",
];
export const CONDITION_COMPARISONS = ["Above", "Below", "StrictlyAbove", "StrictlyBelow"];
export const SAMPLE_LIMIT = 127;
export const CHANNELS = Object.freeze({
  pressure: { chart: "pressure", field: "pressure", mode: "pressure", unit: "bar" },
  flow: { chart: "flow", field: "flowRate", mode: "flow", unit: "ml/s" },
});
export function channelDefinition(name) {
  const channel = Object.values(CHANNELS).find(item => item.chart === name || item.field === name);
  if (!channel) throw new Error(`Unknown chart channel: ${name}`);
  return channel;
}
export const MIN_PHASE_DURATION_MS = 100;

export function deepCopy(value) {
  return JSON.parse(JSON.stringify(value));
}

export function createBlankRecord(id) {
  return {
    id,
    revision: 0,
    favorite: false,
    deleted: false,
    profile: {
      name: "",
      description: "",
      finishCondition: null,
      autoLinkOpen: false,
      singleDoseSettings: { enabled: false, grindingSize: 300, grindingSpeed: 500 },
      program: { type: "phases", version: 1, phases: [] },
    },
  };
}

export function cloneRecord(source, id) {
  const copy = deepCopy(source);
  copy.id = id;
  copy.revision = 0;
  copy.favorite = false;
  copy.deleted = false;
  copy.profile.name = `${source.profile.name} copy`;
  return copy;
}

export function effectivePhaseDurationMs(phase) {
  const max = Number(phase.maximumDurationMillis);
  const min = Number(phase.minimumDurationMillis || 0);
  const timeExit = phase.exitConditions?.length === 1 ? phase.exitConditions[0] : null;
  if (timeExit?.metric === "PhaseTime" && timeExit.comparison === "Above") {
    return Math.min(max, Math.max(min, Number(timeExit.threshold) * 1000));
  }
  return max;
}

export function isVariablePhase(phase) {
  const exits = phase.exitConditions || [];
  return Boolean(phase.adaptiveFlowProbe || phase.secondaryLimit) || (exits.length > 0 && !(exits.length === 1 && exits[0].metric === 'PhaseTime' && exits[0].comparison === 'Above'));
}

export function targetValue(control) {
  if (control?.type === "pressure") return Number(control.bar);
  if (control?.type === "flow") return Number(control.millilitresPerSecond);
  return 0;
}

/** A zero target still has one selected controller. Recorded measurements are never zeroed here. */
export function activeControl(control, previous = null) {
  if (control?.type === 'pump_pause') return previous?.type === 'flow' ? {...previous,millilitresPerSecond:0} : {type:'pressure',bar:0,location:previous?.location||'Pump'};
  if (!CONTROL_TYPES.includes(control?.type)) throw new Error('Select pressure or flow as the controller.');
  return control;
}
export function controlTargets(control, previous = null) {
  const active=activeControl(control,previous),value=targetValue(active);
  if(!Number.isFinite(value)||value<0)throw new Error('Invalid controller target.');
  return {controller:active.type,pressureBar:active.type==='pressure'?value:0,flowMillilitresPerSecond:active.type==='flow'?value:0};
}
export function canonicalPhases(phases) {
  let previous=phases.find(p=>CONTROL_TYPES.includes(p.control?.type))?.control||null;
  return deepCopy(phases).map(phase=>{
    if(phase.control?.type==='pump_pause'){
      phase.control=activeControl(phase.control,previous);phase.targetSource=null;
      phase.ramp={style:'Instant',durationMillis:0,start:'PreviousTarget'};phase.adaptiveFlowProbe=null;phase.secondaryLimit=null;
    }
    previous=phase.control;return phase;
  });
}

export function mapChartCoordinates(x, y, bounds, { durationMs, maxValue = 12 } = {}) {
  const width = Number(bounds.right) - Number(bounds.left);
  const height = Number(bounds.bottom) - Number(bounds.top);
  if (!(width > 0) || !(height > 0) || !(durationMs > 0) || !(maxValue > 0)) {
    throw new Error("Chart bounds must have a positive width, height, duration, and value range.");
  }
  const timeFraction = Math.min(1, Math.max(0, (Number(x) - Number(bounds.left)) / width));
  const valueFraction = Math.min(1, Math.max(0, (Number(bounds.bottom) - Number(y)) / height));
  return { timeMs: timeFraction * durationMs, value: valueFraction * maxValue };
}

export function withTarget(control, value) {
  if (control.type === "pressure") return { ...control, bar: Number(value) };
  if (control.type === "flow") return { ...control, millilitresPerSecond: Number(value) };
  return control;
}

export function phaseWithTarget(phase, value) {
  const next = deepCopy(phase);
  if (next.targetSource) {
    if (next.targetSource.kind !== 'Constant') throw new Error('Edit the computed target source in the inspector.');
    next.targetSource.value = Number(value);
  } else next.control = withTarget(next.control, value);
  return next;
}

export function rampFraction(ramp, elapsedMs) {
  if (!ramp || ramp.style === "Instant" || Number(ramp.durationMillis) === 0) return 1;
  const duration = Number(ramp.durationMillis);
  const t = Math.min(1, Math.max(0, Number(elapsedMs) / duration));
  switch (ramp.style) {
    case "Linear": return t;
    case "EaseIn": return t * t;
    case "EaseOut": return 1 - (1 - t) * (1 - t);
    case "EaseInOut": return t < 0.5 ? 2 * t * t : 1 - 2 * (1 - t) * (1 - t);
    default: return 1;
  }
}

export function projectPhases(program, { measurements = {} } = {}) {
  let offset = 0;
  let previous = null;
  let lastController=(program.phases||[]).find(p=>CONTROL_TYPES.includes(p.control?.type))?.control||null;
  return (program.phases || []).map((phase, index) => {
    const durationMs = effectivePhaseDurationMs(phase);
    const control = activeControl(phase.control,lastController);
    lastController=control;
    const endValue = phase.targetSource ? (phase.targetSource.kind === 'Constant' ? phase.targetSource.value : null) : targetValue(control);
    const sameControl = previous?.control?.type === control.type && (control.type !== "pressure" || (previous.control.location || "Pump") === (control.location || "Pump"));
    const startValue = control.type === "pump_pause" ? 0 :
      phase.ramp?.source ? (phase.ramp.source.kind === 'Constant' ? phase.ramp.source.value : phase.ramp.source.kind === 'MeasurementAtEntry' ? measurements[phase.ramp.source.metric] ?? measurements[control.type] ?? null : null) : phase.ramp?.start === "CurrentMeasurement" ? (measurements[control.type] ?? null) :
        sameControl ? targetValue(previous.control) : (measurements[control.type] ?? null);
    const segment = {
      index,
      phase,
      startMs: offset,
      endMs: offset + durationMs,
      durationMs,
      mode: control.type,
      startValue: phase.ramp?.style === "Instant" ? endValue : startValue,
      endValue,
      variable: isVariablePhase(phase),
      conditions: phase.exitConditions || [],
      unknownStart: (startValue == null && phase.ramp?.style !== "Instant") || endValue == null,
    };
    offset += durationMs;
    previous = phase;
    return segment;
  });
}

export function valueAtPhase(segment, absoluteTimeMs) {
  if (segment.mode === "pump_pause") return 0;
  if (segment.startValue == null || segment.endValue == null || segment.phase.adaptiveFlowProbe || segment.phase.secondaryLimit) return null;
  const elapsed = Math.min(segment.durationMs, Math.max(0, absoluteTimeMs - segment.startMs));
  const phase = segment.phase;
  return segment.startValue + (segment.endValue - segment.startValue) * rampFraction(phase.ramp, elapsed);
}

export function phaseIsSplittable(phase) {
  if (isVariablePhase(phase) || phase.targetSource || phase.ramp?.source || phase.entryCaptures?.length || phase.exitCaptures?.length) return false;
  return (phase.exitConditions || []).every((condition) => condition.metric === "PhaseTime");
}

function splitRamp(phase, splitOffsetMs) {
  const ramp = phase.ramp || { style: "Instant", durationMillis: 0, start: "PreviousTarget" };
  if (!["Instant", "Linear"].includes(ramp.style)) {
    throw new Error("Adding a point would change this eased curve. Convert to Linear explicitly first.");
  }
  const remaining = Math.max(0, Number(ramp.durationMillis) - splitOffsetMs);
  if (ramp.style === "Instant" || Number(ramp.durationMillis) === 0) {
    return [
      { style: "Instant", durationMillis: 0, start: ramp.start || "PreviousTarget" },
      { style: "Instant", durationMillis: 0, start: "PreviousTarget" },
    ];
  }
  return [
    {
      style: "Linear",
      durationMillis: Math.min(splitOffsetMs, Number(ramp.durationMillis)),
      start: ramp.start || "PreviousTarget",
    },
    {
      style: remaining > 0 ? "Linear" : "Instant",
      durationMillis: remaining,
      start: "PreviousTarget",
    },
  ];
}

export function insertPhasePoint(program, phaseIndex, absoluteTimeMs, value, idFactory, measurements = {}) {
  const phases = program.phases || [];
  const phase = phases[phaseIndex];
  if (!phase || !phaseIsSplittable(phase) || !["pressure", "flow"].includes(phase.control?.type)) {
    throw new Error("Points can only be added inside fixed-time pressure or flow phases.");
  }
  const segment = projectPhases(program, { measurements })[phaseIndex];
  if (valueAtPhase(segment, absoluteTimeMs) == null) throw new Error("This target depends on a measured start. Supply a simulation input before splitting it.");
  const offset = Math.round(absoluteTimeMs - segment.startMs);
  if (offset < MIN_PHASE_DURATION_MS || segment.durationMs - offset < MIN_PHASE_DURATION_MS) {
    throw new Error("A new point must leave at least 0.1 seconds on both sides.");
  }
  if (offset >= Number(phase.maximumDurationMillis)) {
    throw new Error("The point falls outside this phase's stored duration.");
  }

  const timeExits = (phase.exitConditions || []).map((condition) => ({
    ...condition,
    threshold: Math.max(0, Number(condition.threshold) - offset / 1000),
  }));
  const [firstRamp, secondRamp] = splitRamp(phase, offset);
  const first = {
    ...deepCopy(phase),
    id: idFactory(),
    name: "",
    control: withTarget(phase.control, value),
    maximumDurationMillis: offset,
    minimumDurationMillis: Math.min(Number(phase.minimumDurationMillis || 0), offset),
    ramp: firstRamp,
    exitConditions: [],
    conditionOperator: "Or",
    adaptiveFlowProbe: undefined,
  };
  const second = {
    ...deepCopy(phase),
    maximumDurationMillis: Number(phase.maximumDurationMillis) - offset,
    minimumDurationMillis: Math.max(0, Number(phase.minimumDurationMillis || 0) - offset),
    ramp: secondRamp,
    exitConditions: timeExits,
  };
  const updated = deepCopy(program);
  updated.phases.splice(phaseIndex, 1, first, second);
  return updated;
}

export function movePhasePoint(program, phaseIndex, { endMs, value, timeMode = "boundary" }) {
  const phases = program.phases || [];
  const phase = phases[phaseIndex];
  if (!phase) throw new Error("Select a phase point first.");
  const segments = projectPhases(program);
  const segment = segments[phaseIndex];
  const updated = deepCopy(program);
  const next = phases[phaseIndex + 1];

  if (value != null) updated.phases[phaseIndex] = phaseWithTarget(phase, value);
  if (endMs != null && Math.abs(endMs - segment.endMs) >= 1) {
    if (isVariablePhase(phase) || phase.adaptiveFlowProbe) {
      throw new Error("Keep this trigger boundary fixed; edit its duration or trigger in the inspector.");
    }
    const minimumDuration = Math.max(MIN_PHASE_DURATION_MS, Number(phase.minimumDurationMillis || 0));
    const newDuration = Math.round(endMs - segment.startMs);
    const delta = newDuration - segment.durationMs;
    if (newDuration < minimumDuration) throw new Error("The phase cannot end before its minimum duration.");
    setPlannedPhaseDuration(updated.phases[phaseIndex], newDuration);
    updated.phases[phaseIndex].minimumDurationMillis = Math.min(
      Number(phase.minimumDurationMillis || 0), newDuration,
    );
    if (timeMode === "boundary" && next && isVariablePhase(next)) {
      throw new Error("Use Resize phase to shift a following conditional phase without changing its rules.");
    }
    if (timeMode === "boundary" && next) {
      const nextDuration = effectivePhaseDurationMs(next) - delta;
      const nextMinimum = Math.max(MIN_PHASE_DURATION_MS, Number(next.minimumDurationMillis || 0));
      if (nextDuration < nextMinimum) {
        throw new Error("There is not enough time between neighboring points.");
      }
      setPlannedPhaseDuration(updated.phases[phaseIndex + 1], nextDuration);
      updated.phases[phaseIndex + 1].ramp.durationMillis = Math.min(
        Number(next.ramp?.durationMillis || 0) === effectivePhaseDurationMs(next) ? nextDuration : Number(next.ramp?.durationMillis || 0), nextDuration,
      );
    }
    updated.phases[phaseIndex].ramp.durationMillis = Math.min(
      Number(phase.ramp?.durationMillis || 0) === segment.durationMs ? newDuration : Number(phase.ramp?.durationMillis || 0), newDuration,
    );
  }
  return updated;
}

export function removePhasePoint(program, phaseIndex) {
  const phases = program.phases || [];
  const phase = phases[phaseIndex];
  const previous = phases[phaseIndex - 1];
  const next = phases[phaseIndex + 1];
  if (!phase || !previous || !next) throw new Error("Only interior control points can be removed.");
  if ([phase, previous, next].some((item) => isVariablePhase(item) || item.adaptiveFlowProbe)) {
    throw new Error("Triggered phases keep their points so their exit rules remain unchanged.");
  }
  if (phase.control?.type !== previous.control?.type || phase.control?.type !== next.control?.type) {
    throw new Error("A control-mode transition cannot be removed as a curve point.");
  }
  const updated = deepCopy(program);
  const removedDuration = effectivePhaseDurationMs(phase);
  const nextPhase = updated.phases[phaseIndex + 1];
  const mergedDuration = effectivePhaseDurationMs(nextPhase) + removedDuration;
  nextPhase.maximumDurationMillis = Number(nextPhase.maximumDurationMillis) + removedDuration;
  if (Number(nextPhase.minimumDurationMillis || 0) > 0) {
    nextPhase.minimumDurationMillis = Number(nextPhase.minimumDurationMillis) + removedDuration;
  }
  setPlannedPhaseDuration(nextPhase, mergedDuration);
  nextPhase.ramp = {
    style: "Linear",
    durationMillis: mergedDuration,
    start: "PreviousTarget",
  };
  updated.phases.splice(phaseIndex, 1);
  return updated;
}

function setPlannedPhaseDuration(phase, durationMs) {
  const condition = phase.exitConditions?.length === 1 ? phase.exitConditions[0] : null;
  if (condition?.metric === "PhaseTime" && condition.comparison === "Above") {
    phase.maximumDurationMillis = Math.max(Number(phase.maximumDurationMillis), durationMs);
    condition.threshold = durationMs / 1000;
  } else {
    phase.maximumDurationMillis = durationMs;
  }
}

export function interpolateSample(recording, elapsedMillis) {
  const samples = recording.samples || [];
  if (!samples.length) throw new Error("Add the first sample at time 0 before editing this recording.");
  const time = Math.max(0, Math.round(Number(elapsedMillis)));
  const exact = samples.find((sample) => sample.elapsedMillis === time);
  if (exact) return deepCopy(exact);
  const rightIndex = samples.findIndex((sample) => sample.elapsedMillis > time);
  if (rightIndex < 0) {
    return { elapsedMillis: time, data: deepCopy(samples[samples.length - 1].data) };
  }
  const left = samples[Math.max(0, rightIndex - 1)];
  const right = samples[rightIndex];
  const fraction = (time - left.elapsedMillis) / (right.elapsedMillis - left.elapsedMillis);
  const data = {};
  for (const key of ["pressure", "flowRate", "volume", "weight", "weightRate"]) {
    data[key] = Number(left.data[key]) + (Number(right.data[key]) - Number(left.data[key])) * fraction;
  }
  return { elapsedMillis: time, data };
}

export function insertRecordingSample(recording, elapsedMillis, limit = Infinity) {
  const time = Math.max(0, Math.round(Number(elapsedMillis)));
  if (time === 0) throw new Error("The first recording sample stays anchored at time 0.");
  if (recording.samples.some((sample) => sample.elapsedMillis === time)) {
    throw new Error("There is already a sample at that time.");
  }
  if (recording.samples.length >= limit) throw new Error(`Recordings are limited to ${limit} samples.`);
  const updated = deepCopy(recording);
  updated.samples.push(interpolateSample(recording, time));
  updated.samples.sort((a, b) => a.elapsedMillis - b.elapsedMillis);
  return updated;
}

export function moveRecordingSample(recording, sampleIndex, { elapsedMillis, channel, value }) {
  const updated = deepCopy(recording);
  const sample = updated.samples[sampleIndex];
  if (!sample) throw new Error("Select a recording point first.");
  if (channel && ["pressure", "flowRate"].includes(channel)) sample.data[channel] = Number(value);
  if (elapsedMillis != null) {
    if (sampleIndex === 0) {
      if (Math.round(elapsedMillis) !== 0) throw new Error("The first recording sample stays anchored at time 0.");
      return updated;
    }
    const lower = updated.samples[sampleIndex - 1].elapsedMillis + 1;
    const upper = sampleIndex + 1 < updated.samples.length
      ? updated.samples[sampleIndex + 1].elapsedMillis - 1
      : Number.MAX_SAFE_INTEGER;
    sample.elapsedMillis = Math.min(upper, Math.max(lower, Math.round(elapsedMillis)));
  }
  return updated;
}

export function removeRecordingSample(recording, sampleIndex) {
  if (sampleIndex === 0) throw new Error("The first recording sample stays anchored at time 0.");
  if (!recording.samples[sampleIndex]) throw new Error("Select a recording point first.");
  const updated = deepCopy(recording);
  updated.samples.splice(sampleIndex, 1);
  return updated;
}

function finiteNonNegative(value) {
  return Number.isFinite(Number(value)) && Number(value) >= 0;
}

export function validateProfile(profile) {
  const errors = [];
  if (!profile || typeof profile !== "object") return ["Profile data is missing."];
  if (typeof profile.name !== "string" || !profile.name.trim()) errors.push("Name is required.");
  if (typeof profile.description !== "string") errors.push("Description must be text.");
  if (typeof profile.autoLinkOpen !== "boolean") errors.push("Auto-link setting is invalid.");
  const dose = profile.singleDoseSettings;
  if (!dose || typeof dose.enabled !== "boolean" || !Number.isInteger(dose.grindingSize) ||
      dose.grindingSize < 0 || dose.grindingSize > 600 || !Number.isInteger(dose.grindingSpeed) ||
      dose.grindingSpeed < 200 || dose.grindingSpeed > 1000) {
    errors.push("Single-dose settings are invalid.");
  }
  if (profile.finishCondition && (!['weight', 'volume'].includes(profile.finishCondition.type) ||
      (!finiteNonNegative(profile.finishCondition.target) || Number(profile.finishCondition.target) === 0))) errors.push("Finish condition is invalid.");
  errors.push(...validateProgram(profile.program));
  return errors;
}

export function validateProgram(program) {
  if (!program || ![1, 2].includes(program.version ?? 1)) return ["Program version must be 1 or 2."];
  const extensionErrors = validateExtensions(program);
  if (program.type === "phases") {
    const phases = program.phases;
    if (!Array.isArray(phases) || phases.length === 0) return ["Add at least one phase before saving."];
    const errors = [...extensionErrors];
    const ids = new Set();
    phases.forEach((phase, index) => {
      const label = `Phase ${index + 1}`;
      if (!phase.id || ids.has(phase.id)) errors.push(`${label} needs a unique ID.`);
      ids.add(phase.id);
      const max = Number(phase.maximumDurationMillis);
      const min = Number(phase.minimumDurationMillis || 0);
      if (!Number.isSafeInteger(max) || !(max > 0) || !Number.isSafeInteger(min) || min < 0 || min > max) errors.push(`${label} duration is invalid.`);
      const rampDuration = Number(phase.ramp?.durationMillis || 0);
      if (!Number.isSafeInteger(rampDuration) || rampDuration < 0 || rampDuration > max || !RAMP_STYLES.includes(phase.ramp?.style || "Instant") ||
          !RAMP_STARTS.includes(phase.ramp?.start || "PreviousTarget")) errors.push(`${label} ramp is invalid.`);
      const control = phase.control;
      if (!control || !CONTROL_TYPES.includes(control.type) && control.type !== "pump_pause") errors.push(`${label} control mode is invalid.`);
      else if (control.type === "pressure" && (!finiteNonNegative(control.bar))) errors.push(`${label} pressure must be between 0 and 12 bar.`);
      else if (control.type === "flow" && (!finiteNonNegative(control.millilitresPerSecond))) errors.push(`${label} flow must be between 0 and 12 ml/s.`);
      if(control && Object.keys(control).some(key=>!(control.type==='pressure'?['type','bar','location']:control.type==='flow'?['type','millilitresPerSecond']:['type']).includes(key))) errors.push(`${label} must have exactly one controller.`);
      if (control?.type === "pump_pause" && (phase.ramp?.style || "Instant") !== "Instant") errors.push(`${label} pause phases cannot ramp.`);
      if (phase.adaptiveFlowProbe) {
        const probe = phase.adaptiveFlowProbe;
        if (control?.type !== "flow" || !Number.isSafeInteger(Number(probe.samplingDurationMillis)) || Number(probe.samplingDurationMillis) < 1 ||
            Number(probe.samplingDurationMillis) > max || !finiteNonNegative(probe.pressureCeilingBar) ||
            Number(probe.pressureCeilingBar) === 0 || Number(probe.pressureCeilingBar) > 12 || !finiteNonNegative(probe.flowBackoffMillilitresPerSecond) || Number(probe.flowBackoffMillilitresPerSecond) > 12) {
          errors.push(`${label} adaptive flow probe is invalid.`);
        }
        if ((phase.ramp?.style || "Instant") === "Instant" || Number(phase.ramp?.durationMillis || 0) < Number(probe.samplingDurationMillis)) {
          errors.push(`${label} adaptive probe ramp must cover the sampling window.`);
        }
      }
      for (const condition of phase.exitConditions || []) {
        if (!CONDITION_METRICS.includes(condition.metric) || !CONDITION_COMPARISONS.includes(condition.comparison) ||
            !finiteNonNegative(condition.threshold)) errors.push(`${label} has an invalid exit condition.`);
        if (condition.scope === "Step" && !["PumpedVolume", "CupWeight"].includes(condition.metric)) {
          errors.push(`${label} has an unsupported step-scoped condition.`);
        }
      }
    });
    return errors;
  }
  if (program.type === "recording") {
    const recording = program.recording;
    if (!recording || !["Pressure", "Flow"].includes(recording.controlMode) || !Array.isArray(recording.samples) ||
        recording.samples.length === 0) return ["Recording needs a control mode and at least one sample."];
    const errors = [...extensionErrors];
    if (program.originalRecording) errors.push(...validateProgram({ type: "recording", version: 1, recording: program.originalRecording }));
    if (recording.samples[0].elapsedMillis !== 0) errors.push("The first recording sample must start at 0 seconds.");
    for (let i = 0; i < recording.samples.length; i++) {
      const sample = recording.samples[i];
      if (!Number.isSafeInteger(sample.elapsedMillis) || sample.elapsedMillis < 0) {
        errors.push(`Recording sample ${i + 1} time must be a non-negative whole millisecond.`);
        break;
      }
      if (i > 0 && sample.elapsedMillis <= recording.samples[i - 1].elapsedMillis) {
        errors.push("Recording sample times must increase strictly.");
        break;
      }
      if (["pressure", "flowRate", "volume", "weight", "weightRate"].some((key) => !finiteNonNegative(sample.data?.[key]))) {
        errors.push(`Recording sample ${i + 1} contains an invalid measurement.`);
        break;
      }
    }
    return errors;
  }
  return ["Program type must be phases or recording."];
}


export function phaseCurvePoints(segment) {
  if (segment.unknownStart || segment.phase.adaptiveFlowProbe || segment.phase.secondaryLimit) return [];
  const rampEnd = Math.min(segment.durationMs, Number(segment.phase.ramp?.durationMillis || 0));
  const times = new Set([0, rampEnd, segment.durationMs]);
  if (["EaseIn", "EaseOut", "EaseInOut"].includes(segment.phase.ramp?.style)) {
    for (let i = 1; i < 32; i++) times.add(rampEnd * i / 32);
  }
  return [...times].sort((a, b) => a - b).map(t => [segment.startMs + t, valueAtPhase(segment, segment.startMs + t)]);
}

export function recordingReplayPoints(recording, route = "app", policy = "Legacy") {
  const samples = recording.samples || [];
  if (!samples.length) return [];
  const key = recording.controlMode === "Pressure" ? "pressure" : "flowRate";
  if (route === "native") {
    const count = Math.floor(samples.at(-1).elapsedMillis / 500 + 0.5) + 1;
    return Array.from({ length: count }, (_, index) => {
      const nearest = index === count - 1 ? samples.at(-1) : samples.reduce((a, b) =>
        Math.abs(b.elapsedMillis - index * 500) < Math.abs(a.elapsedMillis - index * 500) ? b : a);
      return [index * 500, nearest.data[key]];
    });
  }
  if (policy === "Linear") return samples.map(sample => [sample.elapsedMillis, sample.data[key]]);
  const result = [[samples[0].elapsedMillis, samples[0].data[key]]];
  for (let i = 1; i < samples.length; i++) {
    result.push([samples[i].elapsedMillis, samples[i - 1].data[key]], [samples[i].elapsedMillis, samples[i].data[key]]);
  }
  return result;
}

export function describePhaseRules(phase) {
  const rules = (phase.exitConditions || []).map(c => `${c.scope === "Step" ? "phase " : ""}${c.metric} ${{Above:"≥",Below:"≤",StrictlyAbove:">",StrictlyBelow:"<"}[c.comparison]} ${c.threshold}`);
  const measurements = rules.filter((_, i) => phase.exitConditions[i].metric !== "PhaseTime");
  const deadlines = rules.filter((_, i) => phase.exitConditions[i].metric === "PhaseTime");
  return `After at least ${(phase.minimumDurationMillis || 0) / 1000}s: ${[measurements.join(phase.conditionOperator === "And" ? " AND " : " OR "), ...deadlines].filter(Boolean).join(" OR ") || "no measurement exit"}; maximum ${phase.maximumDurationMillis / 1000}s.`;
}

export function assessCompatibility(profile, model = "LitaBA") {
  const issues = [];
  const program = profile.program;
  const phases = program?.phases || [];
  const flowSupported = !["LitaBR", "LitaLR"].includes(model);
  for (const phase of phases) {
    if (phase.control.type === "pressure" && (phase.control.location || "Pump") !== "Pump") issues.push("Only pump pressure is supported on this LITA route.");
    if (phase.control.type === "flow" && !flowSupported) issues.push(`${model} does not support flow control.`);
    if (targetValue(phase.control) > 12) issues.push("Target exceeds this machine's 12-unit limit.");
  }
  if (program?.recording?.controlMode === "Flow" && !flowSupported) issues.push(`${model} does not support flow replay.`);
  if (program?.recording?.samples.some(s => s.data[program.recording.controlMode === 'Flow' ? 'flowRate' : 'pressure'] > 12)) issues.push("Replay target exceeds this machine's 12-unit limit.");
  const nativeCount = program?.recording ? Math.floor(program.recording.samples.at(-1)?.elapsedMillis / 500 + 0.5) + 1 : 0;
  if (program?.recipe?.unresolvedSemantics?.length) issues.push(...program.recipe.unresolvedSemantics);
  if (program?.recipe && !profile.finishCondition) issues.push('Imported recipes require a positive shot finish configuration.');
  if (phases.some(p => p.secondaryLimit)) issues.push("Secondary limiter: simulation only until device strategy is verified.");
  if (phases.some(p => p.temperatureC != null)) issues.push("Per-phase temperature is preserved but unsupported on this route.");
  if (program?.version === 2 && (profile.finishCondition?.type === "weight" || phases.some(p => [...(p.exitConditions || []), ...(p.entryCaptures || []), ...(p.exitCaptures || []), p.targetSource || {}, p.ramp?.source || {}].some(x => x.metric === "CupWeight")))) issues.push("V2 weight rules require a verified timestamped scale route.");
  const native = program?.version !== 2 && Boolean(profile.finishCondition) && (program?.type === "recording" ? nativeCount <= 127 : phases.every((phase, i) =>
    !phase.adaptiveFlowProbe && (phase.ramp?.style || "Instant") === "Instant" && !(phase.minimumDurationMillis || 0) &&
    phase.maximumDurationMillis % 1000 === 0 && effectivePhaseDurationMs(phase) >= 1000 &&
    (!(phase.exitConditions || []).length || (phase.exitConditions.length === 1 && phase.exitConditions[0].metric === "PhaseTime" && phase.exitConditions[0].comparison === "Above" && phase.exitConditions[0].threshold % 1 === 0)) &&
    (phase.control.type !== "pump_pause" || (i > 0 && i < phases.length - 1 && phases[i - 1].control.type !== "pump_pause"))));
  return { route: issues.length ? "unsupported" : native ? "native" : "app", issues: [...new Set(issues)], nativeCount,
    label: issues.length ? "Unsupported on selected machine" : native ? "Native upload · code support" : "App-controlled · foreground + Bluetooth required" };
}

export function normalizeRecord(record) {
  const r = deepCopy(record), p = r.profile;
  r.revision ??= 0; r.favorite ??= false; r.deleted ??= false;
  p.finishCondition ??= null; p.autoLinkOpen ??= false;
  p.singleDoseSettings = { enabled:false, grindingSize:300, grindingSpeed:500, ...p.singleDoseSettings };
  p.program.version ??= 1;
  if(p.program.phases)p.program.phases=canonicalPhases(p.program.phases);
  if(p.program.recipe?.templatePhases)p.program.recipe.templatePhases=canonicalPhases(p.program.recipe.templatePhases);
  for (const phase of p.program.phases || []) {
    phase.name ??= ''; phase.minimumDurationMillis ??= 0;
    phase.ramp = { style:'Instant',durationMillis:0,start:'PreviousTarget',...phase.ramp };
    phase.exitConditions ??= [];phase.conditionOperator ??= 'Or';
  }
  return r;
}
