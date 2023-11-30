package me.oxstone.googlenmtapplier;

import javafx.application.Application;
import javafx.application.Platform;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.stage.Stage;
import me.oxstone.googlenmtapplier.controller.MainFxController;
import net.rgielen.fxweaver.core.FxWeaver;
import org.springframework.boot.builder.SpringApplicationBuilder;
import org.springframework.context.ConfigurableApplicationContext;

public class JavaFxApplication extends Application {

    private ConfigurableApplicationContext applicationContext;
    public static final String PROGRAM_VER = "Google NMT Applier Ver 2.7.1";
    public static final String PROGRAM_AUTHOR = "oxstone7@gmail.com";
    public static final String PROGRAM_COPYRIGHT = "World Mission Society Church of God";
    public static final String PROGRAM_LAST_MODIFIED = "30 Nov 2023";

    @Override
    public void init() {
        String[] args = getParameters().getRaw().toArray(new String[0]);

        this.applicationContext = new SpringApplicationBuilder()
                .sources(GoogleNMTApplierApplication.class)
                .run(args);
    }

    @Override
    public void stop() {
        this.applicationContext.close();
        Platform.exit();
    }

    @Override
    public void start(Stage stage) throws Exception {
        FxWeaver fxWeaver = applicationContext.getBean(FxWeaver.class);
        Parent root = fxWeaver.loadView(MainFxController.class);

        Scene scene = new Scene(root);
        scene.getStylesheets().add(MainFxController.class.getResource("Stylesheet.css").toExternalForm());
        stage.setTitle(PROGRAM_VER);
        stage.setScene(scene);
        stage.setResizable(false);
        stage.show();
    }
}
