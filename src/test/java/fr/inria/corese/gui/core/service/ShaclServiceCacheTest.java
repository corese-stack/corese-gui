package fr.inria.corese.gui.core.service;

import fr.inria.corese.gui.core.enums.SerializationFormat;
import fr.inria.corese.gui.core.model.ValidationResult;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class ShaclServiceCacheTest {

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
	}

	private void resetState() {
		rdfDataService.clearData();
		shaclService.clearCachedReportsForTesting();
	}

	@Test
	void validate_reportCacheBound_evictsOldestReport() {
		int previousMax = shaclService.setMaxCachedReportsForTesting(2);
		shaclService.clearCachedReportsForTesting();
		try {
			ValidationResult first = shaclService.validate(VALID_SHAPES);
			ValidationResult second = shaclService.validate(VALID_SHAPES);
			ValidationResult third = shaclService.validate(VALID_SHAPES);

			String firstReportId = first.getReportId();
			String secondReportId = second.getReportId();
			String thirdReportId = third.getReportId();

			String firstFormatted = shaclService.formatReport(firstReportId, SerializationFormat.TURTLE);
			String secondFormatted = shaclService.formatReport(secondReportId, SerializationFormat.TURTLE);
			String thirdFormatted = shaclService.formatReport(thirdReportId, SerializationFormat.TURTLE);

			assertTrue(firstFormatted.startsWith("Error: Report not found"),
					"Oldest cached SHACL report should be evicted once cache limit is exceeded.");
			assertFalse(secondFormatted.startsWith("Error: Report not found"),
					"Second SHACL report should still be available in cache.");
			assertFalse(thirdFormatted.startsWith("Error: Report not found"),
					"Most recent SHACL report should still be available in cache.");
		} finally {
			shaclService.clearCachedReportsForTesting();
			shaclService.setMaxCachedReportsForTesting(previousMax);
		}
	}
}
