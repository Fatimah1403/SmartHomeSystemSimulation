package com.fatty.smarthome;

import com.fatty.smarthome.devices.Light;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static junit.framework.Assert.assertEquals;
import static junit.framework.Assert.assertTrue;

public class SmartHomeTest {
    private SmartHome smartHome;
    private Light light;

    @BeforeEach
    void setUp() {
        smartHome = new SmartHome();
        light = new Light("Test Light");
    }
    @Test
    void testAddDevice() {
        smartHome.addDevice(light);
        assertEquals(1, smartHome.getDevices().size());
        assertEquals("Test Light", smartHome.getDevices().getFirst().getName());
    }

    @Test
    void testRunAutomation() {
        smartHome.addDevice(light);
        light.turnOff();
        smartHome.runAutomation(new LightAutomationRule());
        assertEquals("Test Light is ON", smartHome.getDevices().getFirst().getStatus());
    }

    @Test
    void testReportStatus() {
        smartHome.addDevice(light);
        light.turnOn();
        String status = smartHome.reportStatus();
        assertTrue(status.contains("Test Light is ON"));
        assertTrue(status.contains("Security Check: Secure"));
    }
}
