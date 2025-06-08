package com.fatty.smarthome.devices.test;

import com.fatty.smarthome.devices.Controllable;
import com.fatty.smarthome.devices.Light;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

public class ControllableTest {
    @Test
    void testLightImplementsControllable() {
        Controllable light = new Light("Test light");
        light.turnOn();
        assertTrue(light.getStatus().contains("ON"));
        assertEquals("Test light is ON", light.getStatus());

    }
    @Test
    void testLightTurnOnAndOff() {
        Controllable light = new Light("Test light");
        light.turnOn();
        light.turnOff();
        assertEquals("Test light is OFF", light.getStatus());
    }
}
