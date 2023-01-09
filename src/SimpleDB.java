import java.io.*;
import java.util.*;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Iterator;
import java.util.List;



public class SimpleDB {
    private final File dbFile;
    Map<String, Table> tables;
    String regexCleaner = "[\\[\\],() \"']";

    public static void main(String[] args) {
        try {
            SimpleDB db = new SimpleDB("Students");
            db.executeSQL("CREATE TABLE stud (name, surname, age)");
            db.executeSQL("CREATE TABLE teacher (name, surname, age)");
            db.executeSQL("INSERT INTO stud VALUES (Pierre, Papin, 58)");
            db.executeSQL("INSERT INTO teacher VALUES (Yo, wtf, 50)");
            db.executeSQL("SELECT * FROM stud");
            db.executeSQL("UPDATE stud SET name = Jean, surname = Michel WHERE name = Pierre");
            db.executeSQL("SELECT * FROM teacher");
        }catch (Exception e){
            System.out.println(e);
        }

    }

    public SimpleDB(String fileName) throws IOException {
        dbFile = new File(fileName);
        tables = new HashMap<>();

        if (dbFile.exists()) {
            // Load existing database
            try (BufferedReader reader = new BufferedReader(new FileReader(dbFile))) {
                String line;
                Table currentTable = null;
                while ((line = reader.readLine()) != null) {
                    if (line.startsWith("CREATE TABLE")) {
                        // This line creates a new table
                        String[] parts = line.split(" ");
                        String tableName = parts[2];
                        currentTable = new Table(tableName, getColumnParts(parts, 3));
                        tables.put(tableName, currentTable);
                    }
                    else if (line.startsWith("INSERT INTO")) {
                        // This line inserts a new row into the current table
                        String[] parts = line.split(" ");
                        String tableName = parts[2];
                        currentTable = tables.get(tableName);
                        if (currentTable == null) {
                            throw new IOException("Invalid database file: table " + tableName + " does not exist");
                        }
                        String[] values = getColumnParts(parts, 4);
                        currentTable.addRow(values);
                    }
                }
            }
        }
        else {
            // Create new database file
            dbFile.createNewFile();
        }
    }

    public void executeSQL(String sql) throws IOException {
        String[] parts = sql.split(" ");
        String command = parts[0];
        switch (command) {
            case "SELECT" -> handleSelect(parts);
            case "INSERT" -> handleInsert(parts);
            case "UPDATE" -> handleUpdate(parts);
            case "DELETE" -> handleDelete(parts);
            case "CREATE" -> handleCreate(parts);
            default -> throw new IOException("Invalid SQL command: " + command);
        }
    }

    private void handleSelect(String[] parts) {
        // SELECT * FROM table WHERE condition
        String tableName = parts[3];
        Table table = tables.get(tableName);
        if (table == null) {
            System.out.println("Table " + tableName + " does not exist");
            return;
        }
        List<String[]> rows = table.getRows();
        if (Arrays.asList(parts).contains("WHERE")) {
            int index = Arrays.asList(parts).indexOf("WHERE");
            // Apply WHERE condition
            String condition = String.join(" ", getColumnParts(parts, index+1));
            rows = table.applyWhere(condition);
        }
        for (String[] row : rows) {
            System.out.println(Arrays.toString(row));
        }
    }

    private void handleInsert(String[] parts) throws IOException {
        // INSERT INTO table VALUES (value1, value2, ...)
        String tableName = parts[2];
        Table table = tables.get(tableName);
        if (table == null) {
            System.out.println("Table " + tableName + " does not exist");
            return;
        }
        String[] values = getColumnParts(parts, 4);
        table.addRow(values);
        try (BufferedWriter writer = new BufferedWriter(new FileWriter(dbFile, true))) {
            writer.write("INSERT INTO " + tableName + " VALUES " + Arrays.toString(getColumnParts(parts, 4)));
            writer.newLine();
        }
    }

    private void handleUpdate(String[] parts) throws IOException {
        // UPDATE table SET column1 = value1, column2 = value2 WHERE condition
        String tableName = parts[1];
        Table table = tables.get(tableName);
        if (table == null) {
            System.out.println("Table " + tableName + " does not exist");
            return;
        }
        int setIndex = Arrays.asList(parts).indexOf("SET");
        int whereIndex = Arrays.asList(parts).indexOf("WHERE");
        String setParts = String.join(" ", Arrays.copyOfRange(parts, setIndex+1, whereIndex));
        String[] changes = setParts.split(", ");
        for (String change:changes) {
            String[] splitted = change.split("=");
            String column = splitted[0];
            String value = splitted[1];
            String condition = String.join(" ", getColumnParts(parts, whereIndex+1));
            table.update(column, value, condition);
        }

        try (BufferedWriter writer = new BufferedWriter(new FileWriter(dbFile))) {
            writer.write(table.toString());
        }
    }

    private void handleDelete(String[] parts) throws IOException {
        // DELETE FROM table WHERE condition
        String tableName = parts[2];
        Table table = tables.get(tableName);
        if (table == null) {
            System.out.println("Table " + tableName + " does not exist");
            return;
        }
        String condition = String.join("", getColumnParts(parts, 4));
        table.delete(condition);
        try (BufferedWriter writer = new BufferedWriter(new FileWriter(dbFile))) {
            writer.write(table.toString());
        }
    }


    private void handleCreate(String[] parts) throws IOException {
        // CREATE TABLE table (column1 type, column2 type, ...)
        String tableName = parts[2];
        if (tables.containsKey(tableName)) {
            System.out.println("Table " + tableName + " already exists");
            return;
        }
        //String[] columnParts = parts[4].split(",");
        String[] columnParts = getColumnParts(parts, 3);
        String[] columns = new String[columnParts.length];
        System.arraycopy(columnParts, 0, columns, 0, columnParts.length);
        Table table = new Table(tableName, columns);
        tables.put(tableName, table);
        try (BufferedWriter writer = new BufferedWriter(new FileWriter(dbFile, true))) {
            writer.write("CREATE TABLE " + tableName + " " + Arrays.toString(getColumnParts(parts, 3)));
            writer.newLine();
        }
    }

    private String[] getColumnParts(String[] parts, int startIndexInclusive) {
        return getColumnParts(parts, startIndexInclusive, -1);
    }

    private String[] getColumnParts(String[] parts, int startIndexInclusive, int stopIndexExclusive) {
        if (stopIndexExclusive == -1) {
            stopIndexExclusive = parts.length;
        }
        String[] columnParts = Arrays.copyOfRange(parts, startIndexInclusive, stopIndexExclusive);
        columnParts = Arrays.stream(columnParts).map(s -> s.replaceAll(regexCleaner, "")).toArray(String[]::new);
        return columnParts;
    }

    class Table {
        private final String name;
        private final String[] columns;
        private final List<String[]> rows;

        public Table(String name, String[] columns) {
            this.name = name;
            this.columns = columns;
            this.rows = new ArrayList<>();
        }

        public Table(String name) {
            this.name = name;
            this.columns = null;
            this.rows = new ArrayList<>();
        }

        public void addRow(String[] values) {
            if (values.length != columns.length) {
                throw new IllegalArgumentException("Invalid number of values");
            }
            rows.add(values);
        }

        public List<String[]> getRows() {
            return rows;
        }

        public String[] getColumns() {
            return columns;
        }

        public List<String[]> applyWhere(String condition) {
            List<String[]> result = new ArrayList<>();
            for (String[] row : rows) {
                if (evaluateWhere(row, condition)) {
                    result.add(row);
                }
            }
            return result;
        }

        private boolean evaluateWhere(String[] row, String condition) {
            String[] parts = condition.split(" ");
            String column = parts[0];
            String operator = parts[1];
            String value = parts[2];

            int columnIndex = getColumnIndex(column);
            String cellValue = row[columnIndex];

            return switch (operator) {
                case "=" -> cellValue.equals(value);
                case "<" -> Integer.parseInt(cellValue) < Integer.parseInt(value);
                case ">" -> Integer.parseInt(cellValue) > Integer.parseInt(value);
                default -> throw new IllegalArgumentException("Invalid operator: " + operator);
            };
        }

        public void update(String column, String value, String condition) {
            for (String[] row : rows) {
                if (evaluateWhere(row, condition)) {
                    int columnIndex = getColumnIndex(column.strip());
                    row[columnIndex] = value.strip();
                }
            }
        }

        public void delete(String condition) {
            Iterator<String[]> iterator = rows.iterator();
            while (iterator.hasNext()) {
                String[] row = iterator.next();
                if (evaluateWhere(row, condition)) {
                    iterator.remove();
                }
            }
        }

        private int getColumnIndex(String column) {
            for (int i = 0; i < columns.length; i++) {
                if (columns[i].equals(column)) {
                    return i;
                }
            }
            throw new IllegalArgumentException("Invalid column: " + column);
        }

        @Override
        public String toString() {
            StringBuilder sb = new StringBuilder();

            // Print column names
            sb.append(String.join(", ", columns)).append("\n");

            // Print rows
            for (String[] row : rows) {
                sb.append(String.join(", ", row)).append("\n");
            }

            return sb.toString();
        }
    }



}
