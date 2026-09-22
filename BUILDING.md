# Building BuildRight

## Fastest path: GitHub Actions
This repository contains `.github/workflows/main.yml`.

1. Put the project at the root of a GitHub repository.
2. Push to `main`, or run the workflow manually from the Actions tab.
3. The runner installs Java 17 (Temurin), Android SDK, and uses the Gradle 8.9 wrapper.
4. It runs unit tests, then `./gradlew :app:assembleDebug`.
5. Download the `BuildRight-debug-apk` workflow artifact.

## Android Studio
Open the repository root in a current Android Studio installation. The project uses:
- Android Gradle Plugin 8.7.3
- Kotlin 2.2.20
- compileSdk / targetSdk 35
- minSdk 26
- Jetpack Compose / Material 3
- Gradle wrapper 8.9 (`./gradlew`)

## Release signing
Do not commit a keystore or passwords. Add release signing later using GitHub repository secrets or a local untracked keystore.
