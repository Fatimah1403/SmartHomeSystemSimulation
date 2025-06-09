package com.fatty.smarthome.concurrent.devices;

import com.fatty.smarthome.concurrent.events.EventType;
import com.fatty.smarthome.util.SmartHomeException;

import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.atomic.AtomicInteger;

public class ConcurrentLight extends ConcurrentSmartDevice{
    private final AtomicInteger brightness = new AtomicInteger(100);
    private static final int MIN_BRIGHTNESS = 0;
    private static final int MAX_BRIGHTNESS = 100;
    private static final int SIGNIFICANT_BRIGHTNESS_CHANGE = 20;

    /**
     * Create a new concurrent light
     * @param name The name of the light
     */
    public ConcurrentLight(String name) {
        super(name);
    }
    /**
     * Set the brightness level (0-100)
     * @param level The brightness level
     * throwSmartHomeException if level is out of range
     */
    public void setBrightness(int level) throws SmartHomeException {
        if (level < MIN_BRIGHTNESS || level > MAX_BRIGHTNESS) {
            throw new SmartHomeException(
                    "Brightness must be between " + MIN_BRIGHTNESS + " and " + MAX_BRIGHTNESS);
        }

        int oldBrightness = brightness.getAndSet(level);

        System.out.println("💡 " + name + " brightness: " + oldBrightness + "% → " + level + "%");
        // Emit event if brightness changed significantly
        if (Math.abs(oldBrightness - level) > SIGNIFICANT_BRIGHTNESS_CHANGE && eventSystem != null) {
            Map<String, Object> data = new HashMap<>();
            data.put("device", name);
            data.put("deviceType", "Light");
            data.put("oldBrightness", oldBrightness);
            data.put("newBrightness", level);
            data.put("timestamp", System.currentTimeMillis());

            emitEvent(EventType.DEVICE_STATE_CHANGED, data);
        }
    }

    /**
     * Get the current brightness level
     * @return The brightness level (0-100)
     */
    public int getBrightness() {
        return brightness.get();
    }
    /**
     * Increase brightness by a specified amount
     * @param amount The amount to increase (will cap at 100)
     */
    public void increaseBrightness(int amount) throws SmartHomeException {
        int current = brightness.get();
        int newBrightness = Math.min(current + amount, MAX_BRIGHTNESS);
        setBrightness(newBrightness);
    }

    /**
     * Decrease brightness by a specified amount
     * @param amount The amount to decrease (will floor at 0)
     */
    public void decreaseBrightness(int amount) throws SmartHomeException {
        int current = brightness.get();
        int newBrightness = Math.max(current - amount, MIN_BRIGHTNESS);
        setBrightness(newBrightness);
    }

    /**
     * Toggle between full brightness and off
     */
    public void toggle() throws SmartHomeException {
        lock.writeLock().lock();
        try {
            if (isOn) {
                turnOff();
            } else {
                turnOn();
                setBrightness(100);
            }
        } finally {
            lock.writeLock().unlock();
        }
    }

    /**
     * Dim the light to 25%
     */
    public void dim() throws SmartHomeException {
        if (isOn()) {
            setBrightness(25);
        }
    }

    /**
     * Set light to medium brightness (50%)
     */
    public void setMedium() throws SmartHomeException {
        if (isOn()) {
            setBrightness(50);
        }
    }

    /**
     * Set light to full brightness
     */
    public void setBright() throws SmartHomeException {
        if (isOn()) {
            setBrightness(100);
        }
    }

    @Override
    public String getStatus() {
        lock.readLock().lock();
        try {
            String baseStatus = super.getStatus();
            return baseStatus + ", Brightness: " + brightness.get() + "%";
        } finally {
            lock.readLock().unlock();
        }
    }

    @Override
    protected void onTurnOff() {
        // Optionally reset brightness when turned off
        // brightness.set(100); // Uncomment if you want to reset brightness
    }

    /**
     * Calculate approximate power consumption based on brightness
     * @return Estimated watts
     */
    public double getEstimatedPowerConsumption() {
        if (!isOn()) return 0;
        // Assume 10W base + up to 50W based on brightness
        return 10 + (brightness.get() / 100.0 * 50);
    }
}
