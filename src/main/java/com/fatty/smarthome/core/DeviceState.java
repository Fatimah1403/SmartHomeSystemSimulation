package com.fatty.smarthome.core;

import com.fatty.smarthome.devices.SecurityCamera;
import com.fatty.smarthome.devices.SmartDevice;
import com.fatty.smarthome.devices.Thermostat;

import java.io.Serial;
import java.io.Serializable;
import java.time.LocalDateTime;

/**
 * VALUE-ADDED: Serializable class persisting device states.
 *  This class represents the state of a device that can be saved to binary files.
 *  Implements Serializable  for binary I/O operations.
 */
public class DeviceState implements Serializable {
    @Serial
    private static final long serialVersionUID = 1L;

    private String deviceName;
    private String deviceType;
    private boolean isOn;
    private String status;
    private LocalDateTime lastModified;

    private Integer temperature; // For thermostats
    private boolean isRecording; // For cameras

    public DeviceState(String deviceName, String deviceType, boolean isOn, String status) {
        this.deviceName = deviceName;
        this.deviceType = deviceType;
        this.isOn = isOn;
        this.status = status;
        this.lastModified = LocalDateTime.now();
    }
//
    // Getters and setters
    public Integer getTemperature() { return temperature; }
    public void setTemperature(Integer temperature) { this.temperature = temperature; }

    public boolean isRecording() { return isRecording; }
    public void setRecording(boolean recording) { isRecording = recording; }

    public String getDeviceType() { return deviceType; }
    public void setDeviceType(String deviceType) { this.deviceType = deviceType; }

    public String getDeviceName() { return deviceName; }
    public void setDeviceName(String deviceName) { this.deviceName = deviceName; }

    public boolean isOn() { return isOn; }
    public void setOn(boolean On) { this.isOn = On; }

    public String getStatus() { return status; }
    public void setStatus(String status) { this.status = status; }

    public LocalDateTime getLastModified() { return lastModified; }
    public void setLastModified(LocalDateTime lastModified) { this.lastModified = lastModified; }

    @Override
    public String toString() {
        return String.format("DevicesState{name='%s', type='%s', on=%s, status='%s', modified=%s}",
                deviceName, deviceType, isOn, status, lastModified);
    }
}
