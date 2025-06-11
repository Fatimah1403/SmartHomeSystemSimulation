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
//import java.time.LocalDateTime;
//import java.time.format.DateTimeFormatter;
import java.util.*;
import java.util.stream.Collectors;

import static com.fatty.smarthome.Main.colorize;

/**
 * Enhanced Command Line Interface for Smart Home System
 * Integrates concurrent features and event-driven operations
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

    private boolean running = true;
    private boolean colorEnabled = true;
    private boolean verboseMode = false;

    public SmartHomeCLI() throws SmartHomeException {
        this.facade = FacadeSmartHome.getTheInstance();
        this.scanner = new Scanner(System.in);
        this.concurrentCommands = new ConcurrentCLICommands(facade);
        this.persistenceService = new PersistenceService();
    }

    /**
     * Print welcome banner
     */
    private void printBanner() {
        System.out.println(colorize(CYAN + """
                ╔════════════════════════════════════╗
                ║     SMART HOME CONTROL SYSTEM      ║
                ║          Version 2.0               ║
                ╚════════════════════════════════════╝
                """ + RESET));
    }

    /**
     * Start the CLI
     */
    public void start() {
        printBanner();

        // Load saved devices
        loadDevices();

        // Start concurrent services - FIXED
        System.out.print("Start concurrent services? (y/n): ");
        if (scanner.nextLine().trim().toLowerCase().startsWith("y")) {
            // Start services individually using processConcurrentCommand
            concurrentCommands.processConcurrentCommand("events start");
            concurrentCommands.processConcurrentCommand("monitor start");
            concurrentCommands.processConcurrentCommand("automate start");
            System.out.println(colorize(GREEN + "✓ Concurrent services started" + RESET));
        }

        System.out.println("\nType 'help' for commands or 'exit' to quit.\n");

        // Main command loop
        while (running) {
            displayPrompt();
            String input = scanner.nextLine().trim();

            if (!input.isEmpty()) {
                processCommand(input);
            }
        }

        scanner.close();
    }

    /**
     * Load saved devices from file
     */
    private void loadDevices() {
        try {
            System.out.println("Loading saved devices...");

            // Load from JSON (or binary - your choice)
            List<DeviceState> states = persistenceService.loadDeviceStatesJson();

            if (states.isEmpty()) {
                System.out.println("No saved devices found.");
                return;
            }

            // Reconstruct devices and add to facade
            int loaded = 0;
            for (DeviceState state : states) {
                try {
                    SmartDevice device = persistenceService.reconstructDevice(state);
                    facade.addDevice(device);
                    loaded++;
                } catch (Exception e) {
                    System.err.println("Failed to load device: " + state.getDeviceName() + " - " + e.getMessage());
                }
            }

            System.out.println(colorize(GREEN + "✓ Loaded " + loaded + " device(s)" + RESET));

        } catch (Exception e) {
            System.err.println("Error loading devices: " + e.getMessage());
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

            String status = String.format("[%d/%d active]", activeCount, devices.size());
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

                // Information
                case "status", "s" -> handleStatus(args);
                case "info", "i" -> handleInfo(args);
                case "report", "r" -> handleReport();

                // Data management
                case "save" -> handleSave();
                case "load" -> handleLoad();
                case "export" -> handleExport();
                case "import" -> handleImport();

                // System commands
                case "help", "h", "?" -> showHelp();
                case "clear", "cls" -> clearScreen();
                case "color" -> toggleColor();
                case "verbose", "v" -> toggleVerbose();
                case "reset" -> handleReset();
                case "exit", "quit", "q" -> handleExit();

                // Additional features
                case "search" -> handleSearch(args);
                case "filter" -> handleFilter(args);
                case "history" -> showHistory();

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

    private void handleSave() {
        try {
            List<SmartDevice> devices = facade.getDevices();
            if (devices.isEmpty()) {
                System.out.println("No devices to save.");
                return;
            }

            // Save to both formats
            persistenceService.saveDeviceStatesBinary(devices);
            persistenceService.saveDeviceStatesJson(devices);

            System.out.println(colorize(GREEN + "✓ Saved " + devices.size() + " device(s)" + RESET));
        } catch (Exception e) {
            System.err.println(colorize(RED + "Failed to save devices: " + e.getMessage() + RESET));
        }
    }

    private void handleLoad() {
        try {
            // Clear existing devices
            System.out.print("This will replace all current devices. Continue? (y/n): ");
            if (!scanner.nextLine().trim().toLowerCase().startsWith("y")) {
                System.out.println("Load cancelled.");
                return;
            }

            // Reset the system
            facade.reset();

            // Load devices
            loadDevices();

        } catch (Exception e) {
            System.err.println(colorize(RED + "Failed to load devices: " + e.getMessage() + RESET));
        }
    }

    // ========== Command Handlers ==========

    private void handleAdd(String args) throws SmartHomeException {
        String[] parts = args.split("\\s+");
        if (parts.length < 2) {
            System.out.println("Usage: add <name> <type> [options]");
            System.out.println("Types: light, thermostat, camera");
            System.out.println("Options: concurrent (for thread-safe devices)");
            return;
        }

        String name = parts[0];
        String type = parts[1].toLowerCase();
        boolean concurrent = parts.length > 2 && parts[2].equals("concurrent");

        // Validate type
        if (!Arrays.asList("light", "thermostat", "camera").contains(type)) {
            throw new SmartHomeException("Invalid device type. Use: light, thermostat, or camera");
        }

        // Add device
        String result = facade.smartHomeAccess("add", name, type);
        System.out.println(colorize(GREEN + "✓ " + result + RESET));

        // Ask if user wants to turn it on
        System.out.print("Turn on the device now? (y/n): ");
        if (scanner.nextLine().trim().toLowerCase().startsWith("y")) {
            facade.smartHomeAccess("turnon", name, "");
            System.out.println(colorize(GREEN + "✓ Device turned on" + RESET));
        }
    }

    private void handleExport() {
        try {
            System.out.print("Enter filename for export (default: smart_home_export.json): ");
            String filename = scanner.nextLine().trim();
            if (filename.isEmpty()) {
                filename = "smart_home_export.json";
            }

            List<SmartDevice> devices = facade.getDevices();
            if (devices.isEmpty()) {
                System.out.println("No devices to export.");
                return;
            }

            // Create a custom export with more details
            List<DeviceState> states = devices.stream()
                    .map(this::createDetailedDeviceState)
                    .collect(Collectors.toList());

            // Write to file
            try (FileWriter writer = new FileWriter(filename)) {
                Gson gson = new GsonBuilder().setPrettyPrinting().create();
                gson.toJson(states, writer);
            }

            System.out.println(colorize(GREEN + "✓ Exported " + devices.size() +
                    " device(s) to " + filename + RESET));

        } catch (Exception e) {
            System.err.println(colorize(RED + "Export failed: " + e.getMessage() + RESET));
        }
    }

    private void handleStatus(String args) {
        if (args.isEmpty()) {
            // Show general status
            concurrentCommands.processConcurrentCommand("services");
        } else {
            // Show specific device status
            Optional<SmartDevice> device = findDevice(args);
            if (device.isPresent()) {
                printDeviceRow(device.get());
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
            System.out.println("Status: " + (d.isOn() ? "ON" : "OFF"));
            System.out.println("Details: " + d.getStatus());

            // Add device-specific info
            if (d instanceof Thermostat) {
                System.out.println("Temperature: " + ((Thermostat) d).getTemperature() + "°C");
            }
        } else {
            System.out.println(colorize(RED + "Device not found: " + args + RESET));
        }
    }

    private void handleReport() {
        System.out.println("\n=== SYSTEM REPORT ===");
        // Use the services command to get concurrent status - FIXED
        concurrentCommands.processConcurrentCommand("services");

        // Also show device statistics
        try {
            List<SmartDevice> devices = facade.getDevices();
            System.out.println("\nDevice Statistics:");
            System.out.println("Total devices: " + devices.size());
            System.out.println("Active devices: " + devices.stream().filter(SmartDevice::isOn).count());

            // Count by type
            Map<String, Long> typeCounts = devices.stream()
                    .collect(Collectors.groupingBy(
                            d -> d.getClass().getSimpleName(),
                            Collectors.counting()
                    ));

            System.out.println("\nDevices by type:");
            typeCounts.forEach((type, count) ->
                    System.out.println("  " + type + ": " + count));

        } catch (Exception e) {
            System.err.println("Error generating report: " + e.getMessage());
        }
    }

    private DeviceState createDetailedDeviceState(SmartDevice device) {
        DeviceState state = new DeviceState(
                device.getName(),
                device.getClass().getSimpleName(),
                device.isOn(),
                device.getStatus()
        );

        // Add device-specific details
        if (device instanceof Thermostat) {
            state.setTemperature(((Thermostat) device).getTemperature());
        } else if (device instanceof SecurityCamera) {
            state.setRecording(device.isOn());
        }

        return state;
    }

    private void handleImport() {
        try {
            System.out.print("Enter filename to import (e.g., smart_home_export.json): ");
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
            List<DeviceState> states;
            try (FileReader reader = new FileReader(file)) {
                Type listType = new TypeToken<List<DeviceState>>() {
                }.getType();
                Gson gson = new Gson();
                states = gson.fromJson(reader, listType);
            }

            if (states == null || states.isEmpty()) {
                System.out.println("No devices found in file.");
                return;
            }

            // Import devices
            int imported = 0;
            for (DeviceState state : states) {
                try {
                    SmartDevice device = persistenceService.reconstructDevice(state);
                    facade.addDevice(device);
                    imported++;
                } catch (Exception e) {
                    System.err.println("Failed to import: " + state.getDeviceName() + " - " + e.getMessage());
                }
            }

            System.out.println(colorize(GREEN + "✓ Imported " + imported + " device(s)" + RESET));

        } catch (Exception e) {
            System.err.println(colorize(RED + "Import failed: " + e.getMessage() + RESET));
        }
    }

    private void handleRemove(String args) {
        if (args.isEmpty()) {
            System.out.println("Usage: remove <name>");
            return;
        }

        System.out.println(colorize(YELLOW + "Device removal feature coming soon!" + RESET));
        // In real implementation, would remove device from facade
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
                String.format("%-20s %-15s %-10s %-30s", "Name", "Type", "Status", "Details") +
                RESET));
        System.out.println("─".repeat(75));

        // Print devices
        devices.stream()
                .sorted(Comparator.comparing(SmartDevice::getName))
                .forEach(this::printDeviceRow);

        System.out.println("─".repeat(75));
        System.out.println("Total: " + devices.size() + " device(s)");
    }

    private void handleTurnOn(String args) throws SmartHomeException {
        if (args.isEmpty()) {
            System.out.println("Usage: on <device-name>");
            return;
        }

        String result = facade.smartHomeAccess("turnon", args, "");
        System.out.println(colorize(GREEN + "✓ " + result + RESET));
    }

    private void handleTurnOff(String args) throws SmartHomeException {
        if (args.isEmpty()) {
            System.out.println("Usage: off <device-name>");
            return;
        }

        String result = facade.smartHomeAccess("turnoff", args, "");
        System.out.println(colorize(YELLOW + "✓ " + result + RESET));
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
            System.out.println(colorize(GREEN + "✓ " + result + RESET));
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
        System.out.println(colorize(GREEN + "✓ " + result + RESET));
    }

    private void handleAllOn() throws SmartHomeException {
        List<SmartDevice> devices = facade.getDevices();
        int count = 0;

        for (SmartDevice device : devices) {
            try {
                facade.smartHomeAccess("turnon", device.getName(), "");
                count++;
            } catch (SmartHomeException e) {
                if (verboseMode) {
                    System.err.println("Failed: " + device.getName() + " - " + e.getMessage());
                }
            }
        }

        System.out.println(colorize(GREEN + "✓ Turned on " + count + " devices" + RESET));
    }

    private void handleReset() {
        System.out.print("Are you sure you want to reset? (y/n): ");
        if (scanner.nextLine().trim().toLowerCase().startsWith("y")) {
            // Reset implementation
            System.out.println("System reset!");
        }
    }

    private final List<String> commandHistory = new ArrayList<>();
    private static final int MAX_HISTORY = 100;

    private void addToHistory(String command) {
        commandHistory.add(command);
        if (commandHistory.size() > MAX_HISTORY) {
            commandHistory.remove(0);
        }
    }
    private final Map<String, Integer> commandStats = new HashMap<>();

    private void trackCommand(String command) {
        commandStats.merge(command, 1, Integer::sum);
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
        handleSearch(args); //
    }

    private void showHistory() {
        System.out.println("Command history feature coming soon!");
    }

    private void handleExit() {
        System.out.println("\nShutting down services...");
        concurrentCommands.shutdown();
        running = false;
        System.out.println("Goodbye!");
    }

    private void showHelp() {
        System.out.println(colorize(CYAN + """
                === SMART HOME COMMANDS ===
                
                DEVICE MANAGEMENT:
                  add <name> <type>    Add a device (light/thermostat/camera)
                  remove <name>        Remove a device
                  list [filter]        List all devices (optional filter)
                
                DEVICE CONTROL:
                  on <name>            Turn device on
                  off <name>           Turn device off
                  toggle <name>        Toggle device state
                  set <name> <temp>    Set thermostat temperature
                  all-on               Turn all devices on
                  all-off              Turn all devices off
                
                INFORMATION:
                  status [name]        Show system or device status
                  info <name>          Detailed device information
                  report               Full system report
                
                CONCURRENT FEATURES:
                """ + RESET));

        // Add concurrent commands help
        System.out.println(ConcurrentCLICommands.getConcurrentHelp());

        System.out.println(colorize(CYAN + """
                
                DATA MANAGEMENT:
                  save                 Save configuration
                  load                 Load configuration
                  export               Export to JSON
                  import               Import from JSON
                
                SYSTEM:
                  help                 Show this help
                  clear                Clear screen
                  color                Toggle color output
                  verbose              Toggle verbose mode
                  reset                Reset system
                  exit                 Exit application
                """ + RESET));
    }

    /**
     * Clear the console screen
     */
    private void clearScreen() {
        System.out.print("\033[H\033[2J");
        System.out.flush();
    }

    /**
     * Toggle color output
     */
    private void toggleColor() {
        colorEnabled = !colorEnabled;
        System.out.println("Color output: " + (colorEnabled ? "enabled" : "disabled"));
    }

    /**
     * Toggle verbose mode
     */
    private void toggleVerbose() {
        verboseMode = !verboseMode;
        System.out.println("Verbose mode: " + (verboseMode ? "enabled" : "disabled"));
    }

    /**
     * Find device by name
     */
    private Optional<SmartDevice> findDevice(String name) {
        return facade.getDevices().stream()
                .filter(d -> d.getName().equalsIgnoreCase(name))
                .findFirst();
    }

    /**
     * Filter devices by criteria
     */
    private List<SmartDevice> filterDevices(List<SmartDevice> devices, String filter) {
        String lowerFilter = filter.toLowerCase();
        return devices.stream()
                .filter(d -> d.getName().toLowerCase().contains(lowerFilter) ||
                        d.getClass().getSimpleName().toLowerCase().contains(lowerFilter))
                .collect(Collectors.toList());
    }

    /**
     * Print device row for list command
     */
    private void printDeviceRow(SmartDevice device) {
        String status = device.isOn() ?
                colorize(GREEN + "ON" + RESET) :
                colorize(RED + "OFF" + RESET);

        String type = device.getClass().getSimpleName();
        String details = getDeviceDetails(device);

        System.out.printf("%-20s %-15s %-10s %-30s%n",
                device.getName(), type, status, details);
    }

    /**
     * Get device-specific details
     */
    private String getDeviceDetails(SmartDevice device) {
        if (device instanceof Thermostat) {
            return "Temp: " + ((Thermostat) device).getTemperature() + "°C";
        }
        return "";
    }

    private void handleAllOff() throws SmartHomeException {
        List<SmartDevice> devices = facade.getDevices();
        int count = 0;

        for (SmartDevice device : devices) {
            try {
                facade.smartHomeAccess("turnoff", device.getName(), "");
                count++;
            } catch (SmartHomeException e) {
                if (verboseMode) {
                    System.err.println(" ");
                }
            }
        }
    }
}