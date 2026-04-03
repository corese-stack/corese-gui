package fr.inria.corese.gui.feature.query.template;

/**
 * Immutable options used to generate a SPARQL query template.
 */
public record QueryTemplateOptions(QueryTemplateType type, boolean useGraphClause, boolean useDistinct,
		boolean orderBySubject, boolean useOptionalPattern, boolean useUnionPattern, boolean useServiceClause,
		String serviceEndpointUrl, Integer limit, Integer offset) {

	public static QueryTemplateOptions defaults() {
		return new QueryTemplateOptions(QueryTemplateType.SELECT, false, false, false, false, false, false, "", null,
				null);
	}

	public QueryTemplateOptions {
		type = type == null ? QueryTemplateType.SELECT : type;
		useGraphClause = type.supportsGraphClause() && useGraphClause;
		useDistinct = type.supportsDistinct() && useDistinct;
		orderBySubject = type.supportsOrderBy() && orderBySubject;
		useOptionalPattern = type.supportsPatternVariant() && useOptionalPattern;
		useUnionPattern = type.supportsPatternVariant() && useUnionPattern;
		useServiceClause = type.supportsServiceClause() && useServiceClause;
		serviceEndpointUrl = useServiceClause ? normalizeEndpoint(serviceEndpointUrl) : "";
		limit = normalizeLimit(type, limit);
		offset = normalizeOffset(type, limit, offset);
	}

	private static String normalizeEndpoint(String endpointUrl) {
		return endpointUrl == null ? "" : endpointUrl.trim();
	}

	private static Integer normalizeLimit(QueryTemplateType type, Integer limit) {
		if (!type.supportsLimit() || limit == null) {
			return null;
		}
		return Math.max(1, limit);
	}

	private static Integer normalizeOffset(QueryTemplateType type, Integer limit, Integer offset) {
		if (!type.supportsOffset() || limit == null || offset == null) {
			return null;
		}
		return Math.max(0, offset);
	}
}
