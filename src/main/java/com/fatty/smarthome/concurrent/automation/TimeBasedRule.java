package com.fatty.smarthome.concurrent.automation;

import com.fatty.smarthome.devices.SmartDevice;

import java.time.LocalTime;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.concurrent.atomic.AtomicBoolean;


public class TimeBasedRule implements ConcurrentRule{
    private final String name;
    private final LocalTime executionTime;
    private final Runnable action;
    private volatile LocalTime lastExecuted = null;
    private final AtomicBoolean enabled = new AtomicBoolean(true);

    public TimeBasedRule(String name, LocalTime executionTime, Runnable action) {
        this.name = name;
        this.executionTime = executionTime;
        this.action = action;
    }
    @Override
    public String getName() {
        return name;
    }
    @Override
    public boolean shouldExecute() {
        if (!isEnabled()) return false;

        LocalTime now = LocalTime.now();
        // Execute if we're within 1 minute of execution time and haven't executed today
        boolean inWindow = now.isAfter(executionTime.minusMinutes(1)) &&
                             now.isBefore(executionTime.plusMinutes(1));

        if (inWindow) {
            return true;
        }
        return false;

    }
    @Override
    public void execute(List<SmartDevice> devices) {
        System.out.println("⏰ Executing " + name + " at " +
                LocalTime.now().format(DateTimeFormatter.ofPattern("HH:mm:ss")));

        action.run();
        lastExecuted = LocalTime.now();
    }

    @Override
    public int getPriority() {
        return 5;  // Higher priority for comfort
    }


    public boolean isEnabled() {
        return enabled.get();
    }

    public void setEnabled(boolean enabled) {
        this.enabled.set(enabled);
    }
}
