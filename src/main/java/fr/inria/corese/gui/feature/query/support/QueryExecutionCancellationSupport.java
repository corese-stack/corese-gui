package fr.inria.corese.gui.feature.query.support;

import java.util.Objects;
import java.util.concurrent.CancellationException;
import java.util.concurrent.Future;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicReference;

/**
 * Utilities to coordinate cancellable background query execution.
 */
public final class QueryExecutionCancellationSupport {

	private QueryExecutionCancellationSupport() {
		// Utility class
	}

	/**
	 * Requests query cancellation and interrupts the running background task if
	 * already bound.
	 *
	 * @return true when cancellation has been requested for the first time
	 */
	public static boolean requestCancellation(AtomicBoolean cancellationRequested,
			AtomicReference<Future<?>> futureReference) {
		Objects.requireNonNull(cancellationRequested, "cancellationRequested");
		Objects.requireNonNull(futureReference, "futureReference");

		if (!cancellationRequested.compareAndSet(false, true)) {
			return false;
		}

		Future<?> future = futureReference.get();
		if (future != null) {
			future.cancel(true);
		}
		return true;
	}

	/**
	 * Binds a future and interrupts it immediately when cancellation was already
	 * requested.
	 */
	public static void bindFuture(AtomicBoolean cancellationRequested, AtomicReference<Future<?>> futureReference,
			Future<?> future) {
		Objects.requireNonNull(cancellationRequested, "cancellationRequested");
		Objects.requireNonNull(futureReference, "futureReference");
		Objects.requireNonNull(future, "future");

		futureReference.set(future);
		if (cancellationRequested.get()) {
			future.cancel(true);
		}
	}

	/**
	 * Returns true when an execution failure should be treated as a user
	 * cancellation.
	 */
	public static boolean shouldTreatAsCancellation(AtomicBoolean cancellationRequested, Throwable error) {
		Objects.requireNonNull(cancellationRequested, "cancellationRequested");
		Objects.requireNonNull(error, "error");

		return cancellationRequested.get() || error instanceof CancellationException
				|| error instanceof InterruptedException || Thread.currentThread().isInterrupted();
	}
}
