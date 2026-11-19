# LITA BA Demo and Chinese UI v06

This local customization follows `aed2455` on `custom/dashboard-redesign`. No public fork or push is required.

## Demo model

`DemoDeviceController` now uses `wendougeeCapabilities` and `wendougeeConstraints`, matching the current LITA BA controller. Pressure and flow profiling remain available. Hot-water dispensing is absent and its control is `DeviceHotWaterControl.Unsupported`.

The device identity remains `SupportedDevice.GeeFlowDemo`. `DefaultDeviceControllerFactory` still creates the in-process Demo controller for that identity. Existing Demo records keep working and cannot become physical devices. New Demo records use the name `LITA BA · Demo`; the default name is localized in the custom header. Custom device names remain unchanged.

`DemoLitaBaTest` compares all capabilities, constraints and profiling support with `WendougeeController(LitaBA)`, rejects hot-water control, and verifies that a BA-labelled Demo is still virtual. This test should flag divergence if a future upstream change alters BA behavior.

The simulation does not certify physical BA hydraulics, firmware behavior or Bluetooth connectivity.

## Chinese presentation

Shared custom labels use `ClearText` and normal Compose resources in `shared/core/ui`. Chinese translations now cover every resource key in core UI, dashboard and device settings. Earlier dashboard additions had 35 missing Chinese strings; those fallbacks are filled.

Twelve stock recipe descriptions are translated at presentation time in `BrewProfileDisplay`, matching known original descriptions exactly. Saved profiles, profile names, user descriptions and machine communication formats remain unchanged. Generic chart labels such as Step 1 are localized while custom phase names are retained.

On Android 13 and newer, `MainActivity` defaults an empty application locale to `zh-CN` once, tracked in app preferences. An explicit existing application locale is respected. Older Android versions follow the system language. No system-wide language setting is changed. The debug launcher label is `GeeFlow LITA BA`; the application ID stays `app.geeflow.debug` and signing uses the existing development key.

## Verification and distribution

The Android APK build, five CommonMain Detekt checks, 149 device-data JVM tests and 42 dashboard JVM tests pass. Resource XML and substitution placeholders have been checked. The APK declares minimum API 28 and includes arm64-v8a, armeabi-v7a, x86 and x86_64 libraries. This is a signed development APK, not a Play Store release.

Artifacts, emulator screenshots, APK checksum, verification record and a physical-Samsung USB installer helper are in `/Users/liang/Documents/Personal projects/Wendougee coffee app/GeeFlow LITA BA Chinese v06/`.

The USB helper skips emulators, verifies the APK checksum, selects an authorized physical Samsung, checks Android compatibility and runs `adb install -r`. It does not wipe app data, accept terms or issue machine brew commands. Review its installation guide for first launch, language and Bluetooth steps.

## Machine connection

The physical machine route is `WendougeeController` → `WendougeeBleTransport`, with Modbus framing handled by `ModbusSession`. The existing Bluetooth permission and transport code is unchanged by v06. A real connection has not been exercised against the user's machine.
