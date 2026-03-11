package fr.inria.corese.gui.core.service;

/**
 * Built-in reasoning profiles supported by the Data page.
 */
public enum ReasoningProfile {
	RDFS("RDFS RL entailments", "urn:corese:inference:rdfs"), OWL_RL("OWL RL entailments",
			"urn:corese:inference:owlrl"), OWL_RL_LITE("OWL RL Lite entailments",
					"urn:corese:inference:owlrl-lite"), OWL_RL_EXT("OWL RL Ext entailments",
							"urn:corese:inference:owlrl-ext");

	private final String label;
	private final String namedGraphUri;

	ReasoningProfile(String label, String namedGraphUri) {
		this.label = label;
		this.namedGraphUri = namedGraphUri;
	}

	/**
	 * Returns display label for the profile.
	 *
	 * @return profile label
	 */
	public String label() {
		return label;
	}

	/**
	 * Returns the named graph URI where profile inferences are stored.
	 *
	 * @return named graph URI
	 */
	public String namedGraphUri() {
		return namedGraphUri;
	}
}
