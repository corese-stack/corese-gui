package fr.inria.corese.gui.feature.result.graph.support;

import static org.junit.jupiter.api.Assertions.assertEquals;

import fr.inria.corese.gui.component.graph.GraphDisplayWidget.GraphStats;
import java.util.List;
import java.util.stream.IntStream;
import org.junit.jupiter.api.Test;

class GraphResultStatusTooltipSupportTest {

	@Test
	void buildTriplesTooltipLines_matchesDataWordingWithoutInferenceSection() {
		GraphStats stats = new GraphStats(28, 3, List.of(new GraphStats.NamedGraphStat("urn:a", 10),
				new GraphStats.NamedGraphStat("urn:b", 8), new GraphStats.NamedGraphStat("urn:c", 5)));

		List<String> lines = GraphResultStatusTooltipSupport.buildTriplesTooltipLines(stats);

		assertEquals(List.of("Explicit: 28", "Default graph: 5", "Named graphs: 23"), lines);
	}

	@Test
	void buildNamedGraphTooltipLines_returnsNoNamedGraphMessageWhenCountIsZero() {
		GraphStats stats = new GraphStats(0, 0, List.of());

		List<String> lines = GraphResultStatusTooltipSupport.buildNamedGraphTooltipLines(stats);

		assertEquals(List.of("No named graph with triples."), lines);
	}

	@Test
	void buildNamedGraphTooltipLines_keepsIncomingOrderLikeDataStatus() {
		GraphStats stats = new GraphStats(32, 3, List.of(new GraphStats.NamedGraphStat("urn:z", 5),
				new GraphStats.NamedGraphStat("urn:a", 12), new GraphStats.NamedGraphStat("urn:b", 12)));

		List<String> lines = GraphResultStatusTooltipSupport.buildNamedGraphTooltipLines(stats);

		assertEquals(3, lines.size());
		assertEquals("urn:z: 5", lines.get(0));
		assertEquals("urn:a: 12", lines.get(1));
		assertEquals("urn:b: 12", lines.get(2));
	}

	@Test
	void buildNamedGraphTooltipLines_addsOverflowIndicatorBeyondPreviewLimit() {
		List<GraphStats.NamedGraphStat> namedGraphStats = IntStream.range(0, 10)
				.mapToObj(index -> new GraphStats.NamedGraphStat("urn:g" + index, 50 - index)).toList();
		GraphStats stats = new GraphStats(200, 10, namedGraphStats);

		List<String> lines = GraphResultStatusTooltipSupport.buildNamedGraphTooltipLines(stats);

		assertEquals(6, lines.size(), "Tooltip should keep top 5 rows plus overflow indicator.");
		assertEquals("+ 5 more", lines.get(5));
	}

	@Test
	void graphStats_normalizesNegativeValues() {
		GraphStats stats = new GraphStats(-5, -2, List.of(new GraphStats.NamedGraphStat("urn:valid", -7)));

		assertEquals(0, stats.tripleCount());
		assertEquals(0, stats.namedGraphCount());
		assertEquals(0, stats.namedGraphStats().getFirst().tripleCount());
	}
}
