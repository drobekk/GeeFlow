import test from "node:test";
import assert from "node:assert/strict";
import {
  cloneRecord,
  createBlankRecord,
  insertPhasePoint,
  insertRecordingSample,
  interpolateSample,
  mapChartCoordinates,
  movePhasePoint,
  moveRecordingSample,
  projectPhases,
  removePhasePoint,
  removeRecordingSample,
  validateProfile,
} from "./editor-core.js";

const pressure = (id, bar, duration, extra = {}) => ({
  id,
  name: "",
  control: { type: "pressure", bar, location: "Pump" },
  maximumDurationMillis: duration,
  minimumDurationMillis: 0,
  ramp: { style: "Instant", durationMillis: 0, start: "PreviousTarget" },
  exitConditions: [],
  conditionOperator: "Or",
  ...extra,
});

const baseRecord = () => ({
  ...createBlankRecord("550e8400-e29b-41d4-a716-446655440000"),
  profile: {
    name: "Curve",
    description: "test",
    finishCondition: { type: "volume", target: 40 },
    autoLinkOpen: true,
    singleDoseSettings: { enabled: true, grindingSize: 300, grindingSpeed: 500 },
    program: { type: "phases", version: 1, phases: [pressure("a", 3, 10000)] },
  },
});

test("blank and cloned profiles keep valid catalog defaults and fresh identities", () => {
  const blank = createBlankRecord("new-id");
  assert.equal(blank.id, "new-id");
  assert.deepEqual(blank.profile.program.phases, []);
  assert.match(validateProfile(blank.profile).join(" "), /Name is required/);

  const source = baseRecord();
  const clone = cloneRecord(source, "clone-id");
  assert.equal(clone.id, "clone-id");
  assert.equal(clone.favorite, false);
  assert.equal(clone.deleted, false);
  assert.equal(clone.profile.name, "Curve copy");
  assert.equal(clone.profile.autoLinkOpen, true);
  assert.deepEqual(clone.profile.program, source.profile.program);
});

test("phase projection uses phase-time limits and marks sensor exits as variable", () => {
  const program = {
    type: "phases",
    version: 1,
    phases: [
      pressure("timed", 3, 20000, {
        exitConditions: [{ metric: "PhaseTime", comparison: "Above", threshold: 8, scope: "Total" }],
      }),
      pressure("sensor", 7, 15000, {
        exitConditions: [{ metric: "PumpFlow", comparison: "Above", threshold: 2, scope: "Total" }],
      }),
    ],
  };
  const segments = projectPhases(program);
  assert.equal(segments[0].endMs, 8000);
  assert.equal(segments[1].startMs, 8000);
  assert.equal(segments[1].variable, true);
});

test("chart coordinate mapping clamps time and maps independent pressure or flow scales", () => {
  const bounds = { left: 50, right: 750, top: 20, bottom: 160 };
  assert.deepEqual(mapChartCoordinates(400, 90, bounds, { durationMs: 30000, maxValue: 12 }), { timeMs: 15000, value: 6 });
  assert.deepEqual(mapChartCoordinates(-10, 180, bounds, { durationMs: 30000, maxValue: 12 }), { timeMs: 0, value: 0 });
  assert.deepEqual(mapChartCoordinates(900, -5, bounds, { durationMs: 30000, maxValue: 12 }), { timeMs: 30000, value: 12 });
  assert.throws(() => mapChartCoordinates(1, 1, { left: 1, right: 1, top: 0, bottom: 1 }, { durationMs: 1000 }), /positive/);
});

test("inserting a point splits a linear phase and reconnects both neighboring segments", () => {
  const program = {
    type: "phases",
    version: 1,
    phases: [
      pressure("a", 3, 10000),
      pressure("b", 9, 20000, { ramp: { style: "Linear", durationMillis: 10000, start: "PreviousTarget" } }),
    ],
  };
  const updated = insertPhasePoint(program, 1, 15000, 6, () => "inserted");
  assert.deepEqual(updated.phases.map((phase) => phase.maximumDurationMillis), [10000, 5000, 15000]);
  assert.deepEqual(updated.phases.map((phase) => phase.control.bar), [3, 6, 9]);
  assert.equal(updated.phases[1].ramp.durationMillis, 5000);
  assert.equal(updated.phases[2].ramp.durationMillis, 5000);
  assert.deepEqual(projectPhases(updated).map((segment) => segment.endMs), [10000, 15000, 30000]);
  assert.deepEqual(program.phases.map((phase) => phase.id), ["a", "b"]);
});

test("splitting a phase-time exit moves the remaining deadline to the second segment", () => {
  const program = {
    type: "phases",
    version: 1,
    phases: [pressure("timed", 8, 12000, {
      exitConditions: [{ metric: "PhaseTime", comparison: "Above", threshold: 10, scope: "Total" }],
    })],
  };
  const updated = insertPhasePoint(program, 0, 4000, 5, () => "inserted");
  assert.equal(updated.phases[1].exitConditions[0].threshold, 6);
  assert.deepEqual(projectPhases(updated).map((segment) => segment.durationMs), [4000, 6000]);
});

test("interior points in sensor/adaptive phases stay protected", () => {
  const program = {
    type: "phases",
    version: 1,
    phases: [pressure("sensor", 6, 10000, {
      exitConditions: [{ metric: "PumpPressure", comparison: "Above", threshold: 8, scope: "Total" }],
    })],
  };
  assert.throws(() => insertPhasePoint(program, 0, 5000, 4, () => "id"), /fixed-time/);
  assert.throws(() => movePhasePoint(program, 0, { endMs: 7000, value: 5 }), /trigger boundary fixed/);
  const targetEdit = movePhasePoint(program, 0, { value: 5 });
  assert.equal(targetEdit.phases[0].control.bar, 5);
  assert.deepEqual(targetEdit.phases[0].exitConditions, program.phases[0].exitConditions);

  const adaptive = {
    type: "phases",
    version: 1,
    phases: [
      { ...pressure("before", 3, 5000), control: { type: "flow", millilitresPerSecond: 2 } },
      { ...pressure("probe", 6, 8000), control: { type: "flow", millilitresPerSecond: 5 }, ramp: { style: "Linear", durationMillis: 3000, start: "PreviousTarget" }, adaptiveFlowProbe: { samplingDurationMillis: 2000, pressureCeilingBar: 9, flowBackoffMillilitresPerSecond: 0.1 } },
      { ...pressure("after", 3, 5000), control: { type: "flow", millilitresPerSecond: 2 } },
    ],
  };
  assert.throws(() => insertPhasePoint(adaptive, 1, 9000, 4, () => "id"), /fixed-time/);
  assert.throws(() => removePhasePoint(adaptive, 1), /Triggered phases/);
  const adaptiveEdit = movePhasePoint(adaptive, 1, { value: 4 });
  assert.equal(adaptiveEdit.phases[1].control.millilitresPerSecond, 4);
  assert.deepEqual(adaptiveEdit.phases[1].adaptiveFlowProbe, adaptive.phases[1].adaptiveFlowProbe);
});

test("moving a fixed-time point changes its target and redistributes adjacent time", () => {
  const program = {
    type: "phases",
    version: 1,
    phases: [
      pressure("a", 3, 10000),
      pressure("b", 6, 10000, { ramp: { style: "Linear", durationMillis: 10000, start: "PreviousTarget" } }),
      pressure("c", 9, 10000, { ramp: { style: "Linear", durationMillis: 10000, start: "PreviousTarget" } }),
    ],
  };
  const updated = movePhasePoint(program, 1, { endMs: 18000, value: 7 });
  assert.equal(updated.phases[1].control.bar, 7);
  assert.equal(updated.phases[1].maximumDurationMillis, 8000);
  assert.equal(updated.phases[2].maximumDurationMillis, 12000);
  assert.equal(projectPhases(updated)[2].endMs, 30000);
});

test("moving a point preserves the total estimate for PhaseTime-based fixed phases", () => {
  const program = {
    type: "phases",
    version: 1,
    phases: [
      pressure("a", 3, 10000),
      pressure("b", 8, 12000, {
        exitConditions: [{ metric: "PhaseTime", comparison: "Above", threshold: 8, scope: "Total" }],
      }),
    ],
  };
  const updated = movePhasePoint(program, 0, { endMs: 12000, value: 4 });
  assert.equal(updated.phases[0].maximumDurationMillis, 12000);
  assert.equal(updated.phases[1].exitConditions[0].threshold, 6);
  assert.deepEqual(projectPhases(updated).map((segment) => segment.endMs), [12000, 18000]);
});

test("moving a fixed boundary before a conditional phase shifts its estimate without changing its rules", () => {
  const program = {
    type: "phases",
    version: 1,
    phases: [
      pressure("a", 3, 10000),
      pressure("sensor", 8, 12000, {
        exitConditions: [{ metric: "PumpFlow", comparison: "Above", threshold: 2, scope: "Total" }],
      }),
    ],
  };
  const updated = movePhasePoint(program, 0, { endMs: 12000, value: 4, timeMode: "resize" });
  assert.equal(projectPhases(updated)[1].startMs, 12000);
  assert.equal(updated.phases[1].maximumDurationMillis, 12000);
  assert.deepEqual(updated.phases[1].exitConditions, program.phases[1].exitConditions);
});

test("removing an eligible interior point merges the adjacent ramp", () => {
  const program = {
    type: "phases",
    version: 1,
    phases: [pressure("a", 3, 10000), pressure("b", 6, 5000), pressure("c", 9, 15000)],
  };
  const updated = removePhasePoint(program, 1);
  assert.deepEqual(updated.phases.map((phase) => phase.id), ["a", "c"]);
  assert.equal(updated.phases[1].maximumDurationMillis, 20000);
  assert.deepEqual(updated.phases[1].ramp, { style: "Linear", durationMillis: 20000, start: "PreviousTarget" });
  assert.equal(projectPhases(updated).at(-1).endMs, 30000);
});

test("removing a point preserves the absolute endpoint of a following PhaseTime phase", () => {
  const program = {
    type: "phases",
    version: 1,
    phases: [
      pressure("a", 3, 10000),
      pressure("b", 6, 5000),
      pressure("c", 9, 10000, {
        exitConditions: [{ metric: "PhaseTime", comparison: "Above", threshold: 6, scope: "Total" }],
      }),
    ],
  };
  const updated = removePhasePoint(program, 1);
  assert.equal(updated.phases[1].exitConditions[0].threshold, 11);
  assert.equal(projectPhases(updated).at(-1).endMs, 21000);
});

const recording = {
  controlMode: "Pressure",
  samples: [
    { elapsedMillis: 0, data: { pressure: 0, flowRate: 0, volume: 0, weight: 0, weightRate: 0 } },
    { elapsedMillis: 1000, data: { pressure: 8, flowRate: 2, volume: 3, weight: 2, weightRate: 1 } },
  ],
};

test("recording point insertion interpolates every measurement and keeps sample order", () => {
  assert.deepEqual(interpolateSample(recording, 500), {
    elapsedMillis: 500,
    data: { pressure: 4, flowRate: 1, volume: 1.5, weight: 1, weightRate: 0.5 },
  });
  const updated = insertRecordingSample(recording, 500);
  assert.deepEqual(updated.samples.map((sample) => sample.elapsedMillis), [0, 500, 1000]);
  assert.throws(() => insertRecordingSample(recording, 0), /anchored/);
  assert.throws(() => insertRecordingSample(recording, 1000), /already a sample/);
});

test("recording point edits preserve the anchor and strictly increasing timestamps", () => {
  const moved = moveRecordingSample(recording, 1, { elapsedMillis: 2000, channel: "pressure", value: 9 });
  assert.equal(moved.samples[0].elapsedMillis, 0);
  assert.equal(moved.samples[1].elapsedMillis, 2000);
  assert.equal(moved.samples[1].data.pressure, 9);
  assert.throws(() => moveRecordingSample(recording, 0, { elapsedMillis: 100, channel: "pressure", value: 1 }), /anchored/);
  assert.deepEqual(removeRecordingSample(recording, 1).samples, [recording.samples[0]]);
});

test("profile validation preserves all profile fields through JSON round-trip", () => {
  const record = baseRecord();
  record.profile.program.phases[0].exitConditions = [
    { metric: "PumpedVolume", comparison: "Above", threshold: 40, scope: "Total" },
  ];
  assert.deepEqual(validateProfile(record.profile), []);
  const roundTrip = JSON.parse(JSON.stringify(record));
  assert.deepEqual(roundTrip.profile, record.profile);
});

test("profile validation rejects empty, malformed, and non-monotonic programs", () => {
  const profile = baseRecord().profile;
  profile.program.phases = [];
  assert.match(validateProfile(profile).join(" "), /at least one phase/);
  profile.program = { type: "recording", version: 1, recording: { controlMode: "Flow", samples: [
    { elapsedMillis: 0, data: { pressure: 0, flowRate: 0, volume: 0, weight: 0, weightRate: 0 } },
    { elapsedMillis: 0, data: { pressure: 0, flowRate: 0, volume: 0, weight: 0, weightRate: 0 } },
  ] } };
  assert.match(validateProfile(profile).join(" "), /increase strictly/);
});
