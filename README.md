This is a Kotlin Multiplatform project template targeting Android, iOS, Web, and Desktop (JVM), configured for Android Gradle Plugin 9.0.0.

* [/composeApp](./composeApp/src) contains the code shared across your Compose Multiplatform applications.
  It includes several subfolders:
  - [commonMain](./composeApp/src/commonMain/kotlin) is for code common to all targets.
  - Other folders are for Kotlin code compiled only for the platform indicated in the folder name.
    For example, if you want to use Apple’s CoreCrypto for the iOS part of your Kotlin app,
    the [iosMain](./composeApp/src/iosMain/kotlin) folder is the right place for such calls.
    Similarly, if you want to edit the Desktop (JVM) specific part, the [jvmMain](./composeApp/src/jvmMain/kotlin)
    folder is the appropriate location.

* [/iosApp](./iosApp/iosApp) contains the iOS application. Even if you’re sharing your UI with Compose Multiplatform,
  you need this entry point for your iOS app. This is also where you should add SwiftUI code for your project.
* [/androidApp](./androidApp) contains the entry point for the Android application.

### Build and Run Android Application

To build and run the development version of the Android app, use the run configuration from the run widget in your IDE’s toolbar or build it directly from the terminal:
- on macOS/Linux
  ```shell
  ./gradlew :composeApp:assembleDebug
```

* on Windows
```shell
.\gradlew.bat :composeApp:assembleDebug
```



### Build and Run Desktop (JVM) Application

To build and run the development version of the desktop app, use the run configuration from the run widget in your IDE’s toolbar or run it directly from the terminal:

* on macOS/Linux
```shell
./gradlew :composeApp:run

```


* on Windows
```shell
.\gradlew.bat :composeApp:run

```



### Build and Run Web Application

To build and run the development version of the web app, use the run configuration from the run widget in your IDE's toolbar or run it directly from the terminal:

* For the Wasm target (faster, modern browsers):
* on macOS/Linux
```shell
./gradlew :composeApp:wasmJsBrowserDevelopmentRun

```


* on Windows
```shell
.\gradlew.bat :composeApp:wasmJsBrowserDevelopmentRun

```




* For the JS target (slower, supports older browsers):
* on macOS/Linux
```shell
./gradlew :composeApp:jsBrowserDevelopmentRun

```


* on Windows
```shell
.\gradlew.bat :composeApp:jsBrowserDevelopmentRun

```





### Build and Run iOS Application

To build and run the development version of the iOS app, use the run configuration from the run widget in your IDE’s toolbar, or open the [/iosApp](https://www.google.com/search?q=./iosApp) directory in Xcode and run it from there.

---

Learn more about [Kotlin Multiplatform](https://www.jetbrains.com/help/kotlin-multiplatform-dev/get-started.html),
[Compose Multiplatform](https://github.com/JetBrains/compose-multiplatform/#compose-multiplatform),
[Kotlin/Wasm](https://kotl.in/wasm/)…