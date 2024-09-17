package test;

import javax.swing.*;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CountDownLatch;

public class Shift2GUI extends JFrame {

    private JFrame frame;
    private JTextField[] nameFields;
    private static final int TOTAL_EMPLOYEES = 8;
    private Connection connection;
    private List<String> employeeNames;
    private CountDownLatch latch = new CountDownLatch(1);

    public Shift2GUI() {
        initialize();
    }


    private void initialize() {
        frame = new JFrame("Employee Input");
        frame.setBounds(100, 100, 450, 300);
        frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        frame.getContentPane().setLayout(new GridLayout(TOTAL_EMPLOYEES + 1, 2));

        JLabel[] labels = new JLabel[TOTAL_EMPLOYEES];
        nameFields = new JTextField[TOTAL_EMPLOYEES];

        for (int i = 0; i < TOTAL_EMPLOYEES; i++) {
            labels[i] = new JLabel("Employee " + (i + 1) + ":");
            nameFields[i] = new JTextField();
            frame.getContentPane().add(labels[i]);
            frame.getContentPane().add(nameFields[i]);
        }

        JButton btnSubmit = new JButton("Submit");
        btnSubmit.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                submitData();
                latch.countDown(); // Signal that names have been entered
                frame.dispose(); // Close the GUI
            }
        });
        frame.getContentPane().add(btnSubmit);

        frame.setVisible(true);
    }

    private void submitData() {
        employeeNames = new ArrayList<>();  // Initialize the list
        for (JTextField field : nameFields) {
            String name = field.getText().trim();
            if (!name.isEmpty()) {
                employeeNames.add(name);  // Add names to employeeNames list
            }
        }
        if (employeeNames.size() == TOTAL_EMPLOYEES) {  // Check employeeNames list
            try {
                initializeDatabaseConnection();

                JOptionPane.showMessageDialog(frame, "Data saved successfully!");
            } catch (SQLException ex) {
                ex.printStackTrace();
                JOptionPane.showMessageDialog(frame, "Error saving data: " + ex.getMessage());
            } finally {
                closeDatabaseConnection();
            }

            // Notify that names have been entered
            latch.countDown(); // Signal that names have been entered
            frame.dispose(); // Close the GUI
        } else {
            JOptionPane.showMessageDialog(frame, "Please enter names for all employees.");
        }
    }

    public List<String> getEmployeeNames() {
        // Wait for the user to close the GUI
        try {
            latch.await(); // Wait until the latch count down is called
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
        return employeeNames;
    }

    private void initializeDatabaseConnection() throws SQLException {
        String url = "jdbc:postgresql://localhost:5432/Employees";
        String user = "postgres";
        String password = "123";
        connection = DriverManager.getConnection(url, user, password);
    }

    private void closeDatabaseConnection() {
        try {
            if (connection != null && !connection.isClosed()) {
                connection.close();
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }



}