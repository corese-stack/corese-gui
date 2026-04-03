package fr.inria.corese.gui.feature.query.template;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

class QueryTemplateOptionsTest {

	@Test
	void defaults_returnsStableSelectDefaults() {
		QueryTemplateOptions options = QueryTemplateOptions.defaults();

		assertEquals(QueryTemplateType.SELECT, options.type());
		assertFalse(options.useGraphClause());
		assertFalse(options.useServiceClause());
		assertEquals("", options.serviceEndpointUrl());
		assertNull(options.limit());
		assertNull(options.offset());
	}

	@Test
	void constructor_disablesUnsupportedServiceClause() {
		QueryTemplateOptions options = new QueryTemplateOptions(QueryTemplateType.INSERT_DATA, true, false, false,
				false, false, true, "https://dbpedia.org/sparql", null, null);

		assertFalse(options.useServiceClause());
		assertEquals("", options.serviceEndpointUrl());
	}

	@Test
	void constructor_normalizesServiceEndpointAndPagination() {
		QueryTemplateOptions options = new QueryTemplateOptions(QueryTemplateType.SELECT, false, false, false, false,
				false, true, "  https://example.org/sparql  ", 0, -5);

		assertTrue(options.useServiceClause());
		assertEquals("https://example.org/sparql", options.serviceEndpointUrl());
		assertEquals(1, options.limit());
		assertEquals(0, options.offset());
	}

	@Test
	void constructor_clearsOffsetWhenLimitIsMissing() {
		QueryTemplateOptions options = new QueryTemplateOptions(QueryTemplateType.SELECT, false, false, false, false,
				false, false, "", null, 25);

		assertNull(options.limit());
		assertNull(options.offset());
	}

	@Test
	void constructor_disablesFlagsUnsupportedByLoadTemplate() {
		QueryTemplateOptions options = new QueryTemplateOptions(QueryTemplateType.LOAD_URI, true, true, true, true,
				true, true, "https://dbpedia.org/sparql", 100, 5);

		assertFalse(options.useGraphClause());
		assertFalse(options.useDistinct());
		assertFalse(options.orderBySubject());
		assertFalse(options.useOptionalPattern());
		assertFalse(options.useUnionPattern());
		assertFalse(options.useServiceClause());
		assertEquals("", options.serviceEndpointUrl());
		assertNull(options.limit());
		assertNull(options.offset());
	}

	@Test
	void constructor_defaultsNullTypeToSelect() {
		QueryTemplateOptions options = new QueryTemplateOptions(null, false, false, false, false, false, false, null,
				null, null);

		assertEquals(QueryTemplateType.SELECT, options.type());
	}
}
