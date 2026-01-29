package com.inspector.agent;

import java.lang.instrument.Instrumentation;
import java.io.IOException;
import java.net.ServerSocket;
import java.net.Socket;

/**
 * Java Agent for injecting into target Java processes.
 * This allows inspection of applications without requiring them to be started with special flags.
 * 
 * Usage:
 *   java -javaagent:inspector-agent.jar=port:9999 -jar TargetApp.jar
 * 
 * Or via Attach API:
 *   VirtualMachine.attach(pid).loadAgent("inspector-agent.jar", "port:9999");
 */
public class InspectorAgent {
    
    private static ServerSocket serverSocket;
    private static Thread serverThread;
    
    /**
     * Agent entry point when loaded at startup
     */
    public static void premain(String agentArgs, Instrumentation inst) {
        System.out.println("JavaInspector Agent: Starting in premain mode");
        startAgent(agentArgs);
    }
    
    /**
     * Agent entry point when loaded via Attach API
     */
    public static void agentmain(String agentArgs, Instrumentation inst) {
        System.out.println("=== JavaInspector Agent: Starting inspection ===");
        System.out.println("Agent args: " + agentArgs);
        System.out.println("Working directory: " + System.getProperty("user.dir"));
        
        try {
            // Determine output file - use absolute path to avoid directory issues
            String outputFile = System.getProperty("user.dir") + java.io.File.separator + "inspector-agent-output.json";
            if (agentArgs != null && agentArgs.startsWith("output:")) {
                outputFile = agentArgs.substring(7);
            }
            
            System.out.println("Output file path: " + outputFile);
            
            // Delete old output file to prevent stale data
            java.io.File oldFile = new java.io.File(outputFile);
            if (oldFile.exists()) {
                boolean deleted = oldFile.delete();
                System.out.println("JavaInspector Agent: Deleted old output file: " + deleted);
            }
            
            // Collect all windows from both AWT/Swing and JavaFX
            java.util.List<WindowInfo> allWindows = new java.util.ArrayList<>();
            
            // 1. Find AWT/Swing windows
            java.awt.Window[] awtWindows = java.awt.Window.getWindows();
            System.out.println("JavaInspector Agent: Found " + awtWindows.length + " AWT/Swing windows");
            for (java.awt.Window window : awtWindows) {
                if (window.isVisible()) {
                    allWindows.add(new WindowInfo(window));
                }
            }
            
            // 2. Find JavaFX windows
            try {
                System.out.println("JavaInspector Agent: Attempting to detect JavaFX windows...");
                Class<?> platformClass = Class.forName("javafx.application.Platform");
                System.out.println("JavaInspector Agent: JavaFX Platform class found");
                
                Class<?> stageClass = Class.forName("javafx.stage.Stage");
                Class<?> windowClass = Class.forName("javafx.stage.Window");
                System.out.println("JavaInspector Agent: JavaFX Window classes loaded");
                
                // Get all JavaFX windows using reflection
                java.lang.reflect.Method getWindowsMethod = windowClass.getMethod("getWindows");
                System.out.println("JavaInspector Agent: Found getWindows() method");
                
                Object windowsList = getWindowsMethod.invoke(null);
                System.out.println("JavaInspector Agent: getWindows() returned: " + windowsList);
                
                if (windowsList instanceof javafx.collections.ObservableList) {
                    javafx.collections.ObservableList<?> fxWindows = (javafx.collections.ObservableList<?>) windowsList;
                    System.out.println("JavaInspector Agent: Found " + fxWindows.size() + " JavaFX windows (ObservableList)");
                    
                    for (Object fxWindow : fxWindows) {
                        System.out.println("JavaInspector Agent: Checking window: " + fxWindow.getClass().getName());
                        java.lang.reflect.Method isShowingMethod = windowClass.getMethod("isShowing");
                        boolean isShowing = (Boolean) isShowingMethod.invoke(fxWindow);
                        System.out.println("JavaInspector Agent: Window showing: " + isShowing);
                        
                        if (isShowing) {
                            allWindows.add(new WindowInfo(fxWindow, true));
                            System.out.println("JavaInspector Agent: Added JavaFX window to list");
                        }
                    }
                } else if (windowsList instanceof java.util.List) {
                    java.util.List<?> fxWindows = (java.util.List<?>) windowsList;
                    System.out.println("JavaInspector Agent: Found " + fxWindows.size() + " JavaFX windows (List)");
                    
                    for (Object fxWindow : fxWindows) {
                        java.lang.reflect.Method isShowingMethod = windowClass.getMethod("isShowing");
                        boolean isShowing = (Boolean) isShowingMethod.invoke(fxWindow);
                        
                        if (isShowing) {
                            allWindows.add(new WindowInfo(fxWindow, true));
                        }
                    }
                } else {
                    System.out.println("JavaInspector Agent: getWindows() returned unexpected type: " + 
                        (windowsList != null ? windowsList.getClass().getName() : "null"));
                }
            } catch (ClassNotFoundException e) {
                System.out.println("JavaInspector Agent: JavaFX not found in classpath: " + e.getMessage());
            } catch (Exception e) {
                System.out.println("JavaInspector Agent: Error inspecting JavaFX windows: " + e.getClass().getName() + ": " + e.getMessage());
                e.printStackTrace();
            }
            
            System.out.println("JavaInspector Agent: Total visible windows: " + allWindows.size());
            
            if (allWindows.isEmpty()) {
                System.out.println("JavaInspector Agent: WARNING - No visible windows found");
                // Write empty result instead of leaving stale data
                java.io.PrintWriter writer = new java.io.PrintWriter(new java.io.FileWriter(outputFile));
                writer.println("{");
                writer.println("  \"timestamp\": \"" + new java.util.Date() + "\",");
                writer.println("  \"targetPID\": \"" + java.lang.management.ManagementFactory.getRuntimeMXBean().getName() + "\",");
                writer.println("  \"error\": \"No AWT/Swing/JavaFX windows found in target process. This may be a non-GUI Java application or the UI has not been initialized yet.\",");
                writer.println("  \"totalWindows\": 0,");
                writer.println("  \"windows\": []");
                writer.println("}");
                writer.close();
                return;
            }
            
            // Inspect and write detailed UI tree   
            java.io.PrintWriter writer = new java.io.PrintWriter(new java.io.FileWriter(outputFile));
            writer.println("{");
            writer.println("  \"timestamp\": \"" + new java.util.Date() + "\",");
            writer.println("  \"targetPID\": \"" + java.lang.management.ManagementFactory.getRuntimeMXBean().getName() + "\",");
            writer.println("  \"totalWindows\": " + allWindows.size() + ",");
            writer.println("  \"windows\": [");
            
            for (int i = 0; i < allWindows.size(); i++) {
                WindowInfo windowInfo = allWindows.get(i);
                
                writer.println("    {");
                writer.println("      \"visible\": true,");
                writer.println("      \"class\": \"" + windowInfo.getClassName() + "\",");
                writer.println("      \"type\": \"" + windowInfo.getType() + "\",");
                writer.println("      \"title\": \"" + escapeJson(windowInfo.getTitle()) + "\",");
                
                javax.accessibility.AccessibleContext ac = windowInfo.getAccessibleContext();
                if (ac != null) {
                    writer.println("      \"uiTree\": ");
                    writeAccessibleTree(writer, ac, 8, 0, 20);
                } else {
                    writer.println("      \"uiTree\": null");
                }
                
                writer.println("    }" + (i < allWindows.size() - 1 ? "," : ""));
            }
            
            writer.println("  ]");
            writer.println("}");
            writer.close();
            
            System.out.println("JavaInspector Agent: Full UI tree written to: " + new java.io.File(outputFile).getAbsolutePath());
            System.out.println("=== JavaInspector Agent: Inspection complete ===");
            
        } catch (Exception e) {
            System.err.println("JavaInspector Agent: ERROR during inspection: " + e.getMessage());
            e.printStackTrace();
        }
    }
    
    // Helper class to unify AWT/Swing and JavaFX windows
    private static class WindowInfo {
        private Object window;
        private boolean isJavaFX;
        
        public WindowInfo(java.awt.Window awtWindow) {
            this.window = awtWindow;
            this.isJavaFX = false;
        }
        
        public WindowInfo(Object fxWindow, boolean isJavaFX) {
            this.window = fxWindow;
            this.isJavaFX = isJavaFX;
        }
        
        public String getClassName() {
            return window.getClass().getName();
        }
        
        public String getType() {
            return isJavaFX ? "JavaFX" : "AWT/Swing";
        }
        
        public String getTitle() {
            try {
                if (isJavaFX) {
                    // Use reflection to call Stage.getTitle()
                    java.lang.reflect.Method getTitleMethod = window.getClass().getMethod("getTitle");
                    Object title = getTitleMethod.invoke(window);
                    return title != null ? title.toString() : "Unknown";
                } else {
                    java.awt.Window awtWindow = (java.awt.Window) window;
                    if (awtWindow instanceof java.awt.Frame) {
                        return ((java.awt.Frame) awtWindow).getTitle();
                    }
                    return "Unknown";
                }
            } catch (Exception e) {
                return "Unknown";
            }
        }
        
        public javax.accessibility.AccessibleContext getAccessibleContext() {
            try {
                if (isJavaFX) {
                    // For JavaFX, return a wrapper that will be handled specially
                    return new JavaFXAccessibleContextWrapper(window);
                } else {
                    java.awt.Window awtWindow = (java.awt.Window) window;
                    return awtWindow.getAccessibleContext();
                }
            } catch (Exception e) {
                return null;
            }
        }
        
        public Object getRawWindow() {
            return window;
        }
    }
    
    // Wrapper to mark JavaFX windows for special handling
    private static class JavaFXAccessibleContextWrapper extends javax.accessibility.AccessibleContext {
        private Object fxWindow;
        
        public JavaFXAccessibleContextWrapper(Object fxWindow) {
            this.fxWindow = fxWindow;
        }
        
        public Object getFxWindow() {
            return fxWindow;
        }
        
        @Override
        public javax.accessibility.AccessibleRole getAccessibleRole() {
            return javax.accessibility.AccessibleRole.FRAME;
        }
        
        @Override
        public javax.accessibility.AccessibleStateSet getAccessibleStateSet() {
            return null;
        }
        
        @Override
        public int getAccessibleIndexInParent() {
            return -1;
        }
        
        @Override
        public int getAccessibleChildrenCount() {
            return 0;
        }
        
        @Override
        public javax.accessibility.Accessible getAccessibleChild(int i) {
            return null;
        }
        
        @Override
        public java.util.Locale getLocale() {
            return java.util.Locale.getDefault();
        }
    }
    
    private static void writeJavaFXSceneGraph(java.io.PrintWriter writer, Object fxWindow, int indent, int depth, int maxDepth) {
        if (fxWindow == null || depth > maxDepth) {
            writer.println("null");
            return;
        }
        
        String indentStr = " ".repeat(indent);
        try {
            writer.println("{");
            
            // Get Stage properties using reflection
            Class<?> stageClass = fxWindow.getClass();
            
            // Role
            writer.println(indentStr + "  \"role\": \"frame\",");
            
            // Title
            java.lang.reflect.Method getTitleMethod = stageClass.getMethod("getTitle");
            String title = (String) getTitleMethod.invoke(fxWindow);
            writer.println(indentStr + "  \"name\": \"" + escapeJson(title) + "\",");
            writer.println(indentStr + "  \"description\": \"JavaFX Stage\",");
            writer.println(indentStr + "  \"className\": \"" + escapeJson(stageClass.getName()) + "\",");
            
            // Bounds (Stage position and size)
            java.lang.reflect.Method getXMethod = stageClass.getMethod("getX");
            java.lang.reflect.Method getYMethod = stageClass.getMethod("getY");
            java.lang.reflect.Method getWidthMethod = stageClass.getMethod("getWidth");
            java.lang.reflect.Method getHeightMethod = stageClass.getMethod("getHeight");
            
            double x = (Double) getXMethod.invoke(fxWindow);
            double y = (Double) getYMethod.invoke(fxWindow);
            double width = (Double) getWidthMethod.invoke(fxWindow);
            double height = (Double) getHeightMethod.invoke(fxWindow);
            
            writer.println(indentStr + "  \"bounds\": {");
            writer.println(indentStr + "    \"x\": " + (int)x + ",");
            writer.println(indentStr + "    \"y\": " + (int)y + ",");
            writer.println(indentStr + "    \"width\": " + (int)width + ",");
            writer.println(indentStr + "    \"height\": " + (int)height);
            writer.println(indentStr + "  },");
            
            writer.println(indentStr + "  \"indexInParent\": -1,");
            writer.println(indentStr + "  \"states\": [\"enabled\", \"visible\", \"showing\"],");
            writer.println(indentStr + "  \"actions\": [],");
            writer.println(indentStr + "  \"text\": null,");
            writer.println(indentStr + "  \"value\": null,");
            
            // Get Scene and traverse children
            java.lang.reflect.Method getSceneMethod = stageClass.getMethod("getScene");
            Object scene = getSceneMethod.invoke(fxWindow);
            
            if (scene != null && depth < maxDepth) {
                Class<?> sceneClass = scene.getClass();
                java.lang.reflect.Method getRootMethod = sceneClass.getMethod("getRoot");
                Object root = getRootMethod.invoke(scene);
                
                if (root != null) {
                    writer.println(indentStr + "  \"childCount\": 1,");
                    writer.println(indentStr + "  \"children\": [");
                    writeJavaFXNode(writer, root, indent + 4, depth + 1, maxDepth, (int)x, (int)y);
                    writer.println(indentStr + "  ]");
                } else {
                    writer.println(indentStr + "  \"childCount\": 0,");
                    writer.println(indentStr + "  \"children\": []");
                }
            } else {
                writer.println(indentStr + "  \"childCount\": 0,");
                writer.println(indentStr + "  \"children\": []");
            }
            
            writer.println(indentStr + "}");
            
        } catch (Exception e) {
            writer.println(indentStr + "\"error\": \"Failed to traverse JavaFX scene: " + e.getMessage() + "\"");
            writer.println(indentStr + "}");
        }
    }
    
    private static void writeJavaFXNode(java.io.PrintWriter writer, Object node, int indent, int depth, int maxDepth, int windowX, int windowY) {
        if (node == null || depth > maxDepth) {
            writer.println("null");
            return;
        }
        
        String indentStr = " ".repeat(indent);
        try {
            writer.println(indentStr + "{");
            
            Class<?> nodeClass = node.getClass();
            
            // Determine role based on class name
            String className = nodeClass.getSimpleName();
            String role = determineJavaFXRole(className);
            writer.println(indentStr + "  \"role\": \"" + role + "\",");
            
            // Get ID or styleClass as name
            java.lang.reflect.Method getIdMethod = nodeClass.getMethod("getId");
            String id = (String) getIdMethod.invoke(node);
            writer.println(indentStr + "  \"name\": \"" + escapeJson(id != null ? id : className) + "\",");
            writer.println(indentStr + "  \"description\": null,");
            writer.println(indentStr + "  \"className\": \"" + escapeJson(nodeClass.getName()) + "\",");
            
            // Get bounds using layoutBounds
            java.lang.reflect.Method getBoundsInParentMethod = nodeClass.getMethod("getBoundsInParent");
            Object bounds = getBoundsInParentMethod.invoke(node);
            
            Class<?> boundsClass = bounds.getClass();
            java.lang.reflect.Method getMinXMethod = boundsClass.getMethod("getMinX");
            java.lang.reflect.Method getMinYMethod = boundsClass.getMethod("getMinY");
            java.lang.reflect.Method getWidthMethod = boundsClass.getMethod("getWidth");
            java.lang.reflect.Method getHeightMethod = boundsClass.getMethod("getHeight");
            
            int x = ((Double)getMinXMethod.invoke(bounds)).intValue();
            int y = ((Double)getMinYMethod.invoke(bounds)).intValue();
            int width = ((Double)getWidthMethod.invoke(bounds)).intValue();
            int height = ((Double)getHeightMethod.invoke(bounds)).intValue();
            
            writer.println(indentStr + "  \"bounds\": {");
            writer.println(indentStr + "    \"x\": " + x + ",");
            writer.println(indentStr + "    \"y\": " + y + ",");
            writer.println(indentStr + "    \"width\": " + width + ",");
            writer.println(indentStr + "    \"height\": " + height);
            writer.println(indentStr + "  },");
            
            writer.println(indentStr + "  \"indexInParent\": 0,");
            writer.println(indentStr + "  \"states\": [\"enabled\", \"visible\"],");
            writer.println(indentStr + "  \"actions\": [],");
            
            // Try to get text content
            String text = getJavaFXText(node, nodeClass);
            writer.println(indentStr + "  \"text\": " + (text != null ? "\"" + escapeJson(text) + "\"" : "null") + ",");
            writer.println(indentStr + "  \"value\": null,");
            
            // Get children
            java.util.List<?> children = getJavaFXChildren(node, nodeClass);
            writer.println(indentStr + "  \"childCount\": " + (children != null ? children.size() : 0) + ",");
            
            if (children != null && !children.isEmpty() && depth < maxDepth) {
                writer.println(indentStr + "  \"children\": [");
                for (int i = 0; i < children.size(); i++) {
                    writeJavaFXNode(writer, children.get(i), indent + 4, depth + 1, maxDepth, windowX, windowY);
                    if (i < children.size() - 1) {
                        writer.println(",");
                    }
                }
                writer.println();
                writer.println(indentStr + "  ]");
            } else {
                writer.println(indentStr + "  \"children\": []");
            }
            
            writer.println(indentStr + "}");
            
        } catch (Exception e) {
            writer.println(indentStr + "\"error\": \"" + e.getMessage() + "\"");
            writer.println(indentStr + "}");
        }
    }
    
    private static String determineJavaFXRole(String className) {
        if (className.contains("Button")) return "push button";
        if (className.contains("Label")) return "label";
        if (className.contains("TextField")) return "text";
        if (className.contains("TextArea")) return "text";
        if (className.contains("CheckBox")) return "check box";
        if (className.contains("RadioButton")) return "radio button";
        if (className.contains("ComboBox")) return "combo box";
        if (className.contains("ListView")) return "list";
        if (className.contains("TableView")) return "table";
        if (className.contains("ScrollPane")) return "scroll pane";
        if (className.contains("SplitPane")) return "split pane";
        if (className.contains("TabPane")) return "page tab list";
        if (className.contains("MenuBar")) return "menu bar";
        if (className.contains("Menu")) return "menu";
        if (className.contains("MenuItem")) return "menu item";
        if (className.contains("Pane") || className.contains("Region")) return "panel";
        return "unknown";
    }
    
    private static String getJavaFXText(Object node, Class<?> nodeClass) {
        try {
            java.lang.reflect.Method getTextMethod = nodeClass.getMethod("getText");
            Object text = getTextMethod.invoke(node);
            return text != null ? text.toString() : null;
        } catch (Exception e) {
            return null;
        }
    }
    
    private static java.util.List<?> getJavaFXChildren(Object node, Class<?> nodeClass) {
        try {
            // Try getChildren() for Parent nodes
            java.lang.reflect.Method getChildrenMethod = nodeClass.getMethod("getChildrenUnmodifiable");
            Object children = getChildrenMethod.invoke(node);
            if (children instanceof java.util.List) {
                return (java.util.List<?>) children;
            }
        } catch (Exception e) {
            // Ignore
        }
        return null;
    }
    
    private static void writeAccessibleTree(java.io.PrintWriter writer, javax.accessibility.AccessibleContext ac, int indent, int depth, int maxDepth) {
        // Check if this is a JavaFX window wrapper
        if (ac instanceof JavaFXAccessibleContextWrapper) {
            writeJavaFXSceneGraph(writer, ((JavaFXAccessibleContextWrapper) ac).getFxWindow(), indent, depth, maxDepth);
            return;
        }
        
        if (ac == null || depth > maxDepth) {
            writer.println("null");
            return;
        }
        
        String indentStr = " ".repeat(indent);
        writer.println("{");
        
        // Basic properties
        writer.println(indentStr + "  \"role\": \"" + escapeJson(String.valueOf(ac.getAccessibleRole())) + "\",");
        writer.println(indentStr + "  \"name\": \"" + escapeJson(ac.getAccessibleName()) + "\",");
        writer.println(indentStr + "  \"description\": \"" + escapeJson(ac.getAccessibleDescription()) + "\",");
        
        // Class name
        Object component = ac.getAccessibleComponent();
        if (component != null) {
            writer.println(indentStr + "  \"className\": \"" + component.getClass().getName() + "\",");
        } else {
            writer.println(indentStr + "  \"className\": \"unknown\",");
        }
        
        // Bounds (coordinates and size)
        javax.accessibility.AccessibleComponent accessibleComp = ac.getAccessibleComponent();
        if (accessibleComp != null) {
            java.awt.Rectangle bounds = accessibleComp.getBounds();
            if (bounds != null) {
                writer.println(indentStr + "  \"bounds\": {");
                writer.println(indentStr + "    \"x\": " + bounds.x + ",");
                writer.println(indentStr + "    \"y\": " + bounds.y + ",");
                writer.println(indentStr + "    \"width\": " + bounds.width + ",");
                writer.println(indentStr + "    \"height\": " + bounds.height);
                writer.println(indentStr + "  },");
            } else {
                writer.println(indentStr + "  \"bounds\": null,");
            }
        } else {
            writer.println(indentStr + "  \"bounds\": null,");
        }
        
        // Index in parent
        writer.println(indentStr + "  \"indexInParent\": " + ac.getAccessibleIndexInParent() + ",");
        
        // States (enabled, visible, focused, etc.)
        javax.accessibility.AccessibleStateSet stateSet = ac.getAccessibleStateSet();
        if (stateSet != null) {
            writer.print(indentStr + "  \"states\": [");
            javax.accessibility.AccessibleState[] states = stateSet.toArray();
            for (int i = 0; i < states.length; i++) {
                writer.print("\"" + escapeJson(states[i].toString()) + "\"");
                if (i < states.length - 1) writer.print(", ");
            }
            writer.println("],");
        } else {
            writer.println(indentStr + "  \"states\": [],");
        }
        
        // Actions (press, toggle, etc.)
        javax.accessibility.AccessibleAction action = ac.getAccessibleAction();
        if (action != null && action.getAccessibleActionCount() > 0) {
            writer.print(indentStr + "  \"actions\": [");
            int actionCount = action.getAccessibleActionCount();
            for (int i = 0; i < actionCount; i++) {
                String actionDesc = action.getAccessibleActionDescription(i);
                writer.print("\"" + escapeJson(actionDesc) + "\"");
                if (i < actionCount - 1) writer.print(", ");
            }
            writer.println("],");
        } else {
            writer.println(indentStr + "  \"actions\": [],");
        }
        
        // Text content (for text fields, labels, etc.)
        javax.accessibility.AccessibleText accessibleText = ac.getAccessibleText();
        if (accessibleText != null) {
            int charCount = accessibleText.getCharCount();
            if (charCount > 0) {
                try {
                    StringBuilder textContent = new StringBuilder();
                    for (int idx = 0; idx < charCount; idx++) {
                        textContent.append(accessibleText.getAtIndex(javax.accessibility.AccessibleText.CHARACTER, idx));
                    }
                    writer.println(indentStr + "  \"text\": \"" + escapeJson(textContent.toString()) + "\",");
                } catch (Exception e) {
                    writer.println(indentStr + "  \"text\": null,");
                }
            } else {
                writer.println(indentStr + "  \"text\": \"\",");
            }
        } else {
            writer.println(indentStr + "  \"text\": null,");
        }
        
        // Value (for sliders, scrollbars, progress bars)
        javax.accessibility.AccessibleValue accessibleValue = ac.getAccessibleValue();
        if (accessibleValue != null) {
            Number currentValue = accessibleValue.getCurrentAccessibleValue();
            Number minValue = accessibleValue.getMinimumAccessibleValue();
            Number maxValue = accessibleValue.getMaximumAccessibleValue();
            writer.println(indentStr + "  \"value\": {");
            writer.println(indentStr + "    \"current\": " + (currentValue != null ? currentValue : "null") + ",");
            writer.println(indentStr + "    \"min\": " + (minValue != null ? minValue : "null") + ",");
            writer.println(indentStr + "    \"max\": " + (maxValue != null ? maxValue : "null"));
            writer.println(indentStr + "  },");
        } else {
            writer.println(indentStr + "  \"value\": null,");
        }
        
        writer.println(indentStr + "  \"childCount\": " + ac.getAccessibleChildrenCount() + ",");
        
        // Get children
        int childCount = ac.getAccessibleChildrenCount();
        if (childCount > 0 && depth < maxDepth) {
            writer.println(indentStr + "  \"children\": [");
            for (int i = 0; i < childCount; i++) {
                javax.accessibility.Accessible child = ac.getAccessibleChild(i);
                if (child != null) {
                    javax.accessibility.AccessibleContext childAc = child.getAccessibleContext();
                    writer.print(indentStr + "    ");
                    writeAccessibleTree(writer, childAc, indent + 4, depth + 1, maxDepth);
                    if (i < childCount - 1) writer.println(",");
                    else writer.println();
                }
            }
            writer.println(indentStr + "  ]");
        } else {
            writer.println(indentStr + "  \"children\": []");
        }
        
        writer.print(indentStr + "}");
    }
    
    private static String escapeJson(String str) {
        if (str == null) return "null";
        return str.replace("\\", "\\\\")
                 .replace("\"", "\\\"")
                 .replace("\n", "\\n")
                 .replace("\r", "\\r")
                 .replace("\t", "\\t");
    }
    
    private static void startAgent(String agentArgs) {
        int port = parsePort(agentArgs);
        
        try {
            // Enable accessibility
            enableAccessibility();
            
            // Start server socket to accept connections
            serverSocket = new ServerSocket(port);
            System.out.println("JavaInspector Agent: Listening on port " + port);
            
            serverThread = new Thread(() -> {
                while (!Thread.interrupted()) {
                    try {
                        Socket clientSocket = serverSocket.accept();
                        System.out.println("JavaInspector Agent: Client connected");
                        
                        // Handle client connection
                        AccessibilityBridge bridge = new AccessibilityBridge(clientSocket);
                        new Thread(bridge).start();
                        
                    } catch (IOException e) {
                        if (!serverSocket.isClosed()) {
                            System.err.println("JavaInspector Agent: Error accepting connection: " + e.getMessage());
                        }
                    }
                }
            });
            serverThread.setDaemon(true);
            serverThread.start();
            
        } catch (IOException e) {
            System.err.println("JavaInspector Agent: Failed to start server: " + e.getMessage());
        }
    }
    
    private static int parsePort(String agentArgs) {
        if (agentArgs != null && agentArgs.startsWith("port:")) {
            try {
                return Integer.parseInt(agentArgs.substring(5));
            } catch (NumberFormatException e) {
                System.err.println("JavaInspector Agent: Invalid port, using default 9999");
            }
        }
        return 9999;
    }
    
    private static void enableAccessibility() {
        try {
            // Enable Java Accessibility Bridge
            System.setProperty("javax.accessibility.assistive_technologies", 
                             "com.sun.java.accessibility.util.Translator");
            System.out.println("JavaInspector Agent: Accessibility enabled");
        } catch (Exception e) {
            System.err.println("JavaInspector Agent: Could not enable accessibility: " + e.getMessage());
        }
    }
    
    public static void shutdown() {
        try {
            if (serverSocket != null && !serverSocket.isClosed()) {
                serverSocket.close();
            }
            if (serverThread != null) {
                serverThread.interrupt();
            }
        } catch (IOException e) {
            System.err.println("JavaInspector Agent: Error during shutdown: " + e.getMessage());
        }
    }
}
