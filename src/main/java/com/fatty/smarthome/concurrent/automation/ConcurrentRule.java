package com.fatty.smarthome.concurrent.automation;

import com.fatty.smarthome.devices.SmartDevice;
import com.fatty.smarthome.util.SmartHomeException;

import java.util.List;

public interface ConcurrentRule {
    String getName();
    boolean shouldExecute();
    void execute(List<SmartDevice> devices) throws SmartHomeException;

    int getPriority();

    boolean isEnabled();
}
