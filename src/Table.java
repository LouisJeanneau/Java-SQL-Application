import com.opencsv.CSVReader;
import com.opencsv.CSVWriter;

import java.io.FileReader;
import java.io.FileWriter;
import java.io.Reader;
import java.io.Writer;
import java.util.ArrayList;
import java.util.Iterator;
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

    public void update(String column, String value, String columnWhere, String valueWhere) {
        for (String[] row : rows) {
            if (valueWhere.equals(row[getColumnIndex(columnWhere)])) {
                row[getColumnIndex(column)] = value;
            }
        }
    }

    public void update(String[] columns, String[] values, String[] columnWhere, String[] valueWhere) {
        for (String[] row : rows) {
            boolean doUpdate = true;
            for (int i = 0; i < columnWhere.length; i++) {
                if (!valueWhere[i].equals(row[getColumnIndex(columnWhere[i])])) {
                    doUpdate = false;
                }
            }
            if (doUpdate) {
                for (int i = 0; i < columns.length; i++) {
                    row[getColumnIndex(columns[i])] = values[i];
                }

            }

        }
    }

    public void delete(String column, String value) {
        Iterator<String[]> iterator = rows.iterator();
        while (iterator.hasNext()) {
            String[] row = iterator.next();
            if (value.equals(row[getColumnIndex(column)])) {
                iterator.remove();
            }
        }
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
        Table table = null;

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
        return -1;
    }

    public int[] getColumnsIndex(String[] columns){
        int[] res = new int[columns.length];
        for (int i = 0; i < columns.length; i++) {
            res[i] = getColumnIndex(columns[i]);
        }
        return res;
    }

}
