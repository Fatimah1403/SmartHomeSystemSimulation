//package com.fatty.smarthome.concurrent;
//
//import com.fatty.smarthome.devices.*;
//import com.fatty.smarthome.core.FacadeSmartHome;
//import java.util.*;
//import java.util.concurrent.*;
//import java.util.concurrent.atomic.AtomicInteger;
//import java.util.concurrent.atomic.DoubleAdder;
//
///**
// * PowerMonitoringService tracks power consumption across all devices
// * and alerts when thresholds are exceeded.
// */
//public class PowerMonitoringService {
//    private final FacadeSmartHome facade;
//    private final EventSystem eventSystem;
//    private final ScheduledExecutorService scheduler;
//    private final Map<String, PowerMetrics> deviceMetrics;
//    private final DoubleAdder totalPowerConsumed;
//    private final AtomicInteger powerThreshold;
//    private volatile boolean monitoring = false;
//
//    /**
//     * Power metrics for a device
//     */
//    private static class PowerMetrics {
//        final String deviceName;
//        final DoubleAdder totalConsumption = new DoubleAdder();
//        volatile double currentPower = 0;
//        volatile long lastUpdateTime = System.currentTimeMillis();
//
//        PowerMetrics(String deviceName) {
//            this.deviceName = deviceName;
//        }
//
//        void updatePower(double watts) {
//            long now = System.currentTimeMillis();
//            long duration = now - lastUpdateTime;
//
//            // Calculate energy consumed (watt-hours)
//            double hoursElapsed = duration / 3600000.0;
//            double energyConsumed = currentPower * hoursElapsed;
//            totalConsumption.add(energyConsumed);
//
//            currentPower = watts;
//            lastUpdateTime = now;
//        }
//    }
//
//    public PowerMonitoringService(FacadeSmartHome facade, EventSystem eventSystem) {
//        this.facade = facade;
//        this.eventSystem = eventSystem;
//        this.scheduler = Executors.newScheduledThreadPool(2);
//        this.deviceMetrics = new ConcurrentHashMap<>();
//        this.totalPowerConsumed = new DoubleAdder();
//        this.powerThreshold = new AtomicInteger(500); // Default 500W threshold
//    }
//
//    /**
//     * Start monitoring power consumption
//     */
//    public void startMonitoring() {
//        if (monitoring) return;
//
//        monitoring = true;
//
//        // Schedule power calculation every 2 seconds
//        scheduler.scheduleAtFixedRate(
//                this::calculatePowerUsage, 0, 2, TimeUnit.SECONDS);
//
//        // Schedule power report every 30 seconds
//        scheduler.scheduleAtFixedRate(
//                this::generatePowerReport, 30, 30, TimeUnit.SECONDS);
//
//        System.out.println("⚡ Power monitoring service started");
//    }
//
//    /**
//     * Calculate current power usage
//     */
//    private void calculatePowerUsage() {
//        try {
//            List<SmartDevice> devices = facade.getDevices();
//            double totalPower = 0;
//
//            // Calculate power for each device
//            for (SmartDevice device : devices) {
//                double devicePower = calculateDevicePower(device);
//
//                PowerMetrics metrics = deviceMetrics.computeIfAbsent(
//                        device.getName(), PowerMetrics::new);
//                metrics.updatePower(devicePower);
//
//                totalPower += devicePower;
//            }
//
//            // Check if threshold exceeded
//            if (totalPower > powerThreshold.get()) {
//                System.out.println("⚠️  POWER ALERT: Current usage " +
//                        String.format("%.1fW", totalPower) +
//                        " exceeds threshold " + powerThreshold.get() + "W!");
//
//                // Emit power threshold event
//                if (eventSystem != null) {
//                    Map<String, Object> data = new HashMap<>();
//                    data.put("currentPower", totalPower);
//                    data.put("threshold", powerThreshold.get());
//                    data.put("devices", getHighPowerDevices());
//
//                    eventSystem.publishEvent(new EventSystem.Event(
//                            EventSystem.EventType.POWER_THRESHOLD_EXCEEDED,
//                            "PowerMonitor",
//                            data
//                    ));
//                }
//            }
//
//            // Update total consumption
//            totalPowerConsumed.add(totalPower * 2 / 3600.0); // Convert to Wh
//
//        } catch (Exception e) {
//            System.err.println("❌ Error calculating power: " + e.getMessage());
//        }
//    }
//
//    /**
//     * Calculate power consumption for a specific device
//     */
//    private double calculateDevicePower(SmartDevice device) {
//        if (!device.isOn()) return 0;
//
//        // Simulate power consumption based on device type
//        if (device instanceof ConcurrentLight) {
//            ConcurrentLight light = (ConcurrentLight) device;
//            return 10 + (light.getBrightness() / 100.0 * 50); // 10-60W
//        } else if (device instanceof Thermostat) {
//            Thermostat thermostat = (Thermostat) device;
//            int temp = thermostat.getTemperature();
//            // Higher power for extreme temperatures
//            if (temp < 18 || temp > 24) {
//                return 150; // Heating/cooling active
//            }
//            return 50; // Standby power
//        } else if (device instanceof SecurityCamera) {
//            return 15; // Constant 15W when recording
//        }
//
//        return 5; // Default standby power
//    }
//
//    /**
//     * Get list of high power consuming devices
//     */
//    private List<String> getHighPowerDevices() {
//        return deviceMetrics.entrySet().stream()
//                .filter(entry -> entry.getValue().currentPower > 50)
//                .sorted((a, b) -> Double.compare(
//                        b.getValue().currentPower,
//                        a.getValue().currentPower))
//                .limit(3)
//                .map(entry -> String.format("%s (%.1fW)",
//                        entry.getKey(),
//                        entry.getValue().currentPower))
//                .toList();
//    }
//
//    /**
//     * Generate power consumption report
//     */
//    private void generatePowerReport() {
//        System.out.println("\n📊 === POWER CONSUMPTION REPORT ===");
//        System.out.println("Time: " + new Date());
//
//        double currentTotal = deviceMetrics.values().stream()
//                .mapToDouble(m -> m.currentPower)
//                .sum();
//
//        System.out.printf("Current Total Power: %.1f W\n", currentTotal);
//        System.out.printf("Total Energy Consumed: %.2f Wh\n", totalPowerConsumed.sum());
//        System.out.println("\nDevice Breakdown:");
//
//        deviceMetrics.entrySet().stream()
//                .filter(entry -> entry.getValue().currentPower > 0)
//                .sorted((a, b) -> Double.compare(
//                        b.getValue().currentPower,
//                        a.getValue().currentPower))
//                .forEach(entry -> {
//                    PowerMetrics metrics = entry.getValue();
//                    System.out.printf("  %-20s: %6.1f W (Total: %.2f Wh)\n",
//                            entry.getKey(),
//                            metrics.currentPower,
//                            metrics.totalConsumption.sum());
//                });
//
//        System.out.println("================================\n");
//    }
//
//    /**
//     * Set power threshold for alerts
//     */
//    public void setPowerThreshold(int watts) {
//        powerThreshold.set(watts);
//        System.out.println("⚡ Power threshold set to " + watts + "W");
//    }
//
//    /**
//     * Get current power statistics
//     */
//    public String getPowerStatistics() {
//        double currentTotal = deviceMetrics.values().stream()
//                .mapToDouble(m -> m.currentPower)
//                .sum();
//
//        return String.format(
//                "Power Statistics:\n" +
//                        "  Current usage: %.1f W\n" +
//                        "  Threshold: %d W\n" +
//                        "  Total consumed: %.2f Wh\n" +
//                        "  Active devices: %d",
//                currentTotal,
//                powerThreshold.get(),
//                totalPowerConsumed.sum(),
//                (int) deviceMetrics.values().stream()
//                        .filter(m -> m.currentPower > 0)
//                        .count()
//        );
//    }
//
//    /**
//     * Simulate power optimization by turning off high-power devices
//     */
//    public CompletableFuture<String> optimizePowerUsage(int targetWatts) {
//        return CompletableFuture.supplyAsync(() -> {
//            System.out.println("🔋 Starting power optimization to reach " + targetWatts + "W...");
//
//            List<SmartDevice> devices = facade.getDevices();
//            List<SmartDevice> sortedByPower = devices.stream()
//                    .filter(SmartDevice::isOn)
//                    .sorted((a, b) -> Double.compare(
//                            calculateDevicePower(b),
//                            calculateDevicePower(a)))
//                    .toList();
//
//            int devicesOff = 0;
//            double currentPower = deviceMetrics.values().stream()
//                    .mapToDouble(m -> m.currentPower)
//                    .sum();
//
//            for (SmartDevice device : sortedByPower) {
//                if (currentPower <= targetWatts) break;
//
//                double devicePower = calculateDevicePower(device);
//                device.turnOff();
//                currentPower -= devicePower;
//                devicesOff++;
//
//                System.out.println("🔌 Turned off " + device.getName() +
//                        " (saved " + String.format("%.1fW", devicePower) + ")");
//
//                try {
//                    Thread.sleep(500); // Simulate gradual shutdown
//                } catch (InterruptedException e) {
//                    Thread.currentThread().interrupt();
//                }
//            }
//
//            return String.format("Optimization complete: Turned off %d devices, " +
//                    "current usage: %.1fW", devicesOff, currentPower);
//        });
//    }
//
//    /**
//     * Stop power monitoring
//     */
//    public void stopMonitoring() {
//        monitoring = false;
//        scheduler.shutdown();
//
//        try {
//            if (!scheduler.awaitTermination(5, TimeUnit.SECONDS)) {
//                scheduler.shutdownNow();
//            }
//        } catch (InterruptedException e) {
//            scheduler.shutdownNow();
//            Thread.currentThread().interrupt();
//        }
//
//        System.out.println("🛑 Power monitoring service stopped");
//    }
//}