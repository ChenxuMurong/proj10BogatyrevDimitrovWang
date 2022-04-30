/*
 * File: Controller.java
 * Names: Philipp Bogatyrev, Anton Dimitrov, Baron Wang
 * Class: CS 361
 * Project 6
 * Date: March 18
 */


import javafx.scene.control.*;
import javafx.scene.layout.GridPane;
import javafx.scene.layout.Priority;

import java.io.PrintWriter;
import java.io.StringWriter;

/**
 * Collection of prebuilt Dialog options
 */
public class DialogOptions {

    /**
     * Helper method to display error message to user when an exception is thrown
     *
     * @type this type is default since main need to use it to print possible exception message
     */
    public static void exceptionAlert(Exception ex){
        StringWriter sw = new StringWriter();
        PrintWriter pw = new PrintWriter(sw);
        ex.printStackTrace(pw);
        pw.close();
        String exceptionText = sw.toString();

        Alert alert = new Alert(Alert.AlertType.ERROR);
        alert.setTitle("Exception Alert");
        alert.setHeaderText(exceptionText.split("\\s+")[0]);
        alert.setContentText("An exception has been thrown.");
        Label label = new Label("The exception stacktrace is:");

        TextArea textArea = new TextArea(exceptionText);
        textArea.setEditable(false);
        textArea.setWrapText(true);

        textArea.setMaxWidth(Double.MAX_VALUE);
        textArea.setMaxHeight(Double.MAX_VALUE);
        GridPane.setVgrow(textArea, Priority.ALWAYS);
        GridPane.setHgrow(textArea, Priority.ALWAYS);

        GridPane expContent = new GridPane();
        expContent.setMaxWidth(Double.MAX_VALUE);
        expContent.add(label, 0, 0);
        expContent.add(textArea, 0, 1);

        alert.getDialogPane().setExpandableContent(expContent);

        alert.showAndWait();
    }

    /***
     * Dialog box for about menuitem in IDE GUI
     * 
     * @return a Dialog window
     */
    public static Dialog getAboutDialog(){
        // create a new dialog
        Dialog dialog = new Dialog();
        dialog.setContentText("This is a code editor! \n\n "
                + "Authors: Anton Dimitrov, Philipp Bogatyrev, Baron Wang");
        // add a close button so that dialog closing rule is fulfilled
        dialog.getDialogPane().getButtonTypes().add(ButtonType.CLOSE);
        return dialog;
    }

    /**
     * Unsaved changes dialog used when compiling, exiting, and
     * closing tabs
     * 
     * @param fileName the file name that is unsaved
     * @param purpose a string explaining why the dialog exists
     * @return a Dialog window to handle button events
     */
    public static Dialog getUnsavedChangesDialog(String fileName, String purpose){
        Dialog dialog = new Dialog();
        // set the prompt text depending on exiting or closing
        String promptText;
        if ("exit".equals(purpose)) {
            promptText = String.format(
                    "Do you want to save %s before exiting?",
                    fileName);
        } else if("close".equals(purpose)){
            promptText = String.format(
                    "Do you want to save %s before closing it?",
                    fileName);
        } else{ //if("compile".equals(purpose)){
            promptText = String.format(
                    "Do you want to save %s before compiling it?",
                    fileName);
        }
        dialog.setContentText(promptText);
        // add Yes, No, Cancel button
        dialog.getDialogPane().getButtonTypes().addAll(
                ButtonType.YES, ButtonType.NO, ButtonType.CANCEL);
        return dialog;
    }
}
