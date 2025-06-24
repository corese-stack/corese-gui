module fr.inria.corese.demo {
    requires javafx.web;
    requires static lombok;
    requires org.kordamp.ikonli.javafx;
    requires org.kordamp.ikonli.feather;
    requires atlantafx.base;
    requires jdk.jsobject;
    requires org.kordamp.ikonli.materialdesign;
    requires org.kordamp.ikonli.materialdesign2;
    requires org.kordamp.ikonli.core;
    requires fr.inria.corese.corese_core;
    requires MaterialFX;
    requires java.logging;
    requires transitive javafx.graphics;

    exports fr.inria.corese.demo;
    exports fr.inria.corese.demo.controller;
    exports fr.inria.corese.demo.view;
    exports fr.inria.corese.demo.model;

    opens fr.inria.corese.demo.controller to javafx.fxml;
    opens fr.inria.corese.demo.view to javafx.fxml;
    opens fr.inria.corese.demo.model to javafx.fxml;

    exports fr.inria.corese.demo.model.codeEditor;

    opens fr.inria.corese.demo.model.codeEditor to javafx.fxml;

    exports fr.inria.corese.demo.enums.icon;

    opens fr.inria.corese.demo.enums.icon to javafx.fxml;

    exports fr.inria.corese.demo.enums.button;

    opens fr.inria.corese.demo.enums.button to javafx.fxml;

    exports fr.inria.corese.demo.model.fileList;

    opens fr.inria.corese.demo.model.fileList to javafx.fxml;

    exports fr.inria.corese.demo.view.icon;

    opens fr.inria.corese.demo.view.icon to javafx.fxml;

    exports fr.inria.corese.demo.view.rule;

    opens fr.inria.corese.demo.view.rule to javafx.fxml;

    exports fr.inria.corese.demo.view.codeEditor;

    opens fr.inria.corese.demo.view.codeEditor to javafx.fxml;

    exports fr.inria.corese.demo.model.graph;

    opens fr.inria.corese.demo.model.graph to javafx.fxml;
}