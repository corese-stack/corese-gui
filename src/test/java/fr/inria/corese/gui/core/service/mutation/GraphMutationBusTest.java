package fr.inria.corese.gui.core.service.mutation;

import fr.inria.corese.gui.core.service.RdfDataService;
import java.io.File;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Duration;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.TimeUnit;
import java.util.function.Predicate;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;

class GraphMutationBusTest {

	private static final Duration EVENT_TIMEOUT = Duration.ofSeconds(3);

	@TempDir
	Path tempDir;

	private final GraphMutationBus mutationBus = GraphMutationBus.getInstance();
	private final RdfDataService rdfDataService = RdfDataService.getInstance();

	@BeforeEach
	void clearGraphBeforeEach() {
		rdfDataService.clearData();
	}

	@AfterEach
	void clearGraphAfterEach() {
		rdfDataService.clearData();
	}

	@Test
	void subscribe_withNullListener_throwsIllegalArgumentException() {
		assertThrows(IllegalArgumentException.class, () -> mutationBus.subscribe(null));
	}

	@Test
	void collectorPublishesDeltaEvent_afterGraphMutation() throws Exception {
		BlockingQueue<GraphMutationEvent> events = new LinkedBlockingQueue<>();
		AutoCloseable subscription = mutationBus.subscribe(events::add);
		try {
			File file = writeTempTurtle("delta.ttl", """
					@prefix ex: <http://example.org/> .
					ex:a ex:p ex:b .
					ex:c ex:p ex:d .
					""");

			rdfDataService.loadFile(file);

			GraphMutationEvent event = awaitEvent(events,
					candidate -> candidate.source() == GraphMutationEvent.Source.GRAPH_LISTENER
							&& candidate.insertedCount() > 0,
					EVENT_TIMEOUT);
			assertNotNull(event, "Collector should publish at least one inserted delta event.");
		} finally {
			subscription.close();
		}
	}

	@Test
	void suspendPublishing_blocksCollectorEventsWithinScope() throws Exception {
		BlockingQueue<GraphMutationEvent> events = new LinkedBlockingQueue<>();
		AutoCloseable subscription = mutationBus.subscribe(events::add);
		try {
			File firstFile = writeTempTurtle("suppressed.ttl", """
					@prefix ex: <http://example.org/> .
					ex:suppressed ex:p ex:o .
					""");
			File secondFile = writeTempTurtle("published.ttl", """
					@prefix ex: <http://example.org/> .
					ex:published ex:p ex:o .
					""");

			try (var _ = mutationBus.suspendPublishing()) {
				rdfDataService.loadFile(firstFile);
			}

			GraphMutationEvent suppressedEvent = awaitEvent(events,
					candidate -> candidate.source() == GraphMutationEvent.Source.GRAPH_LISTENER
							&& candidate.insertedCount() > 0,
					Duration.ofMillis(600));
			assertNull(suppressedEvent, "Collector events should be suppressed inside suspension scope.");

			events.clear();
			rdfDataService.loadFile(secondFile);
			GraphMutationEvent resumedEvent = awaitEvent(events,
					candidate -> candidate.source() == GraphMutationEvent.Source.GRAPH_LISTENER
							&& candidate.insertedCount() > 0,
					EVENT_TIMEOUT);
			assertNotNull(resumedEvent, "Collector events should resume after suspension scope is closed.");
		} finally {
			subscription.close();
		}
	}

	private File writeTempTurtle(String fileName, String content) throws IOException {
		Path filePath = tempDir.resolve(fileName);
		Files.writeString(filePath, content, StandardCharsets.UTF_8);
		return filePath.toFile();
	}

	private static GraphMutationEvent awaitEvent(BlockingQueue<GraphMutationEvent> events,
			Predicate<GraphMutationEvent> predicate, Duration timeout) throws InterruptedException {
		long deadline = System.nanoTime() + timeout.toNanos();
		while (true) {
			long remainingNanos = deadline - System.nanoTime();
			if (remainingNanos <= 0L) {
				return null;
			}
			GraphMutationEvent event = events.poll(remainingNanos, TimeUnit.NANOSECONDS);
			if (event == null) {
				return null;
			}
			if (predicate.test(event)) {
				return event;
			}
		}
	}
}
