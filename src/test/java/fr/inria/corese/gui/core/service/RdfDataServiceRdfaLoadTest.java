package fr.inria.corese.gui.core.service;

import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assertions.assertThrows;

import java.io.File;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

class RdfDataServiceRdfaLoadTest {

	@TempDir
	Path tempDir;

	private final RdfDataService rdfDataService = RdfDataService.getInstance();
	private final GraphProjectionService projectionService = GraphProjectionService.getInstance();

	@BeforeEach
	void clearGraphBeforeEach() {
		rdfDataService.clearData();
	}

	@AfterEach
	void clearGraphAfterEach() {
		rdfDataService.clearData();
	}

	@Test
	void loadFile_supportsRdfaHtmlExtension() throws IOException {
		File htmlFile = writeTempHtml("profile.html");

		rdfDataService.loadFile(htmlFile);

		assertTrue(rdfDataService.getTripleCount() > 0, "RDFa HTML input should insert triples into the graph.");
		assertTrue(projectionService.snapshotJsonLd().contains("Alice"),
				"JSON-LD snapshot should expose RDFa literal content.");
	}

	@Test
	void loadFile_supportsRdfaHtmAliasExtension() throws IOException {
		File htmFile = writeTempHtml("profile.htm");

		rdfDataService.loadFile(htmFile);

		assertTrue(rdfDataService.getTripleCount() > 0, "RDFa HTM input should insert triples into the graph.");
		assertTrue(projectionService.snapshotJsonLd().contains("Alice"),
				"JSON-LD snapshot should expose RDFa literal content loaded from .htm.");
	}

	@Test
	void loadFile_invalidTurtle_wrapsParserErrorAsRdfLoadException() throws IOException {
		Path filePath = tempDir.resolve("invalid.ttl");
		Files.writeString(filePath, """
				@prefix ex: <http://example.org/ns#> .
				ex:Alice ex:friend ? .
				""", StandardCharsets.UTF_8);

		RdfDataService.RdfLoadException exception = assertThrows(RdfDataService.RdfLoadException.class,
				() -> rdfDataService.loadFile(filePath.toFile()));
		assertTrue(exception.getMessage().contains("Failed to load RDF file"),
				"Invalid Turtle should surface as a user-facing RDF load error.");
	}

	private File writeTempHtml(String fileName) throws IOException {
		Path filePath = tempDir.resolve(fileName);
		Files.writeString(filePath, """
				<!DOCTYPE html>
				<html prefix="schema: http://schema.org/">
				<head><title>RDFa test</title></head>
				<body>
				<div about="http://example.org/alice" typeof="schema:Person">
				  <span property="schema:name">Alice</span>
				</div>
				</body>
				</html>
				""", StandardCharsets.UTF_8);
		return filePath.toFile();
	}
}
