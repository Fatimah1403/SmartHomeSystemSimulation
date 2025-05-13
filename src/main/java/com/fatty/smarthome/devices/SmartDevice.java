package com.fatty.smarthome.devices;

public class SmartDevice implements Controllable {
    protected String name;
    protected boolean isOn;

    public SmartDevice(String name) {
        if (name == null || name.trim().isEmpty()) {    // values added to check
            throw new IllegalArgumentException("Device name cannot be null or empty");// for null name
        }
        this.name = name;
        this.isOn = false;
    }

    @Override
    public void turnOn() {

    }

    @Override
    public void turnOff() {

    }

    @Override
    public String getStatus() {
        return name + " is " + (isOn ? "on" : "off");
    }
    public String getName() {
        return name;
    }
}
