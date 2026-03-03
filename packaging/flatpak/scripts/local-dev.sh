#!/usr/bin/env bash
set -euo pipefail

APP_ID="fr.inria.corese.CoreseGui"
LOCAL_BRANCH="dev-prerelease"
LOCAL_REMOTE_NAME="corese-gui-local-dev"
RUNTIME="org.freedesktop.Platform"
SDK="org.freedesktop.Sdk"
RUNTIME_VERSION="25.08"
OPENJDK_EXTENSION="org.freedesktop.Sdk.Extension.openjdk25"

SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
FLATPAK_DIR="$(cd "$SCRIPT_DIR/.." && pwd)"
REPO_ROOT="$(cd "$FLATPAK_DIR/../.." && pwd)"

BUILDER_ROOT="$FLATPAK_DIR/.flatpak-builder"
LEGACY_BUILDER_ROOT="$REPO_ROOT/.flatpak-builder"
BUILD_DIR="$BUILDER_ROOT/build"
LOCAL_REPO_DIR="$BUILDER_ROOT/repo"
LOCAL_MANIFEST_PATH="$FLATPAK_DIR/${APP_ID}.dev-prerelease.local.yml"

log() {
    printf '[flatpak-local] %s\n' "$*"
}

die() {
    printf '[flatpak-local] error: %s\n' "$*" >&2
    exit 1
}

usage() {
    cat <<EOF
Usage: $(basename "$0") <command> [options] [-- app_args...]

Commands:
  deps                    Install Flatpak runtime dependencies (user scope).
  build [--skip-gradle]   Build local JAR + local Flatpak repo.
  install                 Install/reinstall app from local Flatpak repo.
  run [-- app_args...]    Run local Flatpak branch.
  test [--skip-gradle] [-- app_args...]
                          Build + install + run.
  uninstall               Uninstall local Flatpak branch (user scope, delete data).
  clean                   Remove generated local Flatpak artifacts.
  reset                   uninstall + clean.

Examples:
  $(basename "$0") deps
  $(basename "$0") test
  $(basename "$0") build --skip-gradle
  $(basename "$0") run -- --help
EOF
}

need_cmd() {
    command -v "$1" >/dev/null 2>&1 || die "Missing required command: $1"
}

require_linux() {
    local host_os
    host_os="$(uname -s)"
    [[ "$host_os" == "Linux" ]] || die "Flatpak local workflow is only supported on Linux (current: $host_os)"
}

ensure_flathub_remote() {
    if ! flatpak remotes --columns=name | grep -Fxq "flathub"; then
        die "Missing Flatpak remote 'flathub'. Run: flatpak remote-add --if-not-exists flathub https://flathub.org/repo/flathub.flatpakrepo"
    fi
}

host_target() {
    case "$(uname -m)" in
        x86_64 | amd64)
            echo "linux-x64"
            ;;
        aarch64 | arm64)
            echo "linux-arm64"
            ;;
        *)
            die "Unsupported architecture: $(uname -m) (expected x86_64/amd64 or aarch64/arm64)"
            ;;
    esac
}

build_local_jar() {
    log "Building standalone JAR with Gradle..."
    "$REPO_ROOT/gradlew" -p "$REPO_ROOT" shadowJar
}

resolve_standalone_jar() {
    local target
    target="$(host_target)"

    shopt -s nullglob
    local jars=("$REPO_ROOT"/build/libs/*-standalone-"$target".jar)
    shopt -u nullglob

    [[ ${#jars[@]} -gt 0 ]] || die "No standalone JAR found for target '$target'. Expected build/libs/*-standalone-$target.jar"

    # Keep the most recently modified matching file if multiple jars exist.
    ls -t "${jars[@]}" | head -n 1
}

write_local_manifest() {
    local jar_path="$1"
    mkdir -p "$BUILDER_ROOT"

    cat >"$LOCAL_MANIFEST_PATH" <<EOF
app-id: ${APP_ID}
runtime: ${RUNTIME}
runtime-version: "${RUNTIME_VERSION}"
sdk: ${SDK}
sdk-extensions:
  - ${OPENJDK_EXTENSION}
command: corese-gui

finish-args:
  # Keep in sync with packaging/flatpak/fr.inria.corese.CoreseGui.yml.
  - --share=network
  - --talk-name=org.freedesktop.IBus
  - --talk-name=org.freedesktop.IBus.*
  - --socket=x11
  - --share=ipc
  - --device=dri
  - --filesystem=xdg-documents
  - --filesystem=xdg-download
  - --filesystem=xdg-desktop

modules:
  - name: openjdk
    buildsystem: simple
    build-commands:
      - /usr/lib/sdk/openjdk25/install.sh

  - name: corese-gui
    buildsystem: simple
    build-commands:
      - install -Dm644 corese-gui-standalone.jar /app/bin/corese-gui-standalone.jar
      - install -Dm755 packaging/flatpak/scripts/run.sh /app/bin/corese-gui
      - install -Dm644 src/main/resources/images/startup-splash-primer-dark.png /app/share/corese-gui/startup-splash-primer-dark.png
      - install -Dm644 packaging/flatpak/appdata/\${FLATPAK_ID}.appdata.xml /app/share/metainfo/\${FLATPAK_ID}.appdata.xml
      - install -Dm644 packaging/flatpak/appdata/\${FLATPAK_ID}.desktop /app/share/applications/\${FLATPAK_ID}.desktop
      - install -Dm644 packaging/assets/logo/\${FLATPAK_ID}.svg /app/share/icons/hicolor/scalable/apps/\${FLATPAK_ID}.svg
    sources:
      - type: file
        path: ${jar_path}
        dest-filename: corese-gui-standalone.jar
      - type: dir
        path: ${REPO_ROOT}
EOF
}

build_flatpak_repo() {
    mkdir -p "$BUILD_DIR" "$LOCAL_REPO_DIR"
    log "Building local Flatpak repository (branch: $LOCAL_BRANCH)..."
    flatpak-builder \
        --user \
        --force-clean \
        --disable-rofiles-fuse \
        --state-dir="$BUILDER_ROOT" \
        --install-deps-from=flathub \
        --default-branch="$LOCAL_BRANCH" \
        --repo="$LOCAL_REPO_DIR" \
        "$BUILD_DIR" \
        "$LOCAL_MANIFEST_PATH"
}

local_app_ref() {
    printf 'app/%s/%s/%s\n' "$APP_ID" "$(flatpak --default-arch)" "$LOCAL_BRANCH"
}

local_repo_url() {
    printf 'file://%s\n' "$LOCAL_REPO_DIR"
}

sync_local_remote() {
    local repo_url
    repo_url="$(local_repo_url)"

    if flatpak remotes --user --columns=name | grep -Fxq "$LOCAL_REMOTE_NAME"; then
        flatpak --user remote-modify --no-gpg-verify --url="$repo_url" "$LOCAL_REMOTE_NAME"
    else
        flatpak --user remote-add --no-gpg-verify "$LOCAL_REMOTE_NAME" "$repo_url"
    fi
}

install_local_flatpak() {
    [[ -d "$LOCAL_REPO_DIR" ]] || die "Local Flatpak repo not found at $LOCAL_REPO_DIR. Run 'build' first."
    log "Installing local Flatpak branch '$LOCAL_BRANCH'..."
    sync_local_remote
    flatpak --user install --noninteractive --assumeyes --reinstall "$LOCAL_REMOTE_NAME" "$(local_app_ref)"
}

run_local_flatpak() {
    log "Running $APP_ID (branch: $LOCAL_BRANCH)..."
    flatpak run --branch="$LOCAL_BRANCH" "$APP_ID" "$@"
}

has_local_installation() {
    flatpak info --user "$APP_ID" "$LOCAL_BRANCH" >/dev/null 2>&1
}

uninstall_local_flatpak() {
    if has_local_installation; then
        log "Uninstalling local Flatpak branch '$LOCAL_BRANCH'..."
        flatpak uninstall --user --noninteractive --assumeyes --delete-data "$(local_app_ref)"
    else
        log "No local installation found for branch '$LOCAL_BRANCH'."
    fi
}

remove_local_remote() {
    if flatpak remotes --user --columns=name | grep -Fxq "$LOCAL_REMOTE_NAME"; then
        log "Removing local Flatpak remote '$LOCAL_REMOTE_NAME'..."
        flatpak --user remote-delete "$LOCAL_REMOTE_NAME"
    fi
}

clean_local_artifacts() {
    log "Removing generated local Flatpak artifacts..."
    rm -rf "$BUILDER_ROOT"
    rm -rf "$LEGACY_BUILDER_ROOT"
    rm -f "$LOCAL_MANIFEST_PATH"
    if command -v flatpak >/dev/null 2>&1; then
        remove_local_remote || true
    fi
}

deps_cmd() {
    need_cmd flatpak
    require_linux
    ensure_flathub_remote
    log "Installing Flatpak runtime dependencies in user scope..."
    flatpak --user install --noninteractive --assumeyes flathub \
        "${RUNTIME}//${RUNTIME_VERSION}" \
        "${SDK}//${RUNTIME_VERSION}" \
        "${OPENJDK_EXTENSION}//${RUNTIME_VERSION}"
}

build_cmd() {
    local skip_gradle="${1:-0}"
    need_cmd flatpak
    need_cmd flatpak-builder
    [[ -x "$REPO_ROOT/gradlew" ]] || die "Missing executable Gradle wrapper: $REPO_ROOT/gradlew"
    require_linux
    ensure_flathub_remote

    if [[ "$skip_gradle" != "1" ]]; then
        build_local_jar
    fi

    local jar_path
    jar_path="$(resolve_standalone_jar)"
    log "Using standalone JAR: $jar_path"

    write_local_manifest "$jar_path"
    build_flatpak_repo
}

install_cmd() {
    need_cmd flatpak
    require_linux
    install_local_flatpak
}

run_cmd() {
    need_cmd flatpak
    require_linux
    run_local_flatpak "$@"
}

test_cmd() {
    local skip_gradle="$1"
    shift || true
    build_cmd "$skip_gradle"
    install_cmd
    run_cmd "$@"
}

parse_skip_gradle_flag() {
    local skip_gradle="0"
    if [[ "${1:-}" == "--skip-gradle" ]]; then
        skip_gradle="1"
    fi
    printf '%s\n' "$skip_gradle"
}

if [[ $# -lt 1 ]]; then
    usage
    exit 1
fi

command_name="$1"
shift

case "$command_name" in
    deps)
        deps_cmd
        ;;
    build)
        skip_gradle="$(parse_skip_gradle_flag "${1:-}")"
        if [[ "${1:-}" == "--skip-gradle" ]]; then
            shift
        fi
        [[ $# -eq 0 ]] || die "Unknown argument(s) for build: $*"
        build_cmd "$skip_gradle"
        ;;
    install)
        [[ $# -eq 0 ]] || die "Unknown argument(s) for install: $*"
        install_cmd
        ;;
    run)
        if [[ "${1:-}" == "--" ]]; then
            shift
        fi
        run_cmd "$@"
        ;;
    test)
        skip_gradle="$(parse_skip_gradle_flag "${1:-}")"
        if [[ "${1:-}" == "--skip-gradle" ]]; then
            shift
        fi
        if [[ "${1:-}" == "--" ]]; then
            shift
        fi
        test_cmd "$skip_gradle" "$@"
        ;;
    uninstall)
        [[ $# -eq 0 ]] || die "Unknown argument(s) for uninstall: $*"
        need_cmd flatpak
        require_linux
        uninstall_local_flatpak
        ;;
    clean)
        [[ $# -eq 0 ]] || die "Unknown argument(s) for clean: $*"
        clean_local_artifacts
        ;;
    reset)
        [[ $# -eq 0 ]] || die "Unknown argument(s) for reset: $*"
        need_cmd flatpak
        require_linux
        uninstall_local_flatpak
        clean_local_artifacts
        ;;
    -h | --help | help)
        usage
        ;;
    *)
        usage
        die "Unknown command: $command_name"
        ;;
esac
