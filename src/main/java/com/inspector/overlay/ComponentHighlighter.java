package com.inspector.overlay;

import javax.swing.*;
import java.awt.*;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.awt.event.MouseMotionAdapter;
import java.util.ArrayList;
import java.util.List;

/**
 * Creates a transparent overlay window that highlights UI components
 * by drawing red borders when hovering over them
 */
public class ComponentHighlighter {
    
    private JFrame overlayFrame;
    private OverlayPanel overlayPanel;
    private List<ComponentBounds> components;
    private ComponentBounds currentHighlight;
    private Timer mouseTracker;
    private boolean isActive = false;
    
    public ComponentHighlighter() {
        components = new ArrayList<>();
        initializeOverlay();
    }
    
    private void initializeOverlay() {
        // Suppress console window output
        System.setProperty("java.awt.headless", "false");
        
        // Create transparent, always-on-top overlay frame
        overlayFrame = new JFrame();
        overlayFrame.setUndecorated(true);
        overlayFrame.setAlwaysOnTop(true);
        overlayFrame.setFocusableWindowState(false);
        overlayFrame.setType(Window.Type.UTILITY);
        
        // Make window transparent
        overlayFrame.setBackground(new Color(0, 0, 0, 0));
        
        // Create custom panel for drawing
        overlayPanel = new OverlayPanel();
        overlayFrame.add(overlayPanel);
        
        // Set to full screen
        GraphicsDevice gd = GraphicsEnvironment.getLocalGraphicsEnvironment().getDefaultScreenDevice();
        DisplayMode dm = gd.getDisplayMode();
        overlayFrame.setBounds(0, 0, dm.getWidth(), dm.getHeight());
        
        // Add mouse motion listener to track hover
        overlayPanel.addMouseMotionListener(new MouseMotionAdapter() {
            @Override
            public void mouseMoved(MouseEvent e) {
                Point mousePos = e.getLocationOnScreen();
                updateHighlight(mousePos);
            }
        });
        
        // Allow click-through by making panel non-opaque and ignoring mouse events for interaction
        overlayPanel.setOpaque(false);
    }
    
    /**
     * Load component bounds from inspection JSON
     */
    public void loadComponents(List<ComponentBounds> componentList) {
        this.components = componentList;
        System.out.println("Loaded " + components.size() + " components for highlighting");
    }
    
    /**
     * Start the highlighting overlay
     */
    public void start() {
        if (isActive) return;
        
        isActive = true;
        overlayFrame.setVisible(true);
        
        // Start mouse tracking timer (checks mouse position every 50ms)
        mouseTracker = new Timer(50, e -> {
            try {
                Point mousePos = MouseInfo.getPointerInfo().getLocation();
                updateHighlight(mousePos);
            } catch (Exception ex) {
                // Ignore errors
            }
        });
        mouseTracker.start();
        
        System.out.println("Component highlighter started");
    }
    
    /**
     * Stop the highlighting overlay
     */
    public void stop() {
        if (!isActive) return;
        
        isActive = false;
        if (mouseTracker != null) {
            mouseTracker.stop();
        }
        overlayFrame.setVisible(false);
        currentHighlight = null;
        overlayPanel.repaint();
        
        System.out.println("Component highlighter stopped");
    }
    
    /**
     * Update which component should be highlighted based on mouse position
     */
    private void updateHighlight(Point mousePos) {
        ComponentBounds newHighlight = null;
        
        // Find the smallest component that contains the mouse position
        // (to handle nested components, we want the most specific one)
        int smallestArea = Integer.MAX_VALUE;
        
        for (ComponentBounds comp : components) {
            if (comp.contains(mousePos)) {
                int area = comp.getArea();
                if (area < smallestArea) {
                    smallestArea = area;
                    newHighlight = comp;
                }
            }
        }
        
        // Only repaint if highlight changed
        if (newHighlight != currentHighlight) {
            currentHighlight = newHighlight;
            overlayPanel.repaint();
        }
    }
    
    /**
     * Custom panel that draws red borders around highlighted component
     */
    private class OverlayPanel extends JPanel {
        @Override
        protected void paintComponent(Graphics g) {
            super.paintComponent(g);
            
            if (currentHighlight != null) {
                Graphics2D g2d = (Graphics2D) g.create();
                
                // Enable antialiasing for smooth borders
                g2d.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
                
                // Draw red border with glow effect
                Rectangle bounds = currentHighlight.getBounds();
                
                // Draw outer glow (semi-transparent)
                g2d.setColor(new Color(255, 0, 0, 80));
                g2d.setStroke(new BasicStroke(5));
                g2d.drawRect(bounds.x - 2, bounds.y - 2, bounds.width + 4, bounds.height + 4);
                
                // Draw main border (solid red)
                g2d.setColor(new Color(255, 0, 0, 255));
                g2d.setStroke(new BasicStroke(3));
                g2d.drawRect(bounds.x, bounds.y, bounds.width, bounds.height);
                
                // Draw component info tooltip
                drawTooltip(g2d, bounds);
                
                g2d.dispose();
            }
        }
        
        private void drawTooltip(Graphics2D g2d, Rectangle bounds) {
            if (currentHighlight.getName() == null || currentHighlight.getName().equals("null")) {
                return;
            }
            
            String text = currentHighlight.getName() + " (" + currentHighlight.getRole() + ")";
            FontMetrics fm = g2d.getFontMetrics();
            int textWidth = fm.stringWidth(text);
            int textHeight = fm.getHeight();
            
            // Position tooltip above component
            int tooltipX = bounds.x;
            int tooltipY = bounds.y - textHeight - 8;
            
            // Keep tooltip on screen
            if (tooltipY < 0) tooltipY = bounds.y + bounds.height + 5;
            if (tooltipX + textWidth + 10 > getWidth()) tooltipX = getWidth() - textWidth - 10;
            
            // Draw tooltip background
            g2d.setColor(new Color(50, 50, 50, 220));
            g2d.fillRoundRect(tooltipX, tooltipY, textWidth + 10, textHeight + 4, 5, 5);
            
            // Draw tooltip border
            g2d.setColor(new Color(255, 0, 0, 255));
            g2d.setStroke(new BasicStroke(1));
            g2d.drawRoundRect(tooltipX, tooltipY, textWidth + 10, textHeight + 4, 5, 5);
            
            // Draw text
            g2d.setColor(Color.WHITE);
            g2d.drawString(text, tooltipX + 5, tooltipY + textHeight - 2);
        }
    }
    
    /**
     * Represents bounds and metadata of a UI component
     */
    public static class ComponentBounds {
        private final Rectangle bounds;
        private final String name;
        private final String role;
        
        public ComponentBounds(int x, int y, int width, int height, String name, String role) {
            this.bounds = new Rectangle(x, y, width, height);
            this.name = name;
            this.role = role;
        }
        
        public boolean contains(Point p) {
            return bounds.contains(p);
        }
        
        public Rectangle getBounds() {
            return bounds;
        }
        
        public int getArea() {
            return bounds.width * bounds.height;
        }
        
        public String getName() {
            return name;
        }
        
        public String getRole() {
            return role;
        }
    }
}
