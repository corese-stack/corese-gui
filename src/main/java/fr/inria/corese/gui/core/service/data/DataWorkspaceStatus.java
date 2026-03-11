package fr.inria.corese.gui.core.service.data;

import java.util.List;
import java.util.Objects;

/**
 * Immutable snapshot of Data workspace metrics used by the Data page status
 * bar.
 */
public record DataWorkspaceStatus(int tripleCount, int explicitTripleCount, int inferredTripleCount,
		int defaultGraphTripleCount, int sourceCount, int fileSourceCount, int uriSourceCount, int namedGraphCount,
		List<NamedGraphStat> namedGraphStats, List<ReasoningStat> reasoningStats, boolean nativeRdfsSubsetEnabled) {

	/**
	 * Per named graph triple metric.
	 *
	 * @param graphName
	 *            named graph URI
	 * @param tripleCount
	 *            triples stored in this graph
	 */
	public record NamedGraphStat(String graphName, int tripleCount) {
		public NamedGraphStat {
			if (graphName == null || graphName.isBlank()) {
				throw new IllegalArgumentException("graphName must not be blank");
			}
			tripleCount = Math.max(0, tripleCount);
		}
	}

	/**
	 * Per reasoning profile inferred triple metric.
	 *
	 * @param profileLabel
	 *            display name of the profile
	 * @param graphName
	 *            named graph used by the profile
	 * @param tripleCount
	 *            inferred triples in that profile graph
	 */
	public record ReasoningStat(String profileLabel, String graphName, int tripleCount) {
		public ReasoningStat {
			if (profileLabel == null || profileLabel.isBlank()) {
				throw new IllegalArgumentException("profileLabel must not be blank");
			}
			if (graphName == null || graphName.isBlank()) {
				throw new IllegalArgumentException("graphName must not be blank");
			}
			tripleCount = Math.max(0, tripleCount);
		}
	}

	public DataWorkspaceStatus {
		tripleCount = Math.max(0, tripleCount);
		explicitTripleCount = Math.max(0, explicitTripleCount);
		inferredTripleCount = Math.max(0, inferredTripleCount);
		defaultGraphTripleCount = Math.max(0, defaultGraphTripleCount);
		sourceCount = Math.max(0, sourceCount);
		fileSourceCount = Math.max(0, fileSourceCount);
		uriSourceCount = Math.max(0, uriSourceCount);
		namedGraphCount = Math.max(0, namedGraphCount);
		namedGraphStats = List.copyOf(Objects.requireNonNullElse(namedGraphStats, List.of()));
		reasoningStats = List.copyOf(Objects.requireNonNullElse(reasoningStats, List.of()));
	}

	/**
	 * Empty status snapshot.
	 *
	 * @return zeroed status
	 */
	public static DataWorkspaceStatus empty() {
		return new DataWorkspaceStatus(0, 0, 0, 0, 0, 0, 0, 0, List.of(), List.of(), false);
	}
}
