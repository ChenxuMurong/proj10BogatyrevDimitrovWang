/*
 * File: Controller.java
 * Names: Philipp Bogatyrev, Anton Dimitrov, Baron Wang
 * Class: CS 361
 * Project 6
 * Date: March 18
 */

import java.io.*;
import java.util.HashMap;
import java.util.Optional;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import javafx.application.Platform;
import javafx.beans.binding.Bindings;
import javafx.beans.binding.BooleanBinding;
import javafx.beans.value.ChangeListener;
import javafx.beans.value.ObservableValue;
import javafx.event.ActionEvent;
import javafx.event.Event;
import javafx.event.EventHandler;
import javafx.fxml.FXML;
import javafx.scene.control.*;
import javafx.scene.control.Button;
import javafx.scene.input.KeyEvent;

import org.fxmisc.flowless.VirtualizedScrollPane;
import org.fxmisc.richtext.CodeArea;
import org.fxmisc.richtext.StyleClassedTextArea;

/**
 * Controller class contains handler methods for buttons and menu items
 * @author Philipp Bogatyrev, Anton Dimitrov, Baron Wang
 *
 */

public class Controller {

    @FXML
    private Button compileButton; // disable when no files

    @FXML
    private Button runButton; // disable when no files

    @FXML
    private Button stopButton; // disable when not running a file

    @FXML
    private TabPane tabPane; // tabPane in the window

    @FXML
    private Tab initialTab; // the initial tab

    @FXML
    private VirtualizedScrollPane<StyleClassedTextArea> consoleScrollPane;

    @FXML
    private StyleClassedTextArea console; // console textarea

    @FXML
    private MenuItem undoMI, redoMI; // menu items

    @FXML
    private MenuItem selectAllMI, cutMI, copyMI, pasteMI; // menu items

    @FXML
    private MenuItem saveMI, saveAsMI, closeMI; // menu items

    @FXML
    private ComboBox langBox;

    private FileMenuController fileController = new FileMenuController();
    // List of saved tabs and their content
    private HashMap<Tab, String> savedContents = new HashMap<>();
    // List of saved tabs and their saving path
    private HashMap<Tab, String> savedPaths = new HashMap<>();
    // Keep track of the id for new tabs created
    private int newTabID = 1;
    // Fields for managing compiler and console processes
    private ExecutorService executor = Executors.newSingleThreadExecutor();
    private ProcessBuilder processBuilder = new ProcessBuilder();
    private boolean cancel_compiler = false;
    private String outStreamCommand = "";

    /**
     * Initialize the first tab so that VirtualizedScrollPanes hold CodeArea in the tab
     */
    public void initializeFirstTab() {
        // disable compile, run, and stop buttons
        runButton.setDisable(true);
        compileButton.setDisable(true);
        stopButton.setDisable(true);

        // default code area: java
        JavaCodeArea javaCodeArea = new JavaCodeArea();
        CodeArea codeArea = javaCodeArea.getCodeArea();
        initialTab.setContent(new VirtualizedScrollPane<>(codeArea));

        compileButton.disableProperty().bind(noTabs());
        runButton.disableProperty().bind(noTabs());
    }

    /**
     * 1. Save dialog for unsaved files
     * 2. compiles the file currently open (unless cancelled)
     *
     * @param event ActionEvent related to javafx
    */
    @FXML
    void handleCompileButton(ActionEvent event) {
        cancel_compiler = false;
        if (!this.fileController.selectedTabIsSaved(tabPane)) {
            // prompts user to save before proceeding to compile
            Dialog dialog = DialogOptions.getUnsavedChangesDialog(
                    getSelectedTab().getText(), "compile");

            Optional<ButtonType> result = dialog.showAndWait();
            // Call handleSave() if user chooses YES
            if (result.get() == ButtonType.YES) {
                this.handleSave(event);
            }
            // Quit the process if user chooses CANCEL
            else if (result.get() == ButtonType.CANCEL) {
                cancel_compiler = true;
                return;
            }
        }

        if (!this.fileController.getSavedPaths().containsKey(getSelectedTab())){
            // if no file corresponding to this tab exists in savedPaths hashmap,
            // stop compiling and alert user
            cancel_compiler = true;
            Alert compileBeforeSaveAlert = new Alert(Alert.AlertType.ERROR);
            compileBeforeSaveAlert.setHeaderText("Compilation Unsuccessful");
            compileBeforeSaveAlert.setContentText("Target file does not exist. Please" +
                    " check if this tab has been saved before trying again.");
            compileBeforeSaveAlert.show();
            return;
        }


        try {
            File savedFile = new File(this.fileController.getSavedPaths().get(getSelectedTab()));

            // t.join() blocks thread until process ends.
            // Hangs GUI until current command/compile finishes
            Thread t;
            t = runProcess("cd " + savedFile.getPath().replace(savedFile.getName(),""));
            t.join();

            console.replaceText(
                "cd " + savedFile.getPath().replace(savedFile.getName(),"") 
                + System.lineSeparator());
            System.out.println("**********");
            t = runProcess("javac " + savedFile.getPath());
            t.join();

        } catch (Exception e) {
            DialogOptions.exceptionAlert(e);
        }
    }

    /**
     * Handler method for stop button
     *
     * Stop the compilation when clicked
     *
     * @param event ActionEvent related to JavaFX
     */
    @FXML
    void handleStopButton(ActionEvent event){
        executor.shutdownNow();
        console.clear();
        console.appendText("Process Interrupted");
        stopButton.setDisable(true);
    }

    /**
     * Handler method for Run button
     *
     * First compiles code then runs code
     * Terminal Output redirected to console
     *
     * @param event ActionEvent related to JavaFX
     */
    @FXML
    void handleRunButton(ActionEvent event) {
        if (getSelectedTab() == null) return;


        if(! cancel_compiler){
            File savedFile = new File(this.fileController.getSavedPaths().get(getSelectedTab()));
            switch(langBox.getSelectionModel().getSelectedItem().toString()){
                case "Java":
                    try {
                        handleCompileButton(event);
                        // prepare classpath location
                        int endOfPath =
                                savedFile.getPath().length() - savedFile.getName().length();

                        String pathWithoutFile = savedFile.getPath().substring(0, endOfPath);

                        String fileWithoutExtension =
                                savedFile.getName().substring(0,savedFile.getName().length()-5);

                        console.appendText(
                                "java " + "-cp " + pathWithoutFile + " "
                                        + fileWithoutExtension + "\n");
                        runProcess("java " + "-cp " + pathWithoutFile + " " + fileWithoutExtension);

                    } catch (Exception e) {
                        Alert alertBox = new Alert(Alert.AlertType.ERROR);
                        alertBox.setHeaderText("Process Interrupted");
                        alertBox.setContentText(e.toString());
                        alertBox.show();
                    }
                case "Python":
                    try {
                        // prepare classpath location
                        int endOfPath =
                                savedFile.getPath().length() - savedFile.getName().length();

                        String pathWithoutFile = savedFile.getPath().substring(0, endOfPath);

                        String fileWithoutExtension =
                                savedFile.getName().substring(0,savedFile.getName().length()-3);

                        console.appendText(
                                "python3 " + pathWithoutFile +
                                        savedFile.getName() + "\n");
                        runProcess("python3 " + pathWithoutFile
                                + savedFile.getName() + "\n");

                    } catch (Exception e) {
                        Alert alertBox = new Alert(Alert.AlertType.ERROR);
                        alertBox.setHeaderText("Process Interrupted");
                        alertBox.setContentText(e.toString());
                        alertBox.show();
                    }
            }

        }
    }

    /**
     * public method that calls handleExit() and can be accessed
     * by the Main class
     */
    public void handleWindowExit() {
        handleExit(new ActionEvent());
    }

    /**
     * Sets up listeners to disable/enable menu items +
     * connects existing exit buttons to the created close / exit MenuItems
     */
    @FXML
    private void initialize() {
        // handles clicking "x" for initial tab and primary window

        tabPane.getTabs().get(0).setOnCloseRequest(new EventHandler<Event>() {
            @Override
            public void handle(Event event) {
                tabPane.getSelectionModel().select(0);
                handleClose(new ActionEvent());
                event.consume();
            }
        });

        langBox.getSelectionModel().selectedItemProperty().addListener(new ChangeListener() {
            @Override
            public void changed(ObservableValue observable, Object oldValue, Object newValue) {
                if (oldValue != newValue){
                    CodeArea oldArea;
                    VirtualizedScrollPane vsp;
                    switch(langBox.getSelectionModel().getSelectedItem().toString()){
                        case "Java":
                            compileButton.disableProperty().bind(noTabs());
                            JavaCodeArea javaCodeArea = new JavaCodeArea();
                            vsp = (VirtualizedScrollPane) getSelectedTab().getContent();
                            oldArea = (CodeArea) vsp.getContent();
                            javaCodeArea.getCodeArea().replaceText(oldArea.getText());
                            getSelectedTab().setContent(new VirtualizedScrollPane<>(javaCodeArea.getCodeArea()));
                            break;
                        case "Python":
                            compileButton.disableProperty().unbind();
                            compileButton.setDisable(true);
                            PythonCodeArea pythonCodeArea = new PythonCodeArea();
                            vsp = (VirtualizedScrollPane) getSelectedTab().getContent();
                            oldArea = (CodeArea) vsp.getContent();
                            pythonCodeArea.getCodeArea().replaceText(oldArea.getText());
                            getSelectedTab().setContent(new VirtualizedScrollPane<>(pythonCodeArea.getCodeArea()));
                            break;
                    }
                }
            }
        });

        // set disable property when no tabs are open
        closeMI.disableProperty().bind(noTabs());
        saveMI.disableProperty().bind(noTabs());
        saveAsMI.disableProperty().bind(noTabs());
        undoMI.disableProperty().bind(noTabs());
        redoMI.disableProperty().bind(noTabs());
        selectAllMI.disableProperty().bind(noTabs());
        cutMI.disableProperty().bind(noTabs());
        copyMI.disableProperty().bind(noTabs());
        pasteMI.disableProperty().bind(noTabs());
    }

    /**
     * returns a new BooleanBinding that holds true if
     * there is no tab in TabPane
     */
    private BooleanBinding noTabs() {
        return Bindings.isEmpty(tabPane.getTabs());
    }

    /**
     * helper function to get the currently selected tab in tabPane
     *
     * @return Tab the selected tab
     */
    private Tab getSelectedTab() {
        return tabPane.getSelectionModel().getSelectedItem();
    }

    /**
     * helper function to get the text box in the selected tab
     *
     * @return TextArea the text box in the selected tab
     */
    private CodeArea getSelectedTextBox() {
        Tab currentTab = getSelectedTab();
        VirtualizedScrollPane scrollPane;
        scrollPane = (VirtualizedScrollPane) currentTab.getContent();
        return (CodeArea) scrollPane.getContent();
    }

    /**
     * Handler method for menu bar item About. It shows a dialog that contains
     * program information.
     *
     * @param event An ActionEvent object that gives information about the event
     *              and its source.
     */
    @FXML
    private void handleAbout(ActionEvent event) {
        Dialog dialog = DialogOptions.getAboutDialog();
        dialog.showAndWait();
    }

    /**
     * Handler method for menu bar item New. It creates a new tab and adds it
     * to the tabPane.
     *
     * @param event An ActionEvent object that gives information about the event
     *              and its source.
     */
    @FXML
    private void handleNew(ActionEvent event) {
        this.fileController.handleNew(event, tabPane);
    }

    /**
     * Handler method for menu bar item Open. It shows a dialog and let the user
     * select a text file to be loaded to the text box.
     *
     * @param event An ActionEvent object that gives information about the event
     *              and its source.
     */
    @FXML
    private void handleOpen(ActionEvent event) {
        this.fileController.handleOpen(event, tabPane);
    }

    /**
     * Handler method for menu bar item Close. It creates a dialog if
     * the selected tab is unsaved and closes the tab.
     *
     * @param event An ActionEvent object that gives information about the event
     *              and its source.
     */
    @FXML
    public void handleClose(ActionEvent event) {
        // call helper method
        this.fileController.closeSelectedTab(event, "close", tabPane);
    }

    /**
     * Handler method for menu bar item Exit. When exit item of the menu
     * bar is clicked, the application quits if all tabs in the tabPane are
     * closed properly.
     *
     * @param event An ActionEvent object that gives information about the event
     *              and its source.
     */
    @FXML
    private void handleExit(ActionEvent event) {
        this.fileController.handleExit(event, tabPane);

    }



    /**
     * Handler method for menu bar item Save. Behaves like Save as... if the text
     * has never been saved before. Otherwise, save the text to its corresponding
     * text file.
     *
     * @param event An ActionEvent object that gives information about the event
     *              and its source.
     */
    @FXML
    private void handleSave(ActionEvent event) {
        this.fileController.handleSave(event, tabPane);
    }

    /**
     * Handler method for menu bar item Save as.... a dialog appears
     * in which the user is asked for the name of the file into which the
     * contents of the current text area are to be saved.
     *
     * @param event An ActionEvent object that gives information about the event
     *              and its source.
     */
    @FXML
    private void handleSaveAs(ActionEvent event) {
        this.fileController.handleSaveAs(event, tabPane);
    }

    /**
     * Handler method for menu bar item Undo.
     *
     * @param event An ActionEvent object that gives information about the event
     *              and its source.
     */
    @FXML
    private void handleUndo(ActionEvent event) {
        getSelectedTextBox().undo();
    }

    /**
     * Handler method for menu bar item Redo.
     *
     * @param event An ActionEvent object that gives information about the event
     *              and its source.
     */
    @FXML
    private void handleRedo(ActionEvent event) {
        getSelectedTextBox().redo();
    }

    /**
     * Handler method for menu bar item Cut.
     *
     * @param event An ActionEvent object that gives information about the event
     *              and its source.
     */
    @FXML
    private void handleCut(ActionEvent event) {
        getSelectedTextBox().cut();
    }

    /**
     * Handler method for menu bar item Copy.
     *
     * @param event An ActionEvent object that gives information about the event
     *              and its source.
     */
    @FXML
    private void handleCopy(ActionEvent event) {
        getSelectedTextBox().copy();
    }

    /**
     * Handler method for menu bar item Paste.
     *
     * @param event An ActionEvent object that gives information about the event
     *              and its source.
     */
    @FXML
    private void handlePaste(ActionEvent event) {
        getSelectedTextBox().paste();
    }

    /**
     * Handler method for menu bar item Select all.
     *
     * @param event An ActionEvent object that gives information about the event
     *              and its source.
     */
    @FXML
    private void handleSelectAll(ActionEvent event) {
        getSelectedTextBox().selectAll();
    }

    /**
     * helper method that starts processes in new thread and
     * reads terminal output
     *
     * @param command The command of the process being run
     * @throws Exception
     */
    private Thread runProcess(String command) throws Exception {
        processBuilder.command("bash", "-c", command).redirectErrorStream(true);
        boolean compilingCommand = command.startsWith("javac");
        // new thread to read process results
        Thread t = new Thread(new Runnable() {
            // start process and read from input stream
            public void run(){
                stopButton.setDisable(false);
                boolean compilerError = false;
                Process process;
                try {
                    process = processBuilder.start();
                    OutputStream outStream = process.getOutputStream();
                    // output stream
                    console.setOnKeyTyped(new EventHandler<>(){  
                        String result = "";  
                        @Override
                        public void handle(KeyEvent event) {
                            result += event.getCharacter();
                            if (event.getCharacter().equals("\r")) {
                                try {
                                    for (char c : result.toCharArray()) {
                                        outStream.write(c);
                                    }
                                    outStream.flush();
                                    result = "";
                                } catch (Exception e) {
                                    DialogOptions.exceptionAlert(e);
                                }
                            }
                        }
                    });
                    while(process.isAlive()){
                        // input stream
                        Reader reader = new InputStreamReader(process.getInputStream());
                        // buffer for efficiency
                        Reader buffer = new BufferedReader(reader);
                        int r;
                        while ((r = buffer.read()) != -1) {
                            compilerError = true;
                            char ch = (char) r;
                            String s = Character.toString(ch);
                                Platform.runLater(new Runnable() {
                                    @Override
                                    public void run() {
                                        console.appendText(s);
                                    }
                                });
                        }
                    }
                    if(compilingCommand){
                        if(!compilerError){
                            Platform.runLater(new Runnable() {
                                @Override
                                public void run() {
                                console.appendText("Compilation Was Succesful!\n");
                                }
                            });
                        }//compilerError
                    } //compilingCommand
                } catch (Exception e) {
                    DialogOptions.exceptionAlert(e);
                }
            }// run
        });// runnable

        t.start();
        stopButton.setDisable(true);
        return t;
    }

}
