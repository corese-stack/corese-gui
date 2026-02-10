# Data Page Refactor - Architecture Blueprint

## Scope

Refactor the `Data` feature into a two-pane screen:

- Left pane: reasoning/entailment rule management.
- Right pane: live graph visualization + graph management actions.

This document defines architecture before implementation, with strict separation between GUI and `corese-core`.

## Constraints

- No direct `corese-core` calls from GUI classes.
- Existing shared graph must stay compatible with Query/Validation features.
- Graph updates must be centrally notified.
- Favor incremental graph updates in viewer; avoid full re-render whenever possible.
- Use existing notification/theme/dialog infrastructure.

## Current Baseline (Observed)

- `DataView` is currently minimal (single load button).
- Shared graph lives in `GraphStoreService` (package-private `Graph` holder).
- `RdfDataService` handles file loading + clear only.
- `GraphDisplayWidget` currently renders from full JSON-LD (`jsonld` setter => full redraw).
- No dedicated app-level graph event bus exists.

## Target High-Level Architecture

### Layers

1. GUI layer (`feature/data`, `component/...`)
- Pure UI/controller orchestration.
- Talks only to service facade interfaces.

2. Adapter/service layer (`core/service`)
- Encapsulates all `corese-core` APIs.
- Owns graph mutation publication, reasoning execution, source registry, exports.

3. Corese layer (`corese-core`)
- Accessed only inside adapter services.

### Main Components

#### 1) `DataWorkspaceService` (public facade, GUI-facing)

Single entry point for Data screen operations:

- Load RDF files.
- Load RDF from URI.
- Reload sources.
- Clear graph (with option scopes).
- Export graph (RDF + visual formats).
- Toggle built-in reasoning rules.
- Add/remove/toggle custom `.rul` rules.
- Provide graph snapshots for viewer.
- Provide reasoning state + loaded sources state.

GUI depends on this facade only.

#### 2) `GraphMutationBus` (public, app-wide)

Central publish/subscribe for graph mutations:

- Other features (Data page first, potentially Query/Validation later) subscribe.
- Carries metadata + optional delta payload.
- Coalescing strategy supported.

#### 3) `GraphMutationCollectorService` (package-private)

Attaches once to shared graph (`GraphStoreService`) via `EdgeChangeListener` and emits mutation events to `GraphMutationBus`.

Notes:

- Captures insert/delete edges from graph operations.
- For operations not reported by edge listener (e.g., clear-all), services emit explicit events.

#### 4) `ReasoningService` (public API, adapter)

Manages built-in profiles and custom `.rul` rules:

- Built-in toggles (RDFS, OWL RL variants).
- Custom rules registry.
- Recompute on graph changes when active.
- Named graph isolation per rule/profile.

#### 5) `GraphProjectionService` (public API, adapter)

Provides viewer-ready payloads:

- Full snapshot JSON-LD.
- Optional incremental delta payload for viewer.
- Named graph metadata (graph IDs, triple counts).

#### 6) `DataSourceRegistryService` (public API, adapter)

Tracks loaded sources for reload:

- Local file sources.
- URI sources.
- Source status and last error.

## Proposed Package Map

- `fr.inria.corese.gui.feature.data`
  - `DataPageView`
  - `DataPageController`
  - `ReasoningPaneView`
  - `GraphPaneView`
  - `DataPageModel`
- `fr.inria.corese.gui.core.service`
  - `DataWorkspaceService` (interface)
  - `DefaultDataWorkspaceService` (implementation)
  - `GraphMutationBus`
  - `GraphMutationEvent`
  - `GraphProjectionService`
  - `ReasoningService`
  - `DataSourceRegistryService`
  - `GraphMutationCollectorService` (package-private)

## Core Contracts

### `GraphMutationEvent`

Recommended shape:

- `kind`: `INSERT`, `DELETE`, `BULK_REFRESH_REQUIRED`, `CLEAR_ALL`, `CLEAR_NAMED_GRAPH`, `RULE_APPLIED`, `RULE_REMOVED`, `RELOAD`.
- `affectedNamedGraphs`: `Set<String>`.
- `insertCount`, `deleteCount`.
- `delta`: optional payload for incremental rendering.
- `version`: monotonic long.
- `source`: operation source (`DATA_LOAD`, `QUERY_UPDATE`, `RULE_ENGINE`, ...).

### `GraphDelta`

Incremental payload (transport-safe, GUI-friendly):

- `inserted`: list of lightweight triple records.
- `deleted`: list of lightweight triple records.
- `tooLarge`: boolean fallback hint.

Triple record:

- `graph`, `subject`, `predicate`, `object`, `objectKind` (`IRI`, `BNODE`, `LITERAL`), optional `datatype`, optional `lang`.

### `ReasoningProfile`

- `RDFS`
- `OWL_RL`
- `OWL_RL_LITE`
- `OWL_RL_EXT`

### `ReasoningRuleState`

- `id`, `label`, `type` (`BUILTIN` / `CUSTOM`), `enabled`, `namedGraphIri`, `sourcePath` (custom only), `lastRunStatus`.

## Named Graph Policy

All inferred triples must be isolated by rule/profile graph.

Conventions:

- Built-in profiles:
  - `urn:corese:inference:rdfs`
  - `urn:corese:inference:owlrl`
  - `urn:corese:inference:owlrl-lite`
  - `urn:corese:inference:owlrl-ext`
- Custom rules:
  - `urn:corese:inference:custom:{ruleId}`

Operations:

- Toggle ON => recompute and repopulate only that rule graph.
- Toggle OFF => clear only that named graph.
- Graph mutation in asserted data => recompute active reasoning graphs.

## Incremental Rendering Strategy

## Phase A (safe baseline)

- On mutation event, Data page requests full JSON-LD snapshot.
- Apply debouncing/coalescing (single refresh per burst).
- Guarantees correctness quickly.

## Phase B (incremental path)

- Extend `GraphDisplayWidget` and `graph-viewer` JS API:
  - `window.applyGraphDelta(deltaJson, requestId)`.
- Apply node/link patch in place while preserving simulation state.
- Fallback to full snapshot when:
  - delta too large,
  - unsupported structural mutation,
  - desync detected.

## Data Page UI Composition

## Right Pane (major area)

- `GraphDisplayWidget` centered.
- Vertical `ToolbarWidget` on right:
  - Load files
  - Load URI
  - Reload
  - Export (RDF + visual formats)
  - Clear graph (danger + confirmation)

## Left Pane

- Built-in rules section:
  - Toggle per profile.
- Custom rules section:
  - Empty state + add `.rul`.
  - Vertical list (toggle + remove per rule).
- Optional small status footer:
  - active rules count, inferred triples count.

## Interaction Flows

### Load file(s)

1. UI picks files.
2. `DataWorkspaceService.loadFiles(...)`.
3. Service loads into shared graph, updates source registry.
4. Mutation event published.
5. Data page updates graph view (delta or snapshot).
6. If reasoning active, service recomputes affected reasoning graphs and emits events.

### Toggle rule ON

1. UI calls `setRuleEnabled(ruleId, true)`.
2. Service computes inferred triples into rule named graph.
3. Mutation event published (`RULE_APPLIED`).
4. Graph view updates.

### Toggle rule OFF

1. UI calls `setRuleEnabled(ruleId, false)`.
2. Service clears rule named graph only.
3. Mutation event published (`RULE_REMOVED` / `CLEAR_NAMED_GRAPH`).
4. Graph view updates.

### Clear graph

1. UI asks confirmation (ModalService).
2. Service clears graph + registries + reasoning generated graphs.
3. Explicit mutation event `CLEAR_ALL` published.
4. Graph view resets.

## Threading and Lifecycle

- Heavy operations run on `AppExecutors`.
- UI updates via `Platform.runLater`.
- `DataPageController` subscribes on init, unsubscribes on close/dispose.
- Service serializes write operations to avoid reasoning/load races.

## Integration Rules

- Notifications: use existing `NotificationWidget`.
- Confirmation dialogs: extend `ModalService` with generic confirm API or add dedicated Data clear dialog in `DialogLayout`.
- Theming: use existing CSS variables + `ThemeManager`.

## Recommended Implementation Phases

1. Foundation:
- Introduce `DataWorkspaceService` facade.
- Introduce `GraphMutationBus` + collector.
- Build new `DataPageView` layout (left/right + graph + toolbar).

2. Functional Data Actions:
- File load, URI load, reload, clear, export wiring.
- Full snapshot refresh with debounce.

3. Reasoning:
- Built-in toggles + per-rule named graph lifecycle.
- Custom `.rul` load/list/toggle/remove.

4. Incremental Viewer:
- Extend JS API + widget delta apply.
- Add fallback logic and thresholds.

5. Hardening:
- Performance tuning, tests, failure recovery, UX polish.

## Risks and Mitigations

- Risk: incremental protocol complexity.
  - Mitigation: implement fallback to snapshot first, then incremental.
- Risk: reasoning profile interactions (dependencies between profiles).
  - Mitigation: deterministic evaluation order + documented semantics.
- Risk: missing clear notifications from low-level graph API.
  - Mitigation: explicit event emission in service methods for clear/reset operations.
