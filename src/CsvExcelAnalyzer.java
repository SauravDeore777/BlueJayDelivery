import com.opencsv.CSVReader;
import com.opencsv.exceptions.CsvException;

import java.io.*;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;

public class CsvExcelAnalyzer {

    public static void main(String[] args) {
        String filePath = "Assignment_Timecard.xlsx - Sheet1.csv"; 

        try {
            // Set up console output redirection to a file
            PrintStream originalOut = System.out;
            FileOutputStream fileOut = new FileOutputStream("output.txt");
            PrintStream newOut = new PrintStream(new BufferedOutputStream(fileOut), true);
            System.setOut(newOut);

            // Read data from CSV file
            List<String[]> csvData = readCsv(filePath);

            // Analyze data
            analyzeData(csvData);

            // Reset the console output to the original stream
            System.setOut(originalOut);

            // Print a message to indicate that the output has been written to the file
            System.out.println("Console output has been written to output.txt");

        } catch (IOException | CsvException | ParseException e) {
            e.printStackTrace();
        }
    }

    private static List<String[]> readCsv(String filePath) throws IOException, CsvException {
        try (CSVReader csvReader = new CSVReader(new FileReader(filePath))) {
            return csvReader.readAll();
        }
    }

    private static void analyzeData(List<String[]> csvData) throws ParseException {
        int consecutiveDaysThreshold = 7;
        int timeBetweenShiftsLowerLimit = 1; // in hours
        int timeBetweenShiftsUpperLimit = 10; // in hours
        int singleShiftHoursLimit = 14;

        List<EmployeeShift> employeeShifts = new ArrayList<>();

        // Iterate through CSV data
        for (int i = 1; i < csvData.size(); i++) { // Start from 1 to skip header row
            String[] row = csvData.get(i);

            String employeeName = row[6];

            // Check if the date string is not empty
            if (!row[2].isEmpty()) {
                try {
                    Date timeIn = new SimpleDateFormat("MM/dd/yyyy hh:mm a").parse(row[2]);
                    employeeShifts.add(new EmployeeShift(employeeName, timeIn));
                } catch (ParseException e) {
                    // Handle the ParseException (invalid date format)
                    System.out.println("Error parsing date: " + row[2]);
                    e.printStackTrace();
                }
            }
        }

        // Analyze data
        for (EmployeeShift employeeShift : employeeShifts) {
            // Check for employees who worked for 7 consecutive days
            if (hasConsecutiveDays(employeeShifts, employeeShift, consecutiveDaysThreshold)) {
                System.out.println("Employee " + employeeShift.getEmployeeName() +
                        " has worked for " + consecutiveDaysThreshold + " consecutive days.");
            }

            // Check for employees with less than 10 hours between shifts but greater than 1 hour
            if (hasShortBreaks(employeeShifts, employeeShift, timeBetweenShiftsLowerLimit, timeBetweenShiftsUpperLimit)) {
                System.out.println("Employee " + employeeShift.getEmployeeName() +
                        " has short breaks between shifts.");
            }

            // Check for employees who worked for more than 14 hours in a single shift
            if (hasLongShift(employeeShift, singleShiftHoursLimit)) {
                System.out.println("Employee " + employeeShift.getEmployeeName() +
                        " has worked for more than " + singleShiftHoursLimit + " hours in a single shift.");
            }
        }
    }

    private static boolean hasConsecutiveDays(List<EmployeeShift> shifts, EmployeeShift currentShift, int consecutiveDaysThreshold) throws ParseException {
        SimpleDateFormat sdf = new SimpleDateFormat("MM/dd/yyyy");
        long currentShiftDays = sdf.parse(sdf.format(currentShift.getTimeIn())).getTime();

        for (EmployeeShift shift : shifts) {
            long shiftDays = sdf.parse(sdf.format(shift.getTimeIn())).getTime();
            long diffInDays = Math.abs(shiftDays - currentShiftDays) / (24 * 60 * 60 * 1000);

            if (diffInDays >= consecutiveDaysThreshold - 1) {
                return true;
            }
        }

        return false;
    }

    private static boolean hasShortBreaks(List<EmployeeShift> shifts, EmployeeShift currentShift, int lowerLimit, int upperLimit) {
        for (EmployeeShift shift : shifts) {
            long diffInHours = Math.abs(currentShift.getTimeIn().getTime() - shift.getTimeIn().getTime()) / (60 * 60 * 1000);

            if (diffInHours > lowerLimit && diffInHours < upperLimit) {
                return true;
            }
        }

        return false;
    }

    private static boolean hasLongShift(EmployeeShift shift, int limit) {
        long diffInHours = Math.abs(shift.getTimeIn().getTime() - new Date().getTime()) / (60 * 60 * 1000);

        return diffInHours > limit;
    }

    private static class EmployeeShift {
        private String employeeName;
        private Date timeIn;

        public EmployeeShift(String employeeName, Date timeIn) {
            this.employeeName = employeeName;
            this.timeIn = timeIn;
        }

        public String getEmployeeName() {
            return employeeName;
        }

        public Date getTimeIn() {
            return timeIn;
        }
    }
}
