"""Sphinx configuration for corese-gui documentation."""

import os
import pathlib
import re
import sys

sys.path.insert(0, pathlib.Path(__file__).parents[1].resolve().as_posix())
sys.path.insert(0, pathlib.Path(__file__).parents[2].resolve().as_posix())

project = "Corese-GUI"
author = "Wimmics"

version = ""
release = ""

DEFAULT_APP_VERSION = "5.0.0"
DEV_PRERELEASE_REF = "dev-prerelease"


def _compute_download_context(current_version: str) -> tuple[str, str, str, str]:
    """Return tag, app version, artifact version, and channel label for download links."""
    semver_match = re.fullmatch(r"v(\d+\.\d+\.\d+)", current_version)
    if semver_match:
        app_version = semver_match.group(1)
        download_tag = current_version
        artifact_version = app_version
        channel_label = f"Stable release ({download_tag})"
        return download_tag, app_version, artifact_version, channel_label

    if current_version == DEV_PRERELEASE_REF:
        app_version = os.environ.get("DEV_PRERELEASE_APP_VERSION", DEFAULT_APP_VERSION)
        app_version = app_version.lstrip("v")
        download_tag = DEV_PRERELEASE_REF
        artifact_version = f"{app_version}-SNAPSHOT"
        channel_label = "Development pre-release (dev-prerelease)"
        return download_tag, app_version, artifact_version, channel_label

    app_version = os.environ.get("DOCS_DEFAULT_APP_VERSION", DEFAULT_APP_VERSION)
    app_version = app_version.lstrip("v")
    download_tag = f"v{app_version}"
    artifact_version = app_version
    channel_label = f"Default release ({download_tag})"
    return download_tag, app_version, artifact_version, channel_label


def _build_download_urls(
    download_tag: str, app_version: str, artifact_version: str
) -> dict[str, str]:
    release_base = (
        f"https://github.com/corese-stack/corese-gui/releases/download/{download_tag}"
    )
    return {
        "release_page": (
            f"https://github.com/corese-stack/corese-gui/releases/tag/{download_tag}"
        ),
        "windows_installer": f"{release_base}/corese-gui-{artifact_version}-windows-x64.exe",
        "windows_portable": (
            f"{release_base}/corese-gui-{artifact_version}-windows-x64-portable.zip"
        ),
        "windows_standalone": (
            f"{release_base}/corese-gui-{artifact_version}-standalone-windows-x64.jar"
        ),
        "macos_installer_arm64": f"{release_base}/corese-gui-{artifact_version}-macos-arm64.dmg",
        "macos_installer_x64": f"{release_base}/corese-gui-{artifact_version}-macos-x64.dmg",
        "macos_standalone_arm64": (
            f"{release_base}/corese-gui-{artifact_version}-standalone-macos-arm64.jar"
        ),
        "macos_standalone_x64": (
            f"{release_base}/corese-gui-{artifact_version}-standalone-macos-x64.jar"
        ),
        "linux_archive_x64": f"{release_base}/corese-gui-{artifact_version}-linux-x64.tar.gz",
        "linux_archive_arm64": f"{release_base}/corese-gui-{artifact_version}-linux-arm64.tar.gz",
        "linux_standalone_x64": (
            f"{release_base}/corese-gui-{artifact_version}-standalone-linux-x64.jar"
        ),
        "linux_standalone_arm64": (
            f"{release_base}/corese-gui-{artifact_version}-standalone-linux-arm64.jar"
        ),
        "flathub_page": "https://flathub.org/apps/fr.inria.corese.CoreseGui",
    }


def setup(app):
    def set_version(app, config):
        current_version = getattr(app.config, "smv_current_version", None) or "main"
        config.version = current_version
        config.release = current_version
        html_theme_options["switcher"]["version_match"] = current_version
        download_tag, app_version, artifact_version, channel_label = _compute_download_context(
            current_version
        )
        download_urls = _build_download_urls(download_tag, app_version, artifact_version)
        url_definitions = "\n".join(
            f".. _{name}: {url}" for name, url in download_urls.items()
        )
        config.rst_epilog = (
            (config.rst_epilog or "")
            + f"""

.. |download_tag| replace:: {download_tag}
.. |app_version| replace:: {app_version}
.. |artifact_version| replace:: {artifact_version}
.. |download_channel| replace:: {channel_label}
{url_definitions}
"""
        )

    app.connect("config-inited", set_version)


extensions = [
    "sphinx.ext.duration",
    "sphinx.ext.todo",
    "sphinx_multiversion",
    "sphinx_design",
    "myst_parser",
    "sphinxcontrib.mermaid",
    "sphinx_copybutton",
]

templates_path = ["_templates"]
exclude_patterns = []
source_suffix = [".rst", ".md"]

html_theme = "pydata_sphinx_theme"
html_static_path = ["_static"]

html_css_files = ["css/custom.css"]
html_js_files = ["js/favicon-theme.js"]

html_logo = "_static/logo/corese-gui-logo.png"
html_title = "Corese-GUI Documentation"
html_short_title = "Corese-GUI Docs"

html_theme_options = {
    "logo": {
        "image_light": "_static/logo/corese-gui-logo.png",
        "image_dark": "_static/logo/corese-gui-logo.png",
    },
    "navbar_center": ["navbar-nav"],
    "navbar_end": ["theme-switcher", "navbar-icon-links", "version-switcher"],
    "icon_links": [
        {
            "name": "GitHub",
            "url": "https://github.com/corese-stack/corese-gui",
            "icon": "fab fa-github-square",
        }
    ],
    "switcher": {
        "json_url": "https://corese-stack.github.io/corese-gui/switcher.json",
        "version_match": version,
        "check_switcher": False,
    },
}

html_sidebars = {
    "install": [],
}

myst_heading_anchors = 4
myst_fence_as_directive = ["mermaid"]

highlight_language = "java"

todo_include_todos = True

# Build only semver release tags and main branch in multiversion output.
smv_tag_whitelist = r"^(v\d+\.\d+\.\d+|dev-prerelease)$"
smv_branch_whitelist = r"^main$"
