package com.fatty.smarthome.core.test;

import com.fatty.smarthome.core.FacadeSmartHome;
import com.fatty.smarthome.util.SmartHomeException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.lang.reflect.Field;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.List;

import static junit.framework.Assert.assertEquals;
import static org.junit.jupiter.api.Assertions.*;

public class FacadeSmartHomeTest {
    private FacadeSmartHome facade;

    @BeforeEach
    void setUp() throws IOException {
        facade = FacadeSmartHome.getTheInstance();
        facade.reset();
        Files.deleteIfExists(Paths.get("device_log.txt"));
        Files.createFile(Paths.get("device_log.txt"));
    }
    @Test
    void testSingletonInstance() {
        FacadeSmartHome instance1 = FacadeSmartHome.getTheInstance();
        FacadeSmartHome instance2 = FacadeSmartHome.getTheInstance();
        assertSame(instance1, instance2, "FacadeSmartHome should return the same instance");
    }
    @Test
    void testCaseInsensitiveCommand() throws SmartHomeException {
        String result = facade.smartHomeAccess("ADD", "LivingRoomLight", "light");
        assertEquals("Added LivingRoomLight", result);
    }
    @Test
   void  testInvalidCommand() {
        SmartHomeException thrown = assertThrows(SmartHomeException.class, () ->
                facade.smartHomeAccess("invalid", "", ""));
        assertEquals("Invalid command: invalid", thrown.getMessage());
    }
    @Test
    void testNonExistentDevice() {
        SmartHomeException thrown = assertThrows(SmartHomeException.class, () ->
                facade.smartHomeAccess("turnon", "UnknownDevice", ""));
        assertEquals("Device not found: UnknownDevice", thrown.getMessage());
    }

    @Test
    void testNonExistentThermostat() throws SmartHomeException {
        facade.smartHomeAccess("add", "LivingRoomLight", "light");
        SmartHomeException thrown = assertThrows(SmartHomeException.class, () ->
                facade.smartHomeAccess("settemp", "UnknownThermostat", "25"));
        assertEquals("Thermostat not found: UnknownThermostat", thrown.getMessage());
    }

    @Test
    void testInvalidTemperatureFormat() throws SmartHomeException {
        facade.smartHomeAccess("add", "MainThermostat", "thermostat");
        SmartHomeException thrown = assertThrows(SmartHomeException.class, () ->
                facade.smartHomeAccess("settemp", "MainThermostat", "invalid"));
        assertEquals("Invalid temperature: invalid", thrown.getMessage());
    }

    @Test
    void testHistory() throws SmartHomeException {
        facade.smartHomeAccess("add", "LivingRoomLight", "light");
        facade.smartHomeAccess("turnon", "LivingRoomLight", "");
        String result = facade.smartHomeAccess("history", "", "");
        assertTrue(result.contains("add LivingRoomLight light"));
        assertTrue(result.contains("turnon LivingRoomLight"));
        assertTrue(result.contains("history"));
    }

    @Test
    void testReset() throws SmartHomeException, NoSuchFieldException, IllegalAccessException {
        facade.smartHomeAccess("add", "LivingRoomLight", "light");
        facade.smartHomeAccess("history", "", "");
        facade.reset();

        // Use reflection to access commandHistory directly
        Field historyField = FacadeSmartHome.class.getDeclaredField("commandHistory");
        historyField.setAccessible(true);
        List<String> commandHistory = (List<String>) historyField.get(facade);
        assertTrue(commandHistory.isEmpty(), "Command history should be empty after reset");
    }

    @Test
    void testCommandArguments() {
        // Test invalid argument counts
        SmartHomeException addThrown = assertThrows(SmartHomeException.class, () ->
                facade.smartHomeAccess("add", "LivingRoomLight", ""));
        assertEquals("Invalid device type: ", addThrown.getMessage());

        SmartHomeException turnOnThrown = assertThrows(SmartHomeException.class, () ->
                facade.smartHomeAccess("turnon", "", ""));
        assertEquals("Device not found: ", turnOnThrown.getMessage());

        SmartHomeException setTempThrown = assertThrows(SmartHomeException.class, () ->
                facade.smartHomeAccess("settemp", "MainThermostat", ""));
        assertEquals("Invalid temperature: ", setTempThrown.getMessage());

        // Test commands that require no arguments
        assertDoesNotThrow(() -> facade.smartHomeAccess("automate", "", ""));
        assertDoesNotThrow(() -> facade.smartHomeAccess("report", "", ""));
        assertDoesNotThrow(() -> facade.smartHomeAccess("clearlog", "", ""));
        assertDoesNotThrow(() -> facade.smartHomeAccess("history", "", ""));
    }


}
