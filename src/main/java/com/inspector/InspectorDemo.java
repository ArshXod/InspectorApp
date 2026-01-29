package com.inspector;

import com.inspector.core.UITreeExplorer;
import com.inspector.model.ElementData;
import com.inspector.model.ExplorationResult;
import com.inspector.util.JsonExporter;

import javax.swing.*;
import java.awt.*;

/**
 * Integration demo that creates a Swing window and inspects it.
 * This demonstrates the inspector working within the same JVM process.
 */
public class InspectorDemo {
    
    public static void main(String[] args) throws Exception {
        System.out.println("JavaInspector Demo - Same Process Inspection");
        System.out.println("=============================================\n");
        
        // Create a simple test window
        JFrame frame = createTestWindow();
        frame.setVisible(true);
        
        // Give the window time to render
        Thread.sleep(500);
        
        // Inspect the window
        System.out.println("Inspecting window: " + frame.getTitle());
        UITreeExplorer explorer = new UITreeExplorer(10, false);
        ElementData root = explorer.explore(frame.getAccessibleContext());
        
        ExplorationResult result = new ExplorationResult();
        ExplorationResult.ApplicationInfo appInfo = new ExplorationResult.ApplicationInfo();
        appInfo.setTitle(frame.getTitle());
        appInfo.setPid("DEMO");
        result.setApplication(appInfo);
        result.setUiTree(root);
        
        // Export to JSON
        JsonExporter exporter = new JsonExporter();
        String json = exporter.exportToString(result);
        System.out.println("\nJSON Result Preview (first 500 chars):");
        System.out.println(json.substring(0, Math.min(500, json.length())) + "...\n");
        
        // Print summary
        int elementCount = countElements(root);
        System.out.println("================================");
        System.out.println("Total elements found: " + elementCount);
        System.out.println("Root element: " + root.getRole());
        System.out.println("Root name: " + root.getName());
        System.out.println("Children: " + (root.getChildren() != null ? root.getChildren().size() : 0));
        
        // Save to file
        exporter.exportToFile(result, "demo-output.json");
        System.out.println("\nFull results saved to: demo-output.json");
        
        // Close window
        frame.dispose();
        System.out.println("Demo completed successfully!");
    }
    
    private static JFrame createTestWindow() {
        JFrame frame = new JFrame("Demo Application");
        frame.setDefaultCloseOperation(JFrame.DISPOSE_ON_CLOSE);
        frame.setSize(400, 300);
        
        // Create a panel with various components
        JPanel panel = new JPanel(new BorderLayout());
        
        // Top panel with label
        JPanel topPanel = new JPanel();
        topPanel.add(new JLabel("Welcome to Demo App"));
        panel.add(topPanel, BorderLayout.NORTH);
        
        // Center panel with buttons
        JPanel centerPanel = new JPanel(new GridLayout(2, 2, 5, 5));
        centerPanel.add(new JButton("Button 1"));
        centerPanel.add(new JButton("Button 2"));
        centerPanel.add(new JTextField("Text Field"));
        centerPanel.add(new JCheckBox("Check Box"));
        panel.add(centerPanel, BorderLayout.CENTER);
        
        // Bottom panel with text area
        JTextArea textArea = new JTextArea("Sample text area\nLine 2");
        JScrollPane scrollPane = new JScrollPane(textArea);
        panel.add(scrollPane, BorderLayout.SOUTH);
        
        frame.setContentPane(panel);
        return frame;
    }
    
    private static int countElements(ElementData element) {
        int count = 1;
        if (element.getChildren() != null) {
            for (ElementData child : element.getChildren()) {
                count += countElements(child);
            }
        }
        return count;
    }
}
