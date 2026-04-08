package fr.inria.corese.gui.component.graph;

import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

class GraphViewerLegendResourceTest {

	@Test
	void componentLegend_labelsLinksAsTripleLinks() throws IOException {
		try (InputStream resourceStream = GraphViewerLegendResourceTest.class
				.getResourceAsStream("/graph-viewer/js/kg-graph.js")) {
			assertNotNull(resourceStream, "Graph viewer resource should be available on the classpath.");
			String script = new String(resourceStream.readAllBytes(), StandardCharsets.UTF_8);

			assertTrue(script.contains("<span>Triple Link</span>"),
					"Legend should describe rendered RDF links as triple links.");
			assertFalse(script.contains("<span>Predicate Link</span>"),
					"Legacy predicate-link legend label should no longer be present.");
		}
	}
}
