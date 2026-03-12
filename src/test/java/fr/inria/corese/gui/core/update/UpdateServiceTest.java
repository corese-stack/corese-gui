package fr.inria.corese.gui.core.update;

import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

class UpdateServiceTest {

	private static List<UpdateService.UpdateAsset> devPrereleaseAssetsFixture() {
		// Asset names copied from:
		// gh release view dev-prerelease --repo corese-stack/corese-gui --json assets
		return List.of(
				new UpdateService.UpdateAsset("corese-gui-5.0.0-SNAPSHOT-macos-arm64.dmg",
						"https://example.test/corese-gui-5.0.0-SNAPSHOT-macos-arm64.dmg"),
				new UpdateService.UpdateAsset("corese-gui-5.0.0-SNAPSHOT-macos-x64.dmg",
						"https://example.test/corese-gui-5.0.0-SNAPSHOT-macos-x64.dmg"),
				new UpdateService.UpdateAsset("corese-gui-5.0.0-SNAPSHOT-standalone-linux-arm64.jar",
						"https://example.test/corese-gui-5.0.0-SNAPSHOT-standalone-linux-arm64.jar"),
				new UpdateService.UpdateAsset("corese-gui-5.0.0-SNAPSHOT-standalone-linux-x64.jar",
						"https://example.test/corese-gui-5.0.0-SNAPSHOT-standalone-linux-x64.jar"),
				new UpdateService.UpdateAsset("corese-gui-5.0.0-SNAPSHOT-standalone-macos-arm64.jar",
						"https://example.test/corese-gui-5.0.0-SNAPSHOT-standalone-macos-arm64.jar"),
				new UpdateService.UpdateAsset("corese-gui-5.0.0-SNAPSHOT-standalone-macos-x64.jar",
						"https://example.test/corese-gui-5.0.0-SNAPSHOT-standalone-macos-x64.jar"),
				new UpdateService.UpdateAsset("corese-gui-5.0.0-SNAPSHOT-standalone-windows-x64.jar",
						"https://example.test/corese-gui-5.0.0-SNAPSHOT-standalone-windows-x64.jar"),
				new UpdateService.UpdateAsset("corese-gui-5.0.0-SNAPSHOT-windows-x64.exe",
						"https://example.test/corese-gui-5.0.0-SNAPSHOT-windows-x64.exe"),
				new UpdateService.UpdateAsset("corese-gui-5.0.0-SNAPSHOT-windows-x64-portable.zip",
						"https://example.test/corese-gui-5.0.0-SNAPSHOT-windows-x64-portable.zip"),
				new UpdateService.UpdateAsset("corese-gui-5.0.0-SNAPSHOT-linux-arm64.tar.gz",
						"https://example.test/corese-gui-5.0.0-SNAPSHOT-linux-arm64.tar.gz"),
				new UpdateService.UpdateAsset("corese-gui-5.0.0-SNAPSHOT-linux-x64.tar.gz",
						"https://example.test/corese-gui-5.0.0-SNAPSHOT-linux-x64.tar.gz"),
				new UpdateService.UpdateAsset("SHA256SUMS.txt", "https://example.test/SHA256SUMS.txt"));
	}

	@Test
	void compareVersions_handlesStableAndSnapshotOrdering() {
		assertTrue(UpdateService.isNewerVersion("5.0.0", "5.0.0-SNAPSHOT"));
		assertTrue(UpdateService.isNewerVersion("v5.1.0", "5.0.9"));
		assertFalse(UpdateService.isNewerVersion("5.0.0-beta1", "5.0.0"));
		assertEquals(0, UpdateService.compareVersions("v5.0.0", "5.0.0"));
	}

	@Test
	void parseReleasePayload_extractsReleaseAndAssets() {
		String payload = """
				{
				  "tag_name": "v5.1.0",
				  "name": "Corese GUI 5.1.0",
				  "html_url": "https://github.com/corese-stack/corese-gui/releases/tag/v5.1.0",
				  "published_at": "2026-02-23T12:00:00Z",
				  "draft": false,
				  "prerelease": false,
				  "assets": [
				    {
				      "name": "corese-gui-5.1.0-windows-x64.msi",
				      "browser_download_url": "https://example.test/corese-gui-5.1.0-windows-x64.msi",
				      "uploader": {
				        "login": "corese-bot"
				      }
				    },
				    {
				      "name": "corese-gui-5.1.0-standalone-linux-x64.jar",
				      "browser_download_url": "https://example.test/corese-gui-5.1.0-standalone-linux-x64.jar"
				    }
				  ]
				}
				""";

		UpdateService.ParsedRelease release = UpdateService.parseReleasePayload(payload);
		assertNotNull(release);
		assertEquals("v5.1.0", release.tagName());
		assertEquals("Corese GUI 5.1.0", release.name());
		assertEquals("https://github.com/corese-stack/corese-gui/releases/tag/v5.1.0", release.htmlUrl());
		assertNotNull(release.publishedAt());
		assertEquals(2, release.assets().size());
	}

	@Test
	void parseReleasePayload_returnsNullForInvalidPayload() {
		assertNull(UpdateService.parseReleasePayload(""));
		assertNull(UpdateService.parseReleasePayload("{\"name\":\"missing required fields\"}"));
	}

	@Test
	void selectPreferredAsset_prefersNativeInstallerOverStandaloneJar() {
		List<UpdateService.UpdateAsset> assets = List.of(
				new UpdateService.UpdateAsset("corese-gui-5.1.0-windows-x64.exe", "https://example.test/win.exe"),
				new UpdateService.UpdateAsset("corese-gui-5.1.0-windows-x64.msi", "https://example.test/win.msi"),
				new UpdateService.UpdateAsset("corese-gui-5.1.0-standalone-windows-x64.jar",
						"https://example.test/win.jar"));

		Optional<UpdateService.UpdateAsset> selected = UpdateService.selectPreferredAsset(assets, "windows", "x64");
		assertTrue(selected.isPresent());
		assertEquals("corese-gui-5.1.0-windows-x64.msi", selected.get().name());
	}

	@Test
	void selectPreferredAsset_prefersPortableZipWhenRuntimeIsPortable() {
		List<UpdateService.UpdateAsset> assets = List.of(
				new UpdateService.UpdateAsset("corese-gui-5.1.0-windows-x64.msi", "https://example.test/win.msi"),
				new UpdateService.UpdateAsset("corese-gui-5.1.0-windows-x64.exe", "https://example.test/win.exe"),
				new UpdateService.UpdateAsset("corese-gui-5.1.0-windows-x64-portable.zip",
						"https://example.test/win-portable.zip"));

		Optional<UpdateService.UpdateAsset> selected = UpdateService.selectPreferredAsset(assets, "windows", "x64",
				true);
		assertTrue(selected.isPresent());
		assertEquals("corese-gui-5.1.0-windows-x64-portable.zip", selected.get().name());
	}

	@Test
	void selectPreferredAsset_matchesCurrentDevPrerelease_linuxX64() {
		Optional<UpdateService.UpdateAsset> selected = UpdateService.selectPreferredAsset(devPrereleaseAssetsFixture(),
				"linux", "x64");
		assertTrue(selected.isPresent());
		assertEquals("corese-gui-5.0.0-SNAPSHOT-linux-x64.tar.gz", selected.get().name());
	}

	@Test
	void selectPreferredAsset_matchesCurrentDevPrerelease_linuxArm64() {
		Optional<UpdateService.UpdateAsset> selected = UpdateService.selectPreferredAsset(devPrereleaseAssetsFixture(),
				"linux", "arm64");
		assertTrue(selected.isPresent());
		assertEquals("corese-gui-5.0.0-SNAPSHOT-linux-arm64.tar.gz", selected.get().name());
	}

	@Test
	void selectPreferredAsset_matchesCurrentDevPrerelease_windowsX64() {
		Optional<UpdateService.UpdateAsset> selected = UpdateService.selectPreferredAsset(devPrereleaseAssetsFixture(),
				"windows", "x64");
		assertTrue(selected.isPresent());
		assertEquals("corese-gui-5.0.0-SNAPSHOT-windows-x64.exe", selected.get().name());
	}

	@Test
	void selectPreferredAsset_matchesCurrentDevPrerelease_windowsPortableX64() {
		Optional<UpdateService.UpdateAsset> selected = UpdateService.selectPreferredAsset(devPrereleaseAssetsFixture(),
				"windows", "x64", true);
		assertTrue(selected.isPresent());
		assertEquals("corese-gui-5.0.0-SNAPSHOT-windows-x64-portable.zip", selected.get().name());
	}

	@Test
	void selectPreferredAsset_matchesCurrentDevPrerelease_macosX64() {
		Optional<UpdateService.UpdateAsset> selected = UpdateService.selectPreferredAsset(devPrereleaseAssetsFixture(),
				"macos", "x64");
		assertTrue(selected.isPresent());
		assertEquals("corese-gui-5.0.0-SNAPSHOT-macos-x64.dmg", selected.get().name());
	}

	@Test
	void selectPreferredAsset_matchesCurrentDevPrerelease_macosArm64() {
		Optional<UpdateService.UpdateAsset> selected = UpdateService.selectPreferredAsset(devPrereleaseAssetsFixture(),
				"macos", "arm64");
		assertTrue(selected.isPresent());
		assertEquals("corese-gui-5.0.0-SNAPSHOT-macos-arm64.dmg", selected.get().name());
	}

	@Test
	void selectPreferredAsset_returnsEmptyWhenNoCompatibleAssetExists() {
		Optional<UpdateService.UpdateAsset> selected = UpdateService.selectPreferredAsset(devPrereleaseAssetsFixture(),
				"windows", "arm64");
		assertTrue(selected.isEmpty());
	}
}
