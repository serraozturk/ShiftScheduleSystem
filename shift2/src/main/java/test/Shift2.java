package shift2.src.main.java.test;


import org.apache.poi.ss.usermodel.*;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import shift2.src.main.java.repository.databasemanager;

import java.io.FileOutputStream;
import java.io.IOException;
import java.sql.*;
import java.util.*;
import java.util.List;
import java.util.concurrent.CountDownLatch;


import javax.swing.*;

public class Shift2 {
    static final int TOTAL_DAYS = 7;
    private static final int TOTAL_EMPLOYEES = 8;
    public static final String[] DAYS = {"Monday", "Tuesday", "Wednesday", "Thursday", "Friday", "Saturday", "Sunday"};
    private static final int MIN_MORNING_SHIFT = 2;
    private static final int MIN_AFTERNOON_SHIFT = 2;
    private static final int MIN_NIGHT_SHIFT = 1;
    private static final int MAX_OFF_PER_DAY = 3;
    private static final int TOTAL_WEEKS = 4;  // Number of weeks to schedule
    private static final int ROWS_BETWEEN_WEEKS = 2;
    public static List<List<Integer>> leaveWeeks = new ArrayList<>();
    public static List<List<Integer>> allSpecialLeaveDays = new ArrayList<>();// Space between weeks in Excel
    private static List<Employee> previousWeekendOffEmployees = new ArrayList<>();
    private static Map<Employee, Boolean> employeeHasWeekendOff = new HashMap<>();
    private static Map<Employee, Boolean> employeeHasShift = new HashMap<>();
    private static List<Employee> previousShiftEmployees = new ArrayList<>();


    public static void main(String[] args) {
        try {
            // Initialize the database connection
            databasemanager.initializeDatabaseConnection();

            Shift2GUI inputGUI = new Shift2GUI();
            List<String> actualEmployeeNames = inputGUI.getEmployeeNames();

            if (actualEmployeeNames.size() != TOTAL_EMPLOYEES) {
                throw new IllegalArgumentException("The number of employee names provided does not match the total number of employees.");
            }

            List<Employee> employees = new ArrayList<>();
            for (int i = 0; i < TOTAL_EMPLOYEES; i++) {
                employees.add(new Employee(actualEmployeeNames.get(i)));
            }

            int response = JOptionPane.showConfirmDialog(null,
                    "Are there any special leaves needed?",
                    "Special Leave",
                    JOptionPane.YES_NO_OPTION);

            if (response == JOptionPane.YES_OPTION) {
                CountDownLatch specialLeaveLatch = new CountDownLatch(1);
                SpecialLeaveGUI specialLeaveGUI = new SpecialLeaveGUI(actualEmployeeNames, employees,specialLeaveLatch);
                specialLeaveGUI.display();
                try {
                    specialLeaveLatch.await();
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }

                allSpecialLeaveDays = SpecialLeaveGUI.getAllSpecialLeaveDays();
                leaveWeeks = SpecialLeaveGUI.getLeaveWeeks();
                for (int i = 0; i < employees.size(); i++) {
                    employees.get(i).setSpecialLeaveDays(allSpecialLeaveDays.get(i));
                    employees.get(i).setSpecialLeaveWeeks(leaveWeeks.get(i));
                }
            }

            Workbook workbook = new XSSFWorkbook();
            Sheet sheet = workbook.createSheet("Shift Schedule");

            for (int week = 0; week < TOTAL_WEEKS; week++) {
                generateWeeklySchedule(week, employees, sheet, workbook);
            }

            try (FileOutputStream fileOut = new FileOutputStream("ShiftSchedule.xlsx")) {
                workbook.write(fileOut);
            }
            workbook.close();

        } catch (SQLException | IOException e) {
            e.printStackTrace();
        } finally {
            databasemanager.closeDatabaseConnection();
        }
    }

    private static List<Employee> getAvailableNightShiftEmployees(List<Employee> employees, List<Employee> previousNightShiftEmployees, Random random) {
        List<Employee> availableEmployees = new ArrayList<>(employees);
        availableEmployees.removeAll(previousNightShiftEmployees);
        availableEmployees.removeIf(employee -> employeeHasShift.getOrDefault(employee, false));
        Collections.shuffle(availableEmployees, random);
        return availableEmployees;
    }

    private static void generateWeeklySchedule(int week, List<Employee> employees, Sheet sheet, Workbook workbook) throws SQLException {
        Random random = new Random();

        // Initialize daily shift counts
        int[] morningCounts = new int[TOTAL_DAYS];
        int[] afternoonCounts = new int[TOTAL_DAYS];
        int[] nightCounts = new int[TOTAL_DAYS];
        int[] offCounts = new int[TOTAL_DAYS];

        // Reset off days for all employees
        for (Employee employee : employees) {
            employee.resetOffDays();
        }

        // List of employees with weekend off
        List<Employee> weekendOffEmployees = getWeekendOffEmployees(employees, previousWeekendOffEmployees, random);
        for (Employee employee : weekendOffEmployees) {
            employee.setOff(5); // Saturday
            employee.setOff(6); // Sunday
            offCounts[5]++;
            offCounts[6]++;
            employeeHasWeekendOff.put(employee, true);
        }

        // Create a map to track the number of employees off per shift type during weekdays
        Map<String, Integer>[] shiftOffCounts = new HashMap[TOTAL_DAYS];
        for (int day = 0; day < TOTAL_DAYS; day++) {
            shiftOffCounts[day] = new HashMap<>();
            shiftOffCounts[day].put("09.00-18.00", 0);
            shiftOffCounts[day].put("17.00-01.00", 0);
            shiftOffCounts[day].put("01.00-09.00", 0);
        }

        // Track the number of days off each employee has
        Map<Employee, Integer> employeeOffDaysCount = new HashMap<>();
        for (Employee employee : employees) {
            employeeOffDaysCount.put(employee, 0);
        }

        // Determine the employees who should receive weekday off days
        List<Employee> employeesWithoutWeekendOff = new ArrayList<>(employees);
        employeesWithoutWeekendOff.removeAll(weekendOffEmployees);

        // Assign 2 weekdays off for each employee who has not already used their weekend days off
        for (Employee employee : employeesWithoutWeekendOff) {
            // Ensure the employee has exactly 2 off days
            if (employeeOffDaysCount.get(employee) < 2) {
                List<Integer> availableDays = new ArrayList<>();
                for (int day = 0; day < 5; day++) { // Monday to Friday
                    if (offCounts[day] < MAX_OFF_PER_DAY) { // Max 3 employees off per day
                        // Check if adding this employee's off would exceed the limit for each shift type
                        if (shiftOffCounts[day].get("09.00-18.00") < MIN_MORNING_SHIFT &&
                                shiftOffCounts[day].get("17.00-01.00") < MIN_AFTERNOON_SHIFT) {
                            availableDays.add(day);
                        }
                    }
                }

                // Ensure the employee gets exactly 2 days off
                while (employeeOffDaysCount.get(employee) < 2 && !availableDays.isEmpty()) {
                    int day = availableDays.get(random.nextInt(availableDays.size()));
                    if (!employee.isOff(day)) {
                        employee.setOff(day);
                        offCounts[day]++;
                        employeeOffDaysCount.put(employee, employeeOffDaysCount.get(employee) + 1);
                        // Update the shift off count
                        String shift = employee.getShift(day);
                        if (shift != null) {
                            shiftOffCounts[day].put(shift, shiftOffCounts[day].getOrDefault(shift, 0) + 1);
                        }
                    }
                }
            }
        }

        // Determine night shift employees
        List<Employee> nightShiftEmployees = getAvailableNightShiftEmployees(employees, previousShiftEmployees, random);
        nightShiftEmployees = nightShiftEmployees.subList(0, Math.min(2, nightShiftEmployees.size()));

        // Assign night shifts to selected employees
        for (Employee employee : nightShiftEmployees) {
            employee.setAssignedShift("01.00-09.00");
            employeeHasShift.put(employee, true);
            for (int day = 0; day < TOTAL_DAYS; day++) {
                if (!employee.isOff(day)) {
                    employee.setShift(day, "01.00-09.00");
                    nightCounts[day]++;
                    offCounts[day]++; // Ensure night shift employees have a day off
                }
            }
        }

        // Assign shifts to remaining employees ensuring minimum counts
        for (Employee employee : employees) {
            if (!nightShiftEmployees.contains(employee)) {
                for (int day = 0; day < TOTAL_DAYS; day++) {
                    if (employee.getSpecialLeaveDays().contains(day) && employee.getSpecialLeaveWeeks().contains(week)) {
                        employee.setShift(day, "IZINLI");
                    } else {
                        if (!employee.isOff(day)) {
                            String shift = findSuitableShiftForEmployee(day, nightCounts, morningCounts, afternoonCounts, random);
                            // Check if assigning this shift will exceed the minimum required shifts
                            if (shift.equals("09.00-18.00") && morningCounts[day] < MIN_MORNING_SHIFT) {
                                employee.setShift(day, shift);
                                morningCounts[day]++;
                            } else if (shift.equals("17.00-01.00") && afternoonCounts[day] < MIN_AFTERNOON_SHIFT) {
                                employee.setShift(day, shift);
                                afternoonCounts[day]++;
                            } else if (shift.equals("01.00-09.00") && nightCounts[day] < MIN_NIGHT_SHIFT) {
                                employee.setShift(day, shift);
                                nightCounts[day]++;
                            } else {
                                // Assign alternative shift if the chosen one is not suitable
                                shift = getAlternativeShift(shift, "09.00-18.00", "17.00-01.00", "01.00-09.00");
                                employee.setShift(day, shift);
                                updateShiftCounts(shift, day, morningCounts, afternoonCounts);
                            }
                        }
                    }
                }
            }
        }

        previousShiftEmployees.clear();
        previousShiftEmployees.addAll(nightShiftEmployees);

        // Create the schedule in the Excel sheet
        int startRow = (week * (TOTAL_EMPLOYEES + ROWS_BETWEEN_WEEKS)) + 1;
        int headerRowNumber = startRow - 1;
        Row headerRow = sheet.createRow(headerRowNumber);
        headerRow.createCell(0).setCellValue("Week " + (week + 1));
        for (int i = 0; i < DAYS.length; i++) {
            Cell cell = headerRow.createCell(i + 1);
            cell.setCellValue(DAYS[i]);
        }

        for (int i = 0; i < employees.size(); i++) {
            Row row = sheet.createRow(startRow + i);
            row.createCell(0).setCellValue(employees.get(i).getName());
            for (int day = 0; day < TOTAL_DAYS; day++) {
                Cell cell = row.createCell(day + 1);
                String shift = employees.get(i).getShift(day);
                cell.setCellValue(shift);

                if (shift.equals("09.00-18.00")) {
                    cell.setCellStyle(getCellStyle(workbook, IndexedColors.LIGHT_YELLOW));
                } else if (shift.equals("17.00-01.00")) {
                    cell.setCellStyle(getCellStyle(workbook, IndexedColors.LIGHT_GREEN));
                } else if (shift.equals("01.00-09.00")) {
                    cell.setCellStyle(getCellStyle(workbook, IndexedColors.LIGHT_CORNFLOWER_BLUE));
                } else if (shift.equals("IZINLI")) {
                    cell.setCellStyle(getCellStyle(workbook, IndexedColors.PINK));
                } else {
                    cell.setCellStyle(getCellStyle(workbook, IndexedColors.AQUA));
                }
            }
        }

        // Save the week's schedule to the database
        int weekNumber = databasemanager.getNextWeekNumber() + week;
        databasemanager.saveShiftSummaryToDatabase(weekNumber, employees);
    }

    private static List<Employee> getWeekendOffEmployees(List<Employee> employees, List<Employee> previousWeekendOffEmployees, Random random) {
        List<Employee> weekendOffEmployees = new ArrayList<>();
        List<Employee> availableEmployees = new ArrayList<>(employees);
        availableEmployees.removeAll(previousWeekendOffEmployees);
        availableEmployees.removeIf(employee -> employeeHasWeekendOff.getOrDefault(employee, false));
        Collections.shuffle(availableEmployees, random);
        weekendOffEmployees.add(availableEmployees.get(0));
        weekendOffEmployees.add(availableEmployees.get(1));
        return weekendOffEmployees;
    }
    // Get an alternative shift if the primary shift is overbooked or disallowed
    private static String getAlternativeShift(String currentShift, String... alternatives) {
        for (String shift : alternatives) {
            if (!shift.equals(currentShift)) {
                return shift;
            }
        }
        return currentShift; // fallback if no alternative is found
    }

    private static String findSuitableShiftForEmployee(int day, int[] nightCounts, int[] morningCounts, int[] afternoonCounts, Random random) {
        if (nightCounts[day] < MIN_NIGHT_SHIFT) {
            return "01.00-09.00"; // Ensure that at least one night shift
        } else if (morningCounts[day] < MIN_MORNING_SHIFT) {
            return "09.00-18.00";
        } else if (afternoonCounts[day] < MIN_AFTERNOON_SHIFT) {
            return "17.00-01.00";
        } else {
            // If all shifts are sufficiently filled, return a random one
            String[] allShifts = {"09.00-18.00", "17.00-01.00", "01.00-09.00"};
            return allShifts[random.nextInt(allShifts.length)];
        }
    }


    // Update shift counts for the day
    private static void updateShiftCounts(String shift, int day, int[] morningCounts, int[] afternoonCounts) {
        switch (shift) {
            case "09.00-18.00":
                morningCounts[day]++;
                break;
            case "17.00-01.00":
                afternoonCounts[day]++;
                break;
        }
    }
    // Get the cell style for the shifts
    private static CellStyle getCellStyle(Workbook workbook, IndexedColors color) {
        CellStyle style = workbook.createCellStyle();
        style.setFillForegroundColor(color.getIndex());
        style.setFillPattern(FillPatternType.SOLID_FOREGROUND);
        return style;
    }

}