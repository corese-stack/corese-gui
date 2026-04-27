package fr.inria.corese.gui.component.graph;

import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

class GraphViewerPerformanceResourceTest {

	@Test
	void performanceOptimizations_hideLabelsDuringHeavyInteractionAndInitialSettle() throws IOException {
		try (InputStream resourceStream = GraphViewerPerformanceResourceTest.class
				.getResourceAsStream("/graph-viewer/js/kg-graph.js")) {
			assertNotNull(resourceStream, "Graph viewer resource should be available on the classpath.");
			String script = new String(resourceStream.readAllBytes(), StandardCharsets.UTF_8);

			assertTrue(script.contains("this.interactionHideNodeThreshold = 160;"),
					"Graph viewer should hide labels during interaction earlier on medium graphs.");
			assertTrue(script.contains("this.interactionHideLinkThreshold = 200;"),
					"Graph viewer should hide edge labels during interaction earlier on medium graphs.");
			assertTrue(script.contains("this.labelsHiddenForSettling = false;"),
					"Graph viewer should track temporary label suppression while the initial layout settles.");
			assertTrue(script.contains("Labels temporarily hidden while the initial layout settles."),
					"Render profile should explain when labels are hidden during the initial settle phase.");
			assertTrue(script.contains("hardStopSimulation()"),
					"Graph viewer should expose a dedicated hard-stop path for converged simulations.");
			assertTrue(script.contains("this.simulation.alphaTarget(0).alpha(0).stop();"),
					"Converged simulations should be hard-stopped instead of only lowering alpha.");
		}
	}
}
