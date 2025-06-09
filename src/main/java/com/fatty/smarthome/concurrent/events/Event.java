package com.fatty.smarthome.concurrent.events;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.*;

/**
 *
 * Immutable class representin
 */
public class Event {
    private final EventType type;
    private final String source;
    private final Map<String, Object> data;
    private final LocalDateTime timestamp;
    private final String eventId;

    public Event(EventType type, String source, Map<String, Object> data) {
        this.type = Objects.requireNonNull(type, "Event type cannot be null");
        this.source = Objects.requireNonNull(source, "Event source cannot be null");
        this.data = data != null ? new HashMap<>(data) : new HashMap<>();
        this.timestamp = LocalDateTime.now();
        this.eventId = UUID.randomUUID().toString();
    }
    /**
     * Create event with single key-value pair
     */
    public Event(EventType type, String source, String key, Object value) {
        this(type, source, Map.of(key, value));
    }

    // Getters
    public EventType getType() {
        return type;
    }

    public String getSource() {
        return source;
    }

    public Map<String, Object> getData() {
        return Collections.unmodifiableMap(data);
    }

    public LocalDateTime getTimestamp() {
        return timestamp;
    }

    public String getEventId() {
        return eventId;
    }

    @SuppressWarnings("unchecked")
    public <T> T getDataValue(String key, Class<T> type) {
        Object value = data.get(key);

        if (value != null && type.isInstance(value)) {
            return (T) value;
        }
        return null;
    }
    @Override
    public String toString() {
        return String.format("[%s] %s from %s: %s",
                timestamp.format(DateTimeFormatter.ofPattern("HH:mm:ss.SSS")),
                type, source, data);
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        Event event = (Event) o;
        return eventId.equals(event.eventId);
    }
    @Override
    public int hashCode() {
        return Objects.hash(eventId);
    }

}
