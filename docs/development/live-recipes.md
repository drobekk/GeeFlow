# Local live recipes

Live recipes are data interpreted by `LiveRecipeEngine`. The engine never checks a recipe's title or ID. Cremina, Adaptive v2 and the synthetic recipe are acceptance fixtures, not special runtime implementations.

## Decent profile authoring

The normal new live-recipe entry point authors Decent JSON v2 directly. Stage fields use source units and preserve numeric strings, inactive targets, zero-duration steps, and source temperature and sensor choices. Export writes the source object. Android stores it locally with its compiled representation and re-derives the execution snapshot for the run panel. Earlier local live records retain their original entry path.

The run panel shows the LITA mappings and the constant boiler temperature. The default comes from the final timed stage rounded to whole degrees. A run-only override never changes the Decent file. Stage durations convert from seconds to scheduler milliseconds. Outgoing pressure targets reuse the LITA capability quantization and free-variable Modbus register scaling (10 register units per bar). Temperature is a whole-degree register written while idle if the setpoint needs to change.

The static web editor in `tools/profile-library-sync/decent-editor.html` uses local JSON files. It does not call the catalog API.

## Storage and execution

- Legacy `BrewProfile` records and their existing native/recording paths remain intact. The editor's **Live copy** action takes the current editor draft and creates a separately stored document.
- Android stores live documents in the `local_live_recipe_library_v01` SharedPreferences collection. This collection is not part of the existing profile synchronization repository. Corrupt or newer collections cannot be overwritten by a save.
- The library displays local live documents alongside legacy profiles. The live editor supports file/text JSON import, file/clipboard export, phases, target sources, ramps, captures, exits, limits and controller settings. Invalid documents may be saved as drafts; execution requires successful compilation.
- The compiler serializes and decodes a detached snapshot. Android writes the execution snapshot to an app-private file and passes only its generated filename to the foreground service, avoiding Binder payload limits for retained source JSON. The service consumes that snapshot rather than reconstructing a default recipe. Process death never resumes it.
- The original imported JSON and source identity are preserved. Source mappings must be accepted before live activation. Unknown source execution fields block execution. Decent imports use LITA pump telemetry for pressure and translate limiter ranges to bounds. The Decent coffee/water sensor choice belongs to temperature control and is preserved in the source.

## Document contract

`LiveRecipeDocument.version` is currently 1. Unknown fields in GeeFlow documents and unsupported versions are rejected. Each document has a stable local ID, name, ordered phases, shot settings, and optional source metadata.

Controls use distinct units: pressure in bar, machine flow in mL/s, cup flow in g/s, and pause. Targets and ramp starts may be fixed, use a current measurement, use the previous timed phase's target, or reference a named capture from an earlier phase. Capture references are checked for order and unit compatibility.

For example, a pressure phase can capture `MachineFlow` as `pressurize-flow`. A later machine-flow phase can use this start value:

```json
{
  "rampStart": {
    "source": "Capture",
    "captureId": "pressurize-flow"
  }
}
```

The measured value and its monotonic sample time are recorded at phase exit. Historical captures remain fixed for that shot. They are not re-read from a later sample, replaced with a constant, or carried into another shot. A zero-duration phase is a transition action and produces no target write.

Pressure, machine flow, cup flow, cup weight, pumped volume and phase time can drive exits. Weight and volume support total or step-relative measurement. AND/OR combines measurement exits; time exits and maximum phase duration act as deadlines. Overall weight, optional overall volume and the shot deadline are independent stop conditions.

Shot settings hold yield mode, dose, ratio, explicit weight, constant whole-degree temperature, overall deadline, pressure bounds, pressure slew, command interval and separate machine/cup PI gains. Yield changes do not rescale phases. A blank temperature retains the existing machine setpoint; a configured value is applied once while idle. Source per-stage temperatures are reference metadata only.

## Runtime constraints

- Every app-controlled target sent to LITA is a pressure target. Native live pressure/flow switching remains disabled because a register acknowledgement does not prove that the active regulator switched.
- Each flow basis has its own integral state. PI integration advances only with a fresh measurement stamp. Controller state resets on phase handover, fallback and stop. Phase bounds, global bounds, slew and machine quantization constrain output.
- The legacy adaptive-flow probe is represented as a generic machine-flow probe. It learns measured flow below a pressure ceiling, holds the learned maximum and backs off when pressure reaches the ceiling.
- Scale samples and machine samples are processed independently of machine command acknowledgements. Scale regression uses arrival timestamps, a trailing 500 ms window, at least three samples and a span of at least 200 ms. The control loop consumes the latest estimates. BLE arrival time is not the scale's internal sensor sampling time.
- Scale data older than one second or timer/tare/cup-removal invalidation latches fallback for the remainder of the shot. Pressure falls to the lower of 3 bar and the current target, then stop occurs within five seconds or at the shot deadline. Fresh scale samples cannot resume feedback. Machine telemetry loss, command failure and disconnection enter stop handling. Ownership remains reserved until fresh connected machine status confirms idle.
- Source pressure bounds up to the machine's reported capability can be represented and evaluated in shadow/simulation. Live activation above 9 bar is blocked pending attended hardware validation. The source values remain in the document and are not silently clamped to a different recipe.

## Evidence and limitations

Capture JSONL contains the immutable recipe snapshot, individual sensor measurements, raw packets/frames, captured values, controller output and command events. CSV includes requested control value/unit, requested pressure, commanded pressure and active constraints. In-memory capture retention is bounded at the existing 12,000-event/2,000-sample limits; export after each trial. Synthetic captures are labelled and do not demonstrate hydraulic behavior.

BOOKOO numeric sign polarity remains provisional until confirmed on the actual scale. Constant boiler setpoints do not reproduce DE1 coffee-temperature control. The gains are unvalidated simulation defaults. A phone without a working machine connection cannot guarantee a physical stop. Simultaneous BLE connections, background operation, notification Stop, hydraulic response and tuning require separate attended validation.

The fixture provenance is pinned in `shared/domain/device/src/jvmTest/resources/live_recipes/source_manifest.json`. No source JSON v2 stage is identified by name in the interpreter. The importer's cross-control transition handling is an explicit LITA translation, not a verified implementation of DE1 firmware internals.
