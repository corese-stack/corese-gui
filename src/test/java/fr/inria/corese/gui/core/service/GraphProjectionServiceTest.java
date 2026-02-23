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

class GraphProjectionServiceTest {

	private static final Pattern BROKEN_QUOTE_IN_VALUE_PATTERN = Pattern.compile(
			"\\\"@value\\\"\\s*:\\s*\\\"(?:[^\\\"\\\\]|\\\\.)*\\\"(?=\\s*[A-Za-z0-9_<])",
			Pattern.MULTILINE);
	private static final Pattern BROKEN_CONTROL_CHAR_IN_VALUE_PATTERN = Pattern.compile(
			"\\\"@value\\\"\\s*:\\s*\\\"(?:[^\\\"\\\\]|\\\\.)*[\\n\\r\\t](?:[^\\\"\\\\]|\\\\.)*\\\"",
			Pattern.MULTILINE);

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
	void snapshotJsonLd_preservesRdfTypeStatements() throws IOException {
		File file = writeTempRdf("rdf-type.ttl", """
				@prefix ex: <http://example.org/> .
				ex:alice a ex:Person .
				""");

		rdfDataService.loadFile(file);
		String jsonLd = projectionService.snapshotJsonLd();

		assertTrue(jsonLd.contains("Person"),
				"JSON-LD projection should contain the rdf:type object value label.");
		assertTrue(jsonLd.contains("\"@type\"") || jsonLd.contains("22-rdf-syntax-ns#type"),
				"JSON-LD projection should preserve rdf:type information for graph rendering.");
	}

	@Test
	void snapshotJsonLd_exposesNamedGraphContainers() throws IOException {
		File file = writeTempRdf("named-graph.trig", """
				@prefix ex: <http://example.org/> .
				ex:defaultS ex:p ex:defaultO .
				ex:graphOne {
				  ex:s ex:p ex:o .
				}
				""");

		rdfDataService.loadFile(file);
		String jsonLd = projectionService.snapshotJsonLd();

		assertTrue(jsonLd.contains("graphOne"),
				"Named graph identifier should be present in the JSON-LD snapshot.");
		assertTrue(jsonLd.contains("\"@graph\""),
				"JSON-LD snapshot should contain @graph containers for named graphs.");
	}

	@Test
	void snapshotJsonLd_reflectsIncrementalLoadsWithoutStaleState() throws IOException {
		File firstFile = writeTempRdf("first.ttl", """
				@prefix ex: <http://example.org/> .
				ex:a ex:p ex:b .
				""");
		File secondFile = writeTempRdf("second.ttl", """
				@prefix ex: <http://example.org/> .
				ex:c ex:p ex:d .
				""");

		rdfDataService.loadFile(firstFile);
		String firstSnapshot = projectionService.snapshotJsonLd();
		assertTrue(firstSnapshot.contains("\"@id\": \"ns1:a\""),
				"First load should be reflected in the initial JSON-LD projection.");

		rdfDataService.loadFile(secondFile);
		String secondSnapshot = projectionService.snapshotJsonLd();
		assertTrue(secondSnapshot.contains("\"@id\": \"ns1:a\""),
				"Second projection should still contain triples from the first load.");
		assertTrue(secondSnapshot.contains("\"@id\": \"ns1:c\""),
				"Second projection should contain triples from the incremental load.");
	}

	@Test
	void snapshotJsonLd_escapesInnerQuotesInLiteralValues() throws IOException {
		File file = writeTempRdf("rss-feed.rdf", """
				<?xml version="1.0" encoding="utf-8" ?>
				<rdf:RDF
				    xmlns:rdf="http://www.w3.org/1999/02/22-rdf-syntax-ns#"
				    xmlns="http://purl.org/rss/1.0/"
				    xmlns:dc="http://purl.org/dc/elements/1.1/"
				    xmlns:content="http://purl.org/rss/1.0/modules/content/">
				  <item rdf:about="http://example.org/item">
				    <dc:title>Test</dc:title>
				    <content:encoded><![CDATA[<div class="answer">Quoted "text"</div>]]></content:encoded>
				  </item>
				</rdf:RDF>
				""");

		rdfDataService.loadFile(file);
		String jsonLd = projectionService.snapshotJsonLd();

		assertTrue(jsonLd.contains("\"@value\""),
				"JSON-LD snapshot should include literal values.");
		assertTrue(jsonLd.contains("\\\"answer\\\""),
				"Inner quotes in literal values should be escaped in JSON-LD output.");
		assertFalse(BROKEN_QUOTE_IN_VALUE_PATTERN.matcher(jsonLd).find(),
				"JSON-LD snapshot should not contain malformed @value strings with unescaped quotes.");
		assertFalse(BROKEN_CONTROL_CHAR_IN_VALUE_PATTERN.matcher(jsonLd).find(),
				"JSON-LD snapshot should not contain raw control characters inside @value strings.");
	}

	private File writeTempRdf(String fileName, String content) throws IOException {
		Path filePath = tempDir.resolve(fileName);
		Files.writeString(filePath, content, StandardCharsets.UTF_8);
		return filePath.toFile();
	}
}
