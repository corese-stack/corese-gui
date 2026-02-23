package fr.inria.corese.gui.feature.data.support;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import fr.inria.corese.gui.component.graph.GraphDisplayWidget.GraphRenderMode;
import fr.inria.corese.gui.component.graph.GraphDisplayWidget.GraphRenderStatus;
import java.util.List;
import org.junit.jupiter.api.Test;

class DataStatusTooltipSupportTest {

	@Test
	void buildRenderTooltipLines_keepsTooltipConcise() {
		GraphRenderStatus status = new GraphRenderStatus(GraphRenderMode.DEGRADED, "Performance mode enabled",
				List.of("Link geometry simplified for dense graphs.",
						"Parallel link offset layout disabled for dense edges.",
						"Node tooltips disabled for very large graph."));

		List<String> lines = DataStatusTooltipSupport.buildRenderTooltipLines(status);

		assertEquals(4, lines.size(), "Tooltip should keep summary and precise optimization details.");
		assertEquals("Performance mode enabled", lines.get(0));
		assertEquals("Node tooltips: disabled for this graph size.", lines.get(1));
		assertEquals("Layout: link geometry simplified for dense graphs.", lines.get(2));
		assertEquals("Layout: parallel edge offsets disabled.", lines.get(3));
	}

	@Test
	void buildRenderTooltipLines_simplifiesRuntimeTimingDetail() {
		GraphRenderStatus status = new GraphRenderStatus(GraphRenderMode.DEGRADED, "Performance mode enabled",
				List.of("Recent draw phase took about 734 ms, applying conservative detail."));

		List<String> lines = DataStatusTooltipSupport.buildRenderTooltipLines(status);

		assertEquals(List.of("Performance mode enabled", "Runtime: recent draw phase was heavy; conservative detail enabled."),
				lines);
	}

	@Test
	void compactTooltipLines_normalizesAndLimitsLines() {
		List<String> lines = DataStatusTooltipSupport
				.compactTooltipLines(List.of("  First line  ", "Second line", "Second line", "Third line"), 3);

		assertEquals(3, lines.size());
		assertEquals("First line", lines.get(0));
		assertEquals("Second line", lines.get(1));
		assertEquals("Third line", lines.get(2));
		assertTrue(lines.stream().noneMatch(String::isBlank));
	}

	@Test
	void buildRenderTooltipLines_pausedKeepsDisplayAnywayAction() {
		GraphRenderStatus status = new GraphRenderStatus(GraphRenderMode.PAUSED, "Automatic preview paused",
				List.of("Detected 4,200 triples (auto-preview limit: 1,200).",
						"Threshold can be changed in Settings > Appearance > Graph Preview.",
						"Use \"Display anyway\" to force rendering on demand.",
						"Manual rendering can freeze the interface on very large graphs."));

		List<String> lines = DataStatusTooltipSupport.buildRenderTooltipLines(status);

		assertEquals(5, lines.size(), "Tooltip should keep key detection, actions, and warning details.");
		assertEquals("Automatic preview paused", lines.get(0));
		assertEquals("Detected 4,200 triples (auto-preview limit: 1,200).", lines.get(1));
		assertEquals("Action: adjust preview limit in Settings > Appearance > Graph Preview.", lines.get(2));
		assertEquals("Action: use \"Display anyway\" to force rendering now.", lines.get(3));
		assertEquals("Warning: manual rendering may freeze the interface on very large graphs.", lines.get(4));
	}
}
