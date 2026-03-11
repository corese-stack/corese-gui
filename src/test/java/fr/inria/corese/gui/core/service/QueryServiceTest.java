package fr.inria.corese.gui.core.service;

import fr.inria.corese.core.sparql.triple.parser.ASTQuery;
import fr.inria.corese.gui.core.enums.QueryType;
import fr.inria.corese.gui.core.enums.SerializationFormat;
import fr.inria.corese.gui.core.model.QueryResultRef;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class QueryServiceTest {

	private final QueryService queryService = QueryService.getInstance();
	private final RdfDataService rdfDataService = RdfDataService.getInstance();
	private final ReasoningService reasoningService = DefaultReasoningService.getInstance();

	@BeforeEach
	void clearGraphBeforeEach() {
		reasoningService.resetAllProfiles();
		rdfDataService.clearData();
	}

	@AfterEach
	void clearGraphAfterEach() {
		reasoningService.resetAllProfiles();
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

	@Test
	void formatResult_tsvPreservesLiteralMetadataWhileCsvFlattensValue() {
		QueryResultRef updateRef = queryService.executeQuery("""
				PREFIX ex: <http://example.org/>
				INSERT DATA { ex:s ex:p "Bla bla bla"@fr }
				""");
		QueryResultRef selectRef = queryService.executeQuery("""
				PREFIX ex: <http://example.org/>
				SELECT ?o WHERE { ex:s ex:p ?o }
				""");
		try {
			String csv = queryService.formatResult(selectRef.getId(), SerializationFormat.CSV);
			String tsv = queryService.formatResult(selectRef.getId(), SerializationFormat.TSV);

			assertTrue(csv.contains("Bla bla bla"), "CSV should include the literal lexical value.");
			assertFalse(csv.contains("@fr"), "CSV should not preserve RDF term language metadata.");

			assertTrue(tsv.contains("\"Bla bla bla\"@fr"),
					"TSV should preserve RDF term metadata for language-tagged literals.");
		} finally {
			queryService.releaseResult(selectRef.getId());
			queryService.releaseResult(updateRef.getId());
		}
	}

	@Test
	void executeQuery_cacheBound_evictsOldestResult() {
		int previousMaxEntries = queryService.setMaxCachedResultsForTesting(2);
		queryService.clearCachedResultsForTesting();
		try {
			QueryResultRef first = queryService.executeQuery("SELECT * WHERE { ?s ?p ?o }");
			QueryResultRef second = queryService.executeQuery("SELECT * WHERE { ?s ?p ?o }");
			QueryResultRef third = queryService.executeQuery("SELECT * WHERE { ?s ?p ?o }");

			String firstFormatted = queryService.formatResult(first.getId(), SerializationFormat.JSON);
			String secondFormatted = queryService.formatResult(second.getId(), SerializationFormat.JSON);
			String thirdFormatted = queryService.formatResult(third.getId(), SerializationFormat.JSON);

			assertTrue(firstFormatted.startsWith("Error: Result expired or not found"),
					"Oldest cached query result should be evicted once cache limit is exceeded.");
			assertFalse(secondFormatted.startsWith("Error: Result expired or not found"),
					"Second cached query result should still be available.");
			assertFalse(thirdFormatted.startsWith("Error: Result expired or not found"),
					"Most recent cached query result should still be available.");
		} finally {
			queryService.clearCachedResultsForTesting();
			queryService.setMaxCachedResultsForTesting(previousMaxEntries);
		}
	}

	@Test
	void rdfsSubsetToggle_controlsNativeEntailmentInQueryResults() {
		QueryResultRef insertRef = queryService.executeQuery("""
				PREFIX ex: <http://example.org/>
				PREFIX rdfs: <http://www.w3.org/2000/01/rdf-schema#>
				INSERT DATA {
					ex:Dog rdfs:subClassOf ex:Animal .
					ex:fido a ex:Dog .
				}
				""");
		try {
			QueryResultRef beforeRef = queryService.executeQuery("""
					PREFIX ex: <http://example.org/>
					SELECT ?x WHERE { ?x a ex:Animal }
					""");
			assertEquals(0, beforeRef.getResultCount(),
					"Without native RDFS subset, no inferred ex:Animal typing should be returned.");
			queryService.releaseResult(beforeRef.getId());

			reasoningService.setRdfsSubsetEnabled(true);

			QueryResultRef enabledRef = queryService.executeQuery("""
					PREFIX ex: <http://example.org/>
					SELECT ?x WHERE { ?x a ex:Animal }
					""");
			assertEquals(1, enabledRef.getResultCount(),
					"Native RDFS subset should expose subclass typing during query evaluation.");
			queryService.releaseResult(enabledRef.getId());

			reasoningService.setRdfsSubsetEnabled(false);

			QueryResultRef disabledRef = queryService.executeQuery("""
					PREFIX ex: <http://example.org/>
					SELECT ?x WHERE { ?x a ex:Animal }
					""");
			assertEquals(0, disabledRef.getResultCount(),
					"Disabling native RDFS subset should remove the inferred query answer.");
			queryService.releaseResult(disabledRef.getId());
		} finally {
			queryService.releaseResult(insertRef.getId());
		}
	}

}
