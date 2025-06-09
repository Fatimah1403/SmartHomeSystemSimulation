package com.fatty.smarthome.concurrent.automation;

import com.fatty.smarthome.devices.SmartDevice;
import com.fatty.smarthome.devices.Thermostat;

import java.util.List;
import java.util.concurrent.atomic.AtomicBoolean;

public class TemperatureRule implements ConcurrentRule {
    private final String name;
    private final int targetTemp;
    private final int tolerance;
    private final AtomicBoolean enabled = new AtomicBoolean(true);

    public TemperatureRule(String name, int targetTemp, int tolerance) {
        this.name = name;
        this.targetTemp = targetTemp;
        this.tolerance = tolerance;
    }
    @Override
    public String getName() {
        return name;
    }

    @Override
    public boolean shouldExecute() {
        return isEnabled();
    }
    @Override
    public void execute(List<SmartDevice> devices) {
        devices.stream()
                .filter(d -> d instanceof Thermostat && d.isOn())
                .map(d -> (Thermostat) d)
                .forEach(thermostat -> {
                    int currentTemp = thermostat.getTemperature();
                    int diff = Math.abs(currentTemp - targetTemp);

                    if (diff > tolerance) {
                        System.out.println("🌡️  " + getName() + " adjusting " +
                                thermostat.getName() + " from " + currentTemp +
                                "°C to " + targetTemp + "°C");
                        thermostat.setTemperature(targetTemp);
                    }
                });
    }
    @Override
    public int getPriority() {
        return 5; // Higher priority for comfort
    }

    @Override
    public boolean isEnabled() {
        return enabled.get();
    }

    public void setEnabled(boolean enabled) {
        this.enabled.set(enabled);
    }
}
