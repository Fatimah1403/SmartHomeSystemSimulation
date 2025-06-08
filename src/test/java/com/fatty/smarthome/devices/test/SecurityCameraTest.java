package com.fatty.smarthome.devices.test;

import com.fatty.smarthome.devices.SecurityCamera;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

public class SecurityCameraTest {
    private SecurityCamera camera;

    @BeforeEach
    void setUp() {
        camera = new SecurityCamera("Test camera");
    }
    @Test
    void testTurnOn() {
        camera.turnOn();
        assertEquals("Test camera is ON", camera.getStatus());
    }
    @Test
    void testTurnOff() {
//        camera.turnOn();
        camera.turnOff();
        assertEquals("Test camera is OFF", camera.getStatus());
    }
    @Test
    void testInvalidName() { // value added
        assertThrows(IllegalArgumentException.class, () -> new SecurityCamera(""));
        assertThrows(IllegalArgumentException.class, () -> new SecurityCamera(null));
    }

}
