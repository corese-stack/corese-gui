# Release Prep Review (Phase 1)

Date: 2026-02-17

## Automated Quality Checks

- `./gradlew clean check`: PASS
- `./gradlew spotlessApply test`: PASS

## Codebase Audit Summary

This pass focused on release-readiness without behavior change:

- Reduced complexity in the graph rendering widget by extracting bridge parsing and JavaScript script building logic.
- Added focused unit tests for extracted helpers to preserve deterministic behavior.
- Hardened `GraphDisplayWidget#clear()` for non-FX-thread calls.
- Reduced complexity in `ThemeManager` by extracting:
  - `ThemeManagedStyleSupport` (managed CSS composition/merging)
  - `ThemeVisualPalette` (theme-dependent visual tokens)
- Added unit tests for theme helpers to lock formatting/token behavior.
- Simplified `TabEditorController` shortcut flows:
  - central adjacent-tab navigation helper
  - unified visible-result-controller shortcut dispatch
  - reduced duplicated tab lock/unlock logic
- Simplified `TabStripView` constructor by extracting shared overflow-shadow setup
  (same behavior, less duplication for left/right branches).

## Hotspots To Tackle In Phase 2

These classes remain large and should be split incrementally to reduce regression risk during future releases:

- `src/main/java/fr/inria/corese/gui/component/graph/GraphDisplayWidget.java` (~780 LOC)
- `src/main/java/fr/inria/corese/gui/component/tabstrip/TabStripView.java` (~925 LOC)
- `src/main/java/fr/inria/corese/gui/feature/editor/tab/TabEditorController.java` (~860 LOC)
- `src/main/java/fr/inria/corese/gui/core/theme/ThemeManager.java` (~810 LOC)
- `src/main/java/fr/inria/corese/gui/feature/query/template/QueryTemplateDialog.java` (~730 LOC)

Recommended strategy:

1. Extract pure helper/services first (script builders, parsers, formatters, mappers).
2. Add characterization tests before moving controller/view orchestration code.
3. Split UI construction from event wiring in large JavaFX views.
