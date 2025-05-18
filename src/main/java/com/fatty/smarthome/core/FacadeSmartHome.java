package com.fatty.smarthome.core;

import com.fatty.smarthome.devices.Controllable;
import com.fatty.smarthome.devices.Light;
import com.fatty.smarthome.devices.SecurityCamera;
import com.fatty.smarthome.devices.Thermostat;
import com.fatty.smarthome.util.SmartHomeException;

import java.util.ArrayList;
import java.util.List;

public class FacadeSmartHome {
    private static final FacadeSmartHome INSTANCE = new FacadeSmartHome();
    private SmartHome smartHome;
    private final List<String> commandHistory;

    private FacadeSmartHome() {
        smartHome = new SmartHome();
        commandHistory = new ArrayList<>(); // value added>
    }

    /**
     * Returns the singleton instance of FacadeSmartHome.
     * @return The singleton instance
     */

    public static FacadeSmartHome getTheInstance() {
        return INSTANCE;
    }

    /**
     * Processes smart home commands.
     * @param command The command (e.g., add, turnOn, setTemp)
     * @param deviceName The device name
     * @param value Additional parameter (e.g., device type, temperature)
     * @return Result message
     * @throws SmartHomeException If command is invalid or fails
     */
    public String smartHomeAccess(String command, String deviceName, String value) throws SmartHomeException {
        String commandEntry = command + (deviceName.isEmpty() ? "" : " " + deviceName) + (value.isEmpty() ? "" : " " + value);
        commandHistory.add(commandEntry.trim());
        switch (command.toLowerCase()) { // value added don't want it to be case-sensitive
            case "add":
                Controllable device;
                switch (value.toLowerCase()) { // value added don't want it to be case-sensitive
                    case "light":
                        device = new Light(deviceName);
                        break;
                    case "thermostat":
                        device = new Thermostat(deviceName);
                        break;
                    case "camera":
                        device = new SecurityCamera(deviceName);
                        break;
                    default:
                        throw new SmartHomeException("Invalid device type: " + value);
                }
                smartHome.addDevice(device);
                return "Added " + deviceName;
            case "turnon":
                return controlDevice(deviceName, true);
            case "turnoff":
                return controlDevice(deviceName, false);
            case "settemp":
                int temp;
                try {
                    temp = Integer.parseInt(value);
                } catch (NumberFormatException e) {
                    throw new SmartHomeException("Invalid temperature: " + value);
                }
                // VALUE-ADDED: Temperature range validation
                if (temp < 10 || temp > 32) {
                    throw new SmartHomeException("Temperature must be between 10 and 32°C");
                }
                for (Controllable d : smartHome.getDevices()) {
                    if (d instanceof Thermostat t && t.getName().equals(deviceName)) {
                        t.setTemperature(temp);
                        smartHome.saveDevice(t); // Log updated state
                        return "Set " + deviceName + " to " + temp + "°C";
                    }

                }
                throw new SmartHomeException("Thermostat not found: " + deviceName);
            case "automate":
                smartHome.runAutomation(new LightAutomationRule());
                return "Automation rule applied";
            case "report":
                return smartHome.reportStatus();
            case "clearlog":
                smartHome.clearLogFile();
                return "Log file cleared successfully";
            case "history":
                return commandHistory.isEmpty() ? "No commands executed" : String.join("\n", commandHistory);
            default:
                throw new SmartHomeException("Invalid command: " + command);
        }
    }

    private String controlDevice(String deviceName, boolean turnOn) throws SmartHomeException {
        for (Controllable device : smartHome.getDevices()) {
            if (device.getName().equals(deviceName)) {
                if (turnOn) {
                    device.turnOn();
                    smartHome.saveDevice(device); // Log updated state
                    return deviceName + " turned ON";
                } else {
                    device.turnOff();
                    smartHome.saveDevice(device); // Log updated state);
                    return deviceName + " turned OFF";
                }
            }
        }   throw new SmartHomeException("Device not found: " + deviceName);

    }
    public  void reset() {
        smartHome = new SmartHome();
        commandHistory.clear();
    } // value added for testing purpose
}
