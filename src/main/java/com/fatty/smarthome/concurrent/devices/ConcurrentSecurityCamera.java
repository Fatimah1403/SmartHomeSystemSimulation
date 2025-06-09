package com.fatty.smarthome.concurrent.devices;

import com.fatty.smarthome.concurrent.events.EventType;
import com.fatty.smarthome.devices.SecurityCamera;

import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;

public class ConcurrentSecurityCamera extends ConcurrentSmartDevice {
    private volatile boolean recording = false;
    private final AtomicBoolean motionDetected = new AtomicBoolean(false);
    private final AtomicInteger recordingCount = new AtomicInteger(0);
    private volatile long lastMotionTime = 0;

    // Motion detection settings
    private static final long MOTION_TIMEOUT_MS = 5000; // 5 seconds
    private final ScheduledExecutorService motionScheduler = Executors.newSingleThreadScheduledExecutor(r -> {
        Thread t = new Thread(r);
        t.setName("MotionTimeout-" + getName());
        t.setDaemon(true);
        return t;
    });
    /**
     * Create a new concurrent security camera
     * @param name The name of the camera
     */
    public ConcurrentSecurityCamera(String name) {
        super(name);
    }


    public boolean isRecording() {
        lock.readLock().lock();
        try {
            return recording;
        } finally {
            lock.readLock().unlock();
        }
    }


    public void startRecording() {
        lock.writeLock().lock();
        try {
            if (!isOn) {
                System.out.println("⚠️  Cannot start recording - " + name + " is OFF");
                return;
            }

            if (!recording) {
                recording = true;
                recordingCount.incrementAndGet();
                System.out.println("📹 " + name + " started recording #" + recordingCount.get());

                // Emit recording started event
                if (eventSystem != null) {
                    Map<String, Object> data = new HashMap<>();
                    data.put("camera", name);
                    data.put("action", "RECORDING_STARTED");
                    data.put("sessionNumber", recordingCount.get());
                    data.put("timestamp", System.currentTimeMillis());

                    emitEvent(EventType.DEVICE_STATE_CHANGED, data);
                }
            }
        } finally {
            lock.writeLock().unlock();
        }
    }

    public void stopRecording() {
        lock.writeLock().lock();
        try {
            if (recording) {
                recording = false;
                System.out.println("📹 " + name + " stopped recording");

                // Emit recording stopped event
                if (eventSystem != null) {
                    Map<String, Object> data = new HashMap<>();
                    data.put("camera", name);
                    data.put("action", "RECORDING_STOPPED");
                    data.put("timestamp", System.currentTimeMillis());

                    emitEvent(EventType.DEVICE_STATE_CHANGED, data);
                }
            }
        } finally {
            lock.writeLock().unlock();
        }
    }
    /**
     * Simulate motion detection
     */
    public void detectMotion() {
        lock.writeLock().lock();
        try {
            if (!isOn) {
                System.out.println("⚠️  " + name + " is OFF - cannot detect motion");
                return;
            }

            boolean wasMotionDetected = motionDetected.get();

            if (!wasMotionDetected) {
                motionDetected.set(true);
                lastMotionTime = System.currentTimeMillis();

                System.out.println("🚨 " + name + " detected motion!");

                // Start recording automatically
                if (!recording) {
                    startRecording();
                }

                // Emit motion event
                if (eventSystem != null) {
                    Map<String, Object> data = new HashMap<>();
                    data.put("camera", name);
                    data.put("location", name.replace("Cam", "").replace("Camera", ""));
                    data.put("timestamp", System.currentTimeMillis());

                    emitEvent(EventType.MOTION_DETECTED, data);
                }

                // Schedule motion clear after timeout
                motionScheduler.schedule(this::clearMotion, MOTION_TIMEOUT_MS, TimeUnit.MILLISECONDS);
            } else {
                // Motion still active, update last motion time
                lastMotionTime = System.currentTimeMillis();
            }
        } finally {
            lock.writeLock().unlock();
        }
    }
    /**
     * Clear motion detection flag
     */
    private void clearMotion() {
        lock.writeLock().lock();
        try {
            if (System.currentTimeMillis() - lastMotionTime >= MOTION_TIMEOUT_MS) {
                motionDetected.set(false);
                System.out.println("✅ " + name + " motion cleared");
            }
        } finally {
            lock.writeLock().unlock();
        }
    }
    /**
     * Check if motion is currently detected
     * @return true if motion is detected
     */
    public boolean isMotionDetected() {
        return motionDetected.get();
    }

    /**
     * Get the number of recording sessions
     * @return The number of times recording has started
     */
    public int getRecordingCount() {
        return recordingCount.get();
    }

    /**
     * Trigger an alert (for testing)
     */
    public void triggerAlert() {
        if (isOn()) {
            System.out.println("🚨 ALERT from " + name + "!");
            detectMotion();
        }
    }

    @Override
    protected void onTurnOn() {
        startRecording();
    }

    @Override
    protected void onTurnOff() {
        stopRecording();
        motionDetected.set(false);
    }

    @Override
    public String getStatus() {
        lock.readLock().lock();
        try {
            StringBuilder status = new StringBuilder(super.getStatus());

            if (recording) {
                status.append(" 🔴 REC");
            }

            if (motionDetected.get()) {
                status.append(" 🚨 MOTION");
            }

            status.append(" (Sessions: ").append(recordingCount.get()).append(")");

            return status.toString();
        } finally {
            lock.readLock().unlock();
        }
    }

    /**
     * Calculate approximate power consumption
     * @return Estimated watts
     */
    public double getEstimatedPowerConsumption() {
        if (!isOn()) return 0;
        return recording ? 15 : 5; // Higher power when recording
    }

    /**
     * Cleanup resources when camera is no longer needed
     */
    public void shutdown() {
        motionScheduler.shutdown();
        try {
            if (!motionScheduler.awaitTermination(1, TimeUnit.SECONDS)) {
                motionScheduler.shutdownNow();
            }
        } catch (InterruptedException e) {
            motionScheduler.shutdownNow();
            Thread.currentThread().interrupt();
        }
    }

}
