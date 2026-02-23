package fr.inria.corese.gui.core.service;

import java.io.File;
import java.util.ArrayList;
import java.util.List;

/**
 * Tracks loaded data sources (local files and remote URIs) for reload
 * operations.
 */
@SuppressWarnings("java:S6548") // Singleton is intentional for app-wide source tracking
public final class DataSourceRegistryService {

	private static final DataSourceRegistryService INSTANCE = new DataSourceRegistryService();

	private final List<DataSource> sources = new ArrayList<>();

	private DataSourceRegistryService() {
	}

	/**
	 * Returns the singleton registry.
	 *
	 * @return registry instance
	 */
	public static DataSourceRegistryService getInstance() {
		return INSTANCE;
	}

	/**
	 * Registers a loaded local file.
	 *
	 * @param file
	 *            loaded file
	 */
	public synchronized void registerFile(File file) {
		if (file == null) {
			throw new IllegalArgumentException("file must not be null");
		}
		registerInternal(new DataSource(SourceType.FILE, file.getAbsolutePath()));
	}

	/**
	 * Registers a loaded URI.
	 *
	 * @param uri
	 *            loaded URI
	 */
	public synchronized void registerUri(String uri) {
		registerInternal(new DataSource(SourceType.URI, uri.trim()));
	}

	/**
	 * Returns an immutable snapshot of current sources.
	 *
	 * @return source list snapshot
	 */
	public synchronized List<DataSource> snapshot() {
		return List.copyOf(sources);
	}

	/**
	 * Replaces current sources with the provided list.
	 *
	 * @param newSources
	 *            sources to keep in registry
	 */
	public synchronized void replaceAll(List<DataSource> newSources) {
		sources.clear();
		if (newSources == null || newSources.isEmpty()) {
			return;
		}
		for (DataSource source : newSources) {
			if (source != null) {
				registerInternal(source);
			}
		}
	}

	/**
	 * Returns the number of tracked sources.
	 *
	 * @return source count
	 */
	public synchronized int size() {
		return sources.size();
	}

	/**
	 * Returns whether no source is currently tracked.
	 *
	 * @return true when registry is empty
	 */
	public synchronized boolean isEmpty() {
		return sources.isEmpty();
	}

	/**
	 * Clears the registry.
	 */
	public synchronized void clear() {
		sources.clear();
	}

	private void registerInternal(DataSource source) {
		if (!sources.contains(source)) {
			sources.add(source);
		}
	}
}
