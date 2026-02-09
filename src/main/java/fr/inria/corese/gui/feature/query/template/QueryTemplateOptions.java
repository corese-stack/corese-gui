package fr.inria.corese.gui.feature.query.template;

/**
 * Immutable options used to generate a SPARQL query template.
 */
public record QueryTemplateOptions(QueryTemplateType type, boolean useGraphClause, boolean useDistinct,
		boolean orderBySubject, QueryTemplatePattern pattern, Integer limit, Integer offset) {

	public QueryTemplateOptions {
		type = type == null ? QueryTemplateType.SELECT : type;
		pattern = pattern == null ? QueryTemplatePattern.BASIC : pattern;

		if (!type.supportsGraphClause()) {
			useGraphClause = false;
		}
		if (!type.supportsDistinct()) {
			useDistinct = false;
		}
		if (!type.supportsOrderBy()) {
			orderBySubject = false;
		}
		if (!type.supportsPatternVariant()) {
			pattern = QueryTemplatePattern.BASIC;
		}
		if (!type.supportsLimit()) {
			limit = null;
		} else if (limit != null && limit < 1) {
			limit = 1;
		}
		if (!type.supportsOffset()) {
			offset = null;
		} else if (offset != null && offset < 0) {
			offset = 0;
		}
	}
}
