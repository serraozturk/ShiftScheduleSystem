package test;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

public class Employee {
    private String name;
    private final String[] shifts = new String[7];
    private String assignedShift;
    private final boolean[] isOff = new boolean[7];
    private final List<Integer> specialLeaveDays = new ArrayList<>();
    private List<Integer> specialLeaveWeeks;

    public Employee(String name) {
        this.name = name;
        Arrays.fill(shifts, "Off");
    }

    public String getName() {
        return name;
    }
    public List<Integer> getSpecialLeaveWeeks() {
        return specialLeaveWeeks;
    }

    public void setSpecialLeaveWeeks(List<Integer> specialLeaveWeeks) {
        this.specialLeaveWeeks = specialLeaveWeeks;
    }


    public void setAssignedShift(String shift) {
        this.assignedShift = shift;
    }

    public void setShift(int day, String shift) {
        if (shift.equals("IZINLI")) {
            isOff[day] = false; // İzinli günlerde 'off' durumu yanlış olabilir
        }
        shifts[day] = shift;


    }

    public void setOff(int day) {
        isOff[day] = true;
        shifts[day] = "Off";
    }

    public boolean isOff(int day) {
        return isOff[day];
    }

    public String getShift(int day) {
        return shifts[day];
    }

    public void setName(String name) {
        this.name = name;
    }

    public void setSpecialLeaveDays(List<Integer> days) {
        this.specialLeaveDays.clear();
        this.specialLeaveDays.addAll(days);
    }

    public List<Integer> getSpecialLeaveDays() {
        return specialLeaveDays;
    }

    public String getSpecialLeaveDaysString() {
        return String.join(",", specialLeaveDays.toString());
    }
    public void resetOffDays() {
        Arrays.fill(isOff, false);  // tüm günlerde izin durumunu false olarak ayarlar
        Arrays.fill(shifts, "Off"); // tüm günleri "Off" olarak ayarlar
    }

}