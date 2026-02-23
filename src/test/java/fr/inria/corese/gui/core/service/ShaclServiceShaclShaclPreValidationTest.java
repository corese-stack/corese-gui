package fr.inria.corese.gui.core.service;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import fr.inria.corese.gui.core.model.ValidationResult;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

class ShaclServiceShaclShaclPreValidationTest {

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

	private static final String INVALID_SHAPES = """
			@prefix sh: <http://www.w3.org/ns/shacl#> .
			@prefix ex: <http://example.org/ns#> .

			ex:BrokenPropertyShape
			    a sh:PropertyShape ;
			    sh:minCount 1 .
			""";

	private final RdfDataService rdfDataService = RdfDataService.getInstance();
	private final ShaclService shaclService = ShaclService.getInstance();

	@BeforeEach
	void clearDataBeforeEach() {
		rdfDataService.clearData();
	}

	@AfterEach
	void clearDataAfterEach() {
		rdfDataService.clearData();
	}

	@Test
	void validate_rejectsShapesThatFailShaclShaclProfile() {
		ValidationResult result = shaclService.validate(INVALID_SHAPES);

		assertNotNull(result.getErrorMessage(), "Invalid SHACL shapes should return a pre-validation error.");
		assertTrue(result.getErrorMessage().contains("SHACL-SHACL"),
				"Error should explicitly mention SHACL-SHACL pre-validation.");
		assertFalse(result.getErrorMessage().contains("Top issue(s):"),
				"Error message should stay concise and not duplicate report details.");
		assertNotNull(result.getErrorDetails(),
				"Invalid SHACL shapes should provide SHACL-SHACL report details for debugging.");
		assertTrue(result.getErrorDetails().contains("SHACL-SHACL report (Turtle)"),
				"Error details should expose the SHACL-SHACL report in Turtle.");
		assertNull(result.getReportId(), "No data-validation report should be produced when pre-validation fails.");
	}

	@Test
	void validate_acceptsShapesThatConformToShaclShaclProfile() {
		ValidationResult result = shaclService.validate(VALID_SHAPES);
		String reportId = result.getReportId();
		if (reportId != null) {
			shaclService.releaseReport(reportId);
		}

		assertNull(result.getErrorMessage(), "Valid SHACL shapes should pass pre-validation.");
		assertNull(result.getErrorDetails(), "No technical error details should be provided on success.");
		assertNotNull(reportId, "Data validation should still produce a report.");
	}
}
