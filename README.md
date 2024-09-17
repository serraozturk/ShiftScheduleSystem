# 
# Shift Scheduling System
This Java project generates a weekly shift schedule for employees, considering various constraints and preferences, and saves the schedule in both an Excel file and a PostgreSQL database.

## Features

- **Shift Assignment:**
  - Assigns three types of shifts: `09.00-18.00`, `17.00-01.00`, and `01.00-09.00`.
  - Ensures that each employee gets two days off per week.
  - Considers special leave days for employees.
  - Adheres to constraints like not assigning a morning shift immediately after a night shift.

- **Excel Export:**
  - Exports the shift schedule to an Excel file (`ShiftSchedule.xlsx`).
  - Applies different colors to cells based on the assigned shift:
    - `09.00-18.00` - Light Yellow
    - `17.00-01.00` - Light Green
    - `01.00-09.00` - Light Blue
    - `IZINLI` (special leave) - Pink
    - `Off` days - Aqua

- **Database Integration:**
  - Saves the weekly shift schedule to a PostgreSQL database.
  - Automatically calculates and saves the number of shifts (morning, afternoon, night) and special leave days for each employee.

## Getting Started

### Prerequisites

- **Java:** Ensure you have JDK installed.
- **PostgreSQL:** A running PostgreSQL database with a table named `Employees`.
- **Apache POI:** Include Apache POI dependencies for Excel file handling.

### Database Setup

Ensure you have a PostgreSQL database running and update the database connection details in the `initializeDatabaseConnection` method:

```java
String url = "jdbc:postgresql://localhost:5432/Employees"; // Replace 'Employees' with your database name
String user = "postgres"; // Replace with your PostgreSQL username
String password = "123"; // Replace with your PostgreSQL password
```

### Table Structure

The `Employees` table should have the following structure:

```sql
CREATE TABLE Employees (
    id SERIAL PRIMARY KEY,
    name VARCHAR(255),
    week_number INT,
    monday VARCHAR(255),
    tuesday VARCHAR(255),
    wednesday VARCHAR(255),
    thursday VARCHAR(255),
    friday VARCHAR(255),
    saturday VARCHAR(255),
    sunday VARCHAR(255),
    morning_shifts INT,
    afternoon_shifts INT,
    night_shifts INT,
    special_leave_days VARCHAR(255)
);
```

### Running the Project

1. Compile and run the `Shift2` class.
2. Enter the employee names when prompted (8 names required).
3. Specify any employees who have special leave.
4. The program will generate a shift schedule based on the provided inputs and constraints.
5. The shift schedule will be saved in an Excel file (`ShiftSchedule.xlsx`) and the database.

### Customization

You can modify various constraints and parameters directly in the code:

- `TOTAL_DAYS` - Number of days in a week (default: 7).
- `TOTAL_EMPLOYEES` - Number of employees (default: 8).
- `SHIFTS` - Types of shifts.
- `DAYS` - Days of the week.
- `MIN_MORNING_SHIFT`, `MIN_AFTERNOON_SHIFT`, `MIN_NIGHT_SHIFT` - Minimum number of each shift required per day.
- `MAX_OFF_PER_DAY` - Maximum number of employees off per day (default: 3).

### Known Issues

- Ensure that exactly 8 employee names are provided; otherwise, an exception will be thrown.
- The application assumes the database is correctly configured and the table structure matches the expected format.

### Future Improvements

- Add a graphical interface for better user interaction.
- Implement dynamic handling of the number of employees and shifts.

## Dependencies

- [Apache POI](https://poi.apache.org/) for Excel file creation.
- PostgreSQL JDBC Driver.

## License

This project is licensed under the MIT License - see the [LICENSE](LICENSE) file for details.

---
![Ekran görüntüsü 2024-08-26 123229](https://github.com/user-attachments/assets/5be02da5-b266-4e80-abdc-f08c59f32897)
