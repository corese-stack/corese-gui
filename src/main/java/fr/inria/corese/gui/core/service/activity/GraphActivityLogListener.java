package fr.inria.corese.gui.core.service.activity;

import java.util.List;

/**
 * Listener notified whenever graph activity logs are updated.
 */
@FunctionalInterface
public interface GraphActivityLogListener {

	/**
	 * Called when the activity log content changes.
	 *
	 * @param entries
	 *            immutable snapshot (most recent first)
	 */
	void onLogsUpdated(List<GraphActivityLogEntry> entries);
}
