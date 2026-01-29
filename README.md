# Java UI Inspector

Hybrid UI inspection tool with **modern GUI** and CLI support for both Java and non-Java applications.

## Features
- **🎨 Modern GUI Application**: Stylish JavaFX interface with dark mode support
- **📋 Process Manager**: Task-manager-like view of all running applications
- **🔍 Smart Search & Filters**: Find processes instantly, filter by Java/Non-Java
- **⚡ Real-time Updates**: Auto-refresh process list every 5 seconds
- **💾 Inspection History**: Track all inspections with timestamps
- **📊 Live Preview**: View JSON results immediately in the UI
- **Java Applications**: Deep inspection via Java Accessibility API
- **Non-Java Applications**: Windows UI Automation fallback
- **🌙 Dark Mode**: Easy on the eyes for long inspection sessions

## 🚀 How to Run

### Option 1: GUI Application (Easiest)
```powershell
mvn javafx:run
```

That's it! The GUI will launch showing all running applications.

**What to do:**
1. Find your application in the list (or use search)
2. Click on it to select
3. Click "Inspect Selected Process" button
4. Choose where to save the results
5. Done! View the JSON in the preview pane

### Option 2: Command Line
```powershell
# Set environment variables
$env:JAVA_HOME = // Your java Path here
$env:PATH = // Your maven.bin path here + $env:PATH

# Build first (only needed once)
mvn clean package

# Now You can run
mvn javafx:run

# Find the process ID
Get-Process | Where-Object {$_.MainWindowTitle} | Format-Table Id, Name, MainWindowTitle

# Run inspection
java -jar target\JavaInspector-1.0.0-jar-with-dependencies.jar --pid <PID>

# Results saved to: inspector-agent-output.json
```

### GUI Features
- **Search**: Type process name, PID, or window title in the search box
- **Filter**: Choose "All", "Java Only", or "Non-Java Only" from dropdown
- **Auto-Refresh**: Enable checkbox to auto-update process list every 5 seconds
- **History**: View all past inspections, click "Open File" to view results
- **Dark Mode**: Toggle the 🌙 button for dark theme
- **Preview**: See JSON results immediately after inspection

---

## 💡 Quick Examples

### Example 1: Inspect Notepad
```powershell
# Run the GUI
mvn javafx:run

# Then in the GUI:
# 1. Search for "notepad" in the search box
# 2. Click on Notepad process
# 3. Click "Inspect Selected Process"
# 4. Choose save location
# 5. View results in preview pane!
```

### Example 2: Inspect Java Application
```powershell
# Launch GUI
mvn javafx:run

# Filter to "Java Only" from dropdown
# Select your Java app from the list
# Click "Inspect Selected Process"
# Results show complete UI tree with all properties
```

### Example 3: Command Line Inspection
```powershell
# Find process PID
Get-Process notepad | Select-Object Id, ProcessName

# Inspect (replace 12345 with actual PID)
mvn clean package
java -jar target\JavaInspector-1.0.0-jar-with-dependencies.jar --pid 12345

# View results
Get-Content inspector-agent-output.json
```

## Output Format

### Java Application Output
```json
{
  "timestamp": "...",
  "targetPID": "12345",
  "windows": [{
    "title": "My Java App",
    "class": "javax.swing.JFrame",
    "uiTree": {
      "role": "push button",
      "name": "Submit",
      "className": "javax.swing.JButton",
      "bounds": {"x": 100, "y": 50, "width": 80, "height": 30},
      "indexInParent": 3,
      "states": ["enabled", "visible", "focusable"],
      "actions": ["click"],
      "text": null,
      "value": {"current": 0, "min": 0, "max": 1}
    }
  }]
}
```

### Non-Java Application Output
```json
{
  "timestamp": "...",
  "targetPID": "67890",
  "inspectionMethod": "Windows UIA",
  "windows": [{
    "title": "Notepad",
    "class": "Notepad",
    "bounds": {"x": 80, "y": 80, "width": 1035, "height": 693},
    "uiTree": {
      "role": "window",
      "name": "Notepad",
      "className": "Notepad",
      "children": [
        {
          "role": "control",
          "name": "Text Editor",
          "className": "NotepadTextBox",
          "bounds": {"x": 88, "y": 173, "width": 998, "height": 571},
          "states": ["visible", "enabled"],
          "hwnd": "native@0x1a2b3c"
        }
      ]
    }
  }]
}
```

## How It Works

1. **Process Detection**: Checks if target PID is a Java process
2. **Java Path**: 
   - Connects via Java Attach API
   - Injects agent into target JVM
   - Agent explores UI using Accessibility API
   - Exports detailed UI tree with all properties
3. **Non-Java Path**:
   - Uses Windows UI Automation
   - Enumerates windows and child controls
   - Exports UI tree with window class names and bounds

By : Arshdeep Singh, DXR R&D Testing and Automation
