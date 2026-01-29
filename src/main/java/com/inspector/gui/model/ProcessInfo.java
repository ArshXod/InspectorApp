package com.inspector.gui.model;

import javafx.beans.property.*;

import java.time.LocalDateTime;

/**
 * Model class representing a running process in the UI
 */
public class ProcessInfo {
    private final IntegerProperty pid;
    private final StringProperty name;
    private final StringProperty windowTitle;
    private final StringProperty type; // "Java" or "Non-Java"
    private final ObjectProperty<LocalDateTime> lastInspected;
    private final BooleanProperty hasWindow;
    
    public ProcessInfo(int pid, String name, String windowTitle, boolean isJava, boolean hasWindow) {
        this.pid = new SimpleIntegerProperty(pid);
        this.name = new SimpleStringProperty(name);
        this.windowTitle = new SimpleStringProperty(windowTitle);
        this.type = new SimpleStringProperty(isJava ? "Java" : "Non-Java");
        this.lastInspected = new SimpleObjectProperty<>();
        this.hasWindow = new SimpleBooleanProperty(hasWindow);
    }
    
    // PID
    public int getPid() { return pid.get(); }
    public void setPid(int value) { pid.set(value); }
    public IntegerProperty pidProperty() { return pid; }
    
    // Name
    public String getName() { return name.get(); }
    public void setName(String value) { name.set(value); }
    public StringProperty nameProperty() { return name; }
    
    // Window Title
    public String getWindowTitle() { return windowTitle.get(); }
    public void setWindowTitle(String value) { windowTitle.set(value); }
    public StringProperty windowTitleProperty() { return windowTitle; }
    
    // Type
    public String getType() { return type.get(); }
    public void setType(String value) { type.set(value); }
    public StringProperty typeProperty() { return type; }
    
    // Last Inspected
    public LocalDateTime getLastInspected() { return lastInspected.get(); }
    public void setLastInspected(LocalDateTime value) { lastInspected.set(value); }
    public ObjectProperty<LocalDateTime> lastInspectedProperty() { return lastInspected; }
    
    // Has Window
    public boolean hasWindow() { return hasWindow.get(); }
    public void setHasWindow(boolean value) { hasWindow.set(value); }
    public BooleanProperty hasWindowProperty() { return hasWindow; }
}
