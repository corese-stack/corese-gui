package fr.inria.corese.gui.feature.query.template;

import java.util.EnumSet;

/**
 * Supported SPARQL template families available in the query template dialog.
 */
public enum QueryTemplateType {

	SELECT("SELECT"), SELECT_COUNT("SELECT (COUNT)"), CONSTRUCT("CONSTRUCT"), DESCRIBE("DESCRIBE"), ASK(
			"ASK"), INSERT_DATA("INSERT DATA"), DELETE_DATA("DELETE DATA"), DELETE_INSERT_WHERE(
					"DELETE/INSERT WHERE"), LOAD_URI("LOAD"), CLEAR_GRAPH("CLEAR GRAPH"), DROP_GRAPH("DROP GRAPH");

	private static final EnumSet<QueryTemplateType> GRAPH_CLAUSE_TYPES = EnumSet.of(SELECT, SELECT_COUNT, CONSTRUCT,
			DESCRIBE, ASK, INSERT_DATA, DELETE_DATA, DELETE_INSERT_WHERE);
	private static final EnumSet<QueryTemplateType> PATTERN_VARIANT_TYPES = EnumSet.of(SELECT, SELECT_COUNT, CONSTRUCT,
			DESCRIBE, ASK);
	private static final EnumSet<QueryTemplateType> DISTINCT_TYPES = EnumSet.of(SELECT);
	private static final EnumSet<QueryTemplateType> ORDER_BY_TYPES = EnumSet.of(SELECT);
	private static final EnumSet<QueryTemplateType> LIMIT_TYPES = EnumSet.of(SELECT, CONSTRUCT, DESCRIBE);
	private static final EnumSet<QueryTemplateType> SERVICE_CLAUSE_TYPES = EnumSet.of(SELECT, SELECT_COUNT, CONSTRUCT,
			DESCRIBE, ASK, DELETE_INSERT_WHERE);

	private final String label;

	QueryTemplateType(String label) {
		this.label = label;
	}

	public boolean supportsGraphClause() {
		return GRAPH_CLAUSE_TYPES.contains(this);
	}

	public boolean supportsPatternVariant() {
		return PATTERN_VARIANT_TYPES.contains(this);
	}

	public boolean supportsDistinct() {
		return DISTINCT_TYPES.contains(this);
	}

	public boolean supportsOrderBy() {
		return ORDER_BY_TYPES.contains(this);
	}

	public boolean supportsLimit() {
		return LIMIT_TYPES.contains(this);
	}

	public boolean supportsOffset() {
		return supportsLimit();
	}

	public boolean supportsServiceClause() {
		return SERVICE_CLAUSE_TYPES.contains(this);
	}

	@Override
	public String toString() {
		return label;
	}
}
