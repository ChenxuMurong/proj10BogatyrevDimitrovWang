import java.io.*;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Optional;

import javafx.event.ActionEvent;
import javafx.event.Event;
import javafx.event.EventHandler;
import javafx.fxml.FXML;
import javafx.scene.control.*;
import javafx.stage.FileChooser;

import org.fxmisc.flowless.VirtualizedScrollPane;
import org.fxmisc.richtext.CodeArea;


public class FileMenuController {

    // List of saved tabs and their content
    private HashMap<Tab, String> savedContents = new HashMap<>();
    // List of saved tabs and their saving path
    private HashMap<Tab, String> savedPaths = new HashMap<>();
    // Keep track of the id for new tabs created
    private int newTabID = 1;


    public HashMap<Tab, String> getSavedContents(){
        return this.savedContents;
    }

    public HashMap<Tab, String> getSavedPaths(){
        return this.savedPaths;
    }


    public void handleOpen(ActionEvent event, TabPane tabPane) {
        // create a new file chooser
        FileChooser fileChooser = new FileChooser();
        File initialDir = new File("./saved");
        // handles the case if ./saved directory does not exist
        if (!initialDir.exists()) {
            initialDir = new File("./");
        }
        fileChooser.setInitialDirectory(initialDir);
        List<String> extensionList = Arrays.asList(new String[]{"*.txt", "*.fxml", "*.css", "*.java", "*.py"});
        fileChooser.getExtensionFilters().addAll(
                new FileChooser.ExtensionFilter("Text Files", extensionList),
                new FileChooser.ExtensionFilter("FXML Files", extensionList),
                new FileChooser.ExtensionFilter("CSS Files", extensionList),
                new FileChooser.ExtensionFilter("Java Files", extensionList),
                new FileChooser.ExtensionFilter("Python Files", extensionList));


        File selectedFile = fileChooser.showOpenDialog(tabPane.getScene().getWindow());
        // if user selects a file (instead of pressing cancel button
        if (selectedFile != null) {
            // open a new tab
            this.handleNew(event, tabPane);
            // set text/name of the tab to the filename
            this.getSelectedTab(tabPane).setText(selectedFile.getName());
            Tooltip t = new Tooltip(selectedFile.getPath());
            this.getSelectedTab(tabPane).setTooltip(t);
            this.newTabID--; // no need to increment
            try {
                // reads the file content to a String
                String content = new String(Files.readAllBytes(
                        Paths.get(selectedFile.getPath())));
                this.getSelectedTextBox(tabPane).replaceText(content);
                // update savedContents field
                this.savedContents.put(getSelectedTab(tabPane), content);
                this.savedPaths.put(getSelectedTab(tabPane), selectedFile.getPath());

                //getSelectedTextBox().setFile(File(selectedFile.getPath()));

            } catch (Exception e) {
                DialogOptions.exceptionAlert(e);
            }
        }
    }

    /**
     * helper function to get the currently selected tab in tabPane
     *
     * @return Tab the selected tab
     */
    private Tab getSelectedTab(TabPane tabPane) {
        return tabPane.getSelectionModel().getSelectedItem();
    }



    /**
     * helper function to get the text box in the selected tab
     *
     * @return TextArea the text box in the selected tab
     */
    private CodeArea getSelectedTextBox(TabPane tabPane) {
        Tab currentTab = getSelectedTab(tabPane);
        VirtualizedScrollPane scrollPane;
        scrollPane = (VirtualizedScrollPane) currentTab.getContent();
        return (CodeArea) scrollPane.getContent();
    }

    /**
     * Helper method that checks if the text in the selected tab is saved.
     *
     * @return boolean whether the text in the selected tab is saved.
     */
    public boolean selectedTabIsSaved(TabPane tabPane) {
        // if tab name has been changed and current text matches with
        // the saved text in record
        if (this.savedContents.containsKey(getSelectedTab(tabPane)) &&
                getSelectedTextBox(tabPane).getText().equals(
                        this.savedContents.get(getSelectedTab(tabPane)))) {
            return true;
        }
        return false;
    }


    /**
     * Helper method that handles unsaved text and closes the tab. Shows a dialog
     * if the text gets modified since its last save or has never been saved.
     * Closes the tab if the text has been saved or user confirms.
     *
     * @param event   An ActionEvent object that gives information about the event
     *                and its source.
     * @param exiting a string used in Dialog modification. represents if the
     *                program
     *                is exiting or simply closing a single tab
     *
     * @return Optional the Optional object returned by dialog.showAndWait().
     *         returns null if tab text is already saved.
     */
    public Optional<ButtonType> closeSelectedTab(ActionEvent event,
                                                 String exiting, TabPane tabPane) {
        // if content is not saved
        if (!selectedTabIsSaved(tabPane)) {
            Dialog dialog = DialogOptions.getUnsavedChangesDialog(
                    getSelectedTab(tabPane).getText(), exiting);

            Optional<ButtonType> result = dialog.showAndWait();
            // call handleSave() if user chooses YES
            if (result.get() == ButtonType.YES) {
                this.handleSave(event, tabPane);
                // keep the tab if the save is unsuccessful (eg. canceled)
                if (!this.selectedTabIsSaved(tabPane)) {
                    return result;
                }
            }
            // quit the dialog and keep selected tab if user chooses CANCEL
            else if (result.get() == ButtonType.CANCEL) {
                return result;
            }
        }
        // remove tab from tabPane if text is saved or user chooses NO
        this.savedContents.remove(getSelectedTab(tabPane));
        this.savedPaths.remove(getSelectedTab(tabPane));
        tabPane.getTabs().remove(getSelectedTab(tabPane));
        return Optional.empty();
    }


    public void handleNew(ActionEvent event, TabPane tabPane) {
        // create a new tab
        this.newTabID++;
        Tab newTab = new Tab("Untitled-" + this.newTabID);
        newTab.setOnCloseRequest(new EventHandler<Event>() {
            @Override
            public void handle(Event event) {
                handleClose(new ActionEvent(), tabPane);
                event.consume();
            }
        });

        // create a code area
        JavaCodeArea javaCodeArea = new JavaCodeArea();
        CodeArea codeArea = javaCodeArea.getCodeArea();
        newTab.setContent(new VirtualizedScrollPane<>(codeArea));
        // add new tab to the tabPane
        tabPane.getTabs().add(newTab);
        // make the newly created tab the topmost
        tabPane.getSelectionModel().selectLast();
        newTab.setOnCloseRequest(new EventHandler<Event>() {
            @Override
            public void handle(Event event) {
                tabPane.getSelectionModel().select(newTab);
                handleClose(new ActionEvent(), tabPane);
                event.consume();
            }
        });
    }


    /**
     * Handler method for menu bar item Save. Behaves like Save as... if the text
     * has never been saved before. Otherwise, save the text to its corresponding
     * text file.
     *
     * @param event An ActionEvent object that gives information about the event
     *              and its source.
     */
    public void handleSave(ActionEvent event, TabPane tabPane) {
        // if the text has been saved before
        if (this.savedContents.containsKey(getSelectedTab(tabPane))) {
            // create a File object for the corresponding text file
            File savedFile = new File(this.savedPaths.get(getSelectedTab(tabPane)));
            try {
                // write the new content to the text file
                FileWriter fw = new FileWriter(savedFile);
                fw.write(getSelectedTextBox(tabPane).getText());
                fw.close();
                // update savedContents field
                this.savedContents.put(getSelectedTab(tabPane), getSelectedTextBox(tabPane).getText());
            } catch (Exception e) {
                DialogOptions.exceptionAlert(e);
            }
        }
        // if text in selected tab was not loaded from a file nor ever saved to a file
        else {
            this.handleSaveAs(event, tabPane);
        }
    }

    /**
     * Handler method for menu bar item Close. It creates a dialog if
     * the selected tab is unsaved and closes the tab.
     *
     * @param event An ActionEvent object that gives information about the event
     *              and its source.
     */
    @FXML
    public void handleClose(ActionEvent event, TabPane tabPane) {
        // call helper method
        this.closeSelectedTab(event, "close", tabPane);
    }

    /**
     * Handler method for menu bar item Save as.... a dialog appears
     * in which the user is asked for the name of the file into which the
     * contents of the current text area are to be saved.
     *
     * @param event An ActionEvent object that gives information about the event
     *              and its source.
     */
    public void handleSaveAs(ActionEvent event, TabPane tabPane) {
        // create a new fileChooser
        FileChooser fileChooser = new FileChooser();
        List<String> extensionList = Arrays.asList(new String[]{"*.txt", "*.fxml", "*.css", "*.java", "*.py"});
        fileChooser.getExtensionFilters().addAll(
                new FileChooser.ExtensionFilter("Text Files", extensionList),
                new FileChooser.ExtensionFilter("FXML Files", extensionList),
                new FileChooser.ExtensionFilter("CSS Files", extensionList),
                new FileChooser.ExtensionFilter("Java Files", extensionList),
                new FileChooser.ExtensionFilter("Python Files", extensionList));





        File fileToSave = fileChooser.showSaveDialog(tabPane.getScene().getWindow());
        // if user did not choose CANCEL
        if (fileToSave != null) {
            try {
                // save file
                FileWriter fw = new FileWriter(fileToSave);
                fw.write(this.getSelectedTextBox(tabPane).getText());
                fw.close();
                // update savedContents field and tab text
                this.savedContents.put(getSelectedTab(tabPane), getSelectedTextBox(tabPane).getText());
                this.savedPaths.put(getSelectedTab(tabPane), fileToSave.getPath());
                this.getSelectedTab(tabPane).setText(fileToSave.getName());
                Tooltip t = new Tooltip(fileToSave.getPath());
                this.getSelectedTab(tabPane).setTooltip(t);
            } catch (Exception e) {
                DialogOptions.exceptionAlert(e);
            }
        }
    }
    /**
     * Handler method for menu bar item Exit. When exit item of the menu
     * bar is clicked, the application quits if all tabs in the tabPane are
     * closed properly.
     *
     * @param event An ActionEvent object that gives information about the event
     *              and its source.
     */
    public void handleExit(ActionEvent event, TabPane tabPane) {
        // start while loop iteration from the last tab
        tabPane.getSelectionModel().selectLast();
        // loop through the tabs in tabPane
        while (tabPane.getTabs().size() > 0) {
            // try close the currently selected tab
            Optional<ButtonType> result = closeSelectedTab(event, "exit", tabPane);
            // if the user chooses Cancel at any time, then the exiting is canceled,
            // and the application stays running.
            if (result.isPresent() && result.get() == ButtonType.CANCEL) {
                return;
            }
        }
        // exit if all tabs are closed
        System.exit(0);
    }
}
