import java.awt.BasicStroke;
import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Component;
import java.awt.Cursor;
import java.awt.Dimension;
import java.awt.FlowLayout;
import java.awt.Font;
import java.awt.FontMetrics;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.GridLayout;
import java.awt.RenderingHints;
import java.awt.Window;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.awt.geom.Rectangle2D;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.io.Serializable;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.Date;
import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

import javax.swing.BorderFactory;
import javax.swing.Box;
import javax.swing.BoxLayout;
import javax.swing.DefaultListModel;
import javax.swing.JButton;
import javax.swing.JComboBox;
import javax.swing.JComponent;
import javax.swing.JDialog;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JList;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JSpinner;
import javax.swing.JTextArea;
import javax.swing.JTextField;
import javax.swing.ListCellRenderer;
import javax.swing.ListSelectionModel;
import javax.swing.SpinnerDateModel;
import javax.swing.SwingConstants;
import javax.swing.SwingUtilities;
import javax.swing.SwingWorker;
import javax.swing.UIManager;
import javax.swing.border.EmptyBorder;
import javax.swing.border.LineBorder;
import javax.swing.border.MatteBorder;
import javax.swing.event.DocumentEvent;
import javax.swing.event.DocumentListener;

public class ModernCalendarApp {

    // ==========================================
    // THEME & STYLING
    // ==========================================
    public static class Theme {
        public static final Color BG_APP = new Color(243, 244, 246);
        public static final Color TEXT_PRIMARY = new Color(17, 24, 39);
        public static final Color TEXT_SECONDARY = new Color(107, 114, 128);
        public static final Color ACCENT = new Color(79, 70, 229);
        public static final Color BORDER = new Color(229, 231, 235);
        public static final Color INPUT_BG = new Color(249, 250, 251);
        public static final Color SELECTION = new Color(238, 242, 255);
        public static final Color PRIORITY_HIGH = new Color(220, 38, 38);
        public static final Color PRIORITY_MED = new Color(217, 119, 6);
        public static final Color PRIORITY_LOW = new Color(5, 150, 105);

        public static final Font FONT_HEADER = new Font("Segoe UI", Font.BOLD, 22);
        public static final Font FONT_TITLE = new Font("Segoe UI", Font.BOLD, 16);
        public static final Font FONT_BOLD = new Font("Segoe UI", Font.BOLD, 13);
        public static final Font FONT_REGULAR = new Font("Segoe UI", Font.PLAIN, 13);
        public static final Font FONT_SMALL = new Font("Segoe UI", Font.PLAIN, 11);

        public static void applyAntialiasing(Graphics2D g2) {
            g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
            g2.setRenderingHint(RenderingHints.KEY_TEXT_ANTIALIASING, RenderingHints.VALUE_TEXT_ANTIALIAS_LCD_HRGB);
        }
    }

    // ==========================================
    // PROFILE MANAGEMENT
    // ==========================================
    static class Profile implements Serializable {
        private static final long serialVersionUID = 1L;
        private String id;
        private String name;
        
        public Profile(String name) {
            this.id = UUID.randomUUID().toString();
            this.name = name;
        }
        
        public String getId() { return id; }
        public String getName() { return name; }
        public String getFileName() { return "events_" + id + ".dat"; }
        
        @Override
        public String toString() { return name; }
    }
    
    static class ProfileManager {
        private static final String PROFILES_FILE = "profiles.dat";
        private static final String ACTIVE_PROFILE_FILE = "active_profile.dat";
        private List<Profile> profiles = new ArrayList<>();
        private Profile activeProfile;
        private List<Runnable> listeners = new ArrayList<>();
        
        public ProfileManager() {
            loadProfiles();
            loadActiveProfile();
            if (profiles.isEmpty()) {
                Profile defaultProfile = new Profile("Default");
                profiles.add(defaultProfile);
                activeProfile = defaultProfile;
                saveProfiles();
                saveActiveProfile();
            }
        }
        
        public Profile getActiveProfile() { return activeProfile; }
        public List<Profile> getProfiles() { return new ArrayList<>(profiles); }
        
        public void createProfile(String name) {
            Profile profile = new Profile(name);
            profiles.add(profile);
            saveProfiles();
            notifyListeners();
        }
        
        public void switchProfile(Profile profile) {
            if (profiles.contains(profile)) {
                activeProfile = profile;
                saveActiveProfile();
                notifyListeners();
            }
        }
        
        public void deleteProfile(Profile profile) {
            if (profiles.size() <= 1) {
                throw new IllegalStateException("Cannot delete the last profile");
            }
            profiles.remove(profile);
            File eventFile = new File(profile.getFileName());
            if (eventFile.exists()) eventFile.delete();
            if (activeProfile.equals(profile)) {
                activeProfile = profiles.get(0);
                saveActiveProfile();
            }
            saveProfiles();
            notifyListeners();
        }
        
        public void addListener(Runnable r) { listeners.add(r); }
        private void notifyListeners() { listeners.forEach(Runnable::run); }
        
        @SuppressWarnings("unchecked")
        private void loadProfiles() {
            File f = new File(PROFILES_FILE);
            if (f.exists()) {
                try (ObjectInputStream ois = new ObjectInputStream(new FileInputStream(f))) {
                    profiles = (List<Profile>) ois.readObject();
                } catch (Exception e) { profiles = new ArrayList<>(); }
            }
        }
        
        private void saveProfiles() {
            try (ObjectOutputStream oos = new ObjectOutputStream(new FileOutputStream(PROFILES_FILE))) {
                oos.writeObject(profiles);
            } catch (IOException e) { e.printStackTrace(); }
        }
        
        private void loadActiveProfile() {
            File f = new File(ACTIVE_PROFILE_FILE);
            if (f.exists()) {
                try (ObjectInputStream ois = new ObjectInputStream(new FileInputStream(f))) {
                    String activeId = (String) ois.readObject();
                    activeProfile = profiles.stream().filter(p -> p.getId().equals(activeId))
                        .findFirst().orElse(profiles.isEmpty() ? null : profiles.get(0));
                } catch (Exception e) { 
                    activeProfile = profiles.isEmpty() ? null : profiles.get(0);
                }
            } else {
                activeProfile = profiles.isEmpty() ? null : profiles.get(0);
            }
        }
        
        private void saveActiveProfile() {
            if (activeProfile != null) {
                try (ObjectOutputStream oos = new ObjectOutputStream(new FileOutputStream(ACTIVE_PROFILE_FILE))) {
                    oos.writeObject(activeProfile.getId());
                } catch (IOException e) { e.printStackTrace(); }
            }
        }
    }

    // ==========================================
    // DATA MODEL
    // ==========================================
    enum EventPriority {
        HIGH("High Priority", Theme.PRIORITY_HIGH),
        MEDIUM("Medium Priority", Theme.PRIORITY_MED),
        LOW("Low Priority", Theme.PRIORITY_LOW);

        final String label;
        final Color color;
        EventPriority(String label, Color color) { this.label = label; this.color = color; }
    }

    static class CalendarEvent implements Serializable {
        private static final long serialVersionUID = 1L;
        private String id;
        private String title;
        private LocalDateTime start;
        private LocalDateTime end;
        private EventPriority priority;
        private String description;

        public CalendarEvent(String title, LocalDateTime start, LocalDateTime end, EventPriority priority, String description) {
            this.id = UUID.randomUUID().toString();
            this.title = title;
            this.start = start;
            this.end = end;
            this.priority = priority;
            this.description = description;
        }
        
        public String getId() { return id; }
        public String getTitle() { return title; }
        public void setTitle(String t) { this.title = t; }
        public LocalDateTime getStart() { return start; }
        public void setStart(LocalDateTime s) { this.start = s; }
        public EventPriority getPriority() { return priority; }
        public void setPriority(EventPriority p) { this.priority = p; }
        public String getDescription() { return description; }
        public void setDescription(String d) { this.description = d; }
    }

    // ==========================================
    // EVENT MANAGER
    // ==========================================
    static class EventManager {
        private List<CalendarEvent> events = new ArrayList<>();
        private List<Runnable> listeners = new ArrayList<>();
        private ProfileManager profileManager;

        public EventManager(ProfileManager profileManager) {
            this.profileManager = profileManager;
        }

        public void addEvent(CalendarEvent e) { events.add(e); update(); }
        public void removeEvent(CalendarEvent e) { events.remove(e); update(); }
        public void updateEvent(CalendarEvent original, CalendarEvent updated) {
            int idx = events.indexOf(original);
            if (idx >= 0) { events.set(idx, updated); update(); }
        }

        public List<CalendarEvent> getEvents(LocalDate date) {
            return events.stream()
                .filter(e -> e.getStart().toLocalDate().equals(date))
                .sorted(Comparator.comparing(CalendarEvent::getPriority).thenComparing(CalendarEvent::getStart))
                .collect(Collectors.toList());
        }
        
        public List<CalendarEvent> searchEvents(String query) {
            if (query == null || query.trim().isEmpty()) return getAllEvents();
            String lowerQ = query.toLowerCase();
            return events.stream()
                .filter(e -> e.getTitle().toLowerCase().contains(lowerQ))
                .sorted(Comparator.comparing(CalendarEvent::getPriority))
                .collect(Collectors.toList());
        }

        public List<CalendarEvent> getAllEvents() {
            return events.stream()
                .sorted(Comparator.comparing(CalendarEvent::getPriority).thenComparing(CalendarEvent::getStart))
                .collect(Collectors.toList());
        }

        private void update() { notifyListeners(); saveAsync(); }
        public void addListener(Runnable r) { listeners.add(r); }
        private void notifyListeners() { listeners.forEach(Runnable::run); }

        @SuppressWarnings("unchecked")
        public void loadAsync() {
            new SwingWorker<List<CalendarEvent>, Void>() {
                @Override
                protected List<CalendarEvent> doInBackground() {
                    Profile active = profileManager.getActiveProfile();
                    if (active == null) return new ArrayList<>();
                    File f = new File(active.getFileName());
                    if (f.exists()) {
                        try (ObjectInputStream ois = new ObjectInputStream(new FileInputStream(f))) {
                            return (List<CalendarEvent>) ois.readObject();
                        } catch (Exception e) { e.printStackTrace(); }
                    }
                    return new ArrayList<>();
                }
                @Override
                protected void done() {
                    try { events = get(); notifyListeners(); } catch (Exception e) { e.printStackTrace(); }
                }
            }.execute();
        }

        private void saveAsync() {
            Profile active = profileManager.getActiveProfile();
            if (active == null) return;
            List<CalendarEvent> snapshot = new ArrayList<>(events);
            new SwingWorker<Void, Void>() {
                @Override
                protected Void doInBackground() {
                    try (ObjectOutputStream oos = new ObjectOutputStream(new FileOutputStream(active.getFileName()))) {
                        oos.writeObject(snapshot);
                    } catch (IOException e) { e.printStackTrace(); }
                    return null;
                }
            }.execute();
        }
    }

    // ==========================================
    // PROFILE SELECTOR UI
    // ==========================================
    static class ProfileSelector extends JPanel {
        private ProfileManager profileManager;
        private JComboBox<Profile> profileCombo;
        private EventManager eventManager;
        
        public ProfileSelector(ProfileManager profileManager, EventManager eventManager) {
            this.profileManager = profileManager;
            this.eventManager = eventManager;
            
            setLayout(new FlowLayout(FlowLayout.LEFT, 10, 0));
            setBackground(Theme.BG_APP);
            setBorder(new EmptyBorder(10, 20, 10, 20));
            
            JLabel label = new JLabel("Profile:");
            label.setFont(Theme.FONT_BOLD);
            label.setForeground(Theme.TEXT_PRIMARY);
            
            profileCombo = new JComboBox<>();
            profileCombo.setFont(Theme.FONT_REGULAR);
            profileCombo.setBackground(Color.WHITE);
            profileCombo.setPreferredSize(new Dimension(150, 30));
            refreshProfiles();
            
            profileCombo.addActionListener(e -> {
                Profile selected = (Profile) profileCombo.getSelectedItem();
                if (selected != null && !selected.equals(profileManager.getActiveProfile())) {
                    profileManager.switchProfile(selected);
                    eventManager.loadAsync();
                }
            });
            
            StyledButton btnNew = new StyledButton("New", Theme.ACCENT, Color.WHITE);
            btnNew.setPreferredSize(new Dimension(70, 30));
            btnNew.addActionListener(e -> createProfile());
            
            StyledButton btnDelete = new StyledButton("Delete", Theme.PRIORITY_HIGH, Color.WHITE);
            btnDelete.setPreferredSize(new Dimension(70, 30));
            btnDelete.addActionListener(e -> deleteProfile());
            
            add(label);
            add(profileCombo);
            add(btnNew);
            add(btnDelete);
            
            profileManager.addListener(this::refreshProfiles);
        }
        
        private void refreshProfiles() {
            profileCombo.removeAllItems();
            for (Profile p : profileManager.getProfiles()) {
                profileCombo.addItem(p);
            }
            profileCombo.setSelectedItem(profileManager.getActiveProfile());
        }
        
        private void createProfile() {
            String name = JOptionPane.showInputDialog(this, "Enter profile name:", "New Profile", JOptionPane.PLAIN_MESSAGE);
            if (name != null && !name.trim().isEmpty()) {
                profileManager.createProfile(name.trim());
            }
        }
        
        private void deleteProfile() {
            Profile active = profileManager.getActiveProfile();
            if (profileManager.getProfiles().size() <= 1) {
                JOptionPane.showMessageDialog(this, "Cannot delete the last profile!", "Error", JOptionPane.ERROR_MESSAGE);
                return;
            }
            int result = JOptionPane.showConfirmDialog(this, 
                "Delete profile '" + active.getName() + "' and all its events?", 
                "Confirm Delete", JOptionPane.YES_NO_OPTION, JOptionPane.WARNING_MESSAGE);
            if (result == JOptionPane.YES_OPTION) {
                try {
                    profileManager.deleteProfile(active);
                    eventManager.loadAsync();
                } catch (IllegalStateException ex) {
                    JOptionPane.showMessageDialog(this, ex.getMessage(), "Error", JOptionPane.ERROR_MESSAGE);
                }
            }
        }
    }

    // ==========================================
    // UI COMPONENTS
    // ==========================================
    
    static class ModernTextField extends JTextField {
        public ModernTextField(String text) {
            super(text);
            setOpaque(false);
            setBorder(new EmptyBorder(10, 15, 10, 15));
            setFont(Theme.FONT_REGULAR);
            setForeground(Theme.TEXT_PRIMARY);
        }
        @Override
        protected void paintComponent(Graphics g) {
            Graphics2D g2 = (Graphics2D) g.create();
            Theme.applyAntialiasing(g2);
            g2.setColor(Theme.INPUT_BG);
            g2.fillRoundRect(0, 0, getWidth(), getHeight(), 12, 12);
            g2.setColor(Theme.BORDER);
            g2.drawRoundRect(0, 0, getWidth()-1, getHeight()-1, 12, 12);
            super.paintComponent(g);
            g2.dispose();
        }
    }

    static class StyledButton extends JButton {
        private Color bg, fg;
        public StyledButton(String text, Color bg, Color fg) {
            super(text);
            this.bg = bg;
            this.fg = fg;
            setContentAreaFilled(false);
            setFocusPainted(false);
            setBorderPainted(false);
            setFont(Theme.FONT_BOLD);
            setForeground(fg);
            setCursor(new Cursor(Cursor.HAND_CURSOR));
            setBorder(new EmptyBorder(10, 20, 10, 20));
        }
        @Override
        protected void paintComponent(Graphics g) {
            Graphics2D g2 = (Graphics2D) g.create();
            Theme.applyAntialiasing(g2);
            g2.setColor(getModel().isRollover() ? bg.brighter() : bg);
            g2.fillRoundRect(0, 0, getWidth(), getHeight(), 10, 10);
            FontMetrics fm = g2.getFontMetrics();
            Rectangle2D r = fm.getStringBounds(getText(), g2);
            int x = (getWidth() - (int) r.getWidth()) / 2;
            int y = (getHeight() - (int) r.getHeight()) / 2 + fm.getAscent();
            g2.setColor(getForeground());
            g2.drawString(getText(), x, y);
            g2.dispose();
        }
    }

    static class CalendarCell extends JPanel {
        private LocalDate date;
        private boolean isToday, isSelected, isCurrentMonth;
        private List<CalendarEvent> events = new ArrayList<>();

        public CalendarCell(LocalDate date, boolean isCurrentMonth, boolean isToday) {
            this.date = date;
            this.isCurrentMonth = isCurrentMonth;
            this.isToday = isToday;
            setBackground(Color.WHITE);
            setLayout(null);
        }
        public void setEvents(List<CalendarEvent> events) { this.events = events; repaint(); }
        public void setSelected(boolean b) { this.isSelected = b; repaint(); }

        @Override
        protected void paintComponent(Graphics g) {
            super.paintComponent(g);
            Graphics2D g2 = (Graphics2D) g.create();
            Theme.applyAntialiasing(g2);
            int w = getWidth(), h = getHeight();

            if (isSelected) {
                g2.setColor(Theme.SELECTION);
                g2.fillRect(0, 0, w, h);
                g2.setColor(Theme.ACCENT);
                g2.setStroke(new BasicStroke(2));
                g2.drawRect(1,1,w-2,h-2);
            } else if (isToday) {
                g2.setColor(new Color(248, 250, 252));
                g2.fillRect(0, 0, w, h);
            }

            // Draw date number
            g2.setFont(Theme.FONT_BOLD);
            g2.setColor(isToday ? Theme.ACCENT : (isCurrentMonth ? Theme.TEXT_PRIMARY : Theme.TEXT_SECONDARY));
            g2.drawString(String.valueOf(date.getDayOfMonth()), 8, 20);

            // Draw event titles
            if (!events.isEmpty()) {
                g2.setFont(Theme.FONT_SMALL);
                int yOffset = 30;
                int maxEvents = Math.min(3, events.size());
                
                for (int i = 0; i < maxEvents; i++) {
                    CalendarEvent evt = events.get(i);
                    g2.setColor(evt.getPriority().color);
                    
                    String title = evt.getTitle();
                    FontMetrics fm = g2.getFontMetrics();
                    if (fm.stringWidth(title) > w - 10) {
                        while (fm.stringWidth(title + "...") > w - 10 && title.length() > 0) {
                            title = title.substring(0, title.length() - 1);
                        }
                        title = title + "...";
                    }
                    
                    int textWidth = fm.stringWidth(title);
                    g2.setColor(new Color(evt.getPriority().color.getRed(), 
                                         evt.getPriority().color.getGreen(), 
                                         evt.getPriority().color.getBlue(), 30));
                    g2.fillRoundRect(5, yOffset - 10, Math.min(textWidth + 6, w - 10), 14, 4, 4);
                    
                    g2.setColor(evt.getPriority().color.darker());
                    g2.drawString(title, 7, yOffset);
                    yOffset += 16;
                }
                
                if (events.size() > maxEvents) {
                    g2.setColor(Theme.TEXT_SECONDARY);
                    g2.drawString("+" + (events.size() - maxEvents) + " more", 7, yOffset);
                }
            }
            
            g2.setColor(Theme.BORDER);
            g2.drawRect(0, 0, w-1, h-1);
            g2.dispose();
        }
    }

    static class CalendarPanel extends JPanel {
        private LocalDate currentMonth;
        private EventManager manager;
        private Consumer<LocalDate> dateCallback;
        private List<CalendarCell> cells = new ArrayList<>();
        private CalendarCell selectedCell = null;
        private JLabel monthLabel;
        private JPanel gridContainer;

        public CalendarPanel(EventManager manager, Consumer<LocalDate> dateCallback) {
            this.manager = manager;
            this.dateCallback = dateCallback;
            this.currentMonth = LocalDate.now().withDayOfMonth(1);
            
            setLayout(new BorderLayout());
            setBackground(Color.WHITE);
            
            initHeader();
            
            gridContainer = new JPanel(new BorderLayout());
            gridContainer.setBackground(Color.WHITE);
            add(gridContainer, BorderLayout.CENTER);
            
            refresh();
        }

        private void initHeader() {
            JPanel header = new JPanel(new BorderLayout());
            header.setBackground(Color.WHITE);
            header.setBorder(new EmptyBorder(0, 0, 15, 0));

            monthLabel = new JLabel("", SwingConstants.CENTER);
            monthLabel.setFont(Theme.FONT_HEADER);
            monthLabel.setForeground(Theme.TEXT_PRIMARY);

            // Navigation Buttons
            StyledButton prev = new StyledButton("<", Theme.BG_APP, Theme.TEXT_PRIMARY);
            StyledButton next = new StyledButton(">", Theme.BG_APP, Theme.TEXT_PRIMARY);
            StyledButton todayBtn = new StyledButton("Today", Theme.SELECTION, Theme.ACCENT);
            
            prev.setPreferredSize(new Dimension(50, 35));
            next.setPreferredSize(new Dimension(50, 35));
            todayBtn.setPreferredSize(new Dimension(80, 35));

            prev.addActionListener(e -> { currentMonth = currentMonth.minusMonths(1); refresh(); });
            next.addActionListener(e -> { currentMonth = currentMonth.plusMonths(1); refresh(); });
            todayBtn.addActionListener(e -> { 
                currentMonth = LocalDate.now().withDayOfMonth(1); 
                refresh(); 
            });

            // Layout for Left (Prev + Today)
            JPanel leftActions = new JPanel(new FlowLayout(FlowLayout.LEFT, 5, 0));
            leftActions.setBackground(Color.WHITE);
            leftActions.add(prev);
            leftActions.add(todayBtn);

            header.add(leftActions, BorderLayout.WEST);
            header.add(monthLabel, BorderLayout.CENTER);
            header.add(next, BorderLayout.EAST);
            add(header, BorderLayout.NORTH);
        }
        
        public void setDate(LocalDate date) {
            this.currentMonth = date.withDayOfMonth(1);
            refresh();
        }

        public void refresh() {
            monthLabel.setText(currentMonth.format(DateTimeFormatter.ofPattern("MMMM yyyy")));

            gridContainer.removeAll();
            cells.clear();
            selectedCell = null;

            JPanel dayNames = new JPanel(new GridLayout(1, 7));
            dayNames.setBackground(Color.WHITE);
            dayNames.setBorder(new EmptyBorder(0,0,5,0));
            String[] days = {"Mon", "Tue", "Wed", "Thu", "Fri", "Sat", "Sun"};
            for(String d : days) {
                JLabel l = new JLabel(d, SwingConstants.CENTER);
                l.setFont(Theme.FONT_BOLD);
                l.setForeground(Theme.TEXT_SECONDARY);
                dayNames.add(l);
            }
            gridContainer.add(dayNames, BorderLayout.NORTH);

            JPanel grid = new JPanel(new GridLayout(0, 7)); 
            LocalDate firstDay = currentMonth.withDayOfMonth(1);
            int startDayOfWeek = firstDay.getDayOfWeek().getValue() - 1; 
            LocalDate displayDate = firstDay.minusDays(startDayOfWeek);

            for(int i=0; i<42; i++) {
                boolean isCurrent = displayDate.getMonth() == currentMonth.getMonth() && 
                                   displayDate.getYear() == currentMonth.getYear();
                boolean isToday = displayDate.equals(LocalDate.now());
                
                CalendarCell cell = new CalendarCell(displayDate, isCurrent, isToday);
                cell.setEvents(manager.getEvents(displayDate));
                cell.setPreferredSize(new Dimension(100, 80));
                
                final LocalDate d = displayDate;
                final CalendarCell c = cell;
                
                cell.addMouseListener(new MouseAdapter() {
                    public void mousePressed(MouseEvent e) {
                        if(selectedCell != null) selectedCell.setSelected(false);
                        selectedCell = c;
                        selectedCell.setSelected(true);
                        dateCallback.accept(d);
                    }
                });

                cells.add(cell);
                grid.add(cell);
                displayDate = displayDate.plusDays(1);
            }
            
            gridContainer.add(grid, BorderLayout.CENTER);
            gridContainer.revalidate();
            gridContainer.repaint();
        }

        public LocalDate getCurrentSelection() { return currentMonth; }
    }

    static class Sidebar extends JPanel {
        private EventManager manager;
        private DefaultListModel<CalendarEvent> listModel;
        private JList<CalendarEvent> eventList;
        private LocalDate selectedDate;
        private CalendarPanel linkedCalendar;

        public Sidebar(EventManager manager) {
            this.manager = manager;
            setLayout(new BorderLayout());
            setBackground(Theme.BG_APP);
            setBorder(new EmptyBorder(25, 20, 25, 20));
            setPreferredSize(new Dimension(340, 0));

            JPanel topPanel = new JPanel(new BorderLayout(0, 10));
            topPanel.setOpaque(false);
            JLabel lbl = new JLabel("Events Manager");
            lbl.setFont(Theme.FONT_HEADER);
            
            ModernTextField searchField = new ModernTextField("");
            searchField.getDocument().addDocumentListener(new SimpleDocListener(e -> filterList(searchField.getText())));

            topPanel.add(lbl, BorderLayout.NORTH);
            topPanel.add(searchField, BorderLayout.CENTER);
            add(topPanel, BorderLayout.NORTH);

            listModel = new DefaultListModel<>();
            eventList = new JList<>(listModel);
            eventList.setCellRenderer(new EventRenderer());
            eventList.setBackground(Theme.BG_APP);
            eventList.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
            
            eventList.addMouseListener(new MouseAdapter() {
                public void mouseClicked(MouseEvent e) {
                    CalendarEvent sel = eventList.getSelectedValue();
                    if (sel == null) return;
                    if (linkedCalendar != null) linkedCalendar.setDate(sel.getStart().toLocalDate());
                    if (e.getClickCount() == 2) editEvent(sel);
                }
            });

            JScrollPane scroll = new JScrollPane(eventList);
            scroll.setBorder(null);
            scroll.getViewport().setBackground(Theme.BG_APP);
            
            JPanel scrollWrapper = new JPanel(new BorderLayout());
            scrollWrapper.setOpaque(false);
            scrollWrapper.setBorder(new EmptyBorder(15, 0, 15, 0));
            scrollWrapper.add(scroll);
            add(scrollWrapper, BorderLayout.CENTER);

            JPanel controls = new JPanel(new GridLayout(1, 3, 10, 0));
            controls.setOpaque(false);
            
            StyledButton btnAdd = new StyledButton("Add", Theme.ACCENT, Color.WHITE);
            StyledButton btnEdit = new StyledButton("Edit", Color.WHITE, Theme.TEXT_PRIMARY);
            StyledButton btnDel = new StyledButton("Del", Theme.PRIORITY_HIGH, Color.WHITE);

            btnAdd.addActionListener(e -> addEvent());
            btnEdit.addActionListener(e -> editEvent(eventList.getSelectedValue()));
            btnDel.addActionListener(e -> deleteEvent());

            controls.add(btnAdd);
            controls.add(btnEdit);
            controls.add(btnDel);
            add(controls, BorderLayout.SOUTH);
            
            JButton showAllBtn = new JButton("Show All Events");
            showAllBtn.setContentAreaFilled(false);
            showAllBtn.setBorderPainted(false);
            showAllBtn.setForeground(Theme.ACCENT);
            showAllBtn.setCursor(new Cursor(Cursor.HAND_CURSOR));
            showAllBtn.addActionListener(e -> { selectedDate = null; searchField.setText(""); filterList(""); });
            topPanel.add(showAllBtn, BorderLayout.SOUTH);
        }
        
        public void setLinkedCalendar(CalendarPanel cp) { this.linkedCalendar = cp; }

        public void filterByDate(LocalDate date) {
            this.selectedDate = date;
            filterList("");
        }

        private void filterList(String query) {
            listModel.clear();
            List<CalendarEvent> data;
            if (!query.isEmpty()) data = manager.searchEvents(query);
            else if (selectedDate != null) data = manager.getEvents(selectedDate);
            else data = manager.getAllEvents();
            data.forEach(listModel::addElement);
        }

        private void addEvent() {
            LocalDate baseDate = (linkedCalendar != null) ? linkedCalendar.getCurrentSelection().withDayOfMonth(LocalDate.now().getDayOfMonth()) : LocalDate.now();
            if (selectedDate != null) baseDate = selectedDate;
            new ModernEventDialog(SwingUtilities.getWindowAncestor(this), manager, baseDate, null).setVisible(true);
        }
        
        private void editEvent(CalendarEvent sel) {
            if (sel != null) new ModernEventDialog(SwingUtilities.getWindowAncestor(this), manager, sel.getStart().toLocalDate(), sel).setVisible(true);
        }
        
        private void deleteEvent() {
            CalendarEvent sel = eventList.getSelectedValue();
            if (sel != null && JOptionPane.showConfirmDialog(this, "Delete event?", "Confirm", JOptionPane.YES_NO_OPTION) == JOptionPane.YES_OPTION) {
                manager.removeEvent(sel);
            }
        }
    }

    static class EventRenderer extends JPanel implements ListCellRenderer<CalendarEvent> {
        private JLabel title = new JLabel();
        private JLabel meta = new JLabel();
        private JPanel statusColor = new JPanel();

        public EventRenderer() {
            setLayout(new BorderLayout(10, 0));
            setBackground(Color.WHITE);
            setBorder(new EmptyBorder(10, 10, 10, 10));
            statusColor.setPreferredSize(new Dimension(5, 40));
            
            JPanel text = new JPanel(new GridLayout(2, 1));
            text.setOpaque(false);
            title.setFont(Theme.FONT_BOLD);
            meta.setFont(Theme.FONT_SMALL);
            meta.setForeground(Theme.TEXT_SECONDARY);
            text.add(title);
            text.add(meta);
            
            add(statusColor, BorderLayout.WEST);
            add(text, BorderLayout.CENTER);
            setBorder(BorderFactory.createCompoundBorder(new MatteBorder(0,0,1,0, Theme.BORDER), new EmptyBorder(10, 10, 10, 10)));
        }

        @Override
        public Component getListCellRendererComponent(JList<? extends CalendarEvent> list, CalendarEvent value, int index, boolean isSelected, boolean cellHasFocus) {
            title.setText(value.getTitle());
            meta.setText(value.getStart().format(DateTimeFormatter.ofPattern("MMM d, yyyy • HH:mm")) + " • " + value.getPriority().label);
            statusColor.setBackground(value.getPriority().color);
            setBackground(isSelected ? Theme.SELECTION : Color.WHITE);
            return this;
        }
    }

    static class ModernEventDialog extends JDialog {
        public ModernEventDialog(Window owner, EventManager manager, LocalDate date, CalendarEvent editing) {
            super(owner, editing == null ? "New Event" : "Edit Event", ModalityType.APPLICATION_MODAL);
            setSize(450, 580);
            setLocationRelativeTo(owner);
            
            JPanel p = new JPanel();
            p.setLayout(new BoxLayout(p, BoxLayout.Y_AXIS));
            p.setBorder(new EmptyBorder(25, 30, 25, 30));
            p.setBackground(Color.WHITE);

            JLabel header = new JLabel(editing == null ? "Create New Event" : "Edit Event");
            header.setFont(Theme.FONT_TITLE);
            header.setAlignmentX(Component.LEFT_ALIGNMENT);
            p.add(header);
            p.add(Box.createVerticalStrut(20));

            ModernTextField title = new ModernTextField(editing != null ? editing.getTitle() : "");
            ModernTextField dateField = new ModernTextField(date.toString());
            
            SpinnerDateModel timeModel = new SpinnerDateModel();
            JSpinner timeSpin = new JSpinner(timeModel);
            timeSpin.setEditor(new JSpinner.DateEditor(timeSpin, "HH:mm"));
            timeSpin.setValue(java.sql.Timestamp.valueOf((editing != null ? editing.getStart() : date.atTime(9, 0))));
            timeSpin.setBorder(BorderFactory.createCompoundBorder(new LineBorder(Theme.BORDER), new EmptyBorder(5,5,5,5)));

            JComboBox<EventPriority> priCombo = new JComboBox<>(EventPriority.values());
            priCombo.setBackground(Color.WHITE);
            if (editing != null) priCombo.setSelectedItem(editing.getPriority());

            JTextArea desc = new JTextArea(editing != null ? editing.getDescription() : "", 4, 20);
            desc.setLineWrap(true);
            desc.setBorder(new EmptyBorder(10, 10, 10, 10));
            desc.setBackground(Theme.INPUT_BG);
            JScrollPane descScroll = new JScrollPane(desc);
            descScroll.setBorder(new LineBorder(Theme.BORDER));

            addLabel(p, "Event Title"); p.add(wrap(title));
            p.add(Box.createVerticalStrut(15));
            
            JPanel row2 = new JPanel(new GridLayout(1, 2, 15, 0));
            row2.setBackground(Color.WHITE);
            row2.setAlignmentX(Component.LEFT_ALIGNMENT);
            row2.setMaximumSize(new Dimension(Short.MAX_VALUE, 60));
            
            JPanel p1 = new JPanel(new BorderLayout()); p1.setBackground(Color.WHITE);
            p1.add(createLabel("Date (YYYY-MM-DD)"), BorderLayout.NORTH);
            p1.add(dateField, BorderLayout.CENTER);
            
            JPanel p2 = new JPanel(new BorderLayout()); p2.setBackground(Color.WHITE);
            p2.add(createLabel("Time"), BorderLayout.NORTH);
            p2.add(timeSpin, BorderLayout.CENTER);
            
            row2.add(p1); row2.add(p2);
            p.add(row2);
            p.add(Box.createVerticalStrut(15));
            
            addLabel(p, "Priority"); p.add(wrap(priCombo));
            p.add(Box.createVerticalStrut(15));
            
            addLabel(p, "Description"); p.add(descScroll);
            p.add(Box.createVerticalStrut(25));
            
            JPanel btnPanel = new JPanel(new FlowLayout(FlowLayout.RIGHT));
            btnPanel.setBackground(Color.WHITE);
            btnPanel.setAlignmentX(Component.LEFT_ALIGNMENT);
            
            StyledButton cancel = new StyledButton("Cancel", Theme.BG_APP, Theme.TEXT_PRIMARY);
            cancel.addActionListener(e -> dispose());
            
            StyledButton save = new StyledButton("Save Event", Theme.ACCENT, Color.WHITE);
            save.addActionListener(e -> {
                try {
                    String t = title.getText();
                    if(t.isEmpty()) throw new Exception("Title required");
                    LocalDate d = LocalDate.parse(dateField.getText());
                    LocalTime tm = ((Date)timeSpin.getValue()).toInstant().atZone(ZoneId.systemDefault()).toLocalTime();
                    LocalDateTime dt = LocalDateTime.of(d, tm);
                    
                    if (editing == null) manager.addEvent(new CalendarEvent(t, dt, dt.plusHours(1), (EventPriority)priCombo.getSelectedItem(), desc.getText()));
                    else {
                        editing.setTitle(t); editing.setStart(dt);
                        editing.setPriority((EventPriority)priCombo.getSelectedItem()); 
                        editing.setDescription(desc.getText());
                        manager.updateEvent(editing, editing);
                    }
                    dispose();
                } catch(Exception ex) { JOptionPane.showMessageDialog(this, "Error: " + ex.getMessage()); }
            });
            btnPanel.add(cancel);
            btnPanel.add(save);
            p.add(btnPanel);
            add(p);
        }
        
        private void addLabel(JPanel p, String t) {
            JLabel l = new JLabel(t);
            l.setFont(Theme.FONT_BOLD);
            l.setForeground(Theme.TEXT_SECONDARY);
            l.setAlignmentX(Component.LEFT_ALIGNMENT);
            p.add(l);
            p.add(Box.createVerticalStrut(5));
        }
        private JLabel createLabel(String t) {
            JLabel l = new JLabel(t);
            l.setFont(Theme.FONT_BOLD);
            l.setForeground(Theme.TEXT_SECONDARY);
            l.setBorder(new EmptyBorder(0,0,5,0));
            return l;
        }
        private JComponent wrap(JComponent c) {
            c.setMaximumSize(new Dimension(Short.MAX_VALUE, 40));
            c.setAlignmentX(Component.LEFT_ALIGNMENT);
            return c;
        }
    }

    static class SimpleDocListener implements DocumentListener {
        private final Consumer<DocumentEvent> c;
        public SimpleDocListener(Consumer<DocumentEvent> c) { this.c = c; }
        public void insertUpdate(DocumentEvent e) { c.accept(e); }
        public void removeUpdate(DocumentEvent e) { c.accept(e); }
        public void changedUpdate(DocumentEvent e) { c.accept(e); }
    }

    public static void main(String[] args) {
        SwingUtilities.invokeLater(() -> {
            try { UIManager.setLookAndFeel(UIManager.getSystemLookAndFeelClassName()); } catch(Exception ignored){}
            
            JFrame frame = new JFrame("Swing AI Scheduler - Multi-Profile");
            frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
            frame.setSize(1100, 750);
            frame.setLocationRelativeTo(null);
            
            ProfileManager profileManager = new ProfileManager();
            EventManager eventManager = new EventManager(profileManager);
            
            Sidebar sidebar = new Sidebar(eventManager);
            CalendarPanel calendar = new CalendarPanel(eventManager, date -> sidebar.filterByDate(date));
            sidebar.setLinkedCalendar(calendar);
            
            ProfileSelector profileSelector = new ProfileSelector(profileManager, eventManager);
            
            eventManager.addListener(() -> {
                calendar.refresh();
                sidebar.filterByDate(null);
            });
            
            JPanel mainContent = new JPanel(new BorderLayout());
            mainContent.setBackground(Theme.BG_APP);
            mainContent.setBorder(new EmptyBorder(30, 30, 30, 10)); 
            
            calendar.setBorder(BorderFactory.createCompoundBorder(
                new LineBorder(Theme.BORDER, 1), new EmptyBorder(15, 15, 15, 15)
            ));
            
            mainContent.add(profileSelector, BorderLayout.NORTH);
            mainContent.add(calendar, BorderLayout.CENTER);
            frame.add(mainContent, BorderLayout.CENTER);
            frame.add(sidebar, BorderLayout.EAST);
            
            frame.setVisible(true);
            eventManager.loadAsync();
        });
    }
    
    interface Consumer<T> { void accept(T t); }
}