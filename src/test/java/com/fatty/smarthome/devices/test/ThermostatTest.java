package com.fatty.smarthome.devices;

import com.fatty.smarthome.util.SmartHomeException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

public class ThermostatTest {
    private Thermostat thermostat;

    @BeforeEach
    void setUp() {
        thermostat = new Thermostat("TestThermostat");
    }

    @Test
    void testSetTemperature() {
        thermostat.setTemperature(25);
        assertEquals(25, thermostat.getTemperature());
        assertEquals("TestThermostat is OFF, Temperature: 25°C", thermostat.getStatus());
    }

    @Test
    void testInvalidTemperature() {
        int initialTemp = thermostat.getTemperature(); // 21
        thermostat.setTemperature(100); // <span style="color:red">Removed assertThrows to align with caught exception, value-added for test accuracy.</span>
        assertEquals(initialTemp, thermostat.getTemperature(), "Temperature should remain 21°C on invalid input");
        thermostat.setTemperature(40);
        assertEquals(initialTemp, thermostat.getTemperature(), "Temperature should remain 21°C on invalid input");
    }

    @Test
    void testTurnOn() {
        thermostat.turnOn();
        assertEquals("TestThermostat is ON, Temperature: 21°C", thermostat.getStatus());
    }

    @Test
    void testInvalidName() {
        assertThrows(IllegalArgumentException.class, () -> new Thermostat(null));
        assertThrows(IllegalArgumentException.class, () -> new Thermostat(""));
    }
}
