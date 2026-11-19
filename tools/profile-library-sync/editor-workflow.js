import { deepCopy, effectivePhaseDurationMs, isVariablePhase } from './editor-core.js';
const equal = (a, b) => JSON.stringify(a) === JSON.stringify(b);
export class DraftHistory {
  constructor(record) { this.reset(record); }
  reset(record) { this.current = deepCopy(record); this.past = []; this.future = []; this.group = null; }
  record(record, group = null) {
    if (equal(record, this.current)) return;
    if (!group || group !== this.group) this.past.push(deepCopy(this.current));
    this.current = deepCopy(record); this.future = []; this.group = group;
  }
  undo() { if (!this.past.length) return deepCopy(this.current); this.future.push(this.current); this.current = this.past.pop(); this.group = null; return deepCopy(this.current); }
  redo() { if (!this.future.length) return deepCopy(this.current); this.past.push(this.current); this.current = this.future.pop(); this.group = null; return deepCopy(this.current); }
}
export function mergeRecords(base, local, remote) {
  const conflicts = [];
  function merge(b, l, r, path) {
    if (equal(l, b)) return r;
    if (equal(r, b) || equal(l, r)) return l;
    if (Array.isArray(b) && Array.isArray(l) && Array.isArray(r) &&
        b.every(x => x?.id) && equal(b.map(x => x.id), l.map(x => x.id)) && equal(b.map(x => x.id), r.map(x => x.id))) {
      return b.map((x, i) => merge(x, l[i], r[i], [...path, i]));
    }
    if ([b, l, r].every(x => x && typeof x === 'object' && !Array.isArray(x))) {
      return Object.fromEntries([...new Set([...Object.keys(b), ...Object.keys(l), ...Object.keys(r)])].map(k => [k, merge(b[k], l[k], r[k], [...path, k])]));
    }
    conflicts.push({ path, base: b, local: l, remote: r }); return l;
  }
  const result = merge(base, local, remote, []);
  return { result: deepCopy(result), conflicts };
}
export function resolveMerge(merge, choices) {
  const result = deepCopy(merge.result);
  for (let i = 0; i < merge.conflicts.length; i++) {
    if (!['local', 'remote'].includes(choices[i])) throw new Error('Choose a value for every overlapping change.');
    const conflict = merge.conflicts[i]; const path = conflict.path;
    let object = result; for (const key of path.slice(0, -1)) object = object[key];
    const value = conflict[choices[i]];
    if (!path.length) return deepCopy(value);
    if (value === undefined) delete object[path.at(-1)]; else object[path.at(-1)] = deepCopy(value);
  }
  return result;
}
export function operatePhase(program, index, operation, idFactory) {
  const result = deepCopy(program); const phases = result.phases;
  if (operation === 'duplicate') { const phase = deepCopy(phases[index]); phase.id = idFactory();
    const names = new Map();
    for (const action of [...(phase.entryCaptures || []), ...(phase.exitCaptures || [])]) { const old = action.name; action.name = `${old}_${phase.id.slice(0, 8)}`; names.set(old, action.name); }
    for (const source of [phase.targetSource, phase.ramp?.source]) if (source?.kind === 'CapturedValue' && names.has(source.capture)) source.capture = names.get(source.capture);
    phase.name = `${phase.name || 'Phase'} copy`; phases.splice(index + 1, 0, phase); }
  else if (operation === 'before' || operation === 'after') {
    const offset = operation === 'before' ? index : index + 1;
    phases.splice(offset, 0, { id: idFactory(), name: '', control: { type: 'pressure', bar: 0, location: 'Pump' }, maximumDurationMillis: 5000, minimumDurationMillis: 0, ramp: { style: 'Instant', durationMillis: 0, start: 'PreviousTarget' }, exitConditions: [], conditionOperator: 'Or' });
  } else if (operation === 'up' && index > 0) [phases[index - 1], phases[index]] = [phases[index], phases[index - 1]];
  else if (operation === 'down' && index < phases.length - 1) [phases[index + 1], phases[index]] = [phases[index], phases[index + 1]];
  else if (operation === 'remove') phases.splice(index, 1);
  return result;
}
export function resizePhase(program, index, durationMs) {
  if (!(durationMs > 0)) throw new Error('Duration must be positive.');
  const result = deepCopy(program); const phase = result.phases[index];
  if (isVariablePhase(phase)) throw new Error('Edit the conditional deadline in the inspector.');
  const old = effectivePhaseDurationMs(phase);
  phase.maximumDurationMillis = Math.round(durationMs);
  if (phase.exitConditions?.length === 1 && phase.exitConditions[0].metric === 'PhaseTime') phase.exitConditions[0].threshold = durationMs / 1000;
  if (phase.ramp.durationMillis === old) phase.ramp.durationMillis = Math.round(durationMs);
  phase.ramp.durationMillis = Math.min(phase.ramp.durationMillis, durationMs);
  if ((phase.minimumDurationMillis || 0) > durationMs) throw new Error('Duration is shorter than the minimum.');
  return result;
}
