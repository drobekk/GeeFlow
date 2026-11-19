# Tablet workspace v04

Continues the local custom/dashboard-redesign branch after tablet v03. Keep this branch local until publication is requested.

## Dashboard

The first visible chart owns the seconds axis, so adding a third chart does not hide the time reference. The footer time caption is removed. A non-interactive profile scroll indicator stays visible when there are hidden items and follows the existing LazyListState.

Flush still dispatches StartManualBrewingUseCase. Its configured duration is in the idle action label. The running state shows the manual cycle pressure, elapsed time, duration, and progress, with Stop flush using the existing StopBrewingUseCase. Zero-duration Demo cycles now run until explicitly stopped. Treat-as-flush exclusions remain intact; retained shot charts and readouts are identified as historical while a flush is active.

## Shot origin correction

ObserveBrewDataUseCase used an old telemetry timestamp when a new native brew began. After idle time, the next shot included that gap. Session origin now prefers execution-trace start, then status timestamp, then telemetry timestamp. Measurements older than the session origin are excluded rather than converted into fabricated zero samples. The Demo controller emits a fresh zero sample with the same timestamp as its new brewing status, and resets its time, flow, and totals.

## Related screens

ClearScreenTheme centralizes the dashboard palette, preserves light/dark mode, and is applied to the machine workspace navigation. The navigation background is painted explicitly so pane gaps do not expose the old warm surface. Existing pane scaffolds gain an opt-in plain header; callers outside this customization keep their defaults.

Quick Settings retains its existing boiler input, power, Confirm, and More settings routes. Device settings uses white groups, compact headers, and the existing adaptive list-detail navigation. Brewing groups stack when the detail pane is less than 760 dp wide.

Manual control retains the existing ViewModel, control modes, constraints, device capabilities, live target updates, recording, stop-on-exit, and saving routes. The presentation now uses shared measurement cards, standard labeled controls, a target slider and 0.1-step buttons, chart selection dialog, rename action, and Save profile. It shares the stored flow-range preference with the dashboard.

## Verification and boundaries

Android assembly succeeds. Common-source Detekt passes for core UI, brew domain, dashboard, device settings, and app. 110 affected tests pass: brew domain 51, selected Demo controller suites 18, dashboard 37, and device settings 4. Seven new regression cases cover shot origins, Demo flush behavior, repeated 0.1 target steps, and device limits.

Manual emulator checks cover 1280 x 800 and 1024 x 640 viewports, the first seconds axis with three charts, profile scrolling, repeated Cremina starts, Flush progress and completion, setting input dialogs, Pressure and Water flow manual modes, target adjustment, chart choices, rename dialog, Start/Stop, and Save availability after recording. No physical LITA BA was tested, and no machine protocol or profile serialization changes are included.

## Upstream merge notes

Review changes to ObserveBrewDataUseCase, DemoDeviceController, RootNavigation, pane scaffolds, ClearScreenTheme, and the dashboard/settings presentation modules when taking upstream updates. The versioned workspace folder GeeFlow screens refinement v04 contains the installed APK, image evidence, test manifest, and build logs. Prior customization guides and output versions remain intact.
