package fr.inria.corese.gui.core.service;

import fr.inria.corese.gui.core.service.ReasoningService.RuleFileState;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.regex.Pattern;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class ReasoningCustomRuleGraphNameTest {

	private static final Pattern CUSTOM_RULE_GRAPH_PATTERN = Pattern
			.compile("^urn:corese:inference:custom:[a-z0-9-]+:[0-9a-f]{12}$");

	@TempDir
	Path tempDir;

	private final RdfDataService rdfDataService = RdfDataService.getInstance();
	private final ReasoningService reasoningService = DefaultReasoningService.getInstance();

	@BeforeEach
	void setUp() {
		reasoningService.resetAllProfiles();
		reasoningService.removeAllRuleFiles();
		rdfDataService.clearData();
	}

	@Test
	void customRuleFile_inferenceGraphNameIsReadableAndStable() throws IOException {
		Path customRule = tempDir.resolve("My Custom Inference Rules.rul");
		String builtInRuleSource = reasoningService.getBuiltInProfileSource(ReasoningProfile.RDFS).sourceContent();
		Files.writeString(customRule, builtInRuleSource, StandardCharsets.UTF_8);

		reasoningService.addRuleFile(customRule.toFile());
		RuleFileState firstState = reasoningService.snapshotRuleFiles().get(0);
		String firstNamedGraphUri = firstState.namedGraphUri();

		assertTrue(firstNamedGraphUri.startsWith("urn:corese:inference:custom:my-custom-inference-rules:"),
				"Named graph URI should expose a readable segment based on the rule file name.");
		assertTrue(CUSTOM_RULE_GRAPH_PATTERN.matcher(firstNamedGraphUri).matches(),
				"Named graph URI should end with a deterministic hexadecimal suffix.");

		reasoningService.removeAllRuleFiles();
		reasoningService.addRuleFile(customRule.toFile());
		RuleFileState secondState = reasoningService.snapshotRuleFiles().get(0);
		String secondNamedGraphUri = secondState.namedGraphUri();

		assertFalse(secondNamedGraphUri.isBlank(), "Named graph URI should never be blank.");
		assertEquals(firstNamedGraphUri, secondNamedGraphUri,
				"Named graph URI should remain stable for the same custom rule file path.");
	}

	@Test
	void customRuleFile_inUnicodeDirectory_loadsWithoutMalformedUrl() throws IOException {
		Path unicodeDir = tempDir.resolve("d\u00e9mo");
		Files.createDirectories(unicodeDir);
		Path customRule = unicodeDir.resolve("sym-trans.rul");
		String builtInRuleSource = reasoningService.getBuiltInProfileSource(ReasoningProfile.RDFS).sourceContent();
		Files.writeString(customRule, builtInRuleSource, StandardCharsets.UTF_8);

		reasoningService.addRuleFile(customRule.toFile());

		assertEquals(1, reasoningService.snapshotRuleFiles().size(),
				"Rule file should load successfully from a path containing non-ASCII characters.");
	}
}
