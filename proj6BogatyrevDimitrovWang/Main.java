/*
 * File: Controller.java
 * Names: Philipp Bogatyrev, Anton Dimitrov, Baron Wang
 * Class: CS 361
 * Project 6
 * Date: March 18
 */

package proj6BogatyrevDimitrovWang;

import javafx.application.Application;
import javafx.collections.ObservableList;
import javafx.event.EventHandler;
import javafx.fxml.FXMLLoader;
import javafx.stage.Stage;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.stage.WindowEvent;

import java.io.IOException;

/**
 * Main class sets up the stage
 *
 */
public class Main extends Application {

    /**
     * Implements the start method of the Application class. This method will
     * be called after {@code launch} method, and it is responsible for initializing
     * the contents of the window.
     *
     * @param primaryStage A Stage object that is created by the {@code launch}
     *                     method
     *                     inherited from the Application class.
     */
    @Override
    public void start(Stage primaryStage){

        // Load fxml file
        FXMLLoader fxmlLoader = new FXMLLoader(getClass().getResource("Main.fxml"));
        Controller controller = new Controller();
        fxmlLoader.setController(controller);
        Parent root;
        try {
            root = fxmlLoader.load();
            controller.initializeFirstTab();


            // handle clicking close box of the window
            primaryStage.setOnCloseRequest(new EventHandler<WindowEvent>() {
                @Override
                public void handle(WindowEvent windowEvent) {
                    controller.handleWindowExit();
                    windowEvent.consume();
                }
            });
    
            primaryStage.setTitle("Project 6");
    
            // Load css file
            Scene scene = new Scene(root);
            ObservableList<String> stylesheets = scene.getStylesheets();
            stylesheets.add(getClass().getResource("Main.css").toExternalForm());
            stylesheets.add(getClass().getResource("java-keywords.css").toExternalForm());
            primaryStage.setScene(scene);
    
            // Set the minimum height and width of the main stage
            primaryStage.setMinHeight(600);
            primaryStage.setMinWidth(800);
    
            // Show the stage
            primaryStage.show();
        } catch (IOException e) {
            // cant load FXML
            e.printStackTrace();
        }
    }

    /**
     * Main method of the program that calls {@code launch} inherited from the
     * Application class
     *
     * @param args
     */
    public static void main(String[] args) {
        launch(args);
    }
}