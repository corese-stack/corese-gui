package fr.inria.corese.gui.core.service;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.regex.Pattern;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

class ReasoningEmptyWorkspaceSnapshotTest {

	private static final Pattern INFERENCE_GRAPH_PATTERN = Pattern
			.compile("\"@id\"\\s*:\\s*\"urn:corese:inference:owlrl(?:-lite)?\"");

	private final RdfDataService rdfDataService = RdfDataService.getInstance();
	private final GraphProjectionService projectionService = GraphProjectionService.getInstance();
	private final ReasoningService reasoningService = DefaultReasoningService.getInstance();
	private final DataWorkspaceService workspaceService = DefaultDataWorkspaceService.getInstance();

	@BeforeEach
	void setUp() {
		reasoningService.resetAllProfiles();
		rdfDataService.clearData();
	}

	@AfterEach
	void tearDown() {
		reasoningService.resetAllProfiles();
		rdfDataService.clearData();
	}

	@Test
	void owlRlOnEmptyWorkspace_serializesInferenceNamedGraphContainer() throws Exception {
		reasoningService.setEnabled(ReasoningProfile.OWL_RL, true);

		String snapshot = projectionService.snapshotJsonLd();
		assertFalse(snapshot.isBlank(), "Snapshot should not be blank after OWL RL on empty workspace.");
		assertTrue(snapshot.contains("\"@graph\""), "Snapshot should include @graph containers.");
		assertTrue(INFERENCE_GRAPH_PATTERN.matcher(snapshot).find(),
				"Snapshot should include OWL RL inference named graph container.");
		assertTrue(workspaceService.getStatus().namedGraphCount() > 0,
				"Status should report at least one named graph in this scenario.");
	}

	@Test
	void owlRlLiteOnEmptyWorkspace_serializesInferenceNamedGraphContainer() throws Exception {
		reasoningService.setEnabled(ReasoningProfile.OWL_RL_LITE, true);

		String snapshot = projectionService.snapshotJsonLd();
		assertFalse(snapshot.isBlank(), "Snapshot should not be blank after OWL RL Lite on empty workspace.");
		assertTrue(snapshot.contains("\"@graph\""), "Snapshot should include @graph containers.");
		assertTrue(INFERENCE_GRAPH_PATTERN.matcher(snapshot).find(),
				"Snapshot should include OWL RL/Lite inference named graph container.");
		assertTrue(workspaceService.getStatus().namedGraphCount() > 0,
				"Status should report at least one named graph in this scenario.");
	}
}
