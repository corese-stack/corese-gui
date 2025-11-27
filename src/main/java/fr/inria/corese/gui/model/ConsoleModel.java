package fr.inria.corese.gui.model;

import java.util.ArrayList;
import java.util.List;

import javafx.beans.property.SimpleStringProperty;
import javafx.beans.property.StringProperty;

public class ConsoleModel {
    private final StringProperty content = new SimpleStringProperty("");
    private final List<String> messageHistory = new ArrayList<>();
    private static final int MAX_HISTORY = 1000;

    public void appendMessage(String message) {
        messageHistory.add(message);
        if (messageHistory.size() > MAX_HISTORY) {
            messageHistory.remove(0);
        }
        updateContent();
    }

    public void clear() {
        messageHistory.clear();
        updateContent();
    }

    private void updateContent() {
        content.set(String.join("\n", messageHistory));
    }

    public StringProperty contentProperty() {
        return content;
    }

    public List<String> getMessageHistory() {
        return new ArrayList<>(messageHistory);
    }
}