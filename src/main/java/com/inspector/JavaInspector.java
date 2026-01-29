package com.inspector;

import com.inspector.core.ApplicationConnector;
import com.inspector.core.ElementSerializer;
import com.inspector.core.UITreeExplorer;
import com.inspector.model.ElementData;
import com.inspector.model.ExplorationResult;
import com.inspector.util.JsonExporter;
import com.inspector.util.WindowEnumerator;

import javax.accessibility.AccessibleContext;
import java.util.List;

public class JavaInspector {
    
    private static final int DEFAULT_MAX_DEPTH = 20;
    
    public static void main(String[] args) {
        if (args.length == 0) {
            printUsage();
            return;
        }
        
        try {
            CommandLineOptions options = parseArguments(args);
            
            if (options.list) {
                listApplications();
                return;
            }
            
            if (options.pid == null && options.title == null) {
                System.err.println("Error: Either --pid or --title must be specified");
                printUsage();
                System.exit(1);
            }
            
            inspectApplication(options);
            
        } catch (Exception e) {
            System.err.println("Error: " + e.getMessage());
            if (options != null && options.verbose) {
                e.printStackTrace();
            }
            System.exit(1);
        }
    }
    
    private static CommandLineOptions options;
    
    private static CommandLineOptions parseArguments(String[] args) {
        CommandLineOptions opts = new CommandLineOptions();
        
        for (int i = 0; i < args.length; i++) {
            switch (args[i]) {
                case "--pid":
                    if (i + 1 < args.length) {
                        opts.pid = args[++i];
                    }
                    break;
                case "--title":
                    if (i + 1 < args.length) {
                        opts.title = args[++i];
                    }
                    break;
                case "--output":
                case "-o":
                    if (i + 1 < args.length) {
                        opts.output = args[++i];
                    }
                    break;
                case "--max-depth":
                    if (i + 1 < args.length) {
                        opts.maxDepth = Integer.parseInt(args[++i]);
                    }
                    break;
                case "--include-invisible":
                    opts.includeInvisible = true;
                    break;
                case "--list":
                    opts.list = true;
                    break;
                case "--verbose":
                case "-v":
                    opts.verbose = true;
                    break;
                case "--help":
                case "-h":
                    printUsage();
                    System.exit(0);
                    break;
                default:
                    System.err.println("Unknown option: " + args[i]);
                    printUsage();
                    System.exit(1);
            }
        }
        
        return opts;
    }
    
    private static void listApplications() {
        System.out.println("Listing all visible windows:\n");
        System.out.println(String.format("%-8s %-50s", "PID", "Title"));
        System.out.println("----------------------------------------------------------------");
        
        List<WindowEnumerator.WindowInfo> windows = ApplicationConnector.listAllWindows();
        for (WindowEnumerator.WindowInfo window : windows) {
            System.out.println(String.format("%-8d %-50s", window.pid, window.title));
        }
        
        System.out.println("\nTotal windows: " + windows.size());
    }
    
    private static void inspectApplication(CommandLineOptions options) throws Exception {
        System.out.println("Java UI Inspector v1.0.0");
        System.out.println("========================================\n");
        
        // Connect to application
        ApplicationConnector.ConnectionResult connection;
        if (options.pid != null) {
            int pid = Integer.parseInt(options.pid);
            System.out.println("Connecting to PID: " + pid);
            connection = ApplicationConnector.connectByPid(pid);
        } else {
            System.out.println("Connecting to window with title: " + options.title);
            connection = ApplicationConnector.connectByTitle(options.title);
        }
        
        if (connection.error != null) {
            throw new Exception(connection.error);
        }
        
        System.out.println("Connected to: " + connection.title);
        System.out.println("PID: " + connection.pid);
        System.out.println("\nExploring UI tree...");
        
        // Explore UI tree
        long startTime = System.currentTimeMillis();
        UITreeExplorer explorer = new UITreeExplorer(options.maxDepth, options.includeInvisible);
        ElementData rootElement = explorer.explore(connection.rootContext);
        long duration = System.currentTimeMillis() - startTime;
        
        System.out.println("Exploration complete!");
        System.out.println("Total elements found: " + explorer.getTotalElements());
        System.out.println("Duration: " + duration + "ms");
        
        // Create result
        ExplorationResult result = ElementSerializer.createResult(
            rootElement,
            String.valueOf(connection.pid),
            connection.title,
            explorer.getElementCounts(),
            explorer.getTotalElements(),
            duration
        );
        
        // Export to JSON
        JsonExporter exporter = new JsonExporter();
        if (options.output != null) {
            exporter.exportToFile(result, options.output);
            System.out.println("\nResults exported to: " + options.output);
        } else {
            String json = exporter.exportToString(result);
            System.out.println("\nJSON Output:");
            System.out.println(json);
        }
        
        // Print summary
        System.out.println("\nElement Summary:");
        explorer.getElementCounts().forEach((role, count) -> 
            System.out.println("  " + role + ": " + count)
        );
    }
    
    private static void printUsage() {
        System.out.println("Java UI Inspector - Explore Java application UI structure\n");
        System.out.println("Usage:");
        System.out.println("  java -jar JavaInspector.jar [options]\n");
        System.out.println("Options:");
        System.out.println("  --pid <pid>              Connect to application by process ID");
        System.out.println("  --title <title>          Connect to application by window title (partial match)");
        System.out.println("  --output <file>          Output JSON file path (default: print to console)");
        System.out.println("  --max-depth <n>          Maximum exploration depth (default: 20)");
        System.out.println("  --include-invisible      Include invisible elements");
        System.out.println("  --list                   List all visible windows");
        System.out.println("  --verbose, -v            Enable verbose output");
        System.out.println("  --help, -h               Show this help message\n");
        System.out.println("Examples:");
        System.out.println("  java -jar JavaInspector.jar --list");
        System.out.println("  java -jar JavaInspector.jar --pid 1234 --output ui.json");
        System.out.println("  java -jar JavaInspector.jar --title Calculator --output calc.json");
    }
    
    private static class CommandLineOptions {
        String pid;
        String title;
        String output;
        int maxDepth = DEFAULT_MAX_DEPTH;
        boolean includeInvisible = false;
        boolean list = false;
        boolean verbose = false;
    }
}
