package fr.inria.corese.gui.core.service;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicLong;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Central publish/subscribe bus for graph mutation notifications.
 *
 * <p>
 * The bus is app-wide and used to propagate graph changes to UI layers without
 * coupling them to Corese internals.
 */
@SuppressWarnings("java:S6548") // Singleton is intentional for app-wide event routing
public final class GraphMutationBus {

	private static final Logger LOGGER = LoggerFactory.getLogger(GraphMutationBus.class);
	private static final GraphMutationBus INSTANCE = new GraphMutationBus();

	private final Map<Long, GraphMutationListener> listeners = new ConcurrentHashMap<>();
	private final AtomicLong listenerIdGenerator = new AtomicLong();

	private GraphMutationBus() {
		GraphMutationCollectorService.getInstance().initialize(this);
	}

	/**
	 * Returns the singleton event bus.
	 *
	 * @return bus instance
	 */
	public static GraphMutationBus getInstance() {
		return INSTANCE;
	}

	/**
	 * Subscribes to graph mutation events.
	 *
	 * @param listener
	 *            listener callback
	 * @return handle to unsubscribe
	 */
	public AutoCloseable subscribe(GraphMutationListener listener) {
		if (listener == null) {
			throw new IllegalArgumentException("listener must not be null");
		}
		long listenerId = listenerIdGenerator.incrementAndGet();
		listeners.put(listenerId, listener);
		return () -> listeners.remove(listenerId);
	}

	/**
	 * Publishes an event to all subscribers.
	 *
	 * @param event
	 *            event to publish
	 */
	public void publish(GraphMutationEvent event) {
		if (event == null) {
			return;
		}
		for (GraphMutationListener listener : listeners.values()) {
			try {
				listener.onGraphMutation(event);
			} catch (Exception e) {
				LOGGER.warn("Graph mutation listener failed", e);
			}
		}
	}
}
