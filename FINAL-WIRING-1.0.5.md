# BuildRight v1.0.5 — Final Wiring

This release closes the six source-level blockers identified in the v1.0.4c triple-check.

## Completed

1. Field Mode
   - Camera-backed field photo capture via ActivityResult TakePicture.
   - Camera-backed receipt capture.
   - Saved measurement dialog with label, numeric value, unit, notes, timestamp.
   - Measurement and attachment lists with delete/open actions.
   - FileProvider-backed private app storage for captured images.

2. Permit workflow
   - Add/edit/delete permit records.
   - Track permit type, stage, application number, official-source URL, notes and verification status.
   - Open official source in browser.

3. Code-source handling
   - Project now persists verified CodeReferenceRecord entries.
   - User can attach official jurisdiction/code URLs and notes.
   - Official provider integration now requires a real loader; it cannot silently pretend to be connected.
   - BuildRight continues to avoid certifying structural/code compliance itself.

4. Smart Assistant edits
   - Active UI now calls ProjectCommandEngine.propose().
   - Proposed edits are displayed before mutation.
   - User must press Confirm edit.
   - Confirmed edits call ProjectCommandEngine.execute() and persist.

5. Backup / restore
   - Settings uses Android CreateDocument and OpenDocument contracts.
   - Full-project JSON backup export works through the system file picker.
   - Import parses through ProjectStore and requires confirmation before replacing local projects.

6. Settings truthfulness
   - Removed misleading fake-action rows.
   - Appearance/units/safety rows are explicitly informational.
   - Backup controls are real buttons.

## Additional hardening
- Added CodeReferenceRecord JSON persistence and migration-safe empty default.
- Added androidx.core dependency for FileProvider.
- Added FileProvider manifest entry and file_paths.xml.
- ManualPriceProvider now returns supplied/manual quotes instead of being an empty placeholder.
- HomeDepotPriceProvider now fails explicitly when no authorized feed is configured instead of silently returning no data.
- OfficialSourceCodeProvider requires a loader instead of silently returning no rules.
- Added ProjectCommandEngine unit tests.
- Version: 1.0.5-final-wiring / versionCode 18.

## Verification performed locally
- ProjectCommandEngine compiled independently and executed width + opening edits correctly.
- Price planning/provider core compiled successfully with kotlinc.
- Code/provider package compiled successfully with kotlinc.
- AndroidManifest.xml and file_paths.xml parsed successfully as XML.
- No empty onClick handlers found.
- No disabled Field Mode action buttons remain.
- Persistence save/load paths contain measurements, attachments, permits and code references.

## Remaining release gate
A full Android/Compose build cannot be run in the current environment because the Android SDK is unavailable. GitHub Actions must run:

- `:app:testDebugUnitTest`
- `:app:assembleDebug`

After that succeeds, install the generated APK on a real Android device and smoke-test camera capture, document import/export and URI opening.
