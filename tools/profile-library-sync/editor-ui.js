import {
  phaseWithTarget, normalizeRecord, channelDefinition, phaseCurvePoints, recordingReplayPoints, describePhaseRules, assessCompatibility,
  CONDITION_COMPARISONS,
  CONDITION_METRICS,
  CONTROL_TYPES,
  RAMP_STARTS,
  RAMP_STYLES,
  cloneRecord,
  createBlankRecord,
  deepCopy,
  effectivePhaseDurationMs,
  insertPhasePoint,
  insertRecordingSample,
  isVariablePhase,
  mapChartCoordinates,
  movePhasePoint,
  moveRecordingSample,
  projectPhases,
  removePhasePoint,
  removeRecordingSample,
  targetValue,
  validateProfile,
  valueAtPhase,
  withTarget,
} from "./editor-core.js";

import { DraftHistory, mergeRecords, resolveMerge, operatePhase } from "./editor-workflow.js";

import { upgradeProgram, preserveRecording, configureYield } from './recipe-contract.js';
import { importDecent } from './decent-import.js';
import { simulateProfile, syntheticScenario } from './recipe-runtime.js';
import { renderRecipeFields } from './recipe-inspector.js';
import { startOptions, changeStart, changeTransition, setPhaseGoal, changeLimit, changeFeedbackMode, changeRangeMinimum, metricLabels, comparisons } from './recipe-controls.js';

const API_URL = new URL("api/v1/catalog", document.baseURI);
const DATABASE_API_URL = new URL("api/local-db/", document.baseURI);
const SVG_WIDTH = 760;
const SVG_HEIGHT = 196;
const PLOT = { left: 48, right: 738, top: 15, bottom: 155 };
const Y_MAX = 12;
const $ = (selector) => document.querySelector(selector);
const refs = {
  token: $("#token"), load: $("#load"), status: $("#status"), accessCard: $("#access-card"), workspace: $("#workspace"),
  catalogSummary: $("#catalog-summary"), profiles: $("#profiles"), search: $("#search"),
  newProfile: $("#new-profile"), editor: $("#editor"), editorEmpty: $("#editor-empty"),
  editorHeading: $("#editor-heading"), editorSubtitle: $("#editor-subtitle"),
  profileFields: $("#profile-fields"), profileSettings: $("#profile-settings"),
  phaseList: $("#phase-list"), phaseListTitle: $("#program-list-title"), phaseListHint: $("#program-list-hint"),
  inspector: $("#inspector"), addPhase: $("#add-phase"), clone: $("#clone-profile"),
  discard: $("#discard"), save: $("#save"), editorStatus: $("#editor-status"),
  conflict: $("#conflict"), useLatest: $("#use-latest"), pressureChart: $("#pressure-chart"),
  flowChart: $("#flow-chart"), chartFooter: $("#chart-footer"),
  openAppDatabase: $("#open-app-database"), downloadAppDatabase: $("#download-app-database"), closeAppDatabase: $("#close-app-database"),
  appDatabaseFile: $("#app-database-file"), databaseBanner: $("#database-banner"), databaseMessage: $("#database-message"),
};

let bearerToken = "";
let catalog = null;
let draft = null;
let baseline = null;
let selectedId = null;
let selectedPhaseIndex = -1;
let selectedSampleIndex = -1;
let dirty = false;
let isNewDraft = false;
let conflictCatalog = null;
let conflictNeedsReload = false;
let pendingDeleteId = null;
let drag = null;
let history = null;
let editGroup = null;
let pendingMerge = null;
let simulationResult = null;
let importedScenario = null;
let selectedPoint = null;
let localDatabase = null;
let localDatabaseEditorAvailable = false;
function clearSimulation(message = '') {
  simulationResult = null;
  $('#simulation-summary').textContent = message;
  $('#simulation-events').replaceChildren();
  $('#simulation-trace').textContent = '';
}
function preserveReplaySource() { draft.profile.program = preserveRecording(program()); }
function programV2() { draft.profile.program = upgradeProgram(program()); }
function litaCandidate(record = draft) {
  const candidate = deepCopy(record);
  candidate.profile.program.version = 1;
  return candidate;
}
function updateExecutionRouteControl() {
  const control = $('#execution-route');
  if (!control || !draft) return;
  const machine = $('#machine-model').value;
  const current = assessCompatibility(draft.profile, machine);
  control.value = current.route === 'native' ? 'lita' : 'app';
  const candidate = litaCandidate();
  const errors = validateProfile(candidate.profile);
  const assessment = errors.length ? { route: 'unsupported', issues: errors } : assessCompatibility(candidate.profile, machine);
  const litaOption = $('#execution-route-lita');
  litaOption.disabled = assessment.route !== 'native';
  const reason = candidate.profile.program.recipe
    ? 'Imported recipe data needs GeeFlow app control.'
    : assessment.issues[0] === 'A custom deadline requires program v2.'
      ? 'A custom GeeFlow time limit cannot be sent to LITA.'
      : assessment.issues[0] || 'This program needs GeeFlow app control.';
  $('#execution-route-help').textContent = assessment.route === 'native'
    ? 'GeeFlow uploads this profile to LITA, which runs it from the machine.'
    : `LITA cannot run this profile: ${reason}`;
}
function setExecutionRoute(value) {
  if (value === 'app') {
    if (program().version !== 2) programV2();
  } else if (value === 'lita') {
    const candidate = litaCandidate();
    const errors = validateProfile(candidate.profile);
    if (errors.length) throw new Error(`This program needs GeeFlow app control. ${errors[0]}`);
    const assessment = assessCompatibility(candidate.profile, $('#machine-model').value);
    if (assessment.route !== 'native') throw new Error(assessment.issues.join(' ') || 'This program cannot run on LITA.');
    draft = candidate;
  } else {
    throw new Error('Select GeeFlow app or LITA machine.');
  }
  markDirty();
  renderAll();
}
function availablePhase() { return program()?.phases?.[selectedPhaseIndex]; }
function renderRecipeSummary() {
  const recipe = program()?.recipe, finish=draft?.profile.finishCondition, settings=recipe?.settings || {};
  $('#recipe-summary').innerHTML = recipe ? `<strong>${escapeHtml(recipe.source.title)}</strong><span>${escapeHtml(recipe.source.author)}</span><details><summary>Import details</summary><ul>${recipe.diagnostics.map(x=>`<li>${escapeHtml(x)}</li>`).join('')}</ul><p>SHA-256 ${escapeHtml(recipe.source.sha256)}</p></details>` : '<p>Created in GeeFlow.</p>';
  $('#doseG').value=settings.doseG??'';$('#ratio').value=settings.ratio??'';
  $('#doseG').setCustomValidity('');$('#targetYieldG').setCustomValidity('');
  $('#targetYieldG').value=finish?.target??'';
  $('#targetYieldG').disabled=false;
  $('#dose-wrap').classList.toggle('hidden',!recipe || finish?.type==='volume');
  $('#yield-arrow').classList.toggle('hidden',!recipe || finish?.type==='volume');
  $('#ratio-toggle').classList.toggle('hidden',!recipe || finish?.type==='volume');
  const ratio=settings.doseG&&finish?.target?finish.target/settings.doseG:settings.ratio;
  $('#ratio-toggle').textContent=ratio?`1:${numberText(ratio,2)}`:'Ratio';
  $('#yield-label').textContent=finish?.type==='volume'?'Volume':'Yield';
  $('#yield-unit').textContent=finish?.type==='volume'?'ml':'g';
  $('#targetYieldG').setAttribute('aria-label', finish?.type==='volume' ? 'Pumped volume target (ml)' : finish?.type==='weight' ? 'Cup weight target (g)' : 'Finish target');
  $('#ratio-status').textContent=settings.doseG?'Changing the ratio updates yield.':'Enter a coffee dose first.';
  $('#deadline-seconds').value=(program()?.execution?.maximumTotalDurationMillis??40000)/1000;
  $('#finish-mode').value=finish?.type||'none';
  updateExecutionRouteControl();
  $('#profile-description').value=draft?.profile.description||'';
  $('#replay-policy').value=program()?.replayPolicy||'Legacy';
  $('#telemetry-toggle').classList.toggle('hidden',phasesProgram());
}

function overlaySimulation(channel, durationMs) {
  if (!simulationResult || !$('#preview-panel').open || $('#timeline-view').value !== 'overview') return '';
  const paths=[]; let points=[],lastPhase=null;
  function flush(){if(points.length)paths.push(`<path d="${pathFromPoints(points)}" fill="none" stroke="${channel==='pressure'?'#9c2f13':'#075f81'}" stroke-width="2"/>`);points=[];}
  for (const row of simulationResult.trace) {
    if (!row.target || row.target.type !== channel) { flush(); lastPhase=null; continue; }
    if(lastPhase!==row.phaseId){flush();lastPhase=row.phaseId;}
    points.push([xScale(row.elapsedMillis,durationMs),yScale(targetValue(row.target))]);
  }
  flush(); return paths.join('');
}
const DRAFT_KEY = "geeflow.profileDraft.v1";
let recoveredDraft = null;
try { recoveredDraft = JSON.parse(localStorage.getItem(DRAFT_KEY)); } catch { /* Unavailable storage is optional. */ }
function persistDraft() {
  if (localDatabase) return;
  try { if (draft && dirty) localStorage.setItem(DRAFT_KEY, JSON.stringify({ record: draft, baseline, isNewDraft })); } catch { setEditorStatus("Draft recovery unavailable. Export a copy to keep it."); }
}
function clearRecovery() { if (localDatabase) return; try { localStorage.removeItem(DRAFT_KEY); } catch {} recoveredDraft = null; }
function projectCurrent() {
  const measurements = {};
  for (const input of document.querySelectorAll('[data-measured-start]')) if (input.value !== "") measurements[input.dataset.measuredStart] = Number(input.value);
  const segments = projectPhases(program(), { measurements });
  if ($('#timeline-view')?.value === 'phase') {
    const selected = segments[selectedPhaseIndex];
    return selected ? [{ ...selected, endMs: selected.durationMs, startMs: 0 }] : [];
  }
  return segments;
}
function updateWorkflowState() {
  $('#undo').disabled=Boolean(pendingMerge)||!history?.past.length;$('#redo').disabled=Boolean(pendingMerge)||!history?.future.length;
  $('#recover-draft').disabled=!recoveredDraft;
  $('#recovery-banner').classList.toggle('hidden',!recoveredDraft || Boolean(localDatabase));
  if(!draft)return;
  updateExecutionRouteControl();
  const assessment=assessCompatibility(draft.profile,$('#machine-model').value);
  const machine=$('#machine-model').selectedOptions[0].textContent;
  $('#compatibility').textContent=assessment.route==='unsupported'?`Preview only · ${machine}`:assessment.route==='native'?`On machine · ${machine}`:`App controlled · ${machine}`;
  $('#compatibility').classList.toggle('warning',assessment.route==='unsupported');
  const reasons=assessment.issues.map(x=>x.includes('Secondary limiter')?'Pressure/flow limits need device validation.':x.includes('temperature')?'Per-phase temperature is unavailable on this machine.':x.includes('timestamped scale')?'Adaptive weight rules need timestamped scale support.':x.includes('positive shot finish')?'Set a stop weight or volume in Recipe settings.':x);
  $('#compatibility-details').textContent=reasons.join(' ') || (assessment.route==='app'?'Keep GeeFlow open and connected during the shot.':'Runs on the machine after upload.');
}

function escapeHtml(value) {
  return String(value ?? "").replace(/[&<>"']/g, (char) => ({
    "&": "&amp;", "<": "&lt;", ">": "&gt;", '"': "&quot;", "'": "&#39;",
  })[char]);
}

function attr(value) { return escapeHtml(value); }
function numberText(value, digits = 2) {
  const number = Number(value);
  return Number.isFinite(number) ? String(Number(number.toFixed(digits))) : "0";
}
function id() { return crypto.randomUUID(); }
function showStatus(element, message, tone = "") {
  element.textContent = message;
  element.dataset.tone = tone;
  element.classList.toggle("error", tone === "error");
  element.classList.toggle("success", tone === "success");
}
function setEditorStatus(message, tone = "") { showStatus(refs.editorStatus, message, tone); }
function setCatalogStatus(message, tone = "") { showStatus(refs.status, message, tone); if($("#import-dialog").open)$("#import-status").textContent=message; }
function markDirty() {
  setEditorStatus("");
  dirty = true;
  history?.record(draft, editGroup);
  if (!drag && simulationResult) { clearSimulation('Preview out of date.'); renderCharts(); }
  persistDraft();
  updateWorkflowState();
  refs.editorSubtitle.textContent = "Unsaved changes";
}
function setPath(root, path, value) {
  const keys = path.split(".");
  let target = root;
  for (const key of keys.slice(0, -1)) target = target[key];
  target[keys.at(-1)] = value;
}
function readInputValue(target) {
  if (target.type === "checkbox") return target.checked;
  if (target.dataset.number === "true") return Number(target.value);
  return target.value;
}
function program() { return draft?.profile?.program; }
function phasesProgram() { return program()?.type === "phases"; }

function visibleRecords() {
  const query = refs.search.value.trim().toLocaleLowerCase();
  return (catalog?.profiles || [])
    .filter((record) => !record.deleted)
    .filter((record) => !query || `${record.profile.name} ${record.profile.description}`.toLocaleLowerCase().includes(query))
    .sort((a, b) => Number(b.favorite) - Number(a.favorite) || a.profile.name.localeCompare(b.profile.name));
}

function renderCatalog() {
  if(!catalog)return;
  const records=visibleRecords();refs.catalogSummary.textContent=`${records.length} profiles`;
  refs.profiles.innerHTML=records.length?records.map(record=>`<article class="profile-card${selectedId===record.id&&!isNewDraft?' active':''}"><button type="button" class="profile-open" data-action="edit-record" data-id="${attr(record.id)}" aria-label="${attr(record.profile.name)}">${record.favorite?'<span class="favorite-indicator" aria-hidden="true">★</span>':''}<strong>${escapeHtml(record.profile.name)}</strong></button></article>`).join(''):'<div class="empty-list">No profiles</div>';
  refs.newProfile.classList.toggle("hidden", Boolean(localDatabase));
  document.querySelectorAll('[data-open-dialog="import-dialog"]').forEach(button => button.classList.toggle("hidden", Boolean(localDatabase)));
}

function renderProfileFields() {
  const favoriteControl=localDatabase?'':`<label class="favorite-control"><input type="checkbox" aria-label="Favorite" data-path="favorite" ${draft.favorite?'checked':''}><span aria-hidden="true">${draft.favorite?'★':'☆'}</span></label>`;
  refs.profileFields.innerHTML=`<input class="profile-title" style="width:${Math.max(12,Math.min(draft.profile.name.length+1,38))}ch" required maxlength="100" aria-label="Profile name" placeholder="Untitled profile" data-path="profile.name" value="${attr(draft.profile.name)}">${favoriteControl}`;
}

function renderProfileSettings() {
  const settings = draft.profile.singleDoseSettings;
  refs.profileSettings.innerHTML = `
    <div class="check-row"><label><input type="checkbox" data-path="profile.autoLinkOpen"> Auto-link when the machine opens</label></div>
    <div class="check-row"><label><input type="checkbox" data-path="profile.singleDoseSettings.enabled" ${settings.enabled ? "checked" : ""}> Enable single-dose settings</label></div>
    <label>Grinding size <span class="hint">${settings.grindingSize}</span><input type="range" min="0" max="600" step="1" data-path="profile.singleDoseSettings.grindingSize" data-number="true" value="${attr(settings.grindingSize)}"></label>
    <label>Grinding speed <span class="hint">${settings.grindingSpeed}</span><input type="range" min="200" max="1000" step="1" data-path="profile.singleDoseSettings.grindingSpeed" data-number="true" value="${attr(settings.grindingSpeed)}"></label>
  `;
  refs.profileSettings.querySelector('[data-path="profile.autoLinkOpen"]').checked = draft.profile.autoLinkOpen;
  for (const input of refs.profileSettings.querySelectorAll('input[type="range"]')) input.disabled = !settings.enabled;
}

function optionList(values, selected, labels = {}) {
  return values.map((value) => `<option value="${attr(value)}" ${value === selected ? "selected" : ""}>${escapeHtml(labels[value] || value)}</option>`).join("");
}

function phaseLabel(phase) {
  if (phase.name?.trim()) return phase.name.trim();
  if (phase.control?.type === "pressure") return `Pressure · ${numberText(phase.control.bar)} bar`;
  if (phase.control?.type === "flow") return `Flow · ${numberText(phase.control.millilitresPerSecond)} ml/s`;
  return `Pump pause`;
}

function renderPhaseList() {
  $('#start-recording')?.remove();const list=program().phases||[];
  refs.phaseListTitle.textContent='Phases';refs.phaseListHint.textContent='';refs.addPhase.classList.remove('hidden');refs.addPhase.textContent='+ Phase';refs.phaseList.className='phase-list';
  refs.phaseList.innerHTML=list.length?list.map((phase,index)=>`<button type="button" class="phase-row${selectedPhaseIndex===index?' selected':''}" data-action="select-phase" data-index="${index}"><i class="phase-dot ${phase.control.type==='flow'?'flow':phase.control.type==='pump_pause'?'pause':''}"></i><span>${escapeHtml(phase.name||`Phase ${index+1}`)}<small>${isVariablePhase(phase)?'Up to ':''}${numberText(effectivePhaseDurationMs(phase)/1000,1)} s</small></span></button>`).join(''):'<span class="empty-list">Add the first phase.</span>';
  if(!list.length)refs.addPhase.insertAdjacentHTML('afterend','<button id="start-recording" type="button" class="button ghost small" data-action="start-recording">New recording</button>');
}

function renderRecordingList() {
  const recording = program().recording;
  refs.phaseListTitle.textContent = "Recording";
  refs.phaseList.innerHTML = `<button type="button" class="button ghost small" data-open-dialog="recording-dialog">${recording.samples.length} samples · View table</button>`;
  refs.phaseListHint.textContent = "";
  refs.addPhase.classList.add("hidden");
  const mode = recording.controlMode;
  $('#recording-samples').innerHTML = `<div class="field-grid">
      <label class="wide">Replay control<select data-action="recording-mode"><option value="Pressure" ${mode === "Pressure" ? "selected" : ""}>Pressure replay</option><option value="Flow" ${mode === "Flow" ? "selected" : ""}>Flow replay</option></select></label>
    </div>
    <div class="subsection-head"><strong>Recorded samples</strong><button type="button" class="button secondary small" data-action="append-sample">＋ Add sample</button></div>
    <div class="recording-table-wrap"><table class="recording-table"><thead><tr><th>Time s</th><th>Pressure bar</th><th>Flow ml/s</th><th>Volume ml</th><th>Weight g</th><th>Rate g/s</th><th></th></tr></thead><tbody>
    ${recording.samples.map((sample, index) => `<tr class="${selectedSampleIndex === index ? "selected" : ""}" data-action="select-sample" data-index="${index}">
      <td><input aria-label="Sample ${index + 1} time in seconds" type="number" min="${index ? numberText(recording.samples[index - 1].elapsedMillis / 1000, 3) : "0"}" step="0.001" data-sample-time="${index}" value="${numberText(sample.elapsedMillis / 1000, 3)}" ${index === 0 ? "disabled" : ""}></td>
      ${["pressure", "flowRate", "volume", "weight", "weightRate"].map((key) => `<td><input aria-label="Sample ${index + 1} ${key}" type="number" min="0" step="any" data-sample-index="${index}" data-sample-key="${key}" value="${numberText(sample.data[key])}"></td>`).join("")}
      <td><button type="button" class="button ghost small" data-action="remove-sample" data-index="${index}" ${index === 0 ? "disabled" : ""} aria-label="Remove sample ${index + 1}">×</button></td>
    </tr>`).join("")}
    </tbody></table></div>
`;
  $("#start-recording")?.remove();
}

function renderPhaseInspector() {
  const phase=availablePhase();
  if(!phase){refs.inspector.innerHTML='<p class="hint">Select a phase</p>';$('#technical-inspector').replaceChildren();return;}
  const isPause=phase.control.type==='pump_pause', ramp=phase.ramp||{}, smooth=ramp.style!=='Instant';
  const target=phase.targetSource?.kind==='Constant'?phase.targetSource.value:targetValue(phase.control);
  const computed=phase.targetSource&&phase.targetSource.kind!=='Constant', unit=phase.control.type==='flow'?'ml/s':'bar';
  const starts=startOptions(program(),selectedPhaseIndex), conditions=phase.exitConditions||[], measured=conditions.filter(c=>c.metric!=='PhaseTime'), timed=!isVariablePhase(phase)&&!measured.length;
  const limit=phase.secondaryLimit;
  refs.inspector.innerHTML=`<div class="inspector-head"><input class="phase-name" aria-label="Phase name" data-phase-path="name" maxlength="80" value="${attr(phase.name)}" placeholder="Phase ${selectedPhaseIndex+1}"><details class="menu"><summary aria-label="Phase actions">⋯</summary><div class="menu-list">${[['before','Insert before'],['after','Insert after'],['duplicate','Duplicate'],['up','Move up'],['down','Move down']].map(([op,label])=>`<button type="button" data-action="phase-operation" data-operation="${op}">${label}</button>`).join('')}<hr><button type="button" data-action="remove-phase">Remove phase</button></div></details></div>
    <label>Controller<select data-phase-path="control.type">${optionList(CONTROL_TYPES,phase.control.type,{pressure:'Pressure',flow:'Flow',pump_pause:'Pause'})}</select></label>
    ${isPause?'':`<label>Target (${unit})${computed?'<button type="button" class="button secondary" data-open-dialog="diagnostics-dialog">From saved recipe</button>':`<input type="number" min="0" step="any" data-product="goal" value="${target}">`}</label><label>Transition<select data-product="transition">${optionList(['instant','ramp'],smooth?'ramp':'instant',{instant:'Immediate',ramp:'Ramp'})}</select></label>`}
    <label>${timed?'Duration (s)':'Time limit (s)'}<input type="number" min="0.001" step="0.001" data-product="duration" value="${(timed?effectivePhaseDurationMs(phase):phase.maximumDurationMillis)/1000}"></label>
    ${smooth&&!isPause?`<label class="wide">Start from<select data-product="start">${starts.options.map(o=>`<option value="${attr(o.value)}" ${o.value===starts.selected?'selected':''}>${escapeHtml(o.label)}</option>`).join('')}</select></label>${starts.selected==='fixed'?`<label>Start (${unit})<input type="number" min="0" step="any" data-product="fixed-start" value="${ramp.source.value}"></label>`:''}`:''}
    <label class="wide">End<select data-product="end">${optionList(['time','condition'],measured.length?'condition':'time',{time:'After duration',condition:'When a condition is met'})}</select></label>
    ${measured.length===1?`<div class="field-grid wide"><label class="wide">When${measured[0].scope==='Step'?' · this phase':''}<select data-product="condition-metric">${optionList(CONDITION_METRICS.filter(m=>m!=='PhaseTime'),measured[0].metric,metricLabels)}</select></label><label><span class="sr-only">Comparison</span><select aria-label="Comparison" data-product="condition-comparison">${optionList(CONDITION_COMPARISONS,measured[0].comparison,comparisons)}</select></label><label>${measured[0].metric==='PumpFlow'?'ml/s':measured[0].metric==='CupWeight'?'g':measured[0].metric==='PumpedVolume'?'ml':'bar'}<input aria-label="Condition threshold" type="number" min="0" step="any" data-product="condition-value" value="${measured[0].threshold}"></label></div>`:measured.length?`<span class="hint wide">${measured.length} conditions · More options</span>`:''}
    ${!isPause&&limit?`<label class="wide">${phase.control.type==='flow'?'Pressure':'Flow'} feedback<select data-product="feedback-mode">${optionList(['limit','range','off'],limit.minimum!=null?'range':'limit',{limit:'Soft limit',range:'Range',off:'Off'})}</select></label><div class="field-grid wide">${limit.minimum!=null?`<label>Min (${phase.control.type==='flow'?'bar':'ml/s'})<input type="number" min="0" step="any" data-product="range-minimum" value="${limit.minimum}"></label>`:''}<label ${limit.minimum==null?'class="wide"':''}>Max (${phase.control.type==='flow'?'bar':'ml/s'})<input type="number" min="0" step="any" data-product="limit" value="${limit.ceiling}"></label></div>`:''}
    <details class="more-options"><summary>More options</summary><div>${phase.control.type==='pressure'?`<label>Pressure sensor<select data-phase-path="control.location">${optionList(['Pump','Group','Boiler'],phase.control.location||'Pump')}</select></label>`:''}<label>Minimum time (s)<input type="number" min="0" step="any" data-phase-path="minimumDurationMillis" data-seconds="true" data-number="true" value="${(phase.minimumDurationMillis||0)/1000}"></label>${smooth?`<label>Ramp shape<select data-phase-path="ramp.style">${optionList(RAMP_STYLES.filter(x=>x!=='Instant'),ramp.style,{Linear:'Straight',EaseIn:'Ease in',EaseOut:'Ease out',EaseInOut:'Ease in/out'})}</select></label><label>Ramp time (s)<input type="number" min="0" step="any" data-phase-path="ramp.durationMillis" data-number="true" data-seconds="true" value="${ramp.durationMillis/1000}"></label>`:''}${conditions.length>1?`<label>Measurement rules<select data-phase-path="conditionOperator">${optionList(['Or','And'],phase.conditionOperator||'Or',{Or:'Any',And:'All'})}</select></label><div class="condition-list">${conditions.map((c,i)=>renderCondition(c,i)).join('')}</div>`:''}${measured.length===1&&['PumpedVolume','CupWeight'].includes(measured[0].metric)?`<label>Count from<select data-condition-index="${conditions.indexOf(measured[0])}" data-condition-key="scope">${optionList(['Total','Step'],measured[0].scope||'Total',{Total:'Shot start',Step:'Phase start'})}</select></label>`:''}<button type="button" class="button secondary small" data-action="add-condition">Add condition</button>${!isPause&&!limit?`<button type="button" class="button secondary small" data-action="add-limit">Add ${phase.control.type==='flow'?'pressure':'flow'} feedback</button>`:''}${phase.stabilityDurationMillis?`<label>Condition hold (ms)<input type="number" min="0" step="1" data-v2-path="stabilityDurationMillis" value="${phase.stabilityDurationMillis}"></label>`:''}</div></details>`;
  renderTechnicalPhaseInspector();
}

function renderTechnicalPhaseInspector() {
  const phases = program().phases || [];
  const phase = phases[selectedPhaseIndex];
  if (!phase) {
    $('#technical-inspector').innerHTML = `<h3>Phase inspector</h3><p class="hint">Select a phase or add one to edit its target, ramp, and trigger rules.</p>`;
    return;
  }
  const index = selectedPhaseIndex;
  const controlType = phase.control?.type || "pump_pause";
  const target = targetValue(phase.control);
  const ramp = phase.ramp || { style: "Instant", durationMillis: 0, start: "PreviousTarget" };
  const conditions = phase.exitConditions || [];
  const adaptive = phase.adaptiveFlowProbe;
  const variable = isVariablePhase(phase);
  const isRemovable = index > 0 && index < phases.length - 1 && phases[index - 1].control?.type === controlType && phases[index + 1].control?.type === controlType && !phases.slice(index - 1, index + 2).some((item) => isVariablePhase(item) || item.adaptiveFlowProbe);
  $('#technical-inspector').innerHTML = `
    <div class="subsection-head"><h3>Phase ${index + 1}</h3><div class="toolbar-actions"><button type="button" class="button ghost small" data-action="remove-phase-point" ${isRemovable ? "" : "disabled"}>Merge point</button><button type="button" class="button ghost small" data-action="remove-phase" >Remove phase</button></div></div>
    <p class="hint">${escapeHtml(describePhaseRules(phase))}</p>
    <div class="toolbar-actions">${[['before','Insert before'],['after','Insert after'],['duplicate','Duplicate'],['up','Move up'],['down','Move down']].map(([op,label]) => `<button type="button" class="button secondary small" data-action="phase-operation" data-operation="${op}">${label}</button>`).join('')}</div>
    <div class="field-grid">
      <label class="wide">Name<input maxlength="80" data-phase-path="name" value="${attr(phase.name || "")}" placeholder="Optional phase label"></label>
      <label class="wide">Control mode<select data-phase-path="control.type">${optionList(CONTROL_TYPES, controlType, { pressure: "Pressure", flow: "Flow", pump_pause: "Pump pause" })}</select></label>
      ${controlType === "pressure" ? `<label>Pressure target (bar)<input type="number" min="0" step="any" ${phase.targetSource ? 'disabled' : ''} data-phase-path="control.bar" data-number="true" value="${attr(numberText(phase.control.bar))}"></label>
      <label>Pressure location<select data-phase-path="control.location">${optionList(["Pump", "Group", "Boiler"], phase.control.location || "Pump")}</select></label>` : ""}
      ${controlType === "flow" ? `<label class="wide">Flow target (ml/s)<input type="number" min="0" step="any" ${phase.targetSource ? 'disabled' : ''} data-phase-path="control.millilitresPerSecond" data-number="true" value="${attr(numberText(phase.control.millilitresPerSecond))}"></label>` : ""}
      <label>Maximum duration (s)<input type="number" min="0.001" step="0.001" data-phase-path="maximumDurationMillis" data-number="true" data-seconds="true" value="${numberText(phase.maximumDurationMillis / 1000, 2)}"></label>
      <label>Minimum duration (s)<input type="number" min="0" step="any" data-phase-path="minimumDurationMillis" data-number="true" data-seconds="true" value="${numberText((phase.minimumDurationMillis || 0) / 1000, 2)}"></label>
      <label>Ramp shape<select data-phase-path="ramp.style" ${controlType === "pump_pause" ? "disabled" : ""}>${optionList(RAMP_STYLES, ramp.style || "Instant")}</select></label>
      <label>Ramp duration (s)<input type="number" min="0" step="any" data-phase-path="ramp.durationMillis" data-number="true" data-seconds="true" value="${numberText((ramp.durationMillis || 0) / 1000, 2)}" ${controlType === "pump_pause" ? "disabled" : ""}></label>
      <label class="wide">Legacy ramp start<select data-phase-path="ramp.start" ${phase.ramp.source ? 'disabled' : ''}>${optionList(RAMP_STARTS, ramp.start || "PreviousTarget", { PreviousTarget: "Previous target", CurrentMeasurement: "Current measurement" })}</select></label>
    </div>
    <div class="subsection">
      <div class="subsection-head"><div><strong>Exit conditions</strong><div class="hint">${variable ? "Planned-time endpoint is an estimate. Trigger may end this phase earlier or later." : "Edit the rules that end this phase."}</div></div><button type="button" class="button secondary small" data-action="add-condition">＋ Rule</button></div>
      <label>Condition combination<select data-phase-path="conditionOperator">${optionList(["Or", "And"], phase.conditionOperator || "Or", { Or: "Any rule (OR)", And: "All measurement rules; time remains an OR deadline" })}</select></label>
      <div class="condition-list">${conditions.map((condition, conditionIndex) => renderCondition(condition, conditionIndex)).join("") || `<p class="hint">No exit rules. The maximum duration ends this phase.</p>`}</div>
    </div>
    ${adaptive ? `<div class="probe-box"><div class="subsection-head"><strong>Adaptive flow probe</strong><button type="button" class="button ghost small" data-action="remove-probe">Remove</button></div><div class="field-grid">
      <label>Sampling time (s)<input type="number" min="0.001" step="0.001" data-probe-path="samplingDurationMillis" data-number="true" data-seconds="true" value="${numberText(adaptive.samplingDurationMillis / 1000, 2)}"></label>
      <label>Pressure ceiling (bar)<input type="number" min="0.1" step="any" data-probe-path="pressureCeilingBar" data-number="true" value="${attr(numberText(adaptive.pressureCeilingBar))}"></label>
      <label class="wide">Flow backoff (ml/s)<input type="number" min="0" step="any" data-probe-path="flowBackoffMillilitresPerSecond" data-number="true" value="${attr(numberText(adaptive.flowBackoffMillilitresPerSecond))}"></label>
    </div></div>` : `<button type="button" class="button secondary small" data-action="add-probe" ${controlType === "flow" ? "" : "disabled"}>＋ Add adaptive flow probe</button>`}
    ${renderRecipeFields(draft, index, escapeHtml)}
    <p class="hint">Triggered phase endpoints stay at their planned-time estimate. Their target and rules remain editable.</p>
  `;
}

function renderCondition(condition, index) {
  const labels = { PumpPressure: "Pump pressure", GroupPressure: "Group pressure", BoilerPressure: "Boiler pressure", PumpFlow: "Pump flow", PumpedVolume: "Pumped volume", CupWeight: "Cup weight", PhaseTime: "Phase time" };
  const thresholdUnit = ["PumpPressure", "GroupPressure", "BoilerPressure"].includes(condition.metric) ? "bar" : condition.metric === "PumpFlow" ? "ml/s" : condition.metric === "PhaseTime" ? "s" : condition.metric === "CupWeight" ? "g" : "ml";
  const conditionOptions = CONDITION_METRICS.filter((metric) => condition.scope !== "Step" || ["PumpedVolume", "CupWeight"].includes(metric));
  return `<div class="condition-row">
    <label>Measure<select data-condition-index="${index}" data-condition-key="metric">${optionList(conditionOptions, condition.metric, labels)}</select></label>
    <label>Comparison<select data-condition-index="${index}" data-condition-key="comparison">${optionList(CONDITION_COMPARISONS, condition.comparison)}</select></label>
    <label>Threshold (${thresholdUnit})<input type="number" min="0" step="any" data-condition-index="${index}" data-condition-key="threshold" data-number="true" data-time-condition="${condition.metric === "PhaseTime"}" value="${attr(numberText(condition.threshold))}"></label>
    <button type="button" class="button ghost small" data-action="remove-condition" data-index="${index}" aria-label="Remove exit condition">×</button>
    <label>Scope<select data-condition-index="${index}" data-condition-key="scope">${optionList(["Total", "Step"], condition.scope || "Total")}</select></label>
  </div>`;
}

function renderInspector() {
  if (phasesProgram()) renderPhaseInspector();
  else renderRecordingInspector();
}

function renderRecordingInspector() {
  const sample=program().recording.samples[selectedSampleIndex];
  $('#technical-inspector').innerHTML='';
  if(!sample){refs.inspector.innerHTML='<p class="hint">Select a point</p>';return;}
  const channel=selectedPoint?.kind==='sample'&&selectedPoint.index===selectedSampleIndex?selectedPoint.channel:program().recording.controlMode==='Flow'?'flowRate':'pressure';
  refs.inspector.innerHTML=`<div class="inspector-head"><h3>Point ${selectedSampleIndex+1}</h3><button type="button" class="button ghost small" data-action="remove-sample" data-index="${selectedSampleIndex}" ${selectedSampleIndex===0?'disabled':''}>Remove</button></div><label>Time (s)<input type="number" step="0.001" min="0" data-sample-time="${selectedSampleIndex}" value="${sample.elapsedMillis/1000}" ${selectedSampleIndex===0?'disabled':''}></label><label>${channel==='flowRate'?'Flow (ml/s)':'Pressure (bar)'}<input type="number" min="0" step="any" data-sample-index="${selectedSampleIndex}" data-sample-key="${channel}" value="${sample.data[channel]}"></label><button type="button" class="button secondary small wide" data-open-dialog="recording-dialog">View all samples</button>`;
}

function chartBounds() {
  if (drag?.durationMs) return { durationMs: drag.durationMs, maxSeconds: drag.durationMs / 1000 };
  let duration = 0;
  if (phasesProgram()) duration = projectCurrent().at(-1)?.endMs || 0;
  else duration = program().recording.samples.at(-1)?.elapsedMillis || 0;
  const maxSeconds = Math.max(5, Math.ceil(duration / 5000) * 5);
  return { durationMs: maxSeconds * 1000, maxSeconds };
}
function xScale(timeMs, durationMs) { return PLOT.left + Math.min(1, Math.max(0, timeMs / durationMs)) * (PLOT.right - PLOT.left); }
function yScale(value) { return PLOT.bottom - Math.min(Y_MAX, Math.max(0, Number(value) || 0)) / Y_MAX * (PLOT.bottom - PLOT.top); }
function pathFromPoints(points) {
  return points.map(([x, y], index) => `${index ? "L" : "M"}${x.toFixed(1)},${y.toFixed(1)}`).join(" ");
}
function svgAxis(channel, maxSeconds) {
  const ticks = 4;
  const horizontal = Array.from({ length: ticks + 1 }, (_, index) => {
    const y = PLOT.bottom - index / ticks * (PLOT.bottom - PLOT.top);
    const value = index / ticks * Y_MAX;
    return `<line class="chart-grid" x1="${PLOT.left}" x2="${PLOT.right}" y1="${y}" y2="${y}"/><text class="chart-tick" x="${PLOT.left - 8}" y="${y + 4}" text-anchor="end">${numberText(value, 0)}</text>`;
  }).join("");
  const xTicks = 4;
  const vertical = Array.from({ length: xTicks + 1 }, (_, index) => {
    const x = PLOT.left + index / xTicks * (PLOT.right - PLOT.left);
    return `<line class="chart-grid" x1="${x}" x2="${x}" y1="${PLOT.top}" y2="${PLOT.bottom}"/><text class="chart-tick" x="${x}" y="${PLOT.bottom + 18}" text-anchor="middle">${numberText(maxSeconds * index / xTicks, 0)}s</text>`;
  }).join("");
  return `${horizontal}${vertical}<line class="chart-axis" x1="${PLOT.left}" x2="${PLOT.left}" y1="${PLOT.top}" y2="${PLOT.bottom}"/><line class="chart-axis" x1="${PLOT.left}" x2="${PLOT.right}" y1="${PLOT.bottom}" y2="${PLOT.bottom}"/>`;
}

function renderPhaseChart(channel, durationMs) {
  const segments = projectCurrent();
  const paths = [];
  const markers = [];
  const bands = [];
  for (const segment of segments) {
    const startX = xScale(segment.startMs, durationMs);
    const endX = xScale(segment.endMs, durationMs);
    if (segment.mode === "pump_pause") {
      bands.push(`<rect class="pause-band" x="${startX}" y="${PLOT.top}" width="${Math.max(1, endX - startX)}" height="${PLOT.bottom - PLOT.top}"/>`);
      continue;
    }
    const limit=segment.phase.secondaryLimit;
    if(limit && ((channel==='pressure'&&limit.metric==='PumpPressure')||(channel==='flow'&&limit.metric==='PumpFlow'))) {
      const y=yScale(limit.ceiling),range=limit.minimum!=null;
      if(range){const bottom=yScale(limit.minimum);bands.push(`<rect class="feedback-band" data-feedback="range" x="${startX}" y="${y}" width="${endX-startX}" height="${Math.max(0,bottom-y)}"/><line class="limit-line" x1="${startX}" x2="${endX}" y1="${bottom}" y2="${bottom}"/>`);}
      bands.push(`<line class="limit-line" x1="${startX}" x2="${endX}" y1="${y}" y2="${y}"/><text class="chart-tick" x="${startX+4}" y="${y-5}">${range?`${numberText(limit.minimum)}–${numberText(limit.ceiling)} range`:`${numberText(limit.ceiling)} soft limit`}</text>`);
    }
    if (segment.mode !== channel) {
      paths.push(`<path class="chart-path inactive-target" data-inactive-target="${channel}" data-controller="${segment.mode}" d="M${startX},${PLOT.bottom} L${endX},${PLOT.bottom}"><title>${channel==='pressure'?'Pressure target 0 bar':'Flow target 0 ml/s'} · ${segment.mode} control</title></path>`);
      continue;
    }
    const width = endX - startX;
    const clipId = `phase-label-${channel}-${segment.index}`;
    const fullName = `${segment.index + 1}. ${segment.phase.name || 'Phase'}${segment.variable ? ' · variable endpoint' : ''}`;
    const label = width < 70 ? `${segment.index + 1}` : fullName;
    bands.push(`<defs><clipPath id="${clipId}"><rect x="${startX+2}" y="${PLOT.top}" width="${Math.max(0,width-4)}" height="${PLOT.bottom-PLOT.top}"/></clipPath></defs><rect data-action="select-phase" data-index="${segment.index}" x="${startX}" y="${PLOT.top}" width="${width}" height="${PLOT.bottom-PLOT.top}" fill="${selectedPhaseIndex === segment.index ? '#71896716' : '#71896706'}"><title>${escapeHtml(fullName)}</title></rect>`);
    const nominal=segment.phase.secondaryLimit&&!segment.phase.adaptiveFlowProbe?{...segment,phase:{...segment.phase,secondaryLimit:null}}:segment;
    const pathPoints = phaseCurvePoints(nominal).map(([time, value]) => [xScale(time, durationMs), yScale(value)]);
    if (!pathPoints.length) {
      const explanation = segment.phase.adaptiveFlowProbe || segment.phase.secondaryLimit || segment.phase.ramp?.source ? 'Adaptive · Preview' : 'Measured start';
      paths.push(`<text class="chart-empty" clip-path="url(#${clipId})" x="${(startX + endX) / 2}" y="90" text-anchor="middle"><title>${explanation}</title>${width > 85 ? explanation : ''}</text>`);
      continue;
    }
    const to = valueAtPhase(segment, segment.endMs);
    const path = pathFromPoints(pathPoints);
    paths.push(`<path class="chart-hit" d="${path}" data-action="add-phase-point" data-index="${segment.index}" data-channel="${channel}" aria-label="${segment.variable ? "Planned-time estimate for conditional phase" : "Add a point to the target curve"}"/><path class="chart-path ${channel}${segment.variable ? " variable" : ""}" d="${path}" pointer-events="none"/>`);
    const selected = selectedPhaseIndex === segment.index;
    const rampMillis = segment.phase.ramp?.style === 'Instant' ? 0 : Number(segment.phase.ramp?.durationMillis || 0);
    const handleAction = rampMillis > 0 ? 'drag-ramp-point' : 'drag-phase-point';
    const goalTime = rampMillis > 0 ? segment.startMs + Math.min(rampMillis,segment.durationMs) : segment.endMs;
    if (rampMillis <= segment.durationMs) markers.push(`<circle class="chart-point ${channel}${selected ? ' selected' : ''}" cx="${xScale(goalTime,durationMs)}" cy="${yScale(segment.endValue)}" r="${selected ? 6 : 5}" data-action="${handleAction}" data-index="${segment.index}" data-channel="${channel}" tabindex="0" role="slider" aria-valuemin="0" aria-valuemax="12" aria-valuenow="${segment.endValue}" aria-label="Phase ${segment.index+1} ${channel} goal at ramp completion"/>`);
    if (!segment.variable) markers.push(`<rect class="chart-point ${channel}" x="${endX-4}" y="${PLOT.bottom-4}" width="8" height="8" data-action="drag-boundary" data-index="${segment.index}" data-channel="${channel}" tabindex="0" role="slider" aria-valuemin="0" aria-valuenow="${segment.endMs/1000}" aria-label="Phase ${segment.index+1} deadline in seconds"/>`);
    if (segment.variable) {
      markers.push(`<line class="conditional-line" x1="${endX}" x2="${endX}" y1="${PLOT.top}" y2="${PLOT.bottom}"/>`);

    }
  }
  if (!paths.length) return `${bands.join("")}<text class="chart-empty" x="${(PLOT.left + PLOT.right) / 2}" y="85" text-anchor="middle">No ${channel} target</text>`;
  return `${bands.join("")}${paths.join("")}${markers.join("")}`;
}

function renderRecordingChart(channel, durationMs) {
  const recording = program().recording;
  const targetKey = recording.controlMode === "Pressure" ? "pressure" : "flowRate";
  const points = recording.samples.map((sample) => [xScale(sample.elapsedMillis, durationMs), yScale(sample.data[channel === "pressure" ? "pressure" : "flowRate"])]);
  const isTarget = targetKey === (channel === "pressure" ? "pressure" : "flowRate");
  const pathClass = isTarget ? channel : "telemetry";
  const inactive=`<path class="chart-path inactive-target" data-inactive-target="${channel}" d="M${PLOT.left},${PLOT.bottom} L${xScale(recording.samples.at(-1).elapsedMillis,durationMs)},${PLOT.bottom}"><title>Inactive target 0</title></path>`;
  if(!isTarget&&!$('#show-telemetry').checked)return inactive;
  const replay = recording.controlMode === (channel === 'pressure' ? 'Pressure' : 'Flow');
  const path = pathFromPoints(replay ? recordingReplayPoints(recording, $('#replay-route').value, program().replayPolicy).map(([time, value]) => [xScale(time, durationMs), yScale(value)]) : points);
  const pointNodes = recording.samples.map((sample, index) => {
    const dataKey = channel === "pressure" ? "pressure" : "flowRate";
    const selected = selectedSampleIndex === index;
    return `<circle class="chart-point ${pathClass}${selected ? " selected" : ""}" cx="${xScale(sample.elapsedMillis, durationMs)}" cy="${yScale(sample.data[dataKey])}" r="${selected ? 6 : 4.5}" data-action="drag-sample-point" data-index="${index}" data-channel="${dataKey}" tabindex="0" role="slider" aria-valuemin="0" aria-valuemax="12" aria-valuenow="${attr(numberText(sample.data[dataKey]))}" aria-label="Sample ${index + 1}, ${channel} ${numberText(sample.data[dataKey])}, ${numberText(sample.elapsedMillis / 1000, 1)} seconds"/>`;
  }).join("");
  const line = `<path class="chart-hit" d="${path}" data-action="add-recording-sample" data-channel="${channel}" aria-label="Add a sample on the ${isTarget ? "replay target" : "recorded telemetry"} line"/><path class="chart-path ${pathClass}" d="${path}" pointer-events="none"/>`;
  return `${isTarget?'':inactive}${line}${pointNodes}`;
}

function renderChart(channel) {
  const { durationMs, maxSeconds } = chartBounds();
  const label = channel === "pressure" ? "Pressure" : "Flow";
  const unit = channel === "pressure" ? "bar" : "ml/s";
  const body = phasesProgram() ? renderPhaseChart(channel, durationMs) : renderRecordingChart(channel, durationMs);
  const container = channel === "pressure" ? refs.pressureChart : refs.flowChart;
  const currentSvg = container.querySelector('svg.target-chart');
  if (drag && currentSvg === drag.svg) {
    currentSvg.innerHTML = `${svgAxis(channel, maxSeconds)}${body}${overlaySimulation(channel, durationMs)}`;
    return;
  }
  container.innerHTML = `<div class="chart-label"><strong>${label}${!phasesProgram()&&((program().recording.controlMode==='Flow'&&channel==='pressure')||(program().recording.controlMode==='Pressure'&&channel==='flow'))?' · recording':''}</strong><small>${unit}</small></div>
    <svg class="target-chart" viewBox="0 0 ${SVG_WIDTH} ${SVG_HEIGHT}" preserveAspectRatio="none" data-chart="${channel}" aria-label="${label} over time, 0 to ${Y_MAX} ${unit}">
      ${svgAxis(channel, maxSeconds)}${body}${overlaySimulation(channel, durationMs)}
    </svg>`;
}

function renderCharts() {
  if (!draft) return;
  renderChart("pressure");
  renderChart("flow");
  $('#timing-badge').classList.toggle('hidden',!phasesProgram()||!program().phases.some(isVariablePhase));
  if (phasesProgram()) {
    const variableCount = program().phases.filter(isVariablePhase).length;
    refs.chartFooter.textContent = variableCount
      ? `Each phase commands pressure, flow, or pause. Dashed segments and marked endpoints use planned-time estimates for ${variableCount} conditional or adaptive phase${variableCount === 1 ? "" : "s"}.`
      : "Each phase commands pressure, flow, or pause. The other target channel is inactive during that phase.";
  } else {
    refs.chartFooter.textContent = `Step preview follows the selected replay policy. Recorded samples are preserved when deriving a v2 recipe. Solid line is the ${program().recording.controlMode.toLowerCase()} replay target. Dashed gray line is recorded telemetry. Both share the same time axis.`;
  }
  if (simulationResult) refs.chartFooter.textContent += ' Thin dark lines show simulated commands on the actual simulated timeline. Phase bands remain the planned-time projection.';
}

function renderEditor() {
  const hasDraft = Boolean(draft);
  refs.editor.classList.toggle("hidden", !hasDraft);
  refs.editorEmpty.classList.toggle("hidden", hasDraft);
  if (!hasDraft) return;
  for (const section of refs.editor.querySelectorAll("section")) section.inert = Boolean(pendingMerge);
  refs.profileFields.inert=Boolean(pendingMerge);refs.editor.querySelector(".recipe-bar").inert=Boolean(pendingMerge);
  $("#recipe-dialog").inert=Boolean(pendingMerge);$("#diagnostics-dialog").inert=Boolean(pendingMerge);
  renderRecipeSummary();
  refs.editorHeading.textContent = draft.profile.name || "Untitled profile";
  refs.editorSubtitle.textContent = dirty ? "Unsaved changes" : isNewDraft ? "Draft" : "";
  refs.clone.classList.toggle("hidden", isNewDraft || Boolean(localDatabase));
  $('#delete-profile')?.classList.toggle('hidden', isNewDraft || Boolean(localDatabase));
  refs.profileFields.innerHTML = "";
  renderProfileFields();
  renderProfileSettings();
  if (phasesProgram()) renderPhaseList(); else renderRecordingList();
  renderInspector();
  renderCharts();
  refs.conflict.classList.toggle("hidden", !conflictNeedsReload);
  refs.save.disabled = conflictNeedsReload;
  updateWorkflowState();
}

function renderAll() {
  refs.openAppDatabase.classList.toggle("hidden", !localDatabaseEditorAvailable);
  refs.downloadAppDatabase.classList.toggle("hidden", !localDatabase);
  refs.closeAppDatabase.classList.toggle("hidden", !localDatabase);
  refs.databaseBanner.classList.toggle("hidden", !localDatabase);
  if (localDatabase) {
    refs.databaseMessage.textContent = `${localDatabase.fileName} · ${catalog.profiles.length} recipes. Save edits here, then download the updated database. Favorites are stored separately from recipes.`;
  }
  renderCatalog();
  renderEditor();
}

function beginDraft(record, { fresh = false, preserveRecovery = false } = {}) {
  if (dirty && !confirm("Discard the unsaved changes and switch profiles?")) return false;
  if (!preserveRecovery && !localDatabase) clearRecovery();
  closePointEditor();
  selectedPoint=null;
  pendingMerge = null;
  draft = normalizeRecord(record);
  clearSimulation();
  importedScenario = null;
  history = new DraftHistory(draft);
  editGroup = null;
  baseline = fresh ? null : normalizeRecord(record);
  selectedId = record.id;
  selectedPhaseIndex = record.profile.program.type === "phases" ? 0 : -1;
  selectedSampleIndex = record.profile.program.type === "recording" ? 0 : -1;
  dirty = false;
  isNewDraft = fresh;
  conflictNeedsReload = false;
  conflictCatalog = null;
  pendingDeleteId = null;
  setEditorStatus("");
  renderAll();
  return true;
}

function stableJson(value) {
  if (Array.isArray(value)) return `[${value.map(stableJson).join(",")}]`;
  if (value && typeof value === "object") {
    return `{${Object.keys(value).sort().map(key => `${JSON.stringify(key)}:${stableJson(value[key])}`).join(",")}}`;
  }
  return JSON.stringify(value);
}

function appDatabaseUpdates(baselines = localDatabase?.originals) {
  if (!localDatabase || !catalog || !baselines) return [];
  const fieldsToCompare = ["name", "description", "finishCondition", "autoLinkOpen", "singleDoseSettings", "program"];
  return catalog.profiles.flatMap(record => {
    const original = baselines.get(record.id);
    if (!original) return [];
    const profile = normalizeRecord(record).profile;
    const fields = fieldsToCompare.filter(field => stableJson(profile[field]) !== stableJson(original.profile[field]));
    return fields.length ? [{ id: record.id, fields, profile }] : [];
  });
}

function setDatabaseStatus(message, tone = "") {
  if (localDatabase) setEditorStatus(message, tone);
  else setCatalogStatus(message, tone);
}

async function detectLocalDatabaseEditor() {
  if (!["127.0.0.1", "localhost"].includes(location.hostname)) return;
  try {
    const response = await fetch(new URL("status", DATABASE_API_URL), { cache: "no-store" });
    if (!response.ok) return;
    const body = await response.json();
    localDatabaseEditorAvailable = body.available === true;
    refs.openAppDatabase.classList.toggle("hidden", !localDatabaseEditorAvailable);
  } catch {
    localDatabaseEditorAvailable = false;
  }
}

async function openAppDatabase(file) {
  if (!file) return;
  if ((dirty || localDatabase?.hasPendingChanges) && !confirm("Discard unsaved edits and open another app database?")) return;
  refs.openAppDatabase.disabled = true;
  refs.openAppDatabase.textContent = "Opening…";
  setDatabaseStatus("Opening the app database…");
  try {
    const response = await fetch(new URL("open", DATABASE_API_URL), {
      method: "POST",
      headers: {
        "Content-Type": "application/vnd.sqlite3",
        "X-Database-Name": encodeURIComponent(file.name),
      },
      body: file,
      cache: "no-store",
    });
    const body = await response.json();
    if (!response.ok) throw new Error(body.message || `Could not open the database (${response.status}).`);
    if (!body.catalog || !Array.isArray(body.catalog.profiles)) throw new Error("The database response did not contain recipe profiles.");

    const originals = new Map(body.catalog.profiles.map(record => [record.id, normalizeRecord(record)]));
    localDatabase = {
      sessionId: body.sessionId,
      fileName: body.fileName || file.name,
      originals,
      downloaded: new Map(originals),
      hasPendingChanges: false,
    };
    catalog = body.catalog;
    bearerToken = "";
    dirty = false;
    draft = null;
    baseline = null;
    selectedId = null;
    isNewDraft = false;
    refs.accessCard.classList.add("hidden");
    refs.workspace.classList.remove("hidden");
    $("#connection-state").textContent = `App database · ${body.profileCount} recipes`;
    if (catalog.profiles.length) beginDraft(catalog.profiles[0], { preserveRecovery: true });
    else renderAll();
    setDatabaseStatus(`Opened ${body.profileCount} recipes from ${localDatabase.fileName}.`, "success");
  } catch (error) {
    setDatabaseStatus(error.message || "Could not open the app database.", "error");
  } finally {
    refs.openAppDatabase.disabled = false;
    refs.openAppDatabase.textContent = "Open app database";
    refs.appDatabaseFile.value = "";
  }
}

async function downloadAppDatabase() {
  if (!localDatabase) return;
  if (dirty) {
    setEditorStatus("Save this recipe before downloading the database.", "error");
    return;
  }
  refs.downloadAppDatabase.disabled = true;
  refs.downloadAppDatabase.textContent = "Preparing…";
  try {
    const updates = appDatabaseUpdates(localDatabase.originals);
    const response = await fetch(new URL("download", DATABASE_API_URL), {
      method: "POST",
      headers: { "Content-Type": "application/json" },
      body: JSON.stringify({ sessionId: localDatabase.sessionId, updates }),
      cache: "no-store",
    });
    if (!response.ok) {
      const body = await response.json();
      throw new Error(body.message || `Could not create the database copy (${response.status}).`);
    }
    const blob = await response.blob();
    const url = URL.createObjectURL(blob);
    const link = document.createElement("a");
    link.href = url;
    link.download = `${localDatabase.fileName.replace(/\.(db|sqlite)$/i, "") || "geeflow-app"}-edited.db`;
    link.click();
    setTimeout(() => URL.revokeObjectURL(url), 1000);
    localDatabase.downloaded = new Map(catalog.profiles.map(record => [record.id, normalizeRecord(record)]));
    localDatabase.hasPendingChanges = false;
    setEditorStatus(`Downloaded a database copy with ${updates.length} edited ${updates.length === 1 ? "recipe" : "recipes"}.`, "success");
  } catch (error) {
    setEditorStatus(error.message || "Could not create the database copy.", "error");
  } finally {
    refs.downloadAppDatabase.disabled = false;
    refs.downloadAppDatabase.textContent = "Download database";
  }
}

function closeAppDatabase() {
  if ((dirty || localDatabase?.hasPendingChanges) && !confirm("Discard changes in this editor? The app database file has not been replaced.")) return;
  localDatabase = null;
  catalog = null;
  draft = null;
  baseline = null;
  history = null;
  selectedId = null;
  dirty = false;
  isNewDraft = false;
  refs.workspace.classList.add("hidden");
  refs.accessCard.classList.remove("hidden");
  $("#connection-state").textContent = "Not connected";
  setCatalogStatus("");
  renderAll();
}

async function loadCatalog() {
  const typedToken = refs.token.value.trim();
  if (!typedToken && !bearerToken) { setCatalogStatus("Paste the library bearer token to load the catalog.", "error"); return; }
  if (dirty && !confirm("Reloading replaces the current catalog view. Your unsaved draft will remain open only if the reload succeeds.")) return;
  bearerToken = typedToken || bearerToken;
  refs.load.disabled = true;
  setCatalogStatus("Loading private catalog…");
  try {
    const response = await fetch(API_URL, { headers: { Authorization: `Bearer ${bearerToken}`, Accept: "application/json" }, cache: "no-store" });
    const body = await response.json();
    if (!response.ok) throw new Error(response.status === 401 ? "The token was not accepted." : `Catalog load failed (${response.status}).`);
    if (![1,2].includes(body.schemaVersion) || !Array.isArray(body.profiles) || body.profiles.some(r => ![1,2].includes(r.profile?.program?.version ?? 1))) throw new Error('This catalog requires a newer editor.');
    catalog = body;
    refs.token.value = "";
    refs.workspace.classList.remove("hidden");
    $("#access-card").classList.add("hidden");
    $("#connection-state").textContent="Connected";
    setCatalogStatus("Connected", "success");
    renderAll();
  } catch (error) {
    bearerToken = "";
    setCatalogStatus(error.message || "Could not load the catalog.", "error");
  } finally {
    refs.load.disabled = false;
  }
}

function startBlankDraft() {
  const record = createBlankRecord(id());
  beginDraft(record, { fresh: true });
  setEditorStatus("");
}

function cloneSelected(sourceId) {
  const source = draft?.id === sourceId ? draft : catalog?.profiles.find(record => record.id === sourceId);
  if (!source) return;
  const copy=cloneRecord(source,id());
  dirty=false;
  beginDraft(copy,{fresh:true});markDirty();setEditorStatus('');
}

function assignField(target, path, value) {
  setPath(target, path, value);
}

function updatePhasePath(path, value) {
  const phase = program().phases[selectedPhaseIndex];
  if (!phase) return;
  if (path === "control.type") {
    if(value===phase.control.type)return;
    // Target sources and response rates use the old control's units.
    phase.targetSource=null;phase.ramp.source=null;phase.secondaryLimit=null;
    if(value!=='flow')phase.adaptiveFlowProbe=null;
    if (value === "pressure") phase.control = { type: "pressure", bar: 0, location: "Pump" };
    else if (value === "flow") phase.control = { type: "flow", millilitresPerSecond: 0 };
    else { phase.control = { type: "pump_pause" }; phase.ramp = { style: "Instant", durationMillis: 0, start: "PreviousTarget" }; phase.adaptiveFlowProbe = null; }
    markDirty(); renderAll(); return;
  }
  let adjusted = value;
  if (path.endsWith("Millis")) adjusted = Math.round(Number(value));
  setPath(phase, path, adjusted);
  if (phase.control.type === "pump_pause") phase.ramp = { style: "Instant", durationMillis: 0, start: phase.ramp?.start || "PreviousTarget" };
  markDirty();
  renderCharts();
  renderPhaseList();
  refs.editorHeading.textContent = draft.profile.name || "Edit profile";
}

function updateCondition(index, key, value) {
  const condition = program().phases[selectedPhaseIndex]?.exitConditions?.[index];
  if (!condition) return;
  condition[key] = value;
  if (key === "metric" && value === "PhaseTime") condition.threshold = Number(condition.threshold);
  if (key === "scope" && value === "Step" && !["PumpedVolume", "CupWeight"].includes(condition.metric)) condition.metric = "PumpedVolume";
  markDirty();
  renderAll();
}

function addPhase() {
  const phases = program().phases;
  const previous = phases.at(-1);
  const index = phases.length;
  const control = previous?.control?.type === "flow"
    ? { type: "flow", millilitresPerSecond: targetValue(previous.control) }
    : { type: "pressure", bar: previous?.control?.type === "pressure" ? targetValue(previous.control) : 0, location: "Pump" };
  phases.push({
    id: id(), name: "", control, maximumDurationMillis: 5000, minimumDurationMillis: 0,
    ramp: { style: previous ? "Linear" : "Instant", durationMillis: previous ? 5000 : 0, start: "PreviousTarget" },
    exitConditions: [], conditionOperator: "Or", adaptiveFlowProbe: null,
  });
  selectedPhaseIndex = index;
  markDirty(); renderAll();
}

function startRecording() {
  draft.profile.program = { type: "recording", version: 1, recording: { controlMode: "Pressure", samples: [
    { elapsedMillis: 0, data: { pressure: 0, flowRate: 0, volume: 0, weight: 0, weightRate: 0 } },
  ] } };
  selectedSampleIndex = 0;
  markDirty(); renderAll();
}

function removeSelectedPhasePoint() {
  try {
    draft.profile.program = removePhasePoint(program(), selectedPhaseIndex);
    selectedPhaseIndex = Math.max(0, selectedPhaseIndex - 1);
    markDirty(); renderAll(); setEditorStatus("");
  } catch (error) { setEditorStatus(error.message, "error"); }
}

function removeSelectedPhase() {
  const phase = program().phases[selectedPhaseIndex];
  if (!phase) return;
  if (isVariablePhase(phase) && !confirm("Remove this phase and its conditions?")) return;
  program().phases.splice(selectedPhaseIndex, 1);
  selectedPhaseIndex = Math.min(selectedPhaseIndex, program().phases.length - 1);
  markDirty(); renderAll();
  setEditorStatus("");
}

function appendRecordingSample() {
  preserveReplaySource();
  const recording = program().recording;
  const nextTime = (recording.samples.at(-1)?.elapsedMillis || 0) + 500;
  try {
    draft.profile.program.recording = insertRecordingSample(recording, nextTime);
    selectedSampleIndex = recording.samples.length;
    markDirty(); renderAll();
    setEditorStatus("");
  } catch (error) { setEditorStatus(error.message, "error"); }
}

function addCondition() {
  const phase = program().phases[selectedPhaseIndex];
  phase.exitConditions.push({ metric: "PumpedVolume", comparison: "Above", threshold: 40, scope: "Total" });
  markDirty(); renderAll();
}

function removeCondition(index) {
  program().phases[selectedPhaseIndex].exitConditions.splice(index, 1);
  markDirty(); renderAll();
}

function addProbe() {
  const phase = program().phases[selectedPhaseIndex];
  if (phase.control.type !== "flow") return;
  const samplingDurationMillis = Math.min(1500, Number(phase.maximumDurationMillis));
  phase.adaptiveFlowProbe = { samplingDurationMillis, pressureCeilingBar: 9, flowBackoffMillilitresPerSecond: 0.1 };
  phase.ramp = { style: "Linear", durationMillis: Math.max(samplingDurationMillis, Number(phase.ramp?.durationMillis || 0)), start: phase.ramp?.start || "PreviousTarget" };
  markDirty(); renderAll();
}

function addChartPoint(channel, svg, event) {
  const path = event.target.closest('[data-action="add-phase-point"], [data-action="add-recording-sample"]');
  if (!path) return false;
  const point = svgPoint(svg, event);
  const { durationMs } = chartBounds();
  const timeMs = Math.max(0, Math.min(durationMs, Math.round(point.timeMs)));
  try {
    if (phasesProgram()) {
      const index = Number(path.dataset.index);
      const segment = projectCurrent().find(item => item.index === index);
      if (!segment || isVariablePhase(segment.phase) || !["pressure", "flow"].includes(segment.mode)) {
        setEditorStatus("Triggered phase interiors stay locked. Edit this phase at its boundary marker or change its target and rules in the inspector.", "error"); return true;
      }
      if (timeMs <= segment.startMs || timeMs >= segment.endMs) return true;
      const value = valueAtPhase(segment, timeMs);
      if (value == null) throw new Error('Supply a measured start before adding a point.');
      const globalSegment = projectPhases(program(), { measurements: { pressure: Number($('#measured-pressure').value), flow: Number($('#measured-flow').value) } })[index];
      draft.profile.program = insertPhasePoint(program(), index, timeMs + (globalSegment.startMs - segment.startMs), value, id, { pressure: $("#measured-pressure").value === "" ? undefined : Number($("#measured-pressure").value), flow: $("#measured-flow").value === "" ? undefined : Number($("#measured-flow").value) });
      selectedPhaseIndex = index;
      setEditorStatus("");
    } else {
      preserveReplaySource();
      const recording = program().recording;
      draft.profile.program.recording = insertRecordingSample(recording, timeMs);
      selectedSampleIndex = recording.samples.findIndex((sample) => sample.elapsedMillis > timeMs);
      if (selectedSampleIndex < 0) selectedSampleIndex = recording.samples.length;
      setEditorStatus("");
    }
    markDirty(); renderAll();
  } catch (error) { setEditorStatus(error.message, "error"); }
  return true;
}

function svgPoint(svg, event) {
  const bounds = svg.getBoundingClientRect();
  const x = (event.clientX - bounds.left) / bounds.width * SVG_WIDTH;
  const y = (event.clientY - bounds.top) / bounds.height * SVG_HEIGHT;
  return { x, y, ...mapChartCoordinates(x, y, PLOT, { durationMs: chartBounds().durationMs, maxValue: Y_MAX }) };
}

function handleChartPointerDown(event) {
  closePointEditor();
  const svg = event.target.closest("svg.target-chart");
  if (!svg) return;
  const actionNode = event.target.closest("[data-action]");
  if (!actionNode) return;
  const action = actionNode.dataset.action;
  if (action === "add-phase-point" || action === "add-recording-sample") {
    event.preventDefault();
    addChartPoint(svg.dataset.chart, svg, event);
    return;
  }
  if (["drag-phase-point", "drag-ramp-point", "drag-boundary", "drag-sample-point"].includes(action)) {
    event.preventDefault();
    if (action !== "drag-sample-point") {
      selectedPhaseIndex = Number(actionNode.dataset.index);
      drag = { kind: action === "drag-ramp-point" ? "ramp" : action === "drag-boundary" ? "boundary" : "phase", action, index: selectedPhaseIndex, channel: actionNode.dataset.channel, chart: svg.dataset.chart, svg, pointerId: event.pointerId, original: deepCopy(draft), historySnapshot: deepCopy({current:history.current,past:history.past,future:history.future,group:history.group}), durationMs:chartBounds().durationMs, wasDirty: dirty, changed: false };
    } else {
      selectedSampleIndex = Number(actionNode.dataset.index);
      drag = { kind: "sample", action, index: selectedSampleIndex, channel: actionNode.dataset.channel, chart: svg.dataset.chart, svg, pointerId: event.pointerId, original: deepCopy(draft), historySnapshot: deepCopy({current:history.current,past:history.past,future:history.future,group:history.group}), durationMs:chartBounds().durationMs, wasDirty: dirty, changed: false };
    }
    selectedPoint={kind:drag.kind,index:drag.index,channel:drag.channel,action:drag.action,chart:drag.chart};
    editGroup = 'drag';
    svg.setPointerCapture?.(event.pointerId);
    renderInspector();
  }
}

function handleChartPointerMove(event) {
  if (!drag || drag.pointerId !== event.pointerId) return;
  const point = svgPoint(drag.svg, event);
  const snap = 100;
  const timeMs = snap ? Math.round(point.timeMs / snap) * snap : point.timeMs;
  try {
    if (drag.kind !== "sample") {
      const segment = projectPhases(program())[drag.index];
      const display = projectCurrent().find(item => item.index === drag.index);
      const mappedTime = timeMs + segment.startMs - (display?.startMs || 0);
      const value = drag.kind === 'boundary' ? null : point.value;
      if (drag.kind === 'ramp') {
        program().phases[drag.index]=phaseWithTarget(program().phases[drag.index],value);const phase=program().phases[drag.index];
        if (!segment.variable) phase.ramp.durationMillis=Math.min(phase.maximumDurationMillis,Math.max(0,Math.round(mappedTime-segment.startMs)));
      } else draft.profile.program = movePhasePoint(program(), drag.index, { endMs: segment.variable ? null : mappedTime, value, timeMode: 'resize' });
    } else {
      preserveReplaySource();
      const channelKey = drag.channel;
      draft.profile.program.recording = moveRecordingSample(program().recording, drag.index, {
        elapsedMillis: drag.index === 0 ? null : Math.round(timeMs), channel: channelKey, value: point.value,
      });
    }
    drag.changed = true;
    markDirty();
    // Redraw curve children while retaining pointer capture on the same SVG element.
    renderCharts();
  } catch (error) { setEditorStatus(error.message, "error"); }
}

function endChartDrag(event) {
  if (!drag || (event.pointerId != null && drag.pointerId !== event.pointerId)) return;
  const completed = drag;
  if (event.type === 'pointercancel') {
    draft = completed.original;
    Object.assign(history, completed.historySnapshot); dirty = completed.wasDirty;
    if (dirty) persistDraft(); else clearRecovery();
  }
  if (completed.changed && event.type !== 'pointercancel') { clearSimulation('Preview out of date.'); }
  drag = null; editGroup = null;
  renderAll();
  if (completed.changed && event.type !== 'pointercancel') setEditorStatus('');
  if(!completed.changed&&event.type!=='pointercancel')openPointEditor(selectedPoint,event.clientX,event.clientY);
}

function handleChartKeydown(event) {
  const point = event.target.closest?.('[data-action="drag-phase-point"], [data-action="drag-ramp-point"], [data-action="drag-boundary"], [data-action="drag-sample-point"]');
  if(point&&event.key==='Enter'){event.preventDefault();const rect=point.getBoundingClientRect();openPointEditor({kind:point.dataset.action==='drag-sample-point'?'sample':point.dataset.action==='drag-ramp-point'?'ramp':point.dataset.action==='drag-boundary'?'boundary':'phase',index:Number(point.dataset.index),channel:point.dataset.channel,action:point.dataset.action},rect.left,rect.bottom);return;}
  if (!point || !["ArrowUp", "ArrowDown", "ArrowLeft", "ArrowRight"].includes(event.key)) return;
  event.preventDefault();
  const index = Number(point.dataset.index);
  const channel = point.dataset.channel;
  try {
    if (point.dataset.action !== "drag-sample-point") {
      const segment = projectPhases(program())[index];
      const phase = program().phases[index];
      const current = phase.targetSource?.kind === 'Constant' ? phase.targetSource.value : targetValue(phase.control);
      const value = event.key === "ArrowUp" ? Math.min(12, current + 0.1) :
        event.key === "ArrowDown" ? Math.max(0, current - 0.1) : null;
      const endMs = event.key === "ArrowLeft" ? segment.endMs - 500 :
        event.key === "ArrowRight" ? segment.endMs + 500 : null;
      if (segment.variable && endMs != null) return;
      if (point.dataset.action === 'drag-ramp-point') {
        if(value!=null)program().phases[index]=phaseWithTarget(phase,value);
        if(endMs!=null)program().phases[index].ramp.durationMillis=Math.max(0,Math.min(phase.maximumDurationMillis,phase.ramp.durationMillis+(event.key==='ArrowRight'?500:-500)));
      } else draft.profile.program = movePhasePoint(program(), index, { value: point.dataset.action==='drag-boundary'?null:value, endMs, timeMode: 'resize' });
      selectedPhaseIndex = index;
    } else {
      preserveReplaySource();
      const recording = program().recording;
      const sample = recording.samples[index];
      const current = sample.data[channel];
      const value = event.key === "ArrowUp" ? Math.min(12, current + 0.1) :
        event.key === "ArrowDown" ? Math.max(0, current - 0.1) : null;
      const elapsedMillis = event.key === "ArrowLeft" ? sample.elapsedMillis - 500 :
        event.key === "ArrowRight" ? sample.elapsedMillis + 500 : null;
      draft.profile.program.recording = moveRecordingSample(recording, index, { elapsedMillis, channel: value == null ? undefined : channel, value });
      selectedSampleIndex = index;
    }
    markDirty(); renderAll();
    document.querySelector(`svg[data-chart="${channel === "pressure" ? "pressure" : channel === "flow" || channel === "flowRate" ? "flow" : ""}"] [data-action="${point.dataset.action}"][data-index="${index}"]`)?.focus();
  } catch (error) { setEditorStatus(error.message, "error"); }
}

function setFinishType(value) {
  if (value === "none") draft.profile.finishCondition = null;
  else if (!draft.profile.finishCondition || draft.profile.finishCondition.type !== value) {
    draft.profile.finishCondition = { type: value, target: value === "weight" ? 36 : 40 };
  }
  if (program().recipe) program().recipe.settings = draft.profile.finishCondition?.type === 'weight' ? { ...program().recipe.settings, targetYieldG: draft.profile.finishCondition.target } : {};
  markDirty(); renderProfileFields(); renderRecipeSummary();
}

function clearDeletedProfile(name) {
  dirty = false;
  draft = null;
  baseline = null;
  selectedId = null;
  selectedPhaseIndex = -1;
  selectedSampleIndex = -1;
  isNewDraft = false;
  conflictNeedsReload = false;
  conflictCatalog = null;
  pendingDeleteId = null;
  history = null;
  pendingMerge = null;
  clearRecovery();
  renderAll();
  const next = visibleRecords()[0];
  if (next) beginDraft(next, { preserveRecovery: true });
  showStatus($('#catalog-status'), `Deleted "${name}". GeeFlow will remove it from devices on the next sync.`, 'success');
}

async function deleteSelectedProfile() {
  if (localDatabase || !draft || isNewDraft || !catalog) return;
  const idToDelete = draft.id;
  const current = catalog.profiles.find(record => record.id === idToDelete);
  if (!current || current.deleted) return;
  const name = current.profile.name;
  if (!confirm(`Delete "${name}" from the library? Unsaved edits to this profile will be discarded. GeeFlow will remove it from devices on the next sync.`)) return;

  const button = $('#delete-profile');
  button.disabled = true;
  const sendDeletion = async latest => {
    const outgoing = deepCopy(latest);
    outgoing.profiles = outgoing.profiles.map(record => record.id === idToDelete ? { ...record, deleted: true } : record);
    return fetch(API_URL, {
      method: 'PUT',
      headers: {
        Authorization: `Bearer ${bearerToken}`,
        'Content-Type': 'application/json',
        'If-Match': String(latest.revision),
      },
      body: JSON.stringify(outgoing),
      cache: 'no-store',
    });
  };
  try {
    const response = await sendDeletion(catalog);
    const body = await response.json();
    if (response.status === 409) {
      catalog = body.catalog;
      const latest = catalog.profiles.find(record => record.id === idToDelete);
      if (latest?.deleted) {
        clearDeletedProfile(name);
      } else {
        conflictCatalog = catalog;
        conflictNeedsReload = true;
        pendingDeleteId = idToDelete;
        renderAll();
        setEditorStatus('The library changed while deleting. Review the latest catalog before saving or deleting again.', 'error');
      }
      return;
    }
    if (!response.ok) throw new Error(body.message || `Delete failed (${response.status}).`);
    catalog = body;
    clearDeletedProfile(name);
  } catch (error) {
    try {
      const response = await fetch(API_URL, {
        headers: { Authorization: `Bearer ${bearerToken}`, Accept: 'application/json' },
        cache: 'no-store',
      });
      if (!response.ok) throw new Error(`Catalog reload failed (${response.status}).`);
      catalog = await response.json();
      const latest = catalog.profiles.find(record => record.id === idToDelete);
      if (latest?.deleted) clearDeletedProfile(name);
      else {
        conflictCatalog = catalog;
        conflictNeedsReload = true;
        pendingDeleteId = idToDelete;
        renderAll();
        setEditorStatus(`${error.message || 'Delete failed.'} The profile is still active. Review the latest catalog before retrying.`, 'error');
      }
    } catch (reloadError) {
      conflictNeedsReload = true;
      pendingDeleteId = idToDelete;
      renderAll();
      setEditorStatus(`${error.message || 'Delete failed.'} The server response is unclear. Reload the library before retrying.`, 'error');
    }
  } finally {
    button.disabled = false;
  }
}

function readRecordFromEditor() {
  return deepCopy(draft);
}

async function saveDraft(event) {
  event?.preventDefault();
  if (!catalog || !draft) return;
  if (conflictNeedsReload) { setEditorStatus("Load the latest catalog before retrying this save.", "error"); return; }
  if ($('#execution-route')?.value === 'app' && program().version !== 2) programV2();
  const errors = validateProfile(draft.profile);
  if (errors.length) { setEditorStatus(errors.join(" "), "error"); return; }
  const source = catalog.profiles.find((record) => record.id === draft.id);
  if (source?.deleted) { setEditorStatus("This cloud profile is marked deleted. Clone the draft to save it under a new ID.", "error"); return; }
  const outgoing = readRecordFromEditor();
  outgoing.deleted = false;
  const outgoingCatalog = deepCopy(catalog);
  if (outgoing.profile.program.version === 2) outgoingCatalog.schemaVersion = 2;
  if (!outgoingCatalog.profiles.some((record) => record.id === outgoing.id)) outgoingCatalog.profiles.push(outgoing);
  else outgoingCatalog.profiles = outgoingCatalog.profiles.map((record) => record.id === outgoing.id ? outgoing : record);
  if (localDatabase) {
    catalog = outgoingCatalog;
    const saved = catalog.profiles.find(record => record.id === outgoing.id);
    draft = normalizeRecord(saved);
    baseline = normalizeRecord(saved);
    selectedId = saved.id;
    isNewDraft = false;
    dirty = false;
    history.reset(draft);
    localDatabase.hasPendingChanges = appDatabaseUpdates(localDatabase.downloaded).length > 0;
    setEditorStatus(localDatabase.hasPendingChanges ? "Saved in this editor. Download the database copy to apply the edits." : "No recipe changes to download.", "success");
    renderAll();
    return;
  }
  refs.save.disabled = true;
  refs.editor.inert = true;
  setEditorStatus("Saving…");
  try {
    const response = await fetch(API_URL, {
      method: "PUT",
      headers: {
        Authorization: `Bearer ${bearerToken}`,
        "Content-Type": "application/json",
        "If-Match": String(catalog.revision),
      },
      body: JSON.stringify(outgoingCatalog),
      cache: "no-store",
    });
    const body = await response.json();
    if (response.status === 409) {
      conflictCatalog = body.catalog;
      conflictNeedsReload = true;
      setEditorStatus("Review the latest changes before saving.", "error");
      renderAll();
      return;
    }
    if (!response.ok) throw new Error(body.message || `Save failed (${response.status}).`);
    catalog = body;
    const saved = catalog.profiles.find((record) => record.id === outgoing.id);
    draft = deepCopy(saved);
    baseline = deepCopy(saved);
    selectedId = saved.id;
    isNewDraft = false;
    dirty = false;
    history.reset(draft); clearRecovery();
    setEditorStatus("Saved", "success");
    renderAll();
  } catch (error) {
    // The request may have reached the server before the connection failed. Force a reload before any retry.
    conflictNeedsReload = true;
    setEditorStatus(`${error.message || "Save failed."} Reload the catalog before retrying.`, "error");
    refs.conflict.classList.remove("hidden");
    refs.save.disabled = true;
  } finally {
    refs.editor.inert = false;
    if (!conflictNeedsReload) refs.save.disabled = false;
  }
}

async function reloadLatestKeepDraft() {
  refs.useLatest.disabled = true;
  try {
    const response = await fetch(API_URL, { headers: { Authorization: `Bearer ${bearerToken}`, Accept: 'application/json' }, cache: 'no-store' });
    if (!response.ok) throw new Error(`Catalog reload failed (${response.status}).`);
    const latest = await response.json();
    const remote = latest.profiles.find(record => record.id === draft.id);
    if (remote?.deleted && pendingDeleteId === draft.id) {
      const name = draft.profile.name;
      catalog = latest;
      clearDeletedProfile(name);
      return;
    }
    if (remote?.deleted) throw new Error('Cloud record was deleted. Export this draft or clone it under a new ID.');
    catalog = latest;
    if (baseline && remote) {
      pendingMerge = mergeRecords(baseline, draft, remote);
      if (pendingMerge.conflicts.length) {
        $('#merge-choices').innerHTML = pendingMerge.conflicts.map((conflict, index) => `<label>${escapeHtml(conflict.path.join('.'))}<select data-merge-index="${index}"><option value="">Choose…</option><option value="local">Keep my draft: ${attr(JSON.stringify(conflict.local))}</option><option value="remote">Use cloud: ${attr(JSON.stringify(conflict.remote))}</option></select></label>`).join('') + '<button type="button" class="button" id="resolve-conflicts">Apply choices</button>';
        baseline = deepCopy(remote);
        setEditorStatus('Overlapping changes need a choice. Your draft remains open.', 'error');
        renderAll(); return;
      }
      draft = pendingMerge.result;
    }
    baseline = remote ? deepCopy(remote) : null;
    conflictCatalog = null; conflictNeedsReload = false; pendingMerge = null;
    pendingDeleteId = null;
    history.reset(draft); persistDraft();
    setEditorStatus('Changes merged. Ready to save.');
    renderAll();
  } catch (error) {
    setEditorStatus(error.message || "Could not reload the catalog.", "error");
  } finally {
    refs.useLatest.disabled = false;
  }
}

function discardDraft() {
  if (dirty && !confirm("Discard the unsaved draft changes?")) return;
  clearRecovery();
  if (baseline && !isNewDraft) {
    draft = deepCopy(baseline);
    history.reset(draft);
    selectedPhaseIndex = draft.profile.program.type === "phases" ? 0 : -1;
    selectedSampleIndex = draft.profile.program.type === "recording" ? 0 : -1;
    dirty = false;
    conflictNeedsReload = false;
    conflictCatalog = null;
    setEditorStatus("");
    renderAll();
    return;
  }
  draft = null;
  baseline = null;
  selectedId = null;
  selectedPhaseIndex = -1;
  selectedSampleIndex = -1;
  dirty = false;
  isNewDraft = false;
  conflictNeedsReload = false;
  conflictCatalog = null;
  setEditorStatus("");
  renderAll();
}

function handleClick(event) {
  const button = event.target.closest("[data-action]");
  if (!button) return;
  const action = button.dataset.action;
  editGroup = null;
  if (action === 'delete-point') {try{deletePoint();}catch(error){setEditorStatus(error.message,'error');}return;}
  if (action === 'delete-profile') {deleteSelectedProfile();return;}
  if (action === 'upgrade-v2') { programV2();markDirty();renderAll();return; }
  if (action === 'add-capture') { programV2();const phase=availablePhase();phase[button.dataset.at]??=[];phase[button.dataset.at].push({name:`capture_${crypto.randomUUID().slice(0,8)}`,metric:phase.control.type==='flow'?'PumpFlow':'PumpPressure',windowMillis:500,maximumAgeMillis:500,filter:'Mean',minimum:0,maximum:12});markDirty();renderAll();return; }
  if (action === 'remove-capture') { availablePhase()[button.dataset.at].splice(Number(button.dataset.index),1);markDirty();renderAll();return; }
  if (action === 'add-limit') { draft.profile.program=changeFeedbackMode(program(),selectedPhaseIndex,'limit');markDirty();renderAll();return; }
  if (action === 'remove-limit') { availablePhase().secondaryLimit=null;markDirty();renderAll();return; }
  if (action === 'phase-operation') {
    draft.profile.program = operatePhase(program(), selectedPhaseIndex, button.dataset.operation, id);
    if (button.dataset.operation === 'up') selectedPhaseIndex = Math.max(0, selectedPhaseIndex - 1);
    if (['down','after','duplicate'].includes(button.dataset.operation)) selectedPhaseIndex = Math.min(program().phases.length - 1, selectedPhaseIndex + 1);
    markDirty(); renderAll(); return;
  }
  if (action === "edit-record") {
    const record = catalog.profiles.find((item) => item.id === button.dataset.id);
    if (record) beginDraft(record);
  } else if (action === "clone-record") cloneSelected(button.dataset.id);
  else if (action === "select-phase") { selectedPhaseIndex = Number(button.dataset.index); renderAll(); }
  else if (action === "start-recording") startRecording();
  else if (action === "select-sample") {
    selectedSampleIndex = Number(button.dataset.index);
    if (event.target.closest("input")) {
      $("#recording-samples").querySelectorAll("tr[data-action=select-sample]").forEach((row) => row.classList.toggle("selected", Number(row.dataset.index) === selectedSampleIndex));
      renderRecordingInspector(); renderCharts();
    } else renderAll();
  }
  else if (action === "remove-phase-point") removeSelectedPhasePoint();
  else if (action === "remove-phase") removeSelectedPhase();
  else if (action === "add-condition") addCondition();
  else if (action === "remove-condition") removeCondition(Number(button.dataset.index));
  else if (action === "add-probe") addProbe();
  else if (action === "remove-probe") { program().phases[selectedPhaseIndex].adaptiveFlowProbe = null; markDirty(); renderAll(); }
  else if (action === "remove-sample") {
    try { preserveReplaySource(); draft.profile.program.recording = removeRecordingSample(program().recording, Number(button.dataset.index)); selectedSampleIndex = Math.max(0, Number(button.dataset.index) - 1); markDirty(); renderAll(); }
    catch (error) { setEditorStatus(error.message, "error"); }
  }
  else if (action === "append-sample") appendRecordingSample();
}

function handleInput(event) {
  const target = event.target;
  editGroup = target;
  if (target.dataset.path) {
    assignField(draft, target.dataset.path, readInputValue(target));
    markDirty();
    if (target.dataset.path === "profile.name") {refs.editorHeading.textContent=draft.profile.name||"Untitled profile";target.style.width=`${Math.max(12,Math.min(draft.profile.name.length+1,38))}ch`;}
    if (target.dataset.path === "favorite") {renderCatalog();target.nextElementSibling.textContent=target.checked?"★":"☆";}
    if (target.dataset.path.startsWith("profile.singleDoseSettings.")) {
      const hint = target.closest("label")?.querySelector(".hint");
      if (hint) hint.textContent = target.value;
    }
    return;
  }
  if (target.dataset.phasePath) {
    const path = target.dataset.phasePath;
    let value = readInputValue(target);
    if (target.dataset.seconds === "true") value = Math.round(value * 1000);
    updatePhasePath(path, value);
    return;
  }
  if (target.dataset.probePath) {
    let value = readInputValue(target);
    if (target.dataset.seconds === "true") value = Math.round(value * 1000);
    program().phases[selectedPhaseIndex].adaptiveFlowProbe[target.dataset.probePath] = value;
    markDirty(); renderCharts();
    return;
  }
  if (target.dataset.conditionIndex != null && target.dataset.conditionKey === "threshold") {
    const condition = program().phases[selectedPhaseIndex]?.exitConditions?.[Number(target.dataset.conditionIndex)];
    if (condition) condition.threshold = Number(target.value);
    markDirty(); renderCharts();
    return;
  }
  if (target.dataset.conditionIndex != null) {
    let value = readInputValue(target);
    if (target.dataset.timeCondition === "true") value = Number(value);
    updateCondition(Number(target.dataset.conditionIndex), target.dataset.conditionKey, value);
    return;
  }
  if (target.dataset.sampleIndex != null) {
    preserveReplaySource();
    const sampleIndex = Number(target.dataset.sampleIndex);
    const key = target.dataset.sampleKey;
    program().recording.samples[sampleIndex].data[key] = Number(target.value);
    selectedSampleIndex = sampleIndex;
    markDirty(); renderCharts();
    return;
  }
}

function handleChange(event) {
  const target = event.target;
  if(target.dataset.product){try{handleProductChange(target);}catch(error){setEditorStatus(error.message,'error');renderAll();}return;}
  if(target.dataset.pointField){try{changePoint(target.dataset.pointField,Number(target.value));}catch(error){setEditorStatus(error.message,'error');renderPointEditor();}return;}
  if (target.dataset.v2Path) {
    programV2();
    const value = target.type === 'number' ? (target.value === '' ? null : Number(target.value)) : target.value;
    setPath(availablePhase(), target.dataset.v2Path, value);
    markDirty(); renderAll(); return;
  }
  if (target.dataset.sourceSlot) {
    programV2();const phase=availablePhase(),slot=target.dataset.sourceSlot,key=target.dataset.sourceKey;
    const prior=slot==='targetSource'?phase.targetSource:phase.ramp.source;
    let source=prior;
    if(key==='kind') {
      const kind=target.value;
      source=kind==='Legacy'?null:kind==='Constant'?{kind,value:targetValue(phase.control)}:kind==='MeasurementAtEntry'?{kind,metric:phase.control.type==='flow'?'PumpFlow':'PumpPressure'}:kind==='CapturedValue'?{kind,capture:''}:{kind};
    } else { source={...source,[key]:target.type==='number'?Number(target.value):target.value}; }
    setPath(phase,slot,source);markDirty();renderAll();return;
  }
  editGroup = null;
  if (target.id === 'execution-route') {try{setExecutionRoute(target.value);}catch(error){setEditorStatus(error.message,'error');renderAll();}return;}
  if (target.dataset.action === "finish-type") { setFinishType(target.value); return; }
  if (target.dataset.action === "finish-target") {
    draft.profile.finishCondition.target = Number(target.value);
    if (program().recipe && draft.profile.finishCondition.type === "weight") program().recipe.settings.targetYieldG = Number(target.value);
    markDirty(); return;
  }
  if (target.dataset.action === "recording-mode") {
    preserveReplaySource();
    program().recording.controlMode = target.value;
    markDirty(); renderAll(); return;
  }
  if (target.dataset.sampleTime != null) {
    preserveReplaySource();
    const sampleIndex = Number(target.dataset.sampleTime);
    draft.profile.program.recording = moveRecordingSample(program().recording, sampleIndex, {
      elapsedMillis: Math.round(Number(target.value) * 1000),
    });
    selectedSampleIndex = sampleIndex;
    markDirty(); renderAll();
    return;
  }
  if (target.dataset.conditionIndex != null) {
    updateCondition(Number(target.dataset.conditionIndex), target.dataset.conditionKey, readInputValue(target)); return;
  }
  if (target.dataset.phasePath) {
    let value = readInputValue(target);
    if (target.dataset.seconds === "true") value = Math.round(value * 1000);
    updatePhasePath(target.dataset.phasePath, value);
  }
}

refs.load.addEventListener("click", loadCatalog);
refs.token.addEventListener("keydown", (event) => { if (event.key === "Enter") loadCatalog(); });
refs.openAppDatabase.addEventListener("click", () => refs.appDatabaseFile.click());
refs.appDatabaseFile.addEventListener("change", event => openAppDatabase(event.target.files[0]));
refs.downloadAppDatabase.addEventListener("click", downloadAppDatabase);
refs.closeAppDatabase.addEventListener("click", closeAppDatabase);
refs.newProfile.addEventListener("click", startBlankDraft);
refs.search.addEventListener("input", renderCatalog);
refs.clone.addEventListener("click", () => cloneSelected(selectedId));
refs.discard.addEventListener("click", discardDraft);
refs.editor.addEventListener("submit", saveDraft);
refs.useLatest.addEventListener("click", reloadLatestKeepDraft);
refs.addPhase.addEventListener("click", addPhase);
document.addEventListener("click", handleClick);
document.addEventListener("input", handleInput);
document.addEventListener("change", handleChange);
document.addEventListener("pointerdown", handleChartPointerDown);
document.addEventListener("pointermove", handleChartPointerMove);
document.addEventListener("pointerup", endChartDrag);
document.addEventListener("pointercancel", endChartDrag);
document.addEventListener("keydown", handleChartKeydown);
window.addEventListener("beforeunload", (event) => {
  if (dirty || localDatabase?.hasPendingChanges) { event.preventDefault(); event.returnValue = ""; }
});

renderAll();
detectLocalDatabaseEditor();

$('#undo').addEventListener('click', () => { if (!history) return; draft = history.undo(); dirty = true; persistDraft(); renderAll(); });
$('#redo').addEventListener('click', () => { if (!history) return; draft = history.redo(); dirty = true; persistDraft(); renderAll(); });
$('#recover-draft').addEventListener('click', () => {
  if (!recoveredDraft) return;
  const recovery = deepCopy(recoveredDraft);
  if (!beginDraft(recovery.record, { fresh: recovery.isNewDraft })) return;
  baseline = recovery.baseline; dirty = true; history.reset(draft); persistDraft(); renderAll();
});
$('#export-draft').addEventListener('click', () => {
  if (!draft) return;
  const blob = new Blob([JSON.stringify(draft, null, 2)], { type: 'application/json' });
  const url = URL.createObjectURL(blob); const a = document.createElement('a'); a.href = url; a.download = 'geeflow-draft.json'; a.click(); URL.revokeObjectURL(url);
});
$('#import-draft').addEventListener('change', async event => {
  try {
    const file = event.target.files[0]; if (!file) return;
    const record = JSON.parse(await file.text());
    if (validateProfile(record.profile).length) throw new Error(validateProfile(record.profile).join(' '));
    record.id = id(); record.revision = 0; record.deleted = false;
    if (beginDraft(record, { fresh: true })) {markDirty();$("#import-dialog").close();}
  } catch (error) { setCatalogStatus(error.message, 'error'); }
});
for (const name of ['timeline-view','replay-route','machine-model','measured-pressure','measured-flow']) {
  $(`#${name}`).addEventListener('change', () => { if (draft) { renderCharts(); updateWorkflowState(); } });
}
document.addEventListener('click', event => {
  if (event.target.id !== 'resolve-conflicts') return;
  try {
    draft = resolveMerge(pendingMerge, [...document.querySelectorAll('[data-merge-index]')].map(input => input.value));
    pendingMerge = null; conflictNeedsReload = false; $('#merge-choices').innerHTML = ''; history.reset(draft); dirty = true; persistDraft(); renderAll();
    setEditorStatus('Changes merged. Ready to save.');
  } catch (error) { setEditorStatus(error.message, 'error'); }
});
updateWorkflowState();

async function loadDecentSource(source, url = '', adaptation = 'preserve') {
  try {
    const record=await importDecent(source,{url,adaptation});
    if (!beginDraft(record,{fresh:true})) return;markDirty();renderAll();setEditorStatus('');$('#import-dialog').close();
  } catch(error){setCatalogStatus(error.message,'error');}
}
$('#import-decent').addEventListener('change',async event=>{try{const file=event.target.files[0];if(file){await loadDecentSource(JSON.parse(await file.text()));}event.target.value='';}catch(error){setCatalogStatus(error.message,'error');}});
for(const [button,file] of [['load-cremina','cremina'],['load-adaptive','best_practice']])$('#'+button).addEventListener('click',async()=>{try{const response=await fetch(`recipes/decent/${file}.json`);if(!response.ok)throw new Error('Bundled source could not be loaded');await loadDecentSource(await response.json(),`https://raw.githubusercontent.com/decentespresso/decaid/main/assets/defaultProfiles/${file}.json`,file==='cremina'?'pressure-only':'adaptive-capture');}catch(error){setCatalogStatus(error.message,'error');}});
$('#replay-policy').addEventListener('change',event=>{if(!draft||phasesProgram())return;preserveReplaySource();program().replayPolicy=event.target.value;markDirty();renderAll();});
$('#sensor-trace').addEventListener('change',async event=>{try{const file=event.target.files[0];if(file){importedScenario=JSON.parse(await file.text());$('#simulation-summary').textContent='Custom sensor trace loaded. Run simulation to inspect it.';}}catch(error){setEditorStatus(error.message,'error');}});
$('#run-simulation').addEventListener('click',()=>{
  try {
    if(!draft)throw new Error('Choose a profile first.');
    const errors=validateProfile(draft.profile);if(errors.length)throw new Error(errors.join(' '));
    const scenario=importedScenario||syntheticScenario({flow:Number($('#sim-flow').value),noise:Number($('#sim-noise').value),staleAfterMillis:$('#sim-stale').value===''?Infinity:Number($('#sim-stale').value)});
    scenario.acknowledgementDelayMillis=Number($('#sim-ack').value);
    simulationResult=simulateProfile(draft.profile,scenario,{fullTimeline:$('#sim-full').checked});
    $('#timeline-view').value='overview';
    const last=simulationResult.trace.at(-1), reason={global_goal:'Yield reached',program_complete:'Profile complete',profile_timeout:'Time limit reached',recording_timeout:'Recording time limit',device_disconnected:'Disconnected',telemetry_timeout:'Sensor updates stopped',secondary_limit_fault:'Limit exceeded'}[last?.finish]||last?.finish||'End of sample data';
    $('#simulation-summary').textContent=`${numberText((last?.elapsedMillis||0)/1000,1)} s · ${reason}`;
    const events=simulationResult.transitions.map(x=>({time:x.elapsedMillis,text:`${program().phases?.find(p=>p.id===x.phaseId)?.name||'Phase'} · ${{MaximumDuration:'time limit',ConditionMatched:'condition met'}[x.reason]||x.reason}`}));
    $('#simulation-events').innerHTML='<table class="recording-table"><tbody>'+events.map(x=>`<tr><td>${numberText(x.time/1000,1)} s</td><td>${escapeHtml(x.text)}</td></tr>`).join('')+'</tbody></table>';
    $('#simulation-trace').textContent=JSON.stringify({transitions:simulationResult.transitions,captures:simulationResult.captures,decisions:simulationResult.decisions.filter((_,i)=>i%10===0).slice(-100)},null,2);
    renderCharts();
  } catch(error){setEditorStatus(error.message,'error');}
});
$('#export-trace').addEventListener('click',()=>{if(!simulationResult)return;const url=URL.createObjectURL(new Blob([JSON.stringify({profile:draft.profile,...simulationResult},null,2)],{type:'application/json'}));const a=document.createElement('a');a.href=url;a.download='geeflow-simulation-trace.json';a.click();URL.revokeObjectURL(url);});

function closePointEditor() {
  const popover=$('#point-editor');
  if(popover?.matches(':popover-open'))popover.hidePopover();
}
function openPointEditor(point,clientX,clientY) {
  selectedPoint=point;renderPointEditor();
  const popover=$('#point-editor');
  popover.style.margin='0';popover.style.left=`${Math.max(10,Math.min(innerWidth-270,clientX||innerWidth/2-130))}px`;
  popover.style.top=`${Math.max(10,Math.min(innerHeight-260,clientY||innerHeight/2-100))}px`;
  if(!popover.matches(':popover-open'))popover.showPopover();
}
function renderPointEditor() {
  if(!selectedPoint||!draft)return;
  const point=selectedPoint;let time,value,unit,locked=false,removable=false;
  if(point.kind==='sample') {
    const sample=program().recording.samples[point.index];if(!sample){closePointEditor();return;}
    time=sample.elapsedMillis;value=sample.data[point.channel];unit=point.channel==='flowRate'?'ml/s':'bar';locked=point.index===0;removable=point.index>0;
  }else{
    const segment=projectPhases(program())[point.index];if(!segment){closePointEditor();return;}
    time=point.kind==='ramp'?segment.startMs+segment.phase.ramp.durationMillis:segment.endMs;
    value=segment.endValue;unit=segment.mode==='flow'?'ml/s':'bar';locked=segment.variable;
    try{removePhasePoint(program(),point.index);removable=true;}catch{}
  }
  $('#point-title').textContent=point.kind==='boundary'?'Phase edge':'Point';
  $('#point-fields').innerHTML=`<label>Time (s)<input type="number" data-point-field="time" step="0.001" min="0" value="${time/1000}" ${locked?'disabled':''}></label>${point.kind==='boundary'?'':`<label>${unit==='bar'?'Pressure (bar)':'Flow (ml/s)'}<input type="number" data-point-field="value" min="0" step="any" value="${value}"></label>`}${removable?'<button type="button" class="button ghost small" data-action="delete-point">Remove point</button>':''}`;
}
function changePoint(field,value) {
  const point=selectedPoint;if(!point)return;
  if(!Number.isFinite(value)||value<0)throw new Error('Enter a positive value.');
  if(point.kind==='sample'){
    preserveReplaySource();draft.profile.program.recording=moveRecordingSample(program().recording,point.index,field==='time'?{elapsedMillis:Math.round(value*1000)}:{channel:point.channel,value});
  }else if(point.kind==='ramp'){
    const phase=program().phases[point.index],segment=projectPhases(program())[point.index];
    if(field==='value')draft.profile.program=setPhaseGoal(program(),point.index,value);
    else {if(segment.variable)throw new Error('Timing depends on the phase condition.');const duration=Math.round(value*1000)-segment.startMs;if(duration<0||duration>phase.maximumDurationMillis)throw new Error('Keep this point inside its phase.');phase.ramp.durationMillis=duration;}
  }else draft.profile.program=movePhasePoint(program(),point.index,{value:field==='value'?value:null,endMs:field==='time'?Math.round(value*1000):null,timeMode:'resize'});
  markDirty();renderAll();renderPointEditor();
}
function deletePoint() {
  const point=selectedPoint;if(!point)return;
  if(point.kind==='sample'){preserveReplaySource();draft.profile.program.recording=removeRecordingSample(program().recording,point.index);selectedSampleIndex=Math.max(0,point.index-1);}
  else {draft.profile.program=removePhasePoint(program(),point.index);selectedPhaseIndex=Math.max(0,point.index-1);}
  closePointEditor();selectedPoint=null;markDirty();renderAll();
}
function openDialog(id) {
  closePointEditor();document.querySelectorAll('details.menu[open]').forEach(x=>x.open=false);
  const dialog=$('#'+id);if(!dialog||dialog.open)return;
  dialog.showModal();
}
function firstMeasurement() {return availablePhase()?.exitConditions?.find(c=>c.metric!=='PhaseTime');}
function handleProductChange(target) {
  const key=target.dataset.product,phase=availablePhase(),index=selectedPhaseIndex;
  if(!phase)return;
  const n=Number(target.value);
  if(key==='goal'){if(target.value===''||!Number.isFinite(n)||n<0)throw new Error('Enter a target.');draft.profile.program=setPhaseGoal(program(),index,n);}
  else if(key==='transition')draft.profile.program=changeTransition(program(),index,target.value);
  else if(key==='start')draft.profile.program=changeStart(program(),index,target.value);
  else if(key==='fixed-start'){if(target.value===''||!Number.isFinite(n)||n<0)throw new Error('Enter a starting value.');phase.ramp.source.value=n;}
  else if(key==='duration'){
    const ms=Math.round(n*1000);if(!(ms>0)||ms<(phase.minimumDurationMillis||0))throw new Error('Duration must exceed the minimum time.');
    if(phase.adaptiveFlowProbe&&ms<phase.adaptiveFlowProbe.samplingDurationMillis)throw new Error('Duration must cover the sampling time.');
    const old=phase.maximumDurationMillis;
    phase.maximumDurationMillis=ms;
    if(phase.ramp.durationMillis===old)phase.ramp.durationMillis=ms;else phase.ramp.durationMillis=Math.min(ms,phase.ramp.durationMillis);
    if(phase.exitConditions?.length===1&&phase.exitConditions[0].metric==='PhaseTime')phase.exitConditions[0].threshold=ms/1000;
  }else if(key==='end'){
    if(target.value==='time')phase.exitConditions=[];
    else if(!firstMeasurement())phase.exitConditions=[...(phase.exitConditions||[]),{metric:'PumpPressure',comparison:'Above',threshold:6,scope:'Total'}];
  }else if(key==='condition-metric'){
    const c=firstMeasurement();c.metric=target.value;if(!['CupWeight','PumpedVolume'].includes(c.metric))c.scope='Total';
  }else if(key==='condition-comparison')firstMeasurement().comparison=target.value;
  else if(key==='condition-value'){if(target.value===''||!Number.isFinite(n)||n<0)throw new Error('Enter a threshold.');firstMeasurement().threshold=n;}
  else if(key==='feedback-mode')draft.profile.program=changeFeedbackMode(program(),index,target.value);
  else if(key==='range-minimum'){if(target.value==='')throw new Error('Enter a range minimum.');draft.profile.program=changeRangeMinimum(program(),index,n);}
  else if(key==='limit'){if(target.value!==''&&(!Number.isFinite(n)||n<=0))throw new Error('Enter a limit or leave it empty.');draft.profile.program=changeLimit(program(),index,target.value===''?null:n);}
  markDirty();renderAll();
}

$('#close-point').addEventListener('click',closePointEditor);
document.addEventListener('click',event=>{
  if(!event.target.closest('#point-editor,svg.target-chart'))closePointEditor();
  const opener=event.target.closest('[data-open-dialog]');if(opener)openDialog(opener.dataset.openDialog);
  const closer=event.target.closest('[data-close-dialog]');if(closer)closer.closest('dialog').close();
});
for(const dialog of document.querySelectorAll('dialog'))dialog.addEventListener('click',event=>{if(event.target===dialog){const b=dialog.getBoundingClientRect();if(event.clientX<b.left||event.clientX>b.right||event.clientY<b.top||event.clientY>b.bottom)dialog.close();}});
$('#preview').addEventListener('click',()=>{closePointEditor();if(!$('#preview-panel').open)$('#preview-panel').show();$('#run-simulation').click();});
$('#show-telemetry').addEventListener('change',()=>renderCharts());
$('#preview-panel').addEventListener('close',()=>renderCharts());
$('#ratio-dialog').addEventListener('close',()=>{if(draft)renderRecipeSummary();});
$('#deadline-seconds').addEventListener('change',event=>{
  const n=Math.round(Number(event.target.value)*1000);if(!Number.isSafeInteger(n)||n<1||n>40000){setEditorStatus('Time limit must be between 0 and 40 seconds.','error');renderRecipeSummary();return;}
  programV2();program().execution.maximumTotalDurationMillis=n;markDirty();renderAll();
});
$('#targetYieldG').addEventListener('change',event=>{
  const value=Number(event.target.value);if(!draft)return;
  event.target.setCustomValidity('');
  if(!Number.isFinite(value)||value<=0){event.target.setCustomValidity('Enter a yield greater than zero.');setEditorStatus('Enter a yield greater than zero.','error');return;}
  if(!draft.profile.finishCondition)draft.profile.finishCondition={type:'weight',target:value};else draft.profile.finishCondition.target=value;
  if(program().recipe&&draft.profile.finishCondition.type==='weight')program().recipe.settings.targetYieldG=value;
  markDirty();renderRecipeSummary();
});
$('#doseG').addEventListener('change',event=>{
  if(!program()?.recipe)return;
  const value=event.target.value===''?null:Number(event.target.value);event.target.setCustomValidity('');if(value!=null&&(!Number.isFinite(value)||value<=0)){event.target.setCustomValidity('Enter a dose greater than zero.');setEditorStatus('Enter a dose greater than zero.','error');return;}
  const settings=program().recipe.settings;settings.doseG=value;
  if(value&&settings.ratio&&!settings.targetYieldG)draft=configureYield(draft,settings);
  markDirty();renderRecipeSummary();
});
$('#ratio').addEventListener('change',event=>{
  if(!program()?.recipe)return;
  try{const ratio=Number(event.target.value),settings=program().recipe.settings;if(!settings.doseG)throw new Error('Enter a coffee dose first.');if(!(ratio>0))throw new Error('Enter a ratio greater than zero.');draft=configureYield(draft,{...settings,ratio,targetYieldG:null});markDirty();renderRecipeSummary();}catch(error){$('#ratio-status').textContent=error.message;}
});
try{const saved=localStorage.getItem('geeflow.library.machine');if(['LitaBA','LitaBR','LitaLR'].includes(saved))$('#machine-model').value=saved;}catch{}
$('#machine-model').addEventListener('change',event=>{try{localStorage.setItem('geeflow.library.machine',event.target.value);}catch{}updateWorkflowState();});
$('#reconnect').addEventListener('click',()=>{$('#library-dialog').close();$('#access-card').classList.remove('hidden');$('#workspace').classList.add('hidden');$('#token').focus();});
document.addEventListener('keydown',event=>{
  const field=event.target.closest('input,select,textarea,[contenteditable=true]');
  if((event.metaKey||event.ctrlKey)&&event.key.toLowerCase()==='s'){event.preventDefault();saveDraft();}
  if((event.metaKey||event.ctrlKey)&&event.key.toLowerCase()==='z'&&!field){event.preventDefault();$(event.shiftKey?'#redo':'#undo').click();}
  if(event.key==='Escape')closePointEditor();
});
