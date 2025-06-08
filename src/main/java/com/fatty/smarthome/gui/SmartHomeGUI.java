package com.fatty.smarthome.gui;

import com.fatty.smarthome.core.*;
import com.fatty.smarthome.core.DeviceAnalytics;
import com.fatty.smarthome.core.DeviceState;
import com.fatty.smarthome.core.DeviceView;
import com.fatty.smarthome.core.FacadeSmartHome;
import com.fatty.smarthome.core.PersistenceService;
import com.fatty.smarthome.devices.SmartDevice;import com.fatty.smarthome.devices.SmartDevice;
import com.fatty.smarthome.util.SmartHomeException;
import javafx.animation.KeyFrame;
import javafx.animation.Timeline;
import javafx.application.Application;
import javafx.application.Platform;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.geometry.Insets;
import javafx.geometry.Orientation;
import javafx.geometry.Pos;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.scene.control.cell.PropertyValueFactory;
import javafx.scene.layout.*;
import javafx.scene.paint.Color;
import javafx.stage.Stage;
import javafx.util.Duration;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.*;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

/**
 * Sophisticated JavaFX GUI for Smart Home System.
 * Combines table view, advanced controls, and real-time updates.
 */
public class SmartHomeGUI extends Application {
    // Core components
    private FacadeSmartHome facade;
    private PersistenceService persistenceService;

    // GUI components
    private TableView<DeviceView> deviceTable;
    private ObservableList<DeviceView> deviceData;
    private TextArea logArea;
    private Label statusLabel;
    private Label statsLabel;
    private TextField searchField;
    private ComboBox<String> filterCombo;
    private ProgressBar systemLoadBar;

    // Scheduler for auto-save and updates
    private ScheduledExecutorService scheduler;

    // Settings
    private boolean autoSaveEnabled = true;

    @Override
    public void start(Stage primaryStage) {
        // Initialize components
        facade = FacadeSmartHome.getTheInstance();
        persistenceService = new PersistenceService();
        deviceData = FXCollections.observableArrayList();
        scheduler = Executors.newScheduledThreadPool(2);


        // Setup main window
        primaryStage.setTitle("Smart Home System - Advanced Control Center");
        primaryStage.setScene(createMainScene());
        primaryStage.setMinWidth(1000);
        primaryStage.setMinHeight(700);
        primaryStage.show();


        // Load saved devices
        loadDevices();


        // Start auto-save timer
        if (autoSaveEnabled) {
            // seconds
            int autoSaveInterval = 30;
            scheduler.scheduleAtFixedRate(this::autoSave, autoSaveInterval, autoSaveInterval, TimeUnit.SECONDS);
        }

        // Start statistics updater
        scheduler.scheduleAtFixedRate(this::updateStatistics, 0, 5, TimeUnit.SECONDS);

        // Cleanup on close
        primaryStage.setOnCloseRequest(event -> {
            // Save devices before closing
            saveDevices();

            // Shutdown scheduler threads
            scheduler.shutdown();
            try {
                // Wait up to 5 seconds for tasks to complete
                if (!scheduler.awaitTermination(5, TimeUnit.SECONDS)) {
                    scheduler.shutdownNow();
                }
            } catch (InterruptedException e) {
                scheduler.shutdownNow();
            }

            // Ensure the application exits completely
            Platform.exit();
            System.exit(0);
        });
    }


    /**
     * Creates the main scene with all components.
     */
    private Scene createMainScene() {
        BorderPane root = new BorderPane();

        // Apply styling
        root.setStyle("-fx-background-color: #f5f5f5;");

        // Top section
        VBox topSection = new VBox();
        topSection.getChildren().addAll(createMenuBar(), createToolBar());
        root.setTop(topSection);

        // Center section - Main content area
        root.setCenter(createCenterContent());

        // Left section - Control panel
        root.setLeft(createControlPanel());

        // Right section - Analytics panel
        root.setRight(createAnalyticsPanel());

        // Bottom section - Status bar
        root.setBottom(createStatusBar());

        return new Scene(root, 1200, 800);
    }

    /**
     * Creates the menu bar with comprehensive options.
     */
    private MenuBar createMenuBar() {
        MenuBar menuBar = new MenuBar();

        // File menu
        Menu fileMenu = new Menu("File");
        MenuItem newSessionItem = new MenuItem("New Session");
        newSessionItem.setOnAction(e -> newSession());
        MenuItem saveItem = new MenuItem("Save Devices");
        saveItem.setOnAction(e -> saveDevices());
        MenuItem loadItem = new MenuItem("Load Devices");
        loadItem.setOnAction(e -> loadDevices());
        MenuItem exportItem = new MenuItem("Export to JSON");
        exportItem.setOnAction(e -> exportToJson());
        MenuItem exitItem = new MenuItem("Exit");
        exitItem.setOnAction(e -> Platform.exit());
        fileMenu.getItems().addAll(newSessionItem, new SeparatorMenuItem(),
                saveItem, loadItem, exportItem, new SeparatorMenuItem(), exitItem);

        // Device menu
        Menu deviceMenu = new Menu("Device");
        MenuItem addDeviceItem = new MenuItem("Add Device");
        addDeviceItem.setOnAction(e -> showAddDeviceDialog());
        MenuItem removeDeviceItem = new MenuItem("Remove Selected");
        removeDeviceItem.setOnAction(e -> removeSelectedDevice());
        MenuItem turnAllOnItem = new MenuItem("Turn All On");
        turnAllOnItem.setOnAction(e -> bulkOperation(true));
        MenuItem turnAllOffItem = new MenuItem("Turn All Off");
        turnAllOffItem.setOnAction(e -> bulkOperation(false));
        deviceMenu.getItems().addAll(addDeviceItem, removeDeviceItem,
                new SeparatorMenuItem(), turnAllOnItem, turnAllOffItem);

        // Automation menu
        Menu automationMenu = new Menu("Automation");
        MenuItem runRulesItem = new MenuItem("Run Automation Rules");
        runRulesItem.setOnAction(e -> runAutomation());
        MenuItem configureRulesItem = new MenuItem("Configure Rules");
        configureRulesItem.setOnAction(e -> showAutomationDialog());
        automationMenu.getItems().addAll(runRulesItem, configureRulesItem);

        // View menu
        Menu viewMenu = new Menu("View");
        MenuItem refreshItem = new MenuItem("Refresh");
        refreshItem.setOnAction(e -> updateDeviceTable());
        MenuItem clearLogItem = new MenuItem("Clear Log");
        clearLogItem.setOnAction(e -> logArea.clear());
        CheckMenuItem autoSaveItem = new CheckMenuItem("Auto-Save Enabled");
        autoSaveItem.setSelected(autoSaveEnabled);
        autoSaveItem.setOnAction(e -> autoSaveEnabled = autoSaveItem.isSelected());
        viewMenu.getItems().addAll(refreshItem, clearLogItem, new SeparatorMenuItem(), autoSaveItem);

        // Help menu
        Menu helpMenu = new Menu("Help");
        MenuItem userGuideItem = new MenuItem("User Guide");
        userGuideItem.setOnAction(e -> showUserGuide());
        MenuItem aboutItem = new MenuItem("About");
        aboutItem.setOnAction(e -> showAbout());
        helpMenu.getItems().addAll(userGuideItem, aboutItem);

        menuBar.getMenus().addAll(fileMenu, deviceMenu,  viewMenu, helpMenu);
//        menuBar.getMenus().addAll(fileMenu, deviceMenu, automationMenu, viewMenu, helpMenu);

        return menuBar;
    }

    /**
     * Creates the toolbar with quick access buttons.
     */
    private ToolBar createToolBar() {
        ToolBar toolBar = new ToolBar();

        Button addButton = new Button("Add Device");
        addButton.setOnAction(e -> showAddDeviceDialog());

        Button refreshButton = new Button("Refresh");
        refreshButton.setOnAction(e -> updateDeviceTable());

        Button saveButton = new Button("Save");
        saveButton.setOnAction(e -> saveDevices());

        Separator separator1 = new Separator();

        Label searchLabel = new Label("Search:");
        searchField = new TextField();
        searchField.setPromptText("Search devices...");
        searchField.setPrefWidth(200);
        searchField.textProperty().addListener((obs, oldText, newText) -> filterDevices());

        Label filterLabel = new Label("Filter:");
        filterCombo = new ComboBox<>();
        filterCombo.getItems().addAll("All Devices", "Lights", "Thermostats", "Cameras", "Active Only", "Inactive Only");
        filterCombo.setValue("All Devices");
        filterCombo.setOnAction(e -> filterDevices());

        toolBar.getItems().addAll(addButton, refreshButton, saveButton, separator1,
                searchLabel, searchField, filterLabel, filterCombo);

        return toolBar;
    }

    /**
     * Creates the center content area with device table.
     */
    private VBox createCenterContent() {
        VBox content = new VBox(10);
        content.setPadding(new Insets(10));

        // Device table
        Label tableLabel = new Label("Device Management");
        tableLabel.setStyle("-fx-font-size: 18; -fx-font-weight: bold;");

        deviceTable = createDeviceTable();
        VBox.setVgrow(deviceTable, Priority.ALWAYS);

        // Log area
        Label logLabel = new Label("System Log");
        logLabel.setStyle("-fx-font-size: 14; -fx-font-weight: bold;");

        logArea = new TextArea();
        logArea.setPrefRowCount(6);
        logArea.setEditable(false);
        logArea.setStyle("-fx-font-family: monospace;");

        content.getChildren().addAll(tableLabel, deviceTable, logLabel, logArea);

        return content;
    }

    /**
     * Creates the device table with all columns.
     */
    private TableView<DeviceView> createDeviceTable() {
        TableView<DeviceView> table = new TableView<>();
        table.setItems(deviceData);

        // Name column
        TableColumn<DeviceView, String> nameCol = new TableColumn<>("Name");
        nameCol.setCellValueFactory(new PropertyValueFactory<>("name"));
        nameCol.setPrefWidth(200);

        // Type column
        TableColumn<DeviceView, String> typeCol = new TableColumn<>("Type");
        typeCol.setCellValueFactory(new PropertyValueFactory<>("type"));
        typeCol.setPrefWidth(100);

        // Status column
        TableColumn<DeviceView, String> statusCol = new TableColumn<>("Status");
        statusCol.setCellValueFactory(new PropertyValueFactory<>("status"));
        statusCol.setPrefWidth(250);

        // Power state column with color coding
        TableColumn<DeviceView, String> powerCol = new TableColumn<>("Power");
        powerCol.setCellValueFactory(new PropertyValueFactory<>("on"));
        powerCol.setCellFactory(column -> new TableCell<DeviceView, String>() {
            @Override
            protected void updateItem(String item, boolean empty) {
                super.updateItem(item, empty);
                if (empty || item == null) {
                    setText(null);
                    setStyle("");
                } else {
                    setText(item);
                    if ("ON".equals(item)) {
                        setTextFill(Color.GREEN);
                        setStyle("-fx-font-weight: bold;");
                    } else {
                        setTextFill(Color.RED);
                        setStyle("-fx-font-weight: bold;");
                    }
                }
            }
        });
        powerCol.setPrefWidth(80);

        // Actions column
        TableColumn<DeviceView, Void> actionsCol = new TableColumn<>("Actions");
        actionsCol.setCellFactory(param -> new TableCell<DeviceView, Void>() {
            private final Button toggleBtn = new Button();
            private final Button configBtn = new Button("Config");

            {
                toggleBtn.setOnAction(event -> {
                    DeviceView device = getTableView().getItems().get(getIndex());
                    toggleDevice(device);
                });

                configBtn.setOnAction(event -> {
                    DeviceView device = getTableView().getItems().get(getIndex());
                    configureDevice(device);
                });
            }

            @Override
            protected void updateItem(Void item, boolean empty) {
                super.updateItem(item, empty);
                if (empty) {
                    setGraphic(null);
                } else {
                    DeviceView device = getTableView().getItems().get(getIndex());
                    toggleBtn.setText("ON".equals(device.isOn()) ? "Turn OFF" : "Turn ON");

                    HBox buttons = new HBox(5);
                    buttons.getChildren().addAll(toggleBtn, configBtn);
                    setGraphic(buttons);
                }
            }
        });
        actionsCol.setPrefWidth(150);

        table.getColumns().addAll(nameCol, typeCol, statusCol, powerCol, actionsCol);
        table.setColumnResizePolicy(TableView.CONSTRAINED_RESIZE_POLICY); // Updated to non-deprecated policy

        // Enable row selection
        table.getSelectionModel().setSelectionMode(SelectionMode.MULTIPLE);

        // Context menu
        ContextMenu contextMenu = new ContextMenu();
        MenuItem toggleItem = new MenuItem("Toggle Power");
        toggleItem.setOnAction(e -> {
            DeviceView selected = table.getSelectionModel().getSelectedItem();
            if (selected != null) toggleDevice(selected);
        });
        MenuItem configureItem = new MenuItem("Configure");
        configureItem.setOnAction(e -> {
            DeviceView selected = table.getSelectionModel().getSelectedItem();
            if (selected != null) configureDevice(selected);
        });
        MenuItem removeItem = new MenuItem("Remove");
        removeItem.setOnAction(e -> removeSelectedDevice());
        contextMenu.getItems().addAll(toggleItem, configureItem, new SeparatorMenuItem(), removeItem);
        table.setContextMenu(contextMenu);

        return table;
    }

    /**
     * Creates the control panel on the left.
     */
    private VBox createControlPanel() {
        VBox panel = new VBox(15);
        panel.setPadding(new Insets(15));
        panel.setPrefWidth(250);
        panel.setStyle("-fx-background-color: #e8e8e8;");

        Label titleLabel = new Label("Quick Controls");
        titleLabel.setStyle("-fx-font-size: 16; -fx-font-weight: bold;");

        // Quick add section
        TitledPane addPane = new TitledPane();
        addPane.setText("Quick Add Device");
        VBox addContent = new VBox(10);
        addContent.setPadding(new Insets(10));

        TextField nameField = new TextField();
        nameField.setPromptText("Device name");

        ComboBox<String> typeCombo = new ComboBox<>();
        typeCombo.getItems().addAll("Light", "Thermostat", "Camera");
        typeCombo.setValue("Light");
        typeCombo.setPrefWidth(Double.MAX_VALUE);

        Button quickAddBtn = new Button("Add Device");
        quickAddBtn.setPrefWidth(Double.MAX_VALUE);
        quickAddBtn.setOnAction(e -> {
            if (!nameField.getText().trim().isEmpty()) {
                addDevice(nameField.getText().trim(), typeCombo.getValue());
                nameField.clear();
            }
        });

        addContent.getChildren().addAll(nameField, typeCombo, quickAddBtn);
        addPane.setContent(addContent);

        // Bulk operations
        TitledPane bulkPane = new TitledPane();
        bulkPane.setText("Bulk Operations");
        VBox bulkContent = new VBox(10);
        bulkContent.setPadding(new Insets(10));

        Button allOnBtn = new Button("Turn All ON");
        allOnBtn.setPrefWidth(Double.MAX_VALUE);
        allOnBtn.setOnAction(e -> bulkOperation(true));

        Button allOffBtn = new Button("Turn All OFF");
        allOffBtn.setPrefWidth(Double.MAX_VALUE);
        allOffBtn.setOnAction(e -> bulkOperation(false));

        Button automateBtn = new Button("Run Automation");
        automateBtn.setPrefWidth(Double.MAX_VALUE);
        automateBtn.setOnAction(e -> runAutomation());

        bulkContent.getChildren().addAll(allOnBtn, allOffBtn, automateBtn);

        bulkPane.setContent(bulkContent);

        // System info
        TitledPane infoPane = new TitledPane();
        infoPane.setText("System Information");
        VBox infoContent = new VBox(10);
        infoContent.setPadding(new Insets(10));

        statsLabel = new Label("Loading statistics...");
        statsLabel.setWrapText(true);

        systemLoadBar = new ProgressBar(0);
        systemLoadBar.setPrefWidth(Double.MAX_VALUE);

        infoContent.getChildren().addAll(statsLabel, new Label("System Load:"), systemLoadBar);
        infoPane.setContent(infoContent);

        panel.getChildren().addAll(titleLabel, addPane, bulkPane, infoPane);

        return panel;
    }

    /**
     * Creates the analytics panel on the right.
     */
    private VBox createAnalyticsPanel() {
        VBox panel = new VBox(15);
        panel.setPadding(new Insets(15));
        panel.setPrefWidth(250);
        panel.setStyle("-fx-background-color: #e8e8e8;");

        Label titleLabel = new Label("Analytics");
        titleLabel.setStyle("-fx-font-size: 16; -fx-font-weight: bold;");

        Button generateReportBtn = new Button("Generate Report");
        generateReportBtn.setPrefWidth(Double.MAX_VALUE);
        generateReportBtn.setOnAction(e -> showAnalyticsReport());

        Button exportDataBtn = new Button("Export Data");
        exportDataBtn.setPrefWidth(Double.MAX_VALUE);
        exportDataBtn.setOnAction(e -> exportToJson());

        Separator separator = new Separator();

        Label tipsLabel = new Label("Tips:");
        tipsLabel.setStyle("-fx-font-weight: bold;");

        TextArea tipsArea = new TextArea();
        tipsArea.setWrapText(true);
        tipsArea.setEditable(false);
        tipsArea.setPrefRowCount(10);
        tipsArea.setText(
                "• Use Ctrl+Click to select multiple devices\n" +
                        "• Right-click for context menu\n" +
                        "• Auto-save runs every 30 seconds\n" +
                        "• Thermostats can be set between 10-32°C\n" +
                        "• Use search to quickly find devices\n" +
                        "• Export to JSON for backup"
        );

        panel.getChildren().addAll(titleLabel, generateReportBtn, exportDataBtn,
                separator, tipsLabel, tipsArea);

        return panel;
    }

    /**
     * Creates the status bar at the bottom.
     */
    private HBox createStatusBar() {
        HBox statusBar = new HBox(10);
        statusBar.setPadding(new Insets(5, 10, 5, 10));
        statusBar.setAlignment(Pos.CENTER_LEFT);
        statusBar.setStyle("-fx-background-color: #d0d0d0;");

        statusLabel = new Label("Ready");
        statusLabel.setStyle("-fx-font-weight: bold;");

        Region spacer = new Region();
        HBox.setHgrow(spacer, Priority.ALWAYS);

        Label timeLabel = new Label();

        // Update time every second
        Timeline timeline = new Timeline(
                new KeyFrame(Duration.seconds(1), e -> {
                    timeLabel.setText(LocalDateTime.now().format(
                            DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss")
                    ));
                })
        );
        timeline.setCycleCount(Timeline.INDEFINITE);
        timeline.play();

        statusBar.getChildren().addAll(statusLabel, spacer,
                new Label("Auto-save: " + (autoSaveEnabled ? "ON" : "OFF")),
                new Separator(Orientation.VERTICAL), timeLabel);

        return statusBar;
    }

    /**
     * Shows dialog to add a new device.
     */
    private void showAddDeviceDialog() {
        Dialog<ButtonType> dialog = new Dialog<>();
        dialog.setTitle("Add New Device");
        dialog.setHeaderText("Enter device details");

        // Create form
        GridPane grid = new GridPane();
        grid.setHgap(10);
        grid.setVgap(10);
        grid.setPadding(new Insets(20));

        TextField nameField = new TextField();
        nameField.setPromptText("Device name");

        ComboBox<String> typeCombo = new ComboBox<>();
        typeCombo.getItems().addAll("Light", "Thermostat", "Camera");
        typeCombo.setValue("Light");

        CheckBox turnOnCheck = new CheckBox("Turn on immediately");

        grid.add(new Label("Name:"), 0, 0);
        grid.add(nameField, 1, 0);
        grid.add(new Label("Type:"), 0, 1);
        grid.add(typeCombo, 1, 1);
        grid.add(turnOnCheck, 1, 2);

        dialog.getDialogPane().setContent(grid);
        dialog.getDialogPane().getButtonTypes().addAll(ButtonType.OK, ButtonType.CANCEL);

        // Focus on name field
        Platform.runLater(nameField::requestFocus);

        Optional<ButtonType> result = dialog.showAndWait();
        if (result.isPresent() && result.get() == ButtonType.OK) {
            String name = nameField.getText().trim();
            if (!name.isEmpty()) {
                addDevice(name, typeCombo.getValue());
                if (turnOnCheck.isSelected()) {
                    try {
                        facade.smartHomeAccess("turnon", name, "");
                        updateDeviceTable();
                    } catch (SmartHomeException ex) {
                        // Device was just added, shouldn't fail
                    }
                }
            }
        }
    }

    /**
     * Adds a device to the system.
     */
    private void addDevice(String name, String type) {
        try {
            String result = facade.smartHomeAccess("add", name, type.toLowerCase());
            log("SUCCESS: " + result);
            updateDeviceTable();
            updateStatus("Device added: " + name);
        } catch (SmartHomeException e) {
            showError("Failed to add device", e.getMessage());
        }
    }

    /**
     * Toggles a device on/off.
     */
    private void toggleDevice(DeviceView deviceView) {
        try {
            String command = "ON".equals(deviceView.isOn()) ? "turnoff" : "turnon";
            String result = facade.smartHomeAccess(command, deviceView.getName(), "");
            log("SUCCESS: " + result);
            updateDeviceTable();
            updateStatus("Toggled: " + deviceView.getName());
        } catch (SmartHomeException e) {
            showError("Failed to toggle device", e.getMessage());
        }
    }

    /**
     * Configures a device (e.g., set temperature).
     */
    private void configureDevice(DeviceView deviceView) {
        if ("Thermostat".equals(deviceView.getType())) {
            TextInputDialog dialog = new TextInputDialog("21");
            dialog.setTitle("Configure Thermostat");
            dialog.setHeaderText("Set temperature for " + deviceView.getName());
            dialog.setContentText("Temperature (10-32°C):");

            Optional<String> result = dialog.showAndWait();
            result.ifPresent(temp -> {
                try {
                    String response = facade.smartHomeAccess("settemp", deviceView.getName(), temp);
                    log("SUCCESS: " + response);
                    updateDeviceTable();
                    updateStatus("Temperature set: " + deviceView.getName());
                } catch (SmartHomeException e) {
                    showError("Failed to set temperature", e.getMessage());
                }
            });
        } else {
            showInfo("Device Configuration",
                    "Configuration options for " + deviceView.getType() + " devices coming soon!");
        }
    }

    /**
     * Removes selected devices.
     */
    private void removeSelectedDevice() {
        ObservableList<DeviceView> selected = deviceTable.getSelectionModel().getSelectedItems();
        if (selected.isEmpty()) {
            showWarning("No Selection", "Please select devices to remove.");
            return;
        }

        Alert confirm = new Alert(Alert.AlertType.CONFIRMATION);
        confirm.setTitle("Confirm Removal");
        confirm.setHeaderText("Remove " + selected.size() + " device(s)?");
        confirm.setContentText("This action cannot be undone.");

        Optional<ButtonType> result = confirm.showAndWait();
        if (result.isPresent() && result.get() == ButtonType.OK) {
            // In real implementation, would add remove functionality to facade
            showInfo("Remove Devices", "Device removal functionality coming soon!");
        }
    }

    /**
     * Updates the device table with current data.
     */
    private void updateDeviceTable() {
        try {
            List<SmartDevice> devices = facade.getDevices();

            // Convert to DeviceView objects
            List<DeviceView> views = devices.stream()
                    .map(DeviceView::new)
                    .toList();

            deviceData.clear();
            deviceData.addAll(views);

            // Apply current filter
            filterDevices();
        } catch (Exception e) {
            showError("Update Error", "Failed to update device list: " + e.getMessage());
        }
    }

    /**
     * Filters devices based on search and filter criteria.
     */
    private void filterDevices() {

        String searchText = searchField.getText().toLowerCase();
        String filterType = filterCombo.getValue();

        List<DeviceView> filtered = new ArrayList<>(deviceData);

        // Apply search filter
        if (!searchText.isEmpty()) {
            filtered = filtered.stream()
                    .filter(d -> d.getName().toLowerCase().contains(searchText) ||
                            d.getType().toLowerCase().contains(searchText) ||
                            d.getStatus().toLowerCase().contains(searchText))
                    .collect(Collectors.toList());
        }

        // Apply type filter
        switch (filterType) {
            case "Lights" -> filtered = filtered.stream()
                    .filter(d -> "Light".equals(d.getType()))
                    .collect(Collectors.toList());
            case "Thermostats" -> filtered = filtered.stream()
                    .filter(d -> "Thermostat".equals(d.getType()))
                    .collect(Collectors.toList());
            case "Cameras" -> filtered = filtered.stream()
                    .filter(d -> "SecurityCamera".equals(d.getType()))
                    .collect(Collectors.toList());
            case "Active Only" -> filtered = filtered.stream()
                    .filter(d -> "ON".equals(d.isOn()))
                    .collect(Collectors.toList());
            case "Inactive Only" -> filtered = filtered.stream()
                    .filter(d -> "OFF".equals(d.isOn()))
                    .collect(Collectors.toList());
        }

        deviceData.clear();
        deviceData.addAll(filtered);
    }

    /**
     * Performs bulk operation on all devices.
     */
    private void bulkOperation(boolean turnOn) {
        try {
            List<SmartDevice> devices = facade.getDevices();
            int count = 0;

            for (SmartDevice device : devices) {
                try {
                    String command = turnOn ? "turnon" : "turnoff";
                    facade.smartHomeAccess(command, device.getName(), "");
                    count++;
                } catch (SmartHomeException e) {
                    log("Failed to toggle " + device.getName() + ": " + e.getMessage());
                }
            }

            log("Bulk operation completed: " + count + " devices " + (turnOn ? "turned ON" : "turned OFF"));
            updateDeviceTable();
            updateStatus("Bulk operation completed");
        } catch (Exception e) {
            showError("Bulk Operation Failed", e.getMessage());
        }
    }

    /**
     * Runs automation rules.
     */
    private void runAutomation() {
        try {
            String result = facade.smartHomeAccess("automate", "", "");
            log("SUCCESS: " + result);
            updateDeviceTable();
            updateStatus("Automation rules applied");
        } catch (SmartHomeException e) {
            showError("Automation Failed", e.getMessage());
        }
    }

    /**
     * Shows automation configuration dialog.
     */
    private void showAutomationDialog() {
        showInfo("Automation Rules",
                "Advanced automation rule configuration coming soon!\n\n" +
                        "Current rule: Turn on all lights automatically");
    }

    /**
     * Saves devices to persistent storage.
     */
    private void saveDevices() {
        try {
            List<SmartDevice> devices = facade.getDevices();
            persistenceService.saveDeviceStatesBinary(devices);
            persistenceService.saveDeviceStatesJson(devices);
            log("Devices saved successfully");
            updateStatus("Devices saved");
        } catch (Exception e) {
            showError("Save Failed", e.getMessage());
        }
    }

    /**
     * Loads devices from persistent storage.
     */
    private void loadDevices() {
        try {
            List<DeviceState> states = persistenceService.loadDeviceStatesBinary();
            if (states.isEmpty()) {
                states = persistenceService.loadDeviceStatesJson();
            }

            facade.reset(); // Clear existing devices

            for (DeviceState state : states) {
                try {
                    SmartDevice device = persistenceService.reconstructDevice(state);
                    facade.smartHomeAccess("add", device.getName(),
                            device.getClass().getSimpleName().toLowerCase());
                    if (state.isOn()) {
                        facade.smartHomeAccess("turnon", device.getName(), "");
                    }
                } catch (SmartHomeException e) {
                    log("Failed to load device: " + state.getDeviceName());
                }
            }

            updateDeviceTable();
            log("Loaded " + states.size() + " devices");
            updateStatus("Devices loaded");
        } catch (Exception e) {
            log("No saved devices found or load failed: " + e.getMessage());
        }
    }

    /**
     * Exports devices to JSON file.
     */
    private void exportToJson() {
        try {
            List<SmartDevice> devices = facade.getDevices();
            persistenceService.saveDeviceStatesJson(devices);
            showInfo("Export Complete",
                    "Devices exported to device_states.json\n" +
                            "Total devices: " + devices.size());
            log("Exported " + devices.size() + " devices to JSON");
        } catch (Exception e) {
            showError("Export Failed", e.getMessage());
        }
    }

    /**
     * Auto-save function called by scheduler.
     */
    private void autoSave() {
        Platform.runLater(() -> {
            try {
                List<SmartDevice> devices = facade.getDevices();
                persistenceService.saveDeviceStatesBinary(devices);
                updateStatus("Auto-saved at " + LocalDateTime.now().format(
                        DateTimeFormatter.ofPattern("HH:mm:ss")));
            } catch (Exception e) {
                log("Auto-save failed: " + e.getMessage());
            }
        });
    }

    /**
     * Updates statistics display.
     */
    private void updateStatistics() {
        Platform.runLater(() -> {
            try {
                List<SmartDevice> devices = facade.getDevices();

                // Calculate statistics using DeviceAnalytics
                long totalDevices = devices.size();
                long activeDevices = DeviceAnalytics.getActiveDevices(devices).size();
                Map<String, Long> typeCounts = DeviceAnalytics.countByType(devices);

                // Build statistics text
                StringBuilder stats = new StringBuilder();
                stats.append("Total Devices: ").append(totalDevices).append("\n");
                stats.append("Active: ").append(activeDevices).append("\n");
                stats.append("Inactive: ").append(totalDevices - activeDevices).append("\n\n");

                typeCounts.forEach((type, count) ->
                        stats.append(type).append(": ").append(count).append("\n"));

                statsLabel.setText(stats.toString());

                // Update system load bar (simulated)
                double load = activeDevices / (double) Math.max(totalDevices, 1);
                systemLoadBar.setProgress(load);
            } catch (Exception e) {
                statsLabel.setText("Error loading statistics");
            }
        });
    }

    /**
     * Shows analytics report.
     */
    private void showAnalyticsReport() {
        try {
            List<SmartDevice> devices = facade.getDevices();
            String report = DeviceAnalytics.generateReport(devices);

            // Add thermostat statistics
            IntSummaryStatistics thermoStats = DeviceAnalytics.getThermostatStatistics(devices);
            if (thermoStats.getCount() > 0) {
                report += "\n\nThermostat Statistics:\n";
                report += "  Average Temperature: " + String.format("%.1f°C\n", thermoStats.getAverage());
                report += "  Min Temperature: " + thermoStats.getMin() + "°C\n";
                report += "  Max Temperature: " + thermoStats.getMax() + "°C\n";
            }

            // Show in dialog
            Alert alert = new Alert(Alert.AlertType.INFORMATION);
            alert.setTitle("Analytics Report");
            alert.setHeaderText("Smart Home System Analytics");

            TextArea textArea = new TextArea(report);
            textArea.setEditable(false);
            textArea.setWrapText(true);
            textArea.setPrefRowCount(20);

            alert.getDialogPane().setContent(textArea);
            alert.getDialogPane().setPrefWidth(500);
            alert.showAndWait();

        } catch (Exception e) {
            showError("Analytics Error", e.getMessage());
        }
    }

    /**
     * Creates a new session (clears all devices).
     */
    private void newSession() {
        Alert confirm = new Alert(Alert.AlertType.CONFIRMATION);
        confirm.setTitle("New Session");
        confirm.setHeaderText("Start a new session?");
        confirm.setContentText("This will clear all current devices. Unsaved changes will be lost.");

        Optional<ButtonType> result = confirm.showAndWait();
        if (result.isPresent() && result.get() == ButtonType.OK) {
            facade.reset();
            updateDeviceTable();
            logArea.clear();
            log("New session started");
            updateStatus("New session");
        }
    }

    /**
     * Shows user guide.
     */
    private void showUserGuide() {
        String guide = """
            SMART HOME SYSTEM - USER GUIDE
            
            ADDING DEVICES:
            • Use the Quick Add panel or Device menu
            • Supported types: Light, Thermostat, Camera
            • Device names must be unique
            
            CONTROLLING DEVICES:
            • Click action buttons in the table
            • Use right-click context menu
            • Select multiple devices with Ctrl+Click
            
            THERMOSTATS:
            • Temperature range: 10-32°C
            • Click Config to set temperature
            
            AUTOMATION:
            • Run automation rules from menu
            • Currently turns on all lights
            
            SAVING/LOADING:
            • Auto-save every 30 seconds (when enabled)
            • Manual save from File menu
            • Data saved in binary and JSON formats
            
            SEARCH & FILTER:
            • Use search box for text search
            • Filter dropdown for device types
            • Filters work together
            
            KEYBOARD SHORTCUTS:
            • Ctrl+S: Save
            • Ctrl+R: Refresh
            • F1: This help guide
            """;

        Alert alert = new Alert(Alert.AlertType.INFORMATION);
        alert.setTitle("User Guide");
        alert.setHeaderText("How to Use Smart Home System");

        TextArea textArea = new TextArea(guide);
        textArea.setEditable(false);
        textArea.setWrapText(true);
        textArea.setPrefRowCount(25);

        alert.getDialogPane().setContent(textArea);
        alert.getDialogPane().setPrefWidth(600);
        alert.showAndWait();
    }

    /**
     * Shows about dialog.
     */
    private void showAbout() {
        Alert alert = new Alert(Alert.AlertType.INFORMATION);
        alert.setTitle("About");
        alert.setHeaderText("Smart Home System v2.0");
        alert.setContentText(
                "Advanced Smart Home Management System\n\n" +
//                        "MET CS 622 - Advanced Programming I\n" +
//                        "Assignment 4 Implementation\n\n" +
                        "Features:\n" +
                        "• Device Management (Lights, Thermostats, Cameras)\n" +
                        "• Real-time Control and Monitoring\n" +
                        "• Automation Rules\n" +
                        "• Binary and JSON Persistence\n" +
                        "• Stream-based Analytics\n" +
                        "• Lambda-powered Event Handling\n\n" +
                        "© 2025 Smart Home Systems"
        );
        alert.showAndWait();
    }

    /**
     * Logs a message to the log area.
     */
    private void log(String message) {
        String timestamp = LocalDateTime.now().format(DateTimeFormatter.ofPattern("HH:mm:ss"));
        if (logArea != null) {
            logArea.appendText("[" + timestamp + "] " + message + "\n");
        } else {
            System.out.println("[" + timestamp + "] " + message);
        }
    }

    /**
     * Updates the status bar.
     */
    private void updateStatus(String message) {
        statusLabel.setText(message);
    }

    /**
     * Shows an error alert.
     */
    private void showError(String title, String message) {
        Alert alert = new Alert(Alert.AlertType.ERROR);
        alert.setTitle(title);
        alert.setHeaderText(null);
        alert.setContentText(message);
        alert.showAndWait();
        log("ERROR: " + message);
    }

    /**
     * Shows a warning alert.
     */
    private void showWarning(String title, String message) {
        Alert alert = new Alert(Alert.AlertType.WARNING);
        alert.setTitle(title);
        alert.setHeaderText(null);
        alert.setContentText(message);
        alert.showAndWait();
    }

    /**
     * Shows an information alert.
     */
    private void showInfo(String title, String message) {
        Alert alert = new Alert(Alert.AlertType.INFORMATION);
        alert.setTitle(title);
        alert.setHeaderText(null);
        alert.setContentText(message);
        alert.showAndWait();
    }

    /**
     * Main method to launch the application.
     */
    public static void main(String[] args) {
        launch(args);
    }
}
