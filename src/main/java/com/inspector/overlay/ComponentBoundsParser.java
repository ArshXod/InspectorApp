package com.inspector.overlay;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

/**
 * Parses inspection JSON and extracts component bounds for highlighting
 */
public class ComponentBoundsParser {
    
    private static final ObjectMapper mapper = new ObjectMapper();
    
    /**
     * Parse inspection JSON file and extract all component bounds
     */
    public static List<ComponentHighlighter.ComponentBounds> parseFromFile(String filePath) throws IOException {
        File jsonFile = new File(filePath);
        if (!jsonFile.exists()) {
            throw new IOException("Inspection file not found: " + filePath);
        }
        
        JsonNode root = mapper.readTree(jsonFile);
        List<ComponentHighlighter.ComponentBounds> components = new ArrayList<>();
        
        // Navigate to windows array
        JsonNode windows = root.get("windows");
        if (windows != null && windows.isArray()) {
            for (JsonNode window : windows) {
                JsonNode uiTree = window.get("uiTree");
                if (uiTree != null) {
                    // Get window's screen position (the frame bounds are in screen coordinates)
                    JsonNode frameBounds = uiTree.get("bounds");
                    int windowX = 0;
                    int windowY = 0;
                    if (frameBounds != null) {
                        windowX = frameBounds.has("x") ? frameBounds.get("x").asInt() : 0;
                        windowY = frameBounds.has("y") ? frameBounds.get("y").asInt() : 0;
                        System.out.println("Window position: (" + windowX + ", " + windowY + ")");
                    }
                    
                    // Recursively extract all components with window offset
                    extractComponents(uiTree, components, windowX, windowY, true);
                }
            }
        }
        
        return components;
    }
    
    /**
     * Recursively extract component bounds from UI tree
     * @param node Current UI tree node
     * @param components List to accumulate component bounds
     * @param windowX Window's X position on screen
     * @param windowY Window's Y position on screen
     * @param isRoot Whether this is the root frame node (don't add offset to root)
     */
    private static void extractComponents(JsonNode node, List<ComponentHighlighter.ComponentBounds> components, 
                                         int windowX, int windowY, boolean isRoot) {
        // Extract current node's bounds
        JsonNode boundsNode = node.get("bounds");
        if (boundsNode != null) {
            int x = boundsNode.has("x") ? boundsNode.get("x").asInt() : 0;
            int y = boundsNode.has("y") ? boundsNode.get("y").asInt() : 0;
            int width = boundsNode.has("width") ? boundsNode.get("width").asInt() : 0;
            int height = boundsNode.has("height") ? boundsNode.get("height").asInt() : 0;
            
            // Only add components with valid bounds (positive dimensions)
            if (width > 0 && height > 0) {
                String name = node.has("name") ? node.get("name").asText() : "null";
                String role = node.has("role") ? node.get("role").asText() : "unknown";
                
                // For child components, add window offset to convert to screen coordinates
                // Root frame already has screen coordinates, so don't offset it
                int screenX = isRoot ? x : (x + windowX);
                int screenY = isRoot ? y : (y + windowY);
                
                components.add(new ComponentHighlighter.ComponentBounds(screenX, screenY, width, height, name, role));
            }
        }
        
        // Recursively process children (all children need offset)
        JsonNode children = node.get("children");
        if (children != null && children.isArray()) {
            for (JsonNode child : children) {
                extractComponents(child, components, windowX, windowY, false);
            }
        }
    }
}
