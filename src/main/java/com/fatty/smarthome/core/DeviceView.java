package com.fatty.smarthome.core;

import com.fatty.smarthome.devices.SmartDevice;
import javafx.beans.property.SimpleStringProperty;
import javafx.beans.property.StringProperty;

/**
 *
 * VALUE-ADDED: Class representing a device view in the GUI.
 * Uses JavaFX properties for automatic UI updates
 */
public class DeviceView {
    private final StringProperty name;
    private final StringProperty type;
    private final StringProperty status;
    private final StringProperty on;
    private final SmartDevice device;

    public DeviceView(SmartDevice device) {
        this.device = device;
        this.name = new SimpleStringProperty(device.getName());
        this.type = new SimpleStringProperty(device.getClass().getSimpleName());
        this.status = new SimpleStringProperty(device.getStatus());
        this.on = new SimpleStringProperty(device.isOn() ? "ON" : "OFF");
    }

    // Property getters for JavaFX binding
    public StringProperty nameProperty() { return name; }
    public StringProperty typeProperty() { return type; }
    public StringProperty statusProperty() { return status; }
    public StringProperty onProperty() { return on; }

    // <span style="color:red">Regular getters for table columns</span>
    public String getName() { return name.get(); }
    public String getType() { return type.get(); }
    public String getStatus() { return status.get(); }
    public Boolean isOn() { return device.isOn(); }

    // Setters update properties</span>
    public void setName(String value) { name.set(value); }
    public void setType(String value) { type.set(value); }
    public void setStatus(String value) { status.set(value); }
    public void setOn(boolean value) { on.set(String.valueOf(value)); }

    public SmartDevice getDevice() { return device; }
}
