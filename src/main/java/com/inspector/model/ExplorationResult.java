package com.inspector.model;

import com.fasterxml.jackson.annotation.JsonProperty;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.HashMap;
import java.util.Map;

public class ExplorationResult {
    private ApplicationInfo application;
    private Map<String, Object> statistics;
    @JsonProperty("ui_tree")
    private ElementData uiTree;

    public ExplorationResult() {
        this.statistics = new HashMap<>();
    }

    public ApplicationInfo getApplication() { return application; }
    public void setApplication(ApplicationInfo application) { this.application = application; }

    public Map<String, Object> getStatistics() { return statistics; }
    public void setStatistics(Map<String, Object> statistics) { this.statistics = statistics; }

    public ElementData getUiTree() { return uiTree; }
    public void setUiTree(ElementData uiTree) { this.uiTree = uiTree; }

    public static class ApplicationInfo {
        private String pid;
        private String title;
        private String timestamp;
        @JsonProperty("java_version")
        private String javaVersion;
        private String toolkit;

        public ApplicationInfo() {
            this.timestamp = LocalDateTime.now().format(DateTimeFormatter.ISO_LOCAL_DATE_TIME);
            this.javaVersion = System.getProperty("java.version");
        }

        public String getPid() { return pid; }
        public void setPid(String pid) { this.pid = pid; }

        public String getTitle() { return title; }
        public void setTitle(String title) { this.title = title; }

        public String getTimestamp() { return timestamp; }
        public void setTimestamp(String timestamp) { this.timestamp = timestamp; }

        public String getJavaVersion() { return javaVersion; }
        public void setJavaVersion(String javaVersion) { this.javaVersion = javaVersion; }

        public String getToolkit() { return toolkit; }
        public void setToolkit(String toolkit) { this.toolkit = toolkit; }
    }
}
