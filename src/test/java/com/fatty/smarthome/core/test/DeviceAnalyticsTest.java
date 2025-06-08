package com.fatty.smarthome.core.test;

import com.fatty.smarthome.core.DeviceAnalytics;
import com.fatty.smarthome.devices.Light;
import com.fatty.smarthome.devices.SecurityCamera;
import com.fatty.smarthome.devices.SmartDevice;
import com.fatty.smarthome.devices.Thermostat;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.*;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

public class DeviceAnalyticsTest {
    private List<SmartDevice> testDevices;

    @BeforeEach
    void setUp() {
        testDevices = new ArrayList<>();

        // Create various test devices
        Light light1 = new Light("LivingRoomLight");
        light1.turnOn();
        testDevices.add(light1);

        Light light2 = new Light("KitchenLight");
        testDevices.add(light2);

        Thermostat thermo1 = new Thermostat("MainThermostat");
        thermo1.setTemperature(22);
        thermo1.turnOn();
        testDevices.add(thermo1);

        Thermostat thermo2 = new Thermostat("BedroomThermostat");
        thermo2.setTemperature(20);
        testDevices.add(thermo2);

        SecurityCamera camera = new SecurityCamera("FrontCamera");
        camera.turnOn();
        testDevices.add(camera);

    }
    @Test
    void testFilterByType() {
        List<Light> lights = DeviceAnalytics.filterByType(testDevices, Light.class);
        assertEquals(2, lights.size(), "Expected 2 lights");
        assertTrue(lights.stream().allMatch(Objects::nonNull), "Expected only Light objects");

        // Test filtering thermostats
        List<Thermostat> thermostats = DeviceAnalytics.filterByType(testDevices, Thermostat.class);
        assertEquals(2, thermostats.size());
        assertTrue(thermostats.stream().allMatch(Objects::nonNull), "Expected only Thermostat objects");

        // Test filtering cameras
        List<SecurityCamera> cameras = DeviceAnalytics.filterByType(testDevices, SecurityCamera.class);
        assertEquals(1, cameras.size());
        assertTrue(cameras.stream().allMatch(Objects::nonNull), "Expected only SecurityCamera objects");
    }
    @Test
    void testGetActiveDevices() {
        List<SmartDevice> activeDevices = DeviceAnalytics.getActiveDevices(testDevices);

        // Should have 3 active devices (Light, Thermostat, Camera)
        assertEquals(3, activeDevices.size());
        assertTrue(activeDevices.stream().allMatch(SmartDevice::isOn));
    }

    @Test
    void testGroupByPowerState() {
        Map<Boolean, List<SmartDevice>> grouped = DeviceAnalytics.groupByPowerState(testDevices);

        // Check ON devices
        assertEquals(3, grouped.get(true).size());

        // Check OFF devices
        assertEquals(2, grouped.get(false).size());
    }

    @Test
    void testCountByType() {
        Map<String, Long> counts = DeviceAnalytics.countByType(testDevices);

        assertEquals(3, counts.size());
        assertEquals(2L, counts.get("Light"));
        assertEquals(2L, counts.get("Thermostat"));
        assertEquals(1L, counts.get("SecurityCamera"));
    }

    @Test
    void testSearchByName() {
        // Search for "Room"
        List<SmartDevice> results = DeviceAnalytics.searchByName(testDevices, "Room");
        assertEquals(2, results.size()); // LivingRoomLight and BedroomThermostat

        // Search for "light" (case-insensitive)
        results = DeviceAnalytics.searchByName(testDevices, "light");
        assertEquals(2, results.size());

        // Search for non-existent
        results = DeviceAnalytics.searchByName(testDevices, "xyz");
        assertTrue(results.isEmpty());
    }

    @Test
    void testGetThermostatStatistics() {
        IntSummaryStatistics stats = DeviceAnalytics.getThermostatStatistics(testDevices);

        assertEquals(2, stats.getCount());
        assertEquals(20, stats.getMin());
        assertEquals(22, stats.getMax());
        assertEquals(21.0, stats.getAverage(), 0.01);
    }

    @Test
    void testGenerateReport() {
        String report = DeviceAnalytics.generateReport(testDevices);

        // Verify report contains expected information
        assertTrue(report.contains("Total devices: 5"));
        assertTrue(report.contains("Active devices: 3"));
        assertTrue(report.contains("Light: 2"));
        assertTrue(report.contains("Thermostat: 2"));
        assertTrue(report.contains("SecurityCamera: 1"));
    }
    @Test
    void testApplyToMatching() {
        // Turn off all lights
        int affected = DeviceAnalytics.applyToMatching(
                testDevices,
                device -> device instanceof Light,  // Predicate: is it a light?
                device -> { device.turnOff(); return null; }
        );

        assertEquals(2, affected);

        // Verify all lights are now off
        List<Light> lights = DeviceAnalytics.filterByType(testDevices, Light.class);
        assertTrue(lights.stream().noneMatch(SmartDevice::isOn));
    }

    @Test
    void testGetUniqueDeviceTypes() {
        Set<String> types = DeviceAnalytics.getUniqueDeviceTypes(testDevices);

        assertEquals(3, types.size());
        assertTrue(types.contains("Light"));
        assertTrue(types.contains("Thermostat"));
        assertTrue(types.contains("SecurityCamera"));
    }
    @Test
    void testFindLongestRunningDevice() {
        Optional<SmartDevice> device = DeviceAnalytics.findLongestRunningDevice(testDevices);

        assertTrue(device.isPresent());
        // Based on alphabetical order of ON devices
        assertEquals("FrontCamera", device.get().getName());
    }

    @Test
    void testEmptyDeviceList() {
        List<SmartDevice> emptyList = new ArrayList<>();

        // Test various methods with empty list
        assertTrue(DeviceAnalytics.getActiveDevices(emptyList).isEmpty());
        assertTrue(DeviceAnalytics.countByType(emptyList).isEmpty());
        assertEquals("No devices in the system", DeviceAnalytics.generateReport(emptyList));
        assertFalse(DeviceAnalytics.findLongestRunningDevice(emptyList).isPresent());
    }
}
