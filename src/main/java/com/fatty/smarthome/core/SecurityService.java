package com.fatty.smarthome.core;

import com.fatty.smarthome.devices.Controllable;
import com.fatty.smarthome.devices.SmartDevice;

import java.util.List;

public class SecurityService {
    public <T extends SmartDevice & Controllable> boolean checkSecurity(List<T> devices) {
        if (devices == null || devices.isEmpty()) {
            System.out.println("Security check: No devices to check, all systems secure");
            return true;

        }
        for (T device : devices) {
            if (device.getStatus().contains("OFF")) {
                System.out.println("Security check: Device" + device.getName() + " is OFF, insecure");
                return false;
            }
        }
        System.out.println("Security check: All devices OS are ON, all systems secure");
        return true;
    }
}
