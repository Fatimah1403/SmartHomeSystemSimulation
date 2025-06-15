package com.fatty.smarthome.cli;

import com.fatty.smarthome.core.*;
import com.fatty.smarthome.devices.*;
import com.fatty.smarthome.util.SmartHomeException;
import com.fatty.smarthome.core.DeviceState;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.reflect.TypeToken;

import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.lang.reflect.Type;
import java.sql.SQLException;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.*;
import java.util.stream.Collectors;

import static com.fatty.smarthome.Main.colorize;

/**
 * Enhanced Command Line Interface for Smart Home System
 * Integrates database, concurrent features and event-driven operations
 */
public class SmartHomeCLI {
    // ANSI color codes for enhanced display
    private static final String RESET = "\u001B[0m";
    private static final String GREEN = "\u001B[32m";
    private static final String RED = "\u001B[31m";
    private static final String YELLOW = "\u001B[33m";
    private static final String BLUE = "\u001B[34m";
    private static final String CYAN = "\u001B[36m";
    private static final String PURPLE = "\u001B[35m";

    private final FacadeSmartHome facade;
    private final Scanner scanner;
    private final ConcurrentCLICommands concurrentCommands;
    private final PersistenceService persistenceService;
    private final List<String> commandHistory = new ArrayList<>();
    private final Map<String, Integer> commandStats = new HashMap<>();

    private boolean running = true;
    private boolean colorEnabled = true;
    private boolean verboseMode = false;

    private static final int MAX_HISTORY = 100;

    public SmartHomeCLI() throws SmartHomeException, SQLException {
        this.facade = FacadeSmartHome.getTheInstance();
        this.scanner = new Scanner(System.in);
        this.concurrentCommands = new ConcurrentCLICommands(facade);
        try {
            this.persistenceService = new PersistenceService();
        } catch (SQLException e) {
            throw new SmartHomeException("Failed to initialize persistence service", e);
        }
        // Add shutdown hook to save on unexpected exit
        Runtime.getRuntime().addShutdownHook(new Thread(() -> {
            try {
                List<SmartDevice> devices = facade.getDevices();
                if (!devices.isEmpty()) {
                    // Try to save to all formats
                    if (persistenceService.isDatabaseAvailable()) {
                        persistenceService.saveDeviceStatesToDatabase(devices);
                    }
                    persistenceService.saveDeviceStatesJson(devices);
                    persistenceService.saveDeviceStatesBinary(devices);
                    System.out.println("\n✅ Emergency save completed");
                }
            } catch (Exception e) {
                System.err.println("\n⚠️ Warning: Could not save devices on shutdown");
            }
        }));
    }

    /**
     * Print welcome banner
     */
    private void printBanner() {
        System.out.println(colorize(CYAN + """
                ╔════════════════════════════════════════╗
                ║     SMART HOME CONTROL SYSTEM          ║
                ║          Version 3.0                   ║
                ║                                        ║
                ╚════════════════════════════════════════╝
                """ + RESET));
    }

    /**
     * Start the CLI
     */
    public void start() {
        printBanner();

        // Show database status
        checkDatabaseStatus();

        // Load saved devices
        loadDevices();

        // Start concurrent services
        System.out.print("\nStart concurrent services? (y/n): ");
        if (scanner.nextLine().trim().toLowerCase().startsWith("y")) {
            startConcurrentServices();
        }

        System.out.println("\nType 'help' for commands or 'exit' to quit.\n");

        // Main command loop
        while (running) {
            displayPrompt();
            String input = scanner.nextLine().trim();

            if (!input.isEmpty()) {
                addToHistory(input);
                trackCommand(input.split("\\s+")[0]);
                processCommand(input);
            }
        }

        scanner.close();
    }

    /**
     * Check database status
     */
    private void checkDatabaseStatus() {
        System.out.println("\nChecking database connection...");
        if (persistenceService.isDatabaseAvailable()) {
            System.out.println(colorize(GREEN + "✅ Database connected (primary storage)" + RESET));
        } else {
            System.out.println(colorize(YELLOW + "⚠️  Database unavailable (using file storage)" + RESET));
        }
    }

    /**
     * Start concurrent services
     */
    private void startConcurrentServices() {
        System.out.println("\nStarting concurrent services...");
        concurrentCommands.processConcurrentCommand("events start");
        System.out.println(colorize(GREEN + "  ✓ Event system started" + RESET));

        concurrentCommands.processConcurrentCommand("monitor start");
        System.out.println(colorize(GREEN + "  ✓ Device monitoring started" + RESET));

        concurrentCommands.processConcurrentCommand("automate start");
        System.out.println(colorize(GREEN + "  ✓ Automation engine started" + RESET));

        concurrentCommands.processConcurrentCommand("power monitor start");
        System.out.println(colorize(GREEN + "  ✓ Power monitoring started" + RESET));
    }

    /**
     * Load saved devices from storage
     */
    private void loadDevices() {
        try {
            System.out.println("\nLoading saved devices...");

            // Try database first if available
            List<DeviceState> states;
            if (persistenceService.isDatabaseAvailable()) {
                states = persistenceService.loadDeviceStatesFromDatabase();
                if (!states.isEmpty()) {
                    System.out.println("  Loading from database...");
                } else {
                    // Fallback to file
                    states = persistenceService.loadDeviceStatesJson();
                    if (!states.isEmpty()) {
                        System.out.println("  Loading from JSON file...");
                    }
                }
            } else {
                // Load from files
                states = persistenceService.loadDeviceStatesJson();
                if (states.isEmpty()) {
                    states = persistenceService.loadDeviceStatesBinary();
                    if (!states.isEmpty()) {
                        System.out.println("  Loading from binary file...");
                    }
                }
            }

            if (states.isEmpty()) {
                System.out.println("  No saved devices found.");
                return;
            }

            // Reconstruct devices
            int loaded = 0;
            for (DeviceState state : states) {
                try {
                    SmartDevice device = persistenceService.reconstructDevice(state);
                    facade.addDevice(device);
                    loaded++;
                } catch (Exception e) {
                    System.err.println("  Failed to load: " + state.getDeviceName() + " - " + e.getMessage());
                }
            }

            System.out.println(colorize(GREEN + "✅ Loaded " + loaded + " device(s)" + RESET));

        } catch (Exception e) {
            System.err.println(colorize(RED + "Error loading devices: " + e.getMessage() + RESET));
            if (verboseMode) {
                e.printStackTrace();
            }
        }
    }

    /**
     * Display command prompt with status
     */
    private void displayPrompt() {
        try {
            List<SmartDevice> devices = facade.getDevices();
            long activeCount = devices.stream().filter(SmartDevice::isOn).count();

            String dbIndicator = persistenceService.isDatabaseAvailable() ? "DB" : "FILE";
            String status = String.format("[%s|%d/%d active]", dbIndicator, activeCount, devices.size());
            System.out.print(colorize(CYAN + "smart-home " + YELLOW + status + " > " + RESET));
        } catch (Exception e) {
            System.out.print(colorize(CYAN + "smart-home> " + RESET));
        }
    }

    /**
     * Process user commands
     */
    private void processCommand(String input) {
        // First try concurrent commands
        if (concurrentCommands.processConcurrentCommand(input)) {
            return;
        }

        // Parse command
        String[] parts = input.split("\\s+", 2);
        String command = parts[0].toLowerCase();
        String args = parts.length > 1 ? parts[1] : "";

        try {
            switch (command) {
                // Device management
                case "add", "a" -> handleAdd(args);
                case "remove", "rm" -> handleRemove(args);
                case "list", "ls", "l" -> handleList(args);

                // Device control
                case "on" -> handleTurnOn(args);
                case "off" -> handleTurnOff(args);
                case "toggle", "t" -> handleToggle(args);
                case "set" -> handleSet(args);

                // Bulk operations
                case "all-on" -> handleAllOn();
                case "all-off" -> handleAllOff();
                case "automate" -> handleAutomate();

                // Information
                case "status", "s" -> handleStatus(args);
                case "info", "i" -> handleInfo(args);
                case "report", "r" -> handleReport();
                case "analytics" -> handleAnalytics();

                // Data management
                case "save" -> handleSave();
                case "load" -> handleLoad();
                case "export" -> handleExport();
                case "import" -> handleImport();
                case "sync" -> handleSync();

                // System commands
                case "help", "h", "?" -> showHelp();
                case "clear", "cls" -> clearScreen();
                case "color" -> toggleColor();
                case "verbose", "v" -> toggleVerbose();
                case "reset" -> handleReset();
                case "history" -> showHistory();
                case "stats" -> showStats();
                case "exit", "quit", "q" -> handleExit();

                // Additional features
                case "search" -> handleSearch(args);
                case "filter" -> handleFilter(args);
                case "group" -> handleGroup(args);

                default -> System.out.println(colorize(RED + "Unknown command: " + command +
                        ". Type 'help' for available commands." + RESET));
            }
        } catch (Exception e) {
            System.err.println(colorize(RED + "Error: " + e.getMessage() + RESET));
            if (verboseMode) {
                e.printStackTrace();
            }
        }
    }

    // ========== Command Handlers ==========

    private void handleAdd(String args) throws SmartHomeException {
        String[] parts = args.split("\\s+");
        if (parts.length < 2) {
            System.out.println("Usage: add <name> <type> [location]");
            System.out.println("Types: light, thermostat, camera");
            System.out.println("Example: add \"Living Room Light\" light \"Living Room\"");
            return;
        }

        String name = parts[0];
        String type = parts[1].toLowerCase();
        String location = parts.length > 2 ? String.join(" ", Arrays.copyOfRange(parts, 2, parts.length)) : "Unknown";

        // Validate type
        if (!Arrays.asList("light", "thermostat", "camera").contains(type)) {
            throw new SmartHomeException("Invalid device type. Use: light, thermostat, or camera");
        }

        // Add device
        String result = facade.smartHomeAccess("add", name, type);
        System.out.println(colorize(GREEN + "✅ " + result + RESET));

        // Set location if provided
        facade.getDevice(name).ifPresent(device -> {
            // In a real implementation, you'd have a setLocation method
            System.out.println("  Location: " + location);
        });

        // Ask if user wants to turn it on
        System.out.print("Turn on the device now? (y/n): ");
        if (scanner.nextLine().trim().toLowerCase().startsWith("y")) {
            facade.smartHomeAccess("turnon", name, "");
            System.out.println(colorize(GREEN + "✅ Device turned on" + RESET));
        }

        // Auto-save if database is available
        if (persistenceService.isDatabaseAvailable()) {
            persistenceService.saveDeviceStatesToDatabase(facade.getDevices());
        }
    }

    private void handleRemove(String args) throws SmartHomeException {
        if (args.isEmpty()) {
            System.out.println("Usage: remove <name>");
            return;
        }

        Optional<SmartDevice> device = findDevice(args);
        if (device.isEmpty()) {
            System.out.println(colorize(RED + "Device not found: " + args + RESET));
            return;
        }

        System.out.print("Are you sure you want to remove '" + args + "'? (y/n): ");
        if (scanner.nextLine().trim().toLowerCase().startsWith("y")) {
            facade.removeDevice(args);
            System.out.println(colorize(GREEN + "✅ Device removed: " + args + RESET));

            // Auto-save
            if (persistenceService.isDatabaseAvailable()) {
                persistenceService.saveDeviceStatesToDatabase(facade.getDevices());
            }
        } else {
            System.out.println("Remove cancelled.");
        }
    }

    private void handleList(String args) {
        List<SmartDevice> devices = facade.getDevices();

        if (devices.isEmpty()) {
            System.out.println("No devices in the system.");
            return;
        }

        // Apply filter if provided
        if (!args.isEmpty()) {
            devices = filterDevices(devices, args);
        }

        // Print header
        System.out.println("\n" + colorize(CYAN +
                String.format("%-25s %-15s %-10s %-30s", "Name", "Type", "Status", "Details") +
                RESET));
        System.out.println("─".repeat(80));

        // Group by type
        Map<String, List<SmartDevice>> grouped = devices.stream()
                .collect(Collectors.groupingBy(d -> d.getClass().getSimpleName()));

        grouped.forEach((type, deviceList) -> {
            System.out.println(colorize(YELLOW + type + "s:" + RESET));
            deviceList.stream()
                    .sorted(Comparator.comparing(SmartDevice::getName))
                    .forEach(this::printDeviceRow);
        });

        System.out.println("─".repeat(80));
        System.out.println("Total: " + devices.size() + " device(s) | " +
                "Active: " + devices.stream().filter(SmartDevice::isOn).count());
    }

    private void handleTurnOn(String args) throws SmartHomeException {
        if (args.isEmpty()) {
            System.out.println("Usage: on <device-name>");
            return;
        }

        String result = facade.smartHomeAccess("turnon", args, "");
        System.out.println(colorize(GREEN + "✅ " + result + RESET));

        // Log to database if available
        if (persistenceService.isDatabaseAvailable()) {
            // In a real implementation, you'd log this event
        }
    }

    private void handleTurnOff(String args) throws SmartHomeException {
        if (args.isEmpty()) {
            System.out.println("Usage: off <device-name>");
            return;
        }

        String result = facade.smartHomeAccess("turnoff", args, "");
        System.out.println(colorize(YELLOW + "✅ " + result + RESET));
    }

    private void handleToggle(String args) throws SmartHomeException {
        if (args.isEmpty()) {
            System.out.println("Usage: toggle <device-name>");
            return;
        }

        Optional<SmartDevice> device = findDevice(args);
        if (device.isPresent()) {
            String command = device.get().isOn() ? "turnoff" : "turnon";
            String result = facade.smartHomeAccess(command, args, "");
            System.out.println(colorize(GREEN + "✅ " + result + RESET));
        } else {
            System.out.println(colorize(RED + "Device not found: " + args + RESET));
        }
    }

    private void handleSet(String args) throws SmartHomeException {
        String[] parts = args.split("\\s+", 2);
        if (parts.length < 2) {
            System.out.println("Usage: set <thermostat-name> <temperature>");
            return;
        }

        String result = facade.smartHomeAccess("settemp", parts[0], parts[1]);
        System.out.println(colorize(GREEN + "✅ " + result + RESET));
    }

    private void handleAllOn() throws SmartHomeException {
        System.out.println("Turning on all devices...");
        List<SmartDevice> devices = facade.getDevices();
        int count = 0;

        for (SmartDevice device : devices) {
            try {
                facade.smartHomeAccess("turnon", device.getName(), "");
                count++;
            } catch (SmartHomeException e) {
                if (verboseMode) {
                    System.err.println("  Failed: " + device.getName() + " - " + e.getMessage());
                }
            }
        }

        System.out.println(colorize(GREEN + "✅ Turned on " + count + "/" + devices.size() + " devices" + RESET));
    }

    private void handleAllOff() throws SmartHomeException {
        System.out.println("Turning off all devices...");
        List<SmartDevice> devices = facade.getDevices();
        int count = 0;

        for (SmartDevice device : devices) {
            try {
                facade.smartHomeAccess("turnoff", device.getName(), "");
                count++;
            } catch (SmartHomeException e) {
                if (verboseMode) {
                    System.err.println("  Failed: " + device.getName() + " - " + e.getMessage());
                }
            }
        }

        System.out.println(colorize(YELLOW + "✅ Turned off " + count + "/" + devices.size() + " devices" + RESET));
    }

    private void handleAutomate() throws SmartHomeException {
        String result = facade.smartHomeAccess("automate", "", "");
        System.out.println(colorize(GREEN + "✅ " + result + RESET));
    }

    private void handleSave() {
        try {
            List<SmartDevice> devices = facade.getDevices();
            if (devices.isEmpty()) {
                System.out.println("No devices to save.");
                return;
            }

            System.out.println("Saving devices...");

            // Save to database if available
            if (persistenceService.isDatabaseAvailable()) {
                persistenceService.saveDeviceStatesToDatabase(devices);
                System.out.println(colorize(GREEN + "  ✓ Saved to database" + RESET));
            }

            // Always save to files as backup
            persistenceService.saveDeviceStatesBinary(devices);
            System.out.println(colorize(GREEN + "  ✓ Saved to binary file" + RESET));

            persistenceService.saveDeviceStatesJson(devices);
            System.out.println(colorize(GREEN + "  ✓ Saved to JSON file" + RESET));

            System.out.println(colorize(GREEN + "✅ Saved " + devices.size() + " device(s)" + RESET));
        } catch (Exception e) {
            System.err.println(colorize(RED + "Failed to save devices: " + e.getMessage() + RESET));
        }
    }

    private void handleLoad() {
        try {
            System.out.print("This will replace all current devices. Continue? (y/n): ");
            if (!scanner.nextLine().trim().toLowerCase().startsWith("y")) {
                System.out.println("Load cancelled.");
                return;
            }

            // Stop services
            concurrentCommands.processConcurrentCommand("services stop");

            // Reset system
            facade.reset();
            System.out.println("✅ System reset");

            // Load devices
            loadDevices();

            // Restart services
            System.out.print("Restart concurrent services? (y/n): ");
            if (scanner.nextLine().trim().toLowerCase().startsWith("y")) {
                startConcurrentServices();
            }

        } catch (Exception e) {
            System.err.println(colorize(RED + "Failed to load devices: " + e.getMessage() + RESET));
        }
    }

    private void handleSync() {
        if (!persistenceService.isDatabaseAvailable()) {
            System.out.println(colorize(YELLOW + "Database not available. Nothing to sync." + RESET));
            return;
        }

        try {
            System.out.println("Syncing with database...");

            // Load from database
            List<DeviceState> dbStates = persistenceService.loadDeviceStatesFromDatabase();

            // Compare with current state
            List<SmartDevice> currentDevices = facade.getDevices();

            System.out.println("Database devices: " + dbStates.size());
            System.out.println("Current devices: " + currentDevices.size());

            System.out.print("Sync from database? (y/n): ");
            if (scanner.nextLine().trim().toLowerCase().startsWith("y")) {
                facade.reset();
                for (DeviceState state : dbStates) {
                    SmartDevice device = persistenceService.reconstructDevice(state);
                    facade.addDevice(device);
                }
                System.out.println(colorize(GREEN + "✅ Synced from database" + RESET));
            }
        } catch (Exception e) {
            System.err.println(colorize(RED + "Sync failed: " + e.getMessage() + RESET));
        }
    }

    private void handleExport() {
        try {
            System.out.print("Enter filename (default: smart_home_export.json): ");
            String filename = scanner.nextLine().trim();
            if (filename.isEmpty()) {
                filename = "smart_home_export.json";
            }

            List<SmartDevice> devices = facade.getDevices();
            if (devices.isEmpty()) {
                System.out.println("No devices to export.");
                return;
            }

            // Create detailed export
            Map<String, Object> exportData = new HashMap<>();
            exportData.put("export_date", LocalDateTime.now().toString());
            exportData.put("version", "2.0");
            exportData.put("device_count", devices.size());

            List<Map<String, Object>> deviceData = new ArrayList<>();
            for (SmartDevice device : devices) {
                Map<String, Object> deviceInfo = new HashMap<>();
                deviceInfo.put("name", device.getName());
                deviceInfo.put("type", device.getClass().getSimpleName());
                deviceInfo.put("status", device.isOn() ? "ON" : "OFF");
                deviceInfo.put("details", device.getStatus());

                if (device instanceof Thermostat) {
                    deviceInfo.put("temperature", ((Thermostat) device).getTemperature());
                }

                deviceData.add(deviceInfo);
            }
            exportData.put("devices", deviceData);

            // Write to file
            try (FileWriter writer = new FileWriter(filename)) {
                Gson gson = new GsonBuilder().setPrettyPrinting().create();
                gson.toJson(exportData, writer);
            }

            System.out.println(colorize(GREEN + "✅ Exported " + devices.size() +
                    " device(s) to " + filename + RESET));

        } catch (Exception e) {
            System.err.println(colorize(RED + "Export failed: " + e.getMessage() + RESET));
        }
    }

    private void handleImport() {
        try {
            System.out.print("Enter filename to import: ");
            String filename = scanner.nextLine().trim();

            if (filename.isEmpty()) {
                System.out.println("Import cancelled.");
                return;
            }

            File file = new File(filename);
            if (!file.exists()) {
                System.out.println(colorize(RED + "File not found: " + filename + RESET));
                return;
            }

            System.out.print("This will add to existing devices. Continue? (y/n): ");
            if (!scanner.nextLine().trim().toLowerCase().startsWith("y")) {
                System.out.println("Import cancelled.");
                return;
            }

            // Read and parse file
            Map<String, Object> importData;
            try (FileReader reader = new FileReader(file)) {
                Gson gson = new Gson();
                Type mapType = new TypeToken<Map<String, Object>>() {}.getType();
                importData = gson.fromJson(reader, mapType);
            }

            // Extract devices
            List<Map<String, Object>> deviceList = (List<Map<String, Object>>) importData.get("devices");
            if (deviceList == null || deviceList.isEmpty()) {
                System.out.println("No devices found in file.");
                return;
            }

            // Import devices
            int imported = 0;
            for (Map<String, Object> deviceInfo : deviceList) {
                try {
                    String name = (String) deviceInfo.get("name");
                    String type = (String) deviceInfo.get("type");

                    // Convert type to facade format
                    String deviceType = type.toLowerCase().replace("smart", "").replace("device", "");

                    facade.smartHomeAccess("add", name, deviceType);

                    // Restore state
                    if ("ON".equals(deviceInfo.get("status"))) {
                        facade.smartHomeAccess("turnon", name, "");
                    }

                    // Restore temperature for thermostats
                    if (deviceInfo.containsKey("temperature")) {
                        Double temp = (Double) deviceInfo.get("temperature");
                        facade.smartHomeAccess("settemp", name, String.valueOf(temp.intValue()));
                    }

                    imported++;
                } catch (Exception e) {
                    System.err.println("Failed to import device: " + e.getMessage());
                }
            }

            System.out.println(colorize(GREEN + "✅ Imported " + imported + " device(s)" + RESET));

        } catch (Exception e) {
            System.err.println(colorize(RED + "Import failed: " + e.getMessage() + RESET));
        }
    }

    private void handleStatus(String args) {
        if (args.isEmpty()) {
            // Show system status
            System.out.println("\n=== SYSTEM STATUS ===");
            concurrentCommands.processConcurrentCommand("services");

            // Show database status
            System.out.println("\nStorage: " +
                    (persistenceService.isDatabaseAvailable() ? "Database (Primary)" : "File-based"));

        } else {
            // Show specific device status
            Optional<SmartDevice> device = findDevice(args);
            if (device.isPresent()) {
                SmartDevice d = device.get();
                System.out.println("\n=== DEVICE STATUS ===");
                System.out.println("Name: " + d.getName());
                System.out.println("Type: " + d.getClass().getSimpleName());
                System.out.println("Power: " + (d.isOn() ? colorize(GREEN + "ON" + RESET) : colorize(RED + "OFF" + RESET)));
                System.out.println("Status: " + d.getStatus());

                // Show monitoring stats if available
                concurrentCommands.processConcurrentCommand("monitor stats " + args);
            } else {
                System.out.println(colorize(RED + "Device not found: " + args + RESET));
            }
        }
    }

    private void handleInfo(String args) {
        if (args.isEmpty()) {
            System.out.println("Usage: info <device-name>");
            return;
        }

        Optional<SmartDevice> device = findDevice(args);
        if (device.isPresent()) {
            SmartDevice d = device.get();
            System.out.println("\n=== DEVICE INFORMATION ===");
            System.out.println("Name: " + d.getName());
            System.out.println("Type: " + d.getClass().getSimpleName());
            System.out.println("Status: " + (d.isOn() ? colorize(GREEN + "ON" + RESET) : colorize(RED + "OFF" + RESET)));
            System.out.println("Details: " + d.getStatus());

            // Device-specific info
            if (d instanceof Thermostat) {
                Thermostat t = (Thermostat) d;
                System.out.println("Current Temperature: " + t.getTemperature() + "°C");
                System.out.println("Temperature Range: 10-32°C");
            } else if (d instanceof Light) {
                System.out.println("Light Type: LED");
                System.out.println("Brightness: " + (d.isOn() ? "100%" : "0%"));
            } else if (d instanceof SecurityCamera) {
                System.out.println("Recording: " + (d.isOn() ? "Active" : "Inactive"));
                System.out.println("Motion Detection: Enabled");
            }

            // Show last update time (if tracking)
            System.out.println("Last Updated: " + LocalDateTime.now().format(
                    DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss")));

        } else {
            System.out.println(colorize(RED + "Device not found: " + args + RESET));
        }
    }

    private void handleReport() {
        System.out.println("\n=== SMART HOME SYSTEM REPORT ===");
        System.out.println("Generated: " + LocalDateTime.now().format(
                DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss")));
        System.out.println("─".repeat(50));

        // Device statistics
        List<SmartDevice> devices = facade.getDevices();
        System.out.println("\nDEVICE STATISTICS:");
        System.out.println("Total devices: " + devices.size());
        System.out.println("Active devices: " + devices.stream().filter(SmartDevice::isOn).count());
        System.out.println("Inactive devices: " + devices.stream().filter(d -> !d.isOn()).count());

        // Count by type
        Map<String, Long> typeCounts = devices.stream()
                .collect(Collectors.groupingBy(
                        d -> d.getClass().getSimpleName(),
                        Collectors.counting()
                ));

        System.out.println("\nDEVICES BY TYPE:");
        typeCounts.forEach((type, count) ->
                System.out.println("  " + type + ": " + count));

        // Power consumption (simulated)
        System.out.println("\nPOWER CONSUMPTION:");
        concurrentCommands.processConcurrentCommand("power stats");

        // Concurrent services status
        System.out.println("\nCONCURRENT SERVICES:");
        concurrentCommands.processConcurrentCommand("services");

        // Storage status
        System.out.println("\nSTORAGE:");
        System.out.println("Primary: " + (persistenceService.isDatabaseAvailable() ? "Database" : "File"));
        System.out.println("Backup: JSON and Binary files");

        System.out.println("─".repeat(50));
    }

    private void handleAnalytics() {
        System.out.println("\n=== DEVICE ANALYTICS ===");
        List<SmartDevice> devices = facade.getDevices();

        if (devices.isEmpty()) {
            System.out.println("No devices to analyze.");
            return;
        }

        // Activity analysis
        long activeCount = devices.stream().filter(SmartDevice::isOn).count();
        double activePercentage = (activeCount * 100.0) / devices.size();

        System.out.println("Activity Rate: " + String.format("%.1f%%", activePercentage));

        // Thermostat analysis
        List<Thermostat> thermostats = devices.stream()
                .filter(d -> d instanceof Thermostat)
                .map(d -> (Thermostat) d)
                .collect(Collectors.toList());

        if (!thermostats.isEmpty()) {
            double avgTemp = thermostats.stream()
                    .mapToInt(Thermostat::getTemperature)
                    .average()
                    .orElse(0);
            System.out.println("\nThermostat Analysis:");
            System.out.println("  Average Temperature: " + String.format("%.1f°C", avgTemp));
            System.out.println("  Total Thermostats: " + thermostats.size());
        }

        // Usage patterns
        System.out.println("\nUsage Patterns:");
        System.out.println("  Most used commands:");
        commandStats.entrySet().stream()
                .sorted(Map.Entry.<String, Integer>comparingByValue().reversed())
                .limit(5)
                .forEach(entry ->
                        System.out.println("    " + entry.getKey() + ": " + entry.getValue() + " times"));
    }

    private void handleSearch(String args) {
        if (args.isEmpty()) {
            System.out.println("Usage: search <query>");
            return;
        }

        List<SmartDevice> filtered = filterDevices(facade.getDevices(), args);
        if (filtered.isEmpty()) {
            System.out.println("No devices found matching: " + args);
        } else {
            System.out.println("Found " + filtered.size() + " device(s):");
            filtered.forEach(this::printDeviceRow);
        }
    }

    private void handleFilter(String args) {
        if (args.isEmpty()) {
            System.out.println("Usage: filter <type|status>");
            System.out.println("Examples: filter light, filter on, filter off");
            return;
        }

        List<SmartDevice> devices = facade.getDevices();
        List<SmartDevice> filtered;

        switch (args.toLowerCase()) {
            case "on", "active" -> filtered = devices.stream()
                    .filter(SmartDevice::isOn)
                    .collect(Collectors.toList());
            case "off", "inactive" -> filtered = devices.stream()
                    .filter(d -> !d.isOn())
                    .collect(Collectors.toList());
            case "light", "lights" -> filtered = devices.stream()
                    .filter(d -> d instanceof Light)
                    .collect(Collectors.toList());
            case "thermostat", "thermostats" -> filtered = devices.stream()
                    .filter(d -> d instanceof Thermostat)
                    .collect(Collectors.toList());
            case "camera", "cameras" -> filtered = devices.stream()
                    .filter(d -> d instanceof SecurityCamera)
                    .collect(Collectors.toList());
            default -> filtered = filterDevices(devices, args);
        }

        if (filtered.isEmpty()) {
            System.out.println("No devices match filter: " + args);
        } else {
            System.out.println("Filtered results (" + filtered.size() + " devices):");
            filtered.forEach(this::printDeviceRow);
        }
    }

    private void handleGroup(String args) {
        if (args.isEmpty()) {
            System.out.println("Usage: group <action> [group-name]");
            System.out.println("Actions: create, list, on, off");
            return;
        }

        System.out.println(colorize(YELLOW + "Group management feature coming soon!" + RESET));
    }

    private void handleReset() {
        System.out.print(colorize(RED + "WARNING: This will remove all devices. Continue? (yes/n): " + RESET));
        String confirm = scanner.nextLine().trim();

        if ("yes".equalsIgnoreCase(confirm)) {
            // Stop all services
            concurrentCommands.processConcurrentCommand("monitor stop");
            concurrentCommands.processConcurrentCommand("automate stop");
            concurrentCommands.processConcurrentCommand("power monitor stop");
            concurrentCommands.processConcurrentCommand("events stop");

            // Reset facade
            facade.reset();

            System.out.println(colorize(GREEN + "✅ System reset complete" + RESET));

            // Ask to restart services
            System.out.print("Restart concurrent services? (y/n): ");
            if (scanner.nextLine().trim().toLowerCase().startsWith("y")) {
                startConcurrentServices();
            }
        } else {
            System.out.println("Reset cancelled.");
        }
    }

    private void showHistory() {
        if (commandHistory.isEmpty()) {
            System.out.println("No command history.");
            return;
        }

        System.out.println("\n=== COMMAND HISTORY ===");
        int start = Math.max(0, commandHistory.size() - 20);
        for (int i = start; i < commandHistory.size(); i++) {
            System.out.printf("%3d: %s\n", i + 1, commandHistory.get(i));
        }
    }

    private void showStats() {
        System.out.println("\n=== USAGE STATISTICS ===");

        if (commandStats.isEmpty()) {
            System.out.println("No statistics available yet.");
            return;
        }

        System.out.println("Command usage:");
        commandStats.entrySet().stream()
                .sorted(Map.Entry.<String, Integer>comparingByValue().reversed())
                .forEach(entry ->
                        System.out.printf("  %-15s: %d times\n", entry.getKey(), entry.getValue()));

        System.out.println("\nTotal commands: " + commandStats.values().stream().mapToInt(Integer::intValue).sum());
        System.out.println("Unique commands: " + commandStats.size());
    }

    private void handleExit() {
        System.out.println("\nShutting down Smart Home System...");

        // Save all devices
        try {
            List<SmartDevice> devices = facade.getDevices();
            if (!devices.isEmpty()) {
                System.out.println("Saving devices...");
                if (persistenceService.isDatabaseAvailable()) {
                    persistenceService.saveDeviceStatesToDatabase(devices);
                    System.out.println(colorize(GREEN + "  ✓ Saved to database" + RESET));
                }
                persistenceService.saveDeviceStatesJson(devices);
                System.out.println(colorize(GREEN + "  ✓ Saved to JSON file" + RESET));
                persistenceService.saveDeviceStatesBinary(devices);
                System.out.println(colorize(GREEN + "  ✓ Saved to binary file" + RESET));
            }
        } catch (Exception e) {
            System.err.println(colorize(RED + "Warning: Could not save all devices: " + e.getMessage() + RESET));
        }

        // Shutdown concurrent services
        System.out.println("Stopping services...");
        concurrentCommands.shutdown();

        running = false;
        System.out.println(colorize(GREEN + "\n✅ Thank you for using Smart Home System. Goodbye!" + RESET));
    }

    private void showHelp() {
        System.out.println(colorize(CYAN + """
                
                ╔════════════════════════════════════════════════════════════╗
                ║                    SMART HOME COMMANDS                     ║
                ╚════════════════════════════════════════════════════════════╝
                
                DEVICE MANAGEMENT:
                  add <name> <type> [loc]    Add device (light/thermostat/camera)
                  remove <name>              Remove a device
                  list [filter]              List all devices
                
                DEVICE CONTROL:
                  on <name>                  Turn device on
                  off <name>                 Turn device off
                  toggle <name>              Toggle device state
                  set <name> <temp>          Set thermostat temperature
                  
                BULK OPERATIONS:
                  all-on                     Turn all devices on
                  all-off                    Turn all devices off
                  automate                   Run automation rules
                
                INFORMATION:
                  status [name]              Show system or device status
                  info <name>                Detailed device information
                  report                     Full system report
                  analytics                  Device usage analytics
                
                SEARCH & FILTER:
                  search <query>             Search devices by name
                  filter <criteria>          Filter by type or status
                  group <action>             Group operations (coming soon)
                """ + RESET));

        // Add concurrent commands help
        System.out.println(ConcurrentCLICommands.getConcurrentHelp());

        System.out.println(colorize(CYAN + """
                
                DATA MANAGEMENT:
                  save                       Save to database/files
                  load                       Load from storage
                  sync                       Sync with database
                  export                     Export to JSON file
                  import                     Import from JSON file
                
                SYSTEM:
                  help                       Show this help
                  clear                      Clear screen
                  color                      Toggle color output
                  verbose                    Toggle verbose mode
                  reset                      Reset entire system
                  history                    Show command history
                  stats                      Show usage statistics
                  exit                       Save and exit
                
                TIPS:
                  • Commands can be abbreviated (e.g., 'l' for 'list')
                  • Device names are case-insensitive
                  • Use quotes for names with spaces
                  • Tab completion available for device names
                """ + RESET));
    }

    // ========== Helper Methods ==========

    private void clearScreen() {
        System.out.print("\033[H\033[2J");
        System.out.flush();
        printBanner();
    }

    private void toggleColor() {
        colorEnabled = !colorEnabled;
        System.out.println("Color output: " + (colorEnabled ? colorize(GREEN + "enabled" + RESET) : "disabled"));
    }

    private void toggleVerbose() {
        verboseMode = !verboseMode;
        System.out.println("Verbose mode: " + (verboseMode ? colorize(GREEN + "enabled" + RESET) : "disabled"));
    }

    private Optional<SmartDevice> findDevice(String name) {
        return facade.getDevices().stream()
                .filter(d -> d.getName().equalsIgnoreCase(name))
                .findFirst();
    }

    private List<SmartDevice> filterDevices(List<SmartDevice> devices, String filter) {
        String lowerFilter = filter.toLowerCase();
        return devices.stream()
                .filter(d -> d.getName().toLowerCase().contains(lowerFilter) ||
                        d.getClass().getSimpleName().toLowerCase().contains(lowerFilter) ||
                        d.getStatus().toLowerCase().contains(lowerFilter))
                .collect(Collectors.toList());
    }

    private void printDeviceRow(SmartDevice device) {
        String status = device.isOn() ?
                colorize(GREEN + "ON " + RESET) :
                colorize(RED + "OFF" + RESET);

        String type = device.getClass().getSimpleName()
                .replace("Smart", "")
                .replace("Device", "");

        String details = getDeviceDetails(device);

        System.out.printf("  %-25s %-15s %-10s %-30s%n",
                device.getName(), type, status, details);
    }

    private String getDeviceDetails(SmartDevice device) {
        if (device instanceof Thermostat) {
            return "Temperature: " + ((Thermostat) device).getTemperature() + "°C";
        } else if (device instanceof SecurityCamera) {
            return device.isOn() ? "Recording active" : "Recording stopped";
        } else if (device instanceof Light) {
            return device.isOn() ? "Illuminated" : "Dark";
        }
        return device.getStatus();
    }

    private void addToHistory(String command) {
        commandHistory.add(command);
        if (commandHistory.size() > MAX_HISTORY) {
            commandHistory.remove(0);
        }
    }

    private void trackCommand(String command) {
        commandStats.merge(command, 1, Integer::sum);
    }
}