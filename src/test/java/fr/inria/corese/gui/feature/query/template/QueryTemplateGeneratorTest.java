package fr.inria.corese.gui.feature.query.template;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class QueryTemplateGeneratorTest {

	@Test
	void generate_selectTemplate_canWrapWherePatternInServiceClause() {
		QueryTemplateOptions options = new QueryTemplateOptions(QueryTemplateType.SELECT, true, true, true, true,
				false, true, "https://dbpedia.org/sparql", 100, 0);

		String query = QueryTemplateGenerator.generate(options);

		assertTrue(query.contains("SERVICE <https://dbpedia.org/sparql> {"));
		assertTrue(query.contains("GRAPH ?g { ?s ?p ?o . }"));
		assertTrue(query.contains("OPTIONAL { GRAPH ?g { ?s ?optionalPredicate ?optionalObject . } }"));
		assertTrue(query.contains("ORDER BY ?s"));
		assertTrue(query.contains("LIMIT 100"));
		assertTrue(query.contains("OFFSET 0"));
	}

	@Test
	void generate_deleteInsertWhereTemplate_placesServiceInsideWhereClause() {
		QueryTemplateOptions options = new QueryTemplateOptions(QueryTemplateType.DELETE_INSERT_WHERE, false, false,
				false, false, false, true,
				"https://example.org/sparql?default-graph-uri=https%3A%2F%2Fexample.org%2Fgraph", null, null);

		String query = QueryTemplateGenerator.generate(options);

		assertTrue(query.contains("DELETE {\n  ?s ?p ?o .\n}"));
		assertTrue(query.contains(
				"WHERE {\n  SERVICE <https://example.org/sparql?default-graph-uri=https%3A%2F%2Fexample.org%2Fgraph> {"));
		assertTrue(query.contains("    ?s ?p ?o ."));
	}

	@Test
	void generate_insertDataTemplate_ignoresUnsupportedServiceConfiguration() {
		QueryTemplateOptions options = new QueryTemplateOptions(QueryTemplateType.INSERT_DATA, true, false, false,
				false, false, true, "https://dbpedia.org/sparql", null, null);

		String query = QueryTemplateGenerator.generate(options);

		assertFalse(query.contains("SERVICE"));
		assertTrue(query.contains("INSERT DATA"));
		assertTrue(query.contains("GRAPH <http://example.org/graph> {"));
	}

	@Test
	void generate_serviceTemplate_withBlankEndpoint_usesDefaultEndpointPlaceholder() {
		QueryTemplateOptions options = new QueryTemplateOptions(QueryTemplateType.ASK, false, false, false, false,
				false, true, "   ", null, null);

		String query = QueryTemplateGenerator.generate(options);

		assertTrue(query.contains("SERVICE <https://dbpedia.org/sparql> {"));
	}

	@Test
	void generate_nullOptions_returnsDefaultSelectTemplate() {
		String query = QueryTemplateGenerator.generate(null);

		assertTrue(query.startsWith("SELECT ?s ?p ?o"));
		assertTrue(query.contains("WHERE {\n  ?s ?p ?o .\n}"));
		assertFalse(query.contains("SERVICE"));
		assertFalse(query.contains("LIMIT"));
	}

	@Test
	void generate_deleteInsertWhereWithGraphClause_wrapsGraphPatternInService() {
		QueryTemplateOptions options = new QueryTemplateOptions(QueryTemplateType.DELETE_INSERT_WHERE, true, false,
				false, false, false, true, "https://example.org/sparql", null, null);

		String query = QueryTemplateGenerator.generate(options);

		assertTrue(query.contains("DELETE {\n  GRAPH ?g { ?s ?p ?o . }\n}"));
		assertTrue(query.contains("INSERT {\n  GRAPH ?g { ?s ?p \"updated\" . }\n}"));
		assertTrue(query.contains("WHERE {\n  SERVICE <https://example.org/sparql> {"));
		assertTrue(query.contains("    GRAPH ?g { ?s ?p ?o . }"));
	}

	@Test
	void generate_selectCountTemplate_omitsUnsupportedClauses() {
		QueryTemplateOptions options = new QueryTemplateOptions(QueryTemplateType.SELECT_COUNT, true, true, true, true,
				false, true, "https://example.org/sparql", 50, 10);

		String query = QueryTemplateGenerator.generate(options);

		assertTrue(query.startsWith("SELECT (COUNT(*) AS ?count)"));
		assertTrue(query.contains("SERVICE <https://example.org/sparql> {"));
		assertFalse(query.contains("DISTINCT"));
		assertFalse(query.contains("ORDER BY"));
		assertFalse(query.contains("LIMIT 50"));
		assertFalse(query.contains("OFFSET 10"));
	}
}
