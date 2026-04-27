package fr.inria.corese.gui.core.service;

import fr.inria.corese.gui.core.service.ReasoningService.RuleFileState;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.LinkedHashSet;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class ReasoningCustomRuleSnapshotStabilityTest {

	private static final Pattern JSON_LD_ID_PATTERN = Pattern.compile("\"@id\"\\s*:\\s*\"([^\"]+)\"");

	@TempDir
	Path tempDir;

	private final RdfDataService rdfDataService = RdfDataService.getInstance();
	private final GraphProjectionService projectionService = GraphProjectionService.getInstance();
	private final ReasoningService reasoningService = DefaultReasoningService.getInstance();

	@BeforeEach
	void setUp() {
		reasoningService.resetAllProfiles();
		reasoningService.removeAllRuleFiles();
		rdfDataService.clearData();
	}

	@Test
	void togglingCustomRule_keepsSnapshotRenderableAndStableForAssertedNodes() throws IOException {
		Path dataset = tempDir.resolve("animals.ttl");
		Files.writeString(dataset, """
				@prefix ex: <http://example.org/> .
				@prefix rdfs: <http://www.w3.org/2000/01/rdf-schema#> .
				ex:Dog rdfs:subClassOf ex:Animal .
				ex:fido a ex:Dog .
				""", StandardCharsets.UTF_8);

		Path customRule = tempDir.resolve("custom-rdfs.rul");
		String builtInRuleSource = reasoningService.getBuiltInProfileSource(ReasoningProfile.RDFS).sourceContent();
		Files.writeString(customRule, builtInRuleSource, StandardCharsets.UTF_8);

		rdfDataService.loadFile(dataset.toFile());
		String beforeEnable = projectionService.snapshotJsonLd();
		Set<String> assertedNodeIds = extractJsonLdIds(beforeEnable);
		assertFalse(assertedNodeIds.isEmpty(), "Asserted snapshot should expose stable JSON-LD node ids.");

		reasoningService.addRuleFile(customRule.toFile());
		RuleFileState rule = reasoningService.snapshotRuleFiles().get(0);
		String afterEnable = projectionService.snapshotJsonLd();
		Set<String> enabledNodeIds = extractJsonLdIds(afterEnable);

		assertFalse(afterEnable.isBlank(), "Snapshot should remain non-empty after enabling a custom rule file.");
		assertTrue(afterEnable.contains(rule.namedGraphUri()),
				"Snapshot should expose the custom rule named graph while the rule file is enabled.");
		assertTrue(enabledNodeIds.containsAll(assertedNodeIds),
				"Enabling a custom rule should preserve previously asserted node ids in the snapshot.");

		reasoningService.setRuleFileEnabled(rule.id(), false);
		String afterDisable = projectionService.snapshotJsonLd();
		Set<String> disabledNodeIds = extractJsonLdIds(afterDisable);

		assertFalse(afterDisable.isBlank(), "Snapshot should remain non-empty after disabling a custom rule file.");
		assertFalse(afterDisable.contains(rule.namedGraphUri()),
				"Snapshot should no longer expose the custom rule named graph once the rule file is disabled.");
		assertEquals(assertedNodeIds, disabledNodeIds,
				"Disabling the custom rule should restore the asserted-node snapshot footprint.");
	}

	private static Set<String> extractJsonLdIds(String snapshot) {
		Set<String> ids = new LinkedHashSet<>();
		if (snapshot == null || snapshot.isBlank()) {
			return ids;
		}
		Matcher matcher = JSON_LD_ID_PATTERN.matcher(snapshot);
		while (matcher.find()) {
			String id = matcher.group(1);
			if (id != null && !id.isBlank()) {
				ids.add(id);
			}
		}
		return ids;
	}
}
