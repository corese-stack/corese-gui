package fr.inria.corese.gui.feature.data.support;

import fr.inria.corese.gui.component.graph.GraphDisplayWidget.GraphRenderCapabilities;
import fr.inria.corese.gui.component.graph.GraphDisplayWidget.GraphRenderMode;
import fr.inria.corese.gui.component.graph.GraphDisplayWidget.GraphRenderStatus;
import fr.inria.corese.gui.core.service.data.DataWorkspaceStatus;
import fr.inria.corese.gui.feature.data.support.DataStatusTooltipSupport.RenderStatusBadge;
import java.util.List;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

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

		assertEquals(List.of("Performance mode enabled",
				"Runtime: recent draw phase was heavy; conservative detail enabled."), lines);
	}

	@Test
	void compactTooltipLines_normalizesAndLimitsLines() {
		List<String> lines = DataStatusTooltipSupport
				.compactTooltipLines(List.of("  First line  ", "Second line", "Third line", "Fourth line"), 3);

		assertEquals(3, lines.size());
		assertEquals("First line", lines.get(0));
		assertEquals("Second line", lines.get(1));
		assertEquals("+ 2 more", lines.get(2));
		assertTrue(lines.stream().noneMatch(String::isBlank));
	}

	@Test
	void compactTooltipLines_singleLineLimitUsesMoreIndicator() {
		List<String> lines = DataStatusTooltipSupport.compactTooltipLines(List.of("A", "B", "C"), 1);

		assertEquals(List.of("+ 3 more"), lines);
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

	@Test
	void resolveRenderStatusBadge_returnsLockedWhenInteractionsAreDisabled() {
		GraphRenderStatus status = new GraphRenderStatus(GraphRenderMode.NORMAL, "Standard rendering",
				List.of("Graph interactions disabled for very large graph."));

		RenderStatusBadge badge = DataStatusTooltipSupport.resolveRenderStatusBadge(status);

		assertEquals(RenderStatusBadge.LOCKED, badge);
	}

	@Test
	void resolveRenderStatusBadge_returnsAdaptiveForIntermediateNormalModeDetails() {
		GraphRenderStatus status = new GraphRenderStatus(GraphRenderMode.NORMAL, "Adaptive detail mode",
				List.of("Node labels hidden at current zoom level."));

		RenderStatusBadge badge = DataStatusTooltipSupport.resolveRenderStatusBadge(status);

		assertEquals(RenderStatusBadge.ADAPTIVE, badge);
	}

	@Test
	void resolveRenderStatusBadge_returnsAdaptiveForSummarySignalWithoutDetails() {
		GraphRenderStatus status = new GraphRenderStatus(GraphRenderMode.NORMAL, "Adaptive detail mode", List.of());

		RenderStatusBadge badge = DataStatusTooltipSupport.resolveRenderStatusBadge(status);

		assertEquals(RenderStatusBadge.ADAPTIVE, badge);
	}

	@Test
	void resolveRenderStatusBadge_returnsLockedForSummarySignalWithoutDetails() {
		GraphRenderStatus status = new GraphRenderStatus(GraphRenderMode.NORMAL,
				"Interactions disabled for very large graph", List.of());

		RenderStatusBadge badge = DataStatusTooltipSupport.resolveRenderStatusBadge(status);

		assertEquals(RenderStatusBadge.LOCKED, badge);
	}

	@Test
	void resolveRenderStatusBadge_returnsFailedForRenderingErrors() {
		GraphRenderStatus status = new GraphRenderStatus(GraphRenderMode.PAUSED, "Rendering failed",
				List.of("JSON parse error"));

		RenderStatusBadge badge = DataStatusTooltipSupport.resolveRenderStatusBadge(status);

		assertEquals(RenderStatusBadge.FAILED, badge);
	}

	@Test
	void resolveRenderStatusBadge_usesCapabilitiesToDetectLockedState() {
		GraphRenderStatus status = new GraphRenderStatus(GraphRenderMode.NORMAL, "Standard rendering", List.of(),
				new GraphRenderCapabilities(false, false, false, false, true, true, true, true, true));

		RenderStatusBadge badge = DataStatusTooltipSupport.resolveRenderStatusBadge(status);

		assertEquals(RenderStatusBadge.LOCKED, badge);
	}

	@Test
	void buildRenderTooltipLines_usesCapabilitiesWhenDetailsAreMissing() {
		GraphRenderStatus status = new GraphRenderStatus(GraphRenderMode.NORMAL, "Standard rendering", List.of(),
				new GraphRenderCapabilities(true, true, true, true, false, false, true, true, true));

		List<String> lines = DataStatusTooltipSupport.buildRenderTooltipLines(status);

		assertEquals("Standard rendering", lines.get(0));
		assertTrue(lines.stream().anyMatch(line -> line.contains("Node labels")));
		assertTrue(lines.stream().anyMatch(line -> line.contains("Edge labels")));
	}

	@Test
	void buildReasoningTooltipLines_mentionsNativeRdfsSubsetWhenEnabled() {
		DataWorkspaceStatus status = new DataWorkspaceStatus(2, 2, 0, 2, 1, 1, 0, 0, List.of(), List.of(), true);

		List<String> lines = DataStatusTooltipSupport.buildReasoningTooltipLines(status);

		assertEquals(3, lines.size(), "Tooltip should explain materialized and query-time effects.");
		assertEquals("RDFS Subset: active", lines.get(0));
		assertTrue(lines.get(1).contains("kg:entailment"));
		assertTrue(lines.get(2).contains("query time"));
	}
}
