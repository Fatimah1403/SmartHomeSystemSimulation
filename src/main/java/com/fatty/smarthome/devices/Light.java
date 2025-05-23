package com.fatty.smarthome.devices;

public class Light  extends SmartDevice {
    public Light(String name) {
        super(name);
    }
    @Override
    public void turnOn() {
        super.turnOn();
        System.out.println(getName() + " is now ON.");
    }

    @Override
    public void turnOff() {
        super.turnOff();
        System.out.println(getName() + " is now OFF.");
    }

}