package fr.inria.corese.gui.core.service;

import fr.inria.corese.core.Graph;
import fr.inria.corese.core.logic.Entailment;
import fr.inria.corese.core.query.QueryProcess;
import fr.inria.corese.core.sparql.exceptions.EngineException;
import java.util.LinkedHashSet;
import java.util.Set;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assertions.fail;

class RdfsSubsetParityTest {

	private static final String FIXTURE_UPDATE = """
			PREFIX ex: <http://example.org/>
			PREFIX rdf: <http://www.w3.org/1999/02/22-rdf-syntax-ns#>
			PREFIX rdfs: <http://www.w3.org/2000/01/rdf-schema#>
			INSERT DATA {
			  ex:Dog rdfs:subClassOf ex:Animal .
			  ex:fido rdf:type ex:Dog .
			  ex:hasPet rdfs:domain ex:Person .
			  ex:alice ex:hasPet ex:fido .
			}
			""";

	private static final String SUPERCLASS_TYPE_TRIPLE = """
			<http://example.org/fido> rdf:type <http://example.org/Animal>
			""".strip();

	private static final String DOMAIN_TYPE_TRIPLE = """
			<http://example.org/alice> rdf:type <http://example.org/Person>
			""".strip();

	private final ReasoningService reasoningService = DefaultReasoningService.getInstance();
	private final RdfDataService rdfDataService = RdfDataService.getInstance();

	@BeforeEach
	void setUp() {
		reasoningService.resetAllProfiles();
		rdfDataService.clearData();
	}

	@AfterEach
	void tearDown() {
		reasoningService.resetAllProfiles();
		rdfDataService.clearData();
	}

	@Test
	void managedRdfsSubset_matchesLegacyNativeRdfsEntailment() {
		Set<String> legacyNativeInferences = nativeRdfsSubsetInferences(FIXTURE_UPDATE);
		Set<String> managedSubsetInferences = managedRdfsSubsetInferences(FIXTURE_UPDATE);

		assertTrue(legacyNativeInferences.contains(DOMAIN_TYPE_TRIPLE),
				"Legacy native RDFS entailment should still infer rdf:type from rdfs:domain.");
		assertTrue(managedSubsetInferences.contains(DOMAIN_TYPE_TRIPLE),
				"Managed RDFS subset should preserve native domain-based typing.");
		assertFalse(legacyNativeInferences.contains(SUPERCLASS_TYPE_TRIPLE),
				"Legacy native RDFS entailment should not materialize rdf:type through rdfs:subClassOf.");
		assertFalse(managedSubsetInferences.contains(SUPERCLASS_TYPE_TRIPLE),
				"Managed RDFS subset should no longer materialize rdf:type through rdfs:subClassOf.");
		assertEquals(legacyNativeInferences, managedSubsetInferences,
				"Managed RDFS subset should now match legacy native RDFS entailment on this dataset.");
	}

	private Set<String> nativeRdfsSubsetInferences(String updateQuery) {
		Graph graph = Graph.create();
		insertData(graph, updateQuery);
		try {
			graph.setRDFSEntailment(true);
			graph.process();
		} catch (EngineException e) {
			fail("Failed to run native Corese RDFS entailment: " + e.getMessage());
		}
		return triplesInGraph(graph, Entailment.ENTAIL);
	}

	private Set<String> managedRdfsSubsetInferences(String updateQuery) {
		Graph graph = GraphStoreService.getInstance().getGraph();
		insertData(graph, updateQuery);
		reasoningService.setRdfsSubsetEnabled(true);
		return triplesInGraph(graph, ReasoningProfile.RDFS.namedGraphUri());
	}

	private static Set<String> triplesInGraph(Graph graph, String graphLabel) {
		Set<String> triples = new LinkedHashSet<>();
		for (var edge : graph.getEdges()) {
			if (edge.getGraph() == null || !graphLabel.equals(edge.getGraph().getLabel())) {
				continue;
			}
			String triple = edge.getNode(0).getDatatypeValue().toSparql(false, true) + " "
					+ edge.getEdgeNode().getDatatypeValue().toSparql(false, true) + " "
					+ edge.getNode(1).getDatatypeValue().toSparql(false, true);
			triples.add(triple);
		}
		return triples;
	}

	private static void insertData(Graph graph, String updateQuery) {
		try {
			QueryProcess.create(graph).query(updateQuery);
		} catch (EngineException e) {
			fail("Failed to prepare RDFS subset fixture: " + e.getMessage());
		}
	}
}
