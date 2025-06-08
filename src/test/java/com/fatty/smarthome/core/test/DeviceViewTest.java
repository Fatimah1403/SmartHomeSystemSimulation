package com.fatty.smarthome.core.test;

import com.fatty.smarthome.core.DeviceView;
import com.fatty.smarthome.devices.Light;
import com.fatty.smarthome.devices.SmartDevice;
import com.fatty.smarthome.devices.Thermostat;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

public class DeviceViewTest {
    private SmartDevice testLight;
    private SmartDevice testThermostat;

    @BeforeEach
    void setUp() {
        testLight = new Light("TestLight");
        testThermostat = new Thermostat("TestThermostat");
    }

    @Test
    void testDeviceViewCreation() {
        DeviceView view = new DeviceView(testLight);

        // Test initial values
        assertEquals("TestLight", view.getName());
        assertEquals("Light", view.getType());
        assertEquals("TestLight is OFF", view.getStatus());
        assertFalse(view.isOn());
        assertSame(testLight, view.getDevice());
    }
    @Test
    void testDeviceViewWithOnDevice() {
        testLight.turnOn();
        DeviceView view = new DeviceView(testLight);

        assertEquals("TestLight is ON", view.getStatus());
//        assertEquals("ON", view.isOn());
    }
    @Test
    void testDeviceViewWithThermostat() {
        Thermostat thermo = (Thermostat) testThermostat;
        thermo.setTemperature(25);
        thermo.turnOn();

        DeviceView view = new DeviceView(thermo);

        assertEquals("TestThermostat", view.getName());
        assertEquals("Thermostat", view.getType());
        assertEquals("TestThermostat is ON, Temperature: 25°C", view.getStatus());
//        assertEquals("ON", view.isOn());
    }
    @Test
    void testPropertyGetters() {
        DeviceView view = new DeviceView(testLight);

        // Test that properties are not null
        assertNotNull(view.nameProperty());
        assertNotNull(view.typeProperty());
        assertNotNull(view.statusProperty());
        assertNotNull(view.onProperty());

        // Test property values
        assertEquals("TestLight", view.nameProperty().get());
        assertEquals("Light", view.typeProperty().get());
        assertEquals("OFF", view.onProperty().get());
    }
    @Test
    void testSetters() {
        DeviceView view = new DeviceView(testLight);

        // Test setters
        view.setName("NewName");
        assertEquals("NewName", view.getName());

        view.setType("NewType");
        assertEquals("NewType", view.getType());

        view.setStatus("Custom Status");
        assertEquals("Custom Status", view.getStatus());

        view.setOn(true);
//        assertEquals("true", view.isOn());
    }
}
