# Audit fixes applied in v1.0.2

- Added root-level UI refresh propagation after Project mutation.
- Added Android system Back handling.
- Restored project delete action in the active dashboard UI.
- Corrected assistant Send behavior and routed supported questions through IntentParser / ProjectExplainer.
- Surfaced v0.6 framing-plan warnings and member totals in Build Center.
- Removed inconsistent hidden 12-inch spacing coercion between engines.
- Replaced free-text wall/foundation/roof choices with constrained UI choices.
- Marked unconnected field-capture controls as pending/disabled instead of fake-functional.
- Changed GitHub build workflow to Gradle 8.9 and made unit tests mandatory before APK assembly.
- Added unit tests covering calculations, pricing package rounding, code applicability and assistant intent parsing.
