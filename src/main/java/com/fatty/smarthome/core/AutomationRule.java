package com.fatty.smarthome.core;

import com.fatty.smarthome.devices.SmartDevice;

/**
 * Represents a rule that can be applied to a SmartDevice in an automated manner.
 * This interface is designed to define a visiting functionality for devices,
 * allowing specific automation logic to be executed based on the type or state of the device.
 */
public interface AutomationRule {
    void visit(SmartDevice device);
}
