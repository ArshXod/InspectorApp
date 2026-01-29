package com.inspector.core;

import com.inspector.model.ElementData;

import javax.accessibility.*;
import java.awt.*;
import java.util.*;
import java.util.List;

public class UITreeExplorer {
    private final int maxDepth;
    private final boolean includeInvisible;
    private final Set<AccessibleContext> visitedContexts;
    private final Map<String, Integer> elementCounts;

    public UITreeExplorer(int maxDepth, boolean includeInvisible) {
        this.maxDepth = maxDepth;
        this.includeInvisible = includeInvisible;
        this.visitedContexts = Collections.newSetFromMap(new IdentityHashMap<>());
        this.elementCounts = new HashMap<>();
    }

    public ElementData explore(AccessibleContext rootContext) {
        visitedContexts.clear();
        elementCounts.clear();
        return exploreElement(rootContext, 0, "");
    }

    private ElementData exploreElement(AccessibleContext ac, int depth, String parentPath) {
        if (ac == null || depth > maxDepth || visitedContexts.contains(ac)) {
            return null;
        }

        visitedContexts.add(ac);
        ElementData element = new ElementData();

        // Extract basic properties
        element.setName(ac.getAccessibleName());
        
        AccessibleRole role = ac.getAccessibleRole();
        if (role != null) {
            element.setRole(role.toString());
            elementCounts.merge(role.toString(), 1, Integer::sum);
        }
        
        element.setDescription(ac.getAccessibleDescription());
        element.setIndexInParent(ac.getAccessibleIndexInParent());
        element.setChildrenCount(ac.getAccessibleChildrenCount());

        // Extract states
        AccessibleStateSet stateSet = ac.getAccessibleStateSet();
        if (stateSet != null) {
            List<String> states = new ArrayList<>();
            for (AccessibleState state : stateSet.toArray()) {
                states.add(state.toString());
            }
            element.setStates(states);
            
            // Check visibility
            if (!includeInvisible && !stateSet.contains(AccessibleState.VISIBLE)) {
                return element;
            }
        }

        // Extract component bounds
        AccessibleComponent comp = ac.getAccessibleComponent();
        if (comp != null) {
            Rectangle bounds = comp.getBounds();
            if (bounds != null) {
                element.setBounds(new ElementData.BoundsData(bounds));
            }
            
            // Get class name
            if (comp instanceof Component) {
                element.setClassName(comp.getClass().getName());
            }
        }

        // Extract text content
        AccessibleText text = ac.getAccessibleText();
        if (text != null) {
            try {
                int charCount = text.getCharCount();
                if (charCount > 0 && charCount < 10000) { // Limit to prevent huge strings
                    StringBuilder sb = new StringBuilder();
                    for (int i = 0; i < charCount; i++) {
                        String charAtIndex = text.getAtIndex(AccessibleText.CHARACTER, i);
                        if (charAtIndex != null) {
                            sb.append(charAtIndex);
                        }
                    }
                    if (sb.length() > 0) {
                        element.setText(sb.toString());
                    }
                }
            } catch (Exception e) {
                // Ignore text extraction errors
            }
        }

        // Extract value
        AccessibleValue value = ac.getAccessibleValue();
        if (value != null) {
            Number currentValue = value.getCurrentAccessibleValue();
            if (currentValue != null) {
                element.setValue(currentValue.toString());
            }
        }

        // Extract actions
        AccessibleAction action = ac.getAccessibleAction();
        if (action != null) {
            List<String> actions = new ArrayList<>();
            int actionCount = action.getAccessibleActionCount();
            for (int i = 0; i < actionCount; i++) {
                String actionDesc = action.getAccessibleActionDescription(i);
                if (actionDesc != null) {
                    actions.add(actionDesc);
                }
            }
            element.setActions(actions);
        }

        // Build path
        String currentPath = parentPath + "/" + (element.getName() != null ? element.getName() : element.getRole());
        element.setParentPath(parentPath);

        // Recursively explore children
        int childCount = ac.getAccessibleChildrenCount();
        if (childCount > 0 && depth < maxDepth) {
            List<ElementData> children = new ArrayList<>();
            for (int i = 0; i < childCount; i++) {
                Accessible child = ac.getAccessibleChild(i);
                if (child != null) {
                    AccessibleContext childContext = child.getAccessibleContext();
                    ElementData childElement = exploreElement(childContext, depth + 1, currentPath);
                    if (childElement != null) {
                        children.add(childElement);
                    }
                }
            }
            element.setChildren(children);
        }

        return element;
    }

    public Map<String, Integer> getElementCounts() {
        return new HashMap<>(elementCounts);
    }

    public int getTotalElements() {
        return visitedContexts.size();
    }
}
