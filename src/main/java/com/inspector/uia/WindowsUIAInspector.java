package com.inspector.uia;

import com.sun.jna.Native;
import com.sun.jna.Pointer;
import com.sun.jna.platform.win32.*;
import com.sun.jna.platform.win32.WinDef.HWND;
import com.sun.jna.platform.win32.WinDef.RECT;
import com.sun.jna.ptr.IntByReference;

import java.io.FileWriter;
import java.io.PrintWriter;
import java.util.Date;

/**
 * Enhanced Windows UI Automation inspector for non-Java applications
 * 
 * IMPORTANT LIMITATIONS:
 * - Chromium/Electron apps (Teams, VS Code, Discord, Slack) render UI content in a browser canvas
 * - Native Windows APIs can only see window containers (Chrome_WidgetWin_*, RenderWidgetHostHWND)
 * - Actual UI elements (buttons, text, menus) are NOT accessible via standard Win32/UIA APIs
 * - Full UI tree inspection requires Chrome DevTools Protocol or specialized accessibility APIs
 * - Traditional Windows apps (Notepad, Calculator, Office) work correctly with full element names
 */
public class WindowsUIAInspector {
    
    private static final int MAX_DEPTH = 30; // Increased depth for complex UIs
    
    /**
     * Inspect a non-Java process using Windows UI Automation
     * NOTE: Modern web-based apps (Electron/CEF) only show window structure, not UI content
     */
    public static String inspectProcess(int pid) {
        long startTime = System.currentTimeMillis();
        System.out.println("Using Enhanced Windows UI Automation for non-Java process");
        
        try {
            // Find window by PID
            HWND hwnd = findWindowByPid(pid);
            if (hwnd == null) {
                return "No window found for PID: " + pid;
            }
            
            // Check if this is a Chromium/Electron-based app (limited accessibility)
            char[] classNameTemp = new char[256];
            User32.INSTANCE.GetClassName(hwnd, classNameTemp, 256);
            String windowClassTemp = Native.toString(classNameTemp);
            boolean isChromiumApp = windowClassTemp.contains("Chrome") || 
                                   windowClassTemp.contains("Electron") || 
                                   windowClassTemp.contains("Teams") ||
                                   windowClassTemp.contains("Discord") ||
                                   windowClassTemp.contains("Slack") ||
                                   windowClassTemp.contains("VSCode");
            
            String outputFile = "inspector-agent-output.json";
            try (PrintWriter writer = new PrintWriter(new FileWriter(outputFile))) {
            
            writer.println("{");
            writer.println("  \"timestamp\": \"" + new Date() + "\",");
            writer.println("  \"targetPID\": \"" + pid + "\",");
            writer.println("  \"inspectionMethod\": \"Enhanced Windows UIA\",");
            writer.println("  \"maxDepth\": " + MAX_DEPTH + ",");
            
            // Add warning for Chromium apps
            if (isChromiumApp) {
                writer.println("  \"WARNING\": \"This is a Chromium/Electron application. The output shows only window containers, not actual UI content. UI elements are rendered in a canvas and not accessible via standard Windows APIs. For full UI inspection, use Chrome DevTools Protocol or specialized accessibility tools.\",");
            }
            
            writer.println("  \"totalWindows\": 1,");
            writer.println("  \"windows\": [");
            writer.println("    {");
            
            // Get window title
            char[] windowText = new char[512];
            User32.INSTANCE.GetWindowText(hwnd, windowText, 512);
            String title = Native.toString(windowText);
            
            // Get window class
            char[] className = new char[256];
            User32.INSTANCE.GetClassName(hwnd, className, 256);
            String windowClass = Native.toString(className);
            
            // Get window rectangle
            RECT rect = new RECT();
            User32.INSTANCE.GetWindowRect(hwnd, rect);
            
            writer.println("      \"visible\": " + User32.INSTANCE.IsWindowVisible(hwnd) + ",");
            writer.println("      \"class\": \"" + escapeJson(windowClass) + "\",");
            writer.println("      \"title\": \"" + escapeJson(title) + "\",");
            writer.println("      \"bounds\": {");
            writer.println("        \"x\": " + rect.left + ",");
            writer.println("        \"y\": " + rect.top + ",");
            writer.println("        \"width\": " + (rect.right - rect.left) + ",");
            writer.println("        \"height\": " + (rect.bottom - rect.top));
            writer.println("      },");
            
            // Enumerate child windows
            writer.println("      \"uiTree\": {");
            writer.println("        \"role\": \"window\",");
            writer.println("        \"name\": \"" + escapeJson(title) + "\",");
            writer.println("        \"className\": \"" + escapeJson(windowClass) + "\",");
            writer.println("        \"bounds\": {");
            writer.println("          \"x\": " + rect.left + ",");
            writer.println("          \"y\": " + rect.top + ",");
            writer.println("          \"width\": " + (rect.right - rect.left) + ",");
            writer.println("          \"height\": " + (rect.bottom - rect.top));
            writer.println("        },");
            
            // Enumerate child controls recursively with enhanced metadata
            writer.println("        \"children\": [");
            int totalControls = enumerateChildWindowsRecursive(writer, hwnd, 10, 0, MAX_DEPTH);
            writer.println("        ]");
            writer.println("      }");
            
            writer.println("    }");
            writer.println("  ],");
            
            long duration = System.currentTimeMillis() - startTime;
            writer.println("  \"inspectionStats\": {");
            writer.println("    \"totalControlsFound\": " + totalControls + ",");
            writer.println("    \"durationMs\": " + duration + ",");
            writer.println("    \"maxDepthLimit\": " + MAX_DEPTH);
            writer.println("  }");
            writer.println("}");
            
            System.out.println("Enhanced Windows UIA inspection complete");
            System.out.println("Total controls found: " + totalControls + " in " + duration + "ms");
            return "Results saved to: " + new java.io.File(outputFile).getAbsolutePath();
            
            }
        } catch (java.io.IOException e) {
            return "Windows UIA inspection failed - IO Error: " + e.getMessage();
        } catch (Exception e) {
            e.printStackTrace();
            return "Windows UIA inspection failed: " + e.getMessage();
        }
    }
    
    private static int enumerateChildWindowsRecursive(PrintWriter writer, HWND parent, int indent, int depth, int maxDepth) {
        if (depth > maxDepth) return 0;
        
        final java.util.List<HWND> childHandles = new java.util.ArrayList<>();
        int controlCount = 0;
        
        // First, collect all child HWNDs
        User32.INSTANCE.EnumChildWindows(parent, (hwnd, data) -> {
            childHandles.add(hwnd);
            return true;
        }, null);
        
        String indentStr = " ".repeat(indent);
        
        // Now process each child with enhanced metadata
        for (int i = 0; i < childHandles.size(); i++) {
            HWND hwnd = childHandles.get(i);
            
            try {
                controlCount++;
                
                // Get control info
                char[] text = new char[512];
                User32.INSTANCE.GetWindowText(hwnd, text, 512);
                String controlText = Native.toString(text);
                
                char[] className = new char[256];
                User32.INSTANCE.GetClassName(hwnd, className, 256);
                String controlClass = Native.toString(className);
                
                RECT rect = new RECT();
                User32.INSTANCE.GetWindowRect(hwnd, rect);
                
                boolean isVisible = User32.INSTANCE.IsWindowVisible(hwnd);
                boolean isEnabled = User32.INSTANCE.IsWindowEnabled(hwnd);
                
                // Get window styles for additional metadata
                int style = User32.INSTANCE.GetWindowLong(hwnd, WinUser.GWL_STYLE);
                int exStyle = User32.INSTANCE.GetWindowLong(hwnd, WinUser.GWL_EXSTYLE);
                int controlId = User32.INSTANCE.GetWindowLong(hwnd, WinUser.GWL_ID);
                
                // Detect control type for better semantic information
                String controlType = detectControlType(controlClass, style);
                
                writer.println(indentStr + "{");
                writer.println(indentStr + "  \"role\": \"" + controlType + "\",");
                writer.println(indentStr + "  \"name\": \"" + escapeJson(controlText) + "\",");
                writer.println(indentStr + "  \"className\": \"" + escapeJson(controlClass) + "\",");
                
                if (controlId != 0) {
                    writer.println(indentStr + "  \"controlId\": " + controlId + ",");
                }
                
                writer.println(indentStr + "  \"bounds\": {");
                writer.println(indentStr + "    \"x\": " + rect.left + ",");
                writer.println(indentStr + "    \"y\": " + rect.top + ",");
                writer.println(indentStr + "    \"width\": " + (rect.right - rect.left) + ",");
                writer.println(indentStr + "    \"height\": " + (rect.bottom - rect.top));
                writer.println(indentStr + "  },");
                writer.println(indentStr + "  \"depth\": " + depth + ",");
                writer.println(indentStr + "  \"indexInParent\": " + i + ",");
                
                // Enhanced state detection
                writer.print(indentStr + "  \"states\": [");
                java.util.List<String> states = new java.util.ArrayList<>();
                if (isVisible) states.add("\"visible\"");
                if (isEnabled) states.add("\"enabled\"");
                if ((style & WinUser.WS_DISABLED) != 0) states.add("\"disabled\"");
                if ((style & WinUser.WS_CHILD) != 0) states.add("\"child\"");
                if ((exStyle & 0x00000008) != 0) states.add("\"topmost\""); // WS_EX_TOPMOST
                writer.print(String.join(", ", states));
                writer.println("],");
                
                writer.println(indentStr + "  \"hwnd\": \"" + hwnd.getPointer().toString() + "\",");
                
                // Recursively enumerate children of this control
                writer.println(indentStr + "  \"children\": [");
                int childCount = enumerateChildWindowsRecursive(writer, hwnd, indent + 4, depth + 1, maxDepth);
                writer.println(indentStr + "  ],");
                writer.println(indentStr + "  \"childCount\": " + childCount);
                
                writer.print(indentStr + "}");
                
                if (i < childHandles.size() - 1) {
                    writer.println(",");
                } else {
                    writer.println();
                }
                
            } catch (Exception e) {
                // Skip problematic controls but log details
                System.err.println("Error processing control at depth " + depth + ": " + e.getMessage());
            }
        }
        
        return controlCount;
    }
    
    private static HWND findWindowByPid(int targetPid) {
        final HWND[] result = new HWND[1];
        
        User32.INSTANCE.EnumWindows((hwnd, data) -> {
            IntByReference pid = new IntByReference();
            User32.INSTANCE.GetWindowThreadProcessId(hwnd, pid);
            
            if (pid.getValue() == targetPid && User32.INSTANCE.IsWindowVisible(hwnd)) {
                result[0] = hwnd;
                return false; // Stop enumeration
            }
            return true; // Continue enumeration
        }, null);
        
        return result[0];
    }
    
    /**
     * Detect control type from class name and styles
     */
    private static String detectControlType(String className, int style) {
        if (className == null) return "control";
        
        String lower = className.toLowerCase();
        
        // Common Windows controls
        if (lower.contains("button")) return "button";
        if (lower.contains("edit")) return "textbox";
        if (lower.contains("static")) return "label";
        if (lower.contains("listbox")) return "list";
        if (lower.contains("combobox")) return "combobox";
        if (lower.contains("scrollbar")) return "scrollbar";
        if (lower.contains("toolbar")) return "toolbar";
        if (lower.contains("statusbar")) return "statusbar";
        if (lower.contains("treeview")) return "tree";
        if (lower.contains("listview")) return "table";
        if (lower.contains("header")) return "header";
        if (lower.contains("tab")) return "tab";
        if (lower.contains("progress")) return "progressbar";
        if (lower.contains("slider") || lower.contains("trackbar")) return "slider";
        if (lower.contains("spin")) return "spinner";
        if (lower.contains("rebar") || lower.contains("coolbar")) return "toolbar";
        if (lower.contains("tooltip")) return "tooltip";
        if (lower.contains("menu")) return "menu";
        if (lower.contains("pane")) return "pane";
        if (lower.contains("panel")) return "panel";
        if (lower.contains("group")) return "groupbox";
        if (lower.contains("splitter")) return "splitter";
        if (lower.contains("link")) return "link";
        
        // Modern UI frameworks
        if (lower.contains("xaml") || lower.contains("wpf")) return "wpf-element";
        if (lower.contains("directui")) return "directui-element";
        if (lower.contains("netsuitablewindow")) return "net-window";
        
        return "control";
    }
    
    private static String escapeJson(String str) {
        if (str == null || str.isEmpty()) return "";
        return str.replace("\\", "\\\\")
                 .replace("\"", "\\\"")
                 .replace("\n", "\\n")
                 .replace("\r", "\\r")
                 .replace("\t", "\\t");
    }
}
