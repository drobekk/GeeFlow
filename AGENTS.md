# GeeFlow project instructions

## Canonical source checkout

- The only active GeeFlow source checkout is `/Users/liang/Developer/wendougee/GeeFlow`, on branch `codex/brew-shot-summary`. It was consolidated from the app baseline `0fb72b2`.
- Treat this Developer checkout as master. Tablet installs and other device copies are test builds, never the source of truth.
- Do app work in the canonical checkout. Keep one active GeeFlow checkout. Use a temporary worktree only when the user asks for isolation; after integration, archive its changes and remove the extra checkout.
- Before editing or building, record `git status --short --branch`, the commit, and the exact checkout path. Preserve existing changes before switching revisions.
- Keep code in this repository. Save screenshots, APKs, build records, and feature notes under `/Users/liang/Documents/Personal projects/Wendougee coffee app/milestones/YYYY-MM/geeflow/NN-feature-vNN/`. Do not create conversation-named folders. Include a short README and verification record when the task produces a build or other milestone artifact.
- Read the beginner guide only when the user asks for workflow or terminology help.

## Product baseline

Preserve these user-approved behaviors when changing the related areas:

- Separate recipe browsing and brewing screens.
- One-tap BOOKOO scale connection beside the device controls. Recipes remain runnable without the app-connected scale.
- When the app scale is connected, its weight target can end any recipe before that recipe's own finish target.
- Save brew telemetry, including BOOKOO readings, and make brew history viewable by profile.
- Keep the chart inside the available screen area, and retain the OLED pixel-shift screensaver.
- Keep recipe sync manual and preserve the hosted profile catalog/editor behavior.
- Each brew phase has one active controller, pressure or flow. Use the other parameter as zero, a soft limit, or a condition that adjusts the active controller.

## Development workflow

- Treat each chat as one feature or bug. Inspect the branch, working tree, relevant code, and milestone notes before editing. Use source and recorded evidence rather than another chat's transcript.
- For dependent changes, build on the integrated commit. Avoid concurrent edits to the canonical checkout.
- Keep durable implementation decisions in source docs or a milestone README, with links to related protocol evidence.
- Do not send brew or other control commands to a physical machine unless the user explicitly asks. Do not push, publish, or change external services without explicit instruction.
- Preserve package `app.geeflow.debug` and the development signing key when an install must retain local profiles and history.

## Android toolchain

- Use Java 21 for the Gradle daemon. On Liang's Mac, the installed JDK is `/opt/homebrew/opt/openjdk@21/libexec/openjdk.jdk/Contents/Home`. Confirm `bin/java` exists and set `JAVA_HOME` for each Gradle invocation. Set `ANDROID_HOME` and `ANDROID_SDK_ROOT` to `/Users/liang/Library/Android/sdk` for Android tasks.
- Do not use Android Studio's bundled JDK 25 for a build requiring JDK 21. Check the installed JDK and `./gradlew --version` before diagnosing a toolchain failure. Use the checked-in Gradle wrapper and shared Gradle cache; avoid routine `--rerun-tasks` and `--refresh-dependencies`.

## Build and install checks

- After an app build, run the narrowest relevant automated checks before reporting it as verified. For Android builds, launch the APK on an available device or emulator and check startup. Report exact commands and results. Label emulator and demo checks accurately; they do not prove physical-machine behavior.
- Name APKs and build records with the source branch, short commit, and clean/dirty state. Do not identify an APK by version name or version code alone.
- Before installing, compare the tablet database schema and migration path with the APK. Test a migration on a database copy when versions differ. Preserve app data; stop if the APK would downgrade the database.
- Before `adb install`, verify the APK path, package, version, and signing certificate. Use an in-place install when retaining profiles/history, launch `app.geeflow.MainActivity`, and confirm startup.
- For Samsung Galaxy S24+ installs, target owner account 0 only. After verifying the normal app, remove the temporary Bluetooth diagnostic package unless the user asks to keep it.
