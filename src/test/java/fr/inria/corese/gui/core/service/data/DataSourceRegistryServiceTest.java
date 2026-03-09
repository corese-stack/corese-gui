package fr.inria.corese.gui.core.service.data;

import java.io.File;
import java.util.ArrayList;
import java.util.List;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class DataSourceRegistryServiceTest {

	private final DataSourceRegistryService registry = DataSourceRegistryService.getInstance();

	@BeforeEach
	void clearBeforeEach() {
		registry.clear();
	}

	@AfterEach
	void clearAfterEach() {
		registry.clear();
	}

	@Test
	void registerFile_withNullFile_throwsIllegalArgumentException() {
		assertThrows(IllegalArgumentException.class, () -> registry.registerFile(null));
	}

	@Test
	void registerFileAndUri_deduplicatesAndNormalizesSources() {
		File file = new File("/tmp/example.ttl");
		registry.registerFile(file);
		registry.registerFile(file);
		registry.registerUri("  https://example.org/data.ttl  ");
		registry.registerUri("https://example.org/data.ttl");

		List<DataSource> snapshot = registry.snapshot();
		assertEquals(2, snapshot.size(), "Duplicate file/URI registrations should be deduplicated.");
		assertTrue(snapshot.contains(new DataSource(SourceType.FILE, file.getAbsolutePath())),
				"Snapshot should contain normalized file source.");
		assertTrue(snapshot.contains(new DataSource(SourceType.URI, "https://example.org/data.ttl")),
				"Snapshot should contain trimmed URI source.");
	}

	@Test
	void replaceAll_ignoresNullEntriesAndDeduplicates() {
		DataSource fileSource = new DataSource(SourceType.FILE, "/tmp/a.ttl");
		DataSource uriSource = new DataSource(SourceType.URI, "https://example.org/a.ttl");
		List<DataSource> mixedSources = new ArrayList<>();
		mixedSources.add(fileSource);
		mixedSources.add(null);
		mixedSources.add(fileSource);
		mixedSources.add(uriSource);

		registry.replaceAll(mixedSources);

		List<DataSource> snapshot = registry.snapshot();
		assertEquals(2, snapshot.size(), "replaceAll should keep only unique non-null sources.");
		assertTrue(snapshot.contains(fileSource));
		assertTrue(snapshot.contains(uriSource));
	}

	@Test
	void snapshot_isImmutable() {
		registry.registerUri("https://example.org/data.ttl");
		List<DataSource> snapshot = registry.snapshot();
		DataSource additionalSource = new DataSource(SourceType.URI, "https://example.org/other.ttl");

		assertThrows(UnsupportedOperationException.class, () -> snapshot.add(additionalSource));
	}
}
