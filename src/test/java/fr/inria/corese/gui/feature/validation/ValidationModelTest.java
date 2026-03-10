package fr.inria.corese.gui.feature.validation;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import fr.inria.corese.gui.core.model.ValidationResult;
import fr.inria.corese.gui.core.service.RdfDataService;
import fr.inria.corese.gui.core.service.ShaclService;

class ValidationModelTest {

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

	@Test
	void discardResult_releasesCachedReportAndClearsRenderState() {
		ValidationModel model = new ValidationModel();
		ValidationResult result = model.validate(VALID_SHAPES);
		String reportId = result.getReportId();

		assertNotNull(reportId, "Validation should produce a cached report.");
		model.markResultRendered(result);
		assertTrue(model.isResultRendered(result), "Rendered result should be tracked before discard.");

		model.discardResult(result);

		assertNull(model.getLastResult(), "Discarded validation result should no longer remain attached to the model.");
		assertFalse(model.isResultRendered(result), "Discarded validation result should no longer be marked rendered.");
		assertEquals(0, shaclService.getReportTripleCount(reportId),
				"Discarding a cancelled validation should release its cached report.");
	}

	private void resetState() {
		rdfDataService.clearData();
	}
}
