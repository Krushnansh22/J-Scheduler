import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Component;
import java.awt.Cursor;
import java.awt.FlowLayout;
import java.awt.Font;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.GridLayout;
import java.awt.Insets;
import java.awt.Toolkit;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.io.PrintWriter;
import java.time.DayOfWeek;
import java.time.Duration;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.UUID;

import javax.swing.BorderFactory;
import javax.swing.Box;
import javax.swing.BoxLayout;
import javax.swing.DefaultListCellRenderer;
import javax.swing.DefaultListModel;
import javax.swing.JButton;
import javax.swing.JComboBox;
import javax.swing.JDialog;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JList;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JSplitPane;
import javax.swing.JTextArea;
import javax.swing.JTextField;
import javax.swing.SwingConstants;
import javax.swing.SwingUtilities;
import javax.swing.border.EmptyBorder;
import javax.swing.border.TitledBorder;

public class CalendarApplication extends JFrame {
    private EventManager eventManager;
    private JList<Event> eventList;
    private DefaultListModel<Event> listModel;
    private JPanel calendarPanel;
    private JLabel monthYearLabel;
    private LocalDate currentViewDate;
    private JComboBox<ViewMode> viewModeCombo;
    private javax.swing.Timer reminderTimer;
    private Set<String> notifiedEvents = new HashSet<>();

    enum ViewMode { MONTH, WEEK, DAY }

    public static void main(String[] args) {
        SwingUtilities.invokeLater(() -> new CalendarApplication().setVisible(true));
    }

    public CalendarApplication() {
        setTitle("Enhanced Calendar Application");
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        setSize(1200, 800);
        setLocationRelativeTo(null);
        
        eventManager = new EventManager();
        currentViewDate = LocalDate.now();
        
        initComponents();
        loadEvents();
        startReminderTimer();
        
        addWindowListener(new WindowAdapter() {
            @Override
            public void windowClosing(WindowEvent e) {
                eventManager.saveEvents();
            }
        });
    }

    private void initComponents() {
        setLayout(new BorderLayout(10, 10));
        
        // Top panel with navigation
        JPanel topPanel = new JPanel(new BorderLayout());
        topPanel.setBorder(new EmptyBorder(10, 10, 10, 10));
        
        JPanel navPanel = new JPanel(new FlowLayout(FlowLayout.LEFT));
        JButton prevButton = new JButton("◀ Previous");
        JButton todayButton = new JButton("Today");
        JButton nextButton = new JButton("Next ▶");
        monthYearLabel = new JLabel();
        monthYearLabel.setFont(new Font("Arial", Font.BOLD, 18));
        
        viewModeCombo = new JComboBox<>(ViewMode.values());
        viewModeCombo.addActionListener(e -> updateCalendarView());
        
        navPanel.add(prevButton);
        navPanel.add(todayButton);
        navPanel.add(nextButton);
        navPanel.add(Box.createHorizontalStrut(20));
        navPanel.add(monthYearLabel);
        navPanel.add(Box.createHorizontalStrut(20));
        navPanel.add(new JLabel("View:"));
        navPanel.add(viewModeCombo);
        
        topPanel.add(navPanel, BorderLayout.WEST);
        
        prevButton.addActionListener(e -> {
            ViewMode mode = (ViewMode) viewModeCombo.getSelectedItem();
            switch (mode) {
                case MONTH: currentViewDate = currentViewDate.minusMonths(1); break;
                case WEEK: currentViewDate = currentViewDate.minusWeeks(1); break;
                case DAY: currentViewDate = currentViewDate.minusDays(1); break;
            }
            updateCalendarView();
        });
        
        todayButton.addActionListener(e -> {
            currentViewDate = LocalDate.now();
            updateCalendarView();
        });
        
        nextButton.addActionListener(e -> {
            ViewMode mode = (ViewMode) viewModeCombo.getSelectedItem();
            switch (mode) {
                case MONTH: currentViewDate = currentViewDate.plusMonths(1); break;
                case WEEK: currentViewDate = currentViewDate.plusWeeks(1); break;
                case DAY: currentViewDate = currentViewDate.plusDays(1); break;
            }
            updateCalendarView();
        });
        
        add(topPanel, BorderLayout.NORTH);
        
        // Main split pane
        JSplitPane splitPane = new JSplitPane(JSplitPane.HORIZONTAL_SPLIT);
        splitPane.setDividerLocation(750);
        
        // Calendar view panel
        calendarPanel = new JPanel(new BorderLayout());
        calendarPanel.setBorder(new EmptyBorder(10, 10, 10, 10));
        updateCalendarView();
        
        // Event list panel
        JPanel eventListPanel = new JPanel(new BorderLayout());
        eventListPanel.setBorder(new TitledBorder("Events List"));
        
        listModel = new DefaultListModel<>();
        eventList = new JList<>(listModel);
        eventList.setCellRenderer(new EventListCellRenderer());
        
        JScrollPane scrollPane = new JScrollPane(eventList);
        eventListPanel.add(scrollPane, BorderLayout.CENTER);
        
        JPanel buttonPanel = new JPanel(new GridLayout(4, 1, 5, 5));
        buttonPanel.setBorder(new EmptyBorder(5, 5, 5, 5));
        
        JButton addButton = new JButton("Add Event");
        JButton editButton = new JButton("Edit Event");
        JButton deleteButton = new JButton("Delete Event");
        JButton refreshButton = new JButton("Refresh");
        
        buttonPanel.add(addButton);
        buttonPanel.add(editButton);
        buttonPanel.add(deleteButton);
        buttonPanel.add(refreshButton);
        
        eventListPanel.add(buttonPanel, BorderLayout.SOUTH);
        
        splitPane.setLeftComponent(calendarPanel);
        splitPane.setRightComponent(eventListPanel);
        
        add(splitPane, BorderLayout.CENTER);
        
        // Button actions
        addButton.addActionListener(e -> showEventDialog(null));
        editButton.addActionListener(e -> {
            Event selected = eventList.getSelectedValue();
            if (selected != null) {
                showEventDialog(selected);
            } else {
                JOptionPane.showMessageDialog(this, "Please select an event to edit.");
            }
        });
        
        deleteButton.addActionListener(e -> {
            Event selected = eventList.getSelectedValue();
            if (selected != null) {
                int confirm = JOptionPane.showConfirmDialog(this, 
                    "Are you sure you want to delete this event?", 
                    "Confirm Delete", JOptionPane.YES_NO_OPTION);
                if (confirm == JOptionPane.YES_OPTION) {
                    eventManager.deleteEvent(selected);
                    updateEventList();
                    updateCalendarView();
                }
            }
        });
        
        refreshButton.addActionListener(e -> {
            updateEventList();
            updateCalendarView();
        });
        
        eventList.addMouseListener(new MouseAdapter() {
            public void mouseClicked(MouseEvent e) {
                if (e.getClickCount() == 2) {
                    Event selected = eventList.getSelectedValue();
                    if (selected != null) {
                        showEventDialog(selected);
                    }
                }
            }
        });
    }

    private void updateCalendarView() {
        ViewMode mode = (ViewMode) viewModeCombo.getSelectedItem();
        calendarPanel.removeAll();
        
        switch (mode) {
            case MONTH:
                monthYearLabel.setText(currentViewDate.format(DateTimeFormatter.ofPattern("MMMM yyyy")));
                calendarPanel.add(createMonthView(), BorderLayout.CENTER);
                break;
            case WEEK:
                LocalDate weekStart = currentViewDate.with(DayOfWeek.MONDAY);
                LocalDate weekEnd = weekStart.plusDays(6);
                monthYearLabel.setText(weekStart.format(DateTimeFormatter.ofPattern("MMM d")) + 
                    " - " + weekEnd.format(DateTimeFormatter.ofPattern("MMM d, yyyy")));
                calendarPanel.add(createWeekView(), BorderLayout.CENTER);
                break;
            case DAY:
                monthYearLabel.setText(currentViewDate.format(DateTimeFormatter.ofPattern("EEEE, MMMM d, yyyy")));
                calendarPanel.add(createDayView(), BorderLayout.CENTER);
                break;
        }
        
        calendarPanel.revalidate();
        calendarPanel.repaint();
    }

    private JPanel createMonthView() {
        JPanel monthPanel = new JPanel(new BorderLayout());
        
        // Day headers
        JPanel headerPanel = new JPanel(new GridLayout(1, 7));
        String[] days = {"Mon", "Tue", "Wed", "Thu", "Fri", "Sat", "Sun"};
        for (String day : days) {
            JLabel label = new JLabel(day, SwingConstants.CENTER);
            label.setFont(new Font("Arial", Font.BOLD, 12));
            label.setBorder(BorderFactory.createLineBorder(Color.GRAY));
            headerPanel.add(label);
        }
        
        // Calendar grid
        JPanel gridPanel = new JPanel(new GridLayout(0, 7, 2, 2));
        
        LocalDate firstDay = currentViewDate.withDayOfMonth(1);
        int startDayOfWeek = firstDay.getDayOfWeek().getValue() - 1;
        
        LocalDate displayDate = firstDay.minusDays(startDayOfWeek);
        
        for (int i = 0; i < 42; i++) {
            JPanel dayPanel = createDayPanel(displayDate, displayDate.getMonth() == currentViewDate.getMonth());
            gridPanel.add(dayPanel);
            displayDate = displayDate.plusDays(1);
        }
        
        monthPanel.add(headerPanel, BorderLayout.NORTH);
        monthPanel.add(gridPanel, BorderLayout.CENTER);
        
        return monthPanel;
    }

    private JPanel createDayPanel(LocalDate date, boolean currentMonth) {
        JPanel panel = new JPanel(new BorderLayout());
        panel.setBorder(BorderFactory.createLineBorder(Color.GRAY));
        panel.setBackground(currentMonth ? Color.WHITE : new Color(240, 240, 240));
        
        if (date.equals(LocalDate.now())) {
            panel.setBackground(new Color(255, 255, 200));
        }
        
        JLabel dayLabel = new JLabel(String.valueOf(date.getDayOfMonth()), SwingConstants.CENTER);
        dayLabel.setFont(new Font("Arial", Font.PLAIN, 14));
        panel.add(dayLabel, BorderLayout.NORTH);
        
        List<Event> dayEvents = eventManager.getEventsForDate(date);
        if (!dayEvents.isEmpty()) {
            JPanel eventIndicator = new JPanel();
            eventIndicator.setBackground(panel.getBackground());
            eventIndicator.setLayout(new BoxLayout(eventIndicator, BoxLayout.Y_AXIS));
            
            for (int i = 0; i < Math.min(3, dayEvents.size()); i++) {
                Event event = dayEvents.get(i);
                JLabel eventLabel = new JLabel("• " + event.getName());
                eventLabel.setFont(new Font("Arial", Font.PLAIN, 10));
                eventLabel.setForeground(event.getCategory().getColor());
                eventIndicator.add(eventLabel);
            }
            
            if (dayEvents.size() > 3) {
                JLabel moreLabel = new JLabel("+" + (dayEvents.size() - 3) + " more");
                moreLabel.setFont(new Font("Arial", Font.ITALIC, 9));
                eventIndicator.add(moreLabel);
            }
            
            panel.add(eventIndicator, BorderLayout.CENTER);
        }
        
        final LocalDate clickedDate = date;
        panel.addMouseListener(new MouseAdapter() {
            @Override
            public void mouseClicked(MouseEvent e) {
                if (e.getClickCount() == 2) {
                    showEventDialog(null, clickedDate);
                } else {
                    currentViewDate = clickedDate;
                    updateEventList();
                }
            }
            
            @Override
            public void mouseEntered(MouseEvent e) {
                panel.setCursor(new Cursor(Cursor.HAND_CURSOR));
            }
        });
        
        return panel;
    }

    private JPanel createWeekView() {
        JPanel weekPanel = new JPanel(new GridLayout(1, 7, 5, 5));
        LocalDate weekStart = currentViewDate.with(DayOfWeek.MONDAY);
        
        for (int i = 0; i < 7; i++) {
            LocalDate date = weekStart.plusDays(i);
            weekPanel.add(createDetailedDayPanel(date));
        }
        
        return weekPanel;
    }

    private JPanel createDayView() {
        JPanel dayPanel = new JPanel(new BorderLayout());
        dayPanel.add(createDetailedDayPanel(currentViewDate), BorderLayout.CENTER);
        return dayPanel;
    }

    private JPanel createDetailedDayPanel(LocalDate date) {
        JPanel panel = new JPanel(new BorderLayout());
        panel.setBorder(BorderFactory.createTitledBorder(
            date.format(DateTimeFormatter.ofPattern("EEE, MMM d"))));
        
        if (date.equals(LocalDate.now())) {
            panel.setBackground(new Color(255, 255, 200));
        }
        
        DefaultListModel<Event> dayListModel = new DefaultListModel<>();
        List<Event> dayEvents = eventManager.getEventsForDate(date);
        dayEvents.sort(Comparator.comparing(Event::getStartDateTime));
        dayEvents.forEach(dayListModel::addElement);
        
        JList<Event> dayEventList = new JList<>(dayListModel);
        dayEventList.setCellRenderer(new EventListCellRenderer());
        
        JScrollPane scrollPane = new JScrollPane(dayEventList);
        panel.add(scrollPane, BorderLayout.CENTER);
        
        dayEventList.addMouseListener(new MouseAdapter() {
            public void mouseClicked(MouseEvent e) {
                if (e.getClickCount() == 2) {
                    Event selected = dayEventList.getSelectedValue();
                    if (selected != null) {
                        showEventDialog(selected);
                    }
                }
            }
        });
        
        return panel;
    }

    private void showEventDialog(Event event) {
        showEventDialog(event, currentViewDate);
    }

    private void showEventDialog(Event event, LocalDate defaultDate) {
        JDialog dialog = new JDialog(this, event == null ? "Add Event" : "Edit Event", true);
        dialog.setSize(500, 600);
        dialog.setLocationRelativeTo(this);
        dialog.setLayout(new BorderLayout(10, 10));
        
        JPanel formPanel = new JPanel(new GridBagLayout());
        formPanel.setBorder(new EmptyBorder(15, 15, 15, 15));
        GridBagConstraints gbc = new GridBagConstraints();
        gbc.fill = GridBagConstraints.HORIZONTAL;
        gbc.insets = new Insets(5, 5, 5, 5);
        
        // Name
        gbc.gridx = 0; gbc.gridy = 0;
        formPanel.add(new JLabel("Event Name:"), gbc);
        gbc.gridx = 1; gbc.weightx = 1.0;
        JTextField nameField = new JTextField(event != null ? event.getName() : "", 20);
        formPanel.add(nameField, gbc);
        
        // Date
        gbc.gridx = 0; gbc.gridy = 1; gbc.weightx = 0;
        formPanel.add(new JLabel("Date:"), gbc);
        gbc.gridx = 1; gbc.weightx = 1.0;
        JTextField dateField = new JTextField(event != null ? 
            event.getStartDateTime().toLocalDate().toString() : defaultDate.toString(), 10);
        formPanel.add(dateField, gbc);
        
        // Start Time
        gbc.gridx = 0; gbc.gridy = 2; gbc.weightx = 0;
        formPanel.add(new JLabel("Start Time:"), gbc);
        gbc.gridx = 1; gbc.weightx = 1.0;
        JTextField startTimeField = new JTextField(event != null ? 
            event.getStartDateTime().toLocalTime().toString() : "09:00", 10);
        formPanel.add(startTimeField, gbc);
        
        // End Time
        gbc.gridx = 0; gbc.gridy = 3; gbc.weightx = 0;
        formPanel.add(new JLabel("End Time:"), gbc);
        gbc.gridx = 1; gbc.weightx = 1.0;
        JTextField endTimeField = new JTextField(event != null ? 
            event.getEndDateTime().toLocalTime().toString() : "10:00", 10);
        formPanel.add(endTimeField, gbc);
        
        // Category
        gbc.gridx = 0; gbc.gridy = 4; gbc.weightx = 0;
        formPanel.add(new JLabel("Category:"), gbc);
        gbc.gridx = 1; gbc.weightx = 1.0;
        JComboBox<EventCategory> categoryCombo = new JComboBox<>(EventCategory.values());
        if (event != null) categoryCombo.setSelectedItem(event.getCategory());
        formPanel.add(categoryCombo, gbc);
        
        // Priority
        gbc.gridx = 0; gbc.gridy = 5; gbc.weightx = 0;
        formPanel.add(new JLabel("Priority:"), gbc);
        gbc.gridx = 1; gbc.weightx = 1.0;
        JComboBox<EventPriority> priorityCombo = new JComboBox<>(EventPriority.values());
        if (event != null) priorityCombo.setSelectedItem(event.getPriority());
        formPanel.add(priorityCombo, gbc);
        
        // Location
        gbc.gridx = 0; gbc.gridy = 6; gbc.weightx = 0;
        formPanel.add(new JLabel("Location:"), gbc);
        gbc.gridx = 1; gbc.weightx = 1.0;
        JTextField locationField = new JTextField(event != null ? event.getLocation() : "", 20);
        formPanel.add(locationField, gbc);
        
        // Reminder
        gbc.gridx = 0; gbc.gridy = 7; gbc.weightx = 0;
        formPanel.add(new JLabel("Reminder:"), gbc);
        gbc.gridx = 1; gbc.weightx = 1.0;
        JComboBox<ReminderTime> reminderCombo = new JComboBox<>(ReminderTime.values());
        if (event != null) reminderCombo.setSelectedItem(event.getReminderTime());
        formPanel.add(reminderCombo, gbc);
        
        // Description
        gbc.gridx = 0; gbc.gridy = 8; gbc.weightx = 0; gbc.anchor = GridBagConstraints.NORTH;
        formPanel.add(new JLabel("Description:"), gbc);
        gbc.gridx = 1; gbc.weightx = 1.0; gbc.weighty = 1.0; gbc.fill = GridBagConstraints.BOTH;
        JTextArea descArea = new JTextArea(event != null ? event.getDescription() : "", 5, 20);
        descArea.setLineWrap(true);
        descArea.setWrapStyleWord(true);
        JScrollPane descScroll = new JScrollPane(descArea);
        formPanel.add(descScroll, gbc);
        
        // Buttons
        JPanel buttonPanel = new JPanel(new FlowLayout(FlowLayout.RIGHT));
        JButton saveButton = new JButton("Save");
        JButton cancelButton = new JButton("Cancel");
        
        buttonPanel.add(saveButton);
        buttonPanel.add(cancelButton);
        
        dialog.add(formPanel, BorderLayout.CENTER);
        dialog.add(buttonPanel, BorderLayout.SOUTH);
        
        saveButton.addActionListener(e -> {
            try {
                String name = nameField.getText().trim();
                if (name.isEmpty()) {
                    JOptionPane.showMessageDialog(dialog, "Event name is required.");
                    return;
                }
                
                LocalDate date = LocalDate.parse(dateField.getText());
                LocalTime startTime = LocalTime.parse(startTimeField.getText());
                LocalTime endTime = LocalTime.parse(endTimeField.getText());
                
                LocalDateTime startDateTime = LocalDateTime.of(date, startTime);
                LocalDateTime endDateTime = LocalDateTime.of(date, endTime);
                
                if (endDateTime.isBefore(startDateTime)) {
                    JOptionPane.showMessageDialog(dialog, "End time must be after start time.");
                    return;
                }
                
                if (event == null) {
                    Event newEvent = new Event(name, startDateTime, endDateTime);
                    newEvent.setCategory((EventCategory) categoryCombo.getSelectedItem());
                    newEvent.setPriority((EventPriority) priorityCombo.getSelectedItem());
                    newEvent.setLocation(locationField.getText().trim());
                    newEvent.setDescription(descArea.getText().trim());
                    newEvent.setReminderTime((ReminderTime) reminderCombo.getSelectedItem());
                    eventManager.addEvent(newEvent);
                } else {
                    event.setName(name);
                    event.setStartDateTime(startDateTime);
                    event.setEndDateTime(endDateTime);
                    event.setCategory((EventCategory) categoryCombo.getSelectedItem());
                    event.setPriority((EventPriority) priorityCombo.getSelectedItem());
                    event.setLocation(locationField.getText().trim());
                    event.setDescription(descArea.getText().trim());
                    event.setReminderTime((ReminderTime) reminderCombo.getSelectedItem());
                    eventManager.saveEvents();
                }
                
                updateEventList();
                updateCalendarView();
                dialog.dispose();
            } catch (Exception ex) {
                JOptionPane.showMessageDialog(dialog, "Error: " + ex.getMessage());
            }
        });
        
        cancelButton.addActionListener(e -> dialog.dispose());
        
        dialog.setVisible(true);
    }

    private void updateEventList() {
        listModel.clear();
        List<Event> allEvents = eventManager.getAllEvents();
        allEvents.sort(Comparator.comparing(Event::getStartDateTime));
        allEvents.forEach(listModel::addElement);
    }

    private void loadEvents() {
        eventManager.loadEvents();
        updateEventList();
        updateCalendarView();
    }

    private void startReminderTimer() {
        reminderTimer = new javax.swing.Timer(30000, e -> checkReminders());
        reminderTimer.start();
    }

    private void checkReminders() {
        LocalDateTime now = LocalDateTime.now();
        
        for (Event event : eventManager.getAllEvents()) {
            if (event.getReminderTime() == ReminderTime.NONE) continue;
            
            String eventKey = event.getId() + "-" + event.getStartDateTime().toString();
            if (notifiedEvents.contains(eventKey)) continue;
            
            LocalDateTime reminderTime = event.getStartDateTime().minus(
                event.getReminderTime().getDuration());
            
            if (now.isAfter(reminderTime) && now.isBefore(event.getStartDateTime())) {
                showReminder(event);
                notifiedEvents.add(eventKey);
            }
        }
    }

    private void showReminder(Event event) {
        String message = String.format(
            "Event: %s\nTime: %s\nLocation: %s",
            event.getName(),
            event.getStartDateTime().format(DateTimeFormatter.ofPattern("MMM d, yyyy h:mm a")),
            event.getLocation().isEmpty() ? "Not specified" : event.getLocation()
        );
        
        Toolkit.getDefaultToolkit().beep();
        
        JOptionPane.showMessageDialog(this, message, "Event Reminder", 
            JOptionPane.INFORMATION_MESSAGE);
    }
}

enum EventCategory {
    WORK(new Color(70, 130, 180)),
    PERSONAL(new Color(50, 205, 50)),
    MEDICAL(new Color(220, 20, 60)),
    SOCIAL(new Color(255, 165, 0)),
    OTHER(new Color(128, 128, 128));
    
    private Color color;
    
    EventCategory(Color color) {
        this.color = color;
    }
    
    public Color getColor() {
        return color;
    }
}

enum EventPriority {
    LOW, MEDIUM, HIGH
}

enum ReminderTime {
    NONE("No reminder", Duration.ZERO),
    MINUTES_15("15 minutes before", Duration.ofMinutes(15)),
    HOUR_1("1 hour before", Duration.ofHours(1)),
    DAY_1("1 day before", Duration.ofDays(1));
    
    private String label;
    private Duration duration;
    
    ReminderTime(String label, Duration duration) {
        this.label = label;
        this.duration = duration;
    }
    
    public Duration getDuration() {
        return duration;
    }
    
    @Override
    public String toString() {
        return label;
    }
}

class Event {
    private String id;
    private String name;
    private LocalDateTime startDateTime;
    private LocalDateTime endDateTime;
    private String description;
    private EventCategory category;
    private EventPriority priority;
    private String location;
    private ReminderTime reminderTime;
    
    public Event(String name, LocalDateTime startDateTime, LocalDateTime endDateTime) {
        this.id = UUID.randomUUID().toString();
        this.name = name;
        this.startDateTime = startDateTime;
        this.endDateTime = endDateTime;
        this.description = "";
        this.category = EventCategory.OTHER;
        this.priority = EventPriority.MEDIUM;
        this.location = "";
        this.reminderTime = ReminderTime.NONE;
    }
    
    // Getters and setters
    public String getId() { return id; }
    public String getName() { return name; }
    public void setName(String name) { this.name = name; }
    public LocalDateTime getStartDateTime() { return startDateTime; }
    public void setStartDateTime(LocalDateTime startDateTime) { this.startDateTime = startDateTime; }
    public LocalDateTime getEndDateTime() { return endDateTime; }
    public void setEndDateTime(LocalDateTime endDateTime) { this.endDateTime = endDateTime; }
    public String getDescription() { return description; }
    public void setDescription(String description) { this.description = description; }
    public EventCategory getCategory() { return category; }
    public void setCategory(EventCategory category) { this.category = category; }
    public EventPriority getPriority() { return priority; }
    public void setPriority(EventPriority priority) { this.priority = priority; }
    public String getLocation() { return location; }
    public void setLocation(String location) { this.location = location; }
    public ReminderTime getReminderTime() { return reminderTime; }
    public void setReminderTime(ReminderTime reminderTime) { this.reminderTime = reminderTime; }
    
    @Override
    public String toString() {
        DateTimeFormatter timeFormatter = DateTimeFormatter.ofPattern("h:mm a");
        return String.format("[%s] %s - %s (%s)", 
            category, name, 
            startDateTime.format(timeFormatter),
            priority);
    }
}

class EventManager {
    private List<Event> events;
    private static final String DATA_FILE = "calendar_events.txt";
    
    public EventManager() {
        events = new ArrayList<>();
    }
    
    public void addEvent(Event event) {
        events.add(event);
        saveEvents();
    }
    
    public void deleteEvent(Event event) {
        events.remove(event);
        saveEvents();
    }
    
    public List<Event> getAllEvents() {
        return new ArrayList<>(events);
    }
    
    public List<Event> getEventsForDate(LocalDate date) {
        List<Event> dateEvents = new ArrayList<>();
        for (Event event : events) {
            if (event.getStartDateTime().toLocalDate().equals(date)) {
                dateEvents.add(event);
            }
        }
        return dateEvents;
    }
    
    public void saveEvents() {
        try (PrintWriter writer = new PrintWriter(new FileWriter(DATA_FILE))) {
            for (Event event : events) {
                writer.println("EVENT_START");
                writer.println("ID:" + event.getId());
                writer.println("NAME:" + event.getName());
                writer.println("START:" + event.getStartDateTime().toString());
                writer.println("END:" + event.getEndDateTime().toString());
                writer.println("DESCRIPTION:" + event.getDescription());
                writer.println("CATEGORY:" + event.getCategory().name());
                writer.println("PRIORITY:" + event.getPriority().name());
                writer.println("LOCATION:" + event.getLocation());
                writer.println("REMINDER:" + event.getReminderTime().name());
                writer.println("EVENT_END");
            }
        } catch (IOException e) {
            System.err.println("Error saving events: " + e.getMessage());
        }
    }
    
    public void loadEvents() {
        File file = new File(DATA_FILE);
        if (!file.exists()) return;
        
        try (BufferedReader reader = new BufferedReader(new FileReader(DATA_FILE))) {
            String line;
            Event currentEvent = null;
            String id = null, name = null, description = null, location = null;
            LocalDateTime start = null, end = null;
            EventCategory category = null;
            EventPriority priority = null;
            ReminderTime reminder = null;
            
            while ((line = reader.readLine()) != null) {
                if (line.equals("EVENT_START")) {
                    id = name = description = location = null;
                    start = end = null;
                    category = null;
                    priority = null;
                    reminder = null;
                } else if (line.equals("EVENT_END")) {
                    if (name != null && start != null && end != null) {
                        Event event = new Event(name, start, end);
                        if (description != null) event.setDescription(description);
                        if (category != null) event.setCategory(category);
                        if (priority != null) event.setPriority(priority);
                        if (location != null) event.setLocation(location);
                        if (reminder != null) event.setReminderTime(reminder);
                        events.add(event);
                    }
                } else if (line.startsWith("ID:")) {
                    id = line.substring(3);
                } else if (line.startsWith("NAME:")) {
                    name = line.substring(5);
                } else if (line.startsWith("START:")) {
                    start = LocalDateTime.parse(line.substring(6));
                } else if (line.startsWith("END:")) {
                    end = LocalDateTime.parse(line.substring(4));
                } else if (line.startsWith("DESCRIPTION:")) {
                    description = line.substring(12);
                } else if (line.startsWith("CATEGORY:")) {
                    try {
                        category = EventCategory.valueOf(line.substring(9));
                    } catch (IllegalArgumentException e) {
                        category = EventCategory.OTHER;
                    }
                } else if (line.startsWith("PRIORITY:")) {
                    try {
                        priority = EventPriority.valueOf(line.substring(9));
                    } catch (IllegalArgumentException e) {
                        priority = EventPriority.MEDIUM;
                    }
                } else if (line.startsWith("LOCATION:")) {
                    location = line.substring(9);
                } else if (line.startsWith("REMINDER:")) {
                    try {
                        reminder = ReminderTime.valueOf(line.substring(9));
                    } catch (IllegalArgumentException e) {
                        reminder = ReminderTime.NONE;
                    }
                }
            }
        } catch (IOException e) {
            System.err.println("Error loading events: " + e.getMessage());
        }
    }
}

class EventListCellRenderer extends DefaultListCellRenderer {
    @Override
    public Component getListCellRendererComponent(JList<?> list, Object value, 
            int index, boolean isSelected, boolean cellHasFocus) {
        
        JLabel label = (JLabel) super.getListCellRendererComponent(
            list, value, index, isSelected, cellHasFocus);
        
        if (value instanceof Event) {
            Event event = (Event) value;
            
            DateTimeFormatter dateFormatter = DateTimeFormatter.ofPattern("MMM d, yyyy");
            DateTimeFormatter timeFormatter = DateTimeFormatter.ofPattern("h:mm a");
            
            String text = String.format("<html><b>%s</b><br>%s | %s - %s<br><i>%s</i></html>",
                event.getName(),
                event.getStartDateTime().format(dateFormatter),
                event.getStartDateTime().format(timeFormatter),
                event.getEndDateTime().format(timeFormatter),
                event.getLocation().isEmpty() ? "No location" : event.getLocation()
            );
            
            label.setText(text);
            
            if (!isSelected) {
                label.setBackground(new Color(255, 255, 255));
                label.setForeground(event.getCategory().getColor());
            }
            
            // Priority indicator
            switch (event.getPriority()) {
                case HIGH:
                    label.setBorder(BorderFactory.createLineBorder(Color.RED, 2));
                    break;
                case MEDIUM:
                    label.setBorder(BorderFactory.createLineBorder(Color.ORANGE, 1));
                    break;
                case LOW:
                    label.setBorder(BorderFactory.createLineBorder(Color.GREEN, 1));
                    break;
            }
        }
        
        return label;
    }
}