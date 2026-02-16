package fr.inria.corese.gui.core.service;

import fr.inria.corese.core.Graph;
import java.util.ArrayDeque;
import java.util.Deque;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicLong;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Centralized in-memory timeline of graph-changing operations.
 *
 * <p>
 * Entries are stored in reverse chronological order (newest first) and capped
 * by a bounded capacity to keep memory usage predictable.
 */
@SuppressWarnings("java:S6548") // Singleton is intentional for app-wide activity timeline
public final class GraphActivityLogService {

	static final int DEFAULT_MAX_ENTRIES = 600;

	private static final Logger LOGGER = LoggerFactory.getLogger(GraphActivityLogService.class);
	private static final GraphActivityLogService INSTANCE = new GraphActivityLogService();

	private final Deque<GraphActivityLogEntry> entries = new ArrayDeque<>();
	private final Map<Long, GraphActivityLogListener> listeners = new ConcurrentHashMap<>();
	private final AtomicLong listenerIdGenerator = new AtomicLong();

	private volatile int maxEntries = DEFAULT_MAX_ENTRIES;

	private GraphActivityLogService() {
	}

	/**
	 * Returns the singleton service instance.
	 *
	 * @return service instance
	 */
	public static GraphActivityLogService getInstance() {
		return INSTANCE;
	}

	/**
	 * Registers an activity listener.
	 *
	 * @param listener
	 *            callback to invoke on updates
	 * @return handle used to unsubscribe
	 */
	public AutoCloseable subscribe(GraphActivityLogListener listener) {
		Objects.requireNonNull(listener, "listener must not be null");
		long listenerId = listenerIdGenerator.incrementAndGet();
		listeners.put(listenerId, listener);
		notifyListenerSafely(listener, snapshot());
		return () -> listeners.remove(listenerId);
	}

	/**
	 * Appends a new activity entry.
	 *
	 * @param entry
	 *            entry to append
	 */
	public void log(GraphActivityLogEntry entry) {
		if (entry == null) {
			return;
		}
		List<GraphActivityLogEntry> snapshot;
		synchronized (this) {
			entries.addFirst(entry);
			trimToCapacity();
			snapshot = List.copyOf(entries);
		}
		notifyListeners(snapshot);
	}

	/**
	 * Convenience overload to append a timestamped entry.
	 *
	 * @param source
	 *            activity source
	 * @param action
	 *            short action label
	 * @param details
	 *            optional details
	 * @param insertedCount
	 *            inserted triples count
	 * @param deletedCount
	 *            deleted triples count
	 */
	public void log(GraphActivityLogEntry.Source source, String action, String details, int insertedCount,
			int deletedCount) {
		GraphStateSnapshot graphState = snapshotGraphState();
		log(GraphActivityLogEntry.now(source, action, details, insertedCount, deletedCount,
				graphState.totalTripleCount(), graphState.namedGraphCount()));
	}

	/**
	 * Clears all activity entries.
	 */
	public void clear() {
		List<GraphActivityLogEntry> snapshot;
		synchronized (this) {
			if (entries.isEmpty()) {
				return;
			}
			entries.clear();
			snapshot = List.of();
		}
		notifyListeners(snapshot);
	}

	/**
	 * Returns an immutable snapshot of the activity list (newest first).
	 *
	 * @return snapshot list
	 */
	public synchronized List<GraphActivityLogEntry> snapshot() {
		return List.copyOf(entries);
	}

	/**
	 * Returns current activity entry count.
	 *
	 * @return number of entries in memory
	 */
	public synchronized int size() {
		return entries.size();
	}

	synchronized void setMaxEntriesForTesting(int maxEntries) {
		this.maxEntries = Math.max(1, maxEntries);
		trimToCapacity();
	}

	private void trimToCapacity() {
		while (entries.size() > maxEntries) {
			entries.removeLast();
		}
	}

	private void notifyListeners(List<GraphActivityLogEntry> snapshot) {
		for (GraphActivityLogListener listener : listeners.values()) {
			notifyListenerSafely(listener, snapshot);
		}
	}

	private void notifyListenerSafely(GraphActivityLogListener listener, List<GraphActivityLogEntry> snapshot) {
		try {
			listener.onLogsUpdated(snapshot);
		} catch (Exception e) {
			LOGGER.warn("Graph activity log listener failed.", e);
		}
	}

	private GraphStateSnapshot snapshotGraphState() {
		try {
			Graph graph = GraphStoreService.getInstance().getGraph();
			if (graph == null) {
				return new GraphStateSnapshot(0, 0);
			}
			int totalTripleCount = Math.max(0, graph.size());
			int namedGraphCount = 0;
			if (totalTripleCount > 0) {
				DataWorkspaceStatusSupport.GraphCountSnapshot graphCountSnapshot = DataWorkspaceStatusSupport
						.computeGraphCountSnapshot(graph, totalTripleCount, LOGGER);
				namedGraphCount = graphCountSnapshot.namedGraphCounts().size();
			}
			return new GraphStateSnapshot(totalTripleCount, namedGraphCount);
		} catch (Exception e) {
			LOGGER.debug("Unable to compute graph state snapshot for activity log entry.", e);
			return new GraphStateSnapshot(0, 0);
		}
	}

	private record GraphStateSnapshot(int totalTripleCount, int namedGraphCount) {
	}
}
