package fr.inria.corese.gui.core.service.data;

import fr.inria.corese.gui.core.service.DefaultReasoningService;
import fr.inria.corese.gui.core.service.GraphProjectionService;
import fr.inria.corese.gui.core.service.RdfDataService;
import fr.inria.corese.gui.core.service.ReasoningProfile;
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
import static org.junit.jupiter.api.Assertions.assertNotNull;
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
		assertEquals(2, reloadEntry.insertedCount(),
				"Reload log should report inserted triples from the clean baseline.");
		assertEquals(0, reloadEntry.deletedCount(),
				"Reload log should report deleted triples as zero after a clean baseline.");
		assertEquals(2, reloadEntry.totalTripleCount(),
				"Reload log should keep the post-operation total triple count snapshot.");
		GraphActivityLogEntry cleanEntry = activityLogService.snapshot().stream()
				.filter(entry -> "Cleared data graph before reload".equals(entry.action())).findFirst().orElseThrow();
		assertEquals(0, cleanEntry.totalTripleCount(),
				"Clean log should expose the temporary zero-triple state before reloading sources.");
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
		IllegalStateException exception = assertThrows(IllegalStateException.class,
				() -> workspaceService.reloadSources(selectedSources));
		assertTrue(exception.getMessage().contains("Reload sources failed"),
				"Failure should expose reload context in the top-level error.");
		Throwable rootCause = exception.getCause();
		assertNotNull(rootCause, "Top-level contextual exception should keep the underlying cause.");
		assertTrue(rootCause.getMessage().contains("does not exist"),
				"Failure cause should still expose the missing-source reason.");

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

	@Test
	void reloadWithActiveRdfs_preservesProfileAndLogsRecomputeWithoutReset() throws IOException {
		File baseline = writeTempTurtle("baseline-rdfs.ttl", """
				@prefix ex: <http://example.org/> .
				@prefix rdfs: <http://www.w3.org/2000/01/rdf-schema#> .
				ex:Dog rdfs:subClassOf ex:Animal .
				ex:fido a ex:Dog .
				""");
		File replacement = writeTempTurtle("replacement-rdfs.ttl", """
				@prefix ex: <http://example.org/> .
				@prefix rdfs: <http://www.w3.org/2000/01/rdf-schema#> .
				ex:Cat rdfs:subClassOf ex:Mammal .
				ex:felix a ex:Cat .
				""");

		workspaceService.loadFile(baseline);
		reasoningService.setEnabled(ReasoningProfile.RDFS, true);
		assertTrue(reasoningService.isEnabled(ReasoningProfile.RDFS), "RDFS should be enabled before reload.");
		assertTrue(reasoningTripleCount(ReasoningProfile.RDFS) > 0,
				"RDFS should produce inferred triples before reload.");

		activityLogService.clear();
		int reloaded = reloadLikeDataViewController(
				List.of(new DataSource(SourceType.FILE, replacement.getAbsolutePath())));

		assertEquals(1, reloaded, "Exactly one selected source should be reloaded.");
		assertTrue(reasoningService.isEnabled(ReasoningProfile.RDFS), "RDFS must remain enabled after reload.");
		assertTrue(reasoningTripleCount(ReasoningProfile.RDFS) > 0,
				"RDFS inferred triples should be recomputed after reload.");
		assertTrue(
				activityLogService.snapshot().stream()
						.anyMatch(entry -> "Reloaded data sources".equals(entry.action())),
				"Reload should still emit the data workspace log entry.");
		GraphActivityLogEntry reloadEntry = activityLogService.snapshot().stream()
				.filter(entry -> "Reloaded data sources".equals(entry.action())).findFirst().orElseThrow();
		assertTrue(reloadEntry.details().contains("will be recomputed"),
				"Reload log should explain the temporary inference drop when reasoning is enabled.");
		GraphActivityLogEntry cleanEntry = activityLogService.snapshot().stream()
				.filter(entry -> "Cleared data graph before reload".equals(entry.action())).findFirst().orElseThrow();
		assertEquals(0, cleanEntry.totalTripleCount(),
				"Clean log should expose the temporary zero-triple state before reasoning recompute.");
		assertTrue(
				activityLogService.snapshot().stream()
						.anyMatch(entry -> "Recomputed reasoning inferences".equals(entry.action())),
				"Reload with active reasoning should emit a reasoning recompute log entry.");
		assertTrue(
				activityLogService.snapshot().stream()
						.noneMatch(entry -> "Reset reasoning profiles".equals(entry.action())),
				"Reload must not reset reasoning profiles anymore.");
	}

	@Test
	void reloadEmptySelection_keepsRdfsEnabledAndDoesNotLogReset() throws IOException {
		File baseline = writeTempTurtle("baseline-empty-selection.ttl", """
				@prefix ex: <http://example.org/> .
				@prefix rdfs: <http://www.w3.org/2000/01/rdf-schema#> .
				ex:Dog rdfs:subClassOf ex:Animal .
				ex:fido a ex:Dog .
				""");
		workspaceService.loadFile(baseline);
		reasoningService.setEnabled(ReasoningProfile.RDFS, true);
		assertTrue(reasoningService.isEnabled(ReasoningProfile.RDFS), "RDFS should be enabled before empty reload.");

		activityLogService.clear();
		int reloaded = reloadLikeDataViewController(List.of());

		assertEquals(0, reloaded, "Empty selection reload should return 0.");
		assertTrue(reasoningService.isEnabled(ReasoningProfile.RDFS),
				"RDFS should stay enabled even when reload selection is empty.");
		assertEquals(0, workspaceService.getTripleCount(), "Empty selection reload should clear graph triples.");
		assertTrue(
				activityLogService.snapshot().stream()
						.anyMatch(entry -> "Reloaded data sources (empty selection)".equals(entry.action())),
				"Empty selection reload should keep its dedicated data log entry.");
		assertTrue(
				activityLogService.snapshot().stream()
						.noneMatch(entry -> "Recomputed reasoning inferences".equals(entry.action())),
				"No reasoning recompute should run when no source is reloaded.");
		assertTrue(
				activityLogService.snapshot().stream()
						.noneMatch(entry -> "Reset reasoning profiles".equals(entry.action())),
				"Empty selection reload must not reset reasoning profiles.");
	}

	private int reloadLikeDataViewController(List<DataSource> selectedSources) {
		int reloaded = workspaceService.reloadSources(selectedSources);
		if (reloaded > 0 && reasoningService.hasAnyEnabledProfile()) {
			reasoningService.recomputeEnabledProfiles();
		}
		return reloaded;
	}

	private int reasoningTripleCount(ReasoningProfile profile) {
		return workspaceService.getStatus().reasoningStats().stream()
				.filter(stat -> stat.graphName().equals(profile.namedGraphUri()))
				.mapToInt(DataWorkspaceStatus.ReasoningStat::tripleCount).findFirst().orElse(0);
	}

	private File writeTempTurtle(String fileName, String content) throws IOException {
		Path filePath = tempDir.resolve(fileName);
		Files.writeString(filePath, content, StandardCharsets.UTF_8);
		return filePath.toFile();
	}
}
