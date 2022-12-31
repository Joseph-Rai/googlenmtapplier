package me.oxstone.googlenmtapplier.controller;

import javafx.collections.ObservableList;
import javafx.fxml.FXML;
import javafx.fxml.Initializable;
import javafx.scene.Node;
import javafx.scene.control.Label;
import javafx.scene.control.TextArea;
import javafx.scene.layout.VBox;
import lombok.RequiredArgsConstructor;
import net.rgielen.fxweaver.core.FxmlView;
import org.springframework.stereotype.Controller;

import javax.annotation.Nonnull;
import java.net.URL;
import java.util.ResourceBundle;

@Controller
@RequiredArgsConstructor
@FxmlView("FileEditor.fxml")
public class FileEditorController implements Initializable {

    @Nonnull
    MainFxController mainFxController;

    @FXML
    VBox rootVBox;

    @Override
    public void initialize(URL url, ResourceBundle resourceBundle) {
        ObservableList<Node> children = rootVBox.getChildren();

        for (Node node : children) {
            if (node instanceof TextArea) {
                mainFxController.arrangeTextAreaHeight((TextArea) node, ((TextArea) node).getText());
            }
        }
    }
}
