# BuildRight v1.0.4a build fix

GitHub Actions failed at `:app:compileDebugKotlin` because the project compiler plugins were Kotlin 2.0.21 while resolved Kotlin stdlib/coroutines artifacts carried Kotlin 2.2 metadata.

## Fix
- `org.jetbrains.kotlin.android`: 2.0.21 -> 2.2.20
- `org.jetbrains.kotlin.plugin.compose`: 2.0.21 -> 2.2.20
- Kept Gradle 8.9 and Android Gradle Plugin 8.7.3.
- Kept kotlinx-coroutines-android 1.11.0.

This aligns the Kotlin compiler with the dependency metadata and removes the `binary version 2.2.0, expected 2.0.0` failure that caused the K2 `source must not be null` internal compiler exception.
