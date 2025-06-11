package com.fatty.smarthome.cli;

import com.fatty.smarthome.concurrent.monitoring.DeviceMonitor;
import com.fatty.smarthome.concurrent.monitoring.PowerMonitoringService;
import com.fatty.smarthome.concurrent.automation.AutomationEngine;
import com.fatty.smarthome.concurrent.devices.ConcurrentDeviceFactory;
import com.fatty.smarthome.concurrent.devices.ConcurrentSecurityCamera;
import com.fatty.smarthome.concurrent.devices.ConcurrentSmartDevice;
import com.fatty.smarthome.concurrent.devices.ConcurrentThermostat;
import com.fatty.smarthome.concurrent.events.EventSystem;
import com.fatty.smarthome.concurrent.events.EventType;
import com.fatty.smarthome.devices.*;
import com.fatty.smarthome.core.FacadeSmartHome;
import com.fatty.smarthome.util.SmartHomeException;
import com.fatty.smarthome.concurrent.automation.TemperatureRule;
import com.fatty.smarthome.concurrent.automation.SecurityRule;

import java.util.*;
import java.util.concurrent.*;

/**
 * Additional CLI commands for concurrent features
 */
public class ConcurrentCLICommands {
    private final FacadeSmartHome facade;
    private final EventSystem eventSystem;
    private final AutomationEngine automationEngine;
    private final DeviceMonitor deviceMonitor;
    private final PowerMonitoringService powerMonitor;
    private final ConcurrentDeviceFactory deviceFactory;

    // Thread pool for async operations
    private final ExecutorService asyncExecutor;

    public ConcurrentCLICommands(FacadeSmartHome facade) throws SmartHomeException {
        this.facade = facade;
        this.eventSystem = new EventSystem(3);
        this.automationEngine = new AutomationEngine(facade);
        this.deviceMonitor = new DeviceMonitor(facade, 5000);
        this.powerMonitor = new PowerMonitoringService(facade, eventSystem);
        this.deviceFactory = new ConcurrentDeviceFactory(eventSystem);
        this.asyncExecutor = Executors.newCachedThreadPool();

        // Initialize event listeners
        setupEventListeners();
    }

    /**
     * Setup default event listeners
     */
    private void setupEventListeners() {
        // Log all events
        eventSystem.subscribeToAll(new EventSystem.LoggingListener("CLI-Logger"));

        // Alert on critical events
        eventSystem.subscribe(EventType.DEVICE_MALFUNCTION, event ->
                System.out.println("🚨 CRITICAL: Device malfunction - " +
                        event.getData().getOrDefault("error", "Unknown error")));
        eventSystem.subscribe(EventType.MOTION_DETECTED, event ->
                System.out.println("👁️  Motion detected by " + event.getSource()));
    }

    /**
     * Process concurrent commands
     */
    public boolean processConcurrentCommand(String command) {
        String[] parts = command.toLowerCase().split("\\s+");
        String cmd = parts[0];

        try {
            return switch (cmd) {
                case "monitor" -> handleMonitorCommand(parts);
                case "automate" -> handleAutomateCommand(parts);
                case "power" -> handlePowerCommand(parts);
                case "events" -> handleEventsCommand(parts);
                case "concurrent" -> handleConcurrentCommand(parts);
                case "simulate" -> handleSimulateCommand(parts);
                case "services" -> handleServicesCommand(parts);
                default -> false; // Not a concurrent command
            };
        } catch (Exception e) {
            System.err.println("❌ Error: " + e.getMessage());
            return true;
        }
    }

    /**
     * Handle monitor commands
     */
    private boolean handleMonitorCommand(String[] parts) {
        if (parts.length < 2) {
            System.out.println("Usage: monitor <start|stop|stats>");
            return true;
        }

        switch (parts[1]) {
            case "start":
                if (!deviceMonitor.isRunning()) {
                    deviceMonitor.startMonitoring();
                } else {
                    System.out.println("ℹ️  Device monitoring is already running");
                }
                break;

            case "stop":
                deviceMonitor.stopMonitoring();
                break;

            case "stats":
                if (parts.length > 2) {
                    String deviceName = String.join(" ", Arrays.copyOfRange(parts, 2, parts.length));
                    System.out.println(deviceMonitor.getDeviceStats(deviceName));
                } else {
                    System.out.println("Usage: monitor stats <device-name>");
                }
                break;

            default:
                System.out.println("Unknown monitor command: " + parts[1]);
        }
        return true;
    }

    /**
     * Handle automation commands
     */
    private boolean handleAutomateCommand(String[] parts) {
        if (parts.length < 2) {
            System.out.println("Usage: automate <start|stop|add|status>");
            return true;
        }

        switch (parts[1]) {
            case "start":
                automationEngine.start();
                setupDefaultRules();
                break;

            case "stop":
                automationEngine.stop();
                break;

            case "add":
                if (parts.length > 2) {
                    addAutomationRule(Arrays.copyOfRange(parts, 2, parts.length));
                } else {
                    System.out.println("Usage: automate add <rule-type> [parameters]");
                }
                break;

            case "status":
                System.out.println(automationEngine.getStatus());
                break;

            default:
                System.out.println("Unknown automate command: " + parts[1]);
        }
        return true;
    }

    /**
     * Handle power commands
     */
    private boolean handlePowerCommand(String[] parts) {
        if (parts.length < 2) {
            System.out.println("Usage: power <monitor|threshold|optimize|stats>");
            return true;
        }

        switch (parts[1]) {
            case "monitor":
                if (parts.length > 2 && parts[2].equals("start")) {
                    powerMonitor.startMonitoring();
                } else if (parts.length > 2 && parts[2].equals("stop")) {
                    powerMonitor.stopMonitoring();
                } else {
                    System.out.println("Usage: power monitor <start|stop>");
                }
                break;

            case "threshold":
                if (parts.length > 2) {
                    try {
                        int watts = Integer.parseInt(parts[2]);
                        powerMonitor.setPowerThreshold(watts);
                    } catch (NumberFormatException e) {
                        System.out.println("Invalid threshold value");
                    }
                } else {
                    System.out.println("Usage: power threshold <watts>");
                }
                break;

            case "optimize":
                if (parts.length > 2) {
                    try {
                        int targetWatts = Integer.parseInt(parts[2]);
                        System.out.println("🔋 Starting power optimization...");

                        powerMonitor.optimizePowerUsage(targetWatts)
                                .thenAccept(System.out::println)
                                .exceptionally(e -> {
                                    System.err.println("❌ Optimization failed: " + e.getMessage());
                                    return null;
                                });
                    } catch (NumberFormatException e) {
                        System.out.println("Invalid target watts");
                    }
                } else {
                    System.out.println("Usage: power optimize <target-watts>");
                }
                break;

            case "stats":
                System.out.println(powerMonitor.getPowerStatistics());
                break;

            default:
                System.out.println("Unknown power command: " + parts[1]);
        }
        return true;
    }

    /**
     * Handle events commands
     */
    private boolean handleEventsCommand(String[] parts) {
        if (parts.length < 2) {
            System.out.println("Usage: events <start|stop|stats>");
            return true;
        }

        switch (parts[1]) {
            case "start":
                eventSystem.start();
                System.out.println("✅ Event system started");
                break;

            case "stop":
                eventSystem.stop();
                break;

            case "stats":
                System.out.println(eventSystem.getStatistics());
                break;

            default:
                System.out.println("Unknown events command: " + parts[1]);
        }
        return true;
    }

    /**
     * Handle concurrent operations
     */
    private boolean handleConcurrentCommand(String[] parts) {
        if (parts.length < 2) {
            System.out.println("Usage: concurrent <test|control>");
            return true;
        }

        switch (parts[1]) {
            case "test":
                runConcurrentTest();
                break;

            case "control":
                if (parts.length > 2) {
                    String action = parts[2];
                    runConcurrentControl(action);
                } else {
                    System.out.println("Usage: concurrent control <on|off|random>");
                }
                break;

            default:
                System.out.println("Unknown concurrent command: " + parts[1]);
        }
        return true;
    }

    /**
     * Handle simulation commands
     */
    private boolean handleSimulateCommand(String[] parts) {
        if (parts.length < 2) {
            System.out.println("Usage: simulate <motion|malfunction|temperature>");
            return true;
        }

        switch (parts[1]) {
            case "motion":
                simulateMotion(parts);
                break;

            case "malfunction":
                simulateMalfunction(parts);
                break;

            case "temperature":
                simulateTemperature(parts);
                break;

            default:
                System.out.println("Unknown simulate command: " + parts[1]);
        }
        return true;
    }

    /**
     * Handle services command
     */
    private boolean handleServicesCommand(String[] parts) {
        System.out.println("\n=== CONCURRENT SERVICES STATUS ===");
        System.out.println("Device Monitor: " + (deviceMonitor.isRunning() ? "✅ Running" : "❌ Stopped"));
        System.out.println("Event System: " + eventSystem.getStatistics().split("\n")[3]);
        System.out.println("Automation Engine: " + automationEngine.getStatus().split("\n")[2]);
        System.out.println("Power Monitor: Active");
        System.out.println("Active threads: " + Thread.activeCount());
        return true;
    }

    // Helper methods...

    private void setupDefaultRules() {
        // Temperature comfort rule
        automationEngine.addRule((AutomationEngine.ConcurrentRule) new TemperatureRule("Comfort Maintenance", 22, 2));

        // Security rule
        automationEngine.addRule((AutomationEngine.ConcurrentRule) new SecurityRule("Security Response"));
    }

    private void addAutomationRule(String[] params) {
        // Implementation for adding custom rules
        System.out.println("✅ Rule added (feature demonstration)");
    }

    private void runConcurrentTest() {
        System.out.println("🧪 Running concurrent test...");

        List<CompletableFuture<Void>> futures = new ArrayList<>();
        Random random = new Random();

        // Create 10 concurrent operations
        for (int i = 0; i < 10; i++) {
            final int index = i;
            futures.add(CompletableFuture.runAsync(() -> {
                List<SmartDevice> devices = facade.getDevices();
                if (!devices.isEmpty()) {
                    SmartDevice device = devices.get(random.nextInt(devices.size()));
                    if (random.nextBoolean()) {
                        device.turnOn();
                    } else {
                        device.turnOff();
                    }
                    System.out.println("Thread-" + index + " controlled " + device.getName());
                }
            }, asyncExecutor));
        }

        CompletableFuture.allOf(futures.toArray(new CompletableFuture[0]))
                .thenRun(() -> System.out.println("✅ Concurrent test completed"));
    }

    private void runConcurrentControl(String action) {
        System.out.println("🔧 Running concurrent control: " + action);

        List<SmartDevice> devices = facade.getDevices();
        devices.parallelStream().forEach(device -> {
            switch (action) {
                case "on":
                    device.turnOn();
                    break;
                case "off":
                    device.turnOff();
                    break;
                case "random":
                    if (Math.random() > 0.5) {
                        device.turnOn();
                    } else {
                        device.turnOff();
                    }
                    break;
            }
        });

        System.out.println("✅ Concurrent control completed");
    }

    private void simulateMotion(String[] parts) {
        if (parts.length > 2) {
            String cameraName = String.join(" ", Arrays.copyOfRange(parts, 2, parts.length));
            facade.getDevice(cameraName).ifPresent(device -> {
                if (device instanceof ConcurrentSecurityCamera) {
                    ((ConcurrentSecurityCamera) device).detectMotion();
                } else {
                    System.out.println("❌ Device is not a camera: " + cameraName);
                }
            });
        } else {
            // Simulate on all cameras
            facade.getDevices().stream()
                    .filter(d -> d instanceof ConcurrentSecurityCamera)
                    .forEach(camera -> ((ConcurrentSecurityCamera) camera).detectMotion());
        }
    }

    private void simulateMalfunction(String[] parts) {
        if (parts.length > 2) {
            String deviceName = String.join(" ", Arrays.copyOfRange(parts, 2, parts.length));
            facade.getDevice(deviceName).ifPresent(device -> {
                if (device instanceof ConcurrentSmartDevice) {
                    ((ConcurrentSmartDevice) device).simulateMalfunction();
                }
            });
        } else {
            System.out.println("Usage: simulate malfunction <device-name>");
        }
    }

    private void simulateTemperature(String[] parts) {
        facade.getDevices().stream()
                .filter(d -> d instanceof ConcurrentThermostat)
                .forEach(thermostat -> ((ConcurrentThermostat) thermostat).simulateTemperatureDrift());
        System.out.println("✅ Temperature drift simulated");
    }

    /**
     * Shutdown all concurrent services
     */
    public void shutdown() {
        deviceMonitor.stopMonitoring();
        automationEngine.stop();
        powerMonitor.stopMonitoring();
        eventSystem.stop();
        asyncExecutor.shutdown();

        try {
            if (!asyncExecutor.awaitTermination(5, TimeUnit.SECONDS)) {
                asyncExecutor.shutdownNow();
            }
        } catch (InterruptedException e) {
            asyncExecutor.shutdownNow();
            Thread.currentThread().interrupt();
        }
    }

    /**
     * Get help text for concurrent commands
     */
    public static String getConcurrentHelp() {
        return """
            
            CONCURRENT FEATURES:
              monitor start/stop           Start/stop device monitoring
              monitor stats <device>       Show device statistics
              
              automate start/stop          Start/stop automation engine
              automate add <rule>          Add automation rule
              automate status              Show automation status
              
              power monitor start/stop     Start/stop power monitoring
              power threshold <watts>      Set power alert threshold
              power optimize <watts>       Optimize to target power
              power stats                  Show power statistics
              
              events start/stop            Start/stop event system
              events stats                 Show event statistics
              
              concurrent test              Run concurrent test
              concurrent control <action>  Control all devices concurrently
              
              simulate motion [camera]     Simulate motion detection
              simulate malfunction <dev>   Simulate device malfunction
              simulate temperature         Simulate temperature changes
              
              services                     Show all services status
            """;
    }
}