//package com.fatty.smarthome;
//
//import com.fatty.smarthome.core.*;
//import com.fatty.smarthome.devices.*;
//import com.fatty.smarthome.gui.SmartHomeGUI;
//import com.fatty.smarthome.util.SmartHomeException;
//import javafx.application.Application;
//
//import java.time.LocalDateTime;
//import java.time.format.DateTimeFormatter;
//import java.util.*;
//import java.util.stream.Collectors;
//
///**
// * Main entry point for the Smart Home System.
// * Provides both CLI and GUI interfaces with enhanced features.
// */
//public class Main {
//    // ANSI color codes for enhanced CLI display
//    private static final String RESET = "\u001B[0m";
//    private static final String GREEN = "\u001B[32m";
//    private static final String RED = "\u001B[31m";
//    private static final String YELLOW = "\u001B[33m";
//    private static final String BLUE = "\u001B[34m";
//    private static final String CYAN = "\u001B[36m";
//
//    private static final Scanner scanner = new Scanner(System.in);
//    private static final FacadeSmartHome facade = FacadeSmartHome.getTheInstance();
//    private static final PersistenceService persistenceService = new PersistenceService();
//
//    // CLI state
//    private static boolean colorEnabled = true;
//    private static boolean verboseMode = false;
//
//    public static void main(String[] args) {
//        // Check for command line arguments
//        if (args.length > 0) {
//            switch (args[0].toLowerCase()) {
//                case "--cli" -> runCLI();
//                case "--gui" -> runGUI(args);
//                case "--help" -> showCommandLineHelp();
//                default -> showInterfaceSelection();
//            }
//        } else {
//            showInterfaceSelection();
//        }
//    }
//
//    /**
//     * Shows interface selection menu.
//     */
//    private static void showInterfaceSelection() {
//        printBanner();
//
//        System.out.println("\nSelect Interface Mode:");
//        System.out.println(colorize("1. " + CYAN + "Command Line Interface (CLI)" + RESET + " - Text-based control"));
//        System.out.println(colorize("2. " + BLUE + "Graphical User Interface (GUI)" + RESET + " - Visual control center"));
//        System.out.println(colorize("3. " + YELLOW + "Quick Demo" + RESET + " - See system capabilities"));
//        System.out.println(colorize("4. " + RED + "Exit" + RESET));
//        System.out.print("\nEnter your choice (1-4): ");
//
//        String choice = scanner.nextLine().trim();
//
//        switch (choice) {
//            case "1" -> runCLI();
//            case "2" -> runGUI(new String[]{});
//            case "3" -> runDemo();
//            case "4" -> {
//                System.out.println(colorize(GREEN + "Thank you for using Smart Home System. Goodbye!" + RESET));
//                System.exit(0);
//            }
//            default -> {
//                System.out.println(colorize(RED + "Invalid choice. Starting CLI mode..." + RESET));
//                runCLI();
//            }
//        }
//    }
//
//    /**
//     * Prints application banner.
//     */
//    private static void printBanner() {
//        String banner = """
//            ╔═══════════════════════════════════════════════════════╗
//            ║            SMART HOME SYSTEM v2.0                     ║
//            ║         Advanced Home Automation Control              ║
//            ║              MET CS 622 - Assignment 4                ║
//            ╚═══════════════════════════════════════════════════════╝
//            """;
//        System.out.println(colorize(CYAN + banner + RESET));
//    }
//
//    /**
//     * Runs the enhanced CLI mode.
//     */
//    private static void runCLI() {
//        System.out.println(colorize("\n" + GREEN + "=== Command Line Interface ===" + RESET));
//        System.out.println("Type 'help' for commands, 'tutorial' for guide, or 'exit' to quit.\n");
//
//        // Load saved devices
//        loadDevicesCLI();
//
//        // Display initial status
//        displayQuickStatus();
//
//        while (true) {
//            System.out.print(colorize(CYAN + "smart-home> " + RESET));
//            String input = scanner.nextLine().trim();
//
//            if (input.equalsIgnoreCase("exit") || input.equalsIgnoreCase("quit")) {
//                handleExit();
//                break;
//            }
//
//            processCommand(input);
//        }
//
//        scanner.close();
//    }
//
//    /**
//     * Processes CLI commands with enhanced features.
//     */
//    private static void processCommand(String input) {
//        if (input.isEmpty()) return;
//
//        // Parse command and arguments
//        String[] parts = input.split("\\s+");
//        String command = parts[0].toLowerCase();
//
//        try {
//            switch (command) {
//                // Basic commands
//                case "help", "h", "?" -> displayHelp();
//                case "tutorial" -> displayTutorial();
//                case "status", "s" -> displayDetailedStatus();
//                case "list", "ls" -> listDevices();
//
//                // Device management
//                case "add", "a" -> handleAdd(parts);
//                case "remove", "rm" -> handleRemove(parts);
//                case "on" -> handleTurnOn(parts);
//                case "off" -> handleTurnOff(parts);
//                case "toggle", "t" -> handleToggle(parts);
//                case "set" -> handleSet(parts);
//
//                // Bulk operations
//                case "all-on" -> bulkOperation(true);
//                case "all-off" -> bulkOperation(false);
//
//                // Analytics
//                case "report", "r" -> generateReport();
//                case "analytics" -> showAnalytics();
//                case "search" -> handleSearch(parts);
//                case "filter" -> handleFilter(parts);
//
//                // Automation
//                case "automate", "auto" -> runAutomation();
//                case "schedule" -> showScheduleInfo();
//
//                // Data management
//                case "save" -> saveDevices();
//                case "load" -> loadDevicesCLI();
//                case "export" -> exportData();
//                case "import" -> importData();
//
//                // System commands
//                case "clear", "cls" -> clearScreen();
//                case "color" -> toggleColor();
//                case "verbose", "v" -> toggleVerbose();
//                case "reset" -> resetSystem();
//                case "about" -> showAbout();
//
//                default -> System.out.println(colorize(RED + "Unknown command: " + command +
//                        ". Type 'help' for available commands." + RESET));
//            }
//        } catch (Exception e) {
//            System.err.println(colorize(RED + "Error: " + e.getMessage() + RESET));
//            if (verboseMode) {
//                e.printStackTrace();
//            }
//        }
//    }
//
//    /**
//     * Handles add device command.
//     */
//    private static void handleAdd(String[] parts) throws SmartHomeException {
//        if (parts.length < 3) {
//            System.out.println("Usage: add <name> <type>");
//            System.out.println("Types: light, thermostat, camera");
//            return;
//        }
//
//        String name = parts[1];
//        String type = parts[2].toLowerCase();
//
//        // Validate type
//        if (!Arrays.asList("light", "thermostat", "camera").contains(type)) {
//            System.out.println(colorize(RED + "Invalid device type. Use: light, thermostat, or camera" + RESET));
//            return;
//        }
//
//        String result = facade.smartHomeAccess("add", name, type);
//        System.out.println(colorize(GREEN + "✓ " + result + RESET));
//
//        // Ask if user wants to turn it on
//        System.out.print("Turn on the device now? (y/n): ");
//        if (scanner.nextLine().trim().toLowerCase().startsWith("y")) {
//            facade.smartHomeAccess("turnon", name, "");
//            System.out.println(colorize(GREEN + "✓ Device turned on" + RESET));
//        }
//    }
//
//    /**
//     * Handles device removal (simulated).
//     */
//    private static void handleRemove(String[] parts) {
//        if (parts.length < 2) {
//            System.out.println("Usage: remove <name>");
//            return;
//        }
//        System.out.println(colorize(YELLOW + "Device removal feature coming soon!" + RESET));
//    }
//
//    /**
//     * Handles turn on command.
//     */
//    private static void handleTurnOn(String[] parts) throws SmartHomeException {
//        if (parts.length < 2) {
//            System.out.println("Usage: on <device-name>");
//            return;
//        }
//
//        String name = String.join(" ", Arrays.copyOfRange(parts, 1, parts.length));
//        String result = facade.smartHomeAccess("turnon", name, "");
//        System.out.println(colorize(GREEN + "✓ " + result + RESET));
//    }
//
//    /**
//     * Handles turn off command.
//     */
//    private static void handleTurnOff(String[] parts) throws SmartHomeException {
//        if (parts.length < 2) {
//            System.out.println("Usage: off <device-name>");
//            return;
//        }
//
//        String name = String.join(" ", Arrays.copyOfRange(parts, 1, parts.length));
//        String result = facade.smartHomeAccess("turnoff", name, "");
//        System.out.println(colorize(YELLOW + "✓ " + result + RESET));
//    }
//
//    /**
//     * Handles toggle command.
//     */
//    private static void handleToggle(String[] parts) throws SmartHomeException {
//        if (parts.length < 2) {
//            System.out.println("Usage: toggle <device-name>");
//            return;
//        }
//
//        String name = String.join(" ", Arrays.copyOfRange(parts, 1, parts.length));
//        List<SmartDevice> devices = facade.getDevices();
//
//        Optional<SmartDevice> device = devices.stream()
//                .filter(d -> d.getName().equalsIgnoreCase(name))
//                .findFirst();
//
//        if (device.isPresent()) {
//            String command = device.get().isOn ? "turnoff" : "turnon";
//            String result = facade.smartHomeAccess(command, name, "");
//            System.out.println(colorize(GREEN + "✓ " + result + RESET));
//        } else {
//            System.out.println(colorize(RED + "Device not found: " + name + RESET));
//        }
//    }
//
//    /**
//     * Handles set command (for thermostats).
//     */
//    private static void handleSet(String[] parts) throws SmartHomeException {
//        if (parts.length < 3) {
//            System.out.println("Usage: set <thermostat-name> <temperature>");
//            return;
//        }
//
//        String name = parts[1];
//        String temp = parts[2];
//
//        String result = facade.smartHomeAccess("settemp", name, temp);
//        System.out.println(colorize(GREEN + "✓ " + result + RESET));
//    }
//
//    /**
//     * Displays enhanced help.
//     */
//    private static void displayHelp() {
//        String help = """
//
//            === COMMAND REFERENCE ===
//
//            """ + colorize(CYAN + "DEVICE MANAGEMENT:" + RESET) + """
//
//              add <name> <type>    Add a new device (light/thermostat/camera)
//              remove <name>        Remove a device
//              on <name>            Turn device on
//              off <name>           Turn device off
//              toggle <name>        Toggle device state
//              set <name> <temp>    Set thermostat temperature (10-32°C)
//
//            """ + colorize(CYAN + "VIEWING & ANALYTICS:" + RESET) + """
//
//              status, s            Show system status
//              list, ls             List all devices
//              report, r            Generate detailed report
//              analytics            Show device analytics
//              search <term>        Search devices
//              filter <type>        Filter by device type
//
//            """ + colorize(CYAN + "BULK OPERATIONS:" + RESET) + """
//
//              all-on               Turn all devices on
//              all-off              Turn all devices off
//              automate             Run automation rules
//
//            """ + colorize(CYAN + "DATA MANAGEMENT:" + RESET) + """
//
//              save                 Save current state
//              load                 Load saved state
//              export               Export to JSON
//              import               Import from JSON
//
//            """ + colorize(CYAN + "SYSTEM:" + RESET) + """
//
//              help, h, ?           Show this help
//              tutorial             Interactive tutorial
//              clear, cls           Clear screen
//              color                Toggle colors on/off
//              verbose              Toggle verbose mode
//              reset                Reset system
//              about                About this system
//              exit, quit           Exit program
//
//            """ + colorize(YELLOW + "TIP: Use TAB for command completion (if supported)" + RESET);
//
//        System.out.println(help);
//    }
//
//    /**
//     * Displays interactive tutorial.
//     */
//    private static void displayTutorial() {
//        System.out.println(colorize("\n" + CYAN + "=== INTERACTIVE TUTORIAL ===" + RESET));
//        System.out.println("\nWelcome to Smart Home System! Let's learn the basics.\n");
//
//        System.out.println("Press Enter to continue through each step...");
//        scanner.nextLine();
//
//        System.out.println("1. ADDING DEVICES");
//        System.out.println("   Try: " + colorize(GREEN + "add LivingRoomLight light" + RESET));
//        System.out.println("   This adds a new light called 'LivingRoomLight'");
//        scanner.nextLine();
//
//        System.out.println("2. CONTROLLING DEVICES");
//        System.out.println("   Try: " + colorize(GREEN + "on LivingRoomLight" + RESET));
//        System.out.println("   This turns on the light");
//        scanner.nextLine();
//
//        System.out.println("3. VIEWING STATUS");
//        System.out.println("   Try: " + colorize(GREEN + "status" + RESET));
//        System.out.println("   This shows all devices and their states");
//        scanner.nextLine();
//
//        System.out.println("4. THERMOSTATS");
//        System.out.println("   Try: " + colorize(GREEN + "add MainThermostat thermostat" + RESET));
//        System.out.println("   Then: " + colorize(GREEN + "set MainThermostat 22" + RESET));
//        scanner.nextLine();
//
//        System.out.println("5. SAVING YOUR WORK");
//        System.out.println("   Try: " + colorize(GREEN + "save" + RESET));
//        System.out.println("   This saves all devices for next time");
//        scanner.nextLine();
//
//        System.out.println(colorize(CYAN + "Tutorial complete! Try these commands now." + RESET));
//    }
//
//    /**
//     * Displays quick status summary.
//     */
//    private static void displayQuickStatus() {
//        try {
//            List<SmartDevice> devices = facade.getDevices();
//            long activeCount = devices.stream().filter(d -> d.isOn).count();
//
//            System.out.println(colorize(CYAN + "System Status: " + RESET +
//                    devices.size() + " devices (" + activeCount + " active)"));
//        } catch (Exception e) {
//            // Silent fail for quick status
//        }
//    }
//
//    /**
//     * Displays detailed system status.
//     */
//    private static void displayDetailedStatus() throws SmartHomeException {
//        String status = facade.smartHomeAccess("report", "", "");
//
//        // Enhance the output with colors
//        String[] lines = status.split("\n");
//        for (String line : lines) {
//            if (line.contains("is ON")) {
//                System.out.println(colorize(GREEN + line + RESET));
//            } else if (line.contains("is OFF")) {
//                System.out.println(colorize(YELLOW + line + RESET));
//            } else if (line.contains("Security status")) {
//                String color = line.contains("true") ? GREEN : RED;
//                System.out.println(colorize(color + line + RESET));
//            } else {
//                System.out.println(line);
//            }
//        }
//    }
//
//    /**
//     * Lists all devices in a formatted table.
//     */
//    private static void listDevices() {
//        try {
//            List<SmartDevice> devices = facade.getDevices();
//
//            if (devices.isEmpty()) {
//                System.out.println("No devices in the system.");
//                return;
//            }
//
//            // Print header
//            System.out.println("\n" + colorize(CYAN +
//                    String.format("%-20s %-15s %-10s %-30s", "Name", "Type", "Status", "Details") +
//                    RESET));
//            System.out.println("─".repeat(75));
//
//            // Print devices
//            devices.stream()
//                    .sorted(Comparator.comparing(SmartDevice::getName))
//                    .forEach(device -> {
//                        String status = device.isOn ? colorize(GREEN + "ON" + RESET) :
//                                colorize(YELLOW + "OFF" + RESET);
//                        String type = device.getClass().getSimpleName();
//                        String details = "";
//
//                        if (device instanceof Thermostat t) {
//                            details = "Temperature: " + t.getTemperature() + "°C";
//                        }
//
//                        System.out.printf("%-20s %-15s %-20s %-30s%n",
//                                device.getName(), type, status, details);
//                    });
//
//            System.out.println("─".repeat(75));
//            System.out.println("Total: " + devices.size() + " devices");
//        } catch (Exception e) {
//            System.err.println("Error listing devices: " + e.getMessage());
//        }
//    }
//
//    /**
//     * Generates comprehensive report.
//     */
//    private static void generateReport() {
//        try {
//            List<SmartDevice> devices = facade.getDevices();
//            String report = DeviceAnalytics.generateReport(devices);
//
//            System.out.println("\n" + colorize(CYAN + report + RESET));
//
//            // Add time stamp
//            System.out.println("\nGenerated: " + LocalDateTime.now().format(
//                    DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss")));
//        } catch (Exception e) {
//            System.err.println("Error generating report: " + e.getMessage());
//        }
//    }
//
//    /**
//     * Shows device analytics.
//     */
//    private static void showAnalytics() {
//        try {
//            List<SmartDevice> devices = facade.getDevices();
//
//            // Device type distribution
//            Map<String, Long> typeCounts = DeviceAnalytics.countByType(devices);
//            System.out.println("\n" + colorize(CYAN + "=== DEVICE ANALYTICS ===" + RESET));
//            System.out.println("\nDevice Distribution:");
//            typeCounts.forEach((type, count) ->
//                    System.out.println("  " + type + ": " + count));
//
//            // Power consumption simulation
//            long activeDevices = DeviceAnalytics.getActiveDevices(devices).size();
//            System.out.println("\nPower Status:");
//            System.out.println("  Active devices: " + activeDevices);
//            System.out.println("  Estimated power usage: " + (activeDevices * 15) + "W");
//
//            // Thermostat statistics
//            IntSummaryStatistics thermoStats = DeviceAnalytics.getThermostatStatistics(devices);
//            if (thermoStats.getCount() > 0) {
//                System.out.println("\nThermostat Analysis:");
//                System.out.println("  Average temperature: " +
//                        String.format("%.1f°C", thermoStats.getAverage()));
//                System.out.println("  Temperature range: " + thermoStats.getMin() +
//                        "°C - " + thermoStats.getMax() + "°C");
//            }
//
//            // Unique insights
//            Set<String> uniqueTypes = DeviceAnalytics.getUniqueDeviceTypes(devices);
//            System.out.println("\nSystem Capabilities:");
//            System.out.println("  Device types supported: " + String.join(", ", uniqueTypes));
//
//        } catch (Exception e) {
//            System.err.println("Error showing analytics: " + e.getMessage());
//        }
//    }
//
//    /**
//     * Handles device search.
//     */
//    private static void handleSearch(String[] parts) {
//        if (parts.length < 2) {
//            System.out.println("Usage: search <term>");
//            return;
//        }
//
//        String searchTerm = String.join(" ", Arrays.copyOfRange(parts, 1, parts.length));
//
//        try {
//            List<SmartDevice> devices = facade.getDevices();
//            List<SmartDevice> results = DeviceAnalytics.searchByName(devices, searchTerm);
//
//            if (results.isEmpty()) {
//                System.out.println("No devices found matching: " + searchTerm);
//            } else {
//                System.out.println("Found " + results.size() + " device(s):");
//                results.forEach(d -> System.out.println("  - " + d.getName() + " (" +
//                        d.getClass().getSimpleName() + ")"));
//            }
//        } catch (Exception e) {
//            System.err.println("Search error: " + e.getMessage());