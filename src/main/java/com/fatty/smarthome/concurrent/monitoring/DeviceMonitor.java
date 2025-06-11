package com.fatty.smarthome.concurrent.monitoring;

import com.fatty.smarthome.devices.SmartDevice;
import com.fatty.smarthome.devices.Thermostat;
import com.fatty.smarthome.core.FacadeSmartHome;
import java.util.List;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicBoolean;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

/**
 * DeviceMonitor runs in a separate thread to continuously monitor all devices.
 * It checks device states, detects anomalies, and can trigger automated responses.
 */
public class DeviceMonitor implements Runnable {
    private final FacadeSmartHome facade;
    private final AtomicBoolean running = new AtomicBoolean(false);
    private final ConcurrentHashMap<String, DeviceStatus> deviceStatuses = new ConcurrentHashMap<>();
    private final int monitoringIntervalMs;
    private Thread monitorThread;

    // For demonstration, we'll track device uptimes and state changes
    private static class DeviceStatus {
        long lastStateChange = System.currentTimeMillis();
        boolean previousState = false;
        int stateChangeCount = 0;
        long totalOnTime = 0;
        long lastOnTime = 0;
    }

    public DeviceMonitor(FacadeSmartHome facade, int monitoringIntervalMs) {
        this.facade = facade;
        this.monitoringIntervalMs = monitoringIntervalMs;
    }

    /**
     * Start monitoring in a new thread
     */
    public void startMonitoring() {
        if (running.compareAndSet(false, true)) {
            monitorThread = new Thread(this, "DeviceMonitor-Thread");
            monitorThread.setDaemon(true); // Daemon thread dies when main program exits
            monitorThread.start();
            System.out.println("🔍 Device monitoring started (checking every " +
                    monitoringIntervalMs + "ms)");
        }
    }

    /**
     * Stop monitoring gracefully
     */
    public void stopMonitoring() {
        if (running.compareAndSet(true, false)) {
            if (monitorThread != null) {
                monitorThread.interrupt();
                try {
                    monitorThread.join(2000); // Wait up to 2 seconds for thread to finish
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                }
            }
            System.out.println("🛑 Device monitoring stopped");
        }
    }

    @Override
    public void run() {
        System.out.println("📡 Device monitor thread started: " + Thread.currentThread().getName());

        while (running.get() && !Thread.currentThread().isInterrupted()) {
            try {
                monitorDevices();
                Thread.sleep(monitoringIntervalMs);
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                break;
            } catch (Exception e) {
                System.err.println("❌ Monitor error: " + e.getMessage());
            }
        }

        System.out.println("📡 Device monitor thread ending");
    }

    /**
     * Monitor all devices and update their statuses
     */
    private void monitorDevices() {
        List<SmartDevice> devices = facade.getDevices();

        for (SmartDevice device : devices) {
            String deviceName = device.getName();
            boolean currentState = device.isOn();

            // Get or create device status
            DeviceStatus status = deviceStatuses.computeIfAbsent(deviceName,
                    k -> new DeviceStatus());

            // Check if state changed
            if (currentState != status.previousState) {
                handleStateChange(device, status, currentState);
            }

            // Update on-time if device is on
            if (currentState && status.lastOnTime > 0) {
                long currentOnDuration = System.currentTimeMillis() - status.lastOnTime;

                // Alert if device has been on too long (e.g., > 1 hour)
                if (currentOnDuration > 3600000) { // 1 hour in milliseconds
                    System.out.println("⚠️  ALERT: " + deviceName +
                            " has been ON for over " + (currentOnDuration / 60000) + " minutes!");
                }
            }

            // Check thermostat temperatures
            if (device instanceof Thermostat) {
                checkThermostatTemp((Thermostat) device);
            }
        }
    }

    /**
     * Handle device state changes
     */
    private void handleStateChange(SmartDevice device, DeviceStatus status, boolean newState) {
        String timestamp = LocalDateTime.now()
                .format(DateTimeFormatter.ofPattern("HH:mm:ss"));

        System.out.println("🔄 [" + timestamp + "] State change detected: " +
                device.getName() + " → " + (newState ? "ON" : "OFF"));

        status.previousState = newState;
        status.lastStateChange = System.currentTimeMillis();
        status.stateChangeCount++;

        if (newState) {
            status.lastOnTime = System.currentTimeMillis();
        } else if (status.lastOnTime > 0) {
            // Device turned off, calculate on-time
            long onDuration = System.currentTimeMillis() - status.lastOnTime;
            status.totalOnTime += onDuration;
            status.lastOnTime = 0;

            System.out.println("📊 " + device.getName() + " was ON for " +
                    (onDuration / 1000) + " seconds");
        }

        // Trigger automation based on state changes
        if (status.stateChangeCount > 10) {
            System.out.println("⚡ Frequent state changes detected for " +
                    device.getName() + " (" + status.stateChangeCount + " changes)");
        }
    }

    /**
     * Check thermostat temperatures for anomalies
     */
    private void checkThermostatTemp(Thermostat thermostat) {
        int temp = thermostat.getTemperature();

        // Alert for extreme temperatures
        if (temp < 15) {
            System.out.println("❄️  COLD ALERT: " + thermostat.getName() +
                    " temperature is " + temp + "°C");
        } else if (temp > 28) {
            System.out.println("🔥 HEAT ALERT: " + thermostat.getName() +
                    " temperature is " + temp + "°C");
        }
    }

    /**
     * Get monitoring statistics for a device
     */
    public String getDeviceStats(String deviceName) {
        DeviceStatus status = deviceStatuses.get(deviceName);
        if (status == null) {
            return "No statistics available for " + deviceName;
        }

        long totalTime = System.currentTimeMillis() - status.lastStateChange;
        long onTime = status.totalOnTime;
        if (status.previousState && status.lastOnTime > 0) {
            onTime += System.currentTimeMillis() - status.lastOnTime;
        }

        return String.format(
                "%s Statistics:\n" +
                        "  State changes: %d\n" +
                        "  Total ON time: %.1f minutes\n" +
                        "  Current state duration: %.1f seconds",
                deviceName,
                status.stateChangeCount,
                onTime / 60000.0,
                totalTime / 1000.0
        );
    }

    public boolean isRunning() {
        return running.get();
    }
}