package com.fatty.smarthome.core;

import com.fatty.smarthome.devices.Light;
import com.fatty.smarthome.devices.SmartDevice;

public class LightAutomationRule implements ConcurrentRule {
    @Override
    public void visit(SmartDevice device) {
        try {
            // Down casting to check if device is a Light
            if (device instanceof Light light) {
                if (!light.getStatus().contains("ON")) {
                    light.turnOn();
                }
            }
        } catch (Exception e) {
            System.err.println("Error in AutomationRule: " + e.getMessage());
        }
    }
}
