package com.inspector.core;

import com.inspector.model.ElementData;
import com.inspector.model.ExplorationResult;

import java.util.Map;

public class ElementSerializer {
    
    public static ExplorationResult createResult(ElementData rootElement, String pid, String title, 
                                                  Map<String, Integer> elementCounts, int totalElements,
                                                  long durationMs) {
        ExplorationResult result = new ExplorationResult();
        
        // Set application info
        ExplorationResult.ApplicationInfo appInfo = new ExplorationResult.ApplicationInfo();
        appInfo.setPid(pid);
        appInfo.setTitle(title);
        appInfo.setToolkit(detectToolkit(rootElement));
        result.setApplication(appInfo);
        
        // Set statistics
        Map<String, Object> stats = result.getStatistics();
        stats.put("total_elements", totalElements);
        stats.put("duration_ms", durationMs);
        stats.put("exploration_depth", calculateMaxDepth(rootElement, 0));
        
        // Add element counts
        for (Map.Entry<String, Integer> entry : elementCounts.entrySet()) {
            stats.put(entry.getKey().toLowerCase().replace(" ", "_"), entry.getValue());
        }
        
        // Set UI tree
        result.setUiTree(rootElement);
        
        return result;
    }
    
    private static String detectToolkit(ElementData element) {
        if (element == null || element.getClassName() == null) {
            return "Unknown";
        }
        
        String className = element.getClassName();
        if (className.contains("javax.swing") || className.contains("swing")) {
            return "Swing";
        } else if (className.contains("javafx")) {
            return "JavaFX";
        } else if (className.contains("java.awt")) {
            return "AWT";
        }
        return "Unknown";
    }
    
    private static int calculateMaxDepth(ElementData element, int currentDepth) {
        if (element == null || element.getChildren() == null || element.getChildren().isEmpty()) {
            return currentDepth;
        }
        
        int maxChildDepth = currentDepth;
        for (ElementData child : element.getChildren()) {
            int childDepth = calculateMaxDepth(child, currentDepth + 1);
            maxChildDepth = Math.max(maxChildDepth, childDepth);
        }
        
        return maxChildDepth;
    }
}
