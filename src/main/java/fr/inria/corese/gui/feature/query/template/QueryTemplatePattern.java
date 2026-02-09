package fr.inria.corese.gui.feature.query.template;

/**
 * Pattern variants available for WHERE clauses in query templates.
 */
public enum QueryTemplatePattern {

	BASIC("Basic"), OPTIONAL("Optional"), UNION("Union");

	private final String label;

	QueryTemplatePattern(String label) {
		this.label = label;
	}

	@Override
	public String toString() {
		return label;
	}
}
