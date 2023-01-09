package FileDatabase;

import com.opencsv.CSVReader;
import com.opencsv.CSVWriter;
import com.opencsv.exceptions.CsvValidationException;

import java.io.*;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.IntStream;

public class Database {
    private String fileName;
    private int numberOfColumns;

    public Database(String fileName, String[] columnsName) throws IOException, IllegalArgumentException {
        File file = new File(fileName);
        if (file.exists()) {
            throw new IllegalArgumentException("Database already exists");
        }
        this.fileName = fileName;
        this.numberOfColumns = columnsName.length;
        this.addRecord(columnsName);
    }

    public Database(String fileName) throws FileNotFoundException {
        this.fileName = fileName;
        try (CSVReader reader = new CSVReader(new BufferedReader(new FileReader(fileName)))) {
            String[] nextRecord = reader.readNext();
            this.numberOfColumns = nextRecord.length;
        } catch (FileNotFoundException e) {
            throw new FileNotFoundException(e.getMessage());
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }


    public void addRecord(String[] record) throws IOException, IllegalArgumentException {
        // Length verification
        if (record.length != numberOfColumns){
            throw new IllegalArgumentException("Wrong number of columns entered");
        }
        try (CSVWriter writer = new CSVWriter(new FileWriter(fileName, true))) {
            writer.writeNext(record);
        }
    }

    public void deleteRecord(String[] record) throws IOException, CsvValidationException, IllegalArgumentException {
        // chekc argument
        if (record.length != numberOfColumns){
            throw new IllegalArgumentException("Wrong length of record");
        }
        List<String[]> records = new ArrayList<>();
        try (CSVReader reader = new CSVReader(new BufferedReader(new FileReader(fileName)))) {
            String[] nextRecord;
            while ((nextRecord = reader.readNext()) != null) {
                if (!recordMatch(nextRecord, record)) {
                    records.add(nextRecord);
                }
            }
        }
        try (CSVWriter writer = new CSVWriter(new FileWriter(fileName))) {
            writer.writeAll(records);
        }
    }

    public List<String[]> queryRecords(String[] query) throws IOException, CsvValidationException {
        List<String[]> records = new ArrayList<>();
        try (CSVReader reader = new CSVReader(new BufferedReader(new FileReader(fileName)))) {
            String[] nextRecord;
            while ((nextRecord = reader.readNext()) != null) {
                if (recordMatch(nextRecord, query)) {
                    records.add(nextRecord);
                }
            }
        }
        return records;
    }


    public boolean updateRecord(String[] oldRecord, String[] newRecord) throws IOException, CsvValidationException {
        List<String[]> records = new ArrayList<>();
        try (CSVReader reader = new CSVReader(new BufferedReader(new FileReader(fileName)))) {
            String[] nextRecord;
            while ((nextRecord = reader.readNext()) != null) {
                if (recordMatch(nextRecord, oldRecord)) {
                    records.add(newRecord);
                }
                else {
                    records.add(nextRecord);
                }
            }
        }
        try (CSVWriter writer = new CSVWriter(new FileWriter(fileName))) {
            writer.writeAll(records);
        }
        return true;
    }

    private boolean recordMatch(String[] record1, String[] record2) {
        if (record1.length != record2.length) {
            return false;
        }
        for (int i = 0; i < record1.length; i++) {
            if (record2[i] != null && !record2[i].equals(record1[i])) {
                return false;
            }
        }
        return true;
    }


    public List<String[]> SelectByColumnNumber(int[] numbers) throws IOException, IllegalArgumentException, CsvValidationException {
        // empty parameter is a solution to select everything
        if (numbers.length == 0){
            numbers = IntStream.range(0, numberOfColumns).toArray();
        }
        // Check if there is a wrong parameter
        for (int number:numbers) {
            if (number >= numberOfColumns){
                throw new IllegalArgumentException("Column index out-of-range of table");
            }
            else if (number < 0){
                throw new IllegalArgumentException("Column index can't be negative");
            }
        }
        int numberOfSelectedColumns = numbers.length;
        // Actual reading part

        List<String[]> records = new ArrayList<>();
        try (CSVReader reader = new CSVReader(new BufferedReader(new FileReader(fileName)))) {
            String[] nextRecord;
            while ((nextRecord = reader.readNext()) != null) {
                List<String> record = new ArrayList<String>(numberOfSelectedColumns);
                for (int number:numbers) {
                    record.add(nextRecord[number]);
                }
                records.add(record.toArray(new String[0]));
            }
        }
        return records;
    }
}
