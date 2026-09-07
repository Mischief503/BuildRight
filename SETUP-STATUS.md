# Setup status

## Wired now
- One Android application module
- Real Compose UI
- Existing ProjectStore persistence
- v0.4-v0.9 engine source merged into app
- ProjectIntegration bridge to visual openings, framing, cost requirements, code context and assistant snapshot
- GitHub Actions APK build
- Offline project/material/task/notes behavior

## External integrations intentionally require configuration
- Home Depot / retailer authorized price source
- Official jurisdiction code/permit source
- Camera/file attachment implementation
- Remote AI/API provider (optional)
- Release signing keystore

## Validation performed here
The pure Kotlin feature modules (model, designer, framing, cost, code, assistant, backup and field state) compile successfully with `kotlinc` in this workspace. Full Android compilation cannot run locally because this environment does not contain the Android SDK/Gradle distribution; GitHub Actions is configured to perform the Android build.
