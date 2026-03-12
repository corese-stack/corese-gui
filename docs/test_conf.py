import importlib.util
import os
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


if __name__ == "__main__":
    unittest.main()
