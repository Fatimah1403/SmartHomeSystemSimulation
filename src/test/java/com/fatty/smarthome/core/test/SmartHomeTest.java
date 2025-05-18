package com.fatty.smarthome.core.test;

import com.fatty.smarthome.core.FacadeSmartHome;
import com.fatty.smarthome.core.LightAutomationRule;
import com.fatty.smarthome.core.SmartHome;
import com.fatty.smarthome.devices.Light;
import com.fatty.smarthome.util.SmartHomeException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.io.BufferedReader;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Paths;

import static junit.framework.Assert.assertEquals;
import static junit.framework.Assert.assertTrue;
import static org.junit.jupiter.api.Assertions.assertThrows;

public class SmartHomeTest {
    private FacadeSmartHome facade;


    @BeforeEach
    void setUp() throws IOException {
       facade = FacadeSmartHome.getTheInstance();
       facade.reset();
       Files.write(Paths.get("device_log.txt"), new byte[0]);
    }
    @Test
    void testAddLight() throws SmartHomeException, IOException {
        String result = facade.smartHomeAccess("add", "LivingRoomLight", "light");
        assertEquals("Added LivingRoomLight", result);
        assertTrue(readLogFile().contains("LivingRoomLight is OFF"));
    }
    @Test
    void testTurnOnLight() throws SmartHomeException, IOException {
        facade.smartHomeAccess("add", "LivingRoomLight", "light");
        String result = facade.smartHomeAccess("turnon", "LivingRoomLight", "");
        assertEquals("LivingRoomLight turned ON", result);
        assertTrue(readLogFile().contains("LivingRoomLight is ON"));
    }

    @Test
    void testTurnOffLight() throws SmartHomeException, IOException {
        facade.smartHomeAccess("add", "LivingRoomLight", "light");
        facade.smartHomeAccess("turnon", "LivingRoomLight", "");
        String result = facade.smartHomeAccess("turnoff", "LivingRoomLight", "");
        assertEquals("LivingRoomLight turned OFF", result);
        assertTrue(readLogFile().contains("LivingRoomLight is OFF"));
    }

    @Test
    void testSetTemperature() throws SmartHomeException, IOException {
        facade.smartHomeAccess("add", "MainThermostat", "thermostat");
        String result = facade.smartHomeAccess("settemp", "MainThermostat", "25");
        assertEquals("Set MainThermostat to 25°C", result);
        assertTrue(readLogFile().contains("MainThermostat is OFF Temperature: 25°C"));
    }
    @Test
    void testReport() throws SmartHomeException {
        facade.smartHomeAccess("add", "LivingRoomLight", "light");
        facade.smartHomeAccess("add", "MainThermostat", "thermostat");
        String result = facade.smartHomeAccess("report", "", "");
        assertTrue(result.contains("Smart Home Status Report"));
        assertTrue(result.contains("LivingRoomLight: LivingRoomLight is OFF"));
        assertTrue(result.contains("MainThermostat: MainThermostat is OFF, Temperature: 21°C"));
        assertTrue(result.contains("Security Check: Secure"));
    }

    @Test
    void testClearLog() throws SmartHomeException, IOException {
        facade.smartHomeAccess("add", "LivingRoomLight", "light");
        String result = facade.smartHomeAccess("clearlog", "", "");
        assertEquals("Log file cleared successfully", result);
        assertEquals("", readLogFile());
    }
    // VALUE ADDED
    @Test
    void testInvalidCommand() {
        SmartHomeException thrown = assertThrows(SmartHomeException.class, () ->
                facade.smartHomeAccess("invalid", "", ""));
        assertEquals("Invalid command: invalid", thrown.getMessage());
    }
    // VALUE ADDED
    private String readLogFile() throws IOException {
        StringBuilder content = new StringBuilder();
        try (BufferedReader reader = new BufferedReader(new FileReader("device_log.txt"))) {
            String line;
            while ((line = reader.readLine()) != null) {
                content.append(line).append("\n");
            }
            return content.toString();

        }
    }



}
