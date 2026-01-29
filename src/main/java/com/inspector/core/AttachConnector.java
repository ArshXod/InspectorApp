package com.inspector.core;

import com.sun.tools.attach.VirtualMachine;
import com.sun.tools.attach.VirtualMachineDescriptor;
import com.sun.tools.attach.AttachNotSupportedException;
import com.sun.tools.attach.AgentLoadException;
import com.sun.tools.attach.AgentInitializationException;

import java.io.IOException;
import java.util.List;

/**
 * Connects to external Java processes using the Attach API.
 * This allows inspecting Java applications running in different JVM processes.
 */
public class AttachConnector {
    
    /**
     * Check if a PID corresponds to a Java process.
     */
    public static boolean isJavaProcess(String pid) {
        List<VirtualMachineDescriptor> vms = VirtualMachine.list();
        for (VirtualMachineDescriptor vmd : vms) {
            if (vmd.id().equals(pid)) {
                return true;
            }
        }
        return false;
    }
    
    /**
     * List all Java processes.
     */
    public static List<VirtualMachineDescriptor> listJavaProcesses() {
        return VirtualMachine.list();
    }
    
    /**
     * Attach to a running JVM and load inspection agent.
     * The agent will expose the accessibility tree via a socket or file.
     */
    public static String attachAndInspect(String pid, String agentJarPath, String agentArgs) throws IOException, AttachNotSupportedException, AgentLoadException, AgentInitializationException {
        VirtualMachine vm = null;
        try {
            vm = VirtualMachine.attach(pid);
            
            // Load our agent into the target JVM
            vm.loadAgent(agentJarPath, agentArgs);
            
            // Extract output file path from agent args
            String outputFilePath = "inspector-agent-output.json";
            if (agentArgs != null && agentArgs.startsWith("output:")) {
                outputFilePath = agentArgs.substring(7);
            }
            
            // Wait for agent to write output file (max 10 seconds)
            java.io.File outputFile = new java.io.File(outputFilePath);
            int maxWaitMs = 10000;
            int waitedMs = 0;
            int pollIntervalMs = 100;
            
            while (!outputFile.exists() && waitedMs < maxWaitMs) {
                try {
                    Thread.sleep(pollIntervalMs);
                    waitedMs += pollIntervalMs;
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                    break;
                }
            }
            
            if (outputFile.exists()) {
                return "Agent loaded successfully into PID " + pid;
            } else {
                return "Agent loaded but did not produce output within " + (maxWaitMs/1000) + " seconds. Check target process console for errors.";
            }
            
        } finally {
            if (vm != null) {
                vm.detach();
            }
        }
    }
    
    /**
     * Get system properties from target JVM.
     */
    public static java.util.Properties getSystemProperties(String pid) throws IOException, AttachNotSupportedException {
        VirtualMachine vm = null;
        try {
            vm = VirtualMachine.attach(pid);
            return vm.getSystemProperties();
        } finally {
            if (vm != null) {
                vm.detach();
            }
        }
    }
}
