# corese-gui documentation

Sphinx documentation for the `corese-gui` project.

## Dependencies

Install documentation dependencies:

```shell
pip install -r docs/requirements.txt
```

## Build documentation

From the repository root:

```shell
sphinx-multiversion docs/source build/html
```

## Build switcher and landing page

Generate both:

- `build/html/switcher.json` for the version dropdown.
- `build/html/index.html` as landing/redirect page.

```shell
./docs/switcher_generator.sh build/html/switcher.json build/html/index.html
```

Optional environment variables:

- `DOCS_BASE_URL` (default: `https://corese-stack.github.io/corese-gui`)
- `LEGACY_DOCS_BASE_URL` (default: `https://corese-stack.github.io/corese-gui-swing`)
- `MINIMAL_VERSION` (default: `5.0.0`)
- `LEGACY_MINIMAL_VERSION` (default: `4.6.0`, lower legacy tags are excluded from switcher)
- `DEV_PRERELEASE_REF` (default: `dev-prerelease`)
- `DOCS_DEFAULT_APP_VERSION` (default: `5.0.0`, used on non-tag refs like `main`)
- `DEV_PRERELEASE_APP_VERSION` (default: `5.0.0`, used to build `-SNAPSHOT` filenames)

## Versioning rules

- Stable tags in the form `vX.Y.Z` are published on this documentation site when `X.Y.Z >= 5.0.0`.
- `dev-prerelease` is included in the switcher as a preview entry when the tag exists.
- Legacy semver tags are listed only for `4.6.0 <= vX.Y.Z < 5.0.0` and point to `corese-gui-swing`.
- Landing-page redirect priority is:
  1. latest stable `vX.Y.Z >= v5.0.0`
  2. `dev-prerelease`
  3. latest legacy `vX.Y.Z < v5.0.0`
  4. `main` (only if no entry exists)

## Download page policy

- `docs/source/install.rst` is written in English and renders a single download matrix per built docs version.
- The resolved links depend on `smv_current_version`:
  - `vX.Y.Z` docs: tag `vX.Y.Z`, no `SNAPSHOT` suffix.
  - `dev-prerelease` docs: tag `dev-prerelease`, `SNAPSHOT` suffix for portable/standalone artifacts.
  - any other ref (for example `main`): fallback to `v${DOCS_DEFAULT_APP_VERSION:-5.0.0}`.
- Links intentionally point to deterministic CI artifact names:
  - Windows installer: `corese-gui-<version>-windows-x64.exe`
  - Windows portable: `corese-gui-<version>[-SNAPSHOT]-windows-x64-portable.zip`
  - macOS installers: `corese-gui-<version>-macos-<arch>.dmg`
  - Linux archives: `corese-gui-linux-<arch>.tar.gz`
  - Standalone jars: `corese-gui-<version>[-SNAPSHOT]-standalone-<target>.jar`
- If artifact naming changes in CI, update `install.rst` accordingly.

## CI release version alignment

- Reusable artifact workflow accepts `project_version` and forwards it as Gradle property `-PprojectVersion=...`.
- On GitHub release (`release.yml`), this is set from the release tag/ref.
- `build.gradle.kts` normalizes `projectVersion` by stripping `refs/tags/` and leading `v`.
- Maven Central workflow applies the same override only when the ref is a semantic version tag (`vX.Y.Z`).

For local testing before the first official `v5.0.0` tag:

```shell
git tag v5.0.0
sphinx-multiversion docs/source build/html
./docs/switcher_generator.sh build/html/switcher.json build/html/index.html
git tag -d v5.0.0
```
