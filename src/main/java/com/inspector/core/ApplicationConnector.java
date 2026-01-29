package com.inspector.core;

import com.inspector.util.WindowEnumerator;
import com.inspector.uia.WindowsUIAInspector;

import javax.accessibility.AccessibleContext;
import java.awt.*;
import java.util.List;

public class ApplicationConnector {
    
    public static class ConnectionResult {
        public AccessibleContext rootContext;
        public String title;
        public int pid;
        public String error;
        
        public ConnectionResult(AccessibleContext rootContext, String title, int pid) {
            this.rootContext = rootContext;
            this.title = title;
            this.pid = pid;
        }
        
        public ConnectionResult(String error) {
            this.error = error;
        }
    }
    
    public static ConnectionResult connectByPid(int pid) {
        // First check if it's a Java process using Attach API
        if (AttachConnector.isJavaProcess(String.valueOf(pid))) {
            System.out.println("Detected Java process via Attach API");
            // Try to get accessible context through agent injection
            try {
                // Get absolute path to agent JAR
                String agentPath = "target/JavaInspector-1.0.0-agent.jar";
                java.io.File agentFile = new java.io.File(agentPath);
                if (!agentFile.exists()) {
                    // Try absolute path based on current working directory
                    agentFile = new java.io.File(System.getProperty("user.dir"), agentPath);
                }
                
                if (agentFile.exists()) {
                    System.out.println("Attempting to inject agent into target process...");
                    System.out.println("Agent path: " + agentFile.getAbsolutePath());
                    
                    // Specify output file path in our directory, not target process's directory
                    String outputPath = new java.io.File(System.getProperty("user.dir"), "inspector-agent-output.json").getAbsolutePath();
                    String result = AttachConnector.attachAndInspect(String.valueOf(pid), agentFile.getAbsolutePath(), "output:" + outputPath);
                    System.out.println(result);
                    
                    // Check if agent JSON output file was created
                    java.io.File jsonOutput = new java.io.File("inspector-agent-output.json");
                    if (jsonOutput.exists()) {
                        return new ConnectionResult("Agent successfully inspected target process.\nResults saved to: " + jsonOutput.getAbsolutePath());
                    }
                    
                    // Check for text output as fallback
                    java.io.File txtOutput = new java.io.File("inspector-agent-output.txt");
                    if (txtOutput.exists()) {
                        return new ConnectionResult("Agent successfully inspected target process.\nResults saved to: " + txtOutput.getAbsolutePath());
                    }
                    
                    return new ConnectionResult("Agent injected but no output file created. Check target process console.");
                } else {
                    System.out.println("Agent JAR not found at: " + agentFile.getAbsolutePath());
                }
            } catch (Exception e) {
                System.out.println("Attach API failed: " + e.getMessage());
                // Fall through to try window-based approach
            }
        } else {
            // Non-Java process - use Windows UIA
            System.out.println("Detected non-Java process");
            System.out.println("Switching to Windows UI Automation...");
            String result = WindowsUIAInspector.inspectProcess(pid);
            return new ConnectionResult(result);
        }
        
        // Try window-based approach (same-process only)
        WindowEnumerator.WindowInfo window = WindowEnumerator.findWindowByPid(pid);
        if (window == null) {
            return new ConnectionResult("No window found for PID: " + pid);
        }
        return connectToWindow(window);
    }
    
    public static ConnectionResult connectByTitle(String titlePattern) {
        WindowEnumerator.WindowInfo window = WindowEnumerator.findWindowByTitle(titlePattern);
        if (window == null) {
            return new ConnectionResult("No window found with title containing: " + titlePattern);
        }
        return connectToWindow(window);
    }
    
    private static ConnectionResult connectToWindow(WindowEnumerator.WindowInfo window) {
        try {
            // First, try same-process (AWT windows in current JVM)
            Window[] allWindows = Window.getWindows();
            for (Window w : allWindows) {
                if (w.isVisible() && matchesWindow(w, window.title)) {
                    AccessibleContext ac = w.getAccessibleContext();
                    if (ac != null) {
                        return new ConnectionResult(ac, window.title, window.pid);
                    }
                }
            }
            
            // If not found in same process, try Access Bridge for cross-process
            if (AccessBridgeConnector.isAvailable()) {
                try {
                    // Create HWND from window handle
                    Object hwnd = AccessBridgeConnector.createHWND(window.hwnd);
                    
                    if (AccessBridgeConnector.isJavaWindow(hwnd)) {
                        Object result = AccessBridgeConnector.getAccessibleContextFromWindow(hwnd);
                        // This would return AccessibleContext if fully implemented
                        return new ConnectionResult(
                            "Cross-process inspection detected Java window but full " +
                            "Access Bridge integration is not yet implemented. " +
                            "See AccessBridgeConnector for details."
                        );
                    } else {
                        return new ConnectionResult("Window is not a Java application (Access Bridge reports non-Java)");
                    }
                } catch (UnsupportedOperationException e) {
                    return new ConnectionResult(e.getMessage());
                }
            }
            
            String error = "Could not access Java window.\n" +
                          "Same-process: No matching window found in current JVM.\n";
            
            if (AccessBridgeConnector.getInitError() != null) {
                error += "Cross-process: " + AccessBridgeConnector.getInitError();
            } else {
                error += "Cross-process: Access Bridge available but window not accessible.";
            }
            
            return new ConnectionResult(error);
        } catch (Exception e) {
            return new ConnectionResult("Error connecting to window: " + e.getMessage());
        }
    }
    
    private static boolean matchesWindow(Window window, String title) {
        if (window instanceof Frame) {
            Frame frame = (Frame) window;
            return frame.getTitle() != null && frame.getTitle().contains(title);
        } else if (window instanceof Dialog) {
            Dialog dialog = (Dialog) window;
            return dialog.getTitle() != null && dialog.getTitle().contains(title);
        }
        return false;
    }
    
    public static List<WindowEnumerator.WindowInfo> listAllWindows() {
        return WindowEnumerator.listAllWindows();
    }
    
    public static List<WindowEnumerator.WindowInfo> listJavaWindows() {
        return WindowEnumerator.listJavaWindows();
    }
}
