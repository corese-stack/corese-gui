package fr.inria.corese.gui.feature.query.template;

/**
 * Generates ready-to-run SPARQL template text from UI options.
 */
public final class QueryTemplateGenerator {

	private static final String DEFAULT_GRAPH_URI = "http://example.org/graph";
	private static final String LOAD_URI_PLACEHOLDER = "https://ns.inria.fr/humans/humans_data.ttl";
	private static final String EXAMPLE_SUBJECT = "http://example.org/resource/s";
	private static final String EXAMPLE_PREDICATE = "http://example.org/property/p";
	private static final String EXAMPLE_LITERAL = "\"value\"";

	private QueryTemplateGenerator() {
		throw new AssertionError("Utility class");
	}

	public static String generate(QueryTemplateOptions options) {
		QueryTemplateOptions safeOptions = options == null
				? new QueryTemplateOptions(QueryTemplateType.SELECT, false, false, false, false, false, null, null)
				: options;

		return switch (safeOptions.type()) {
			case SELECT -> buildSelect(safeOptions);
			case SELECT_COUNT -> buildSelectCount(safeOptions);
			case CONSTRUCT -> buildConstruct(safeOptions);
			case DESCRIBE -> buildDescribe(safeOptions);
			case ASK -> buildAsk(safeOptions);
			case INSERT_DATA -> buildInsertData(safeOptions);
			case DELETE_DATA -> buildDeleteData(safeOptions);
			case DELETE_INSERT_WHERE -> buildDeleteInsertWhere(safeOptions);
			case LOAD_URI -> buildLoadUri();
			case CLEAR_GRAPH -> buildClearGraph();
			case DROP_GRAPH -> buildDropGraph();
		};
	}

	private static String buildSelect(QueryTemplateOptions options) {
		String distinct = options.useDistinct() ? "DISTINCT " : "";
		String projection = options.useGraphClause() ? "?g ?s ?p ?o" : "?s ?p ?o";
		String query = """
				SELECT %s%s
				WHERE {
				%s
				}
				""".formatted(distinct, projection, buildWherePattern(options, 1));
		if (options.orderBySubject()) {
			query += "ORDER BY ?s\n";
		}
		return appendPagination(query, options);
	}

	private static String buildSelectCount(QueryTemplateOptions options) {
		return """
				SELECT (COUNT(*) AS ?count)
				WHERE {
				%s
				}
				""".formatted(buildWherePattern(options, 1));
	}

	private static String buildConstruct(QueryTemplateOptions options) {
		String constructPattern = options.useGraphClause() ? "  GRAPH ?g { ?s ?p ?o . }\n" : "  ?s ?p ?o .\n";
		String query = """
				CONSTRUCT {
				%s}
				WHERE {
				%s
				}
				""".formatted(constructPattern, buildWherePattern(options, 1));
		return appendPagination(query, options);
	}

	private static String buildDescribe(QueryTemplateOptions options) {
		String query = """
				DESCRIBE ?s
				WHERE {
				%s
				}
				""".formatted(buildWherePattern(options, 1));
		return appendPagination(query, options);
	}

	private static String buildAsk(QueryTemplateOptions options) {
		return """
				ASK {
				%s
				}
				""".formatted(buildWherePattern(options, 1));
	}

	private static String buildInsertData(QueryTemplateOptions options) {
		if (options.useGraphClause()) {
			return """
					INSERT DATA {
					  GRAPH <%s> {
					    <%s> <%s> %s .
					  }
					}
					""".formatted(DEFAULT_GRAPH_URI, EXAMPLE_SUBJECT, EXAMPLE_PREDICATE, EXAMPLE_LITERAL);
		}
		return """
				INSERT DATA {
				  <%s> <%s> %s .
				}
				""".formatted(EXAMPLE_SUBJECT, EXAMPLE_PREDICATE, EXAMPLE_LITERAL);
	}

	private static String buildDeleteData(QueryTemplateOptions options) {
		if (options.useGraphClause()) {
			return """
					DELETE DATA {
					  GRAPH <%s> {
					    <%s> <%s> %s .
					  }
					}
					""".formatted(DEFAULT_GRAPH_URI, EXAMPLE_SUBJECT, EXAMPLE_PREDICATE, EXAMPLE_LITERAL);
		}
		return """
				DELETE DATA {
				  <%s> <%s> %s .
				}
				""".formatted(EXAMPLE_SUBJECT, EXAMPLE_PREDICATE, EXAMPLE_LITERAL);
	}

	private static String buildDeleteInsertWhere(QueryTemplateOptions options) {
		if (options.useGraphClause()) {
			return """
					DELETE {
					  GRAPH ?g { ?s ?p ?o . }
					}
					INSERT {
					  GRAPH ?g { ?s ?p "updated" . }
					}
					WHERE {
					  GRAPH ?g { ?s ?p ?o . }
					}
					""";
		}
		return """
				DELETE {
				  ?s ?p ?o .
				}
				INSERT {
				  ?s ?p "updated" .
				}
				WHERE {
				  ?s ?p ?o .
				}
				""";
	}

	private static String buildLoadUri() {
		return "LOAD <" + LOAD_URI_PLACEHOLDER + ">\n";
	}

	private static String buildClearGraph() {
		return "CLEAR GRAPH <" + DEFAULT_GRAPH_URI + ">\n";
	}

	private static String buildDropGraph() {
		return "DROP GRAPH <" + DEFAULT_GRAPH_URI + ">\n";
	}

	private static String appendPagination(String query, QueryTemplateOptions options) {
		StringBuilder builder = new StringBuilder(query);
		if (options.limit() != null) {
			builder.append("LIMIT ").append(options.limit()).append('\n');
		}
		if (options.offset() != null) {
			builder.append("OFFSET ").append(options.offset()).append('\n');
		}
		return builder.toString();
	}

	private static String buildWherePattern(QueryTemplateOptions options, int indentLevel) {
		String indent = "  ".repeat(Math.max(0, indentLevel));
		StringBuilder pattern = new StringBuilder(
				options.useUnionPattern() ? buildUnionPattern(options, indent) : buildBasicPattern(options, indent));
		if (options.useOptionalPattern()) {
			pattern.append('\n').append(buildOptionalPattern(options, indent));
		}
		return pattern.toString();
	}

	private static String buildBasicPattern(QueryTemplateOptions options, String indent) {
		if (options.useGraphClause()) {
			return indent + "GRAPH ?g { ?s ?p ?o . }";
		}
		return indent + "?s ?p ?o .";
	}

	private static String buildOptionalPattern(QueryTemplateOptions options, String indent) {
		if (options.useGraphClause()) {
			return indent + "OPTIONAL { GRAPH ?g { ?s ?optionalPredicate ?optionalObject . } }";
		}
		return indent + "OPTIONAL { ?s ?optionalPredicate ?optionalObject . }";
	}

	private static String buildUnionPattern(QueryTemplateOptions options, String indent) {
		if (options.useGraphClause()) {
			return """
					%s{ GRAPH ?g { ?s ?p ?o . } }
					%sUNION
					%s{ GRAPH ?g { ?s ?altPredicate ?altObject . } }
					""".formatted(indent, indent, indent).stripTrailing();
		}
		return """
				%s{ ?s ?p ?o . }
				%sUNION
				%s{ ?s ?altPredicate ?altObject . }
				""".formatted(indent, indent, indent).stripTrailing();
	}
}
