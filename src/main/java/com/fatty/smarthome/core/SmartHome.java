package com.fatty.smarthome.core;

import com.fatty.smarthome.devices.SmartDevice;
import com.fatty.smarthome.util.SmartHomeException;

import java.sql.SQLException;
import java.util.List;

// Changed DeviceManager<Controllable> to
// DeviceManager<SmartDevice> to satisfy generic bound T extends SmartDevice
// & Controllable, preserving SmartHome as subsystem coordinator
public class SmartHome {
    private final com.fatty.smarthome.core.DeviceManager<SmartDevice> deviceManager;

    public SmartHome() throws SQLException {
        com.fatty.smarthome.core.DatabaseService dbService = DatabaseService.getInstance();
        SecurityService securityService = new SecurityService();
        this.deviceManager = new DeviceManager<>(dbService, securityService);
    }

    public void addDevice(SmartDevice device) throws SmartHomeException {
        deviceManager.addDevice(device);
    }

    public void saveDevice(SmartDevice device) throws SmartHomeException {
        deviceManager.saveDevice(device);
    }

    public List<SmartDevice> getDevices() {
        return deviceManager.getDevices();
    }

    public String reportStatus() throws SmartHomeException {
        return deviceManager.reportStatus();
    }

    public void runAutomation(AutomationRule rule) throws SmartHomeException {
        deviceManager.runAutomation(rule);
    }

    public void clearLogFile() throws SmartHomeException {
        deviceManager.clearLogFile();
    }

    public List<DatabaseService.EventLog> readLog() throws SmartHomeException {
        return deviceManager.readLog();
    }

    public void reset() {
        deviceManager.reset();
    }
}