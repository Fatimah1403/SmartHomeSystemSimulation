package com.fatty.smarthome.devices;

public abstract class SmartDevice implements com.fatty.smarthome.devices.Controllable {
    protected String name;
    public boolean isOn;

    public SmartDevice(String name) {
        if (name == null || name.trim().isEmpty()) {    // values added to check
            throw new IllegalArgumentException("Device name cannot be null or empty");// for null name
        }
        this.name = name;
        this.isOn = false;
    }

    @Override
    public void turnOn() {
        isOn = true;
    }

    @Override
    public void turnOff() {
        isOn = false;
    }

    @Override
    public String getStatus() {
        return name + " is " + (isOn ? "ON" : "OFF");
    }
    public String getName() {
        return name;
    }

    public boolean isOn() {
        return isOn;
    }
}
