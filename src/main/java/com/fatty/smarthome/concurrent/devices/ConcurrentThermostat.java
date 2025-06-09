package com.fatty.smarthome.concurrent.devices;

import com.fatty.smarthome.concurrent.events.EventType;
import com.fatty.smarthome.devices.Thermostat;

import java.util.HashMap;
import java.util.Map;
import java.util.Random;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * Thread-safe implementation of a smart thermostat.
 * Manages temperature with heating/cooling states.
 */

public class ConcurrentThermostat extends ConcurrentSmartDevice {
    private final AtomicInteger temperature = new AtomicInteger(21);
    private volatile boolean heating = false;
    private volatile boolean cooling = false;
    private volatile int targetTemperature = 21;

    // Temperature constants
    private static final int MIN_TEMP = 10;
    private static final int MAX_TEMP = 32;
    private static final int COLD_THRESHOLD = 15;
    private static final int HOT_THRESHOLD = 28;

    // For temperature simulation
    private final Random random = new Random();
    private Object name;

    /**
     * Create a new concurrent thermostat
     * @param name The name of the thermostat
     */
    public ConcurrentThermostat(String name) {
        super(name);
    }


    public void setTemperature(int temp) {
        if (temp < MIN_TEMP || temp > MAX_TEMP) {
            throw new IllegalArgumentException(
                    "Temperature must be between " + MIN_TEMP + " and " + MAX_TEMP + "°C");
        }

        int oldTemp = temperature.getAndSet(temp);

        // Update heating/cooling state
        lock.writeLock().lock();
        try {
            targetTemperature = temp;

            if (temp > oldTemp) {
                heating = true;
                cooling = false;
                System.out.println("🔥 " + name + " heating: " + oldTemp + "°C → " + temp + "°C");
            } else if (temp < oldTemp) {
                cooling = true;
                heating = false;
                System.out.println("❄️  " + name + " cooling: " + oldTemp + "°C → " + temp + "°C");
            } else {
                heating = false;
                cooling = false;
            }
        } finally {
            lock.writeLock().unlock();
        }

        // Emit temperature alert if needed
        if (eventSystem != null) {
            if (temp < COLD_THRESHOLD || temp > HOT_THRESHOLD) {
                Map<String, Object> data = new HashMap<>();
                data.put("device", name);
                data.put("temperature", temp);
                data.put("previousTemperature", oldTemp);
                data.put("alert", temp < COLD_THRESHOLD ? "TOO_COLD" : "TOO_HOT");
                data.put("timestamp", System.currentTimeMillis());

                emitEvent(EventType.TEMPERATURE_ALERT, data);
            }
        }
    }


    public int getTemperature() {
        return temperature.get();
    }


    /**
     * Get the target temperature
     * @return The target temperature
     */
    public int getTargetTemperature() {
        lock.readLock().lock();
        try {
            return targetTemperature;
        } finally {
            lock.readLock().unlock();
        }
    }

    /**
     * Check if thermostat is heating
     * @return true if heating
     */
    public boolean isHeating() {
        lock.readLock().lock();
        try {
            return heating;
        } finally {
            lock.readLock().unlock();
        }
    }

    /**
     * Check if thermostat is cooling
     * @return true if cooling
     */
    public boolean isCooling() {
        lock.readLock().lock();
        try {
            return cooling;
        } finally {
            lock.readLock().unlock();
        }
    }

    /**
     * Simulate temperature drift over time
     * This simulates real-world temperature changes
     */
    public void simulateTemperatureDrift() {
        if (!isOn()) return;

        int currentTemp = temperature.get();
        int drift = random.nextInt(3) - 1; // -1, 0, or 1

        // Apply drift based on heating/cooling state
        lock.readLock().lock();
        try {
            if (heating && currentTemp < targetTemperature) {
                drift = 1; // Always increase when heating
            } else if (cooling && currentTemp > targetTemperature) {
                drift = -1; // Always decrease when cooling
            }
        } finally {
            lock.readLock().unlock();
        }

        int newTemp = currentTemp + drift;

        // Keep within valid range
        if (newTemp >= MIN_TEMP && newTemp <= MAX_TEMP && newTemp != currentTemp) {
            temperature.set(newTemp);
            System.out.println("🌡️  " + name + " temperature drifted to " + newTemp + "°C");

            // Check if we reached target
            lock.writeLock().lock();
            try {
                if (newTemp == targetTemperature) {
                    heating = false;
                    cooling = false;
                    System.out.println("✅ " + name + " reached target temperature");
                }
            } finally {
                lock.writeLock().unlock();
            }
        }
    }

    /**
     * Set to eco mode (energy saving temperature)
     */
    public void setEcoMode() {
        if (isOn()) {
            setTemperature(20); // Eco temperature
            System.out.println("🌱 " + name + " set to eco mode (20°C)");
        }
    }

    /**
     * Set to comfort mode
     */
    public void setComfortMode() {
        if (isOn()) {
            setTemperature(22); // Comfort temperature
            System.out.println("😊 " + name + " set to comfort mode (22°C)");
        }
    }

    @Override
    public String getStatus() {
        lock.readLock().lock();
        try {
            StringBuilder status = new StringBuilder(super.getStatus());
            status.append(", Temperature: ").append(temperature.get()).append("°C");

            if (targetTemperature != temperature.get()) {
                status.append(" (Target: ").append(targetTemperature).append("°C)");
            }

            if (heating) status.append(" 🔥");
            if (cooling) status.append(" ❄️");

            return status.toString();
        } finally {
            lock.readLock().unlock();
        }
    }

    @Override
    protected void onTurnOff() {
        lock.writeLock().lock();
        try {
            heating = false;
            cooling = false;
        } finally {
            lock.writeLock().unlock();
        }
    }

    /**
     * Calculate approximate power consumption
     * @return Estimated watts
     */
    public double getEstimatedPowerConsumption() {
        if (!isOn()) return 0;

        lock.readLock().lock();
        try {
            if (heating || cooling) {
                return 150; // Active heating/cooling
            } else {
                return 50; // Standby power
            }
        } finally {
            lock.readLock().unlock();
        }
    }
}
