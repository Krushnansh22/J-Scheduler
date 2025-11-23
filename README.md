# J-Scheduler - Enhanced Calendar Application

A feature-rich desktop calendar application built with Java Swing, offering comprehensive event management with an intuitive graphical interface.

## 📋 Table of Contents

-   [Features](#features)
-   [Requirements](#requirements)
-   [Installation](#installation)
-   [Usage](#usage)
-   [OOP Concepts](#oop-concepts)
-   [Project Structure](#project-structure)
-   [Screenshots](#screenshots)
-   [Future Enhancements](#future-enhancements)
-   [Contributing](#contributing)
-   [License](#license)

## ✨ Features

### Event Management

-   ✅ Create, edit, and delete events
-   ✅ Start and end times for events
-   ✅ Event descriptions and notes
-   ✅ Location tracking
-   ✅ Color-coded categories (Work, Personal, Medical, Social, Other)
-   ✅ Priority levels (High, Medium, Low)

### Calendar Views

-   📅 **Month View**: Traditional calendar grid with event indicators
-   📆 **Week View**: 7-day detailed event display
-   📋 **Day View**: Single day with full event details
-   🎯 Current day highlighting
-   🖱️ Click dates to navigate
-   🖱️ Double-click dates to create events

### Smart Reminders

-   ⏰ Configurable reminder times:
    -   15 minutes before
    -   1 hour before
    -   1 day before
    -   No reminder
-   🔔 Visual popup notifications
-   🔊 Sound alerts (system beep)
-   🚫 Duplicate notification prevention

### Data Persistence

-   💾 Automatic save on every change
-   📁 Auto-load on application startup
-   📄 Human-readable text file format
-   🔄 No data loss on application restart

## 🔧 Requirements

-   **Java JDK 8 or higher**
-   No external dependencies required
-   Works on Windows, macOS, and Linux

## 📥 Installation

### Clone Repository

```bash
git clone https://github.com/yourusername/J-Scheduler.gitcd J-Scheduler
```

### Compile

```bash
javac CalendarApplication.java
```

### Run

```bash
java CalendarApplication
```

### Basic Operations

#### Adding an Event

1.  Click **"Add Event"** button, OR
2.  Double-click a date on the calendar
3.  Fill in event details:
    -   Event Name (required)
    -   Date (yyyy-MM-dd format)
    -   Start Time (HH:mm format)
    -   End Time (HH:mm format)
    -   Category
    -   Priority
    -   Location
    -   Reminder time
    -   Description
4.  Click **"Save"**

#### Editing an Event

1.  Select event from the list, OR
2.  Double-click event in calendar view
3.  Click **"Edit Event"**
4.  Modify details
5.  Click **"Save"**

#### Deleting an Event

1.  Select event from the list
2.  Click **"Delete Event"**
3.  Confirm deletion

#### Navigation

-   **Previous/Next**: Navigate between time periods
-   **Today**: Jump to current date
-   **View Selector**: Switch between Month/Week/Day views

## 🎓 OOP Concepts Implemented

This project demonstrates key Object-Oriented Programming principles:

### 1. **Encapsulation**

```java
class Event {    private String name;    private LocalDateTime startDateTime;        public String getName() { return name; }    public void setName(String name) { this.name = name; }}
```

-   All fields are private with public getters/setters
-   Data hiding and controlled access

### 2. **Inheritance**

```java
class EventListCellRenderer extends DefaultListCellRendererclass CalendarApplication extends JFrame
```

-   Extends Swing framework classes
-   Reuses existing functionality

### 3. **Polymorphism**

```java
@Overridepublic String toString() { ... }@Overridepublic Component getListCellRendererComponent(...) { ... }
```

-   Method overriding for custom behavior
-   Interface implementations (ActionListener, MouseListener)

### 4. **Composition**

```java
class CalendarApplication {    private EventManager eventManager;  // Has-A relationship    private JList<Event> eventList;}
```

-   "Has-A" relationships over "Is-A"
-   Flexible object composition

### 5. **Abstraction**

```java
class EventManager {    public void saveEvents() { ... }  // Hides file I/O complexity    public void loadEvents() { ... }}
```

-   Hides implementation details
-   Simple public interfaces

### 6. **Enums with Behavior**

```java
enum EventCategory {    WORK(new Color(70, 130, 180)),    PERSONAL(new Color(50, 205, 50));        public Color getColor() { return color; }}
```

-   Type-safe constants with methods

## 📁 Project Structure

```
J-Scheduler/├── CalendarApplication.java    # Main application file├── calendar_events.txt          # Data storage (auto-generated)├── .gitignore                   # Git ignore rules└── README.md                    # This file
```

### Class Hierarchy

```
CalendarApplication (JFrame)├── EventManager│   └── List<Event>├── Event│   ├── EventCategory (enum)│   ├── EventPriority (enum)│   └── ReminderTime (enum)└── EventListCellRenderer (DefaultListCellRenderer)
```

## 🔮 Future Enhancements

### Planned Features

-    Search and filter events
-    Recurring events support
-    Export to iCalendar (.ics) format
-    Import from other calendar apps
-    Multi-user support
-    Cloud synchronization
-    Drag-and-drop rescheduling
-    Email integration
-    Attachment support
-    Dark mode theme

### Medium Priority

-    Custom color themes
-    Keyboard shortcuts
-    Print calendar view
-    Event templates
-    Time zone support
-    Weather integration

## 🤝 Contributing

Contributions are welcome! Please follow these steps:

1.  Fork the repository
2.  Create a feature branch (`git checkout -b feature/AmazingFeature`)
3.  Commit your changes (`git commit -m 'Add some AmazingFeature'`)
4.  Push to the branch (`git push origin feature/AmazingFeature`)
5.  Open a Pull Request

### Coding Standards

-   Follow Java naming conventions
-   Add comments for complex logic
-   Maintain OOP principles
-   Test before submitting

## 📝 License

This project is licensed under the MIT License - see below for details:

```
MIT LicenseCopyright (c) 2025 J-SchedulerPermission is hereby granted, free of charge, to any person obtaining a copyof this software and associated documentation files (the "Software"), to dealin the Software without restriction, including without limitation the rightsto use, copy, modify, merge, publish, distribute, sublicense, and/or sellcopies of the Software, and to permit persons to whom the Software isfurnished to do so, subject to the following conditions:The above copyright notice and this permission notice shall be included in allcopies or substantial portions of the Software.THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS ORIMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THEAUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHERLIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THESOFTWARE.
```

## 🙏 Acknowledgments

-   Java Swing documentation
-   Stack Overflow community
-   OOP design principles and patterns

## 📞 Support

For issues, questions, or suggestions:

-   Open an issue on GitHub
-   Email: [your.email@example.com](mailto:your.email@example.com)

## 📊 Version History

-   **v1.0.0** (2025-11-23)
    -   Initial release
    -   Full event management
    -   Multiple calendar views
    -   Reminder system
    -   Data persistence

---

**Made with ☕ and Java**