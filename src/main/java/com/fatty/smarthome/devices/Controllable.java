package com.fatty.smarthome.devices;

public interface Controllable {
    void turnOn();
    /**
     * Turn the device off.
     */
    void turnOff();
    String getStatus();
}
