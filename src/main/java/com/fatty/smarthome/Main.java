package com.fatty.smarthome;

import com.fatty.smarthome.cli.SmartHomeCLI;
import com.fatty.smarthome.gui.SmartHomeGUI;
import com.fatty.smarthome.util.SmartHomeException;
import javafx.application.Application;

import java.io.IOException;
import java.io.InputStream;
import java.util.Properties;

/**
 * Main entry point for the Smart Home System.
 * Provides both CLI and GUI interfaces.
 */
public class Main {
    // ANSI color codes
    private static final String RESET = "\u001B[0m";
    private static final String RED = "\u001B[31m";
    private static final String GREEN = "\u001B[32m";
    private static final String YELLOW = "\u001B[33m";
    private static final String BLUE = "\u001B[34m";
    private static final String PURPLE = "\u001B[35m";
    private static final String CYAN = "\u001B[36m";

    // Global color enable flag
    private static boolean colorEnabled = true;

    public static void main(String[] args) {
        // Check for command line arguments
        if (args.length > 0) {
            switch (args[0].toLowerCase()) {
                case "--cli" -> runCLI();
                case "--gui" -> runGUI(args);
                case "--help" -> showHelp();
                case "--no-color" -> {
                    colorEnabled = false;
                    runCLI();
                }
                default -> showInterfaceSelection();
            }
        } else {
            showInterfaceSelection();
        }
    }

    /**
     * Shows interface selection menu
     */
    private static void showInterfaceSelection() {
        printBanner();

        System.out.println("\nSelect Interface Mode:");
        System.out.println(colorize("1. " + CYAN + "Command Line Interface (CLI)" + RESET));
        System.out.println(colorize("2. " + BLUE + "Graphical User Interface (GUI)" + RESET));
        System.out.println(colorize("3. " + RED + "Exit" + RESET));
        System.out.print("\nEnter your choice (1-3): ");

        java.util.Scanner scanner = new java.util.Scanner(System.in);
        String choice = scanner.nextLine().trim();

        switch (choice) {
            case "1" -> runCLI();
            case "2" -> runGUI(new String[]{});
            case "3" -> {
                System.out.println(colorize(GREEN + "Thank you for using Smart Home System. Goodbye!" + RESET));
                System.exit(0);
            }
            default -> {
                System.out.println(colorize(RED + "Invalid choice. Starting CLI mode..." + RESET));
                runCLI();
            }
        }
    }

    /**
     * Prints application banner
     */
    private static void printBanner() {
        String banner = """
            ╔═══════════════════════════════════════════════════════╗
            ║            SMART HOME SYSTEM v2.0                     ║
            ║         Advanced Home Automation Control              ║
            ║                with Concurrent Features               ║
            ╚═══════════════════════════════════════════════════════╝
            """;
        System.out.println(colorize(CYAN + banner + RESET));
    }

    private static void loadConfiguration() {
        Properties props = new Properties();
        try (InputStream input = Main.class.getResourceAsStream("/config.properties")) {
            if (input != null) {
                props.load(input);
                colorEnabled = Boolean.parseBoolean(props.getProperty("color.enabled", "true"));
            }
        } catch (IOException ignored) {

        }
    }

    /**
     * Run CLI mode
     */
    private static void runCLI() {
        try {
            System.out.println(colorize("\n" + GREEN + "=== Starting Command Line Interface ===" + RESET));
            SmartHomeCLI cli = new SmartHomeCLI();
            cli.start();
        } catch (SmartHomeException e) {
            System.err.println(colorize(RED + "Failed to start CLI: " + e.getMessage() + RESET));
            e.printStackTrace();
            System.exit(1);
        } catch (Exception e) {
            System.err.println(colorize(RED + "Unexpected error: " + e.getMessage() + RESET));
            e.printStackTrace();
            System.exit(1);
        }
    }

    /**
     * Run GUI mode
     */
    private static void runGUI(String[] args) {
        System.out.println(colorize(BLUE + "\n=== Launching Graphical Interface ===" + RESET));
        System.out.println("Starting GUI...\n");

        try {
            // Check if JavaFX is available
            Class.forName("javafx.application.Application");
            Application.launch(SmartHomeGUI.class, args);
        } catch (ClassNotFoundException e) {
            System.err.println(colorize(RED + "JavaFX not found. Please ensure JavaFX is installed." + RESET));
            System.err.println("Falling back to CLI mode...");
            runCLI();
        }
    }
    /**
     * Show help
     */
    private static void showHelp() {
        System.out.println("Smart Home System - Command Line Options");
        System.out.println("Usage: java -jar SmartHome.jar [option]");
        System.out.println("\nOptions:");
        System.out.println("  --cli        Start in CLI mode");
        System.out.println("  --gui        Start in GUI mode");
        System.out.println("  --no-color   Start CLI without colors");
        System.out.println("  --help       Show this help");
        System.out.println("\nNo option: Show interface selection menu");
    }

    /**
     * Apply color if enabled (static method for global access)
     */
    public static String colorize(String text) {
        return colorEnabled ? text : text.replaceAll("\u001B\\[[;\\d]*m", "");
    }
}