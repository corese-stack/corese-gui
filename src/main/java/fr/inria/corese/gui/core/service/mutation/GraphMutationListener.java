package fr.inria.corese.gui.core.service.mutation;

/**
 * Listener notified when the shared RDF graph mutates.
 */
@FunctionalInterface
public interface GraphMutationListener {

	/**
	 * Called for each published mutation event.
	 *
	 * @param event
	 *            mutation event
	 */
	void onGraphMutation(GraphMutationEvent event);
}
