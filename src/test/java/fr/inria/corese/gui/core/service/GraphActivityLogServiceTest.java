package fr.inria.corese.gui.core.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.ArrayList;
import java.util.List;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

class GraphActivityLogServiceTest {

	private final GraphActivityLogService logService = GraphActivityLogService.getInstance();

	@BeforeEach
	void setUp() {
		logService.clear();
		logService.setMaxEntriesForTesting(GraphActivityLogService.DEFAULT_MAX_ENTRIES);
	}

	@AfterEach
	void tearDown() {
		logService.clear();
		logService.setMaxEntriesForTesting(GraphActivityLogService.DEFAULT_MAX_ENTRIES);
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
	void subscribe_receivesInitialAndSubsequentSnapshots() throws Exception {
		List<List<GraphActivityLogEntry>> receivedSnapshots = new ArrayList<>();
		AutoCloseable subscription = logService.subscribe(receivedSnapshots::add);
		try {
			logService.log(GraphActivityLogEntry.Source.REASONING_SERVICE, "Reasoning recompute", "RDFS", 4, 0);
			logService.clear();
		} finally {
			subscription.close();
		}

		assertTrue(receivedSnapshots.size() >= 3,
				"Subscriber should receive initial snapshot, update snapshot, and clear snapshot.");
		assertTrue(receivedSnapshots.get(0).isEmpty(), "Initial snapshot should be empty.");
		assertEquals(1, receivedSnapshots.get(1).size(), "Second snapshot should contain one entry.");
		assertTrue(receivedSnapshots.get(receivedSnapshots.size() - 1).isEmpty(),
				"Last snapshot should be empty after clear.");
	}
}
