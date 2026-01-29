package com.inspector.gui.model;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;

import java.io.*;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

/**
 * Manages inspection history persistence
 */
public class InspectionHistory {
    
    public static class HistoryEntry {
        public int pid;
        public String processName;
        public String windowTitle;
        public String type;
        public LocalDateTime timestamp;
        public String outputFile;
        
        public HistoryEntry() {}
        
        public HistoryEntry(int pid, String processName, String windowTitle, String type, 
                           LocalDateTime timestamp, String outputFile) {
            this.pid = pid;
            this.processName = processName;
            this.windowTitle = windowTitle;
            this.type = type;
            this.timestamp = timestamp;
            this.outputFile = outputFile;
        }
    }
    
    private static final String HISTORY_FILE = "inspection-history.json";
    private static final ObjectMapper mapper = new ObjectMapper()
            .registerModule(new JavaTimeModule())
            .enable(SerializationFeature.INDENT_OUTPUT);
    
    private List<HistoryEntry> entries;
    
    public InspectionHistory() {
        entries = new ArrayList<>();
        load();
    }
    
    public void addEntry(int pid, String processName, String windowTitle, String type, String outputFile) {
        HistoryEntry entry = new HistoryEntry(pid, processName, windowTitle, type, 
                                             LocalDateTime.now(), outputFile);
        entries.add(0, entry); // Add to beginning
        
        // Keep only last 100 entries
        if (entries.size() > 100) {
            entries = entries.subList(0, 100);
        }
        
        save();
    }
    
    public List<HistoryEntry> getEntries() {
        return new ArrayList<>(entries);
    }
    
    public void clearHistory() {
        entries.clear();
        save();
    }
    
    private void load() {
        File file = new File(HISTORY_FILE);
        if (file.exists()) {
            try {
                HistoryEntry[] array = mapper.readValue(file, HistoryEntry[].class);
                entries = new ArrayList<>(List.of(array));
            } catch (IOException e) {
                System.err.println("Failed to load history: " + e.getMessage());
            }
        }
    }
    
    private void save() {
        try {
            mapper.writeValue(new File(HISTORY_FILE), entries);
        } catch (IOException e) {
            System.err.println("Failed to save history: " + e.getMessage());
        }
    }
}
