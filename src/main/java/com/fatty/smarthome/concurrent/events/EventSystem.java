package com.fatty.smarthome.concurrent.events;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.*;
import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicLong;
import com.fatty.smarthome.concurrent.events.Event;
import com.fatty.smarthome.concurrent.events.EventType;
import com.fatty.smarthome.concurrent.events.EventListener;


public class EventSystem {
    private final BlockingQueue<Event> eventQueue;
    private final ExecutorService eventProcessors;
    private final Map<EventType, List<EventListener>> listeners;
    private volatile boolean running = false;
    private final List<Thread> processorThreads;

    // Statistics
    private final AtomicLong totalEvents = new AtomicLong(0);
    private final Map<EventType, AtomicInteger> eventCounts = new ConcurrentHashMap<>();
    private final AtomicInteger activeListeners = new AtomicInteger(0);




    /**
     * Event class containing event data
     */

    /**
     * Example listener that logs events
     */
    public static class LoggingListener implements EventListener {
        private final String name;

        public LoggingListener(String name) {
            this.name = name;
        }

        @Override
        public void onEvent(Event event) {
            System.out.println("📝 [" + name + "] Event: " + event);
        }

        @Override
        public String getName() { return name; }
    }

    /**
     * Alert listener for critical events
     */
    public static class AlertListener implements EventListener {
        @Override
        public void onEvent(Event event) {
            if (event.getType() == EventType.TEMPERATURE_ALERT ||
                    event.getType() == EventType.DEVICE_MALFUNCTION ||
                    event.getType() == EventType.POWER_THRESHOLD_EXCEEDED) {

                System.out.println("🚨 ALERT: " + event);
                // In a real system, this could send notifications
            }
        }


    }

    public EventSystem(int processorThreads) {
        this.eventQueue = new LinkedBlockingQueue<>(1000); // Max 1000 pending events
        this.eventProcessors = Executors.newFixedThreadPool(processorThreads);
        this.listeners = new ConcurrentHashMap<>();
        this.processorThreads = new ArrayList<>();

        // Initialize event type listeners
        for (EventType type : EventType.values()) {
            listeners.put(type, new CopyOnWriteArrayList<>());
        }
    }

    /**
     * Start the event system
     */
    public void start() {
        if (running) return;

        running = true;

        // Start processor threads
        for (int i = 0; i < 3; i++) {
            Thread processor = new Thread(this::processEvents, "EventProcessor-" + i);
            processor.setDaemon(true);
            processorThreads.add(processor);
            processor.start();
        }

        System.out.println("📡 Event system started with " + processorThreads.size() + " processors");
    }

    /**
     * Process events from the queue
     */
    private void processEvents() {
        System.out.println("🔄 Event processor started: " + Thread.currentThread().getName());

        while (running) {
            try {
                // Wait for event with timeout
                Event event = eventQueue.poll(1, TimeUnit.SECONDS);

                if (event != null) {
                    // Process event asynchronously
                    CompletableFuture.runAsync(() -> notifyListeners(event), eventProcessors)
                            .exceptionally(throwable -> {
                                System.err.println("❌ Error processing event: " + throwable.getMessage());
                                return null;
                            });
                }
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                break;
            }
        }

        System.out.println("🔄 Event processor stopped: " + Thread.currentThread().getName());
    }

    /**
     * Publish an event to the system
     */
    public void publishEvent(Event event) {
        try {
            boolean added = eventQueue.offer(event, 100, TimeUnit.MILLISECONDS);
            if (!added) {
                System.err.println("⚠️  Event queue full, dropping event: " + event);
            }
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
    }



    /**
     * Subscribe to events of a specific type
     */
    public void subscribe(EventType type, EventListener listener) {
        listeners.get(type).add(listener);
        System.out.println("👂 " + listener.getName() + " subscribed to " + type + " events");
    }

    /**
     * Subscribe to all event types
     */
    public void subscribeToAll(EventListener listener) {
        for (EventType type : EventType.values()) {
            subscribe(type, listener);
        }
    }

    /**
     * Notify all listeners of an event
     */
    private void notifyListeners(Event event) {
        List<EventListener> typeListeners = listeners.get(event.getType());

        if (typeListeners.isEmpty()) {
            return;
        }

        // Notify listeners in parallel
        List<CompletableFuture<Void>> futures = typeListeners.stream()
                .map(listener -> CompletableFuture.runAsync(() -> {
                    try {
                        listener.onEvent(event);
                    } catch (Exception e) {
                        System.err.println("❌ Listener error (" + listener.getName() + "): " + e.getMessage());
                    }
                }, eventProcessors))
                .toList();

        // Wait for all listeners to process (with timeout)
        try {
            CompletableFuture.allOf(futures.toArray(new CompletableFuture[0]))
                    .get(500, TimeUnit.MILLISECONDS);
        } catch (TimeoutException e) {
            System.err.println("⏱️  Some listeners took too long to process event");
        } catch (Exception e) {
            System.err.println("❌ Error notifying listeners: " + e.getMessage());
        }
    }

    /**
     * Stop the event system
     */
    public void stop() {
        running = false;

        // Interrupt processor threads
        processorThreads.forEach(Thread::interrupt);

        // Shutdown executor
        eventProcessors.shutdown();
        try {
            if (!eventProcessors.awaitTermination(5, TimeUnit.SECONDS)) {
                eventProcessors.shutdownNow();
            }
        } catch (InterruptedException e) {
            eventProcessors.shutdownNow();
            Thread.currentThread().interrupt();
        }

        System.out.println("🛑 Event system stopped");
    }

    /**
     * Get event system statistics
     */
    public String getStatistics() {
        int totalListeners = listeners.values().stream()
                .mapToInt(List::size)
                .sum();

        return String.format(
                "Event System Statistics:\n" +
                        "  Queue size: %d / 1000\n" +
                        "  Processor threads: %d\n" +
                        "  Total listeners: %d\n" +
                        "  Running: %s",
                eventQueue.size(),
                processorThreads.size(),
                totalListeners,
                running
        );
    }
    /**
     * Clear all statistics
     */
    public void clearStatistics() {
        totalEvents.set(0);
        eventCounts.values().forEach(count -> count.set(0));
    }

    /**
     * Get queue size
     */
    public int getQueueSize() {
        return eventQueue.size();
    }

    /**
     * Check if system is running
     */
    public boolean isRunning() {
        return running;
    }
}
