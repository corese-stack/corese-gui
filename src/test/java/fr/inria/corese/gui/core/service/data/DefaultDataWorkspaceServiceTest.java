package fr.inria.corese.gui.core.service.data;

import fr.inria.corese.gui.core.service.DefaultReasoningService;
import fr.inria.corese.gui.core.service.GraphProjectionService;
import fr.inria.corese.gui.core.service.RdfDataService;
import fr.inria.corese.gui.core.service.ReasoningService;
import fr.inria.corese.gui.core.service.activity.GraphActivityLogEntry;
import fr.inria.corese.gui.core.service.activity.GraphActivityLogService;
import java.io.File;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class DefaultDataWorkspaceServiceTest {

	@TempDir
	Path tempDir;

	private final DataWorkspaceService workspaceService = DefaultDataWorkspaceService.getInstance();
	private final RdfDataService rdfDataService = RdfDataService.getInstance();
	private final GraphProjectionService projectionService = GraphProjectionService.getInstance();
	private final DataSourceRegistryService sourceRegistryService = DataSourceRegistryService.getInstance();
	private final GraphActivityLogService activityLogService = GraphActivityLogService.getInstance();
	private final ReasoningService reasoningService = DefaultReasoningService.getInstance();

	@BeforeEach
	void clearStateBeforeEach() {
		clearWorkspaceState();
	}

	@AfterEach
	void clearStateAfterEach() {
		clearWorkspaceState();
	}

	private void clearWorkspaceState() {
		reasoningService.resetAllProfiles();
		reasoningService.removeAllRuleFiles();
		rdfDataService.clearData();
		sourceRegistryService.clear();
		activityLogService.clear();
	}

	@Test
	void reloadSources_logsTripleDeltaInsteadOfAbsoluteCounts() throws IOException {
		File baseline = writeTempTurtle("baseline.ttl", """
				@prefix ex: <http://example.org/> .
				ex:baselineNode ex:p ex:o .
				""");
		File replacement = writeTempTurtle("replacement.ttl", """
				@prefix ex: <http://example.org/> .
				ex:replacementNode ex:p ex:o1 .
				ex:replacementNode ex:p ex:o2 .
				""");

		workspaceService.loadFile(baseline);
		activityLogService.clear();

		int reloadedCount = workspaceService
				.reloadSources(List.of(new DataSource(SourceType.FILE, replacement.getAbsolutePath())));
		assertEquals(1, reloadedCount, "Exactly one selected source should be reloaded.");

		GraphActivityLogEntry reloadEntry = activityLogService.snapshot().stream()
				.filter(entry -> "Reloaded data sources".equals(entry.action())).findFirst().orElseThrow();
		assertEquals(1, reloadEntry.insertedCount(),
				"Reload log should report inserted triples as a delta, not as an absolute graph size.");
		assertEquals(0, reloadEntry.deletedCount(),
				"Reload log should report deleted triples as a delta, not as a previous graph size.");
		assertEquals(2, reloadEntry.totalTripleCount(),
				"Reload log should keep the post-operation total triple count snapshot.");
	}

	@Test
	void reloadSources_whenOneSourceFails_restoresPreviousGraphAndTrackedSources() throws IOException {
		File baseline = writeTempTurtle("baseline.ttl", """
				@prefix ex: <http://example.org/> .
				ex:rollbackBaselineNode ex:p ex:o .
				""");
		File candidate = writeTempTurtle("candidate.ttl", """
				@prefix ex: <http://example.org/> .
				ex:reloadCandidateNode ex:p ex:o .
				""");

		workspaceService.loadFile(baseline);
		List<DataSource> previousSources = workspaceService.getTrackedSources();
		activityLogService.clear();

		Path missingFile = tempDir.resolve("missing.ttl");
		DataSource candidateSource = new DataSource(SourceType.FILE, candidate.getAbsolutePath());
		DataSource missingSource = new DataSource(SourceType.FILE, missingFile.toString());
		List<DataSource> selectedSources = List.of(candidateSource, missingSource);
		IllegalArgumentException exception = assertThrows(IllegalArgumentException.class,
				() -> workspaceService.reloadSources(selectedSources));
		assertTrue(exception.getMessage().contains("does not exist"),
				"Failure should expose the missing-source cause.");

		assertEquals(previousSources, workspaceService.getTrackedSources(),
				"Tracked sources must be restored when reload fails.");
		assertEquals(1, workspaceService.getTripleCount(),
				"Graph triple count must be restored to the pre-reload state.");

		String snapshot = projectionService.snapshotJsonLd();
		assertTrue(snapshot.contains("rollbackBaselineNode"),
				"Baseline graph content should still be present after rollback.");
		assertFalse(snapshot.contains("reloadCandidateNode"),
				"Partially reloaded content should be removed by rollback.");
		assertTrue(
				activityLogService.snapshot().stream()
						.noneMatch(entry -> "Reloaded data sources".equals(entry.action())),
				"A failed reload must not emit a success activity entry.");
	}

	private File writeTempTurtle(String fileName, String content) throws IOException {
		Path filePath = tempDir.resolve(fileName);
		Files.writeString(filePath, content, StandardCharsets.UTF_8);
		return filePath.toFile();
	}
}
