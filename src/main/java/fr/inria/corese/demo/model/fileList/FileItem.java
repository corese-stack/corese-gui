package fr.inria.corese.demo.model.fileList;

import java.io.File;

/**
 * Représente un élément de fichier dans une liste de fichiers.
 *
 * <p>Cette classe encapsule les informations de base d'un fichier, notamment son nom et son état de
 * chargement.
 *
 * <p>Caractéristiques principales : - Stockage du nom du fichier - Suivi de l'état de chargement du
 * fichier
 *
 * <p>Utilisation typique dans la gestion de listes de fichiers au sein d'une application de
 * traitement de données.
 *
 * @author Clervie Causer
 * @version 1.0
 * @since 2025
 */
public class FileItem {
  private final File file;
  private boolean loading;

  public FileItem(File file) {
    this.file = file;
    this.loading = false;
  }

  public String getName() {
    return file.getName();
  }

  public File getFile() {
    return file;
  }

  /**
   * Vérifie si le fichier est en cours de chargement.
   *
   * @return true si le fichier est en cours de chargement, false sinon
   */
  public boolean isLoading() {
    return loading;
  }

  /**
   * Définit l'état de chargement du fichier.
   *
   * @param loading Nouvel état de chargement du fichier
   */
  public void setLoading(boolean loading) {
    this.loading = loading;
  }
}
