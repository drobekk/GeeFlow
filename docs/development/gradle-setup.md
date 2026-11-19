# Gradle and JDK setup

## Standard versions

- Use the checked-in Gradle wrapper (`./gradlew`), currently Gradle 9.6.0.
- The Gradle daemon requires Java 21. `gradle/gradle-daemon-jvm.properties` is the source of truth and takes precedence over `JAVA_HOME` and Android Studio's configured Gradle JDK. It deliberately has no daemon JDK download URLs: install Java 21 once and point both command-line and Android Studio Gradle at it.
- CI installs Zulu 21; local builds can use any Java 21 vendor. On this Apple Silicon Mac, Homebrew Java 21 is `/opt/homebrew/opt/openjdk@21/libexec/openjdk.jdk/Contents/Home`.
- The desktop app has a separate Java 11 project toolchain (`desktopApp/build.gradle.kts`). The Foojay resolver in `settings.gradle.kts` can provision that toolchain once; it is not the Java 21 Gradle daemon and is not needed for Android-only tasks.
- The Android SDK used for builds should include API 37.

For command-line builds on this Mac, set the same JDK explicitly:

```sh
export JAVA_HOME=/opt/homebrew/opt/openjdk@21/libexec/openjdk.jdk/Contents/Home
export PATH="$JAVA_HOME/bin:$PATH"
./gradlew --version
```

In Android Studio, set **Settings/Preferences → Build, Execution, Deployment → Build Tools → Gradle → Gradle JDK** to that same Java 21 installation. `./gradlew --version` reports the launcher and daemon JVMs; confirm the daemon is Java 21.

Use the same Gradle user home for command-line and Android Studio builds. Unless overridden, Gradle uses `~/.gradle`. Do not assign a separate `GRADLE_USER_HOME` per worktree. That shared directory holds the wrapper, plugin/dependency caches, and provisioned project JDKs.

## Routine build and test

Use the narrowest task set for the change, for example:

```sh
./gradlew :androidApp:assembleDebug :androidApp:testDebugUnitTest :shared:feature:device:dashboard:jvmTest
```

Do not add `--rerun-tasks` or `--refresh-dependencies` to routine builds. `--rerun-tasks` forces task actions to run even when Gradle's normal up-to-date checks can reuse outputs. `--refresh-dependencies` asks Gradle to recheck dependency metadata. Neither option is needed to run tests after an app build.

## Why downloads or cache messages appear

- Plugins and Maven artifacts are cached under `$GRADLE_USER_HOME/caches`; wrapper distributions are under `$GRADLE_USER_HOME/wrapper/dists`; provisioned JDKs are under `$GRADLE_USER_HOME/jdks`.
- With pinned versions and one shared Gradle user home, plugins should not be downloaded on every local build. Downloads are expected for a new Gradle user home, a newly added or changed plugin/dependency, a cache that was cleared, or the first use of a project JDK toolchain. The local daemon's Java 21 must already be installed; this repo no longer auto-downloads another Java 21 from Foojay.
- If a download message repeats, check that the commands use the same OS user and Gradle user home, and that `--refresh-dependencies` or cache-clearing is not being used. GitHub-hosted CI jobs start in clean runners, but `gradle/actions/setup-gradle` caches Gradle state between runs.
- A message such as `Calculating task graph as no cached configuration is available` describes a configuration-cache miss for that build's task set and inputs. It is not a plugin download. Different commands, JVMs, or changed build files can each need their own configuration-cache entry.
- Use `--offline` to check whether the dependencies and plugins needed by a task are already cached. It makes Gradle fail instead of accessing the network when an artifact is missing.
