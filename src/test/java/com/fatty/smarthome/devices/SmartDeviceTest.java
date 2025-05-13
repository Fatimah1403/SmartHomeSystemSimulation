package com.fatty.smarthome.devices;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static junit.framework.Assert.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

public class SmartDeviceTest {
    private SmartDevice light;

    @BeforeEach
    void setUp() {
       light = new Light("Test Light");
    }
    @Test
    void testGetName() {
        assertEquals("Test Light", light.getName()); // value added

    }

    @Test
    void testGetStatus() {
        assertEquals("Test Light is OFF", light.getStatus());
        light.turnOn();
        assertEquals("Test Light is ON", light.getStatus());
    }
    @Test
    void testInvalidName() {
        assertThrows(IllegalArgumentException.class, () -> new Light(""));

    }
}
