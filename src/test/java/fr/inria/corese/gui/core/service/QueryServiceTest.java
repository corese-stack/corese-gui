package fr.inria.corese.gui.core.service;

import fr.inria.corese.core.sparql.triple.parser.ASTQuery;
import fr.inria.corese.gui.core.enums.QueryType;
import fr.inria.corese.gui.core.enums.SerializationFormat;
import fr.inria.corese.gui.core.model.QueryResultRef;
import fr.inria.corese.gui.feature.result.table.support.TsvTableParser;
import java.util.LinkedHashSet;
import java.util.List;
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
	void executeQuery_withoutLocalData_canStillReturnBindings() {
		QueryResultRef resultRef = queryService.executeQuery("""
				SELECT ?value
				WHERE {
					VALUES ?value { 1 }
				}
				""");

		assertEquals(QueryType.SELECT, resultRef.getQueryType());
		assertEquals(1, resultRef.getResultCount(),
				"Queries that do not depend on the local graph should still execute on an empty dataset.");
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
	void rdfsSubsetToggle_controlsManagedDomainInferenceInQueryResults() {
		QueryResultRef insertRef = queryService.executeQuery("""
				PREFIX ex: <http://example.org/>
				PREFIX rdfs: <http://www.w3.org/2000/01/rdf-schema#>
				INSERT DATA {
					ex:hasPet rdfs:domain ex:Person .
					ex:alice ex:hasPet ex:fido .
				}
				""");
		try {
			QueryResultRef beforeRef = queryService.executeQuery("""
					PREFIX ex: <http://example.org/>
					SELECT ?x WHERE { ?x a ex:Person }
					""");
			assertEquals(0, beforeRef.getResultCount(),
					"Without RDFS subset, no inferred ex:Person typing should be returned.");
			queryService.releaseResult(beforeRef.getId());

			reasoningService.setRdfsSubsetEnabled(true);

			QueryResultRef enabledRef = queryService.executeQuery("""
					PREFIX ex: <http://example.org/>
					SELECT ?x WHERE { ?x a ex:Person }
					""");
			assertEquals(1, enabledRef.getResultCount(),
					"RDFS subset should materialize native domain inference for query evaluation.");
			queryService.releaseResult(enabledRef.getId());

			reasoningService.setRdfsSubsetEnabled(false);

			QueryResultRef disabledRef = queryService.executeQuery("""
					PREFIX ex: <http://example.org/>
					SELECT ?x WHERE { ?x a ex:Person }
					""");
			assertEquals(0, disabledRef.getResultCount(),
					"Disabling RDFS subset should remove the inferred query answer.");
			queryService.releaseResult(disabledRef.getId());
		} finally {
			queryService.releaseResult(insertRef.getId());
		}
	}

	@Test
	void rdfsSubsetAndRdfsRl_shareOneDeduplicatedInferenceGraph() {
		QueryResultRef insertRef = queryService.executeQuery("""
				PREFIX ex: <http://example.org/>
				PREFIX rdfs: <http://www.w3.org/2000/01/rdf-schema#>
				INSERT DATA {
					ex:hasPet rdfs:domain ex:Person .
					ex:flora ex:hasPet ex:rex .
				}
				""");
		QueryResultRef rdfsRlOnlyRef = null;
		QueryResultRef combinedRef = null;
		try {
			reasoningService.setEnabled(ReasoningProfile.RDFS, true);

			rdfsRlOnlyRef = queryService.executeQuery("""
					PREFIX ex: <http://example.org/>
					SELECT ?s WHERE { ?s a ex:Person }
					""");
			assertEquals(List.of("<http://example.org/flora>"), firstColumnValues(rdfsRlOnlyRef),
					"RDFS RL alone should materialize one ex:Person answer for ex:flora via rdfs:domain.");

			reasoningService.setRdfsSubsetEnabled(true);

			combinedRef = queryService.executeQuery("""
					PREFIX ex: <http://example.org/>
					SELECT ?s WHERE { ?s a ex:Person }
					""");
			List<String> combinedValues = firstColumnValues(combinedRef);

			assertEquals(List.of("<http://example.org/flora>"), combinedValues,
					"RDFS subset and RDFS RL should now share one deduplicated RDFS inference graph.");
			assertEquals(1, new LinkedHashSet<>(combinedValues).size(),
					"The combined result should still expose one distinct binding.");
			assertEquals(1, combinedRef.getResultCount(),
					"Query result count should no longer expose duplicate bindings.");
		} finally {
			if (combinedRef != null) {
				queryService.releaseResult(combinedRef.getId());
			}
			if (rdfsRlOnlyRef != null) {
				queryService.releaseResult(rdfsRlOnlyRef.getId());
			}
			queryService.releaseResult(insertRef.getId());
		}
	}

	private List<String> firstColumnValues(QueryResultRef resultRef) {
		String tsv = queryService.formatResult(resultRef.getId(), SerializationFormat.TSV);
		return TsvTableParser.parse(tsv).stream().skip(1).map(row -> row.length == 0 ? "" : row[0]).toList();
	}

}
