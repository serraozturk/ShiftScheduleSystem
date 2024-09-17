
package repository;


import test.Employee;

import java.io.IOException;
import java.io.InputStream;
import java.sql.*;
import java.util.List;
import java.util.Properties;

public class databasemanager {
    static final int TOTAL_DAYS = 7;

    private static Connection connection;

    // Initialize the database connection
    public static void initializeDatabaseConnection() throws SQLException {
        try (InputStream input = databasemanager.class.getClassLoader().getResourceAsStream("application.properties")) {
            Properties prop = new Properties();
            prop.load(input);

            String url = prop.getProperty("db.url");
            String username = prop.getProperty("db.username");
            String password = prop.getProperty("db.password");

            // Establish the connection
            connection = DriverManager.getConnection(url, username, password);
            System.out.println("Connected to the database!");
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    // Close the database connection
    public static void closeDatabaseConnection() {
        try {
            if (connection != null && !connection.isClosed()) {
                connection.close();
                System.out.println("Connection closed!");
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }

    // Retrieve the next week number
    public static int getNextWeekNumber() throws SQLException {
        String sql = "SELECT COALESCE(MAX(week_number), 0) + 1 FROM Employees";
        try (Statement stmt = connection.createStatement(); ResultSet rs = stmt.executeQuery(sql)) {
            if (rs.next()) {
                return rs.getInt(1); // Increment the week number
            } else {
                return 1; // Start with week 1 if no records exist
            }
        }
    }

    // Save shift summary to the database
    public static void saveShiftSummaryToDatabase(int weekNumber, List<Employee> employees) throws SQLException {
        String sql = "INSERT INTO Employees (name, week_number, monday, tuesday, wednesday, thursday, friday, saturday, sunday, morning_shifts, afternoon_shifts, night_shifts, special_leave_days) VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)";
        try (PreparedStatement pstmt = connection.prepareStatement(sql)) {
            for (Employee employee : employees) {
                int morningCount = 0;
                int afternoonCount = 0;
                int nightCount = 0;

                // Count shifts
                for (int day = 0; day < TOTAL_DAYS; day++) {
                    String shift = employee.getShift(day);
                    if (shift.equals("09.00-18.00")) morningCount++;
                    else if (shift.equals("17.00-01.00")) afternoonCount++;
                    else if (shift.equals("01.00-09.00")) nightCount++;
                }

                pstmt.setString(1, employee.getName());
                pstmt.setInt(2, weekNumber);
                pstmt.setString(3, employee.getShift(0)); // Monday
                pstmt.setString(4, employee.getShift(1)); // Tuesday
                pstmt.setString(5, employee.getShift(2)); // Wednesday
                pstmt.setString(6, employee.getShift(3)); // Thursday
                pstmt.setString(7, employee.getShift(4)); // Friday
                pstmt.setString(8, employee.getShift(5)); // Saturday
                pstmt.setString(9, employee.getShift(6)); // Sunday
                pstmt.setInt(10, morningCount);
                pstmt.setInt(11, afternoonCount);
                pstmt.setInt(12, nightCount);
                pstmt.setString(13, employee.getSpecialLeaveDaysString()); // Special leave days
                pstmt.addBatch();
            }
            pstmt.executeBatch();
        }
    }

}
