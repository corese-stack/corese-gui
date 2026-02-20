#!/usr/bin/env bash
set -euo pipefail

target="${1:?missing target (example: linux-x64)}"
jpackage_type="${2:?missing jpackage type (example: app-image)}"
out_dir="${3:?missing output directory}"

reset_output_directory() {
    rm -rf "$out_dir"
    mkdir -p "$out_dir"
}

copy_standalone_jar() {
    shopt -s nullglob
    local jars=(build/libs/*-standalone-"$target".jar)
    shopt -u nullglob
    if [[ ${#jars[@]} -eq 0 ]]; then
        echo "No standalone JAR found for target '$target'." >&2
        exit 1
    fi
    cp "${jars[@]}" "$out_dir/"
}

copy_installer_or_archive_app_image() {
    local jpackage_dir="build/jpackage/output/$target"
    if [[ ! -d "$jpackage_dir" ]]; then
        echo "Missing jpackage output directory: $jpackage_dir" >&2
        exit 1
    fi

    case "$jpackage_type" in
        app-image)
            local app_dir
            app_dir="$(find "$jpackage_dir" -mindepth 1 -maxdepth 1 -type d | sort | head -n 1 || true)"
            if [[ -z "$app_dir" ]]; then
                echo "No app-image directory found in $jpackage_dir" >&2
                exit 1
            fi

            local app_name
            app_name="$(basename "$app_dir")"
            tar -C "$jpackage_dir" -czf "$out_dir/${app_name}-${target}.tar.gz" "$app_name"
            ;;
        exe | msi | dmg | pkg | deb | rpm)
            shopt -s nullglob
            local installers=("$jpackage_dir"/*."$jpackage_type")
            shopt -u nullglob
            if [[ ${#installers[@]} -eq 0 ]]; then
                echo "No *.$jpackage_type installer found in $jpackage_dir" >&2
                exit 1
            fi
            for installer in "${installers[@]}"; do
                local installer_name installer_stem
                installer_name="$(basename "$installer")"
                installer_stem="${installer_name%.$jpackage_type}"
                cp "$installer" "$out_dir/${installer_stem}-${target}.${jpackage_type}"
            done
            ;;
        *)
            echo "Unsupported jpackage type '$jpackage_type'" >&2
            exit 1
            ;;
    esac
}

reset_output_directory
copy_standalone_jar
copy_installer_or_archive_app_image

echo "Prepared distribution assets:"
find "$out_dir" -maxdepth 1 -type f | sort | while IFS= read -r file; do
    printf ' - %s\n' "$(basename "$file")"
done
