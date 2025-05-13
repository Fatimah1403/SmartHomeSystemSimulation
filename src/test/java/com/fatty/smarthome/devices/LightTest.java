package com.fatty.smarthome.devices;

import org.junit.jupiter.api.Assertions.*;
import org.junit.jupiter.api.*;

import static junit.framework.Assert.*;
import static org.junit.jupiter.api.Assertions.assertThrows;

public class LightTest {
    private Light light;
    @BeforeEach
    void setUp() {
        light = new Light("Test light");
    }
    @Test
    void testTurnOn() {
        light.turnOn();
        assertEquals("Test light is ON", light.getStatus());
    }
    @Test
    void testTurnOff() {
        light.turnOff();
        assertEquals("Test light is OFF", light.getStatus());
    }
    @Test
    void testInvalidName() {
        assertThrows(IllegalArgumentException.class, () -> new Light(""));
        assertThrows(IllegalArgumentException.class, () -> new Light(null));
    }
}
