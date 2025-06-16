package com.fatty.smarthome.devices;

import com.fatty.smarthome.util.SmartHomeException;

public class Thermostat extends SmartDevice {
    private int temperature;

    public Thermostat(String name) {
        super(name);
        // setting the temperature to °C instead of °F are value added
        this.temperature = 21; // set default temperature , value added
    }
    @Override
    public void turnOn() {
       super.turnOn();
        System.out.println(getName() + " is now ON. Setting temperature to " + temperature + "°C.");// value added
    }
    public void turnOff() {
        super.turnOff();
        System.out.println(getName() + " is now OFF.");
    }

    public void setTemperature(int temperature) {
        try { // All the try catches are value added
            if (temperature < 10 || temperature > 32) {
                System.err.println("\u001B[31m❌ Temperature must be between 10°C and 32°C\u001B[0m");
                throw new SmartHomeException("Temperature must be between 10°C and 32°C");// value added
            }
            this.temperature = temperature;
        } catch (SmartHomeException e) {
            System.out.println("\u001B[32m✓ Temperature set to " + temperature + "°C\u001B[0m");
        }

    }
    public int getTemperature() {
        return temperature;
    }
    @Override
    public String getStatus() {
        return super.getStatus() + ", Temperature: " + temperature + "°C";
    }
}
