# GeeFlow profile library

The private HK library stores portable profiles and favorites. Local database IDs, selected users and list positions stay on each device. The repository contains the editor and API implementation; deployment details are recorded with each release milestone.

## Catalog and authenticated API

Catalog schema 1 stores legacy programs. Schema 2 also stores conditional recipe programs, original Decent JSON, template phases, independent dose/yield settings and original recordings. The updated service and app accept both versions. Unknown execution fields and unsupported versions fail validation. A schema-2 catalog cannot be overwritten by a schema-1 write.

- `GET /profile-library/api/v1/health` returns service status without profile data.
- `GET /profile-library/api/v1/catalog` requires `Authorization: Bearer <library token>`.
- `PUT /profile-library/api/v1/catalog` requires the bearer token, full catalog and current revision in `If-Match`.

The existing API URL stays `/api/v1/` for both catalog formats. Accepted writes increment catalog and changed-item revisions. Retain UUIDs and deletion tombstones. The service uses atomic replacement under a process lock. Tokens and request bodies are excluded from access logs. The browser token stays in tab memory. Local recovery storage contains the draft and baseline, without the token. Exported drafts and simulation traces contain recipe data.

## Editing

Select a profile directly from the sidebar. Create a blank draft with New, or open Import for templates, Decent files and saved drafts. Recipe settings, duplicate, export and source details are in the profile menu. New drafts need a valid phase or recording before saving. Delete marks a catalog record as a tombstone so GeeFlow sync removes it from devices without discarding its identity or recipe data.

Recipe settings let you choose GeeFlow app control or native LITA execution. App control stores the program as version 2. LITA is available only when the program validates for native execution on the selected LITA model; that version-1 program is uploaded to the machine and runs there. Finish settings support cup weight in grams or pumped volume in millilitres, with the target field relabelled to match.

Pressure and flow share a time axis and have separate scales. Each phase commands one mode. Conditional intervals are planned-time estimates; computed targets need simulation. Phase names and durations appear below the charts. Selecting a phase opens its compact inspector. Unknown measured starts remain undrawn until supplied explicitly. The phase-local view helps inspect short conditional intervals.

Click a fixed curve to split it at its current value. Eased curves require an explicit conversion to Linear first. Target/ramp handles and phase deadlines are separate. Dragging a point changes its time and target. Dragging a square phase edge changes duration and shifts later phases without changing the targets. Clicking a point opens numeric time/value editing. Trigger-dependent times stay locked. Triggered phases retain their rules during point edits. Removing a whole triggered phase is a separate confirmation with undo. Numeric fields, snapping, phase insertion, duplication, reordering and undo/redo are available.

Recording edits anchor the first timestamp at zero and require increasing timestamps. Insertion interpolates every measurement. The original recording is retained on the first derived edit. Legacy app replay holds the previous sample; native replay uses the machine's 500 ms resampling. Explicit v2 Hold/Linear replay is separate from raw telemetry. Native capacity is calculated after resampling.

Save is explicit. A revision conflict preserves the draft and blocks saving until a fresh catalog read. The editor merges independent changes and asks for every overlapping choice. Reorders are handled conservatively. Export or clone remains available. Tablet metadata edits preserve advanced programs; their phase editing stays in this web editor.

## Controller and feedback contract

A running recipe always selects exactly one controller: pressure or flow. The other commanded target is zero. Selecting a zero target still retains that controller. The inactive channel is not a second independent curve. Recorded/measured pressure and flow remain separate and are not zeroed.

Each phase stores a single tagged control. App engine outputs expose `ControlTargets`, and browser preview rows expose `controlTargets`, with an explicit controller and both numerical targets. The inactive value is zero. Native phase compilation already emits pressure/flow steps with zero in the other target field. Native recording uploads retain the original sensor banks and select one stream through the existing control-mode flag; inactive sensor-bank values are not treated as an additional commanded target, and the established upload format is unchanged. The live command path selects one mode and sends that mode's target; it does not send competing zero commands through the other mode.

Legacy waits are accepted for compatibility and resolve to a zero target through the preceding controller. A leading wait uses the first following controller, or pressure when the entire legacy program contains waits. The web editor converts saved wait phases into explicit pressure/flow zero-target phases while retaining their timing, IDs and exit rules. New phase controls offer pressure and flow only. Original imported source and recorded sensor data remain intact.

For the non-controller parameter, choose Soft limit or Range in the phase's feedback controls. Pressure control monitors flow; flow control monitors pump pressure. A soft upper limit reduces the active target on excess readings and permits bounded recovery toward the requested target after the measurement drops below the hysteresis band. A range adds an optional `secondaryLimit.minimum`: above its maximum the active target decreases, inside the inclusive band it holds, and below its minimum it recovers toward the requested target. Recovery never exceeds that requested target. Feedback does not switch controllers. Ordered bounds, opposing metric and fresh readings are validated in the browser, service and app.

Example: pressure goal 9 bar with a flow range of 1.5–2.5 ml/s. Flow above 2.5 lowers the pressure command, readings within the range hold the adjusted pressure, and readings below 1.5 allow recovery up to 9 bar. The commanded flow target stays zero throughout. The flow measurement continues to drive the feedback.

These are reactive soft constraints, not guaranteed hardware ceilings. Existing stale-data, sustained-overshoot, deadline and unverified-device gates remain in force. Update both the service and app before deploying recipes with the new `minimum` field; older strict clients reject unknown execution fields.

## Conditional recipes and simulation

For ramps, Start from selects a fixed value, measured entry value or the measurement when an earlier phase ends. Earlier-phase choices create or reuse the capture automatically; existing custom filters and references remain intact. The version upgrade is automatic when needed. Phase internals, including other typed sources, are available under Diagnostics. Entry/exit captures have a sensor, sample window, age limit, filter, bounds and optional fallback. Missing or stale values stop evaluation unless a capture has an explicit fallback. Time rules remain deadlines even with AND measurement rules and stability windows.

The bundled Cremina and Adaptive JSON files retain every source field. Conversion diagnostics explain linear approximations of Decent `smooth`, temperature support, zero-duration frames and limiter response differences. The generic preserved import is blocked from live use until its semantics are explicitly adapted. Pressure-only Cremina deliberately removes per-phase temperatures. Adaptive's explicit measured-flow variant captures filtered flow at pressurization exit and starts extraction from that capture. It is distinct from the existing learn-and-hold AdaptiveFlowProbe. These are inspectable adaptations, not verified DE1 firmware equivalents.

Dose, ratio and yield update immediately. Dose × ratio sets output weight unless a direct yield overrides it. Changing yield leaves phase targets and timings unchanged. Unset source output goals require configuration before execution.

Preview opens a separate drawer with simulated phase events and temporary command overlays. Closing it returns the chart to its planned view. Source conversion reports, capture internals, replay settings and failure-injection controls are kept out of the normal editor under Source or Diagnostics. Simulation evaluates supplied synthetic or imported sensor inputs. It does not predict puck behavior or send machine commands. JSON input has `durationMillis`, `stepMillis`, and increasing `samples` starting at zero. Each sample has `elapsedMillis`, a `values` map keyed by BrewMetric, and optional `sampledAtMillis` by metric. Optional `acknowledgementDelayMillis` and `disconnectedAtMillis` exercise failure paths. Export includes measurements, targets, captures, transitions and requested/limited/previous-applied decisions. Kotlin and browser evaluators share expected trace fixtures.

Live app execution stops within the 40,000 ms total policy through the existing stop path. A shorter deadline can be set for v2. Full-timeline simulation can exceed this deadline. Native legacy profiles retain their firmware execution route.

Secondary limits are bounded reactive simulation strategies with hysteresis, reduction, recovery and sustained-overshoot faults. They are not hardware-enforced ceilings. Live use is blocked on current LITA routes. Per-phase temperature and v2 weight rules are also blocked until thermal and timestamped-scale routes are verified. BA mode switching has code support; BR/LR flow does not. This release includes no physical-machine validation.

## Local preview and checks

```sh
python3 tools/profile-library-sync/preview_server.py
# Optional: --seed <local-catalog-copy> preserves a sandbox across restarts.
```

Open `http://127.0.0.1:8765/profile-library/` with token `preview-only`. This sandbox has a temporary empty catalog. Load the bundled sources or create drafts. Saves disappear when the process stops. It never contacts HK.

### Edit recipes from the debug app database

The local preview exposes **Open app database**. The selected SQLite file is copied into the preview process's temporary directory, and every `brew_profiles` row appears in the sidebar for chart-based inspection and editing. Save stages edits in the browser. **Download database** creates a new `.db` copy, changing only the recipe columns that were edited. Other tables, profile IDs, users and positions are retained. App favorites are stored separately in preferences and are not edited here. Closing the preview deletes its temporary database copy.

For the debug tablet build, obtain the database from a computer with ADB connected:

```sh
adb shell am force-stop app.geeflow.debug
adb exec-out run-as app.geeflow.debug cat databases/app.db > geeflow-app.db
```

Open that file in the local editor. The downloaded `geeflow-app-edited.db` is a new copy. To put it back, run these commands on the connected computer:

```sh
adb push geeflow-app-edited.db /data/local/tmp/geeflow-app-edited.db
adb shell am force-stop app.geeflow.debug
adb shell run-as app.geeflow.debug sh -c 'cp databases/app.db databases/app.db.before-edit && rm -f databases/app.db-wal databases/app.db-shm databases/app.db-journal && cp /data/local/tmp/geeflow-app-edited.db databases/app.db.edited && mv databases/app.db.edited databases/app.db'
```

The web editor itself never writes to the tablet. Keep the backup until GeeFlow opens and the recipes look right. The app may sync the edited profiles to the library after launch.

Run from the repository root:

```sh
node --test tools/profile-library-sync/*.test.mjs
python3 -m unittest discover -s tools/profile-library-sync -p 'test_*.py' -v
cd tools/profile-library-sync
npm ci
PLAYWRIGHT_CHANNEL=chrome npx playwright test
```

The browser suite checks revision conflicts and desktop/tablet pointer gestures against mocked responses, plus an integrated v2 save/remote-edit/merge round trip against the real temporary loopback service. The Python suite exercises the real authenticated local API and migration/release packaging. JVM sync tests exercise reconciliation and database serialization with v1/v2 fixtures.

```sh
JAVA_HOME=/opt/homebrew/opt/openjdk@21 ./gradlew --offline :shared:data:brew:jvmTest :shared:data:device:jvmTest :shared:domain:device:jvmTest :shared:feature:device:dashboard:jvmTest :shared:app:jvmTest
JAVA_HOME=/opt/homebrew/opt/openjdk@21 ANDROID_HOME=/Users/liang/Library/Android/sdk ANDROID_SDK_ROOT=/Users/liang/Library/Android/sdk ./gradlew --offline :androidApp:assembleDebug
```

## Release and rollback

Only deploy when explicitly requested. Prepare a new versioned local bundle with `python3 tools/profile-library-sync/package_release.py <unused-output-directory>`. It includes a source commit and per-file hashes, without a catalog or credentials.

For schema 2, update compatible clients and service before allowing v2 catalog writes. Back up the exact existing catalog and service/page assets. Prepare migration offline with `python3 migrate_catalog.py <source-copy> <new-output.json>`. This writes a byte-for-byte backup and a separate validated schema-2 catalog; it never replaces the input. If the live revision changes after that snapshot, regenerate from a fresh copy rather than overwriting concurrent changes.

Deploy every `web/` bundle asset under `/var/www/hk-site/profile-library/`, including all JS modules and `recipes/decent/*.json`. Deploy `server.py` and `program_validation.py` together beside the service. Keep credentials and the catalog outside the web root. After authorized rollout, check module hashes, authenticated readback, CAS conflict behavior and client sync. A catalog migration requires the catalog lock and a service/client write pause or equivalent revision guard.

Rollback across schema versions restores a compatible complete catalog backup, app, service and page assets together. Never strip v2 fields to force a downgrade. Backups may omit later edits; preserve those changes separately before restoring.

The service binds to loopback behind existing HK nginx/Cloudflare Tunnel. Its root-owned mode-600 environment file sets `PROFILE_LIBRARY_TOKEN`, `PROFILE_LIBRARY_DATA_FILE` (default `/var/lib/geeflow-profile-library/catalog.json`), `PROFILE_LIBRARY_BIND` (default `127.0.0.1`) and `PROFILE_LIBRARY_PORT` (default `8093`). Run `nginx -t` before any authorized nginx reload. App sync retries on launch, profile/favorite changes and every 15 minutes. The app token remains in the existing preferences store; use a dedicated library token.
