package com.inspector.util;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.inspector.model.ExplorationResult;

import java.io.File;
import java.io.IOException;

public class JsonExporter {
    private final ObjectMapper mapper;
    
    public JsonExporter() {
        this.mapper = new ObjectMapper();
        this.mapper.enable(SerializationFeature.INDENT_OUTPUT);
    }
    
    public void exportToFile(ExplorationResult result, String filePath) throws IOException {
        File file = new File(filePath);
        mapper.writeValue(file, result);
    }
    
    public String exportToString(ExplorationResult result) throws IOException {
        return mapper.writeValueAsString(result);
    }
}
