package com.fatty.smarthome.concurrent.automation;

import com.fatty.smarthome.concurrent.devices.ConcurrentLight;
import com.fatty.smarthome.devices.Light;
import com.fatty.smarthome.devices.SecurityCamera;
import com.fatty.smarthome.devices.SmartDevice;
import com.fatty.smarthome.util.SmartHomeException;

import java.util.List;
import java.util.concurrent.atomic.AtomicBoolean;

public class SecurityRule implements ConcurrentRule{
    private final String name;
    private final AtomicBoolean motionDetected = new AtomicBoolean(false);
    private final AtomicBoolean enabled = new AtomicBoolean(true);
    private volatile long lastTriggered = 0;
    private static final long COOLDOWN_MS = 30000; // 30 second cooldown

    public SecurityRule(String name) {
        this.name = name;
    }

    /**
     * Simulate motion detection
     */
    public void triggerMotion() {
        motionDetected.set(true);
    }

    @Override
    public String getName() {
        return name;
    }

    @Override
    public boolean shouldExecute() {
        if (!isEnabled()) return false;

        // Check cooldown period
        if (System.currentTimeMillis() - lastTriggered < COOLDOWN_MS) {
            return false;
        }

        return motionDetected.get();
    }

    @Override
    public void execute(List<SmartDevice> devices) throws SmartHomeException {
        if (motionDetected.compareAndSet(true, false)) {
            System.out.println("🚨 " + getName() + " activated! Securing home...");
            lastTriggered = System.currentTimeMillis();

            // Turn on all lights
            int lightsOn = 0;
            for (SmartDevice device : devices) {
                if (device instanceof Light && !device.isOn()) {
                    device.turnOn();
                    lightsOn++;

                    // Set brightness to maximum if it's a concurrent light
                    if (device instanceof ConcurrentLight) {
                        ((ConcurrentLight) device).setBrightness(100);
                    }
                }
            }

            // Activate all cameras
            int camerasOn = 0;
            for (SmartDevice device : devices) {
                if (device instanceof SecurityCamera && !device.isOn()) {
                    device.turnOn();
                    camerasOn++;
                }
            }

            System.out.println("✅ Security response: " + lightsOn +
                    " lights and " + camerasOn + " cameras activated");
        }
    }
    @Override
    public int getPriority() {
        return 10; // Highest priority for security
    }

    @Override
    public boolean isEnabled() {
        return enabled.get();
    }

    public void setEnabled(boolean enabled) {
        this.enabled.set(enabled);
    }
}
