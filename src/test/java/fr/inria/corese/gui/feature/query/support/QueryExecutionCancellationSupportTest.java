package fr.inria.corese.gui.feature.query.support;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.concurrent.CompletableFuture;
import java.util.concurrent.Future;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicReference;
import org.junit.jupiter.api.Test;

class QueryExecutionCancellationSupportTest {

	@Test
	void requestCancellation_marksFlagAndCancelsFutureOnce() {
		AtomicBoolean cancellationRequested = new AtomicBoolean(false);
		CompletableFuture<Void> future = new CompletableFuture<>();
		AtomicReference<Future<?>> futureReference = new AtomicReference<>(future);

		assertTrue(QueryExecutionCancellationSupport.requestCancellation(cancellationRequested, futureReference));
		assertTrue(cancellationRequested.get());
		assertTrue(future.isCancelled());

		assertFalse(QueryExecutionCancellationSupport.requestCancellation(cancellationRequested, futureReference));
	}

	@Test
	void bindFuture_cancelsImmediatelyWhenCancellationWasAlreadyRequested() {
		AtomicBoolean cancellationRequested = new AtomicBoolean(true);
		AtomicReference<Future<?>> futureReference = new AtomicReference<>();
		CompletableFuture<Void> future = new CompletableFuture<>();

		QueryExecutionCancellationSupport.bindFuture(cancellationRequested, futureReference, future);

		assertSame(future, futureReference.get());
		assertTrue(future.isCancelled());
	}

	@Test
	void shouldTreatAsCancellation_supportsKnownCancellationCases() {
		AtomicBoolean cancellationRequested = new AtomicBoolean(false);
		assertFalse(QueryExecutionCancellationSupport.shouldTreatAsCancellation(cancellationRequested,
				new IllegalStateException("boom")));

		cancellationRequested.set(true);
		assertTrue(QueryExecutionCancellationSupport.shouldTreatAsCancellation(cancellationRequested,
				new IllegalStateException("any")));

		cancellationRequested.set(false);
		assertTrue(QueryExecutionCancellationSupport.shouldTreatAsCancellation(cancellationRequested,
				new InterruptedException("interrupted")));
	}
}
