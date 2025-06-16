package com.fatty.smarthome.cli;

import com.fatty.smarthome.core.*;
import com.fatty.smarthome.devices.*;
import com.fatty.smarthome.gui.SmartHomeGUI;
import com.fatty.smarthome.util.SmartHomeException;
import javafx.application.Application;

import java.sql.SQLException;
import java.util.*;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

public class SmartHomeCLI {
    private final FacadeSmartHome facade;
    private final Scanner scanner;
    private final PersistenceService persistenceService;
    private final ConcurrentCLICommands concurrentCommands;

    // Thread management
    private final ExecutorService executorService = Executors.newCachedThreadPool();

    // Constants
    private static final String VERSION = "3.0";
    private static final String PROMPT_FORMAT = "smart-home [%s|%d/%d active] > ";

    // ANSI color codes
    private static final String RESET = "\u001B[0m";
    private static final String RED = "\u001B[31m";
    private static final String GREEN = "\u001B[32m";
    private static final String YELLOW = "\u001B[33m";
    private static final String BLUE = "\u001B[34m";
    private static final String PURPLE = "\u001B[35m";
    private static final String CYAN = "\u001B[36m";
    private static final String BOLD = "\u001B[1m";

    private boolean useColors = true;
    private boolean running = true;

    public SmartHomeCLI() throws SmartHomeException {
        try {
            this.facade = FacadeSmartHome.getTheInstance();
            this.scanner = new Scanner(System.in);
            this.persistenceService = new PersistenceService();
            this.concurrentCommands = new ConcurrentCLICommands(facade);

            // Add shutdown hook to save on unexpected exit
            Runtime.getRuntime().addShutdownHook(new Thread(() -> {
                try {
                    List<SmartDevice> devices = facade.getDevices();
                    if (!devices.isEmpty()) {
                        persistenceService.saveAllDevices(devices);
                        System.out.println("\n✅ Emergency save completed");
                    }
                    executorService.shutdownNow();
                } catch (Exception e) {
                    System.err.println("\n⚠️ Warning: Could not save devices on shutdown");
                }
            }));
        } catch (SQLException e) {
            throw new SmartHomeException("Failed to initialize Smart Home system", e);
        }
    }

    /**
     * Main method to run SmartHomeCLI directly
     */
    public static void main(String[] args) {
        try {
            SmartHomeCLI cli = new SmartHomeCLI();

            // Check for command line arguments (for advanced users)
            boolean noColor = false;
            boolean directCLI = false;
            boolean directGUI = false;

            for (String arg : args) {
                switch (arg.toLowerCase()) {
                    case "--cli" -> directCLI = true;
                    case "--gui", "-g" -> directGUI = true;
                    case "--no-color", "--nocolor" -> noColor = true;
                    case "--help", "-h" -> {
                        cli.showCommandLineHelp();
                        return;
                    }
                }
            }

            if (noColor) {
                cli.useColors = false;
            }

            // Direct launch options for advanced users
            if (directGUI) {
                cli.launchGUI();
                return;
            } else if (directCLI) {
                cli.start();
                return;
            }

            // Show user-friendly interface selection for beginners
            cli.showInterfaceSelection();

        } catch (Exception e) {
            System.err.println("Failed to start Smart Home CLI: " + e.getMessage());
            e.printStackTrace();
        }
    }

    /**
     * Shows user-friendly interface selection menu
     */
    private void showInterfaceSelection() {
        printWelcomeBanner();

        System.out.println("\nWelcome! Please select how you'd like to use the Smart Home System:");
        System.out.println();

        if (useColors) {
            System.out.println("  " + CYAN + "1." + RESET + " " + BOLD + "Command Line Interface (CLI)" + RESET);
            System.out.println("     Text-based commands and controls");
            System.out.println("     Perfect for advanced users and automation");
            System.out.println();

            System.out.println("  " + BLUE + "2." + RESET + " " + BOLD + "Graphical User Interface (GUI)" + RESET);
            System.out.println("     Visual controls with buttons and menus");
            System.out.println("     Easy to use for beginners");
            System.out.println();

            System.out.println("  " + GREEN + "3." + RESET + " " + BOLD + "Both Interfaces" + RESET);
            System.out.println("     Start with CLI and launch GUI when needed");
            System.out.println("     Best of both worlds");
            System.out.println();

            System.out.println("  " + RED + "4." + RESET + " " + BOLD + "Exit" + RESET);
            System.out.println("     Quit the application");
        } else {
            System.out.println("  1. Command Line Interface (CLI)");
            System.out.println("     Text-based commands and controls");
            System.out.println("     Perfect for advanced users and automation");
            System.out.println();

            System.out.println("  2. Graphical User Interface (GUI)");
            System.out.println("     Visual controls with buttons and menus");
            System.out.println("     Easy to use for beginners");
            System.out.println();

            System.out.println("  3. Both Interfaces");
            System.out.println("     Start with CLI and launch GUI when needed");
            System.out.println("     Best of both worlds");
            System.out.println();

            System.out.println("  4. Exit");
            System.out.println("     Quit the application");
        }

        System.out.println();
        System.out.print("Enter your choice (1-4): ");

        Scanner scanner = new Scanner(System.in);
        String choice = scanner.nextLine().trim();

        switch (choice) {
            case "1" -> {
                System.out.println("\n" + (useColors ? GREEN + "✅ Starting CLI mode..." + RESET : "Starting CLI mode..."));
                start();
            }
            case "2" -> {
                System.out.println("\n" + (useColors ? BLUE + "✅ Launching GUI mode..." + RESET : "Launching GUI mode..."));
                launchGUI();
            }
            case "3" -> {
                System.out.println("\n" + (useColors ? GREEN + "✅ Starting both interfaces..." + RESET : "Starting both interfaces..."));
                System.out.println("CLI will start first. Type 'gui' anytime to launch the graphical interface.");
                start();
            }
            case "4" -> {
                System.out.println("\n" + (useColors ? YELLOW + "Thank you for using Smart Home System. Goodbye!" + RESET : "Thank you for using Smart Home System. Goodbye!"));
                System.exit(0);
            }
            default -> {
                System.out.println("\n" + (useColors ? RED + "Invalid choice. Please try again." + RESET : "Invalid choice. Please try again."));
                System.out.println();
                showInterfaceSelection(); // Recursively show menu again
            }
        }
    }

    public void start() {
        printWelcomeBanner();

        // Check database connection
        checkDatabaseConnection();

        // Load saved devices
        loadDevices();

        // Ask about concurrent services
        askAndStartConcurrentServices();

        // Print initial help
        System.out.println("\nType 'help' for commands, 'gui' to launch GUI, or 'exit' to quit.\n");

        // Main command loop
        runCommandLoop();
    }

    private void printWelcomeBanner() {
        if (useColors) {
            System.out.println(CYAN + "╔════════════════════════════════════════╗" + RESET);
            System.out.println(CYAN + "║" + BOLD + YELLOW + "     SMART HOME CONTROL SYSTEM          " + RESET + CYAN + "║" + RESET);
            System.out.println(CYAN + "║" + RESET + "          Version " + VERSION + "                   " + CYAN + "║" + RESET);
            System.out.println(CYAN + "║" + RESET + "         " + CYAN + "║" + RESET);
            System.out.println(CYAN + "╚════════════════════════════════════════╝" + RESET);
        } else {
            System.out.println("╔════════════════════════════════════════╗");
            System.out.println("║     SMART HOME CONTROL SYSTEM          ║");
            System.out.println("║          Version " + VERSION + "       ║");
            System.out.println("║                                        ║");
            System.out.println("╚════════════════════════════════════════╝");
        }
    }

    private void checkDatabaseConnection() {
        System.out.println("\nChecking database connection...");
        if (persistenceService.isDatabaseAvailable()) {
            printSuccess("Database connected (primary storage)");
        } else {
            printWarning("Database unavailable (using file storage)");
        }
    }

    private void loadDevices() {
        System.out.println("\nLoading saved devices...");
        try {
            facade.reset();
            List<SmartDevice> loadedDevices = persistenceService.loadDevicesDirectly();
            if (!loadedDevices.isEmpty()) {
                for (SmartDevice device : loadedDevices) {
                    try {
                        facade.loadDevice(device);
                    } catch (Exception e) {
                        printWarning("Could not load device " + device.getName() + ": " + e.getMessage());
                    }
                }
                printSuccess(String.format("Loaded %d device(s)", facade.getDevices().size()));
            } else {
                System.out.println("  No saved devices found.");
            }
        } catch (Exception e) {
            printError("Could not load devices: " + e.getMessage());
        }
    }

    private void askAndStartConcurrentServices() {
        System.out.print("\nStart concurrent services? (y/n): ");
        String response = scanner.nextLine().trim().toLowerCase();

        if (response.equals("y") || response.equals("yes")) {
            System.out.println("\nStarting concurrent services...");
            concurrentCommands.processConcurrentCommand("events start");
            printSuccess("Event system started");
            concurrentCommands.processConcurrentCommand("monitor start");
            printSuccess("Device monitoring started");
            concurrentCommands.processConcurrentCommand("automate start");
            printSuccess("Automation engine started");
            concurrentCommands.processConcurrentCommand("power monitor start");
            printSuccess("Power monitoring started");
        }
    }

    private void runCommandLoop() {
        while (running) {
            try {
                // Print prompt
                System.out.print(getPrompt());

                // Read command
                String input = scanner.nextLine().trim();
                if (input.isEmpty()) continue;

                // Parse and execute command
                String[] parts = input.split("\\s+", 3);
                String command = parts[0].toLowerCase();

                if (command.equals("exit") || command.equals("quit")) {
                    handleExit();
                    break;
                } else if (command.equals("gui")) {
                    launchGUI();
                    // Note: CLI continues running, GUI runs independently
                } else {
                    executeCommand(parts);
                }

            } catch (Exception e) {
                printError("Error: " + e.getMessage());
            }
        }
    }

    private void executeCommand(String[] parts) throws SmartHomeException {
        String command = parts[0].toLowerCase();


        if (concurrentCommands.processConcurrentCommand(String.join(" ", parts))) {
            return;
        }


        switch (command) {
            case "help" -> printHelp();
            case "add" -> handleAdd(parts);
            case "remove" -> handleRemove(parts);
            case "list", "ls" -> handleList();
            case "on", "off" -> handleToggle(parts);
            case "set" -> handleSet(parts);
            case "report" -> handleReport();
            case "save" -> handleSave();
            case "load" -> handleLoad();
            case "reset" -> handleReset();
            case "stats" -> handleStats(parts);
            case "history" -> handleHistory();
            case "db" -> handleDatabaseCommand(parts);
            case "automate" -> handleAutomation(parts);
            case "debug" -> debugTest();
            default -> printError("Unknown command: " + command + ". Type 'help' for available commands.");
        }
    }

    private void handleAutomation(String[] parts) throws SmartHomeException {
        if (parts.length < 2) {
            printError("Usage: automate <command>");
            printAutomationHelp();
            return;
        }

        String subCommand = parts[1].toLowerCase();

        switch (subCommand) {
            case "rules" -> showAvailableRules();
            case "run" -> runSpecificRule(parts);
            case "light" -> runLightAutomation();
            case "status" -> showAutomationStatus();
            default -> {
                printError("Unknown automate command: " + subCommand);
                printAutomationHelp();
            }
        }
    }

    private void showAvailableRules() {
        System.out.println("\n" + BOLD + "Available Automation Rules:" + RESET);
        System.out.println("─".repeat(80));

        System.out.println(CYAN + "1. Light Automation Rule" + RESET);
        System.out.println("   • Turns on all lights that are currently off");
        System.out.println("   • Usage: automate light  OR  automate run light");
        System.out.println();

        System.out.println(CYAN + "2. Custom Rules (Coming Soon)" + RESET);
        System.out.println("   • Temperature automation");
        System.out.println("   • Security automation");
        System.out.println("   • Energy saving automation");

        System.out.println("─".repeat(80));
        System.out.println("Note: Your system uses the Visitor pattern for automation rules.");
        System.out.println("Rules are applied to all devices in the system automatically.");
    }

    private void runSpecificRule(String[] parts) throws SmartHomeException {
        if (parts.length < 3) {
            printError("Usage: automate run <rule_name>");
            System.out.println("Available rules: light");
            return;
        }

        String ruleName = parts[2].toLowerCase();

        switch (ruleName) {
            case "light" -> runLightAutomation();
            default -> printError("Unknown automation rule: " + ruleName + ". Available: light");
        }
    }

    private void runLightAutomation() throws SmartHomeException {
        System.out.println("\n" + BLUE + "🔄 Running Light Automation Rule..." + RESET);

        List<SmartDevice> devices = facade.getDevices();
        if (devices.isEmpty()) {
            printWarning("No devices in system to automate");
            return;
        }

        // Count lights before automation
        long lightsBefore = devices.stream()
                .filter(d -> d instanceof com.fatty.smarthome.devices.Light && d.isOn())
                .count();

        // Use the existing automation system
        String result = facade.smartHomeAccess("automate", "", "");

        // Count lights after automation
        devices = facade.getDevices(); // Refresh the list
        long lightsAfter = devices.stream()
                .filter(d -> d instanceof com.fatty.smarthome.devices.Light && d.isOn())
                .count();

        // Show results
        System.out.println("─".repeat(50));
        System.out.println("Automation Results:");
        System.out.println("  Lights ON before: " + lightsBefore);
        System.out.println("  Lights ON after:  " + lightsAfter);
        System.out.println("  Lights turned on:  " + (lightsAfter - lightsBefore));
        System.out.println("─".repeat(50));

        printSuccess("Light automation completed: " + result);
    }

    private void showAutomationStatus() {
        System.out.println("\n" + BOLD + "Automation System Status:" + RESET);
        System.out.println("─".repeat(80));

        List<SmartDevice> devices = facade.getDevices();
        if (devices.isEmpty()) {
            System.out.println("No devices in the system.");
            return;
        }

        // Analyze device states
        long totalDevices = devices.size();
        long activeDevices = devices.stream().filter(SmartDevice::isOn).count();

        // Count by type
        long lights = devices.stream().filter(d -> d instanceof com.fatty.smarthome.devices.Light).count();
        long lightsOn = devices.stream()
                .filter(d -> d instanceof com.fatty.smarthome.devices.Light && d.isOn()).count();

        long thermostats = devices.stream().filter(d -> d instanceof com.fatty.smarthome.devices.Thermostat).count();
        long thermostatsOn = devices.stream()
                .filter(d -> d instanceof com.fatty.smarthome.devices.Thermostat && d.isOn()).count();

        long cameras = devices.stream().filter(d -> d instanceof com.fatty.smarthome.devices.SecurityCamera).count();
        long camerasOn = devices.stream()
                .filter(d -> d instanceof com.fatty.smarthome.devices.SecurityCamera && d.isOn()).count();

        System.out.println("Overall Status:");
        System.out.println("  Total devices: " + totalDevices);
        System.out.println("  Active devices: " + activeDevices + " (" +
                String.format("%.1f%%", (activeDevices * 100.0 / totalDevices)) + ")");
        System.out.println();

        System.out.println("Device Breakdown:");
        if (lights > 0) {
            System.out.println("  Lights: " + lightsOn + "/" + lights + " active");
        }
        if (thermostats > 0) {
            System.out.println("  Thermostats: " + thermostatsOn + "/" + thermostats + " active");
        }
        if (cameras > 0) {
            System.out.println("  Cameras: " + camerasOn + "/" + cameras + " active");
        }

        System.out.println();
        System.out.println("Automation Recommendations:");
        if (lightsOn == 0 && lights > 0) {
            System.out.println("  💡 Consider running: automate light");
        }
        if (camerasOn < cameras) {
            System.out.println("  🔒 Some security cameras are off");
        }
        if (activeDevices == 0) {
            System.out.println("  ⚡ All devices are off - good for energy saving!");
        }

        System.out.println("─".repeat(80));
    }

    private void printAutomationHelp() {
        System.out.println("\n" + BOLD + "Automation Commands:" + RESET);
        System.out.println("─".repeat(50));
        System.out.println("  automate rules           - Show available automation rules");
        System.out.println("  automate light           - Run light automation (quick)");
        System.out.println("  automate run light       - Run light automation (detailed)");
        System.out.println("  automate status          - Show system automation status");
        System.out.println("─".repeat(50));
        System.out.println("Note: Uses your existing LightAutomationRule with Visitor pattern");
    }

    /**
     * Launch JavaFX GUI independently
     */
    private void launchGUI() {
        System.out.println("\n" + BLUE + "=== Launching JavaFX GUI ===" + RESET);
        System.out.println("Starting graphical interface...");

        try {
            // Check if JavaFX is available
            Class.forName("javafx.application.Application");

            // Launch GUI in separate thread
            executorService.submit(() -> {
                try {
                    // Save current state before launching GUI
                    List<SmartDevice> devices = facade.getDevices();
                    if (!devices.isEmpty()) {
                        persistenceService.saveAllDevices(devices);
                        System.out.println(GREEN + "✅ State saved before GUI launch" + RESET);
                    }

                    // Launch the GUI
                    System.out.println(GREEN + "✅ Launching GUI application..." + RESET);
                    Application.launch(SmartHomeGUI.class);

                } catch (Exception e) {
                    printError("Failed to launch GUI: " + e.getMessage());
                }
            });

            System.out.println(GREEN + "✅ GUI launch initiated" + RESET);
            System.out.println("Note: CLI remains active. You can continue using commands here.");
            System.out.println("Both CLI and GUI will work with the same Smart Home system.\n");

        } catch (ClassNotFoundException e) {
            printError("JavaFX not found. Please ensure JavaFX is installed and in your classpath.");
            printError("Available modules: java.base, java.desktop");
            printError("Required: javafx.controls, javafx.fxml");
        } catch (Exception e) {
            printError("Failed to launch GUI: " + e.getMessage());
        }
    }

    private void handleAdd(String[] parts) throws SmartHomeException {
        if (parts.length < 3) {
            printError("Usage: add <device_name> <device_type>");
            printError("Types: light, thermostat, camera");
            return;
        }

        String deviceName = parts[1];
        String deviceType = parts[2].toLowerCase();

        SmartDevice device = switch (deviceType) {
            case "light" -> new Light(deviceName);
            case "thermostat" -> new Thermostat(deviceName);
            case "camera" -> new SecurityCamera(deviceName);
            default -> {
                printError("Invalid device type. Available: light, thermostat, camera");
                yield null;
            }
        };

        if (device != null) {
            facade.addDevice(device);
            printSuccess(String.format("Device '%s' added successfully", deviceName));

            // Log the action
            persistenceService.logDeviceAction(device, "ADDED");

            // Ask to turn on
            System.out.print("Turn on the device now? (y/n): ");
            String response = scanner.nextLine().trim().toLowerCase();
            if (response.equals("y") || response.equals("yes")) {
                device.turnOn();
                printSuccess("Device turned on");
            }
        }
    }

    private void handleRemove(String[] parts) {
        if (parts.length < 2) {
            printError("Usage: remove <device_name>");
            return;
        }

        String deviceName = parts[1];
        if (facade.removeDevice(deviceName)) {
            printSuccess(String.format("Device '%s' removed", deviceName));
        } else {
            printError(String.format("Device '%s' not found", deviceName));
        }
    }

    private void handleList() {
        List<SmartDevice> devices = facade.getDevices();
        if (devices.isEmpty()) {
            System.out.println("No devices in the system.");
            return;
        }

        System.out.println("\n" + BOLD + "Device List:" + RESET);
        System.out.println("─".repeat(80));

        // Group by type
        Map<String, List<SmartDevice>> devicesByType = new HashMap<>();
        for (SmartDevice device : devices) {
            String type = device.getClass().getSimpleName();
            devicesByType.computeIfAbsent(type, k -> new ArrayList<>()).add(device);
        }

        // Display by type
        for (Map.Entry<String, List<SmartDevice>> entry : devicesByType.entrySet()) {
            System.out.println("\n" + CYAN + entry.getKey() + "s:" + RESET);
            for (SmartDevice device : entry.getValue()) {
                String status = device.isOn() ? GREEN + "ON " + RESET : RED + "OFF" + RESET;
                System.out.printf("  %-25s %-15s %s   %s\n",
                        device.getName(),
                        device.getClass().getSimpleName(),
                        status,
                        device.getStatus());
            }
        }

        System.out.println("─".repeat(80));

        // Summary
        long activeCount = devices.stream().filter(SmartDevice::isOn).count();
        System.out.printf("Total: %d device(s) | Active: %d\n", devices.size(), activeCount);
    }

    private void handleToggle(String[] parts) throws SmartHomeException {
        if (parts.length < 2) {
            printError("Usage: " + parts[0] + " <device_name>");
            return;
        }

        String deviceName = parts[1];
        boolean turnOn = parts[0].equals("on");

        Optional<SmartDevice> deviceOpt = facade.getDevice(deviceName);
        if (deviceOpt.isPresent()) {
            SmartDevice device = deviceOpt.get();
            if (turnOn) {
                device.turnOn();
            } else {
                device.turnOff();
            }
            printSuccess(String.format("Device '%s' turned %s", deviceName, turnOn ? "ON" : "OFF"));

            // Log the action
            persistenceService.logDeviceAction(device, turnOn ? "TURNED_ON" : "TURNED_OFF");
        } else {
            printError(String.format("Device '%s' not found", deviceName));
        }
    }

    private void handleSet(String[] parts) throws SmartHomeException {
        if (parts.length < 3) {
            printError("Usage: set <thermostat_name> <temperature>");
            return;
        }

        String deviceName = parts[1];
        Optional<SmartDevice> deviceOpt = facade.getDevice(deviceName);

        if (deviceOpt.isPresent() && deviceOpt.get() instanceof Thermostat) {
            try {
                int temp = Integer.parseInt(parts[2]);
                Thermostat thermostat = (Thermostat) deviceOpt.get();
                thermostat.setTemperature(temp);
                printSuccess(String.format("Temperature set to %d°C", temp));

                // Log the action
                persistenceService.logDeviceAction(thermostat, "TEMPERATURE_SET");
            } catch (NumberFormatException e) {
                printError("Invalid temperature value");
            }
        } else {
            printError("Thermostat not found: " + deviceName);
        }
    }

    private void handleReport() throws SmartHomeException {
        String report = facade.smartHomeAccess("report", "", "");
        System.out.println("\n" + BOLD + "System Report:" + RESET);
        System.out.println("─".repeat(80));
        System.out.println(report);
        System.out.println("─".repeat(80));
    }

    private void handleSave() throws SmartHomeException {
        System.out.println("Saving devices...");
        List<SmartDevice> devices = facade.getDevices();

        if (devices.isEmpty()) {
            printWarning("No devices to save");
            return;
        }

        try {
            if (persistenceService.isDatabaseAvailable()) {
                persistenceService.saveDeviceStatesToDatabase(devices);
                printSuccess("Saved to database");
            }
            persistenceService.saveDeviceStatesBinary(devices);
            printSuccess("Saved to binary file");
            persistenceService.saveDeviceStatesJson(devices);
            printSuccess("Saved to JSON file");

            printSuccess(String.format("Saved %d device(s)", devices.size()));
        } catch (Exception e) {
            printError("Save failed: " + e.getMessage());
        }
    }

    private void handleLoad() {
        System.out.println("Loading devices...");
        facade.reset();
        loadDevices();
    }

    private void handleReset() {
        System.out.print("Are you sure you want to reset the system? (y/n): ");
        String response = scanner.nextLine().trim().toLowerCase();

        if (response.equals("y") || response.equals("yes")) {
            facade.reset();
            printSuccess("System reset complete");
        } else {
            System.out.println("Reset cancelled");
        }
    }

    private void handleStats(String[] parts) {
        if (parts.length < 2) {
            // Show overall stats
            String summary = persistenceService.getSystemSummary();
            System.out.println("\n" + BOLD + "System Statistics:" + RESET);
            System.out.println("─".repeat(80));
            System.out.println(summary);
            System.out.println("─".repeat(80));
        } else {
            // Show device-specific stats
            String deviceName = parts[1];
            String stats = persistenceService.getDeviceStatistics(deviceName);
            System.out.println("\n" + BOLD + "Device Statistics:" + RESET);
            System.out.println("─".repeat(80));
            System.out.println(stats);
            System.out.println("─".repeat(80));
        }
    }

    private void handleHistory() throws SmartHomeException {
        String history = facade.smartHomeAccess("history", "", "");
        System.out.println("\n" + BOLD + "Command History:" + RESET);
        System.out.println("─".repeat(80));
        System.out.println(history);
        System.out.println("─".repeat(80));
    }

    private void handleDatabaseCommand(String[] parts) {
        if (parts.length < 2) {
            printError("Usage: db <status|primary|file>");
            return;
        }

        switch (parts[1].toLowerCase()) {
            case "status" -> {
                boolean available = persistenceService.isDatabaseAvailable();
                System.out.println("Database status: " + (available ? GREEN + "AVAILABLE" : RED + "UNAVAILABLE") + RESET);
            }
            case "primary" -> {
                persistenceService.setUseDatabasePrimary(true);
                printSuccess("Database set as primary storage");
            }
            case "file" -> {
                persistenceService.setUseDatabasePrimary(false);
                printSuccess("File storage set as primary");
            }
            default -> printError("Unknown database command: " + parts[1]);
        }
    } // DEBUG things
    private void debugTest() {
        System.out.println("DEBUG: Testing automation methods...");

        try {
            // Test if the methods exist
            System.out.println("DEBUG: handleAutomation method exists: " +
                    (this.getClass().getDeclaredMethod("handleAutomation", String[].class) != null));
            System.out.println("DEBUG: showAvailableRules method exists: " +
                    (this.getClass().getDeclaredMethod("showAvailableRules") != null));
            System.out.println("DEBUG: automation methods loaded successfully");
        } catch (NoSuchMethodException e) {
            System.out.println("DEBUG: Missing automation methods - " + e.getMessage());
        }
    }

    private void handleExit() throws SmartHomeException {
        System.out.println("\nShutting down Smart Home System...");

        // Save devices
        List<SmartDevice> devices = facade.getDevices();
        if (!devices.isEmpty()) {
            System.out.println("Saving devices...");
            persistenceService.saveAllDevices(devices);
            printSuccess(String.format("Saved %d device(s)", devices.size()));
        }

        // Stop services
        System.out.println("Stopping services...");
        concurrentCommands.shutdown();

        // Shutdown executor
        executorService.shutdown();
        try {
            if (!executorService.awaitTermination(5, TimeUnit.SECONDS)) {
                executorService.shutdownNow();
            }
        } catch (InterruptedException e) {
            executorService.shutdownNow();
        }

        running = false;
        printSuccess("Thank you for using Smart Home System. Goodbye!");
    }

    private void showCommandLineHelp() {
        System.out.println("Smart Home System - Command Line Options");
        System.out.println("Usage: java SmartHomeCLI [options]");
        System.out.println("\nAdvanced Options (optional):");
        System.out.println("  --cli              Start directly in CLI mode");
        System.out.println("  --gui, -g          Start directly in GUI mode");
        System.out.println("  --no-color         Disable colored output");
        System.out.println("  --help, -h         Show this help");
        System.out.println("\nDefault Behavior:");
        System.out.println("  No options: Show user-friendly interface selection menu");
        System.out.println("\nRecommended for beginners:");
        System.out.println("  Just run: java SmartHomeCLI");
        System.out.println("  Then choose from the easy menu options!");
        System.out.println("\nExamples:");
        System.out.println("  java SmartHomeCLI                    # Show interface menu (recommended)");
        System.out.println("  java SmartHomeCLI --cli              # Direct CLI start");
        System.out.println("  java SmartHomeCLI --gui              # Direct GUI start");
        System.out.println("  java SmartHomeCLI --no-color         # Menu without colors");
    }

    private void printHelp() {
        System.out.println("\n" + BOLD + "Available Commands:" + RESET);
        System.out.println("─".repeat(80));

        System.out.println(CYAN + "Basic Commands:" + RESET);
        System.out.println("  add <name> <type>       - Add a new device (types: light, thermostat, camera)");
        System.out.println("  remove <name>           - Remove a device");
        System.out.println("  list                    - List all devices");
        System.out.println("  on <name>               - Turn device on");
        System.out.println("  off <name>              - Turn device off");
        System.out.println("  set <name> <temp>       - Set thermostat temperature");
        System.out.println("  report                  - Generate system report");
        System.out.println("  gui                     - Launch JavaFX GUI");

        System.out.println("\n" + CYAN + "Data Management:" + RESET);
        System.out.println("  save                    - Save current state");
        System.out.println("  load                    - Load saved state");
        System.out.println("  reset                   - Reset system");
        System.out.println("  history                 - Show command history");
        System.out.println("  stats [device]          - Show statistics");

        System.out.println("\n" + CYAN + "Database Commands:" + RESET);
        System.out.println("  db status               - Check database status");
        System.out.println("  db primary              - Use database as primary storage");
        System.out.println("  db file                 - Use file as primary storage");

        System.out.println("\n" + CYAN + "Automation Commands:" + RESET);
        System.out.println("  automate rules          - Show available automation rules");
        System.out.println("  automate light          - Run light automation (quick)");
        System.out.println("  automate run light      - Run light automation (detailed)");
        System.out.println("  automate status         - Show automation system status");

        // Add concurrent commands help
        System.out.println(ConcurrentCLICommands.getConcurrentHelp());

        System.out.println("\n" + CYAN + "System Commands:" + RESET);
        System.out.println("  help                    - Show this help");
        System.out.println("  exit                    - Exit the program");

        System.out.println("─".repeat(80));
    }

    private String getPrompt() {
        String storageType = persistenceService.isDatabaseAvailable() ? "DB" : "FILE";
        List<SmartDevice> devices = facade.getDevices();
        long activeCount = devices.stream().filter(SmartDevice::isOn).count();

        return String.format(PROMPT_FORMAT, storageType,  devices.size(), activeCount);
    }

    private void printSuccess(String message) {
        System.out.println((useColors ? GREEN : "") + "  ✓ " + message + (useColors ? RESET : ""));
    }

    private void printError(String message) {
        System.out.println((useColors ? RED : "") + "  ✗ " + message + (useColors ? RESET : ""));
    }

    private void printWarning(String message) {
        System.out.println((useColors ? YELLOW : "") + "  ⚠ " + message + (useColors ? RESET : ""));
    }
}