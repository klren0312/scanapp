# Agent Guide

## Project Snapshot

This repository is `scanapp`, a Kotlin Multiplatform WiFi/Bluetooth scanner app.

- Shared business logic lives in `shared`.
- Android shell app lives in `androidApp`.
- iOS shell integration is represented by `iosApp/Podfile`.
- `ohosApp` is currently a placeholder for future HarmonyOS integration.
- UI code is built with Tencent Kuikly in `shared/src/commonMain/kotlin/com/example/scanapp/ui`.
- Local storage uses SQLDelight from `shared/src/commonMain/sqldelight/com/example/scanapp/Database.sq`.
- The main package namespace is `com.example.scanapp`.

## Required Workflow

Every change must be closed with all of the following:

1. Update `CHANGELOG.md` with a concise entry for the change.
2. Run the smallest relevant verification command that matches the files changed.
3. Commit the completed change before reporting it as finished.

Do not stage or commit unrelated user changes. If the worktree already has unrelated changes, leave them alone and stage only the files touched for the current task.

## Coding Note

写完代码后不需要执行编译/构建验证（不要运行 gradlew 编译命令）。直接完成改动、更新 CHANGELOG 并提交即可。

## Common Commands

Use PowerShell from the repository root.

```powershell
.\gradlew.bat :shared:generateCommonMainScanAppDatabaseInterface
.\gradlew.bat :shared:compileDebugKotlinAndroid
.\gradlew.bat :shared:testDebugUnitTest
```

For a focused database test:

```powershell
.\gradlew.bat :shared:testDebugUnitTest --tests com.example.scanapp.DatabaseTest
```

AGP 8.2 requires a compatible JDK. Prefer JDK 17 when Gradle or Android tooling needs `JAVA_HOME`.

## Architecture Notes

- Keep cross-platform models, DAO wrappers, service interfaces, export logic, and Kuikly UI in `shared/src/commonMain`.
- Keep Android-specific scanner, location, database driver, background scan, and export implementations in `shared/src/androidMain` or `androidApp`.
- Keep SQL schema and query changes in `Database.sq`; regenerate SQLDelight interfaces after schema edits.
- Add or update tests in `shared/src/commonTest` for common logic and `shared/src/androidUnitTest` for Android/SQLDelight integration behavior.
- Do not add platform APIs to `commonMain`; use interfaces or expect/actual-style boundaries that match the surrounding code.

## Change Discipline

- Prefer small, scoped edits that match the existing Kotlin style and package layout.
- Preserve generated and local tool output unless it is part of the requested change.
- Treat scanner, location, Bluetooth, storage, and file-sharing code as permission-sensitive. Recheck Android manifest permissions and runtime permission flow when touching those areas.
- When changing exported CSV/JSON behavior, update tests or add focused coverage for output shape.
- When changing database uniqueness, pagination, or count behavior, verify both DAO behavior and SQLDelight generation.

## Changelog Rules

`CHANGELOG.md` is mandatory for every change, including documentation-only changes.

Each entry should include:

- Date.
- Short summary.
- Verification performed, or an explicit note when no runtime verification was needed.

Keep entries factual and concise. Do not rewrite unrelated historical entries.

## Commit Rules

- Commit after completing the change and verification.
- Use a clear imperative commit message, for example `Add agent workflow guide`.
- Stage only files that belong to the completed task.
- If tests cannot be run, record that in `CHANGELOG.md` and mention it in the final response.
