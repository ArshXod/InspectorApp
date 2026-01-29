package com.inspector.core;

import com.sun.jna.Library;
import com.sun.jna.Native;
import com.sun.jna.Pointer;
import com.sun.jna.platform.win32.WinDef.HWND;
import com.sun.jna.ptr.IntByReference;
import com.sun.jna.ptr.LongByReference;

import javax.accessibility.AccessibleContext;

/**
 * Cross-process Java Accessibility Bridge connector.
 * Uses WindowsAccessBridge DLL to access Java applications in other processes.
 */
public class AccessBridgeConnector {
    
    private static WindowsAccessBridge bridge;
    private static boolean initialized = false;
    private static String initError = null;
    
    // Windows Access Bridge library interface
    public interface WindowsAccessBridge extends Library {
        void Windows_run();
        boolean isJavaWindow(HWND hwnd);
        boolean getAccessibleContextFromHWND(HWND hwnd, LongByReference vmID, Pointer ac);
        String getVersionInfo(LongByReference vmID, Pointer ac);
    }
    
    /*
     * Initialize the Windows Access Bridge.
     * The WindowsAccessBridge DLL must be in the system path or java.library.path.
    */
    public static synchronized boolean initialize() {
        if (initialized) {
            return bridge != null;
        }
        
        initialized = true;
        
        try {
            // Try to load WindowsAccessBridge-64.dll or WindowsAccessBridge-32.dll
            String dllName = System.getProperty("os.arch").contains("64") 
                ? "WindowsAccessBridge-64" 
                : "WindowsAccessBridge-32";
            
            bridge = Native.load(dllName, WindowsAccessBridge.class);
            bridge.Windows_run();
            return true;
        } catch (UnsatisfiedLinkError e) {
            initError = "Windows Access Bridge not found: " + e.getMessage() + 
                       "\nThe Java Access Bridge must be installed and enabled.";
            return false;
        } catch (Exception e) {
            initError = "Failed to initialize Access Bridge: " + e.getMessage();
            return false;
        }
    }
    
    /**
     * Check if a window handle belongs to a Java application.
     */
    public static boolean isJavaWindow(Object hwnd) {
        if (!initialize()) {
            return false;
        }
        
        try {
            return bridge.isJavaWindow((HWND) hwnd);
        } catch (Exception e) {
            return false;
        }
    }
    
    /**
     * Create an HWND object from a window handle value.
     */
    public static Object createHWND(Object hwndValue) {
        try {
            if (hwndValue instanceof Long) {
                return new HWND(new Pointer((Long) hwndValue));
            } else {
                return new HWND(new Pointer(Long.parseLong(hwndValue.toString())));
            }
        } catch (Exception e) {
            throw new IllegalArgumentException("Cannot create HWND from: " + hwndValue, e);
        }
    }
    
    /**
     * Get the AccessibleContext from a window handle.
     * Note: This returns raw pointers that need to be wrapped properly.
     * This is a complex operation requiring deep JNA integration.
     */
    public static Object getAccessibleContextFromWindow(Object hwnd) {
        if (!initialize()) {
            throw new IllegalStateException("Access Bridge not initialized: " + initError);
        }
        
        try {
            LongByReference vmID = new LongByReference();
            Pointer acPointer = new Pointer(0);
            
            boolean success = bridge.getAccessibleContextFromHWND((HWND) hwnd, vmID, acPointer);
            
            if (!success) {
                return null;
            }
            
            // Note: Converting the raw pointer to an AccessibleContext is complex
            // and requires additional JNA magic or Java Access Bridge API integration
            throw new UnsupportedOperationException(
                "AccessibleContext extraction from external process requires " +
                "full Java Access Bridge API integration. " +
                "VM ID: " + vmID.getValue() + ", AC Pointer: " + acPointer
            );
        } catch (Exception e) {
            throw new RuntimeException("Error accessing Java window: " + e.getMessage(), e);
        }
    }
    
    /**
     * Get initialization error if any.
     */
    public static String getInitError() {
        return initError;
    }
    
    /**
     * Check if Access Bridge is available.
     */
    public static boolean isAvailable() {
        return initialize();
    }
}
