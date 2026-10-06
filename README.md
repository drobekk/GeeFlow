[![Project Health Checks](https://github.com/drobekk/GeeFlow/actions/workflows/project-health.yml/badge.svg)](https://github.com/drobekk/GeeFlow/actions/workflows/project-health.yml)
[![Version](https://img.shields.io/github/v/release/drobekk/GeeFlow?label=version)](https://github.com/drobekk/GeeFlow/releases/latest)
[![Kotlin](https://img.shields.io/badge/dynamic/toml?url=https%3A%2F%2Fraw.githubusercontent.com%2Fdrobekk%2FGeeFlow%2Fmain%2Fgradle%2Flibs.versions.toml&query=%24.versions.kotlin&label=kotlin&color=blue)](./gradle/libs.versions.toml)
[![License](https://img.shields.io/badge/license-GPL--3.0-blue.svg)](./LICENSE)

![Android](https://img.shields.io/badge/platform-android-brightgreen.svg?style=flat)
![iOS](https://img.shields.io/badge/platform-ios-lightgrey.svg?style=flat)
![JVM](https://img.shields.io/badge/platform-jvm-orange.svg?style=flat)

<p align="center">
  <img src="./docs/Banner.png" alt="GeeFlow Banner" width="100%">
</p>

# GeeFlow

A Kotlin Multiplatform app for controlling and monitoring espresso machines over BLE, targeting **Android**, **iOS**, and **Desktop (JVM)**.

<p align="center">
  <a href="https://apps.apple.com/us/app/geeflow/id6813844388">
    <img src="./docs/appstore-badge.png" alt="Download on the App Store" height="55">
  </a>
  &nbsp;&nbsp;
  <a href="https://play.google.com/store/apps/details?id=app.geeflow">
    <img src="./docs/google-play-badge.png" alt="Get it on Google Play" height="55">
  </a>
</p>

<p align="center">
  <a href="https://github.com/drobekk/GeeFlow/releases/latest">Download for Windows and macOS</a>
</p>

## Screenshots

### Tablet

<p align="center">
  <a href="https://drobekk.github.io/GeeFlow/tablet-1.png"><img src="https://drobekk.github.io/GeeFlow/tablet-1.png" width="220" alt="Dashboard on tablet" /></a>
  <a href="https://drobekk.github.io/GeeFlow/tablet-2.png"><img src="https://drobekk.github.io/GeeFlow/tablet-2.png" width="220" alt="Profile Editor on tablet" /></a>
  <a href="https://drobekk.github.io/GeeFlow/tablet-3.png"><img src="https://drobekk.github.io/GeeFlow/tablet-3.png" width="220" alt="Step Editor on tablet" /></a>
</p>

### Phone

<p align="center">
  <a href="https://drobekk.github.io/GeeFlow/phone-1.png"><img src="https://drobekk.github.io/GeeFlow/phone-1.png" width="140" alt="Dashboard on phone" /></a>
  <a href="https://drobekk.github.io/GeeFlow/phone-2.png"><img src="https://drobekk.github.io/GeeFlow/phone-2.png" width="140" alt="Profile Editor on phone" /></a>
  <a href="https://drobekk.github.io/GeeFlow/phone-3.png"><img src="https://drobekk.github.io/GeeFlow/phone-3.png" width="140" alt="Step Editor on phone" /></a>
</p>

## Supported Hardware

Right now the only real machine supported and tested is the **Wendougee Data-S**. Other machines are not tested yet, the protocol is machine-specific and nothing else has been tried. It might be working on LITA machines.

The **Wendougee smart grinder** is not supported yet, simply because I do not own one to reverse engineer and test against. Only the smart scale is wired up on the accessory side.

A built-in **Demo** device is also available for preview purposes. It simulates a machine entirely in software, so you can explore the whole app (dashboard, profiles, charts, history) without owning any hardware. Add it from the *Add Device* screen.

> [!WARNING]
> **This is an unofficial, third-party application.** While it utilizes the manufacturer's native communication protocol, the integration was independently reverse-engineered and is not endorsed, certified, or supported by the manufacturer.
>
> Because this application sends direct hardware commands (including heating targets, pressure profiles, and cleaning cycles), unforeseen bugs or firmware incompatibilities could cause your machine to operate incorrectly.
>
> **By using this software, you acknowledge and agree that:**
> - You use it entirely at your own risk.
> - It may cause unintended behavior, damage your machine, or void your warranty.
> - You must **never** leave the machine unattended while it is under app control.

### Error Handling Limitation

The error codes reported by the machine have **not** been mapped. The only fault the app currently recognizes and surfaces is the **low water level alarm**. Every other error condition the machine can report is unknown to the app and will pass silently. The app will not warn you, and may keep issuing commands as if nothing were wrong. If the machine behaves unexpectedly, check it with the manufacturer's own app, which can read the fault codes this app cannot.

## Features

**Brewing**
- Manual brewing with configurable time and pressure
- Per-machine paddle settings: treat manual brews as flushes (no history or chart) and Auto flush after brewing with a configurable delay; Auto flush requires the app to stay active and connected
- Profile brewing with multi-step pressure, flow, and wait stages (variable-pressure and constant-pressure modes)
- Experimental profiles with app-controlled ramps and measurement-based step transitions
- Freehand ("free variable") brewing - steer pressure or flow live from a control screen, and optionally save the result as a profile
- Finish conditions by target weight or target volume
- Stop a brew at any time

**Brew Profiles**
- Create, edit, duplicate, delete, and reorder profiles
- Visual profile editor with per-step editing and descriptions
- Profiles are per-user; a set of defaults ships with the app and can be restored
- Bind compatible profiles to the machine's paddle/button for native execution; once bound, edits are synced to the machine as you save them, and a failed sync aborts the save
- Save profiles even when they exceed the machine's native recording capacity or use features that cannot be bound to its paddle/button

**Experimental Profiles**
- GeeFlow runs these profiles by sending live targets and evaluating measurements throughout the brew, rather than uploading the complete profile for the machine to execute
- Use pressure or flow steps with wait steps, each with a required time limit. Combining pressure and flow in an experimental profile requires a machine that supports live control mode switching
- Choose instant, linear, ease-in, ease-out, or ease-in-out transitions for pressure and flow targets, with a configurable ramp duration
- Start a ramp from the previous target or the current measurement
- Add exit conditions for total volume, total weight, pressure, and flow; pressure and flow support upper and lower thresholds
- Use OR to advance when any measurement condition is met, or AND to require all measurement conditions to be met at the same time. The time limit always ends the step independently of AND/OR
- Weight-based conditions require a connected scale
- Experimental indicators identify features that the selected machine cannot execute natively. Available features depend on the machine's capabilities
- Experimental profiles require the app to remain active and connected during brewing and cannot be assigned to the machine's paddle/button

On the Wendougee Data-S, experimental profiles use the machine's native freehand pressure or flow control. GeeFlow sends target updates for ramps and evaluates exit conditions, while the machine regulates pressure or flow internally. Since switching the active control mode during freehand brewing is not supported, an experimental profile must use one control mode throughout, with optional wait steps. Native profiles can still combine pressure and flow steps. Reported pressure is pump pressure, not puck backpressure.

**Live Monitoring**
- Real-time dashboard: brew and steam boiler temperatures, pressure, flow rate, volume, weight, weight rate, and elapsed time
- Live charts for pressure, flow rate, weight rate, volume, and weight, each of which can be hidden or shown at will
- Pressure, pump flow, and weight rate share a chart, with weight rate hidden by default
- Profile charts show target ramps, numbered step boundaries, and exit-condition markers, including the conditions that triggered transitions during brewing
- Brew history with the recorded curve and the profile steps as they were at brew time

**Machine Control & Settings**
- Brew and steam boiler targets, with independent on/off per boiler
- Full-speed and pulse heating modes
- Manual brew time and pressure defaults
- Separate Daily Cleaning and Deep Cleaning programs with configurable flush duration, rest duration, and cycle count, plus start/stop and live cleaning status. Deep Cleaning is intended for use with cleaning detergent
- Water alarm toggle
- Quick settings and quick maintenance dialogs on the dashboard; long-press the quick settings button to toggle the steam boiler without opening anything

**Maintenance Reminders**
- Optional reminders for Daily Cleaning and Deep Cleaning, with independent intervals of 1-365 days, saved per machine
- Due or overdue reminders appear on the machine's dashboard when the app is active and the machine is connected and idle, including after reconnecting or resuming the app
- Reminders use local calendar dates, with no scheduled time or system notifications
- Open Quick Maintenance from a reminder to select and start the suggested cleaning program, or choose Skip to move its next reminder forward by the configured interval
- Pressing Start counts as cleaning for reminder purposes, even if the program later fails or is stopped. Daily Cleaning reschedules its own reminder; Deep Cleaning reschedules both
- When both reminders are due, a single reminder suggests Deep Cleaning, which also covers Daily Cleaning. Skipping it reschedules both reminders

**Smart Scale**
- Scan for, connect, and disconnect a supported Bluetooth scale
- Live weight and flow-by-weight feed into the dashboard, charts, and weight-based finish conditions

**Devices**
- Pair via QR code or by scanning for nearby BLE devices
- Multiple saved devices, with a favorite and optional auto-connect
- Connection state surfaced throughout (connecting, synchronizing, connected)

**Adaptive Layout**
- Works in both portrait and landscape, on phones, tablets, and desktop windows
- Layouts adapt to the available width rather than the platform: settings screens switch to a list-detail arrangement when there is room, and the dashboard, profile editor, and pairing flow reflow to match
- Resizing a desktop window or rotating a device re-lays out live, without losing state

**Users & Personalization**
- Multiple local user profiles, each with its own photo, brew profiles, history, and settings
- Theming: light/dark/system, custom seed color, palette style
- Celsius/Fahrenheit
- Keep-screen-on, full-screen mode
- Open-source license listing


---

Learn more about [Kotlin Multiplatform](https://www.jetbrains.com/help/kotlin-multiplatform-dev/get-started.html) and
[Compose Multiplatform](https://github.com/JetBrains/compose-multiplatform/#compose-multiplatform).

## License

This project is licensed under the GNU General Public License v3.0 - see the [LICENSE](./LICENSE) file for details.
