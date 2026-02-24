package fr.inria.corese.gui.utils;

import java.util.concurrent.Callable;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.atomic.AtomicInteger;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Centralized background executor for application tasks.
 *
 * <p>
 * Uses daemon threads to avoid blocking app shutdown and provides consistent
 * thread naming.
 */
public final class AppExecutors {

	private static final Logger LOGGER = LoggerFactory.getLogger(AppExecutors.class);
	private static final AtomicInteger THREAD_ID = new AtomicInteger(1);

	private static final ExecutorService BACKGROUND = Executors.newCachedThreadPool(runnable -> {
		Thread thread = new Thread(runnable, "Corese-Worker-" + THREAD_ID.getAndIncrement());
		thread.setDaemon(true);
		thread.setUncaughtExceptionHandler(
				(failedThread, ex) -> LOGGER.error("Uncaught background error in {}", failedThread.getName(), ex));
		return thread;
	});

	private AppExecutors() {
		// Utility class
	}

	/** Executes a task in the shared background executor. */
	public static void execute(Runnable task) {
		BACKGROUND.execute(task);
	}

	/** Submits a task in the shared background executor and returns a future. */
	public static Future<?> submit(Runnable task) {
		return BACKGROUND.submit(task);
	}

	/**
	 * Submits a callable in the shared background executor and returns a future.
	 */
	public static <T> Future<T> submit(Callable<T> task) {
		return BACKGROUND.submit(task);
	}

	/** Shuts down the background executor. */
	public static void shutdown() {
		BACKGROUND.shutdownNow();
	}
}
