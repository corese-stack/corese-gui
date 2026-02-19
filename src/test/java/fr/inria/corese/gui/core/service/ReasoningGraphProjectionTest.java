package fr.inria.corese.gui.core.service;

import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

class ReasoningGraphProjectionTest {

    private final RdfDataService rdfDataService = RdfDataService.getInstance();
    private final GraphProjectionService projectionService = GraphProjectionService.getInstance();
    private final ReasoningService reasoningService = DefaultReasoningService.getInstance();
    private final DataWorkspaceService workspaceService = DefaultDataWorkspaceService.getInstance();

    @TempDir
    Path tempDir;

    @BeforeEach
    void setUp() {
        reasoningService.resetAllProfiles();
        rdfDataService.clearData();
    }

    @Test
    void owlRlInference_isSerializedAsNamedGraphInJsonLdSnapshot() throws IOException {
        Path dataset = tempDir.resolve("owlrl-symmetric.ttl");
        Files.writeString(dataset,
                "@prefix ex: <http://example.org/> .\n"
                        + "@prefix owl: <http://www.w3.org/2002/07/owl#> .\n"
                        + "ex:knows a owl:SymmetricProperty .\n"
                        + "ex:alice ex:knows ex:bob .\n");

        rdfDataService.loadFile(dataset.toFile());
        reasoningService.setEnabled(ReasoningProfile.OWL_RL, true);

        DataWorkspaceStatus status = workspaceService.getStatus();
        assertTrue(status.inferredTripleCount() > 0,
                "Expected inferred triples after enabling OWL RL for the symmetric-property dataset.");

        String jsonLd = projectionService.snapshotJsonLd();
        assertTrue(jsonLd.contains("urn:corese:inference:owlrl"),
                "JSON-LD snapshot should expose OWL RL named graph URI for inferred triples.");
        assertTrue(jsonLd.contains("\"@graph\""),
                "JSON-LD snapshot should keep named graph containers after reasoning.");
    }
}
