<!-- markdownlint-disable MD033 -->
<!-- markdownlint-disable MD041 -->

<p align="center">
  <a href="https://project.inria.fr/corese/">
    <img src="docs/source/_static/logo/corese-gui-logo.png" width="200" alt="Corese-GUI logo">
  </a>
  <br>
  <strong>Graphical User Interface for the Semantic Web of Linked Data</strong>
</p>

<p align="center">
  <a href="https://cecill.info/licences/Licence_CeCILL-C_V1-en.html"><img src="https://img.shields.io/badge/License-CECILL--C-blue.svg" alt="License: CeCILL-C"></a>
  <a href="https://github.com/orgs/corese-stack/discussions"><img src="https://img.shields.io/badge/Discussions-GitHub-blue" alt="Discussions"></a>
  <a href="https://corese-stack.github.io/corese-gui/"><img src="https://img.shields.io/badge/Docs-GitHub%20Pages-0A66C2" alt="Documentation"></a>
</p>

# Corese-GUI

Corese-GUI is the desktop application of the Corese Semantic Web stack.
It provides a visual workspace to load RDF data, execute SPARQL queries, inspect results, validate SHACL constraints, and run reasoning workflows.

## Features

- Load and explore RDF datasets
- Execute SPARQL queries (SELECT, CONSTRUCT, ASK, UPDATE)
- Visualize graph results
- Validate data with SHACL
- Apply reasoning with built-in and custom rules
- Manage data, query, validation, logs, and settings from dedicated views

<p align="center">
  <img src="packaging/flatpak/screenshots/data-graph-exploration.png" width="49%" alt="Data graph exploration view">
  <img src="packaging/flatpak/screenshots/query-select-table-results.png" width="49%" alt="SPARQL SELECT table results">
</p>
<p align="center">
  <img src="packaging/flatpak/screenshots/validation-shacl-results.png" width="49%" alt="SHACL validation results">
  <img src="packaging/flatpak/screenshots/data-reasoning-custom-rules.png" width="49%" alt="Reasoning with custom rules">
</p>

## Downloads

- [Latest stable release page](https://github.com/corese-stack/corese-gui/releases/latest)
- [Development pre-release page (`dev-prerelease`)](https://github.com/corese-stack/corese-gui/releases/tag/dev-prerelease)
- [Full installation matrix in the documentation site](https://corese-stack.github.io/corese-gui/)

Stable releases publish Windows installers and portable archives, macOS `.dmg` bundles, Linux archives, and standalone JARs. The rolling `dev-prerelease` channel publishes the same platform matrix, but every downloadable artifact keeps the `-SNAPSHOT` suffix in its filename.

> Standalone JAR files require Java 25 to be installed manually.

## Build and Run (local)

```bash
./gradlew clean check
./gradlew run
```

Build artifacts for the current platform:

```bash
./gradlew packageCurrentPlatform
```

## Documentation

- [Documentation site](https://corese-stack.github.io/corese-gui)

## Contributing

- [Discussions](https://github.com/orgs/corese-stack/discussions)
- [Issues](https://github.com/corese-stack/corese-gui/issues)
- [Pull Requests](https://github.com/corese-stack/corese-gui/pulls)
