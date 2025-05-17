package com.fatty.smarthome.core;

import com.fatty.smarthome.devices.Controllable;
import com.fatty.smarthome.devices.Light;
import com.fatty.smarthome.devices.SecurityCamera;
import com.fatty.smarthome.devices.Thermostat;
import com.fatty.smarthome.util.SmartHomeException;

public class FacadeSmartHome {
    private static final FacadeSmartHome INSTANCE = new FacadeSmartHome();
    private final  SmartHome smartHome;

    private FacadeSmartHome() {
        smartHome = new SmartHome();
    }

    public static FacadeSmartHome getTheInstance() {
        return INSTANCE;
    }

    /**
     * smartHomeAccess(String command, String deviceName, String value) / INTENT / EXAMPLE / DEFINITIONS / PRECONDITIONS / POSTCONDITIONS
     * INTENT: Provide unified access to smart home functionality via CLI commands.
     * EXAMPLE: smartHomeAccess("add", "Living Room Light", "light") -> Adds a Light device.
     * DEFINITIONS: command - Action (add, turnOn, turnOff, setTemp, automate, report); deviceName - Device identifier; value - Optional parameter (e.g., temperature, device type).
     * PRECONDITIONS: command is valid; deviceName is non-null for device-specific commands; value is valid for command (e.g., 10–32 for setTemp).
     * POSTCONDITIONS: Command is executed; SmartHomeException thrown for invalid inputs or failures.
     */
    public String smartHomeAccess(String command, String deviceName, String value) throws SmartHomeException {
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
}
