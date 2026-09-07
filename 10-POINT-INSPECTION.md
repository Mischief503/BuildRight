# BuildRight v1.0.2 — 10-Point Inspection

Inspection method: source audit, wiring trace, Android-free Kotlin compilation, targeted BuildEngine execution, persistence-field review, UI callback review, and GitHub workflow review.

## 1. Project structure / Android configuration — PASS WITH BUILD-ENVIRONMENT CAVEAT
- Android application module, manifest, resources, namespace and package are consistent.
- Java 17 / Kotlin 2.2.20 / Compose configuration is internally consistent.
- GitHub workflow changed to Gradle 8.9 for AGP 8.7.3 and now runs unit tests before APK assembly.
- Full Android compilation could not be executed in the local audit environment because no Android SDK is installed.

## 2. Navigation / project context — PASS AFTER FIX
- Dashboard -> project workspace -> project tools retains one selected project.
- System back is now handled from Project/Settings back to Dashboard.
- Project deletion is exposed again in the current dashboard UI.
- Bottom navigation and More menu route to the expected project screen.

## 3. Input fields / Compose state — PASS AFTER FIX
- Project name and core dimensions are editable.
- Designer dimensions/opening counts are editable.
- Build settings, materials, prices, jurisdiction, tasks, notes and assistant input are editable.
- A root UI revision now forces derived values/top-level UI to refresh after mutable Project data changes.
- Invalid partial numeric text remains local until it becomes a valid number rather than overwriting a saved value with zero.

## 4. Persistence / save-load — CORE PASS, ADVANCED DATA PARTIAL
- Project core fields, jurisdiction, materials and tasks save to SharedPreferences JSON and load back.
- Backward-compatible opt/default reads are used for missing core fields.
- Advanced v0.4+ records such as individual draggable opening positions, attachment URIs, receipt records, permit records, price history and assistant history are NOT yet part of the authoritative persisted Project schema.

## 5. Designer + build/framing wiring — PARTIAL
- Current Canvas reads live Project dimensions, roof style, wall assignments and window count.
- Build Center reads the same Project.
- The newer v0.6 framing plan is now surfaced in Build Center alongside the original shed takeoff engine.
- Spacing behavior was made consistent; entered values are no longer silently forced to 12 in one engine but not the other.
- The advanced DesignerGeometry drag/move-opening model is not yet the renderer/editor driving the screen. Individual opening offsets are therefore not persisted yet.

## 6. Materials + cost flow — CORE PASS, LIVE PRICING NOT CONNECTED
- Generated takeoff can sync into materials.
- Existing prices are retained by material name when regenerated.
- Manual quantity/unit/unit-price edits save and update totals.
- Purchased flags affect purchased/remaining totals.
- PurchasePlanner and price freshness modules compile.
- Home Depot and other live retailer providers are intentionally empty adapters until a permitted data source is connected.

## 7. Field mode + task workflow — TASKS PASS, CAPTURE PARTIAL
- Add task, complete task and progress calculations are connected and persisted.
- Next-step selection updates from task completion.
- Photo, measurement and receipt capture backends are not connected. Those controls are now explicitly shown as pending/disabled rather than pretending they work.

## 8. Code / permits — JURISDICTION PASS, RULE DATA PARTIAL
- Country/state/county/city inputs save to Project.
- Code data model, applicability engine, snapshots/diff and permit-record models compile.
- OfficialSourceCodeProvider is intentionally empty; authoritative local rules are not being downloaded/applied yet.
- Permit status records are not yet persisted through the main ProjectStore/UI.

## 9. Smart Assistant — READ-ONLY CORE PASS AFTER FIX, EDIT COMMANDS PARTIAL
- Send now submits a message instead of responding while the user is still typing.
- IntentParser and ProjectExplainer are used for common project questions.
- Next step, missing materials, cost/project summary and warnings are grounded in current Project state.
- ProjectCommand validation classes compile, but command execution/confirmation is not yet connected to mutate Project through a command bus.

## 10. Tests / release pipeline — PASS FOR WHAT CAN BE VERIFIED LOCALLY
- Android-free feature modules compile together with kotlinc.
- BuildEngine was executed against a 12×16×8 test project successfully.
- JUnit tests were added for BuildEngine, PurchasePlanner, ApplicabilityEngine and IntentParser.
- GitHub Actions now runs `:app:testDebugUnitTest` before `:app:assembleDebug` and uploads the debug APK.
- The final authority on Android/Compose compilation is the GitHub Actions build because this audit environment lacks the Android SDK.

# Inspection verdict

The app is **not yet honestly a 10/10 fully complete release**. The core offline DIY-project application is wired: project creation/editing, navigation, save/load, shed calculations, materials, manual cost, task/field checklist, jurisdiction entry and read-only project assistant.

The remaining incomplete production integrations are:
1. drag-and-drop Designer opening persistence,
2. photo/measurement/receipt field capture,
3. live retailer price source,
4. authoritative jurisdiction code source + persisted permit workflow,
5. assistant edit-command execution/confirmation,
6. full Android SDK compile/instrumentation pass.

Those are concrete integration tasks, not hidden defects.
