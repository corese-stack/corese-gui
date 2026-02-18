package fr.inria.corese.gui.feature.editor.code;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;

class CodeEditorModelTest {

	@Test
	void markAsSaved_preservesUndoHistory() {
		CodeEditorModel model = new CodeEditorModel();
		model.setContent("a");
		model.setContent("ab");
		model.markAsSaved();

		assertFalse(model.isModified(), "Save should reset dirty flag.");
		assertTrue(model.canUndo(), "Save should not clear undo history.");

		model.undo();
		assertEquals("a", model.getContent(), "Undo should restore previous content after save.");
		assertTrue(model.isModified(), "Undo after save should mark content as modified.");
	}

	@Test
	void markAsSavedAndResetHistory_clearsUndoRedoHistory() {
		CodeEditorModel model = new CodeEditorModel();
		model.setContent("x");
		model.setContent("xy");

		model.markAsSavedAndResetHistory();

		assertFalse(model.isModified(), "State should be clean after loading/opening content.");
		assertFalse(model.canUndo(), "Open/load baseline should clear undo history.");
		assertFalse(model.canRedo(), "Open/load baseline should clear redo history.");
	}
}
