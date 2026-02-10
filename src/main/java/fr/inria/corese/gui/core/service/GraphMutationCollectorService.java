package fr.inria.corese.gui.core.service;

import fr.inria.corese.core.elasticsearch.EdgeChangeListener;
import fr.inria.corese.core.kgram.api.core.Edge;
import fr.inria.corese.gui.utils.AppExecutors;
import java.util.List;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Internal collector that listens to low-level graph edge changes and forwards
 * them as coalesced {@link GraphMutationEvent} instances.
 */
@SuppressWarnings("java:S6548") // Singleton is intentional: one collector per shared graph
final class GraphMutationCollectorService {

	private static final Logger LOGGER = LoggerFactory.getLogger(GraphMutationCollectorService.class);
	private static final GraphMutationCollectorService INSTANCE = new GraphMutationCollectorService();
	private static final long COALESCE_DELAY_MS = 120L;

	private final AtomicBoolean initialized = new AtomicBoolean(false);
	private final AtomicInteger pendingInserted = new AtomicInteger();
	private final AtomicInteger pendingDeleted = new AtomicInteger();
	private final AtomicBoolean flushScheduled = new AtomicBoolean(false);
	private final AtomicInteger suppressionDepth = new AtomicInteger(0);

	private volatile GraphMutationBus mutationBus;

	private final EdgeChangeListener edgeChangeListener = new EdgeChangeListener() {
		@Override
		public void onBulkEdgeChange(List<Edge> delete, List<Edge> insert) {
			int deleteCount = delete != null ? delete.size() : 0;
			int insertCount = insert != null ? insert.size() : 0;
			onEdgeDelta(deleteCount, insertCount);
		}
	};

	private GraphMutationCollectorService() {
	}

	static GraphMutationCollectorService getInstance() {
		return INSTANCE;
	}

	void initialize(GraphMutationBus bus) {
		if (!initialized.compareAndSet(false, true)) {
			return;
		}
		this.mutationBus = bus;
		GraphStoreService.getInstance().getGraph().addEdgeChangeListener(edgeChangeListener);
		LOGGER.debug("Graph mutation collector initialized.");
	}

	/**
	 * Temporarily suppresses low-level graph mutation publishing.
	 *
	 * <p>
	 * Callers must close the returned handle to restore publishing.
	 *
	 * @return scope handle
	 */
	AutoCloseable suspendPublishing() {
		suppressionDepth.incrementAndGet();
		return () -> {
			int updated = suppressionDepth.decrementAndGet();
			if (updated < 0) {
				suppressionDepth.set(0);
			}
		};
	}

	private void onEdgeDelta(int deletedCount, int insertedCount) {
		if (suppressionDepth.get() > 0) {
			return;
		}
		if (deletedCount <= 0 && insertedCount <= 0) {
			return;
		}
		if (deletedCount > 0) {
			pendingDeleted.addAndGet(deletedCount);
		}
		if (insertedCount > 0) {
			pendingInserted.addAndGet(insertedCount);
		}
		scheduleFlush();
	}

	private void scheduleFlush() {
		if (!flushScheduled.compareAndSet(false, true)) {
			return;
		}
		AppExecutors.execute(() -> {
			try {
				Thread.sleep(COALESCE_DELAY_MS);
			} catch (InterruptedException e) {
				Thread.currentThread().interrupt();
				LOGGER.debug("Graph mutation coalescing interrupted");
			}
			flushNow();
		});
	}

	private void flushNow() {
		int inserted = pendingInserted.getAndSet(0);
		int deleted = pendingDeleted.getAndSet(0);
		flushScheduled.set(false);

		if (inserted > 0 || deleted > 0) {
			GraphMutationBus bus = mutationBus;
			if (bus != null) {
				bus.publish(GraphMutationEvent.delta(GraphMutationEvent.Source.GRAPH_LISTENER, inserted, deleted));
			}
		}

		if (pendingInserted.get() > 0 || pendingDeleted.get() > 0) {
			scheduleFlush();
		}
	}
}
