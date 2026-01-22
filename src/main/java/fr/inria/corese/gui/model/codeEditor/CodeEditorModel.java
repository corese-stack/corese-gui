package fr.inria.corese.gui.model.codeEditor;

import java.io.File;
import java.util.Stack;

import fr.inria.corese.gui.model.QueryResult;
import javafx.beans.property.BooleanProperty;
import javafx.beans.property.ReadOnlyStringProperty;
import javafx.beans.property.ReadOnlyStringWrapper;
import javafx.beans.property.SimpleBooleanProperty;
import javafx.beans.property.SimpleStringProperty;
import javafx.beans.property.StringProperty;

public class CodeEditorModel {
  private final StringProperty content = new SimpleStringProperty("");
  private final StringProperty filePath = new SimpleStringProperty(null);
  private final BooleanProperty modified = new SimpleBooleanProperty(false);
  private final ReadOnlyStringWrapper displayName = new ReadOnlyStringWrapper("Untitled");
  private final Stack<String> undoStack = new Stack<>();
  private final Stack<String> redoStack = new Stack<>();
  private boolean isUndoingOrRedoing = false;
  private String currentSavedContent = "";
  private QueryResult lastQueryResult;

  public CodeEditorModel() {
    filePath.addListener((obs, o, n) -> updateDisplayName());
    modified.addListener((obs, o, n) -> updateDisplayName());
    content.addListener(
        (obs, oldVal, newVal) -> {
          if (newVal != null) {
            setModified(!newVal.equals(currentSavedContent));
            if (!isUndoingOrRedoing) recordChange(oldVal);
          }
        });
    updateDisplayName();
  }

  private void recordChange(String oldContent) {
    if (oldContent != null) {
      undoStack.push(oldContent);
      redoStack.clear();
    }
  }

  public void undo() {
    if (canUndo()) {
      isUndoingOrRedoing = true;
      redoStack.push(getContent());
      setContent(undoStack.pop());
      isUndoingOrRedoing = false;
    }
  }

  public void redo() {
    if (canRedo()) {
      isUndoingOrRedoing = true;
      undoStack.push(getContent());
      setContent(redoStack.pop());
      isUndoingOrRedoing = false;
    }
  }

  public boolean canUndo() {
    return !undoStack.isEmpty();
  }

  public boolean canRedo() {
    return !redoStack.isEmpty();
  }

  private void updateDisplayName() {
    String name = "Untitled";
    if (filePath.get() != null) name = new File(filePath.get()).getName();
    displayName.set(name);
  }

  public void markAsSaved() {
    this.currentSavedContent = getContent();
    setModified(false);
    undoStack.clear();
    redoStack.clear();
  }

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

  public QueryResult getLastQueryResult() {
    return lastQueryResult;
  }

  public void setLastQueryResult(QueryResult result) {
    this.lastQueryResult = result;
  }
}
