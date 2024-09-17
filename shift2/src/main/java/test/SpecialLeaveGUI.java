package test;

import javax.swing.*;
import java.awt.*;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CountDownLatch;

public class SpecialLeaveGUI {
    private JFrame frame;
    private final List<JCheckBox[]> leaveCheckBoxes;
    private final List<JComboBox<String>> weekComboBoxes;
    private final List<String> employeeNames;
    private final List<Employee> employees;
    private static final int TOTAL_DAYS = 7;
    private static final String[] DAYS = {"Day 1", "Day 2", "Day 3", "Day 4", "Day 5", "Day 6", "Day 7"};
    private final CountDownLatch latch;
    private static List<List<Integer>> allSpecialLeaveDays;
    private static List<List<Integer>> leaveWeeks; // Her çalışan için ayrı bir hafta listesi

    public SpecialLeaveGUI(List<String> employeeNames, List<Employee> employees, CountDownLatch latch) {
        this.employeeNames = employeeNames;
        this.employees = employees;
        this.leaveCheckBoxes = new ArrayList<>();
        this.weekComboBoxes = new ArrayList<>();
        this.latch = latch;
        allSpecialLeaveDays = new ArrayList<>();
        leaveWeeks = new ArrayList<>(); // Her çalışan için ayrı bir hafta listesi
    }

    public static List<List<Integer>> getAllSpecialLeaveDays() {
        return allSpecialLeaveDays;
    }

    public static List<List<Integer>> getLeaveWeeks() {
        return leaveWeeks;
    }

    public void display() {
        if (frame != null && frame.isShowing()) {
            frame.dispose();
        }

        frame = new JFrame("Special Leave Information");
        frame.setLayout(new GridBagLayout());
        GridBagConstraints gbc = new GridBagConstraints();
        gbc.insets = new Insets(5, 5, 5, 5);
        gbc.fill = GridBagConstraints.HORIZONTAL;

        // Create a header row
        gbc.gridx = 0;
        gbc.gridy = 0;
        frame.add(new JLabel("Employee"), gbc);

        for (int i = 0; i < DAYS.length; i++) {
            gbc.gridx = i + 1;
            frame.add(new JLabel(DAYS[i]), gbc);
        }

        gbc.gridx = DAYS.length + 1;
        frame.add(new JLabel("Week"), gbc);

        int row = 1;
        for (String employeeName : employeeNames) {
            gbc.gridx = 0;
            gbc.gridy = row;
            frame.add(new JLabel(employeeName), gbc);

            JCheckBox[] checkBoxes = new JCheckBox[TOTAL_DAYS];
            for (int j = 0; j < TOTAL_DAYS; j++) {
                gbc.gridx = j + 1;
                checkBoxes[j] = new JCheckBox();
                checkBoxes[j].setPreferredSize(new Dimension(30, 30));
                frame.add(checkBoxes[j], gbc);
            }
            leaveCheckBoxes.add(checkBoxes);

            // Add JComboBox for selecting the week
            gbc.gridx = DAYS.length + 1;
            JComboBox<String> weekComboBox = new JComboBox<>(new String[] {"Week 1", "Week 2", "Week 3", "Week 4"});
            frame.add(weekComboBox, gbc);
            weekComboBoxes.add(weekComboBox);

            row++;
        }

        JButton submitButton = new JButton("Submit");
        submitButton.setPreferredSize(new Dimension(100, 40));
        submitButton.addActionListener(e -> {
            allSpecialLeaveDays.clear();
            leaveWeeks.clear();
            for (int i = 0; i < leaveCheckBoxes.size(); i++) {
                JCheckBox[] checkBoxes = leaveCheckBoxes.get(i);
                List<Integer> specialLeaveDays = new ArrayList<>();
                for (int j = 0; j < TOTAL_DAYS; j++) {
                    if (checkBoxes[j].isSelected()) {
                        specialLeaveDays.add(j);
                    }
                }
                allSpecialLeaveDays.add(specialLeaveDays);

                // Get the selected week for each employee
                JComboBox<String> weekComboBox = weekComboBoxes.get(i);
                int selectedWeek = weekComboBox.getSelectedIndex(); // Index starts from 0
                List<Integer> weeks = new ArrayList<>(); // Her çalışan için ayrı bir hafta
                weeks.add(selectedWeek); // Store the selected week for each employee
                leaveWeeks.add(weeks); // Add the week list to the main list
            }

            // Burada çalışanların izin bilgilerini güncelleyelim
            for (int i = 0; i < employeeNames.size(); i++) {
                Employee employee = employees.get(i); // employees listeniz olmalı
                employee.setSpecialLeaveDays(allSpecialLeaveDays.get(i));
                employee.setSpecialLeaveWeeks(leaveWeeks.get(i)); // Set the week list for each employee
            }

            frame.dispose();
            latch.countDown(); // Close the current frame
        });

        gbc.gridx = 0;
        gbc.gridy = row;
        gbc.gridwidth = DAYS.length + 2;
        frame.add(submitButton, gbc);

        frame.setSize(1000, 600);
        frame.setDefaultCloseOperation(JFrame.DISPOSE_ON_CLOSE);
        frame.setVisible(true);
    }
}