package fr.inria.corese.gui.core.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assertions.fail;

import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import fr.inria.corese.core.Graph;
import fr.inria.corese.core.query.QueryProcess;
import fr.inria.corese.core.sparql.exceptions.EngineException;
import fr.inria.corese.gui.core.service.data.DataSource;
import fr.inria.corese.gui.core.service.data.DataWorkspaceStatus;
import fr.inria.corese.gui.core.service.data.SourceType;

class DataWorkspaceStatusSupportTest {

	private static final Logger LOGGER = LoggerFactory.getLogger(DataWorkspaceStatusSupportTest.class);

	@Test
	void computeSourceStats_countsFileAndUriSources() {
		List<DataSource> sources = List.of(new DataSource(SourceType.FILE, "/tmp/a.ttl"),
				new DataSource(SourceType.FILE, "/tmp/b.ttl"), new DataSource(SourceType.URI, "https://example.org/c"));

		DataWorkspaceStatusSupport.SourceStats stats = DataWorkspaceStatusSupport.computeSourceStats(sources);

		assertEquals(3, stats.total());
		assertEquals(2, stats.fileCount());
		assertEquals(1, stats.uriCount());
	}

	@Test
	void computeGraphCountSnapshot_splitsDefaultAndNamedGraphs() {
		Graph graph = Graph.create();
		insertData(graph, """
				INSERT DATA {
				  <http://example.org/s0> <http://example.org/p> <http://example.org/o0> .
				  GRAPH <http://example.org/g1> {
				    <http://example.org/s1> <http://example.org/p> <http://example.org/o1> .
				  }
				  GRAPH <http://example.org/g2> {
				    <http://example.org/s2> <http://example.org/p> <http://example.org/o2> .
				    <http://example.org/s3> <http://example.org/p> <http://example.org/o3> .
				  }
				}
				""");

		DataWorkspaceStatusSupport.GraphCountSnapshot snapshot = DataWorkspaceStatusSupport
				.computeGraphCountSnapshot(graph, graph.size(), LOGGER);

		assertEquals(1, snapshot.defaultGraphTripleCount(),
				"Unassigned/default triples should be counted in default graph stats.");
		assertEquals(2, snapshot.namedGraphCounts().size());
		assertEquals(1, snapshot.namedGraphCounts().get("http://example.org/g1"));
		assertEquals(2, snapshot.namedGraphCounts().get("http://example.org/g2"));
	}

	@Test
	void computeGraphCountSnapshot_normalizesCoreDefaultAliases() {
		Graph graph = Graph.create();
		insertData(graph, """
				INSERT DATA {
				  GRAPH <http://ns.inria.fr/edelweiss/2010/kgram/default> {
				    <http://example.org/s1> <http://example.org/p> <http://example.org/o1> .
				  }
				  GRAPH <http://example.org/g1> {
				    <http://example.org/s2> <http://example.org/p> <http://example.org/o2> .
				  }
				}
				""");

		DataWorkspaceStatusSupport.GraphCountSnapshot snapshot = DataWorkspaceStatusSupport
				.computeGraphCountSnapshot(graph, graph.size(), LOGGER);

		assertEquals(1, snapshot.defaultGraphTripleCount(),
				"Legacy default-graph aliases should be normalized into default graph count.");
		assertEquals(1, snapshot.namedGraphCounts().size(),
				"Normalized default graph alias should not remain in named graph stats.");
		assertEquals(1, snapshot.namedGraphCounts().get("http://example.org/g1"));
	}

	@Test
	void computeGraphCountSnapshot_withNullGraph_usesTotalAsDefaultCount() {
		DataWorkspaceStatusSupport.GraphCountSnapshot snapshot = DataWorkspaceStatusSupport
				.computeGraphCountSnapshot(null, 5, LOGGER);

		assertTrue(snapshot.namedGraphCounts().isEmpty());
		assertEquals(5, snapshot.defaultGraphTripleCount());
	}

	@Test
	void toSortedNamedGraphStats_ordersByCountThenGraphName() {
		Map<String, Integer> counts = Map.of("http://example.org/b", 2, "http://example.org/a", 2,
				"http://example.org/c", 1);

		List<DataWorkspaceStatus.NamedGraphStat> stats = DataWorkspaceStatusSupport.toSortedNamedGraphStats(counts);

		assertEquals(3, stats.size());
		assertEquals("http://example.org/a", stats.get(0).graphName());
		assertEquals(2, stats.get(0).tripleCount());
		assertEquals("http://example.org/b", stats.get(1).graphName());
		assertEquals(2, stats.get(1).tripleCount());
		assertEquals("http://example.org/c", stats.get(2).graphName());
		assertEquals(1, stats.get(2).tripleCount());
	}

	private static void insertData(Graph graph, String updateQuery) {
		try {
			QueryProcess.create(graph).query(updateQuery);
		} catch (EngineException e) {
			fail("Failed to prepare graph fixture: " + e.getMessage());
		}
	}
}
