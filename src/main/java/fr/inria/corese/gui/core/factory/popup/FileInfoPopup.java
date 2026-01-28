package fr.inria.corese.gui.core.factory.popup;

import java.io.File;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.layout.HBox;
import javafx.scene.layout.VBox;

public class FileInfoPopup extends BasePopup {
  private final Label fileNameLabel;
  private final Label fileTypeLabel;
  private final Label fileSizeLabel;
  private final Label fileModifiedLabel;
  private final Label fileLoadingTimeLabel;
  private final Button okButton;

  public FileInfoPopup() {
    setTitle("File Information");

    VBox contentPane = new VBox(10);
    contentPane.setPadding(new Insets(10, 10, 10, 10));

    fileNameLabel = new Label();
    fileTypeLabel = new Label();
    fileSizeLabel = new Label();
    fileModifiedLabel = new Label();
    fileLoadingTimeLabel = new Label();

    okButton = new Button("OK");
    okButton.setOnAction(e -> hide());

    HBox buttonBox = new HBox(10);
    buttonBox.setAlignment(Pos.CENTER_RIGHT);
    buttonBox.getChildren().add(okButton);

    contentPane
        .getChildren()
        .addAll(
            fileNameLabel,
            fileTypeLabel,
            fileSizeLabel,
            fileModifiedLabel,
            fileLoadingTimeLabel,
            buttonBox);

    getDialogPane().setContent(contentPane);
  }

  public void show(File file) {
    fileNameLabel.setText("File name: " + file.getName());
    fileTypeLabel.setText("Type: " + getFileType(file));
    fileSizeLabel.setText("Size: " + formatFileSize(file.length()));
    fileModifiedLabel.setText("Last modified: " + formatDate(file.lastModified()));
    fileLoadingTimeLabel.setText("Loading time: Calculating...");

    showAndWait();
  }

  private String getFileType(File file) {
    String name = file.getName();
    int lastIndexOf = name.lastIndexOf(".");
    if (lastIndexOf == -1) {
      return "Unknown";
    }
    return name.substring(lastIndexOf + 1);
  }

  private String formatFileSize(long size) {
    if (size < 1024) return size + " B";
    int z = (63 - Long.numberOfLeadingZeros(size)) / 10;
    return String.format("%.1f %sB", (double) size / (1L << (z * 10)), " KMGTPE".charAt(z));
  }

  private String formatDate(long time) {
    return DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss")
        .format(LocalDateTime.ofInstant(Instant.ofEpochMilli(time), ZoneId.systemDefault()));
  }
}
