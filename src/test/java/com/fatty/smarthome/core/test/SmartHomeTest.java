package com.fatty.smarthome.core.test;

import com.fatty.smarthome.core.SmartHome;
import com.fatty.smarthome.core.DatabaseService;
import com.fatty.smarthome.core.DeviceManager;
import com.fatty.smarthome.core.LightAutomationRule;
import com.fatty.smarthome.core.SecurityService;
import com.fatty.smarthome.devices.Light;
import com.fatty.smarthome.devices.SecurityCamera;
import com.fatty.smarthome.devices.SmartDevice;
import com.fatty.smarthome.devices.Thermostat;
import com.fatty.smarthome.util.SmartHomeException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertThrows;


public class SmartHomeTest {
    private SmartHome smartHome;
    Path path = Paths.get("device_log.txt");



    @BeforeEach
    void setUp() throws IOException {
        smartHome = new SmartHome();
        // Value-added to ensure clean log file for test isolation, supporting Week 3 file I/O
        Files.deleteIfExists(path);
       Files.createFile(path);
    }
    @Test
    void testAddDevice() throws SmartHomeException {
        Light light = new Light("LivingRoomLight");
        smartHome.addDevice(light);
        List<SmartDevice> devices = smartHome.getDevices();
        assertEquals(1, devices.size());
        assertEquals("LivingRoomLight", devices.get(0).getName());

    }
    @Test
    void testAddDuplicateDevice() throws SmartHomeException {
        Light light = new Light("LivingRoomLight");
        assertDoesNotThrow(() -> smartHome.addDevice(light));
        SmartHomeException thrown = assertThrows(SmartHomeException.class, () ->
                smartHome.addDevice(new Light("LivingRoomLight")));
        assertEquals("Device already exists: LivingRoomLight", thrown.getMessage());
    }

    @Test
    void testAddNullDevice() {
        SmartHomeException thrown = assertThrows(SmartHomeException.class, () ->
                smartHome.addDevice(null));
        assertEquals("Cannot add null device", thrown.getMessage());
    }
    @Test
    void testReportStatusEmpty() throws SmartHomeException {
        assertEquals("No devices in the system", smartHome.reportStatus());
    }
    @Test
    void testReportStatusWithDevices() throws SmartHomeException {
        smartHome.addDevice(new Light("LivingRoomLight"));
        smartHome.addDevice(new Thermostat("MainThermostat"));
        smartHome.addDevice(new SecurityCamera("KitchenCamera"));
        String report = smartHome.reportStatus();
        assertTrue(report.contains("LivingRoomLight is OFF"));
        assertTrue(report.contains("MainThermostat is OFF, Temperature: 21°C"));
        assertTrue(report.contains("KitchenCamera is OFF"));
        assertTrue(report.contains("Security status: false"));
    }
    @Test
    void testSecurityServiceIntegration() throws SmartHomeException {
        Light light = new Light("LivingRoomLight");
        SecurityCamera camera = new SecurityCamera("KitchenCamera");
        smartHome.addDevice(light);
        smartHome.addDevice(camera);
        light.turnOn();
        camera.turnOn();
        smartHome.saveDevice(light);
        smartHome.saveDevice(camera);
        String report = smartHome.reportStatus();
        assertTrue(report.contains("Security status: true"));
    }
    @Test
    void testRunAutomation() throws SmartHomeException {
        Light light = new Light("LivingRoomLight");
        SecurityCamera camera = new SecurityCamera("KitchenCamera");
        smartHome.addDevice(light);
        smartHome.addDevice(camera);
        smartHome.runAutomation(new LightAutomationRule());
        String report = smartHome.reportStatus();
        assertTrue(report.contains("LivingRoomLight is ON"));
        assertTrue(report.contains("KitchenCamera is OFF"));
    }
    @Test
    void testClearLogFile() throws SmartHomeException {
        smartHome.addDevice((new Light("LivingRoomLight")));
        smartHome.addDevice(new SecurityCamera("KitchenCamera"));
        smartHome.clearLogFile();
        List<DatabaseService.LogEntry> logs = smartHome.readLog();
        assertEquals(0, logs.size());
        assertTrue(logs.isEmpty());
    }
    @Test
    void testReadLog() throws SmartHomeException {
        smartHome.addDevice(new Light("LivingRoomLight"));
        List<DatabaseService.LogEntry> logs = smartHome.readLog();
        assertFalse(logs.isEmpty());
        assertEquals("LivingRoomLight", logs.getFirst().getDeviceName());
        assertEquals("LivingRoomLight is OFF", logs.getFirst().getStatus());
    }
    @Test
    void testInvalidLogEntry() throws IOException {
        Files.write(path, "invalid\n".getBytes());
        SmartHomeException thrown = assertThrows(SmartHomeException.class, () ->
                smartHome.readLog());
        assertEquals("Invalid log entry: invalid", thrown.getMessage());
    }
    @Test
    void testSaveDevice() throws SmartHomeException { // value added
        Thermostat thermostat = new Thermostat("MainThermostat");
        thermostat.setTemperature(25);
        smartHome.addDevice(thermostat);
        smartHome.saveDevice(thermostat);
        List<DatabaseService.LogEntry> logs = smartHome.readLog();
        assertTrue(logs.stream().anyMatch(log ->
                log.getDeviceName().equals("MainThermostat") &&
                log.getStatus().contains("Temperature: 25°C")));

    }
    @Test
    void testGenericTypeSafety() throws SmartHomeException {
        DeviceManager<SmartDevice> deviceManager = new DeviceManager<>(
                new DatabaseService(), new SecurityService());
        Light light = new Light("LivingRoomLight");
        SecurityCamera camera = new SecurityCamera("KitchenCamera");
        Thermostat thermostat = new Thermostat("MainThermostat");
        deviceManager.addDevice(light);
        deviceManager.addDevice(camera);
        deviceManager.addDevice(thermostat);
        List<SmartDevice> devices = deviceManager.getDevices();
        assertEquals(3, devices.size(), "Expected 3 devices in DeviceManager");
        assertTrue(devices.contains(light), "Devices should contain LivingRoomLight");
        assertTrue(devices.contains(camera), "Devices should contain KitchenCamera");
        assertTrue(devices.contains(thermostat), "Devices should contain MainThermostat");
    }
}
