package fr.inria.corese.gui.core.service;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.File;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.regex.Pattern;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

class ReasoningBeatlesSnapshotStabilityTest {

	private static final Pattern MALFORMED_DOUBLE_QUOTED_VALUE_PATTERN = Pattern
			.compile("(^|[:\\[,\\s])\"\"([^\"\\\\]*(?:\\\\.[^\"\\\\]*)*)\"\"(?=\\s*[,}\\]])", Pattern.MULTILINE);

	@TempDir
	Path tempDir;

	private final RdfDataService rdfDataService = RdfDataService.getInstance();
	private final GraphProjectionService projectionService = GraphProjectionService.getInstance();
	private final ReasoningService reasoningService = DefaultReasoningService.getInstance();
	private final DataWorkspaceService workspaceService = DefaultDataWorkspaceService.getInstance();

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
	void enablingOwlRl_keepsDataSnapshotRenderableForBeatlesTriG() throws IOException {
		File dataset = writeTempRdf("beatles.trig", """
				@prefix xsd: <http://www.w3.org/2001/XMLSchema#> .
				@prefix ex: <http://example.com/> .
				@prefix rdf: <http://www.w3.org/1999/02/22-rdf-syntax-ns#> .
				@prefix : <http://example.com/graphs/> .

				:artists {
				    <http://example.com/John_Lennon> rdf:type ex:SoloArtist .
				    ex:Paul_McCartney rdf:type ex:SoloArtist .
				    ex:Ringo_Starr rdf:type ex:SoloArtist .
				    ex:George_Harrison rdf:type ex:SoloArtist .
				    ex:The_Beatles ex:member ex:John_Lennon ;
				        ex:member ex:Paul_McCartney ;
				        ex:member ex:George_Harrison ;
				        ex:member ex:Ringo_Starr ;
				        ex:name "The Beatles" ;
				        rdf:type ex:Band .
				}

				:albums {
				    <http://example.com/Please_Please_Me> ex:artist ex:The_Beatles ;
				        ex:date "1963-03-22"^^xsd:date ;
				        ex:name "Please Please Me" ;
				        ex:track ex:Love_Me_Do ;
				        rdf:type ex:Album .
				    ex:Imagine ex:artist ex:John_Lennon ;
				        ex:date "1971-10-11"^^xsd:date ;
				        ex:name "Imagine" ;
				        rdf:type ex:Album .
				    ex:McCartney ex:artist ex:Paul_McCartney ;
				        ex:date "1970-04-17"^^xsd:date ;
				        ex:name "McCartney" ;
				        rdf:type ex:Album .
				}

				:songs {
				    ex:Love_Me_Do ex:length 125 ;
				        ex:name "Love Me Do" ;
				        ex:writer ex:John_Lennon ;
				        ex:writer ex:Paul_McCartney ;
				        rdf:type ex:Song .
				}
				""");

		rdfDataService.loadFile(dataset);
		String beforeReasoning = projectionService.snapshotJsonLd();
		assertFalse(beforeReasoning.isBlank(), "Initial Data JSON-LD snapshot should not be blank.");
		assertTrue(beforeReasoning.contains("\"@graph\""),
				"Initial Data JSON-LD snapshot should expose named graph containers.");

		reasoningService.setEnabled(ReasoningProfile.OWL_RL, true);

		String afterReasoning = projectionService.snapshotJsonLd();
		assertFalse(afterReasoning.isBlank(), "Data JSON-LD snapshot should remain non-empty after OWL RL.");
		assertTrue(afterReasoning.contains("\"@graph\""),
				"Data JSON-LD snapshot should still expose named graph containers after OWL RL.");
		assertFalse(MALFORMED_DOUBLE_QUOTED_VALUE_PATTERN.matcher(afterReasoning).find(),
				"JSON-LD snapshot should not contain malformed double-quoted values like \"\"@type\"\".");
		assertTrue(workspaceService.getStatus().tripleCount() > 0,
				"Workspace should keep triples after OWL RL enablement.");
	}

	private File writeTempRdf(String fileName, String content) throws IOException {
		Path filePath = tempDir.resolve(fileName);
		Files.writeString(filePath, content, StandardCharsets.UTF_8);
		return filePath.toFile();
	}
}
