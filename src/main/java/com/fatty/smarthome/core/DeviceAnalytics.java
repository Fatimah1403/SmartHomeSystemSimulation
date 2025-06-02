package com.fatty.smarthome.core;

import com.fatty.smarthome.devices.SmartDevice;
import com.fatty.smarthome.devices.Thermostat;

import java.util.*;
import java.util.function.Function;
import java.util.function.Predicate;
import java.util.stream.Collectors;

/**
 *
 * Filters devices by their type.
 * Example: Get all lights: DeviceAnalytics.filterByType(devices, Light.class)
 *
 */
public class DeviceAnalytics {
    /**
     *
     * @param devices list of all devices
     * @param deviceType class type to filter by (e.g. Light.class)
     * @return Filtered list of devices
     *
     */
    public static <T extends SmartDevice> List<T> filterByType(List<SmartDevice> devices, Class<T> deviceType) {
        return devices.stream()
                .filter(deviceType::isInstance) // Filter devices of the specified type
                .map(deviceType::cast) // Cast to the specified type
                .collect(Collectors.toList()); // Collect results into a list
    }

    /**
     * Gets all devices that are currently ON
     * Uses lamda expression as predicate.
     *
     * @param devices list of all devices
     * @return List of devices that are ON
     */
    public static List<SmartDevice> getActiveDevices(List<SmartDevice> devices) {
        // Lambda expression: device -> device.isOn
        return devices.stream()
                .filter(device -> device.isOn)
                .collect(Collectors.toList());
    }

    /**
     * Groups devices by their power state (ON/OFF).
     * Returns a map with true -> devices that are ON, false -> devices that are OFF.
     *
     * @param devices List of all devices
     * @return Map of boolean to list of devices
     */
    public static Map<Boolean, List<SmartDevice>> groupByPowerState(List<SmartDevice> devices) {
        // Collectors.partitioningBy creates a map with exactly two keys: true and false
        return devices.stream()
                .collect(Collectors.partitioningBy(SmartDevice::isOn));
    }


    /**
     * Groups devices by their power state (e.g. ON and OFF)
     * Returns a map with true -> devices that are ON, false -> devices that are OFF.
     *
     * @param devices List of all devices
     * @return Map of device name to count
     */
    public static Map<String, Long> countByType(List<SmartDevice> devices) {
        return devices.stream()
                .collect(Collectors.groupingBy(
                        device -> device.getClass().getSimpleName(), // Key: class name
                        Collectors.counting()
                ));

    }
    /**
     * Finds devices whose names contain a search term (case-insensitive).
     *
     * @param devices List of all devices
     * @param searchTerm Term to search for
     * @return List of matching devices
     */
    public static List<SmartDevice> searchByName(List<SmartDevice> devices, String searchTerm) {
        String lowerSearch = searchTerm.toLowerCase();

        return devices.stream()
                .filter(device -> device.getName().toLowerCase().contains(lowerSearch))
                .sorted(Comparator.comparing(SmartDevice::getName)) // Sort results alphabetically
                .collect(Collectors.toList());
    }
    /**
     * Calculates statistics for thermostat temperatures.
     *
     * @param devices List of all devices
     * @return Statistics object with min, max, average, etc.
     */
    public static IntSummaryStatistics getThermostatStatistics(List<SmartDevice> devices) {
        return devices.stream()
                .filter(device -> device instanceof Thermostat) // Only thermostats
                .map(device -> (Thermostat) device)            // Cast to Thermostat
                .mapToInt(Thermostat::getTemperature)          // Get temperature as int
                .summaryStatistics();                           // Calculate statistics
    }
    /**
     * Creates a summary report of all devices.
     * Uses stream reduction to build a string.
     *
     * @param devices List of all devices
     * @return Formatted report string
     */
    public static String generateReport(List<SmartDevice> devices) {
        if (devices.isEmpty()) {
            return "No devices in the system";
        }

        // Count devices by type
        Map<String, Long> typeCounts = countByType(devices);

        // Count active devices
        long activeCount = devices.stream().filter(SmartDevice::isOn).count();

        // Build report using StringBuilder (more efficient than string concatenation)
        StringBuilder report = new StringBuilder("=== Device Report ===\n");
        report.append("Total devices: ").append(devices.size()).append("\n");
        report.append("Active devices: ").append(activeCount).append("\n");
        report.append("\nDevices by type:\n");

        typeCounts.forEach((type, count) ->
                report.append("  ").append(type).append(": ").append(count).append("\n"));

        report.append("\nDevice details:\n");
        devices.stream()
                .sorted(Comparator.comparing(SmartDevice::getName))
                .forEach(device -> report.append("  - ").append(device.getStatus()).append("\n"));

        return report.toString();
    }
    /**
     * Applies an operation to all devices matching a condition.
     * This demonstrates higher-order functions (functions that take functions).
     *
     * @param devices List of all devices
     * @param condition Predicate to filter devices
     * @param operation Function to apply to each matching device
     * @return Number of devices affected
     */
    public static int applyToMatching(List<SmartDevice> devices,
                                      Predicate<SmartDevice> condition,
                                      Function<SmartDevice, Void> operation) {
        return (int) devices.stream()
                .filter(condition)
                .peek(device -> operation.apply(device)) // peek allows side effects
                .count();
    }
    /**
     * Gets unique device types in the system.
     *
     * @param devices List of all devices
     * @return Set of unique device type names
     */
    public static Set<String> getUniqueDeviceTypes(List<SmartDevice> devices) {
        return devices.stream()
                .map(device -> device.getClass().getSimpleName())
                .collect(Collectors.toSet()); // Set automatically removes duplicates
    }
    /**
     * Finds the device that has been on the longest.
     * For this example, we'll use the device name alphabetically (in real app, would use timestamps).
     *
     * @param devices List of all devices
     * @return Optional containing the device, or empty if no devices are on
     */
    public static Optional<SmartDevice> findLongestRunningDevice(List<SmartDevice> devices) {
        return devices.stream()
                .filter(SmartDevice::isOn)
                .min(Comparator.comparing(SmartDevice::getName)); // In real app, compare by start time
    }
    /**
     * Performs a parallel operation on all devices.
     * Useful for operations that can be done independently.
     *
     * @param devices List of all devices
     * @param operation Operation to perform
     */
    /**
     * Performs a parallel operation on all devices.
     * Useful for operations that can be done independently.
     *
     * @param devices List of all devices
     * @param operation Operation to perform
     */
    public static void parallelOperation(List<SmartDevice> devices,
                                         Function<SmartDevice, Void> operation) {
        devices.parallelStream() // Use parallel stream for concurrent processing
                .forEach(device -> operation.apply(device));
    }

}
