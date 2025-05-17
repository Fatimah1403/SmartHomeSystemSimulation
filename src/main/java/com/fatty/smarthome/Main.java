package com.fatty.smarthome;

import com.fatty.smarthome.core.FacadeSmartHome;
import com.fatty.smarthome.util.SmartHomeException;

import java.util.Scanner;

public class Main {
    public static void main(String[] args) {
//        System.out.println("#### Week Two Addition Begins #######");
        System.out.println("Smart Home Automation System Simulation (type 'exit' to quit)");
        System.out.println("Commands: add <name> <type>, turnOn <name>, turnOff <name>, setTemp <name> <temp>, automate, report");
        FacadeSmartHome facade = FacadeSmartHome.getTheInstance();
        Scanner scanner = new Scanner(System.in);

        while (true) {
            // VALUE-ADDED: Print prompt on same line as input for improved CLI usability
            System.out.print("> ");
            String input = scanner.nextLine().trim();
            if (input.equalsIgnoreCase("exit")) {
                break;
            }
            boolean isValidCommand = false;
            while (!isValidCommand) {
                if (input.isEmpty()) {
                    System.err.println("Error: Empty input");
                    System.out.println("Please re-enter a valid command (e.g., 'add LivingRoomLight light', or 'exit' to quit):");
                    System.out.println("> ");
                    input = scanner.nextLine().trim();
                    if (input.equalsIgnoreCase("exit")) {
                        break;
                    }
                    continue;
                }
                try {
                    // VALUE-ADDED: Input validation using regular expression to split command into parts
                    String[] parts = input.split("\\s+");
                    if (parts.length == 0) {
                        throw new SmartHomeException("Empty input");
                    }
                    // VALUE-ADDED: Extract command, deviceName, and value with safe array access
                    String command = parts[0].toLowerCase();
                    String deviceName = parts.length > 1 ? parts[1] : "";
                    String value = parts.length > 2 ? parts[2] : "";
                    // VALUE-ADDED: Validate command syntax for specific commands
                    if (command.equals("add") && parts.length != 3) {
                        throw new SmartHomeException("Add requires device name and type (light, thermostat, camera)");
                    }
                    if (command.equals("settemp") && parts.length != 3) {
                        throw new SmartHomeException("setTemp requires device name and temperature (10–32°C)");
                    }
                    if (command.equals("turnon") || command.equals("turnoff")) {
                        if (parts.length != 2) {
                            throw new SmartHomeException(command + " requires device name");
                        }
                    }
                    if (command.equals("automate") || command.equals("report")) {
                        if (parts.length != 1) {
                            throw new SmartHomeException(command + " requires no arguments");
                        }
                    }
                    String result = facade.smartHomeAccess(command, deviceName, value);
                    System.out.println(result);
                    isValidCommand = true; // Command succeeded, exit reprompt loop
                } catch (SmartHomeException | IllegalArgumentException e) {
                    // VALUE-ADDED: Catch both SmartHomeException and IllegalArgumentException for robust error handling
                    System.err.println("Error: " + e.getMessage());
                    // VALUE-ADDED: Reprompt user to re-enter command after error with same-line prompt
                    System.out.println("Please re-enter a valid command (e.g., 'add LivingRoomLight light', or 'exit' to quit):");
                    System.out.print("> ");
                    input = scanner.nextLine().trim();
                    if (input.equalsIgnoreCase("exit")) {
                        isValidCommand = true;
                        break;
                    }
                }
            }
            // VALUE-ADDED: Provide feedback after each successful command to confirm processing
            if (!input.equalsIgnoreCase("exit")) {
                System.out.println("Command processed successfully.");
            }
        }
        // VALUE-ADDED: Ensure resource cleanup by closing scanner
        scanner.close();
    }
}