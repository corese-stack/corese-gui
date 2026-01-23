package fr.inria.corese.gui.view.rule;

import atlantafx.base.controls.Spacer;
import fr.inria.corese.gui.core.ButtonConfig;
import fr.inria.corese.gui.enums.icon.IconButtonType;
import fr.inria.corese.gui.view.icon.IconButtonView;

import java.io.File;
import javafx.scene.control.Button;
import javafx.scene.control.CheckBox;
import javafx.scene.control.Tooltip;
import javafx.scene.layout.HBox;

/**
 * Représente un élément de règle dans l'interface utilisateur.
 *
 * <p>Cette classe définit la structure visuelle d'un élément de règle, comprenant une case à cocher
 * et un bouton de documentation.
 *
 * <p>Caractéristiques principales : - Disposition horizontale (HBox) - Case à cocher avec le nom de
 * la règle - Bouton de documentation
 *
 * @author Clervie Causer
 * @version 1.0
 * @since 2025
 */
public class RuleItem extends HBox {
  private final CheckBox checkBox;
  private final IconButtonView documentationButton;
  private final File ruleFile;
  private final String ruleName;

  /**
   * Constructeur d'un élément de règle.
   *
   * <p>Crée un élément de règle avec : - Une case à cocher portant le nom de la règle - Un espace
   * flexible - Un bouton de documentation
   *
   * @param ruleName Le nom de la règle à afficher
   */
  public RuleItem(String ruleName) {
    super(5); // spacing between elements

    this.ruleName = ruleName;
    this.ruleFile = null;

    // Create checkbox with rule name
    checkBox = new CheckBox(ruleName);

    // Create flexible space between checkbox and buttons
    Spacer spacer = new Spacer();

    // Create documentation button using IconButtonView
    documentationButton = new IconButtonView(new ButtonConfig(IconButtonType.DOCUMENTATION, "Show documentation"));

    // Add all elements to the HBox
    getChildren().addAll(checkBox, spacer, documentationButton);
  }

  /**
   * Constructeur d'un élément de règle à partir d'un fichier.
   *
   * @param file Le fichier de règle
   */
  public RuleItem(File file) {
    super(5); // spacing between elements

    this.ruleFile = file;
    this.ruleName = file.getName();

    // Create checkbox with rule name
    checkBox = new CheckBox(file.getName());

    // Create flexible space between checkbox and buttons
    Spacer spacer = new Spacer();

    // Create documentation button using IconButtonView
    documentationButton = new IconButtonView(new ButtonConfig(IconButtonType.DOCUMENTATION, "Show documentation"));

    // Add all elements to the HBox
    getChildren().addAll(checkBox, spacer, documentationButton);
  }

  /**
   * Récupère la case à cocher de l'élément de règle.
   *
   * @return La case à cocher associée à la règle
   */
  public CheckBox getCheckBox() {
    return checkBox;
  }

  /**
   * Récupère le bouton de documentation de l'élément de règle.
   *
   * @return Le bouton de documentation
   */
  public Button getDocumentationButton() {
    return documentationButton;
  }

  /**
   * Récupère le fichier de règle associé.
   *
   * @return Le fichier de règle, ou null si l'élément a été créé à partir d'un nom
   */
  public File getRuleFile() {
    return ruleFile;
  }

  /**
   * Récupère le nom de la règle.
   *
   * @return Le nom de la règle
   */
  public String getRuleName() {
    return ruleName;
  }
}