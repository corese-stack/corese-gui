package fr.inria.corese.gui.core.service;

import fr.inria.corese.core.Graph;
import fr.inria.corese.gui.core.enums.SerializationFormat;
import java.util.Arrays;
import java.util.List;

/**
 * Provides serialized projections of the shared graph for UI consumers.
 */
@SuppressWarnings("java:S6548") // Singleton is intentional for shared stateless projection logic
public final class GraphProjectionService {

	private static final GraphProjectionService INSTANCE = new GraphProjectionService();
	private static final List<SerializationFormat> RDF_EXPORT_FORMATS = List
			.copyOf(Arrays.asList(SerializationFormat.rdfFormats()));

	private GraphProjectionService() {
	}

	/**
	 * Returns the singleton projection service.
	 *
	 * @return projection service instance
	 */
	public static GraphProjectionService getInstance() {
		return INSTANCE;
	}

	/**
	 * Serializes the current shared graph as JSON-LD.
	 *
	 * @return JSON-LD snapshot string, or empty string when graph is empty
	 */
	public String snapshotJsonLd() {
		return serializeGraph(SerializationFormat.JSON_LD);
	}

	/**
	 * Serializes the current shared graph with the requested RDF format.
	 *
	 * @param format
	 *            RDF format
	 * @return serialized graph string, or empty string when graph is empty
	 */
	public String serializeGraph(SerializationFormat format) {
		if (format == null) {
			throw new IllegalArgumentException("format must not be null");
		}
		Graph graph = GraphStoreService.getInstance().getGraph();
		if (graph.size() == 0) {
			return "";
		}
		Graph serializationGraph = graph;
		if (format == SerializationFormat.JSON_LD) {
			/*
			 * Corese JSON-LD rendering can keep stale internal state when serializing the
			 * same mutable graph instance repeatedly. Serializing a fresh copy guarantees
			 * that incremental loads are reflected in the preview/export payload.
			 */
			serializationGraph = graph.copy();
		}
		String serialized = ResultFormatter.getInstance().formatGraph(serializationGraph, format);
		if (serialized == null) {
			throw new IllegalStateException("Graph serialization returned null.");
		}
		if (serialized.startsWith("Error:")) {
			throw new IllegalStateException(serialized);
		}
		return serialized;
	}

	/**
	 * Returns all supported RDF export formats.
	 *
	 * @return immutable list of RDF serialization formats
	 */
	public List<SerializationFormat> supportedRdfExportFormats() {
		return RDF_EXPORT_FORMATS;
	}
}
