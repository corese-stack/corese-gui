package fr.inria.corese.gui.feature.query.template;

/**
 * Supported SPARQL template families available in the query template dialog.
 */
public enum QueryTemplateType {

	SELECT("SELECT"), SELECT_COUNT("SELECT COUNT"), CONSTRUCT("CONSTRUCT"), DESCRIBE("DESCRIBE"), ASK(
			"ASK"), INSERT_DATA("INSERT DATA"), DELETE_DATA("DELETE DATA"), DELETE_INSERT_WHERE(
					"DELETE/INSERT WHERE"), LOAD_URI("LOAD URI"), CLEAR_GRAPH("CLEAR GRAPH"), DROP_GRAPH("DROP GRAPH");

	private final String label;

	QueryTemplateType(String label) {
		this.label = label;
	}

	public boolean supportsGraphClause() {
		return switch (this) {
			case SELECT, SELECT_COUNT, CONSTRUCT, DESCRIBE, ASK, INSERT_DATA, DELETE_DATA, DELETE_INSERT_WHERE -> true;
			case LOAD_URI, CLEAR_GRAPH, DROP_GRAPH -> false;
		};
	}

	public boolean supportsPatternVariant() {
		return this == SELECT || this == SELECT_COUNT || this == CONSTRUCT || this == DESCRIBE || this == ASK;
	}

	public boolean supportsDistinct() {
		return this == SELECT;
	}

	public boolean supportsOrderBy() {
		return this == SELECT;
	}

	public boolean supportsLimit() {
		return this == SELECT || this == CONSTRUCT || this == DESCRIBE;
	}

	public boolean supportsOffset() {
		return supportsLimit();
	}

	@Override
	public String toString() {
		return label;
	}
}
