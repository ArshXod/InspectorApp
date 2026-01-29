package com.inspector.gui.controller;

import com.inspector.core.ApplicationConnector;
import com.inspector.core.AttachConnector;
import com.inspector.gui.model.InspectionHistory;
import com.inspector.gui.model.ProcessInfo;
import com.inspector.overlay.ComponentBoundsParser;
import com.inspector.overlay.ComponentHighlighter;
import com.inspector.util.WindowEnumerator;
import com.sun.jna.Native;
import com.sun.jna.platform.win32.User32;
import com.sun.jna.platform.win32.WinDef.HWND;
import com.sun.jna.ptr.IntByReference;
import javafx.animation.KeyFrame;
import javafx.animation.Timeline;
import javafx.application.Platform;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.collections.transformation.FilteredList;
import javafx.concurrent.Task;
import javafx.fxml.FXML;
import javafx.scene.control.*;
import javafx.scene.control.cell.PropertyValueFactory;
import javafx.stage.FileChooser;
import javafx.stage.Stage;
import javafx.util.Duration;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.StandardCopyOption;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.*;

public class MainController {

    @FXML private TableView<ProcessInfo> processTable;
    @FXML private TableColumn<ProcessInfo, Integer> pidColumn;
    @FXML private TableColumn<ProcessInfo, String> nameColumn;
    @FXML private TableColumn<ProcessInfo, String> titleColumn;
    @FXML private TableColumn<ProcessInfo, String> typeColumn;
    
    @FXML private TextField searchField;
    @FXML private ComboBox<String> filterComboBox;
    @FXML private Button refreshButton;
    @FXML private Button inspectButton;
    @FXML private CheckBox autoRefreshCheckBox;
    @FXML private ToggleButton darkModeToggle;
    
    @FXML private ProgressBar progressBar;
    @FXML private Label statusLabel;
    @FXML private TextArea previewTextArea;
    
    @FXML private TableView<InspectionHistory.HistoryEntry> historyTable;
    @FXML private TableColumn<InspectionHistory.HistoryEntry, Integer> historyPidColumn;
    @FXML private TableColumn<InspectionHistory.HistoryEntry, String> historyNameColumn;
    @FXML private TableColumn<InspectionHistory.HistoryEntry, LocalDateTime> historyTimeColumn;
    @FXML private Button clearHistoryButton;
    @FXML private Button openFileButton;
    @FXML private ToggleButton highlightToggle;
    
    private ObservableList<ProcessInfo> processList;
    private FilteredList<ProcessInfo> filteredProcessList;
    private InspectionHistory history;
    private Timer autoRefreshTimer;
    private Timeline loadingAnimation;
    private int animationFrame = 0;
    private Task<String> currentInspectionTask;
    private javafx.beans.property.BooleanProperty isInspecting = new javafx.beans.property.SimpleBooleanProperty(false);
    private ComponentHighlighter highlighter = new ComponentHighlighter();
    
    @FXML
    public void initialize() {
        history = new InspectionHistory();
        
        // Setup process table
        pidColumn.setCellValueFactory(new PropertyValueFactory<>("pid"));
        nameColumn.setCellValueFactory(new PropertyValueFactory<>("name"));
        titleColumn.setCellValueFactory(new PropertyValueFactory<>("windowTitle"));
        typeColumn.setCellValueFactory(new PropertyValueFactory<>("type"));
        
        processList = FXCollections.observableArrayList();
        filteredProcessList = new FilteredList<>(processList, p -> true);
        processTable.setItems(filteredProcessList);
        
        // Setup history table
        historyPidColumn.setCellValueFactory(cellData -> 
            new javafx.beans.property.SimpleIntegerProperty(cellData.getValue().pid).asObject());
        historyNameColumn.setCellValueFactory(cellData -> 
            new javafx.beans.property.SimpleStringProperty(cellData.getValue().processName));
        historyTimeColumn.setCellValueFactory(cellData -> 
            new javafx.beans.property.SimpleObjectProperty<>(cellData.getValue().timestamp));
        historyTimeColumn.setCellFactory(column -> new TableCell<InspectionHistory.HistoryEntry, LocalDateTime>() {
            @Override
            protected void updateItem(LocalDateTime item, boolean empty) {
                super.updateItem(item, empty);
                if (empty || item == null) {
                    setText(null);
                } else {
                    setText(item.format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss")));
                }
            }
        });
        
        refreshHistoryTable();
        
        // Setup filter combo box
        filterComboBox.setItems(FXCollections.observableArrayList("All", "Java Only", "Non-Java Only"));
        filterComboBox.setValue("All");
        
        // Search and filter listeners
        searchField.textProperty().addListener((obs, oldVal, newVal) -> applyFilters());
        filterComboBox.valueProperty().addListener((obs, oldVal, newVal) -> applyFilters());
        
        // Disable inspect button if no selection OR if inspection is running
        inspectButton.disableProperty().bind(
            processTable.getSelectionModel().selectedItemProperty().isNull().or(isInspecting)
        );
        openFileButton.disableProperty().bind(historyTable.getSelectionModel().selectedItemProperty().isNull());
        
        // Auto-stop highlighter when window closes
        Platform.runLater(() -> {
            if (inspectButton.getScene() != null && inspectButton.getScene().getWindow() != null) {
                inspectButton.getScene().getWindow().setOnCloseRequest(e -> {
                    if (highlighter != null) {
                        highlighter.stop();
                    }
                });
            }
        });
        
        // Load initial process list
        refreshProcessList();
        
        // Setup auto-refresh
        autoRefreshCheckBox.selectedProperty().addListener((obs, oldVal, newVal) -> {
            if (newVal) {
                startAutoRefresh();
            } else {
                stopAutoRefresh();
            }
        });
    }
    
    @FXML
    private void handleRefresh() {
        refreshProcessList();
    }
    
    @FXML
    private void handleInspect() {
        ProcessInfo selected = processTable.getSelectionModel().getSelectedItem();
        if (selected == null) return;
        
        // Show file chooser
        FileChooser fileChooser = new FileChooser();
        fileChooser.setTitle("Save Inspection Results");
        fileChooser.setInitialFileName(String.format("%s_%d_%s.json", 
            selected.getName().replaceAll("[^a-zA-Z0-9]", "_"),
            selected.getPid(),
            LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyyMMdd_HHmmss"))));
        fileChooser.getExtensionFilters().add(
            new FileChooser.ExtensionFilter("JSON Files", "*.json"));
        
        Stage stage = (Stage) inspectButton.getScene().getWindow();
        File file = fileChooser.showSaveDialog(stage);
        
        if (file != null) {
            performInspection(selected, file);
        }
    }
    
    private void performInspection(ProcessInfo processInfo, File outputFile) {
        // Start loading animation
        startLoadingAnimation();
        
        // Clear previous preview
        previewTextArea.clear();
        
        currentInspectionTask = new Task<String>() {
            @Override
            protected String call() throws Exception {
                try {
                    Platform.runLater(() -> statusLabel.setText("🔍 Connecting to process " + processInfo.getPid() + "..."));
                    updateProgress(0.1, 1.0);
                    Thread.sleep(200); // Brief pause for visual feedback
                    
                    Platform.runLater(() -> statusLabel.setText("🔌 Detecting process type..."));
                    updateProgress(0.2, 1.0);
                    
                    boolean isJava = AttachConnector.isJavaProcess(String.valueOf(processInfo.getPid()));
                    String method = isJava ? "Java Attach API + Agent" : "Enhanced Windows UIA";
                    
                    Platform.runLater(() -> statusLabel.setText("⚙️ Using " + method + "..."));
                    updateProgress(0.3, 1.0);
                    
                    // Update preview with real-time status
                    Platform.runLater(() -> {
                        previewTextArea.setText(
                            "╔═══════════════════════════════════════════════╗\n" +
                            "║       INSPECTION IN PROGRESS                  ║\n" +
                            "╚═══════════════════════════════════════════════╝\n\n" +
                            "Process ID:      " + processInfo.getPid() + "\n" +
                            "Process Name:    " + processInfo.getName() + "\n" +
                            "Window Title:    " + processInfo.getWindowTitle() + "\n" +
                            "Method:          " + method + "\n\n" +
                            "Status: Analyzing UI structure...\n" +
                            "Please wait while we traverse the component tree...\n"
                        );
                    });
                    
                    Platform.runLater(() -> statusLabel.setText("🔎 Traversing UI component tree..."));
                    updateProgress(0.5, 1.0);
                    
                    // Perform inspection
                    System.out.println("[DEBUG] Starting inspection for PID: " + processInfo.getPid());
                    ApplicationConnector.ConnectionResult result = 
                        ApplicationConnector.connectByPid(processInfo.getPid());
                    System.out.println("[DEBUG] Inspection completed. Error: " + result.error);
                    
                    Platform.runLater(() -> statusLabel.setText("📊 Collecting control metadata..."));
                    updateProgress(0.7, 1.0);
                    
                    Platform.runLater(() -> statusLabel.setText("💾 Generating JSON output..."));
                    updateProgress(0.85, 1.0);
                    
                    // Copy temp output to user-selected location
                    File tempOutput = new File("inspector-agent-output.json");
                    System.out.println("[DEBUG] Temp output exists: " + tempOutput.exists() + ", path: " + tempOutput.getAbsolutePath());
                    
                    if (tempOutput.exists()) {
                        System.out.println("[DEBUG] Copying to: " + outputFile.getAbsolutePath());
                        Files.copy(tempOutput.toPath(), outputFile.toPath(), 
                                  StandardCopyOption.REPLACE_EXISTING);
                        System.out.println("[DEBUG] Copy complete. Output file exists: " + outputFile.exists());
                        
                        updateProgress(1.0, 1.0);
                        return outputFile.getAbsolutePath();
                    } else {
                        throw new IOException("Inspection failed: " + 
                            (result.error != null ? result.error : "No output generated"));
                    }
                } catch (Exception e) {
                    System.err.println("[ERROR] Exception in inspection task: " + e.getMessage());
                    e.printStackTrace();
                    throw e;
                }
            }
            
            @Override
            protected void succeeded() {
                System.out.println("[DEBUG] Task succeeded!");
                stopLoadingAnimation();
                String filePath = getValue();
                System.out.println("[DEBUG] File path from task: " + filePath);
                
                // Load preview with formatted display
                try {
                    File resultFile = new File(filePath);
                    System.out.println("[DEBUG] Result file exists: " + resultFile.exists() + ", size: " + resultFile.length());
                    
                    String content = Files.readString(resultFile.toPath());
                    System.out.println("[DEBUG] Content loaded, length: " + content.length());
                    
                    // Parse JSON to extract stats
                    String formattedPreview = formatPreviewContent(content, processInfo);
                    System.out.println("[DEBUG] Formatted preview length: " + formattedPreview.length());
                    
                    previewTextArea.setText(formattedPreview);
                    System.out.println("[DEBUG] Preview text set successfully");
                    
                } catch (IOException e) {
                    System.err.println("[ERROR] Failed to load preview: " + e.getMessage());
                    e.printStackTrace();
                    previewTextArea.setText("❌ Error loading preview: " + e.getMessage());
                }
                
                // Add to history
                System.out.println("[DEBUG] Adding to history...");
                history.addEntry(processInfo.getPid(), processInfo.getName(), 
                               processInfo.getWindowTitle(), processInfo.getType(), filePath);
                processInfo.setLastInspected(LocalDateTime.now());
                refreshHistoryTable();
                System.out.println("[DEBUG] History updated");
                
                // Show success alert
                showSuccessAlert(filePath);
                
                progressBar.setVisible(false);
            }
            
            @Override
            protected void failed() {
                System.err.println("[ERROR] Task failed!");
                Throwable ex = getException();
                System.err.println("[ERROR] Exception: " + ex.getMessage());
                ex.printStackTrace();
                
                stopLoadingAnimation();
                
                previewTextArea.setText(
                    "╔═══════════════════════════════════════════════╗\n" +
                    "║       INSPECTION FAILED                       ║\n" +
                    "╚═══════════════════════════════════════════════╝\n\n" +
                    "Process: " + processInfo.getName() + " (PID: " + processInfo.getPid() + ")\n\n" +
                    "Error Details:\n" +
                    "─────────────────────────────────────────────────\n" +
                    ex.getMessage() + "\n\n" +
                    "Possible Solutions:\n" +
                    "• Ensure the process is still running\n" +
                    "• For Java processes, verify JVM is accessible\n" +
                    "• Try running as Administrator\n" +
                    "• Check if the application has UI components\n"
                );
                
                progressBar.setVisible(false);
                
                Alert alert = new Alert(Alert.AlertType.ERROR);
                alert.setTitle("Inspection Failed");
                alert.setHeaderText("Failed to inspect process");
                alert.setContentText(ex.getMessage());
                alert.showAndWait();
            }
        };
        
        progressBar.progressProperty().bind(currentInspectionTask.progressProperty());
        progressBar.setVisible(true);
        
        // Set inspection flag
        isInspecting.set(true);
        currentInspectionTask.setOnSucceeded(e -> {
            isInspecting.set(false);
            statusLabel.setText("✅ Inspection complete!");
        });
        currentInspectionTask.setOnFailed(e -> {
            isInspecting.set(false);
            statusLabel.setText("❌ Inspection failed");
        });
        
        Thread thread = new Thread(currentInspectionTask);
        thread.setDaemon(true);
        thread.start();
    }
    
    private void startLoadingAnimation() {
        animationFrame = 0;
        loadingAnimation = new Timeline(new KeyFrame(Duration.millis(200), e -> {
            animationFrame = (animationFrame + 1) % 4;
            String dots = "." .repeat(animationFrame + 1);
            String spaces = " ".repeat(3 - animationFrame);
            Platform.runLater(() -> {
                if (previewTextArea.getText().contains("INSPECTION IN PROGRESS")) {
                    String baseText = previewTextArea.getText().split("Status:")[0];
                    previewTextArea.setText(baseText + "Status: Analyzing" + dots + spaces + "\n" +
                        "Please wait while we traverse the component tree...\n");
                }
            });
        }));
        loadingAnimation.setCycleCount(Timeline.INDEFINITE);
        loadingAnimation.play();
    }
    
    private void stopLoadingAnimation() {
        if (loadingAnimation != null) {
            loadingAnimation.stop();
        }
    }
    
    private String formatPreviewContent(String jsonContent, ProcessInfo processInfo) {
        // Try to extract statistics from JSON
        int totalControls = 0;
        long duration = 0;
        String method = "Unknown";
        
        try {
            if (jsonContent.contains("\"inspectionStats\"")) {
                String stats = jsonContent.substring(jsonContent.indexOf("\"inspectionStats\""));
                if (stats.contains("\"totalControlsFound\"")) {
                    String controlsStr = stats.substring(stats.indexOf("\"totalControlsFound\": ") + 23);
                    totalControls = Integer.parseInt(controlsStr.substring(0, controlsStr.indexOf(",")));
                }
                if (stats.contains("\"durationMs\"")) {
                    String durationStr = stats.substring(stats.indexOf("\"durationMs\": ") + 15);
                    duration = Long.parseLong(durationStr.substring(0, durationStr.indexOf(",")));
                }
            }
            if (jsonContent.contains("\"inspectionMethod\"")) {
                String methodLine = jsonContent.substring(jsonContent.indexOf("\"inspectionMethod\": \"") + 22);
                method = methodLine.substring(0, methodLine.indexOf("\""));
            }
        } catch (Exception e) {
            // Ignore parsing errors, show raw JSON instead
        }
        
        StringBuilder preview = new StringBuilder();
        preview.append("╔═══════════════════════════════════════════════╗\n");
        preview.append("║       INSPECTION SUCCESSFUL ✓                 ║\n");
        preview.append("╚═══════════════════════════════════════════════╝\n\n");
        preview.append("Process Information:\n");
        preview.append("─────────────────────────────────────────────────\n");
        preview.append("  PID:           " + processInfo.getPid() + "\n");
        preview.append("  Name:          " + processInfo.getName() + "\n");
        preview.append("  Type:          " + processInfo.getType() + "\n");
        preview.append("  Window Title:  " + processInfo.getWindowTitle() + "\n\n");
        
        preview.append("Inspection Results:\n");
        preview.append("─────────────────────────────────────────────────\n");
        preview.append("  Method:        " + method + "\n");
        if (totalControls > 0) {
            preview.append("  Controls:      " + totalControls + " UI elements found\n");
        }
        if (duration > 0) {
            preview.append("  Duration:      " + duration + " ms\n");
        }
        preview.append("\n\nJSON Preview (first 2000 chars):\n");
        preview.append("═════════════════════════════════════════════════\n");
        preview.append(jsonContent.substring(0, Math.min(2000, jsonContent.length())));
        if (jsonContent.length() > 2000) {
            preview.append("\n\n... (truncated, open file to view complete output) ...");
        }
        
        return preview.toString();
    }
    
    private void showSuccessAlert(String filePath) {
        Alert alert = new Alert(Alert.AlertType.INFORMATION);
        alert.setTitle("Inspection Complete");
        alert.setHeaderText("Successfully inspected process");
        alert.setContentText("Results saved to:\n" + filePath);
        
        ButtonType openButton = new ButtonType("Open File");
        ButtonType closeButton = new ButtonType("Close", ButtonBar.ButtonData.CANCEL_CLOSE);
        alert.getButtonTypes().setAll(openButton, closeButton);
        
        Optional<ButtonType> result = alert.showAndWait();
        if (result.isPresent() && result.get() == openButton) {
            try {
                java.awt.Desktop.getDesktop().open(new File(filePath));
            } catch (IOException e) {
                showError("Failed to open file", e.getMessage());
            }
        }
    }
    
    @FXML
    private void handleOpenFile() {
        InspectionHistory.HistoryEntry selected = historyTable.getSelectionModel().getSelectedItem();
        if (selected != null && selected.outputFile != null) {
            try {
                java.awt.Desktop.getDesktop().open(new File(selected.outputFile));
            } catch (IOException e) {
                showError("Failed to open file", e.getMessage());
            }
        }
    }
    
    @FXML
    private void handleClearHistory() {
        Alert alert = new Alert(Alert.AlertType.CONFIRMATION);
        alert.setTitle("Clear History");
        alert.setHeaderText("Clear inspection history?");
        alert.setContentText("This will remove all history entries but keep the output files.");
        
        Optional<ButtonType> result = alert.showAndWait();
        if (result.isPresent() && result.get() == ButtonType.OK) {
            history.clearHistory();
            refreshHistoryTable();
        }
    }
    
    @FXML
    private void handleDarkModeToggle() {
        Stage stage = (Stage) darkModeToggle.getScene().getWindow();
        if (darkModeToggle.isSelected()) {
            stage.getScene().getStylesheets().clear();
            stage.getScene().getStylesheets().add(
                getClass().getResource("/styles/dark.css").toExternalForm());
        } else {
            stage.getScene().getStylesheets().clear();
            stage.getScene().getStylesheets().add(
                getClass().getResource("/styles/light.css").toExternalForm());
        }
    }
    
    @FXML
    private void handleHighlightToggle() {
        if (highlightToggle.isSelected()) {
            InspectionHistory.HistoryEntry selected = historyTable.getSelectionModel().getSelectedItem();
            if (selected != null) {
                try {
                    List<ComponentHighlighter.ComponentBounds> bounds = ComponentBoundsParser.parseFromFile(selected.outputFile);
                    highlighter.loadComponents(bounds);
                    highlighter.start();
                    statusLabel.setText("Highlight mode active - hover over components");
                } catch (Exception e) {
                    statusLabel.setText("Failed to load component bounds: " + e.getMessage());
                    highlightToggle.setSelected(false);
                }
            } else {
                statusLabel.setText("Please select a history entry first");
                highlightToggle.setSelected(false);
            }
        } else {
            highlighter.stop();
            statusLabel.setText("Highlight mode disabled");
        }
    }
    
    private void refreshProcessList() {
        Task<List<ProcessInfo>> task = new Task<List<ProcessInfo>>() {
            @Override
            protected List<ProcessInfo> call() {
                List<ProcessInfo> processes = new ArrayList<>();
                
                // Get all windows using JNA
                User32.INSTANCE.EnumWindows((hwnd, data) -> {
                    if (User32.INSTANCE.IsWindowVisible(hwnd)) {
                        IntByReference pid = new IntByReference();
                        User32.INSTANCE.GetWindowThreadProcessId(hwnd, pid);
                        
                        char[] windowText = new char[512];
                        User32.INSTANCE.GetWindowText(hwnd, windowText, 512);
                        String title = Native.toString(windowText);
                        
                        if (!title.isEmpty()) {
                            // Get process name
                            String processName = getProcessName(pid.getValue());
                            boolean isJava = AttachConnector.isJavaProcess(String.valueOf(pid.getValue()));
                            
                            processes.add(new ProcessInfo(pid.getValue(), processName, title, isJava, true));
                        }
                    }
                    return true;
                }, null);
                
                return processes;
            }
            
            @Override
            protected void succeeded() {
                processList.setAll(getValue());
                statusLabel.setText("Loaded " + processList.size() + " processes");
            }
        };
        
        Thread thread = new Thread(task);
        thread.setDaemon(true);
        thread.start();
    }
    
    private String getProcessName(int pid) {
        try {
            ProcessHandle handle = ProcessHandle.of(pid).orElse(null);
            if (handle != null) {
                String command = handle.info().command().orElse("");
                if (!command.isEmpty()) {
                    return new File(command).getName();
                }
            }
        } catch (Exception e) {
            // Ignore
        }
        return "Unknown";
    }
    
    private void applyFilters() {
        String searchText = searchField.getText().toLowerCase();
        String filterValue = filterComboBox.getValue();
        
        filteredProcessList.setPredicate(process -> {
            // Search filter
            if (!searchText.isEmpty()) {
                boolean matchesSearch = 
                    process.getName().toLowerCase().contains(searchText) ||
                    process.getWindowTitle().toLowerCase().contains(searchText) ||
                    String.valueOf(process.getPid()).contains(searchText);
                if (!matchesSearch) return false;
            }
            
            // Type filter
            if ("Java Only".equals(filterValue) && !"Java".equals(process.getType())) {
                return false;
            } else if ("Non-Java Only".equals(filterValue) && "Java".equals(process.getType())) {
                return false;
            }
            
            return true;
        });
        
        statusLabel.setText("Showing " + filteredProcessList.size() + " of " + processList.size() + " processes");
    }
    
    private void refreshHistoryTable() {
        historyTable.setItems(FXCollections.observableArrayList(history.getEntries()));
    }
    
    private void startAutoRefresh() {
        autoRefreshTimer = new Timer(true);
        autoRefreshTimer.scheduleAtFixedRate(new TimerTask() {
            @Override
            public void run() {
                Platform.runLater(() -> refreshProcessList());
            }
        }, 5000, 5000); // Refresh every 5 seconds
    }
    
    private void stopAutoRefresh() {
        if (autoRefreshTimer != null) {
            autoRefreshTimer.cancel();
            autoRefreshTimer = null;
        }
    }
    
    private void showError(String header, String content) {
        Alert alert = new Alert(Alert.AlertType.ERROR);
        alert.setTitle("Error");
        alert.setHeaderText(header);
        alert.setContentText(content);
        alert.showAndWait();
    }
}
