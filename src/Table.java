import com.opencsv.CSVReader;
import com.opencsv.CSVWriter;

import java.io.FileReader;
import java.io.FileWriter;
import java.io.Reader;
import java.io.Writer;
import java.util.ArrayList;
import java.util.List;

public class Table {
    private final String[] columns;
    private final List<String[]> rows;

    public Table(String[] columns) {
        this.columns = columns;
        rows = new ArrayList<>();
    }

    public String[] getColumns() {
        return columns;
    }

    public List<String[]> getRows() {
        return rows;
    }

    public void insert(String[] values) {
        if (values.length != columns.length) {
            throw new IllegalArgumentException("Invalid number of values");
        }
        rows.add(values);
    }

    public boolean update(List<String[]> rowsToUpdate, String[] columnsToUpdate, String[] valuesNew){
        boolean updated = false;
        for (String[] row : rows) {
            if (rowsToUpdate.stream().anyMatch(c -> equalsRow(row, c))){
                for (int i = 0; i < columnsToUpdate.length; i++) {
                    row[getColumnIndex(columnsToUpdate[i])] = valuesNew[i];
                }
                updated = true;
            }
        }
        return updated;
    }

    public boolean deleteWhere(String column, String value) {
        return rows.removeIf(row -> value.equals(row[getColumnIndex(column)]));
    }

    private boolean equalsRow(String[] row1, String[] row2){
        if (row1.length != row2.length) {
            return false;
        }
        for (int i = 0; i < row1.length; i++) {
            if (!row1[i].equals(row2[i])){
                return false;
            }
        }
        return true;
    }

    public boolean deleteRow(String[] row){
        return rows.removeIf(r -> equalsRow(r, row));
    }

    public boolean deleteRows(List<String[]> rowsToDelete){
        boolean modifiedRows = false;
        for (String[] strings : rowsToDelete) {
            if (deleteRow(strings)){
                modifiedRows = true;
            }
        }
        return modifiedRows;
    }

    public void saveToCSV(String fileName) throws Exception {
        try (Writer writer = new FileWriter(fileName)) {
            // Create CSV writer
            CSVWriter csvWriter = new CSVWriter(writer,
                    CSVWriter.DEFAULT_SEPARATOR,
                    CSVWriter.NO_QUOTE_CHARACTER,
                    CSVWriter.DEFAULT_ESCAPE_CHARACTER,
                    CSVWriter.DEFAULT_LINE_END);

            // Write column names
            csvWriter.writeNext(columns);
            // Write rows
            for (String[] row : rows) {
                csvWriter.writeNext(row);
            }
            csvWriter.close();
        }
    }

    public static Table loadFromCSV(String fileName) throws Exception {
        Table table;

        try (Reader reader = new FileReader(fileName)) {
            // Create CSV reader
            CSVReader csvReader = new CSVReader(reader);

            // Read column names
            String[] columns = csvReader.readNext();
            table = new Table(columns);

            // Read rows
            String[] row;
            while ((row = csvReader.readNext()) != null) {
                table.insert(row);
            }
            csvReader.close();
        }

        return table;
    }


    public int getColumnIndex(String column) {
        for (int i = 0; i < columns.length; i++) {
            if (column.equals(columns[i])) {
                return i;
            }
        }
        throw new IllegalArgumentException("Column name invalid in this table : " + column);
    }

    public int[] getColumnsIndex(String[] columns){
        int[] res = new int[columns.length];
        for (int i = 0; i < columns.length; i++) {
            res[i] = getColumnIndex(columns[i]);
        }
        return res;
    }

}
