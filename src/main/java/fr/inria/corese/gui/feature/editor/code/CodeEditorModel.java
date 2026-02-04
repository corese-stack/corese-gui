package fr.inria.corese.gui.feature.editor.code;

import java.io.File;
import java.util.ArrayDeque;
import java.util.Deque;
import javafx.beans.property.BooleanProperty;
import javafx.beans.property.ReadOnlyStringProperty;
import javafx.beans.property.ReadOnlyStringWrapper;
import javafx.beans.property.SimpleBooleanProperty;
import javafx.beans.property.SimpleStringProperty;
import javafx.beans.property.StringProperty;

/**
 * Model for the Code Editor.
 *
 * <p>Manages:
 *
 * <ul>
 *   <li>The text content of the editor.
 *   <li>The associated file path (if any).
 *   <li>The "modified" state (dirty flag).
 *   <li>A basic Undo/Redo history (snapshot-based).
 * </ul>
 */
public class CodeEditorModel {

  private final StringProperty content = new SimpleStringProperty("");
  private final StringProperty filePath = new SimpleStringProperty(null);
  private final BooleanProperty modified = new SimpleBooleanProperty(false);
  private final ReadOnlyStringWrapper displayName = new ReadOnlyStringWrapper("Untitled");

  // Simple snapshot-based undo/redo history
  private final Deque<String> undoStack = new ArrayDeque<>();
  private final Deque<String> redoStack = new ArrayDeque<>();
  private boolean isUndoingOrRedoing = false;
  private String currentSavedContent = "";

  public CodeEditorModel() {
    setupListeners();
  }

  private void setupListeners() {
    filePath.addListener((obs, o, n) -> updateDisplayName());

    content.addListener(
        (obs, oldVal, newVal) -> {
          if (newVal != null) {
            // Check dirty state
            boolean isDirty = !newVal.equals(currentSavedContent);
            setModified(isDirty);

            // Record history if not currently performing undo/redo
            if (!isUndoingOrRedoing) {
              recordChange(oldVal);
            }
          }
        });

    updateDisplayName();
  }

  // ==============================================================================================
  // History (Undo/Redo)
  // ==============================================================================================

  private void recordChange(String oldContent) {
    if (oldContent != null) {
      undoStack.push(oldContent);
      redoStack.clear();
    }
  }

  public void undo() {
    if (canUndo()) {
      isUndoingOrRedoing = true;
      try {
        redoStack.push(getContent());
        setContent(undoStack.pop());
      } finally {
        isUndoingOrRedoing = false;
      }
    }
  }

  public void redo() {
    if (canRedo()) {
      isUndoingOrRedoing = true;
      try {
        undoStack.push(getContent());
        setContent(redoStack.pop());
      } finally {
        isUndoingOrRedoing = false;
      }
    }
  }

  public boolean canUndo() {
    return !undoStack.isEmpty();
  }

  public boolean canRedo() {
    return !redoStack.isEmpty();
  }

  // ==============================================================================================
  // File & State Management
  // ==============================================================================================

  private void updateDisplayName() {
    String name = "Untitled";
    String path = filePath.get();
    if (path != null && !path.isBlank()) {
      name = new File(path).getName();
    }
    displayName.set(name);
  }

  public void markAsSaved() {
    this.currentSavedContent = getContent();
    setModified(false);
    // Note: We clear history on save to avoid complexity with "dirty" state tracking relative to
    // history.
    // A more advanced implementation would track the "saved" index in the stack.
    undoStack.clear();
    redoStack.clear();
  }

  // ==============================================================================================
  // Getters & Setters
  // ==============================================================================================

  public StringProperty contentProperty() {
    return content;
  }

  public StringProperty filePathProperty() {
    return filePath;
  }

  public BooleanProperty modifiedProperty() {
    return modified;
  }

  public ReadOnlyStringProperty displayNameProperty() {
    return displayName.getReadOnlyProperty();
  }

  public String getContent() {
    return content.get();
  }

  public void setContent(String content) {
    this.content.set(content);
  }

  public String getFilePath() {
    return filePath.get();
  }

  public void setFilePath(String filePath) {
    this.filePath.set(filePath);
  }

  public boolean isModified() {
    return modified.get();
  }

  public void setModified(boolean modified) {
    this.modified.set(modified);
  }

  public String getDisplayName() {
    return displayName.get();
  }
}
