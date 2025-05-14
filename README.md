To run the project, ensure you have the following installed:

Java JDK 23 (or compatible version)

Maven 3.8+ (for dependency management and build)

IntelliJ IDEA (recommended IDE, though others like VS Code work)
## Project Overview

The **Smart Home System Simulation (SHSS)** is a Java-based application designed to simulate the functioning of a smart home. It models various devices such as **lights**, **thermostats**, and **security cameras**, and provides functionality to control and automate these devices. The project demonstrates how a smart home system can be designed using **object-oriented programming** (OOP) principles.

The system allows users to **interact** with different smart devices, **automate their actions**, and **monitor their status**. The goal of this simulation is to model the behavior of these devices in a real-world smart home environment, where multiple devices interact seamlessly to provide enhanced home automation and security.

### What the Project Entails:
1. **Smart Device Classes**: Devices like `Light`, `Thermostat`, and `SecurityCamera` are modeled as classes that inherit from an abstract class `SmartDevice`. Each device can be turned on and off, and its status can be reported.
2. **Automation Rules**: The system supports the creation of automation rules. These rules automate actions like turning on a light at a specific time or setting the thermostat temperature.
3. **Security Check**: The system includes a security check to ensure all devices are functioning correctly, simulating a real-world smart home security feature.
4. **Upcasting and Downcasting**: The system leverages **polymorphism** via upcasting (treating devices as their superclass `SmartDevice`) and downcasting (casting to specific device types like `Thermostat` for temperature control).
5. **Test Coverage**: The project is tested using **JUnit** to ensure that each device's behavior is as expected and that automation and interaction work correctly.



Git (for cloning the repository)
### Clone the repository
Clone the project to your local machine:
```bash
git clone https://github.com/your-username/SmartHomeSystemSimulation.git
```
### Run the project
cd SmartHomeSystemSimulation
```
mvn clean install

mvn exec:java -Dexec.mainClass="com.fatty.smarthome.Main"

```
#### Run Tests
Run the JUnit 5 tests to verify functionality:
```
mvn test
```
This executes all tests in src/test/java/com/fatty/smarthome/, producing a test report in the terminal.
Usage

The Main class simulates a smart home environment:





Initializes a SmartHome instance.



Adds devices: Living Room Light, Main Thermostat, Front Door Camera.



Processes user commands:





Turns on the light.



Sets the thermostat to 24°C.



Turns on the camera.



Applies LightAutomationRule to turn on lights.



Applies ThermostatAutomationRule to set the thermostat to 22°C.
```
Logged Living Room Light status: Living Room Light is OFF
Logged Main Thermostat status: Main Thermostat is OFF, Temperature: 21°C
Logged Front Door Camera status: Front Door Camera is OFF
User 1: Turn on the living room light
Living Room Light is now ON.
User 2: Set thermostat to 24°C
Main Thermostat set to 24°C
User 3: Turn on the Front Door camera
Front Door Camera is now ON. Recording...
Running light automation rule...
Logged Living Room Light status: Living Room Light is ON
Logged Main Thermostat status: Main Thermostat is OFF, Temperature: 24°C
Logged Front Door Camera status: Front Door Camera is ON
Automation rule executed successfully.
Smart Home Status Report:
Living Room Light is ON
Main Thermostat is OFF, Temperature: 24°C
Front Door Camera is ON
Security Check: Secure`
```
