package fr.inria.corese.gui.core.service;

import static org.junit.jupiter.api.Assertions.assertThrows;

import java.util.concurrent.CancellationException;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

class ShaclServiceCancellationTest {

	private static final String VALID_SHAPES = """
			@prefix sh: <http://www.w3.org/ns/shacl#> .
			@prefix ex: <http://example.org/ns#> .
			@prefix xsd: <http://www.w3.org/2001/XMLSchema#> .

			ex:PersonShape
			    a sh:NodeShape ;
			    sh:targetClass ex:Person ;
			    sh:property [
			        sh:path ex:name ;
			        sh:datatype xsd:string ;
			        sh:minCount 1 ;
			    ] .
			""";

	private final RdfDataService rdfDataService = RdfDataService.getInstance();
	private final ShaclService shaclService = ShaclService.getInstance();

	@BeforeEach
	void setUp() {
		resetState();
	}

	@AfterEach
	void tearDown() {
		resetState();
		Thread.interrupted();
	}

	@Test
	void validate_throwsCancellationException_whenThreadIsAlreadyInterrupted() {
		Thread.currentThread().interrupt();

		assertThrows(CancellationException.class, () -> shaclService.validate(VALID_SHAPES),
				"Interrupted SHACL validations should propagate as cancellations.");
	}

	private void resetState() {
		rdfDataService.clearData();
		shaclService.clearCachedReportsForTesting();
	}
}
