package com.fatty.smarthome.util;

public class SmartHomeException extends Exception {
    public SmartHomeException(String message) {
        super(message);
    }
    public SmartHomeException(String message, Throwable cause) {
        super(message, cause);
    }
}
