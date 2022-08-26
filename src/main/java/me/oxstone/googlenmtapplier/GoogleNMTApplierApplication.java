package me.oxstone.googlenmtapplier;

import javafx.application.Application;
import org.springframework.boot.autoconfigure.SpringBootApplication;

@SpringBootApplication
public class GoogleNMTApplierApplication {

    /*
    * JavaFX를 SpringBoot Framework에서 실행하기 위한 Weaver Lunch
    */
    public static void main(String[] args) {
        Application.launch(JavaFxApplication.class, args);
    }

}
