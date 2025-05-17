package com.fatty.smarthome.core;

import com.fatty.smarthome.devices.Controllable;
import com.fatty.smarthome.devices.SmartDevice;
import com.fatty.smarthome.util.SmartHomeException;

import java.util.ArrayList;
import java.util.List;

public class SmartHome {
    private final List<SmartDevice> devices;
    private final SecurityService securityService;
    private final DatabaseService dbService;

    /**
     * SmartHome() / INTENT / EXAMPLE / DEFINITIONS / PRECONDITIONS / POSTCONDITIONS
     * INTENT: Initialize a SmartHome with empty device list and services.
     * EXAMPLE: new SmartHome() -> Creates SmartHome with no devices.
     * DEFINITIONS: None.
     * PRECONDITIONS: None.
     * POSTCONDITIONS: devices is empty; dbService and securityService initialized.
     */

    public SmartHome() {
        this.devices = new ArrayList<>();
        this.dbService = new DatabaseService();
        this.securityService = new SecurityService();
    }

    /**
     * addDevice(Controllable device) / INTENT / EXAMPLE / DEFINITIONS / PRECONDITIONS / POSTCONDITIONS
     * INTENT: Add a device to the smart home and log its initial state to device_log.txt.
     * EXAMPLE: addDevice(new Light("Living Room Light")) -> Adds light and logs "[2025-05-16 19:29:03],Living Room Light,OFF".
     * DEFINITIONS: device - The Controllable device to add.
     * PRECONDITIONS: device is a non-null SmartDevice; name is unique.
     * POSTCONDITIONS: Device added; state logged; SmartHomeException thrown if invalid.
     */

    public void addDevice(Controllable device) throws SmartHomeException {
        if (device == null || device.getName() == null || device.getName().isEmpty()) {
            throw new SmartHomeException("Device name cannot be null or empty");
        }
        if (!(device instanceof SmartDevice smartDevice)) {
            throw new SmartHomeException("Device must be a SmartDevice");
        }
        // VALUE - ADDED: Validate unique device name
        for (SmartDevice d : devices) {
            if (d.getName().equals(smartDevice.getName())) {
                throw new SmartHomeException("Device name already exists: " + smartDevice.getName());
            }
        }
        devices.add(smartDevice);
        dbService.save(smartDevice);
        // VALUE-ADDED: user feedback for successful addition
        System.out.println("Device added: " + smartDevice.getName());

    }
    /**
     * getDevices() / INTENT / EXAMPLE / DEFINITIONS / PRECONDITIONS / POSTCONDITIONS
     * INTENT: Return a copy of the current devices list.
     * EXAMPLE: getDevices() -> Returns list with Living Room Light, Kitchen Thermostat.
     * DEFINITIONS: None.
     * PRECONDITIONS: None.
     * POSTCONDITIONS: Returns a new List<Controllable> of all devices.
     */
    public List<Controllable> getDevices() {
        return new ArrayList<>(devices);
    }


    public String reportStatus() throws SmartHomeException {

        StringBuilder report = new StringBuilder("Smart Home Status Report:\n");// value added
        for (SmartDevice device : devices) {
            report.append(device.getName()).append(": ").append(device.getStatus()).append("\n");
            dbService.save(device);
        }
        report.append("Security Check: ").append(securityService.checkSecurity() ? "Secure" : "Not Secure");
        return report.toString();

    }
    /**
     * runAutomation(AutomationRule rule) / INTENT / EXAMPLE / DEFINITIONS / PRECONDITIONS / POSTCONDITIONS
     * INTENT: Apply an automation rule to all devices, logging changes.
     * EXAMPLE: runAutomation(new LightAutomationRule()) -> Turns on lights and logs states.
     * DEFINITIONS: rule - The automation rule to apply.
     * PRECONDITIONS: rule is non-null.
     * POSTCONDITIONS: Rule applied; device states logged; SmartHomeException thrown if invalid.
     */

    public void runAutomation(AutomationRule rule) throws SmartHomeException {
        if (rule == null) {
            throw new SmartHomeException("Automation rule cannot be null");
        }
        for (SmartDevice device : devices) {
            rule.visit(device);
            dbService.save(device);
        }
        // VALUE-ADDED User feedback for automation
        System.out.println("Automation rule executed successfully.");

    }
    public void saveDevice(Controllable device) throws SmartHomeException {
        if (device instanceof SmartDevice smartDevice) {
            dbService.save(smartDevice);
        }
    }
    public void clearLogFile() throws SmartHomeException {
        try {
            dbService.clearLogFile();
        } catch (Exception e) {
            throw new SmartHomeException("Failed to clear log file: " + e.getMessage());
        }
    }

}
