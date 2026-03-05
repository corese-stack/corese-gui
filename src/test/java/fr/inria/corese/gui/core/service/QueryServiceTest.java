package fr.inria.corese.gui.core.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import fr.inria.corese.core.sparql.triple.parser.ASTQuery;
import fr.inria.corese.gui.core.enums.QueryType;
import fr.inria.corese.gui.core.model.QueryResultRef;

class QueryServiceTest {

	private final QueryService queryService = QueryService.getInstance();
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
	void executeQuery_withUndefinedPrefix_throwsQueryExecutionExceptionEvenWhenCoreseStrictModeIsDisabled() {
		boolean previousStrictMode = ASTQuery.STRICT_MODE;
		ASTQuery.STRICT_MODE = false;
		String query = """
				PREFIX : <http://ns.inria.fr/humans/schema#>
				SELECT ?p WHERE { ?p humans:hasChild [] }
				""";
		try {
			QueryService.QueryExecutionException exception = assertThrows(QueryService.QueryExecutionException.class,
					() -> queryService.executeQuery(query));

			assertTrue(exception.getMessage().contains("Undefined prefix"),
					"An undefined SPARQL prefix should fail query execution.");
		} finally {
			ASTQuery.STRICT_MODE = previousStrictMode;
		}
	}

	@Test
	void executeQuery_withDefinedPrefix_returnsSelectResultRef() {
		String query = """
				PREFIX humans: <http://ns.inria.fr/humans/schema#>
				SELECT ?p WHERE { ?p humans:hasChild [] }
				""";

		QueryResultRef resultRef = queryService.executeQuery(query);
		assertEquals(QueryType.SELECT, resultRef.getQueryType());
		assertEquals(0, resultRef.getResultCount(),
				"With an empty graph, a valid SELECT query should return zero bindings.");
		queryService.releaseResult(resultRef.getId());
	}

}
