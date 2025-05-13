package com.fatty.smarthome.devices;

public class Thermostat extends SmartDevice {
    private int temperature;

    public Thermostat(String name) {
        super(name);
        // setting the temperature to °C instead of °F are value added
        this.temperature = 70; // set default temperature , value added
    }
    @Override
    public void turnOn() {
        isOn = true;
        System.out.println(name + " is now ON. Setting temperature to " + temperature + "°C.");// value added
    }
    public void turnOff() {
        isOn = false;
        System.out.println(name + " is now OFF.");
    }

    public void setTemperature(int temperature) {
        try { // All the try catches are value added
            if (temperature < 50 || temperature > 90) {
                throw new IllegalArgumentException("Temperature must be between 50°C and 90°C");// value added
            }
            this.temperature = temperature;
        } catch (Exception e) {
            System.out.println(e.getMessage());
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
