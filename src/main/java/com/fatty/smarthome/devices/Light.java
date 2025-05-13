package com.fatty.smarthome.devices;

public class Light  extends SmartDevice {
    public Light(String name) {
        super(name);
    }
    @Override
    public void turnOn() {
        isOn = true;
        System.out.println(name + " is now ON.");
    }

    @Override
    public void turnOff() {
        isOn = false;
        System.out.println(name + " is now OFF.");
    }

}