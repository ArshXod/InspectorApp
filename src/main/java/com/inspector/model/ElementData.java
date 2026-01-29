package com.inspector.model;

import com.fasterxml.jackson.annotation.JsonInclude;
import java.awt.Rectangle;
import java.util.ArrayList;
import java.util.List;

@JsonInclude(JsonInclude.Include.NON_NULL)
public class ElementData {
    private String name;
    private String role;
    private String description;
    private List<String> states;
    private BoundsData bounds;
    private Integer indexInParent;
    private Integer childrenCount;
    private String text;
    private String value;
    private List<String> actions;
    private String className;
    private String parentPath;
    private List<ElementData> children;

    public ElementData() {
        this.states = new ArrayList<>();
        this.actions = new ArrayList<>();
        this.children = new ArrayList<>();
    }

    // Getters and Setters
    public String getName() { return name; }
    public void setName(String name) { this.name = name; }

    public String getRole() { return role; }
    public void setRole(String role) { this.role = role; }

    public String getDescription() { return description; }
    public void setDescription(String description) { this.description = description; }

    public List<String> getStates() { return states; }
    public void setStates(List<String> states) { this.states = states; }

    public BoundsData getBounds() { return bounds; }
    public void setBounds(BoundsData bounds) { this.bounds = bounds; }

    public Integer getIndexInParent() { return indexInParent; }
    public void setIndexInParent(Integer indexInParent) { this.indexInParent = indexInParent; }

    public Integer getChildrenCount() { return childrenCount; }
    public void setChildrenCount(Integer childrenCount) { this.childrenCount = childrenCount; }

    public String getText() { return text; }
    public void setText(String text) { this.text = text; }

    public String getValue() { return value; }
    public void setValue(String value) { this.value = value; }

    public List<String> getActions() { return actions; }
    public void setActions(List<String> actions) { this.actions = actions; }

    public String getClassName() { return className; }
    public void setClassName(String className) { this.className = className; }

    public String getParentPath() { return parentPath; }
    public void setParentPath(String parentPath) { this.parentPath = parentPath; }

    public List<ElementData> getChildren() { return children; }
    public void setChildren(List<ElementData> children) { this.children = children; }

    public static class BoundsData {
        private int x;
        private int y;
        private int width;
        private int height;

        public BoundsData() {}

        public BoundsData(Rectangle rect) {
            if (rect != null) {
                this.x = rect.x;
                this.y = rect.y;
                this.width = rect.width;
                this.height = rect.height;
            }
        }

        public int getX() { return x; }
        public void setX(int x) { this.x = x; }

        public int getY() { return y; }
        public void setY(int y) { this.y = y; }

        public int getWidth() { return width; }
        public void setWidth(int width) { this.width = width; }

        public int getHeight() { return height; }
        public void setHeight(int height) { this.height = height; }
    }
}
