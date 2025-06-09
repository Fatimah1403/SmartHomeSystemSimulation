package com.fatty.smarthome.concurrent.devices;

import com.fatty.smarthome.concurrent.events.Event;
import com.fatty.smarthome.concurrent.events.EventSystem;
import com.fatty.smarthome.concurrent.events.EventType;
import com.fatty.smarthome.devices.SmartDevice;

import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.locks.ReadWriteLock;
import java.util.concurrent.locks.ReentrantReadWriteLock;

public class ConcurrentSmartDevice extends SmartDevice {
    protected final String name;
    protected volatile boolean isOn = false;
    protected final ReadWriteLock lock = new ReentrantReadWriteLock();
    protected EventSystem eventSystem;

    // Track device metrics
    protected volatile long lastStateChangeTime = System.currentTimeMillis();
    protected volatile int stateChangeCount = 0;

    /**
     * Constructor
     * @param name The unique name of this device
     */
    public ConcurrentSmartDevice(String name) {
        super(name);
        this.name = name;
    }

    /**
     * Set the event system for this device
     * @param eventSystem The event system to use
     */
    public void setEventSystem(EventSystem eventSystem) {
        this.eventSystem = eventSystem;
    }

    @Override
    public String getName() {
        return name;
    }

    @Override
    public void turnOn() {
        lock.writeLock().lock();
        try {
            if (!isOn) {
                isOn = true;
                onTurnOn(); // Hook for subclasses
                stateChangeCount++;
                lastStateChangeTime = System.currentTimeMillis();

                // Emit event
                emitStateChangeEvent("ON");

                System.out.println("✅ " + name + " is now ON (Thread: " +
                        Thread.currentThread().getName() + ")");
            }
        } finally {
            lock.writeLock().unlock();
        }
    }
    @Override
    public void turnOff() {
        lock.writeLock().lock();
        try {
            if (isOn) {
                isOn = false;
                onTurnOff(); // Hook for subclasses
                stateChangeCount++;
                lastStateChangeTime = System.currentTimeMillis();

                // Emit event
                emitStateChangeEvent("OFF");

                System.out.println("🔴 " + name + " is now OFF (Thread: " +
                        Thread.currentThread().getName() + ")");
            }
        } finally {
            lock.writeLock().unlock();
        }
    }
    @Override
    public boolean isOn() {
        lock.readLock().lock();
        try {
            return isOn;
        } finally {
            lock.readLock().unlock();
        }
    }

    @Override
    public String getStatus() {
        lock.readLock().lock();
        try {
            long uptime = System.currentTimeMillis() - lastStateChangeTime;
            return String.format("%s is %s (uptime: %.1fs, changes: %d)",
                    name,
                    isOn ? "ON" : "OFF",
                    uptime / 1000.0,
                    stateChangeCount);
        } finally {
            lock.readLock().unlock();
        }
    }

    /**
     * Get the number of state changes
     */
    public int getStateChangeCount() {
        lock.readLock().lock();
        try {
            return stateChangeCount;
        } finally {
            lock.readLock().unlock();
        }
    }

    /**
     * Get the last state change time
     */
    public long getLastStateChangeTime() {
        lock.readLock().lock();
        try {
            return lastStateChangeTime;
        } finally {
            lock.readLock().unlock();
        }
    }

    /**
     * Emit a state change event
     */
    protected void emitStateChangeEvent(String newState) {
        if (eventSystem != null) {
            Map<String, Object> data = new HashMap<>();
            data.put("device", name);
            data.put("deviceType", this.getClass().getSimpleName());
            data.put("newState", newState);
            data.put("changeCount", stateChangeCount);
            data.put("timestamp", System.currentTimeMillis());

            eventSystem.publishEvent(new Event(
                    EventType.DEVICE_STATE_CHANGED,
                    name,
                    data
            ));
        }
    }

    /**
     * Emit a custom event
     */
    protected void emitEvent(EventType type, Map<String, Object> data) {
        if (eventSystem != null) {
            eventSystem.publishEvent(new Event(type, name, data));
        }
    }

    /**
     * Simulate device malfunction
     */
    public void simulateMalfunction() {
        if (eventSystem != null) {
            Map<String, Object> data = new HashMap<>();
            data.put("device", name);
            data.put("deviceType", this.getClass().getSimpleName());
            data.put("error", "Device not responding");
            data.put("timestamp", System.currentTimeMillis());

            eventSystem.publishEvent(new Event(
                    EventType.DEVICE_MALFUNCTION,
                    name,
                    data
            ));

            System.out.println("⚠️  MALFUNCTION: " + name + " is not responding!");
        }
    }

    // Hooks for subclasses to override
    protected void onTurnOn() {}
    protected void onTurnOff() {}

    @Override
    public String toString() {
        return getStatus();
    }

}
