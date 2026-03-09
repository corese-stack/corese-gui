package fr.inria.corese.gui.core.io;

import java.util.List;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertTrue;

class FileTypeSupportTest {

	@Test
	void rdfExtensions_includeRdfaHtmlExtensions() {
		List<String> extensions = FileTypeSupport.rdfExtensions();

		assertTrue(extensions.contains(".html"), "RDF extension profile should include .html for RDFa files.");
		assertTrue(extensions.contains(".xhtml"), "RDF extension profile should include .xhtml for RDFa files.");
		assertTrue(extensions.contains(".htm"), "RDF extension profile should include .htm as HTML alias.");
	}

	@Test
	void matchesAllowedExtensionsStrict_acceptsRdfaHtmlFiles() {
		List<String> extensions = FileTypeSupport.rdfExtensions();

		assertTrue(FileTypeSupport.matchesAllowedExtensionsStrict("profile.html", extensions),
				".html should be accepted by strict RDF extension matching.");
		assertTrue(FileTypeSupport.matchesAllowedExtensionsStrict("profile.xhtml", extensions),
				".xhtml should be accepted by strict RDF extension matching.");
		assertTrue(FileTypeSupport.matchesAllowedExtensionsStrict("profile.htm", extensions),
				".htm should be accepted by strict RDF extension matching.");
	}
}
