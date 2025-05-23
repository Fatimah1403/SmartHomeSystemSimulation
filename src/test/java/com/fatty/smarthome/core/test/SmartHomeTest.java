package com.fatty.smarthome.core.test;


import com.fatty.smarthome.core.SmartHome;

import org.junit.jupiter.api.BeforeEach;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;



public class SmartHomeTest {
    private SmartHome smartHome;


    @BeforeEach
    void setUp() throws IOException {
      smartHome = new SmartHome();
       // Value-added to ensure clean log file for test isolation, supporting Week 3 file I/O
        Path path = Paths.get("device_log.txt");
        Files.deleteIfExists(path);
       Files.createFile(path);
    }

}
