package com.fatty.smarthome.concurrent.monitoring;

import com.fatty.smarthome.concurrent.events.*;
import com.fatty.smarthome.devices.*;
import com.fatty.smarthome.core.FacadeSmartHome;
import java.util.*;
import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.DoubleAdder;

/**
 * PowerMonitoringService tracks power consumption across all devices
 * and alerts when thresholds are exceeded.
 */
public class PowerMonitoringService {
    private final FacadeSmartHome facade;
    private final EventSystem eventSystem;
    private final ScheduledExecutorService scheduler;
    private final Map<String, PowerMetrics> deviceMetrics;
    private final DoubleAdder totalPowerConsumed;
    private final AtomicInteger powerThreshold;
    private volatile boolean monitoring = false;

    /**
     * Power metrics for a device
     */
    private static class PowerMetrics {
        final String deviceName;
        final DoubleAdder totalConsumption = new DoubleAdder();
        volatile double currentPower = 0;
        volatile long lastUpdateTime = System.currentTimeMillis();

        PowerMetrics(String deviceName) {
            this.deviceName = deviceName;
        }

        void updatePower(double watts) {
            long now = System.currentTimeMillis();
            long duration = now - lastUpdateTime;

            // Calculate energy consumed (watt-hours)
            double hoursElapsed = duration / 3600000.0;
            double energyConsumed = currentPower * hoursElapsed;
            totalConsumption.add(energyConsumed);

            currentPower = watts;
            lastUpdateTime = now;
        }
    }

    public PowerMonitoringService(FacadeSmartHome facade, EventSystem eventSystem) {
        this.facade = facade;
        this.eventSystem = eventSystem;
        this.scheduler = Executors.newScheduledThreadPool(2);
        this.deviceMetrics = new ConcurrentHashMap<>();
        this.totalPowerConsumed = new DoubleAdder();
        this.powerThreshold = new AtomicInteger(500); // Default 500W threshold
    }

    /**
     * Start monitoring power consumption
     */
    public void startMonitoring() {
        if (monitoring) return;

        monitoring = true;

        // Schedule power calculation every 2 seconds
        scheduler.scheduleAtFixedRate(
                this::calculatePowerUsage, 0, 2, TimeUnit.SECONDS);

        // Schedule power report every 30 seconds
        scheduler.scheduleAtFixedRate(
                this::generatePowerReport, 30, 30, TimeUnit.SECONDS);

        System.out.println("⚡ Power monitoring service started");
    }

    /**
     * Calculate current power usage
     */
    private void calculatePowerUsage() {
        try {
            List<SmartDevice> devices = facade.getDevices();
            double totalPower = 0;

            // Calculate power for each device
            for (SmartDevice device : devices) {
                double devicePower = calculateDevicePower(device);

                PowerMetrics metrics = deviceMetrics.computeIfAbsent(
                        device.getName(), PowerMetrics::new);
                metrics.updatePower(devicePower);

                totalPower += devicePower;
            }

            // Check if threshold exceeded
            if (totalPower > powerThreshold.get()) {
                System.out.println("⚠️  POWER ALERT: Current usage " +
                        String.format("%.1fW", totalPower) +
                        " exceeds threshold " + powerThreshold.get() + "W!");

                // Emit power threshold event
                if (eventSystem != null) {
                    Map<String, Object> data = new HashMap<>();
                    data.put("currentPower", totalPower);
                    data.put("threshold", powerThreshold.get());
                    data.put("devices", getHighPowerDevices());

                    eventSystem.publishEvent(new Event(
                            EventType.POWER_THRESHOLD_EXCEEDED,
                            "PowerMonitor",
                            data
                    ));
                }
            }

            // Update total consumption
            totalPowerConsumed.add(totalPower * 2 / 3600.0); // Convert to Wh

        } catch (Exception e) {
            System.err.println("❌ Error calculating power: " + e.getMessage());
        }
    }

    /**
     * Calculate power consumption for a specific device
     */
    private double calculateDevicePower(SmartDevice device) {
        if (!device.isOn()) return 0;

        // Base power consumption by device type
        double basePower = 0;
        double activePower = 0;

        if (device instanceof Light) {
            basePower = 5;  // Standby power
            activePower = 60; // Active power for LED light

            // Could be extended to support brightness levels
            return basePower + activePower;

        } else if (device instanceof Thermostat) {
            Thermostat thermostat = (Thermostat) device;
            int temp = thermostat.getTemperature();
            basePower = 10; // Control unit power

            // Calculate HVAC power based on temperature difference
            int optimalTemp = 22;
            int tempDiff = Math.abs(temp - optimalTemp);

            if (tempDiff > 3) {
                activePower = 200; // High power for heating/cooling
            } else if (tempDiff > 1) {
                activePower = 100; // Medium power
            } else {
                activePower = 50;  // Low power maintenance
            }

            return basePower + activePower;

        } else if (device instanceof SecurityCamera) {
            basePower = 5;   // Standby power
            activePower = 15; // Recording power

            // Add extra power for night vision (simulated)
            Calendar cal = Calendar.getInstance();
            int hour = cal.get(Calendar.HOUR_OF_DAY);
            if (hour < 6 || hour > 18) {
                activePower += 5; // Night vision IR LEDs
            }

            return basePower + activePower;
        }

        return 5; // Default standby power
    }

    /**
     * Get list of high power consuming devices
     */
    private List<String> getHighPowerDevices() {
        return deviceMetrics.entrySet().stream()
                .filter(entry -> entry.getValue().currentPower > 50)
                .sorted((a, b) -> Double.compare(
                        b.getValue().currentPower,
                        a.getValue().currentPower))
                .limit(3)
                .map(entry -> String.format("%s (%.1fW)",
                        entry.getKey(),
                        entry.getValue().currentPower))
                .toList();
    }

    /**
     * Generate power consumption report
     */
    private void generatePowerReport() {
        System.out.println("\n📊 === POWER CONSUMPTION REPORT ===");
        System.out.println("Time: " + new Date());

        double currentTotal = deviceMetrics.values().stream()
                .mapToDouble(m -> m.currentPower)
                .sum();

        System.out.printf("Current Total Power: %.1f W\n", currentTotal);
        System.out.printf("Total Energy Consumed: %.2f Wh\n", totalPowerConsumed.sum());
        System.out.println("\nDevice Breakdown:");

        deviceMetrics.entrySet().stream()
                .filter(entry -> entry.getValue().currentPower > 0)
                .sorted((a, b) -> Double.compare(
                        b.getValue().currentPower,
                        a.getValue().currentPower))
                .forEach(entry -> {
                    PowerMetrics metrics = entry.getValue();
                    System.out.printf("  %-20s: %6.1f W (Total: %.2f Wh)\n",
                            entry.getKey(),
                            metrics.currentPower,
                            metrics.totalConsumption.sum());
                });

        // Add cost estimation
        double costPerKwh = 0.12; // $0.12 per kWh
        double totalKwh = totalPowerConsumed.sum() / 1000.0;
        double estimatedCost = totalKwh * costPerKwh;

        System.out.printf("\nEstimated Cost: $%.2f (at $%.2f/kWh)\n", estimatedCost, costPerKwh);
        System.out.println("================================\n");
    }

    /**
     * Set power threshold for alerts
     */
    public void setPowerThreshold(int watts) {
        powerThreshold.set(watts);
        System.out.println("⚡ Power threshold set to " + watts + "W");
    }

    /**
     * Get current power statistics
     */
    public String getPowerStatistics() {
        double currentTotal = deviceMetrics.values().stream()
                .mapToDouble(m -> m.currentPower)
                .sum();

        return String.format(
                "Power Statistics:\n" +
                        "  Current usage: %.1f W\n" +
                        "  Threshold: %d W\n" +
                        "  Total consumed: %.2f Wh\n" +
                        "  Active devices: %d",
                currentTotal,
                powerThreshold.get(),
                totalPowerConsumed.sum(),
                (int) deviceMetrics.values().stream()
                        .filter(m -> m.currentPower > 0)
                        .count()
        );
    }

    /**
     * Get device power history
     */
    public Map<String, Double> getDevicePowerSnapshot() {
        Map<String, Double> snapshot = new HashMap<>();
        deviceMetrics.forEach((name, metrics) ->
                snapshot.put(name, metrics.currentPower));
        return snapshot;
    }

    /**
     * Simulate power optimization by turning off high-power devices
     */
    public CompletableFuture<String> optimizePowerUsage(int targetWatts) {
        return CompletableFuture.supplyAsync(() -> {
            System.out.println("🔋 Starting power optimization to reach " + targetWatts + "W...");

            List<SmartDevice> devices = facade.getDevices();

            // Sort devices by power consumption
            List<Map.Entry<SmartDevice, Double>> devicePowerList = new ArrayList<>();
            for (SmartDevice device : devices) {
                if (device.isOn()) {
                    devicePowerList.add(new AbstractMap.SimpleEntry<>(device, calculateDevicePower(device)));
                }
            }

            devicePowerList.sort((a, b) -> Double.compare(b.getValue(), a.getValue()));

            int devicesOff = 0;
            double currentPower = deviceMetrics.values().stream()
                    .mapToDouble(m -> m.currentPower)
                    .sum();

            for (Map.Entry<SmartDevice, Double> entry : devicePowerList) {
                if (currentPower <= targetWatts) break;

                SmartDevice device = entry.getKey();
                double devicePower = entry.getValue();

                try {
                    device.turnOff();
                    facade.smartHomeAccess("turnoff", device.getName(), "");
                    currentPower -= devicePower;
                    devicesOff++;

                    System.out.println("🔌 Turned off " + device.getName() +
                            " (saved " + String.format("%.1fW", devicePower) + ")");

                    // Emit device turned off event
                    if (eventSystem != null) {
                        Map<String, Object> eventData = new HashMap<>();
                        eventData.put("device", device.getName());
                        eventData.put("reason", "Power optimization");
                        eventData.put("powerSaved", devicePower);

                        eventSystem.publishEvent(new Event(
                                EventType.DEVICE_STATE_CHANGED,
                                "PowerOptimizer",
                                eventData
                        ));
                    }

                    Thread.sleep(500); // Simulate gradual shutdown
                } catch (Exception e) {
                    System.err.println("Failed to turn off " + device.getName() + ": " + e.getMessage());
                }
            }

            return String.format("Optimization complete: Turned off %d devices, " +
                    "current usage: %.1fW", devicesOff, currentPower);
        });
    }

    /**
     * Schedule power-saving mode
     */
    public void schedulePowerSavingMode(int startHour, int endHour, int maxWatts) {
        scheduler.scheduleAtFixedRate(() -> {
            Calendar cal = Calendar.getInstance();
            int currentHour = cal.get(Calendar.HOUR_OF_DAY);

            if (currentHour >= startHour && currentHour < endHour) {
                double currentPower = deviceMetrics.values().stream()
                        .mapToDouble(m -> m.currentPower)
                        .sum();

                if (currentPower > maxWatts) {
                    System.out.println("🌙 Power saving mode active (Hour: " + currentHour + ")");
                    optimizePowerUsage(maxWatts);
                }
            }
        }, 0, 1, TimeUnit.HOURS);
    }

    /**
     * Stop power monitoring
     */
    public void stopMonitoring() {
        monitoring = false;
        scheduler.shutdown();

        try {
            if (!scheduler.awaitTermination(5, TimeUnit.SECONDS)) {
                scheduler.shutdownNow();
            }
        } catch (InterruptedException e) {
            scheduler.shutdownNow();
            Thread.currentThread().interrupt();
        }

        System.out.println("🛑 Power monitoring service stopped");
    }
}