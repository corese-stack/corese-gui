package fr.inria.corese.gui.core.service;

import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.File;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

class ReasoningGraphProjectionTest {

	private final RdfDataService rdfDataService = RdfDataService.getInstance();
	private final GraphProjectionService projectionService = GraphProjectionService.getInstance();
	private final ReasoningService reasoningService = DefaultReasoningService.getInstance();
	private final DataWorkspaceService workspaceService = DefaultDataWorkspaceService.getInstance();

	@BeforeEach
	void setUp() {
		reasoningService.resetAllProfiles();
		rdfDataService.clearData();
	}

	@Test
	void owlRlInference_isSerializedAsNamedGraphInJsonLdSnapshot() {
		File dataset = new File("reasoning-profile-verification/datasets/owlrl-symmetric.ttl");
		rdfDataService.loadFile(dataset);
		reasoningService.setEnabled(ReasoningProfile.OWL_RL, true);

		DataWorkspaceStatus status = workspaceService.getStatus();
		assertTrue(status.inferredTripleCount() > 0,
				"Expected inferred triples after enabling OWL RL for the symmetric-property dataset.");

		String jsonLd = projectionService.snapshotJsonLd();
		assertTrue(jsonLd.contains("urn:corese:inference:owlrl"),
				"JSON-LD snapshot should expose OWL RL named graph URI for inferred triples.");
		assertTrue(jsonLd.contains("\"@graph\""),
				"JSON-LD snapshot should keep named graph containers after reasoning.");
	}
}
