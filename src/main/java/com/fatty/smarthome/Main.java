package com.fatty.smarthome;

import com.fatty.smarthome.devices.Light;
import com.fatty.smarthome.devices.SecurityCamera;
import com.fatty.smarthome.devices.Thermostat;

public class Main {
    public static void main(String[] args) {
        try{
            SmartHome smartHome = new SmartHome();

            Light livingRoomLight = new Light("Living Room Light");
            Thermostat thermostat = new Thermostat("Main Thermostat");
            SecurityCamera camera = new SecurityCamera("Front Door Camera");

            // Adding the devices to the smart home
            smartHome.addDevice(livingRoomLight);
            smartHome.addDevice(thermostat);
            smartHome.addDevice(camera);

            System.out.println("User 1: Turn on the living room light");
            livingRoomLight.turnOn();

            System.out.println("\nUser 2: Set thermostat to 75°C");
            thermostat.setTemperature(75);

            System.out.println("\nUser 3: Turn on the Front Door camera");
            camera.turnOn();

            System.out.println("Running light automation rule....");
            smartHome.runAutomation(new LightAutomationRule());

            System.out.println("\n" + smartHome.reportStatus());
        } catch (Exception e) {
            System.err.println("Error in simulation: " + e.getMessage());
        }
    }
}