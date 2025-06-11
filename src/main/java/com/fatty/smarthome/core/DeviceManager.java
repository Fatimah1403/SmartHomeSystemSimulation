package com.fatty.smarthome.core;

import com.fatty.smarthome.devices.Controllable;
import com.fatty.smarthome.devices.SmartDevice;
import com.fatty.smarthome.util.SmartHomeException;

import java.util.ArrayList;
import java.util.List;

// Value added: introduced DeviceManager with generics to
// ensure type-safe device handling, replacing raw List<Controllable>
public class DeviceManager<T extends SmartDevice & Controllable> {
    private final List<T> devices;
    private final SecurityService securityService;
    private final DatabaseService dbService;

    public DeviceManager(DatabaseService dbService, SecurityService securityService) {
        this.devices = new ArrayList<>();
        this.dbService = dbService;
        this.securityService = securityService;
    }

    public void addDevice(T device) throws SmartHomeException {
        if (device == null) {
            throw new SmartHomeException("Cannot add null device");
        }
        for (T d : devices) {
            if (d.getName().equals(device.getName())) {
                throw new SmartHomeException("Device already exists: " + device.getName());
            }
        }
        devices.add(device);
        dbService.save(device);
        System.out.println("Device added: " + device.getName());
    }
    public void saveDevice(T device) throws SmartHomeException {
        dbService.save(device);
    }
    public List<T> getDevices() {
        return new ArrayList<>(devices); // Return a copy of the devices list>
    }
    public String reportStatus() throws SmartHomeException {
        if (devices.isEmpty()) {
            return "No devices in the system";
        }
        StringBuilder report = new StringBuilder();
        for (T device : devices) {
            report.append(device.getStatus()).append("\n");
        }
        report.append("Security status: ").append(securityService.checkSecurity(devices));
        return report.toString();
    }
    public void runAutomation(AutomationRule rule) throws SmartHomeException {
        for (T device : devices) {
            rule.visit(device);
        }
        for (T device : devices) {
            dbService.save(device);
        }
    }
    public void clearLogFile() throws SmartHomeException {
        dbService.clearLogFile();
    }

    public List<DatabaseService.LogEntry> readLog() throws SmartHomeException {
        return dbService.readLog();
    }

    public void reset() {
        devices.clear();
    }


}
