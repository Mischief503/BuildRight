# BuildRight v1 — Unified Android Project

BuildRight is an offline-first Android construction planning application built with Kotlin and Jetpack Compose.

This repository is the consolidated setup: the working Android UI plus the Designer, Field, Framing, Cost, Code/Permit and Smart Assistant engine modules are now inside one `app` module instead of separate upgrade archives.

## User-facing areas
- Project dashboard and templates
- Project Overview
- Visual Designer with Compose Canvas
- Build Center / shed takeoff engine
- Materials and shopping checklist
- Field Mode
- Cost Intelligence
- Code & Permits
- Smart Build Assistant
- Tasks and Notes
- Settings

## Integrated engine packages
- `com.buildright.model` — visual/field project records
- `com.buildright.engine` — plan geometry
- `com.buildright.designer` — designer geometry and opening commands
- `com.buildright.framing` — wall/floor/roof framing + cut optimizer
- `com.buildright.cost` — retailer-neutral price and purchase planning
- `com.buildright.code` — jurisdiction/code/permit models and applicability
- `com.buildright.assistant` — typed assistant intents, commands and policies
- `com.buildright.backup` — project backup validation
- `com.buildright.ui` — field-mode state
- `com.buildright.app.ProjectIntegration` — bridge from the saved Project to all feature engines

## Build on GitHub
The included `.github/workflows/android-build.yml` installs Java 17 and Gradle 8.11.1 on the runner, builds `:app:assembleDebug`, then uploads `app-debug.apk` as a workflow artifact.

1. Create a GitHub repository.
2. Upload the contents of this folder to the repository root.
3. Push to `main` or open **Actions → Build Android APK → Run workflow**.
4. Open the completed workflow and download **BuildRight-debug-apk**.
5. Install the APK on an Android 8.0+ device.

## Current boundary
The source is consolidated, but several external functions still need real credentials/data/device services before they can be fully live: retailer price providers, authoritative local code feeds, camera/receipt attachment storage, and an optional remote AI provider. Their architecture is present without fake network behavior.

Package: `com.buildright.app`
Minimum Android: API 26 / Android 8.0

## Universal Pricing (v1.0.4)

Cost Intelligence now supports persisted retailer quotes, package-size normalization, cheapest split-cart comparison, fewest-store planning, and universal product-page price import for sources that permit automated access. See `UNIVERSAL-PRICING.md`.
