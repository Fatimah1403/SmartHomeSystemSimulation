package com.fatty.smarthome.devices;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static junit.framework.Assert.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

public class ThermostatTest {
    private Thermostat thermostat;

    @BeforeEach
    void setUp() {
        thermostat = new Thermostat("Test Thermostat");
    }
    @Test
    void testSetTemperature() {
        thermostat.setTemperature(75);
        assertEquals(75, thermostat.getTemperature());
        assertEquals("Test Thermostat is OFF, Temperature: 75°C", thermostat.getStatus());
    }

    @Test
    void testInvalidTemperature() {
        thermostat.setTemperature(100);
        assertEquals(70, thermostat.getTemperature());
        thermostat.setTemperature(40);
        assertEquals(70, thermostat.getTemperature());
    }
    @Test
    void testTurnOn() {
        thermostat.turnOn();
        assertEquals("Test Thermostat is ON, Temperature: 70°C", thermostat.getStatus());
    }
    @Test
    void testInvalidName() {
        assertThrows(IllegalArgumentException.class, () -> new Thermostat(null));
    }
}
