<!-- markdownlint-disable MD024 -->
# Corese Changelog

## Version 5.0.0 - 2026-03-13

### Changed

- Complete rewrite of Corese GUI in JavaFX, rebuilt from scratch.
- Introduced a new modular architecture to improve maintainability, responsiveness, and long-term evolution.
- Redesigned the interface with dedicated Data, Query, Validation, System Logs, and Settings views.
- Refreshed result exploration with table, text, and graph result views plus smoother navigation.
- Improved data workspace handling with clearer source management and better graph status feedback.

### Added

- Richer application customization with theme mode, accent color, and interface scale controls.

## Version 4.6.1 - 2025-10-10

### Fixed

- Fixed RDFS RL inference engine initialization issue causing failures since version 4.6.0.
- Improved synchronization between UI states and inference engine states.

## Version 4.6.0 - 2025-08-01

### Added

- Allow to zoom in/out/reset the view in the Corese GUI.

### Changed

- Fix zooming and panning functionality of graph view.
- Migration from Maven to Gradle for build management.
- Updated dependencies to their latest versions.
- Removed all `module-info` files.
- Clean up of unused imports and code comments.

### Removed

- Dropped support for `shex`.
- Removed the LUBM benchmark from the source code.
- Dropped support for alternative storage systems (Jena TDB1, Corese Graph, RDF4J Model).

## Version 4.5.0 - 2023-12-14

### Added

- Improved RDF serializers (see [issue #142](https://github.com/Wimmics/corese/issues/142)).
- Updated loading message in Corese-GUI (see [pull request #156](https://github.com/Wimmics/corese/pull/156)).

### Fixed

- Fixed Trig serialization to escape special characters (see [issue #151](https://github.com/Wimmics/corese/issues/151)).
- Fixed federated queries with `PREFIX` statements failing under certain conditions (see [issue #140](https://github.com/Wimmics/corese/issues/140)).

### Security

- Updated `org.json` to `20231013` in `/sparql` ([pull request #163](https://github.com/Wimmics/corese/pull/163)).

## Version 4.4.1 - 2023-07-25

### Fixed

- Removed warning: `sun.reflect.Reflection.getCallerClass is not supported. This will impact performance`.

### Security

- Updated `json` from `20180813` to `20230227` in `/sparql` ([pull request #123](https://github.com/Wimmics/corese/pull/123)).
- Updated `json` from `20180813` to `20230227` in `/corese-test` ([pull request #124](https://github.com/Wimmics/corese/pull/124)).
- Updated `guava` from `31.1-jre` to `32.0.0-jre` in `/corese-jena` ([pull request #128](https://github.com/Wimmics/corese/pull/128)).

## Version 4.4.0 - 2023-03-30

### Added

- Integrated storage systems:
  - Jena TDB1.
  - Corese Graph.
  - RDF4J Model.
  - [More information available here](https://github.com/Wimmics/corese/blob/master/docs/storage/Configuring%20and%20Connecting%20to%20Different%20Storage%20Systems%20in%20Corese.md).
- Beta support for RDF\* and SPARQL\* ([Community Group Report 17 December 2021](https://w3c.github.io/rdf-star/cg-spec/2021-12-17.html)).
- Undo/Redo support added to Corese GUI ([pull request #97](https://github.com/Wimmics/corese/pull/97) thanks to [@alaabenfatma](https://github.com/alaabenfatma)).

### Changed

- Performed code clean-up, corrections, and added comments for improved readability and maintenance.

### Fixed

- Fixed an encoding error when loading a file whose path contains a space.
- Fixed encoding error on Windows when exporting graphs.
- Fixed SPARQL engine bug where it was impossible to load a named graph that contains a non-empty RDF list.
- Fixed issue with `rdf:` prefix not found when sending a federated query to Fuseki (see [issue #114](https://github.com/Wimmics/corese/issues/114)).
- Fixed non-standard JSON format on query timeout (see [issue #113](https://github.com/Wimmics/corese/issues/113)).
- Fixed inconsistent status of the OWL and Rules checkboxes in Corese-GUI that were not updated during reload (see [issue #110](https://github.com/Wimmics/corese/issues/110)).
- Fixed the rule engine that was implementing optimizations incompatible with the `owl:propertyChainAxiom` rule (see [issue #110](https://github.com/Wimmics/corese/issues/110)).

### Security

- Bumped testng from 7.3.0 to 7.7.1. [See pull request #118](https://github.com/Wimmics/corese/pull/118).
- Bumped jsoup from 1.14.2 to 1.15.3 in /corese-server. [See pull request #101](https://github.com/Wimmics/corese/pull/101).
- Bumped junit from 4.11 to 4.13.1 in /corese-storage. [See pull request #98](https://github.com/Wimmics/corese/pull/98).
- Bumped xercesImpl from 2.12.0 to 2.12.2. [See pull request #92](https://github.com/Wimmics/corese/pull/92).
- Bumped gremlin-core from 3.2.3 to 3.6.2.

## Version 4.3.0 - 2022-02-03

### Added

- Added a SHACL editor.
- Added a Turtle editor.
- Added possibility to save the current graph.

### Fixed

- Fixed Log4j vulnerability (CVE-2021-44228).
