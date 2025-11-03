package fr.inria.corese.demo.factory.popup;

import fr.inria.corese.demo.manager.QueryManager;
import java.util.HashMap;
import java.util.Map;

public class PopupFactory {
  public static final String WARNING_POPUP = "warning";
  public static final String FILE_INFO_POPUP = "fileInfo";
  public static final String LOG_POPUP = "log";
  public static final String NEW_FILE_POPUP = "newFile";
  public static final String RULE_INFO_POPUP = "ruleInfo";
  public static final String TOAST_NOTIFICATION = "toast";
  public static final String RENAME_POPUP = "rename";
  public static final String DELETE_POPUP = "delete";
  public static final String CLEAR_GRAPH_CONFIRMATION = "clearGraphConfirmation";
  public static final String SAVE_FILE_CONFIRMATION = "saveFileConfirmation";
  public static final String LOADING_POPUP = "loading";

  private final Map<String, IPopup> popupCache = new HashMap<>();
  private static PopupFactory instance;
  private final QueryManager stateManager;

  /** Private constructor for singleton pattern. */
  public PopupFactory() {
    this.stateManager = QueryManager.getInstance();
  }

  /**
   * Gets the singleton instance.
   *
   * @return The singleton instance
   */
  public static synchronized PopupFactory getInstance() {
    if (instance == null) {
      instance = new PopupFactory();
    }
    return instance;
  }

  public IPopup createPopup(String type) {
    if (popupCache.containsKey(type)) {
      return popupCache.get(type);
    }

    IPopup popup =
        switch (type) {
          case WARNING_POPUP -> new WarningPopup();
          case FILE_INFO_POPUP -> new FileInfoPopup();
          case LOG_POPUP -> new LogDialog();
          case NEW_FILE_POPUP -> new NewFilePopup();
          case RULE_INFO_POPUP -> new RuleInfoPopup();
          case TOAST_NOTIFICATION -> new ToastNotification();
          case RENAME_POPUP -> new RenamePopup();
          case DELETE_POPUP -> new DeleteConfirmationPopup();
          case CLEAR_GRAPH_CONFIRMATION -> new ClearGraphConfirmationPopup();
          case SAVE_FILE_CONFIRMATION -> new SaveConfirmationPopup();
          case LOADING_POPUP -> new LoadingPopup();
          default -> throw new IllegalArgumentException("Unknown popup type: " + type);
        };

    popupCache.put(type, popup);
    return popup;
  }
}
