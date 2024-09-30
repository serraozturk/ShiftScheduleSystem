package test;

import org.apache.poi.ss.usermodel.*;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import repository.databasemanager;

import javax.swing.*;
import java.awt.*;
import java.io.FileOutputStream;
import java.io.IOException;
import java.sql.SQLException;
import java.util.*;
import java.util.List;


public class ShiftScheduler {
    static final int TOTAL_DAYS = 28; // 28 gün
    static final int NUM_EMPLOYEES = 8; // 8 çalışan
    static final int NIGHT_SHIFT_DURATION = 5; // Gece vardiyası süresi
    static final int NIGHT_SHIFT_INTERVAL = 5; // Gece vardiyasının sıklığı (5 günde bir değişim)
    static final int OFF_PERIOD_DURATION = 2; // Off dönemi süresi (2 gün)

    public static void main(String[] args) throws SQLException {
        try{
        databasemanager.initializeDatabaseConnection();
        Shift2GUI inputGUI = new Shift2GUI();
        List<String> employees = inputGUI.getEmployeeNames();
        // Çalışan adları

        // Günlük vardiya programını oluştur
        String[][] schedule = new String[TOTAL_DAYS][NUM_EMPLOYEES];

        // Başlangıçta tüm vardiyalar "NO SHIFT"
        for (int day = 0; day < TOTAL_DAYS; day++) {
            Arrays.fill(schedule[day], "NO SHIFT");
        }

        // Gece vardiyasında çalışacak çalışanların sıraya konması
        List<Integer> nightShiftEmployees = new ArrayList<>();
        for (int i = 0; i < NUM_EMPLOYEES; i++) {
            nightShiftEmployees.add(i);
        }
        Collections.shuffle(nightShiftEmployees); // Gece vardiyasında çalışacakları rastgele sıraya koy

        // Gece vardiyası atama
        int nightShiftIndex = 0;
        int employeeIndex = 0;
        while (nightShiftIndex < TOTAL_DAYS) {
            int currentEmployee = nightShiftEmployees.get(employeeIndex);

            // Gece vardiyasında çalışan günlerini işaretle
            for (int day = nightShiftIndex; day < nightShiftIndex + NIGHT_SHIFT_DURATION && day < TOTAL_DAYS; day++) {
                schedule[day][currentEmployee] = "NIGHT";
            }

            // Bir sonraki gece vardiyası başlangıç gününü hesapla
            nightShiftIndex += NIGHT_SHIFT_INTERVAL;
            employeeIndex++;
            if (employeeIndex >= NUM_EMPLOYEES) {
                employeeIndex = 0; // Eğer çalışanlar bitmişse sırayı başa al
            }
        }

        // Gece vardiyasından sonra 2 off günü ayarlama
        ensureNightShiftOffDays(schedule);

        // Haftalık off günlerini ayarlamak için her 7 günde bir kontrol yap
        for (int weekStart = 0; weekStart < TOTAL_DAYS; weekStart += 7) {
            adjustWeeklyOffDays(schedule, weekStart);
        }

        for (int day = 0; day < TOTAL_DAYS; day++) {
            int morningCount = 0;
            int afternoonCount = 0;
            List<Integer> morningEmployees = new ArrayList<>();
            List<Integer> afternoonEmployees = new ArrayList<>();

            // Assign morning and afternoon shifts
            for (int emp = 0; emp < NUM_EMPLOYEES; emp++) {
                if (!schedule[day][emp].equals("NIGHT") && !schedule[day][emp].equals("OFF")) {
                    if (morningCount < 2) {
                        schedule[day][emp] = "MORNING";
                        morningCount++;
                        morningEmployees.add(emp);
                    } else if (afternoonCount < 2) {
                        schedule[day][emp] = "AFTERNOON";
                        afternoonCount++;
                        afternoonEmployees.add(emp);
                    } else {
                        // If 2 morning and 2 afternoon shifts are already assigned, randomly assign the remaining employees
                        if (Math.random() < 0.5) {
                            schedule[day][emp] = "MORNING";
                            morningEmployees.add(emp);
                        } else {
                            schedule[day][emp] = "MORNING";
                            afternoonEmployees.add(emp);
                        }
                    }
                }
            }

        }
        askForSpecialLeave(schedule, employees);

        // Excel dosyasına yazdır
        writeScheduleToExcel(schedule, employees);


        } catch (SQLException e) {
            e.printStackTrace();
        } finally {
            databasemanager.closeDatabaseConnection();
        }

    }
    private static void askForSpecialLeave(String[][] schedule, List<String> employees) {
        // Ask the user how many employees they want to grant special leave to
        int numEmployees = Integer.parseInt(JOptionPane.showInputDialog(null, "How many employees do you want to grant special leave to?", "Number of Employees", JOptionPane.QUESTION_MESSAGE));

        for (int i = 0; i < numEmployees; i++) {
            // Create a panel to hold all the components
            JPanel panel = new JPanel();
            panel.setLayout(new BorderLayout());

            // Create a combo box to select the employee
            JComboBox<String> employeeComboBox = new JComboBox<>(employees.toArray(new String[0]));
            panel.add(employeeComboBox, BorderLayout.NORTH);

            // Create a spinner to select the week
            SpinnerNumberModel weekModel = new SpinnerNumberModel(1, 1, TOTAL_DAYS / 7, 1);
            JSpinner weekSpinner = new JSpinner(weekModel);
            panel.add(weekSpinner, BorderLayout.CENTER);

            // Create a panel to hold the day checkboxes
            String[] days = {"Monday", "Tuesday", "Wednesday", "Thursday", "Friday", "Saturday", "Sunday"};
            JCheckBox[] dayCheckBoxes = new JCheckBox[7];
            JPanel dayPanel = new JPanel();
            dayPanel.setLayout(new GridLayout(7, 1));
            for (int j = 0; j < 7; j++) {
                dayCheckBoxes[j] = new JCheckBox(days[j]);
                dayPanel.add(dayCheckBoxes[j]);
            }
            panel.add(dayPanel, BorderLayout.SOUTH);

            // Show the dialog
            int result = JOptionPane.showConfirmDialog(null, panel, "Grant Special Leave to Employee " + (i + 1), JOptionPane.OK_CANCEL_OPTION);

            if (result == JOptionPane.OK_OPTION) {
                // Get the selected employee, week, and days
                String employee = (String) employeeComboBox.getSelectedItem();
                int selectedWeek = (int) weekSpinner.getValue();
                int employeeIndex = employees.indexOf(employee);
                int weekStart = (selectedWeek - 1) * 7;

                // Grant the special leave to the selected employee for the selected days
                for (int day = 0; day < 7; day++) {
                    if (dayCheckBoxes[day].isSelected() && weekStart + day < TOTAL_DAYS) {
                        schedule[weekStart + day][employeeIndex] = "İZİNLİ";
                    }
                }
            }
        }
    }
    private static void ensureNightShiftOffDays(String[][] schedule) {
        for (int emp = 0; emp < NUM_EMPLOYEES; emp++) {
            int lastNightShiftDay = -1;

            // Gece vardiyası biten günleri bul
            for (int day = 0; day < TOTAL_DAYS; day++) {
                if (schedule[day][emp].equals("NIGHT")) {
                    lastNightShiftDay = day;
                }
            }

            // Gece vardiyasından sonra 2 off gününü ayarla
            if (lastNightShiftDay != -1) {
                int offStartDay = lastNightShiftDay + 1;
                for (int j = 0; j < OFF_PERIOD_DURATION && offStartDay + j < TOTAL_DAYS; j++) {
                    if (!schedule[offStartDay + j][emp].equals("NIGHT")) {
                        schedule[offStartDay + j][emp] = "OFF";
                    }
                }
            }
        }
    }
    private static void adjustWeeklyOffDays(String[][] schedule, int weekStart) {
        Random random = new Random();

        // Select 2 random employees for the week
        List<Integer> employees = new ArrayList<>();
        for (int emp = 0; emp < NUM_EMPLOYEES; emp++) {
            employees.add(emp);
        }
        Collections.shuffle(employees);
        List<Integer> selectedEmployees = new ArrayList<>();

        // Ensure the selected employees do not have a night shift on the previous days
        while (selectedEmployees.size() < 2) {
            int emp = employees.get(0);
            boolean hasNightShift = false;
            for (int day = weekStart; day < weekStart + 5; day++) {
                if (schedule[day][emp].equals("NIGHT") || schedule[day + 1][emp].equals("NIGHT")|| schedule[day + 2][emp].equals("NIGHT")) {
                    hasNightShift = true;
                    break;
                }
            }
            if (!hasNightShift) {
                selectedEmployees.add(emp);
            }
            employees.remove(0);
        }

        // Assign 2 consecutive off days at the end of the week for the selected employees
        for (int emp : selectedEmployees) {
            int sat = weekStart + 5;
            int sun = weekStart + 6;

            // Ensure these days are within the schedule bounds
            if (sat < TOTAL_DAYS && sun < TOTAL_DAYS) {
                schedule[sat][emp] = "OFF";
                schedule[sun][emp] = "OFF";
            }
        }

        // Assign other off days
        for (int emp = 0; emp < NUM_EMPLOYEES; emp++) {
            if (!selectedEmployees.contains(emp)) {
                Set<Integer> offDays = new HashSet<>();
                // Find the off days for the current employee
                for (int day = weekStart; day < weekStart + 7 && day < TOTAL_DAYS; day++) {
                    if (schedule[day][emp].equals("OFF")) {
                        offDays.add(day);
                    }
                }

                // Assign additional off days if needed
                while (offDays.size() < OFF_PERIOD_DURATION) {
                    List<Integer> availableDays = new ArrayList<>();
                    for (int day = weekStart; day < weekStart + 7 && day < TOTAL_DAYS; day++) {
                        int offCount = 0;
                        for (int e = 0; e < NUM_EMPLOYEES; e++) {
                            if (schedule[day][e].equals("OFF")) {
                                offCount++;
                            }
                        }
                        if (schedule[day][emp].equals("NO SHIFT") && !offDays.contains(day) && offCount < 3) {
                            availableDays.add(day);
                        }
                    }
                    if (!availableDays.isEmpty()) {
                        int randomDay = availableDays.get(random.nextInt(availableDays.size()));
                        schedule[randomDay][emp] = "OFF";
                        offDays.add(randomDay);
                    } else {
                        // If no available days, try to find a day with only 1 off employee
                        for (int day = weekStart; day < weekStart + 7 && day < TOTAL_DAYS; day++) {
                            int offCount = 0;
                            for (int e = 0; e < NUM_EMPLOYEES; e++) {
                                if (schedule[day][e].equals("OFF")) {
                                    offCount++;
                                }
                            }
                            if (offCount == 1 && !offDays.contains(day)) {
                                schedule[day][emp] = "OFF";
                                offDays.add(day);
                                break;
                            }
                        }
                    }
                }
            }
        }

        // Ensure each day has at least 1 employee off if needed
        for (int day = weekStart; day < weekStart + 7 && day < TOTAL_DAYS; day++) {
            int offCount = 0;
            for (int e = 0; e < NUM_EMPLOYEES; e++) {
                if (schedule[day][e].equals("OFF")) {
                    offCount++;
                }
            }

            // Check if the number of off employees for the day is less than 1
            if (offCount == 0) {
                // Check if the total number of off days across the week is sufficient
                int totalOffDays = 0;
                for (int e = 0; e < NUM_EMPLOYEES; e++) {
                    for (int d = weekStart; d < weekStart + 7 && d < TOTAL_DAYS; d++) {
                        if (schedule[d][e].equals("OFF")) {
                            totalOffDays++;
                        }
                    }
                }

                // Only enforce the off day requirement if the total off days are less than required
                if (totalOffDays < NUM_EMPLOYEES) { // Assuming each employee should have at least 1 off day per week
                    List<Integer> availableEmployees = new ArrayList<>();
                    for (int e = 0; e < NUM_EMPLOYEES; e++) {
                        if (schedule[day][e].equals("NO SHIFT")) {
                            availableEmployees.add(e);
                        }
                    }
                    if (!availableEmployees.isEmpty()) {
                        int randomEmployee = availableEmployees.get(random.nextInt(availableEmployees.size()));
                        schedule[day][randomEmployee] = "OFF";
                    }
                }
            }
        }
    }


    private static void writeScheduleToExcel(String[][] schedule, List<String> employees) {
        Workbook workbook = new XSSFWorkbook();
        Sheet sheet = workbook.createSheet("Shift Schedule");

        int rowIndex = 0;
        int daysPerWeek = 7;
        int numWeeks = TOTAL_DAYS / daysPerWeek;

        for (int week = 0; week < numWeeks; week++) {
            // Tablo başlığını ekle
            Row headerRow = sheet.createRow(rowIndex++);
            headerRow.createCell(0).setCellValue("Employee / Day");
            for (int day = 0; day < daysPerWeek; day++) {
                headerRow.createCell(day + 1).setCellValue("Day " + (week * daysPerWeek + day + 1));
            }

            // Çalışanlar için satırları oluştur
            for (int emp = 0; emp < NUM_EMPLOYEES; emp++) {
                Row row = sheet.createRow(rowIndex++);
                row.createCell(0).setCellValue(employees.get(emp));
                for (int day = 0; day < daysPerWeek; day++) {
                    row.createCell(day + 1).setCellValue(schedule[week * daysPerWeek + day][emp]);
                }
            }

            // Boş satır ekleyerek tablolara ayırma
            rowIndex++;
        }



        // Excel dosyasını kaydet
        try (FileOutputStream fileOut = new FileOutputStream("ShiftSchedule.xlsx")) {
            workbook.write(fileOut);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
}