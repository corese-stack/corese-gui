package fr.inria.corese.gui.utils;

import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.ThreadFactory;
import java.util.concurrent.atomic.AtomicInteger;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Centralized background executor for application tasks.
 *
 * <p>Uses daemon threads to avoid blocking app shutdown and provides consistent thread naming.
 */
public final class AppExecutors {

  private static final Logger LOGGER = LoggerFactory.getLogger(AppExecutors.class);
  private static final AtomicInteger THREAD_ID = new AtomicInteger(1);

  private static final ExecutorService BACKGROUND =
      Executors.newCachedThreadPool(new ThreadFactory() {
        @Override
        public Thread newThread(Runnable r) {
          Thread t = new Thread(r, "Corese-Worker-" + THREAD_ID.getAndIncrement());
          t.setDaemon(true);
          t.setUncaughtExceptionHandler(
              (thread, ex) -> LOGGER.error("Uncaught background error in {}", thread.getName(), ex));
          return t;
        }
      });

  private AppExecutors() {
    // Utility class
  }

  /** Executes a task in the shared background executor. */
  public static void execute(Runnable task) {
    BACKGROUND.execute(task);
  }

  /** Shuts down the background executor. */
  public static void shutdown() {
    BACKGROUND.shutdownNow();
  }
}
