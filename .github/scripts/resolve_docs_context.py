#!/usr/bin/env python3
"""Resolve documentation build context for the production docs workflow."""

from __future__ import annotations

import json
import os
import re
import sys
import urllib.error
import urllib.request


SEMVER_TAG_PATTERN = re.compile(r"v(\d+)\.(\d+)\.(\d+)")


def parse_semver_tag(tag: str) -> tuple[int, int, int] | None:
    match = SEMVER_TAG_PATTERN.fullmatch(tag)
    if match is None:
        return None
    return tuple(int(part) for part in match.groups())


def parse_minimal_version(raw_version: str) -> tuple[int, int, int]:
    normalized = raw_version.lstrip("v")
    parsed = parse_semver_tag(f"v{normalized}")
    if parsed is None:
        raise ValueError(f"Invalid MINIMAL_VERSION: {raw_version!r}")
    return parsed


def fetch_releases(repository: str, token: str) -> list[dict[str, object]]:
    raw_releases = os.environ.get("GITHUB_RELEASES_JSON")
    if raw_releases is not None:
        loaded = json.loads(raw_releases)
        if not isinstance(loaded, list):
            raise ValueError("GITHUB_RELEASES_JSON must decode to a list")
        return loaded

    if not repository or not token:
        return []

    request = urllib.request.Request(
        f"https://api.github.com/repos/{repository}/releases?per_page=100",
        headers={
            "Accept": "application/vnd.github+json",
            "Authorization": f"Bearer {token}",
            "X-GitHub-Api-Version": "2022-11-28",
        },
    )

    try:
        with urllib.request.urlopen(request) as response:
            loaded = json.load(response)
    except urllib.error.URLError as exc:
        raise RuntimeError(f"Failed to fetch GitHub releases for {repository}: {exc}") from exc

    if not isinstance(loaded, list):
        raise RuntimeError("GitHub releases API returned an unexpected payload")
    return loaded


def published_stable_tags(
    releases: list[dict[str, object]], minimum: tuple[int, int, int]
) -> list[str]:
    tags: list[tuple[tuple[int, int, int], str]] = []
    seen: set[str] = set()

    for release in releases:
        if not isinstance(release, dict):
            continue
        if bool(release.get("draft")) or bool(release.get("prerelease")):
            continue
        tag = str(release.get("tag_name", "")).strip()
        parsed = parse_semver_tag(tag)
        if parsed is None or parsed < minimum or tag in seen:
            continue
        tags.append((parsed, tag))
        seen.add(tag)

    tags.sort(reverse=True)
    return [tag for _, tag in tags]


def build_tag_whitelist(dev_prerelease_ref: str, stable_tags: list[str]) -> str:
    parts = [re.escape(dev_prerelease_ref), *(re.escape(tag) for tag in stable_tags)]
    return f"^({'|'.join(parts)})$"


def unique_semver_tags(tags: list[str], minimum: tuple[int, int, int]) -> list[str]:
    supported: list[tuple[tuple[int, int, int], str]] = []
    seen: set[str] = set()

    for tag in tags:
        parsed = parse_semver_tag(tag)
        if parsed is None or parsed < minimum or tag in seen:
            continue
        supported.append((parsed, tag))
        seen.add(tag)

    supported.sort(reverse=True)
    return [tag for _, tag in supported]


def resolve_context() -> dict[str, str]:
    event_name = os.environ.get("GITHUB_EVENT_NAME", "")
    ref_name = os.environ.get("GITHUB_REF_NAME", "")
    repository = os.environ.get("GITHUB_REPOSITORY", "")
    token = os.environ.get("GH_TOKEN", "")
    workflow_run_head_sha = os.environ.get("WORKFLOW_RUN_HEAD_SHA", "")
    release_tag_name = os.environ.get("RELEASE_TAG_NAME", "")
    dev_prerelease_ref = os.environ.get("DEV_PRERELEASE_REF", "dev-prerelease")
    minimum = parse_minimal_version(os.environ.get("MINIMAL_VERSION", "5.0.0"))

    source_ref = ref_name
    if event_name == "workflow_run":
        source_ref = workflow_run_head_sha or ref_name
    elif event_name == "release":
        source_ref = release_tag_name or ref_name

    stable_tags = published_stable_tags(fetch_releases(repository, token), minimum)
    if event_name == "release":
        stable_tags = unique_semver_tags([release_tag_name, *stable_tags], minimum)

    return {
        "source_ref": source_ref,
        "published_stable_tags": ",".join(stable_tags),
        "smv_tag_whitelist": build_tag_whitelist(dev_prerelease_ref, stable_tags),
    }


def main() -> int:
    try:
        context = resolve_context()
    except Exception as exc:
        print(str(exc), file=sys.stderr)
        return 1

    for key, value in context.items():
        print(f"{key}={value}")
    return 0


if __name__ == "__main__":
    raise SystemExit(main())
