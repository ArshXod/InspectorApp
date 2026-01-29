package com.inspector.test;

import javax.swing.*;
import java.awt.*;
import java.awt.event.ActionEvent;

/**
 * Sample Java Swing Calculator application for testing the Java UI Inspector.
 * This creates a UI with various components that can be inspected.
 */
public class SampleCalculatorApp {
    
    private JFrame frame;
    private JTextField display;
    private double currentValue = 0;
    private String currentOperation = "";
    private boolean startNewNumber = true;
    
    public static void main(String[] args) {
        // Enable accessibility
        System.setProperty("javax.accessibility.assistive_technologies", 
                         "com.sun.java.accessibility.util.Translator");
        
        SwingUtilities.invokeLater(() -> {
            try {
                UIManager.setLookAndFeel(UIManager.getSystemLookAndFeelClassName());
            } catch (Exception e) {
                e.printStackTrace();
            }
            
            SampleCalculatorApp calculator = new SampleCalculatorApp();
            calculator.createAndShowGUI();
        });
    }
    
    private void createAndShowGUI() {
        frame = new JFrame("Java Calculator");
        frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        frame.setLayout(new BorderLayout(5, 5));
        
        // Create menu bar
        JMenuBar menuBar = new JMenuBar();
        JMenu viewMenu = new JMenu("View");
        viewMenu.add(new JMenuItem("Standard"));
        viewMenu.add(new JMenuItem("Scientific"));
        viewMenu.addSeparator();
        JMenuItem exitItem = new JMenuItem("Exit");
        exitItem.addActionListener(e -> System.exit(0));
        viewMenu.add(exitItem);
        menuBar.add(viewMenu);
        
        JMenu helpMenu = new JMenu("Help");
        helpMenu.add(new JMenuItem("About"));
        menuBar.add(helpMenu);
        
        frame.setJMenuBar(menuBar);
        
        // Create display
        display = new JTextField("0");
        display.setEditable(false);
        display.setHorizontalAlignment(JTextField.RIGHT);
        display.setFont(new Font("Arial", Font.BOLD, 24));
        display.setPreferredSize(new Dimension(300, 50));
        display.getAccessibleContext().setAccessibleName("Display");
        display.getAccessibleContext().setAccessibleDescription("Calculator display showing current value");
        
        frame.add(display, BorderLayout.NORTH);
        
        // Create button panel
        JPanel buttonPanel = new JPanel();
        buttonPanel.setLayout(new GridLayout(5, 4, 5, 5));
        buttonPanel.setBorder(BorderFactory.createEmptyBorder(10, 10, 10, 10));
        buttonPanel.getAccessibleContext().setAccessibleName("Button Panel");
        
        String[] buttons = {
            "MC", "MR", "MS", "M+",
            "7", "8", "9", "/",
            "4", "5", "6", "*",
            "1", "2", "3", "-",
            "0", ".", "=", "+"
        };
        
        for (String label : buttons) {
            JButton button = new JButton(label);
            button.setFont(new Font("Arial", Font.PLAIN, 18));
            button.getAccessibleContext().setAccessibleName(label);
            button.getAccessibleContext().setAccessibleDescription("Button " + label);
            button.addActionListener(this::handleButtonClick);
            buttonPanel.add(button);
        }
        
        frame.add(buttonPanel, BorderLayout.CENTER);
        
        // Create status panel
        JPanel statusPanel = new JPanel(new BorderLayout());
        JLabel statusLabel = new JLabel("Ready");
        statusLabel.setBorder(BorderFactory.createEmptyBorder(5, 10, 5, 10));
        statusPanel.add(statusLabel, BorderLayout.WEST);
        frame.add(statusPanel, BorderLayout.SOUTH);
        
        frame.pack();
        frame.setLocationRelativeTo(null);
        frame.setVisible(true);
        
        System.out.println("Calculator started. PID: " + ProcessHandle.current().pid());
        System.out.println("Window title: " + frame.getTitle());
    }
    
    private void handleButtonClick(ActionEvent e) {
        JButton source = (JButton) e.getSource();
        String command = source.getText();
        
        if (command.matches("[0-9]")) {
            handleDigit(command);
        } else if (command.equals(".")) {
            handleDecimal();
        } else if (command.matches("[+\\-*/]")) {
            handleOperation(command);
        } else if (command.equals("=")) {
            handleEquals();
        } else if (command.equals("MC") || command.equals("MR") || 
                   command.equals("MS") || command.equals("M+")) {
            handleMemory(command);
        }
    }
    
    private void handleDigit(String digit) {
        if (startNewNumber) {
            display.setText(digit);
            startNewNumber = false;
        } else {
            display.setText(display.getText() + digit);
        }
    }
    
    private void handleDecimal() {
        if (startNewNumber) {
            display.setText("0.");
            startNewNumber = false;
        } else if (!display.getText().contains(".")) {
            display.setText(display.getText() + ".");
        }
    }
    
    private void handleOperation(String operation) {
        currentValue = Double.parseDouble(display.getText());
        currentOperation = operation;
        startNewNumber = true;
    }
    
    private void handleEquals() {
        if (!currentOperation.isEmpty()) {
            double secondValue = Double.parseDouble(display.getText());
            double result = 0;
            
            switch (currentOperation) {
                case "+": result = currentValue + secondValue; break;
                case "-": result = currentValue - secondValue; break;
                case "*": result = currentValue * secondValue; break;
                case "/": result = secondValue != 0 ? currentValue / secondValue : 0; break;
            }
            
            display.setText(String.valueOf(result));
            currentOperation = "";
            startNewNumber = true;
        }
    }
    
    private void handleMemory(String command) {
        // Simple memory operations (stub implementation)
        System.out.println("Memory operation: " + command);
    }
}
