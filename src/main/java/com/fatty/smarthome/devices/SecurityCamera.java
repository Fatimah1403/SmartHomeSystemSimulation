package com.fatty.smarthome.devices;

public class SecurityCamera extends com.fatty.smarthome.devices.SmartDevice {
    public SecurityCamera(String name) {
        super(name);
    }
    @Override // value added because of coding convention
    public void turnOn() {
        super.turnOn();
        System.out.println(getName() + " is now ON. Recording...."); // value added
    }
    @Override // value added because of coding convention
    public void turnOff() {
        super.turnOff();
        System.out.println(getName() + " is now OFF. Stopped recording...");
    }

}
