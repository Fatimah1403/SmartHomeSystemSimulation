package com.fatty.smarthome.concurrent.devices;

import com.fatty.smarthome.concurrent.events.EventSystem;
import com.fatty.smarthome.devices.SmartDevice;
import com.fatty.smarthome.util.SmartHomeException;

public class ConcurrentDeviceFactory {
    private final EventSystem eventSystem;

    /**
     * Create a new device factory
     * @param eventSystem The event system to connect devices to
     */
    public ConcurrentDeviceFactory(EventSystem eventSystem) throws SmartHomeException {
        if (eventSystem == null) {
            throw new SmartHomeException("EventSystem cannot be null");
        }
        this.eventSystem = eventSystem;
    }
    /**
     * Create a new concurrent device of the specified type
     * @param name The name of the device
     * @param type The type of device (light, thermostat, camera, securitycamera)
     * @return The created device
     * @throws IllegalArgumentException if type is unknown
     */
    public SmartDevice createDevice(String name, String type) {
        if (name == null || name.trim().isEmpty()) {
            throw new IllegalArgumentException("Device name cannot be null or empty");
        }

        if (type == null || type.trim().isEmpty()) {
            throw new IllegalArgumentException("Device type cannot be null or empty");
        }

        ConcurrentSmartDevice device;

        switch (type.toLowerCase().trim()) {
            case "light":
                device = new ConcurrentLight(name);
                break;

            case "thermostat":
                device = new ConcurrentThermostat(name);
                break;

            case "camera":
            case "securitycamera":
                device = new ConcurrentSecurityCamera(name);
                break;

            default:
                throw new IllegalArgumentException("Unknown device type: " + type +
                        ". Supported types are: light, thermostat, camera");
        }
        // Connect device to event system
        device.setEventSystem(eventSystem);

        System.out.println("🏭 Created concurrent " + type + ": " + name);

        return device;
    }
    /**
     * Create a light with initial brightness
     * @param name The name of the light
     * @param initialBrightness The initial brightness (0-100)
     * @return The created light
     */
    public ConcurrentLight createLight(String name, int initialBrightness) throws SmartHomeException {
        ConcurrentLight light = (ConcurrentLight) createDevice(name, "light");
        light.setBrightness(initialBrightness);
        return light;
    }

    /**
     * Create a thermostat with initial temperature
     * @param name The name of the thermostat
     * @param initialTemp The initial temperature (10-32°C)
     * @return The created thermostat
     */
    public ConcurrentThermostat createThermostat(String name, int initialTemp) {
        ConcurrentThermostat thermostat = (ConcurrentThermostat) createDevice(name, "thermostat");
        thermostat.setTemperature(initialTemp);
        return thermostat;
    }

    /**
     * Create a security camera that starts recording immediately
     * @param name The name of the camera
     * @param autoStart Whether to turn on and start recording immediately
     * @return The created camera
     */
    public ConcurrentSecurityCamera createSecurityCamera(String name, boolean autoStart) {
        ConcurrentSecurityCamera camera = (ConcurrentSecurityCamera) createDevice(name, "camera");
        if (autoStart) {
            camera.turnOn();
            camera.startRecording();
        }
        return camera;
    }

    /**
     * Create multiple devices at once
     * @param deviceSpecs Array of device specifications in format "name:type"
     * @return Array of created devices
     */
    public SmartDevice[] createMultipleDevices(String... deviceSpecs) {
        SmartDevice[] devices = new SmartDevice[deviceSpecs.length];

        for (int i = 0; i < deviceSpecs.length; i++) {
            String spec = deviceSpecs[i];
            String[] parts = spec.split(":");

            if (parts.length != 2) {
                throw new IllegalArgumentException("Invalid device spec: " + spec +
                        ". Format should be 'name:type'");
            }

            devices[i] = createDevice(parts[0].trim(), parts[1].trim());
        }

        return devices;
    }

    /**
     * Create a standard home setup with common devices
     * @param prefix Prefix for device names (e.g., "Home1")
     * @return Array of created devices
     */
    public SmartDevice[] createStandardHomeSetup(String prefix) {
        return new SmartDevice[] {
                createDevice(prefix + "_LivingRoomLight", "light"),
                createDevice(prefix + "_KitchenLight", "light"),
                createDevice(prefix + "_BedroomLight", "light"),
                createDevice(prefix + "_MainThermostat", "thermostat"),
                createDevice(prefix + "_FrontDoorCam", "camera"),
                createDevice(prefix + "_BackyardCam", "camera")
        };
    }

    /**
     * Get the event system used by this factory
     * @return The event system
     */
    public EventSystem getEventSystem() {
        return eventSystem;
    }
}
