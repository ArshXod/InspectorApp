package com.inspector.agent;

import com.inspector.core.UITreeExplorer;
import com.inspector.model.ElementData;
import com.fasterxml.jackson.databind.ObjectMapper;

import javax.accessibility.AccessibleContext;
import java.awt.*;
import java.io.*;
import java.net.Socket;

/**
 * Bridge that handles communication between the inspector client and the target application.
 * Receives commands via socket and returns AccessibleContext data as JSON.
 */
public class AccessibilityBridge implements Runnable {
    
    private final Socket socket;
    private final ObjectMapper mapper;
    
    public AccessibilityBridge(Socket socket) {
        this.socket = socket;
        this.mapper = new ObjectMapper();
    }
    
    @Override
    public void run() {
        try (BufferedReader in = new BufferedReader(new InputStreamReader(socket.getInputStream()));
             PrintWriter out = new PrintWriter(socket.getOutputStream(), true)) {
            
            String command;
            while ((command = in.readLine()) != null) {
                String response = handleCommand(command);
                out.println(response);
            }
            
        } catch (IOException e) {
            System.err.println("AccessibilityBridge: Connection error: " + e.getMessage());
        } finally {
            try {
                socket.close();
            } catch (IOException e) {
                // Ignore
            }
        }
    }
    
    private String handleCommand(String command) {
        try {
            if (command.startsWith("EXPLORE")) {
                return handleExplore(command);
            } else if (command.equals("LIST_WINDOWS")) {
                return handleListWindows();
            } else {
                return errorResponse("Unknown command: " + command);
            }
        } catch (Exception e) {
            return errorResponse("Error processing command: " + e.getMessage());
        }
    }
    
    private String handleExplore(String command) throws Exception {
        // Command format: EXPLORE <maxDepth> <includeInvisible>
        String[] parts = command.split(" ");
        int maxDepth = parts.length > 1 ? Integer.parseInt(parts[1]) : 20;
        boolean includeInvisible = parts.length > 2 ? Boolean.parseBoolean(parts[2]) : false;
        
        // Get all visible windows
        Window[] windows = Window.getWindows();
        for (Window window : windows) {
            if (window.isVisible()) {
                AccessibleContext ac = window.getAccessibleContext();
                if (ac != null) {
                    UITreeExplorer explorer = new UITreeExplorer(maxDepth, includeInvisible);
                    ElementData rootElement = explorer.explore(ac);
                    
                    // Convert to JSON
                    String json = mapper.writeValueAsString(rootElement);
                    return "OK " + json;
                }
            }
        }
        
        return errorResponse("No accessible windows found");
    }
    
    private String handleListWindows() throws Exception {
        Window[] windows = Window.getWindows();
        StringBuilder sb = new StringBuilder();
        sb.append("OK [");
        
        boolean first = true;
        for (Window window : windows) {
            if (window.isVisible()) {
                if (!first) sb.append(",");
                
                String title = "";
                if (window instanceof Frame) {
                    title = ((Frame) window).getTitle();
                } else if (window instanceof Dialog) {
                    title = ((Dialog) window).getTitle();
                }
                
                sb.append("{\"title\":\"").append(escapeJson(title))
                  .append("\",\"class\":\"").append(window.getClass().getName())
                  .append("\"}");
                first = false;
            }
        }
        
        sb.append("]");
        return sb.toString();
    }
    
    private String errorResponse(String message) {
        return "ERROR " + escapeJson(message);
    }
    
    private String escapeJson(String str) {
        if (str == null) return "";
        return str.replace("\\", "\\\\")
                  .replace("\"", "\\\"")
                  .replace("\n", "\\n")
                  .replace("\r", "\\r")
                  .replace("\t", "\\t");
    }
}
