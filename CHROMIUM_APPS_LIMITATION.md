# Chromium/Electron Apps Inspection Limitation

## Issue
When inspecting **Chromium/Electron-based applications** (Microsoft Teams, VS Code, Discord, Slack, etc.), the Windows UIA inspector only shows **window containers** with empty names, not the actual UI content.

## Why This Happens

### Technical Explanation
1. **Traditional Windows Apps** (Notepad, Calculator, classic Win32 apps):
   - Use native Windows controls (buttons, textboxes, labels)
   - Each control is a separate HWND with accessible properties
   - Windows APIs can query names, roles, and states directly

2. **Chromium/Electron Apps** (Teams, VS Code, Discord):
   - Built on web technologies (HTML/CSS/JavaScript)
   - Render UI in a **browser canvas** (like a webpage)
   - UI elements are drawn pixels, not native Windows controls
   - Only container windows are visible to Windows APIs

### What You See
```json
{
  "role": "control",
  "name": "",                    // ❌ EMPTY - UI content not accessible
  "className": "Chrome_WidgetWin_0",
  "bounds": { "x": -32000, "y": -32000, "width": 1920, "height": 1020 }
}
```

The negative X/Y coordinates (-32000) indicate **minimized or off-screen windows**.

## Solutions

### Option 1: Use Chrome DevTools Protocol (CDP)
For Chromium/Electron apps, use the **Chrome DevTools Protocol** to inspect the actual DOM:

```bash
# Start application with remote debugging
"C:\Program Files\Microsoft Teams\current\Teams.exe" --remote-debugging-port=9222

# Connect to http://localhost:9222 to inspect full UI tree
```

### Option 2: Use Accessibility Inspector Tools
- **Accessibility Insights for Windows**: https://accessibilityinsights.io/
- **Inspect.exe** (Windows SDK): Proper UI Automation with MSAA/UIA support
- **Chrome DevTools**: Built-in DOM inspector

### Option 3: Native App Testing
This tool works perfectly with **traditional Windows applications**:
- Notepad, Calculator, Paint
- Microsoft Office (Word, Excel, PowerPoint)
- Windows Explorer, Control Panel
- Any Win32/WPF/.NET application

## Example: Good vs Limited Output

### ✅ Traditional App (Notepad)
```json
{
  "role": "button",
  "name": "Save",
  "className": "Button",
  "states": ["visible", "enabled"]
}
```

### ❌ Chromium App (Teams)
```json
{
  "role": "control",
  "name": "",              // No accessible name
  "className": "Chrome_WidgetWin_0",
  "states": ["visible", "enabled"]
}
```

## Why Can't This Be Fixed?

The fundamental issue is **architectural**:
- Chromium renders UI in a **Skia graphics canvas**
- Content is **painted as pixels**, not structured controls
- Windows APIs see only the canvas container, not individual buttons/text
- Accessibility info requires **specialized Chrome protocols** (CDP, Chrome Accessibility API)

This is not a bug in the inspector - it's how modern web-based applications work.

## Recommendation

For **enterprise UI testing** of web-based apps:
1. Use **Selenium/Playwright** for browser automation
2. Use **Chrome DevTools Protocol** for Electron apps
3. Use **this tool (Phlips Inspector)** for traditional Windows applications
4. Use **specialized accessibility tools** (Accessibility Insights, Inspect.exe)

---

**Updated**: November 25, 2025  
**Affects**: Microsoft Teams, VS Code, Discord, Slack, Spotify, and all Chromium/Electron applications
