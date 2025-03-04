// ConsoleWindow.java
package com.newfangledthings.clickdropprinthelper;

import javax.swing.*;
import javax.swing.border.EmptyBorder;
import java.io.OutputStream;
import java.io.PrintStream;
import java.net.URL;

public class ConsoleWindow extends JFrame {
    private JTextArea textArea;

    public ConsoleWindow() {
        setTitle("Console Output");
        setSize(600, 400);
        setDefaultCloseOperation(JFrame.HIDE_ON_CLOSE);
        textArea = new JTextArea();
        textArea.setEditable(false);
        textArea.setBorder(new EmptyBorder(10, 10, 10, 10)); // Add padding to textArea
        add(new JScrollPane(textArea));
        setLocationRelativeTo(null);

        URL imageUrl = WindowsApp.class.getResource("/icon.png");
        if (imageUrl == null) {
            System.err.println("Icon image not found!");
            return;
        }
        setIconImage(new ImageIcon(imageUrl).getImage());

        // Redirect System.out and System.err to the JTextArea
        PrintStream printStream = new PrintStream(new OutputStream() {
            @Override
            public void write(int b) {
                textArea.append(String.valueOf((char) b));
                textArea.setCaretPosition(textArea.getDocument().getLength());
            }

            @Override
            public void write(byte[] b, int off, int len) {
                textArea.append(new String(b, off, len));
                textArea.setCaretPosition(textArea.getDocument().getLength());
            }
        }, true); // Enable auto-flush
        System.setOut(printStream);
        System.setErr(printStream);
    }
}