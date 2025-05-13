package com.fatty.smarthome;

import com.fatty.smarthome.devices.Controllable;
import com.fatty.smarthome.devices.SmartDevice;

import java.util.ArrayList;
import java.util.List;

public class SmartHome {
    private final List<SmartDevice> devices;
    private final SecurityService securityService;

    public SmartHome() {
        this.devices = new ArrayList<>();
//        this.databaseService = new DatabaseService(); // to be executed
        this.securityService = new SecurityService();
    }

    public void addDevice(Controllable device) {
        try {
            if (!(device instanceof SmartDevice smartDevice)) {
                throw new IllegalArgumentException("Device must be a SmartDevice");
            }
            devices.add(smartDevice);
        } catch (IllegalArgumentException e) {
            System.out.println("Error adding device: " + e.getMessage());
        }
    }

    public String reportStatus() {
        try {
            StringBuilder report = new StringBuilder("Smart Home Status Report:\n");// value added
            for (SmartDevice device : devices) {
                report.append(device.getName()).append(": ").append(device.getStatus()).append("\n");
            }
            report.append("Security Check: ").append(securityService.checkSecurity() ? "Secure" : "Not Secure");
            return report.toString();
        } catch (Exception e) {
            System.out.println("Error reporting status: " + e.getMessage());
            return "Error generating status report";
        }
    }

    public void runAutomation(AutomationRule rule) {
        try {
            if (rule == null) {
                throw new IllegalArgumentException("Automation rule cannot be null");
            }
            for (SmartDevice device : devices) {
                rule.visit(device);
            }
            System.out.println("Automation rule executed successfully.");
        } catch (IllegalArgumentException e) {
            System.out.println("Error running automation: " + e.getMessage());
        }

    }
    // value added
    public List<SmartDevice> getDevices() {
        return new ArrayList<>(devices);
    }
}
