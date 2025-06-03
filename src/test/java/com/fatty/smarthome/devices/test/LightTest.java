package com.fatty.smarthome.devices.test;

import com.fatty.smarthome.devices.Light;
import org.junit.jupiter.api.*;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertEquals;
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
