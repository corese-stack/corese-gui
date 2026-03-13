#!/usr/bin/env bash
set -euo pipefail

if [[ $# -ne 2 ]]; then
    echo "Usage: $0 <json_output_file> <html_output_file>"
    exit 1
fi

json_output_file="$1"
html_output_file="$2"

docs_base_url="${DOCS_BASE_URL:-https://corese-stack.github.io/corese-gui}"
legacy_docs_base_url="${LEGACY_DOCS_BASE_URL:-https://corese-stack.github.io/corese-gui-swing}"
minimal_version="${MINIMAL_VERSION:-5.0.0}"
legacy_minimal_version="${LEGACY_MINIMAL_VERSION:-4.6.0}"
dev_prerelease_ref="${DEV_PRERELEASE_REF:-dev-prerelease}"
semver_pattern='^v[0-9]+\.[0-9]+\.[0-9]+$'

mkdir -p "$(dirname "$json_output_file")" "$(dirname "$html_output_file")"

version_greater_equal() {
    local current="$1"
    local minimum="$2"
    [[ "$(printf '%s\n%s\n' "$current" "$minimum" | sort -V | head -n 1)" == "$minimum" ]]
}

tags="$(git tag --sort=-v:refname | grep -E "$semver_pattern" || true)"
stable_tags=()
legacy_tags=()
latest_stable=""

if [[ -v PUBLISHED_STABLE_TAGS ]]; then
    stable_candidates="$(tr ',' '\n' <<< "${PUBLISHED_STABLE_TAGS}" | grep -E "$semver_pattern" | sort -Vr | uniq || true)"
else
    stable_candidates="$tags"
fi

for tag in $stable_candidates; do
    version="${tag#v}"
    if version_greater_equal "$version" "$minimal_version"; then
        stable_tags+=("$tag")
        if [[ -z "$latest_stable" ]]; then
            latest_stable="$tag"
        fi
    fi
done

for tag in $tags; do
    version="${tag#v}"
    if version_greater_equal "$version" "$legacy_minimal_version" && ! version_greater_equal "$version" "$minimal_version"; then
        legacy_tags+=("$tag")
    fi
done

has_dev_prerelease=false
if git show-ref --verify --quiet "refs/tags/${dev_prerelease_ref}"; then
    has_dev_prerelease=true
fi

preferred_target=""
if [[ -n "$latest_stable" ]]; then
    preferred_target="stable"
elif [[ "$has_dev_prerelease" == "true" ]]; then
    preferred_target="dev"
elif [[ ${#legacy_tags[@]} -gt 0 ]]; then
    preferred_target="legacy"
fi

json_entries=()
html_list_items=()

add_entry() {
    local name="$1"
    local version="$2"
    local url="$3"
    local preferred="$4"
    json_entries+=("  {
    \"name\": \"$name\",
    \"version\": \"$version\",
    \"url\": \"$url\",
    \"preferred\": $preferred
  }")
    html_list_items+=("    <li><a href=\"$url\">$name</a></li>")
}

for i in "${!stable_tags[@]}"; do
    tag="${stable_tags[$i]}"
    if (( i == 0 )); then
        name="$tag (latest)"
    else
        name="$tag"
    fi
    preferred="false"
    if [[ "$preferred_target" == "stable" && "$tag" == "$latest_stable" ]]; then
        preferred="true"
    fi
    add_entry "$name" "$tag" "${docs_base_url%/}/$tag/" "$preferred"
done

if [[ "$has_dev_prerelease" == "true" ]]; then
    preferred="false"
    if [[ "$preferred_target" == "dev" ]]; then
        preferred="true"
    fi
    add_entry "${dev_prerelease_ref} (preview)" "$dev_prerelease_ref" "${docs_base_url%/}/${dev_prerelease_ref}/" "$preferred"
fi

for i in "${!legacy_tags[@]}"; do
    tag="${legacy_tags[$i]}"
    name="$tag (legacy)"
    preferred="false"
    if [[ "$preferred_target" == "legacy" && "$i" -eq 0 ]]; then
        preferred="true"
    fi
    add_entry "$name" "$tag" "${legacy_docs_base_url%/}/$tag/" "$preferred"
done

{
    printf '[\n'
    for i in "${!json_entries[@]}"; do
        printf '%s' "${json_entries[$i]}"
        if (( i < ${#json_entries[@]} - 1 )); then
            printf ',\n'
        else
            printf '\n'
        fi
    done
    printf ']\n'
} > "$json_output_file"

if [[ ${#json_entries[@]} -eq 0 ]]; then
    cat > "$html_output_file" <<EOF
<html>
<head>
  <title>Corese-GUI Documentation Versions</title>
</head>
<body>
  <h1>Corese-GUI Documentation Versions</h1>
  <p>No published documentation version was found yet.</p>
</body>
</html>
EOF
else
    if [[ "$preferred_target" == "stable" ]]; then
        landing_target_url="${docs_base_url%/}/${latest_stable}/"
    elif [[ "$preferred_target" == "dev" ]]; then
        landing_target_url="${docs_base_url%/}/${dev_prerelease_ref}/"
    elif [[ "$preferred_target" == "legacy" ]]; then
        landing_target_url="${legacy_docs_base_url%/}/${legacy_tags[0]}/"
    fi

    {
        cat <<EOF
<html>
<head>
  <meta http-equiv="refresh" content="0; url=${landing_target_url}">
  <title>Corese-GUI Documentation Versions</title>
</head>
<body>
  <h1>Corese-GUI Documentation Versions</h1>
  <ul>
EOF
        printf '%s\n' "${html_list_items[@]}"
        cat <<EOF
  </ul>
  <p>If you are not redirected, click <a href="${landing_target_url}">here</a>.</p>
</body>
</html>
EOF
    } > "$html_output_file"
fi

echo "JSON data has been written to $json_output_file"
echo "HTML landing page has been written to $html_output_file"
