package com.fatty.smarthome.core;

import com.fatty.smarthome.devices.SmartDevice;

// placeholder for now, value added
public class DatabaseService {
    public void save(SmartDevice device) {
        try {
            if (device == null) {
                throw new IllegalArgumentException("Cannot save null device");
            }
            System.out.println("Logged " + device.getName() + " status: " + device.getStatus() + "."); // value added
        } catch (IllegalArgumentException e) {
            System.out.println("Error logging device: " + e.getMessage());
        }
    }
}
