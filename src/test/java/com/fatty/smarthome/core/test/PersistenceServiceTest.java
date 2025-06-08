package com.fatty.smarthome.core.test;


import com.fatty.smarthome.core.DeviceState;
import com.fatty.smarthome.core.PersistenceService;
import com.fatty.smarthome.devices.Light;
import com.fatty.smarthome.devices.SecurityCamera;
import com.fatty.smarthome.devices.SmartDevice;
import com.fatty.smarthome.devices.Thermostat;
import com.fatty.smarthome.util.SmartHomeException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

/**
 * Test class for PersistenceService
 * Tests both binary and JSON serialization/deserialization
 */
import java.io.File;
import java.io.FileNotFoundException;
import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;


public class PersistenceServiceTest {
    private PersistenceService persistenceService;
    private List<SmartDevice> testDevices;

    @BeforeEach
    public void setUp() {
        // Create a new service instance for each test
        persistenceService = new PersistenceService();

        // Create test devices
        testDevices = new ArrayList<>();

        Light light = new Light("TestLight");
        light.turnOn();
        testDevices.add(light);

        Thermostat thermostat = new Thermostat("TestThermostat");
        thermostat.setTemperature(25);
        thermostat.turnOn();
        testDevices.add(thermostat);

        SecurityCamera camera = new SecurityCamera("TestCamera"); // VALUE ADDED
        camera.turnOn();
        testDevices.add(camera);
    }
    @Test
    void testSaveAndLoadBinary() throws SmartHomeException, FileNotFoundException {
        // save devices to binary file
        persistenceService.saveDeviceStatesBinary(testDevices);

        // load devices from binary file
        List<DeviceState> loadedStates = persistenceService.loadDeviceStatesBinary();

        // assert that the loaded states match the saved states
        assertNotNull(loadedStates);
        assertEquals(3, loadedStates.size());

        // Check  first device (Light)
        DeviceState lightState = loadedStates.get(0);
        assertEquals("TestLight", lightState.getDeviceName());
        assertTrue(lightState.isOn());
        assertNull(lightState.getTemperature());

        // Check second device (Thermostat)
        DeviceState thermostatState = loadedStates.get(1);
        assertEquals("TestThermostat", thermostatState.getDeviceName());
        assertEquals("Thermostat", thermostatState.getDeviceType());
        assertTrue(thermostatState.isOn());
        assertEquals(25, thermostatState.getTemperature());

        // Check third device (SecurityCamera) VALUE ADDED
        DeviceState cameraState = loadedStates.get(2);
        assertEquals("TestCamera", cameraState.getDeviceName());
        assertEquals("SecurityCamera", cameraState.getDeviceType());
        assertTrue(cameraState.isOn());
        assertNull(cameraState.getTemperature());

    }
    @Test
    void testSaveAndLoadJson() throws SmartHomeException, FileNotFoundException {
        // save devices to JSON file
        persistenceService.saveDeviceStatesJson(testDevices);

        // load devices from JSON file
        List<DeviceState> loadedStates = persistenceService.loadDeviceStatesJson();

        // Verify the loaded data
        assertNotNull(loadedStates);
        assertEquals(3, loadedStates.size());

        // Verify JSON file exists and is readable
        File jsonFile = new File("devices_states.json");
        assertTrue(jsonFile.exists());
        assertTrue(jsonFile.canRead());
        assertTrue(jsonFile.length() > 0);
    }
    @Test
    void testReconstructThermostat() throws SmartHomeException {
        // Create a thermostat state
        DeviceState state = new DeviceState("TestThermo", "Thermostat", false, "TestThermo is OFF");
        state.setTemperature(25);

        // Reconstruct the device
        SmartDevice device = persistenceService.reconstructDevice(state);

        // verify it's a thermostat with correct temperature
        assertTrue(device instanceof Thermostat);
        Thermostat thermostat = (Thermostat) device;
        assertEquals(25, thermostat.getTemperature());
    }
    @Test
    void testEmptyFileHandling() throws SmartHomeException, FileNotFoundException {
        // Delete existing files to test empty file handling
        new File("devices_states.json").delete();
        new File("devices_states.dat").delete();

        // Create a new  service (which should create empty files)
        PersistenceService newService = new PersistenceService();

        // Load from empty files should return empty lists
        List<DeviceState> binaryStates = newService.loadDeviceStatesBinary();
        List<DeviceState> jsonStates = newService.loadDeviceStatesJson();

        assertTrue(binaryStates.isEmpty());
        assertTrue(jsonStates.isEmpty());
        assertEquals(0, binaryStates.size());
        assertEquals(0, jsonStates.size());
    }
    @Test
    void testInvalidDeviceType() {
        // Create state with invalid device type
        DeviceState state = new DeviceState("Invalid", "unknownType", false, "Invalid device");

        // Attempting to reconstruct should throw exception
        assertThrows(SmartHomeException.class, ()-> {
            persistenceService.reconstructDevice(state);
        });
    }
}
