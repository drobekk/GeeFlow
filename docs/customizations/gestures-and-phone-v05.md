# Gesture controls and phone layout v05

Local-only customization on `custom/dashboard-redesign`, following commit `718dfc6`.

## Interaction changes

- Manual control uses a large vertical gesture pad. Touching the pad preserves the target. Relative upward motion increases it; downward motion decreases it. Releasing holds the target. Stop remains a separate action.
- Fine mode reduces sensitivity to one quarter. Plus and minus adjust by 0.1. Target limits and pressure/flow availability come from the connected device. Manual flow targets above the chart range retain their full value and show an explicit range flag. Its reserved line prevents the fader moving when the flag appears.
- Tablets keep both charts beside the fader. The tablet live values stay large. Phones below 600 dp use a compact live strip and separate Control / Shot charts views with a persistent Start / Stop action.
- Continuous target writes replace the old 200 ms debounce. A serial sender sends immediately, retains only the latest pending target, and waits 200 ms between completed writes. Stop, mode changes, disposal and disconnection discard pending targets. Existing start, stop and target use cases still own machine commands.
- Flush opens an automatic modal over the charts, showing configured target pressure, reported elapsed time and configured duration. It remains open until the machine reports that flushing ended. Untimed cycles show explicit text. If a disconnected cycle is still reported, Open devices provides a reconnect path. Its red Stop spans the panel and is 64 dp high. The old inline progress row no longer compresses the dashboard Stop button.
- Phone dashboard readings use two equal columns. Labeled Menu actions retain Clean, Devices, Settings and the existing steam toggle. Profiles remain a separate page with search, favorites and one clipped filter row.
- Narrow charts use fewer time divisions. Phase boundary lines are retained while labels that would collide are omitted. Narrow boiler cards show Current and Target on separate rows.

## Source map

All changes are in `shared/feature/device/dashboard`.

- `freecontrol/ManualGesturePad.kt`: relative touch handling, progress semantics and display.
- `freecontrol/ManualTarget.kt`: limits, rounding and relative motion math.
- `freecontrol/ManualTargetSender.kt`: serial, conflated target dispatch.
- `freecontrol/FreeControlScreen.kt`, `FreeControlViewModel.kt`: responsive control screen and existing use cases.
- `components/FlushOverlay.kt`, `DashboardBrewButton.kt`: modal flush and removal of squeezed inline progress.
- `components/PhoneBrewControls.kt`, `TopBar.kt`, `ExtractionReadout.kt`, `main/DeviceDashboardScreen.kt`: phone presentation.
- `components/BrewCharts.kt`, `BrewPhaseDecoration.kt`: narrow chart readability.
- `quicksettings/QuickSettingsScreen.kt`: narrow boiler cards.

Profile storage, machine formats, LITA capabilities and controller protocols are unchanged. LITA BA keeps pressure and flow controls and does not gain hot-water support from the Demo presentation.

## Verification

APK build and dashboard CommonMain Detekt pass. All 42 dashboard JVM tests pass, including seven manual-target tests. The new tests cover a 30-second uninterrupted input stream, final-target delivery, serial slow writes, cancellation, relative pickup, sensitivity and limits.

Android SDK Demo checks cover 1280 x 800 dp and 1024 x 640 dp tablets, plus an S24+ proportion portrait viewport at about 411 x 891 dp. Screenshots come from the real MainActivity and Demo controller. No physical LITA or Samsung hardware was connected. Phone landscape and other GeeFlow screens outside this scope have not been certified by this change.

Artifacts are in `/Users/liang/Documents/Personal projects/Wendougee coffee app/GeeFlow gesture and phone refinement v05/`.

## Reviewing future upstream changes

The original project remains the `upstream` remote. This change is local only. To review against the currently stored upstream reference:

```sh
git log --oneline upstream/main..custom/dashboard-redesign
git diff upstream/main...custom/dashboard-redesign -- shared/feature/device/dashboard
```

Refresh upstream before a future merge when authorized. Review conflicts in the screen/view model and chart files, especially the continuous sender, connection checks, start/stop paths and model capabilities. Run the dashboard JVM tests, CommonMain Detekt and Android build, then repeat the actual gesture and flush checks after merging.
