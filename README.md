# Smart Home System.

A comprehensive Java-based home automation control system featuring both Command Line Interface (CLI) and Graphical User Interface (GUI) for managing smart home devices.

## 🏠 Overview

The Smart Home System v2.0 is an advanced home automation platform that simulates and controls various smart devices including lights, thermostats, and security cameras. Built with modern Java features, it provides real-time device management, persistent storage, analytics, and automation capabilities.

### Key Features

- **Dual Interface Support**: Choose between CLI for quick keyboard control or GUI for visual management
- **Persistent Storage**: Save and restore your home configuration with binary and JSON formats
- **Advanced Analytics**: Monitor power consumption, temperature trends, and device usage patterns
- **Smart Search & Filter**: Quickly locate devices with partial name matching and type filtering
- **Bulk Operations**: Control all devices simultaneously with single commands
- **Automation Rules**: Execute predefined actions across multiple devices
- **Real-time Logging**: Track all operations with timestamped audit trails
- **Robust Error Handling**: Intelligent validation prevents unsafe operations
- **Interactive Tutorial**: Built-in guide for new users

## 🚀 Getting Started

### Prerequisites

- **Java JDK 23** or higher
- **JavaFX** (for GUI mode)
- **Maven 3.8+** (for dependency management)
- **Git** (for cloning the repository)

### Installation

1. Clone the repository:
```bash
git clone https://github.com/your-username/SmartHomeSystem.git
cd SmartHomeSystem
```

2. Build the project:
```bash
mvn clean install
```

3. Run the application:
```bash
mvn exec:java -Dexec.mainClass="com.fatty.smarthome.Main"
```

Or run the JAR directly:
```bash
java -jar target/SmartHomeSystem-2.0.jar
```

## 💻 Usage

### Starting the Application

When you launch the application, you'll see:

```
╔═══════════════════════════════════════════════════════════╗
║               SMART HOME SYSTEM v2.0                      ║
║            Advanced Home Automation Control                ║
╚═══════════════════════════════════════════════════════════╝

Select Interface Mode:
1. Command Line Interface (CLI) - Text-based control
2. Graphical User Interface (GUI) - Visual control center  
3. Quick Demo - See system capabilities
4. Exit
```

### Command Line Interface (CLI)

The CLI provides powerful text-based control with command completion and color-coded output.

#### Basic Commands

| Command | Description | Example |
|---------|-------------|---------|
| `add <name> <type>` | Add a new device | `add KitchenLight light` |
| `remove <name>` | Remove a device | `remove KitchenLight` |
| `on <name>` | Turn device on | `on KitchenLight` |
| `off <name>` | Turn device off | `off KitchenLight` |
| `toggle <name>` | Toggle device state | `toggle KitchenLight` |
| `set <name> <temp>` | Set thermostat temperature | `set MainThermostat 23` |

#### View & Analytics Commands

| Command | Description |
|---------|-------------|
| `list` or `ls` | List all devices |
| `status` or `s` | Show system status |
| `report` or `r` | Generate detailed report |
| `analytics` | Show device analytics |
| `search <term>` | Search devices by name |
| `filter <type>` | Filter by device type or status |

#### Data Management

| Command | Description |
|---------|-------------|
| `save` | Save current configuration |
| `load` | Load saved configuration |
| `export` | Export to JSON format |
| `import` | Import from JSON file |

#### System Commands

| Command | Description |
|---------|-------------|
| `help` or `h` | Show command reference |
| `tutorial` | Start interactive tutorial |
| `clear` or `cls` | Clear screen |
| `about` | Show system information |
| `exit` or `quit` | Exit application |

### Graphical User Interface (GUI)

The GUI provides an intuitive visual interface with:

- **Device Table**: View all devices with status indicators
- **Quick Controls**: Add devices and perform bulk operations
- **Analytics Panel**: Real-time system statistics
- **Menu Bar**: File operations and settings
- **Search & Filter**: Dynamic device filtering

### Example Workflow

```bash
# Add devices
smart-home> add LivingRoomLight light
✓ Light LivingRoomLight added to the system

smart-home> add MainThermostat thermostat
✓ Thermostat MainThermostat added to the system

# Control devices
smart-home> on LivingRoomLight
✓ LivingRoomLight turned ON

smart-home> set MainThermostat 23
✓ Set MainThermostat to 23°C

# View analytics
smart-home> analytics
=== DEVICE ANALYTICS ===
Device Distribution:
  Light: 1
  Thermostat: 1
  
Power Status:
  Active devices: 2
  Estimated power usage: 25W

# Save configuration
smart-home> save
✓ Saved 2 devices successfully
```

## 📁 Project Structure

```
SmartHomeSystem/
├── src/
│   ├── main/java/com/fatty/smarthome/
│   │   ├── Main.java                    # Application entry point
│   │   ├── core/
│   │   │   ├── FacadeSmartHome.java     # Core system facade
│   │   │   ├── PersistenceService.java  # Binary/JSON persistence
│   │   │   ├── DeviceAnalytics.java     # Stream-based analytics
│   │   │   └── DeviceState.java         # Serializable state
│   │   ├── devices/
│   │   │   ├── SmartDevice.java         # Abstract base class
│   │   │   ├── Light.java               # Light implementation
│   │   │   ├── Thermostat.java          # Thermostat with temperature
│   │   │   └── SecurityCamera.java      # Camera with recording
│   │   ├── gui/
│   │   │   └── SmartHomeGUI.java        # JavaFX interface
│   │   └── util/
│   │       └── SmartHomeException.java  # Custom exceptions
│   └── test/java/
│       └── com/fatty/smarthome/         # Comprehensive test suite
├── device_states.dat                     # Binary persistence file
├── device_states.json                    # JSON backup file
├── device_log.txt                        # Operation audit log
├── pom.xml                              # Maven configuration
└── README.md                            # This file
```

## 🔧 Technical Features

### Modern Java Implementation
- **Java 23** with latest language features
- **Stream API** for efficient data processing
- **Lambda expressions** for functional operations
- **Optional** for null safety
- **Try-with-resources** for resource management

### Design Patterns
- **Facade Pattern**: Simplified interface to complex subsystems
- **Singleton Pattern**: Single system instance management
- **Factory Pattern**: Device creation abstraction
- **Observer Pattern**: Real-time GUI updates

### Persistence
- **Binary Serialization**: Fast loading/saving for daily use
- **JSON Format**: Human-readable backups and configuration sharing
- **Automatic backup**: Dual-format saving for data safety

### Error Handling
- Temperature validation (10-32°C range)
- Duplicate device prevention
- Invalid operation detection
- Graceful error recovery

## 🧪 Testing

Run the comprehensive test suite:

```bash
mvn test
```

The project includes:
- Unit tests for all device types
- Integration tests for persistence
- Analytics verification tests
- Error handling validation

## 🛡️ Safety Features

- **Temperature Limits**: Prevents unsafe thermostat settings
- **Operation Validation**: Checks device compatibility before operations
- **Data Integrity**: Validates imported configurations
- **Audit Logging**: Tracks all device operations with timestamps

## 📊 Performance

- **Instant Search**: Stream-based filtering for large device lists
- **Efficient Storage**: Binary format for quick startup
- **Lazy Loading**: Devices loaded on-demand
- **Memory Efficient**: Proper resource cleanup

## 🤝 Contributing

Contributions are welcome! Please feel free to submit pull requests or open issues for bugs and feature requests.

### Development Setup

1. Fork the repository
2. Create a feature branch (`git checkout -b feature/AmazingFeature`)
3. Commit your changes (`git commit -m 'Add some AmazingFeature'`)
4. Push to the branch (`git push origin feature/AmazingFeature`)
5. Open a Pull Request

## 📝 License

This project is licensed under the MIT License - see the LICENSE file for details.

## 🙏 Acknowledgments

- Built with modern Java best practices
- Inspired by real-world home automation systems
- Developed for educational and practical purposes

## 📞 Support

For questions or support:
- Open an issue on GitHub
- Check the built-in tutorial with `tutorial` command
- Review the comprehensive help with `help` command

---

**Smart Home System ** - Making home automation accessible and powerful.