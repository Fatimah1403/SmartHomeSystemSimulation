package com.fatty.smarthome.core.test;

import com.fatty.smarthome.core.FacadeSmartHome;
import com.fatty.smarthome.util.SmartHomeException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.io.BufferedReader;
import java.io.FileReader;
import java.io.IOException;
import java.lang.reflect.Field;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.List;

//import static junit.framework.Assert.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertFalse;

import static org.junit.jupiter.api.Assertions.*;

public class FacadeSmartHomeTest {
    private FacadeSmartHome facade;
    private static final Path path = Paths.get("device_log.txt");

    @BeforeEach
    void setUp() throws IOException {
        facade = FacadeSmartHome.getTheInstance();
        facade.reset();
        Files.write(path, new byte[0]);
    }

    @Test
    void testAddLight() throws SmartHomeException, IOException {
        String result = facade.smartHomeAccess("add", "LivingRoomLight", "light");
        assertEquals("Added LivingRoomLight", result);
        assertTrue(readLogFile().contains("LivingRoomLight,LivingRoomLight is OFF"));
    }

    @Test
    void testAddCamera() throws SmartHomeException, IOException {
        String result = facade.smartHomeAccess("add", "KitchenCamera", "camera");
        assertEquals("Added KitchenCamera", result);
        assertTrue(readLogFile().contains("KitchenCamera,KitchenCamera is OFF"));
    }

    @Test
    void testAddThermostat() throws SmartHomeException, IOException {
        String result = facade.smartHomeAccess("add", "MainThermostat", "thermostat");
        assertEquals("Added MainThermostat", result);
        assertTrue(readLogFile().contains("MainThermostat,MainThermostat is OFF Temperature: 21°C"));
    }

    @Test
    void testTurnOnLight() throws SmartHomeException, IOException {
        facade.smartHomeAccess("add", "LivingRoomLight", "light");
        String result = facade.smartHomeAccess("turnon", "LivingRoomLight", "");
        assertEquals("LivingRoomLight turned ON", result);
        assertTrue(readLogFile().contains("LivingRoomLight,LivingRoomLight is ON"));
    }

    @Test
    void testTurnOffCamera() throws SmartHomeException, IOException {
        facade.smartHomeAccess("add", "KitchenCamera", "camera");
        facade.smartHomeAccess("turnon", "KitchenCamera", "");
        String result = facade.smartHomeAccess("turnoff", "KitchenCamera", "");
        assertEquals("KitchenCamera turned OFF", result);
        assertTrue(readLogFile().contains("KitchenCamera,KitchenCamera is OFF"));
    }

    @Test
    void testSetTemperature() throws SmartHomeException, IOException {
        facade.smartHomeAccess("add", "MainThermostat", "thermostat");
        String result = facade.smartHomeAccess("settemp", "MainThermostat", "25");
        assertEquals("Set MainThermostat to 25°C", result);
        assertTrue(readLogFile().contains("MainThermostat,MainThermostat is OFF Temperature: 21°C"));
    }

    @Test
    void testReport() throws SmartHomeException {
        facade.smartHomeAccess("add", "LivingRoomLight", "light");
        facade.smartHomeAccess("add", "KitchenCamera", "camera");
        facade.smartHomeAccess("turnon", "LivingRoomLight", "");
        String result = facade.smartHomeAccess("report", "", "");
        assertTrue(result.contains("LivingRoomLight is ON"));
        assertTrue(result.contains("KitchenCamera is OFF"));
        assertTrue(result.contains("Security status: false")); // Camera OFF, insecure
        // <span style="color:red">Tests SecurityService’s generic <T extends SmartDevice & Controllable>.</span>
    }

    @Test
    void testReadLog() throws SmartHomeException {
        facade.smartHomeAccess("add", "LivingRoomLight", "light");
        String result = facade.smartHomeAccess("readlog", "", "");
        assertTrue(result.contains("LivingRoomLight - LivingRoomLight is OFF"));
        // <span style="color:red">Validates List<LogEntry> in DatabaseService.</span>
    }

    @Test
    void testHistory() throws SmartHomeException {
        facade.smartHomeAccess("add", "LivingRoomLight", "light");
        facade.smartHomeAccess("turnon", "LivingRoomLight", "");
        String result = facade.smartHomeAccess("history", "", "");
        assertEquals("add LivingRoomLight light\nturnon LivingRoomLight\nhistory", result);
    }

    @Test
    void testAutomate() throws SmartHomeException, IOException {
        facade.smartHomeAccess("add", "LivingRoomLight", "light");
        facade.smartHomeAccess("add", "KitchenCamera", "camera");
        String result = facade.smartHomeAccess("automate", "", "");
        assertEquals("Automation rule applied", result);
        assertTrue(readLogFile().contains("LivingRoomLight,LivingRoomLight is ON"));
        assertTrue(readLogFile().contains("KitchenCamera,KitchenCamera is OFF"));
        // <span style="color:red">Tests DeviceManager’s List<T> with LightAutomationRule.</span>
    }

    @Test
    void testClearLog() throws SmartHomeException, IOException {
        facade.smartHomeAccess("add", "LivingRoomLight", "light");
        String result = facade.smartHomeAccess("clearlog", "", "");
        assertEquals("Log file cleared successfully", result);
        assertEquals("", readLogFile());
    }

    @Test
    void testReset() throws SmartHomeException {
        facade.smartHomeAccess("add", "LivingRoomLight", "light");
        facade.smartHomeAccess("turnon", "LivingRoomLight", "");
        facade.reset();
        String report = facade.smartHomeAccess("report", "", "");
        assertEquals("No devices in the system", report);
        String history = facade.smartHomeAccess("history", "", "");
        assertEquals("report\nhistory", history);
    }

    @Test
    void testInvalidCommand() {
        SmartHomeException thrown = assertThrows(SmartHomeException.class, () ->
                facade.smartHomeAccess("invalid", "", ""));
        assertEquals("Invalid command: invalid", thrown.getMessage());
        // <span style="color:red">Value-added test for invalid command handling, prompted for robust error checking.</span>
    }

    @Test
    void testAddDuplicateDevice() {
        assertDoesNotThrow(() -> facade.smartHomeAccess("add", "LivingRoomLight", "light"));
        SmartHomeException thrown = assertThrows(SmartHomeException.class, () ->
                facade.smartHomeAccess("add", "LivingRoomLight", "light"));
        assertEquals("Device already exists: LivingRoomLight", thrown.getMessage());
    }

    @Test
    void testInvalidDeviceType() {
        SmartHomeException thrown = assertThrows(SmartHomeException.class, () ->
                facade.smartHomeAccess("add", "InvalidDevice", "unknown"));
        assertEquals("Invalid device type: unknown", thrown.getMessage());
    }

    @Test
    void testInvalidTemperature() {
        assertDoesNotThrow(() -> facade.smartHomeAccess("add", "MainThermostat", "thermostat"));
        SmartHomeException thrown = assertThrows(SmartHomeException.class, () ->
                facade.smartHomeAccess("settemp", "MainThermostat", "40"));
        assertEquals("Temperature must be between 10 and 32°C", thrown.getMessage());
    }

    @Test
    void testNonExistentDevice() {
        SmartHomeException thrown = assertThrows(SmartHomeException.class, () ->
                facade.smartHomeAccess("turnon", "UnknownDevice", ""));
        assertEquals("Device not found: UnknownDevice", thrown.getMessage());
    }

    private String readLogFile() throws IOException {
        StringBuilder content = new StringBuilder();
        try (BufferedReader reader = new BufferedReader( new FileReader("device_log.txt"))) {
            String line;
            while ((line = reader.readLine()) != null) {
                String[] parts = line.split(",");
                if (parts.length == 3) {
                    content.append(parts[1]).append(",").append(parts[2]).append("\n");
                }
            }
        }
        return content.toString();

    }



}
