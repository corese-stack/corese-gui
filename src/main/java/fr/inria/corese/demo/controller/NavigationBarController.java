package fr.inria.corese.demo.controller;

import fr.inria.corese.demo.manager.ApplicationStateManager;
import fr.inria.corese.demo.view.NavigationBarView;
import javafx.fxml.FXMLLoader;
import javafx.scene.Node;
import javafx.scene.control.Button;
import javafx.scene.layout.BorderPane;

import java.io.IOException;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;

/**
 * Controller for the navigation bar.
 * Manages navigation between different views of the application.
 * It caches specific views (like editors) to preserve their state during
 * navigation.
 */
public class NavigationBarController {
    private final BorderPane mainContent;
    private final ApplicationStateManager stateManager;
    private final NavigationBarView view;
    private String currentViewName;

    private final Map<String, Node> cachedViews = new HashMap<>();
    private final Map<String, Object> cachedControllers = new HashMap<>();

    private final Set<String> cachedViewNames = Set.of(
            "query-view",
            "validation-view",
            "rdf-editor-view");

    /**
     * Constructor for the navigation bar controller.
     *
     * @param mainContent The main content pane
     */
    public NavigationBarController(BorderPane mainContent) {
        if (mainContent == null) {
            throw new IllegalArgumentException("mainContent cannot be null");
        }
        this.mainContent = mainContent;
        this.stateManager = ApplicationStateManager.getInstance();
        this.view = new NavigationBarView();

        // Preload all views marked for caching at application startup.
        for (String viewName : cachedViewNames) {
            preloadView(viewName);
        }

        initializeButtons();
    }

    /**
     * Preloads and caches a specified view and its controller.
     * This is called at startup for all views listed in cachedViewNames.
     *
     * @param viewName The name of the view to preload (e.g., "query-view").
     */
    private void preloadView(String viewName) {
        String fxmlPath = "/fr/inria/corese/demo/" + viewName + ".fxml";
        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource(fxmlPath));
            Node content = loader.load();
            Object controller = loader.getController();
            cachedViews.put(viewName, content);
            cachedControllers.put(viewName, controller);
            stateManager.addLogEntry("Preloaded and cached " + viewName);
        } catch (IOException e) {
            stateManager.addLogEntry("Error preloading " + viewName + ": " + e.getMessage());
            e.printStackTrace();
        }
    }

    /**
     * Selects and loads a view into the main content area.
     * It uses the cache for stateful views or loads them new for stateless views.
     *
     * @param viewName The name of the view to select
     */
    public void selectView(String viewName) {
        try {
            if (currentViewName != null) {
                stateManager.saveCurrentState();
                stateManager.addLogEntry("State saved before navigating from " + currentViewName + " to " + viewName);
            }

            currentViewName = viewName;
            Node content;

            if (cachedViewNames.contains(viewName)) {
                content = cachedViews.get(viewName);
                stateManager.addLogEntry("Using cached " + viewName);
            } else {
                String fxmlPath = "/fr/inria/corese/demo/" + viewName + ".fxml";
                FXMLLoader loader = new FXMLLoader(getClass().getResource(fxmlPath));
                content = loader.load();
                Object controller = loader.getController();

                if (controller instanceof DataViewController) {
                    stateManager.restoreState();
                    stateManager.addLogEntry("Restored state for DataViewController");
                }
            }

            Button selectedButton = getButtonForView(viewName);
            if (selectedButton != null) {
                view.setButtonSelected(selectedButton);
            }

            mainContent.setCenter(content);
            stateManager.addLogEntry("View changed to " + viewName);

        } catch (IOException e) {
            stateManager.addLogEntry("Error loading view: " + e.getMessage());
            e.printStackTrace();
        }
    }

    /**
     * Initializes the button event handlers.
     */
    private void initializeButtons() {
        view.getDataButton().setOnAction(e -> {
            stateManager.addLogEntry("Data button clicked");
            selectView("data-view");
        });

        view.getRdfEditorButton().setOnAction(e -> {
            stateManager.addLogEntry("RDF Editor button clicked");
            selectView("rdf-editor-view");
        });

        view.getValidationButton().setOnAction(e -> {
            stateManager.addLogEntry("Validation button clicked");
            selectView("validation-view");
        });

        view.getQueryButton().setOnAction(e -> {
            stateManager.addLogEntry("Query button clicked");
            selectView("query-view");
        });

        view.getSettingsButton().setOnAction(e -> {
            stateManager.addLogEntry("Settings button clicked");
            selectView("settings-view");
        });
    }

    /**
     * Gets the button for a specific view.
     *
     * @param viewName The name of the view
     * @return The button for the view
     */
    private Button getButtonForView(String viewName) {
        return switch (viewName) {
            case "data-view" -> view.getDataButton();
            case "validation-view" -> view.getValidationButton();
            case "rdf-editor-view" -> view.getRdfEditorButton();
            case "query-view" -> view.getQueryButton();
            case "settings-view" -> view.getSettingsButton();
            default -> null;
        };
    }

    /**
     * Gets the navigation bar view.
     *
     * @return The navigation bar view
     */
    public NavigationBarView getView() {
        return view;
    }
}