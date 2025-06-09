package com.fatty.smarthome.concurrent.events;

/**
 * Interface for objects that want to receive events
 */
@FunctionalInterface

public interface EventListener {
    /**
     * Called when an event occurs
     * @param event The event that occurred
     */
    void onEvent(Event event);

    /**
     * Get the name of this listener for debugging
     * @return listener name
     */
    default String getName() {
        return this.getClass().getSimpleName();
    }

    /**
     * Filter to determine if this listener should receive the event
     * @param event The event to check
     * @return true if this listener should process the event
     */
    default boolean accepts(Event event) {
        return true;
    }
    /**
     * Priority of this listener (higher = processed first)
     * @return priority value
     */
    default int getPriority() {
        return 0;
    }
}
