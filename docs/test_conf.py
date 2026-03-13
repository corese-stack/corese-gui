import importlib.util
import json
import os
import re
import subprocess
import tempfile
import unittest
from pathlib import Path
from unittest import mock


def load_conf_module():
    conf_path = Path(__file__).resolve().parent / "source" / "conf.py"
    spec = importlib.util.spec_from_file_location("corese_gui_docs_conf", conf_path)
    module = importlib.util.module_from_spec(spec)
    assert spec.loader is not None
    spec.loader.exec_module(module)
    return module


class DocsDownloadUrlTests(unittest.TestCase):
    def setUp(self):
        self.conf = load_conf_module()

    def test_dev_prerelease_uses_snapshot_suffix_for_all_downloadable_artifacts(self):
        with mock.patch.dict(os.environ, {}, clear=False):
            tag, app_version, artifact_version, _ = self.conf._compute_download_context(
                "dev-prerelease"
            )
            urls = self.conf._build_download_urls(tag, app_version, artifact_version)

        self.assertEqual(tag, "dev-prerelease")
        self.assertEqual(app_version, "5.0.0")
        self.assertEqual(artifact_version, "5.0.0-SNAPSHOT")

        expected_snapshot_urls = (
            "windows_installer",
            "windows_portable",
            "windows_standalone",
            "macos_installer_arm64",
            "macos_installer_x64",
            "macos_standalone_arm64",
            "macos_standalone_x64",
            "linux_archive_x64",
            "linux_archive_arm64",
            "linux_standalone_x64",
            "linux_standalone_arm64",
        )
        for key in expected_snapshot_urls:
            self.assertIn("5.0.0-SNAPSHOT", urls[key], key)
            self.assertIn("/releases/download/dev-prerelease/", urls[key], key)

        self.assertEqual(
            urls["release_page"],
            "https://github.com/corese-stack/corese-gui/releases/tag/dev-prerelease",
        )
        self.assertEqual(urls["flathub_page"], "https://flathub.org/apps/fr.inria.corese.CoreseGui")

    def test_stable_release_uses_plain_version_for_all_downloadable_artifacts(self):
        tag, app_version, artifact_version, _ = self.conf._compute_download_context("v5.0.0")
        urls = self.conf._build_download_urls(tag, app_version, artifact_version)

        self.assertEqual(tag, "v5.0.0")
        self.assertEqual(app_version, "5.0.0")
        self.assertEqual(artifact_version, "5.0.0")

        downloadable_urls = (
            "windows_installer",
            "windows_portable",
            "windows_standalone",
            "macos_installer_arm64",
            "macos_installer_x64",
            "macos_standalone_arm64",
            "macos_standalone_x64",
            "linux_archive_x64",
            "linux_archive_arm64",
            "linux_standalone_x64",
            "linux_standalone_arm64",
        )
        for key in downloadable_urls:
            self.assertIn("5.0.0", urls[key], key)
            self.assertNotIn("SNAPSHOT", urls[key], key)
            self.assertIn("/releases/download/v5.0.0/", urls[key], key)

        self.assertEqual(
            urls["release_page"],
            "https://github.com/corese-stack/corese-gui/releases/tag/v5.0.0",
        )
        self.assertEqual(urls["flathub_page"], "https://flathub.org/apps/fr.inria.corese.CoreseGui")

    def test_readme_stable_download_links_match_generated_stable_urls(self):
        readme = (Path(__file__).resolve().parents[1] / "README.md").read_text(encoding="utf-8")
        readme_urls = sorted(
            re.findall(
                r"https://github\.com/corese-stack/corese-gui/releases/download/[^)\s]+",
                readme,
            )
        )

        tag, app_version, artifact_version, _ = self.conf._compute_download_context("v5.0.0")
        urls = self.conf._build_download_urls(tag, app_version, artifact_version)
        generated_urls = sorted(
            value
            for key, value in urls.items()
            if key not in {"release_page", "flathub_page"}
        )

        self.assertEqual(generated_urls, readme_urls)

    def test_non_versioned_build_prefers_published_stable_releases_from_environment(self):
        with mock.patch.dict(
            os.environ,
            {"PUBLISHED_STABLE_TAGS": "v5.0.0,v5.1.0"},
            clear=False,
        ):
            tag, app_version, artifact_version, _ = self.conf._compute_download_context("local")

        self.assertEqual(tag, "v5.1.0")
        self.assertEqual(app_version, "5.1.0")
        self.assertEqual(artifact_version, "5.1.0")

    def test_empty_published_stable_tags_skips_git_tag_lookup(self):
        with mock.patch.dict(os.environ, {"PUBLISHED_STABLE_TAGS": ""}, clear=False):
            with mock.patch.object(self.conf.subprocess, "run") as subprocess_run:
                latest_tag = self.conf._latest_supported_stable_tag()

        self.assertIsNone(latest_tag)
        subprocess_run.assert_not_called()

    def test_switcher_generator_prefers_published_stable_releases_from_environment(self):
        repo_root = Path(__file__).resolve().parents[1]

        with tempfile.TemporaryDirectory() as tmpdir:
            json_output = Path(tmpdir) / "switcher.json"
            html_output = Path(tmpdir) / "index.html"
            env = os.environ.copy()
            env.update(
                {
                    "DOCS_BASE_URL": "https://corese-stack.github.io/corese-gui",
                    "MINIMAL_VERSION": "5.0.0",
                    "PUBLISHED_STABLE_TAGS": "v5.0.0",
                }
            )

            subprocess.run(
                ["bash", "docs/switcher_generator.sh", str(json_output), str(html_output)],
                check=True,
                cwd=repo_root,
                env=env,
                capture_output=True,
                text=True,
            )

            switcher_entries = json.loads(json_output.read_text(encoding="utf-8"))
            html_output_text = html_output.read_text(encoding="utf-8")

        self.assertEqual(switcher_entries[0]["version"], "v5.0.0")
        self.assertEqual(switcher_entries[0]["name"], "v5.0.0 (latest)")
        self.assertTrue(switcher_entries[0]["preferred"])
        self.assertIn("https://corese-stack.github.io/corese-gui/v5.0.0/", html_output_text)


if __name__ == "__main__":
    unittest.main()
