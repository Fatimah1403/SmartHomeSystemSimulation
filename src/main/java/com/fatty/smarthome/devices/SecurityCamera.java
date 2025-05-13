package com.fatty.smarthome.devices;

public class SecurityCamera extends SmartDevice{
    public SecurityCamera(String name) {
        super(name);
    }
    @Override // value added because of coding convention
    public void turnOn() {
        isOn = true;
        System.out.println(name + " is now ON. Recording...."); // value added
    }
    @Override // value added because of coding convention
    public void turnOff() {
        isOn = false;
        System.out.println(name + " is now OFF. Stopped recording...");
    }
    @Override
    public String getStatus() {
        return isOn ? "Recording" : "Not Recording"; // value added
    }
}
