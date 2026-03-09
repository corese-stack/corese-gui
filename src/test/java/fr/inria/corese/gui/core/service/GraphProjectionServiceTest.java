package fr.inria.corese.gui.core.service;

import fr.inria.corese.gui.core.enums.SerializationFormat;
import fr.inria.corese.gui.core.model.ValidationResult;
import java.io.File;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.regex.Pattern;
import org.json.JSONArray;
import org.json.JSONObject;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class GraphProjectionServiceTest {

	private static final String BEATLES_INVALID_DATA = """
			PREFIX : <http://example.com/tutorial/>
			PREFIX rdf: <http://www.w3.org/1999/02/22-rdf-syntax-ns#>
			PREFIX xsd: <http://www.w3.org/2001/XMLSchema#>

			:The_Beatles      rdf:type  :Band .
			:The_Beatles      :name     "The Beatles" .
			:The_Beatles      :member   :John_Lennon .
			:The_Beatles      :member   :Paul_McCartney .
			:The_Beatles      :member   :Ringo_Starr .
			:The_Beatles      :member   :George_Harrison .
			:John_Lennon      rdf:type  :SoloArtist .
			:Paul_McCartney   rdf:type  :SoloArtist .
			:Ringo_Starr      rdf:type  :SoloArtist .
			:George_Harrison  rdf:type  :SoloArtist .
			:Please_Please_Me rdf:type  :Album .
			:Please_Please_Me :name     "Please Please Me" .
			:Please_Please_Me :name     "Banana" .
			:Please_Please_Me :date     "1963-03-22"^^xsd:date .
			:Please_Please_Me :artist   :The_Beatles .
			:Please_Please_Me :track    :Love_Me_Do .
			:Love_Me_Do       rdf:type  :Song .
			:Love_Me_Do       :name     "Love Me Do" .
			:Love_Me_Do       :length   125 .
			:Love_Me_Do       :writer   :John_Lennon .
			:Love_Me_Do       :writer   :Paul_McCartney .

			:McCartney        rdf:type  :Album .
			:McCartney        :name     "McCartney" .
			:McCartney        :date     "1970-04-17"^^xsd:date .
			:McCartney        :artist   :Paul_McCartney .

			:Imagine          rdf:type  :Album .
			:Imagine          :name     "Imagine" .
			:Imagine          :date     "1971-10-11"^^xsd:date .
			:Imagine          :artist   :John_Lennon .
			""";

	private static final String BEATLES_VALIDATOR_SHAPES = """
			PREFIX sh: <http://www.w3.org/ns/shacl#>
			PREFIX xsd: <http://www.w3.org/2001/XMLSchema#>
			PREFIX : <http://example.com/tutorial/>

			:BandShape a sh:NodeShape ;
			    sh:targetClass :Band ;
			    sh:property [
			        sh:path :name ;
			        sh:datatype xsd:string ;
			        sh:minCount 1 ;
			        sh:maxCount 1 ;
			    ] ;
			    sh:property [
			        sh:path :member ;
			        sh:class :SoloArtist ;
			        sh:minCount 1 ;
			    ] .

			:SoloArtistShape a sh:NodeShape ;
			    sh:targetClass :SoloArtist .

			:AlbumShape a sh:NodeShape ;
			    sh:targetClass :Album ;
			    sh:property [
			        sh:path :name ;
			        sh:datatype xsd:string ;
			        sh:minCount 1 ;
			        sh:maxCount 1 ;
			    ] ;
			    sh:property [
			        sh:path :date ;
			        sh:datatype xsd:date ;
			        sh:minCount 1 ;
			        sh:maxCount 1 ;
			    ] ;
			    sh:property [
			        sh:path :artist ;
			        sh:nodeKind sh:IRI ;
			        sh:minCount 1 ;
			        sh:maxCount 1 ;
			    ] .

			:SongShape a sh:NodeShape ;
			    sh:targetClass :Song ;
			    sh:property [
			        sh:path :name ;
			        sh:datatype xsd:string ;
			        sh:minCount 1 ;
			        sh:maxCount 1 ;
			    ] ;
			    sh:property [
			        sh:path :length ;
			        sh:datatype xsd:integer ;
			        sh:minCount 1 ;
			        sh:maxCount 1 ;
			    ] ;
			    sh:property [
			        sh:path :writer ;
			        sh:nodeKind sh:IRI ;
			        sh:minCount 1 ;
			    ] .
			""";

	private static final Pattern BROKEN_QUOTE_IN_VALUE_PATTERN = Pattern
			.compile("\\\"@value\\\"\\s*:\\s*\\\"(?:[^\\\"\\\\]|\\\\.)*\\\"(?=\\s*[A-Za-z0-9_<])", Pattern.MULTILINE);
	private static final Pattern BROKEN_CONTROL_CHAR_IN_VALUE_PATTERN = Pattern.compile(
			"\\\"@value\\\"\\s*:\\s*\\\"(?:[^\\\"\\\\]|\\\\.)*[\\n\\r\\t](?:[^\\\"\\\\]|\\\\.)*\\\"",
			Pattern.MULTILINE);

	@TempDir
	Path tempDir;

	private final RdfDataService rdfDataService = RdfDataService.getInstance();
	private final GraphProjectionService projectionService = GraphProjectionService.getInstance();
	private final ShaclService shaclService = ShaclService.getInstance();

	@BeforeEach
	@AfterEach
	void resetServicesState() {
		rdfDataService.clearData();
		shaclService.clearCachedReportsForTesting();
	}

	@Test
	void snapshotJsonLd_preservesRdfTypeStatements() throws IOException {
		File file = writeTempRdf("rdf-type.ttl", """
				@prefix ex: <http://example.org/> .
				ex:alice a ex:Person .
				""");

		rdfDataService.loadFile(file);
		String jsonLd = projectionService.snapshotJsonLd();

		assertTrue(jsonLd.contains("Person"), "JSON-LD projection should contain the rdf:type object value label.");
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

		assertTrue(jsonLd.contains("graphOne"), "Named graph identifier should be present in the JSON-LD snapshot.");
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

		assertTrue(jsonLd.contains("\"@value\""), "JSON-LD snapshot should include literal values.");
		assertTrue(jsonLd.contains("\\\"answer\\\""),
				"Inner quotes in literal values should be escaped in JSON-LD output.");
		assertFalse(BROKEN_QUOTE_IN_VALUE_PATTERN.matcher(jsonLd).find(),
				"JSON-LD snapshot should not contain malformed @value strings with unescaped quotes.");
		assertFalse(BROKEN_CONTROL_CHAR_IN_VALUE_PATTERN.matcher(jsonLd).find(),
				"JSON-LD snapshot should not contain raw control characters inside @value strings.");
	}

	@Test
	void snapshotJsonLd_sanitizesInvalidBackslashEscapesInLiteralValues() throws IOException {
		File file = writeTempRdf("invalid-escape.ttl", """
				@prefix ex: <http://example.org/> .
				ex:item ex:label "L\\\\'article et test \\\\x." .
				""");

		rdfDataService.loadFile(file);
		String jsonLd = projectionService.snapshotJsonLd();

		int invalidEscapeIndex = findInvalidJsonEscapeIndex(jsonLd);
		assertEquals(-1, invalidEscapeIndex, "JSON-LD snapshot should not contain invalid string escapes: "
				+ excerptAround(jsonLd, invalidEscapeIndex));
	}

	@Test
	void sanitizeMalformedJsonLd_keepsInnerQuotesFollowedByCommaInsideLiteral() {
		String malformedJsonLd = """
				{
				  "@context": {},
				  "@graph": [
				    {
				      "@id": "http://example.org/s",
				      "http://example.org/p": [
				        {
				          "@value": "The term "knowledge engineering", sometimes refers to "expert systems".",
				          "@language": "en"
				        }
				      ]
				    }
				  ]
				}
				""";

		String sanitized = sanitizeJsonLd(malformedJsonLd);

		assertTrue(sanitized.contains("\\\"knowledge engineering\\\", sometimes refers to \\\"expert systems\\\"."),
				"Sanitizer should escape inner quoted fragments even when followed by commas.");
		assertFalse(sanitized.contains("\"@value\": \"The term \"knowledge engineering\","),
				"Inner quotes should not prematurely terminate @value string.");
		int invalidEscapeIndex = findInvalidJsonEscapeIndex(sanitized);
		assertEquals(-1, invalidEscapeIndex,
				"Sanitized JSON-LD should keep valid escapes only: " + excerptAround(sanitized, invalidEscapeIndex));
	}

	@Test
	void sanitizeMalformedJsonLd_keepsValidEmptyLiteralValuesUntouched() {
		String validJsonLd = """
				{
				  "@graph": [
				    {
				      "@id": "http://example.org/a",
				      "rdfs:label": {
				        "@value": "",
				        "@language": "en"
				      }
				    },
				    {
				      "@id": "http://example.org/b",
				      "rdfs:label": {
				        "@value": "next object",
				        "@language": "en"
				      }
				    }
				  ]
				}
				""";

		String sanitized = sanitizeJsonLd(validJsonLd);

		assertTrue(sanitized.contains("\"@value\": \"\""), "Valid empty literal values should be preserved.");
		assertTrue(sanitized.contains("\"@id\": \"http://example.org/b\""),
				"Sanitizer should not swallow following objects after empty literals.");
	}

	@Test
	void sanitizeMalformedJsonLd_keepsQuotesBeforeClosingBracketInsideLiteral() {
		String malformedJsonLd = """
				{
				  "@graph": [
				    {
				      "@id": "http://example.org/s",
				      "ex:p": [
				        {
				          "@value": "Data [where term means: "fact"]",
				          "@language": "en"
				        }
				      ]
				    }
				  ]
				}
				""";

		String sanitized = sanitizeJsonLd(malformedJsonLd);

		assertTrue(sanitized.contains("\\\"fact\\\"]"),
				"Quote followed by closing bracket inside literal should be escaped, not treated as value terminator.");
	}

	@Test
	void sanitizeJsonLdForDisplay_producesValidJsonForBeatlesShaclReport() throws IOException {
		File dataFile = writeTempRdf("beatles-invalid.ttl", BEATLES_INVALID_DATA);
		rdfDataService.loadFile(dataFile);

		ValidationResult validationResult = shaclService.validate(BEATLES_VALIDATOR_SHAPES);
		String reportId = validationResult.getReportId();
		String rawJsonLd = shaclService.formatReport(reportId, SerializationFormat.JSON_LD);
		String sanitized = projectionService.sanitizeJsonLdForDisplay(rawJsonLd);

		assertTrue(sanitized != null && !sanitized.isBlank(),
				"Sanitized SHACL report should produce a non-empty JSON-LD payload.");
		assertDoesNotThrow(() -> assertValidJson(sanitized),
				"Sanitized SHACL JSON-LD should be strict-JSON parseable.");
		assertEquals(-1, findInvalidJsonEscapeIndex(sanitized),
				"Sanitized SHACL JSON-LD should not contain invalid escapes: "
						+ excerptAround(sanitized, findInvalidJsonEscapeIndex(sanitized)));
	}

	private File writeTempRdf(String fileName, String content) throws IOException {
		Path filePath = tempDir.resolve(fileName);
		Files.writeString(filePath, content, StandardCharsets.UTF_8);
		return filePath.toFile();
	}

	private String sanitizeJsonLd(String jsonLd) {
		Object result = projectionService.sanitizeJsonLdForDisplay(jsonLd);
		return result == null ? "" : result.toString();
	}

	private static int findInvalidJsonEscapeIndex(String json) {
		if (json == null || json.isEmpty()) {
			return -1;
		}
		boolean inString = false;
		boolean escaping = false;
		for (int i = 0; i < json.length(); i++) {
			char current = json.charAt(i);
			if (!inString) {
				if (current == '"') {
					inString = true;
				}
				continue;
			}
			if (escaping) {
				if (current == 'u') {
					if (i + 4 >= json.length() || !isHexDigit(json.charAt(i + 1)) || !isHexDigit(json.charAt(i + 2))
							|| !isHexDigit(json.charAt(i + 3)) || !isHexDigit(json.charAt(i + 4))) {
						return i;
					}
					i += 4;
					escaping = false;
					continue;
				}
				if (current != '"' && current != '\\' && current != '/' && current != 'b' && current != 'f'
						&& current != 'n' && current != 'r' && current != 't') {
					return i;
				}
				escaping = false;
				continue;
			}
			if (current == '\\') {
				escaping = true;
				continue;
			}
			if (current == '"') {
				inString = false;
				continue;
			}
			if (current <= 0x1F) {
				return i;
			}
		}
		return escaping ? json.length() - 1 : -1;
	}

	private static boolean isHexDigit(char value) {
		return (value >= '0' && value <= '9') || (value >= 'a' && value <= 'f') || (value >= 'A' && value <= 'F');
	}

	private static String excerptAround(String value, int index) {
		if (value == null || value.isEmpty() || index < 0) {
			return "";
		}
		int start = Math.max(0, index - 24);
		int end = Math.min(value.length(), index + 24);
		return value.substring(start, end).replace('\n', ' ').replace('\r', ' ');
	}

	private static void assertValidJson(String json) {
		String trimmed = json == null ? "" : json.trim();
		if (trimmed.isEmpty()) {
			throw new IllegalArgumentException("JSON-LD payload is empty.");
		}
		if (trimmed.startsWith("{")) {
			new JSONObject(trimmed);
			return;
		}
		if (trimmed.startsWith("[")) {
			new JSONArray(trimmed);
			return;
		}
		throw new IllegalArgumentException("JSON-LD payload must start with '{' or '['.");
	}
}
