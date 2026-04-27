package fr.inria.corese.gui.core.service.activity;

import fr.inria.corese.core.query.QueryProcess;
import fr.inria.corese.core.sparql.exceptions.EngineException;
import fr.inria.corese.gui.core.service.GraphStoreService;
import java.util.ArrayList;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assertions.fail;

class GraphActivityLogServiceTest {

	private final GraphActivityLogService logService = GraphActivityLogService.getInstance();

	@BeforeEach
	void setUp() {
		logService.clear();
		logService.setMaxEntriesForTesting(GraphActivityLogService.DEFAULT_MAX_ENTRIES);
		GraphStoreService.getInstance().clear();
	}

	@Test
	void log_addsEntriesNewestFirst() {
		logService.log(GraphActivityLogEntry.Source.DATA_WORKSPACE, "Load file A", "/tmp/a.ttl", 3, 0);
		logService.log(GraphActivityLogEntry.Source.QUERY_SERVICE, "Run update", "INSERT DATA ...", 1, 0);

		List<GraphActivityLogEntry> snapshot = logService.snapshot();
		assertEquals(2, snapshot.size(), "Two entries should be stored.");
		assertEquals("Run update", snapshot.get(0).action(), "Newest entry should appear first.");
		assertEquals("Load file A", snapshot.get(1).action(), "Oldest entry should appear last.");
	}

	@Test
	void log_trimsOldestEntriesWhenCapacityIsExceeded() {
		logService.setMaxEntriesForTesting(3);

		logService.log(GraphActivityLogEntry.Source.DATA_WORKSPACE, "A1", "", 1, 0);
		logService.log(GraphActivityLogEntry.Source.DATA_WORKSPACE, "A2", "", 1, 0);
		logService.log(GraphActivityLogEntry.Source.DATA_WORKSPACE, "A3", "", 1, 0);
		logService.log(GraphActivityLogEntry.Source.DATA_WORKSPACE, "A4", "", 1, 0);

		List<GraphActivityLogEntry> snapshot = logService.snapshot();
		assertEquals(3, snapshot.size(), "Capacity should be enforced.");
		assertEquals("A4", snapshot.get(0).action(), "Newest entry should be retained.");
		assertEquals("A2", snapshot.get(2).action(), "Oldest retained entry should be A2.");
		assertFalse(snapshot.stream().anyMatch(entry -> "A1".equals(entry.action())), "A1 should be trimmed.");
	}

	@Test
	void subscribe_receivesInitialAndSubsequentSnapshots() {
		List<List<GraphActivityLogEntry>> receivedSnapshots = new ArrayList<>();
		AutoCloseable subscription = logService.subscribe(receivedSnapshots::add);
		try {
			logService.log(GraphActivityLogEntry.Source.REASONING_SERVICE, "Reasoning recompute", "RDFS", 4, 0);
			logService.clear();
		} finally {
			try {
				subscription.close();
			} catch (Exception closeError) {
				throw new RuntimeException(closeError);
			}
		}

		assertTrue(receivedSnapshots.size() >= 3,
				"Subscriber should receive initial snapshot, update snapshot, and clear snapshot.");
		assertTrue(receivedSnapshots.get(0).isEmpty(), "Initial snapshot should be empty.");
		assertEquals(1, receivedSnapshots.get(1).size(), "Second snapshot should contain one entry.");
		assertTrue(receivedSnapshots.get(receivedSnapshots.size() - 1).isEmpty(),
				"Last snapshot should be empty after clear.");
	}

	@Test
	void log_usesDistinctVisibleTripleCountInGraphStateSnapshot() {
		insertData("""
				INSERT DATA {
				  <http://example.org/s> <http://example.org/p> <http://example.org/o> .
				  GRAPH <urn:corese:inference:rdfs> {
				    <http://example.org/s> <http://example.org/p> <http://example.org/o> .
				  }
				}
				""");

		logService.log(GraphActivityLogEntry.Source.REASONING_SERVICE, "Recomputed reasoning inferences", "RDFS", 1, 0);

		GraphActivityLogEntry entry = logService.snapshot().getFirst();
		assertEquals(1, entry.totalTripleCount(),
				"Activity log snapshot should use the same distinct visible triple count as the workspace status.");
		assertEquals(1, entry.namedGraphCount(),
				"Activity log snapshot should still report the managed inference named graph.");
	}

	private void insertData(String updateQuery) {
		try {
			QueryProcess.create(GraphStoreService.getInstance().getGraph()).query(updateQuery);
		} catch (EngineException e) {
			fail("Failed to prepare graph fixture: " + e.getMessage());
		}
	}
}
