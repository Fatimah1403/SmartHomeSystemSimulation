To run the project, ensure you have the following installed:

Java JDK 23 (or compatible version)

Maven 3.8+ (for dependency management and build)

IntelliJ IDEA (recommended IDE, though others like VS Code work)
# Smart Home System Simulation (SHSS)

## Project Overview

The **Smart Home System Simulation (SHSS)** is a Java-based application designed to simulate the functioning of a smart home. It models various devices such as **lights**, **thermostats**, and **security cameras**, and provides functionality to control and automate these devices. The project demonstrates how a smart home system can be designed using **object-oriented programming** (OOP) principles.

The system allows users to **interact** with different smart devices, **automate their actions**, and **monitor their status**. The goal of this simulation is to model the behavior of these devices in a real-world smart home environment, where multiple devices interact seamlessly to provide enhanced home automation and security.

### What the Project Entails:
1. **Smart Device Classes**:
    - `Light`, `Thermostat`, and `SecurityCamera` inherit from `SmartDevice` and implement `Controllable`, supporting on/off, temperature settings, and status reporting.

2. **Automation Rules**:
    - `LightAutomationRule` automates actions like turning on lights based on conditions.

3. **Security Check**:
    - Verifies device states for security, simulating real-world monitoring.

4. **Upcasting and Downcasting**:
    - Uses **polymorphism** to treat devices as `SmartDevice` or specific types (e.g., `Thermostat`).

5. **File I/O**:
    - Persists device states to `device_log.txt` with timestamps.

6. **Exception Handling**:
    - `SmartHomeException` handles invalid inputs and file errors, with CLI recovery loops.

7. **Command History**:
    - Tracks all user commands for debugging via the `history` command.

8. **Test Coverage**:
    - 20 **JUnit 5 tests** (11 in **FacadeSmartHomeTest**, 9 in **SmartHomeTest**) ensure reliability. I proposed and implemented history, case-insensitive commands, and Javadoc, optimizing tests to avoid redundancy and fixing **testReset** for accurate state validation.

---


Git (for cloning the repository)
### Clone the repository
Clone the project to your local machine:
```bash
git clone https://github.com/your-username/SmartHomeSystemSimulation.git
cd SmartHomeSystemSimulation```
#
```
## Prerequisites

To run the project, ensure you have the following installed:

- **Java JDK 23** (or compatible version)
- **Maven 3.8+** (for dependency management and build)
- **IntelliJ IDEA** (recommended IDE; others like VS Code work)
- **Git** (for cloning the repository)

---
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
add <name> <type> (e.g., add LivingRoomLight light)
turnOn <name> (e.g., turnOn LivingRoomLight)

turnOff <name>

setTemp <name> <temp> (e.g., setTemp MainThermostat 25)

automate (applies automation rules)

report (generates status report)

clearLog (clears device_log.txt)

history (displays command history)

exit (quits the application)
```
Smart Home Automation System Simulation (type 'exit' to quit)
Commands: add <name> <type>, turnOn <name>, turnOff <name>, setTemp <name> <temp>, automate, report, clearLog, history
> ADD LivingRoomLight light
Device added: LivingRoomLight
Added LivingRoomLight
Command processed successfully.
> turnOn LivingRoomLight
LivingRoomLight is now ON.
LivingRoomLight turned ON
Command processed successfully.
> add MainThermostat thermostat
Device added: MainThermostat
Added MainThermostat
Command processed successfully.
> setTemp MainThermostat 25
Set MainThermostat to 25°C
Command processed successfully.
> history
add LivingRoomLight light
turnOn LivingRoomLight
add MainThermostat thermostat
setTemp MainThermostat 25
history
Command processed successfully.
> invalidCommand
Error: Invalid command: invalidCommand
Please re-enter a valid command (e.g., 'add LivingRoomLight light', or 'exit' to quit):
> exit
```
