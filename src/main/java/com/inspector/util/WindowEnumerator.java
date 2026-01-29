package com.inspector.util;

import com.sun.jna.Native;
import com.sun.jna.platform.win32.User32;
import com.sun.jna.platform.win32.WinDef.HWND;
import com.sun.jna.platform.win32.WinUser;
import com.sun.jna.ptr.IntByReference;

import java.util.ArrayList;
import java.util.List;

public class WindowEnumerator {
    
    public static class WindowInfo {
        public HWND hwnd;
        public String title;
        public int pid;
        
        public WindowInfo(HWND hwnd, String title, int pid) {
            this.hwnd = hwnd;
            this.title = title;
            this.pid = pid;
        }
    }
    
    public static List<WindowInfo> listAllWindows() {
        List<WindowInfo> windows = new ArrayList<>();
        User32.INSTANCE.EnumWindows((hwnd, data) -> {
            if (User32.INSTANCE.IsWindowVisible(hwnd)) {
                char[] windowText = new char[512];
                User32.INSTANCE.GetWindowText(hwnd, windowText, 512);
                String title = Native.toString(windowText);
                
                if (!title.isEmpty()) {
                    IntByReference pid = new IntByReference();
                    User32.INSTANCE.GetWindowThreadProcessId(hwnd, pid);
                    windows.add(new WindowInfo(hwnd, title, pid.getValue()));
                }
            }
            return true;
        }, null);
        return windows;
    }
    
    public static WindowInfo findWindowByPid(int targetPid) {
        List<WindowInfo> windows = listAllWindows();
        for (WindowInfo window : windows) {
            if (window.pid == targetPid) {
                return window;
            }
        }
        return null;
    }
    
    public static WindowInfo findWindowByTitle(String titlePattern) {
        List<WindowInfo> windows = listAllWindows();
        for (WindowInfo window : windows) {
            if (window.title.contains(titlePattern)) {
                return window;
            }
        }
        return null;
    }
    
    public static List<WindowInfo> listJavaWindows() {
        // This is a heuristic - Java windows often have specific characteristics
        List<WindowInfo> allWindows = listAllWindows();
        List<WindowInfo> javaWindows = new ArrayList<>();
        
        for (WindowInfo window : allWindows) {
            // Filter based on title or other characteristics
            // This is a simplified version - more sophisticated detection needed
            if (window.title.length() > 0) {
                javaWindows.add(window);
            }
        }
        return javaWindows;
    }
}
