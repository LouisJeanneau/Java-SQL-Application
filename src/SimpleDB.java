import java.io.File;
import java.io.FilenameFilter;
import java.util.*;
import java.util.concurrent.CancellationException;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class SimpleDB {
    private static final String CREATE_TABLE_REGEX = "CREATE TABLE (\\w+) \\(([\\w, ]+)\\)";
    private static final String INSERT_REGEX = "INSERT INTO (\\w+) VALUES \\(([\\w ,']+)\\)";
    private static final String UPDATE_REGEX = "UPDATE (\\w+) SET (((\\w+) ?= ?'([\\w ]+)' *,* *)+).*";
    private static final String DELETE_REGEX = "DELETE FROM (\\w+)(.*)";
    private static final String SELECT_REGEX = "SELECT ([\\w, ]+|\\*) FROM (\\w+)";
    private static final String WHERE_REGEX = "WHERE (((\\w+) = '(\\w+)' *(AND)* *)+)";

    Map<String, Table> tables;

    public SimpleDB(String folderName) throws Exception {
        tables = new HashMap<>();
        // Load existing tables from file
        loadFromFile(folderName);
    }

    /**
     * @param sql
     */
    public void executeSQL(String sql) {
        System.out.println("You typed : " + sql);
        try {
            // Create table
            if (sql.matches(CREATE_TABLE_REGEX)) {
                handleCreate(sql);
            }
            // Insert row
            else if (sql.matches(INSERT_REGEX)) {
                handleInsert(sql);
            }
            // Update row
            else if (sql.matches(UPDATE_REGEX)) {
                handleUpdate(sql);
            }
            // Delete row
            else if (sql.matches(DELETE_REGEX)) {
                handleDelete(sql);
            }
            // Select rows
            else if (sql.matches(SELECT_REGEX)) {
                handleSelect(sql);
            } else {
                System.out.println("Statement not recognized");
            }
        } catch (CancellationException e) {
            System.out.println(e.getMessage());
        } catch (IllegalArgumentException e) {
            System.out.println("Your prompt is invalid : " + e.getMessage());
        } catch (NullPointerException e){
            System.out.println("This table does not exist");
        }
    }

    /**
     * Table creation, with overwrite check and confirmation
     * @param sql valid CREATE sql query
     */
    private void handleCreate(String sql) {
        // Parse table name and column names from SQL
        Matcher m = Pattern.compile(CREATE_TABLE_REGEX).matcher(sql);
        m.find();
        String tableName = m.group(1);
        String[] columnNames = m.group(2).split(",");

        // Trim leading/trailing whitespace from column names
        columnNames = Arrays.stream(columnNames).map(String::trim).toArray(String[]::new);

        // Override check and validation by user
        if (tables.containsKey(tableName)) {
            System.out.println("This table already exists. This command will overwrite the existing table. Do you agree ?");
            if (!Main.userConfirmation()) {
                throw new CancellationException("Creation of table canceled by user");
            }
        }

        // Create new table
        Table table = new Table(columnNames);
        tables.put(tableName, table);

        // Saving to files
        onExecutionSaving(tableName);
    }


    /**
     * Insert into a table a UNIQUE line of values
     *
     * @param sql valid INSERT INTO sql query
     */
    private void handleInsert(String sql) {
        // Parse table name and values from SQL
        Matcher m = Pattern.compile(INSERT_REGEX).matcher(sql);
        m.find();
        String tableName = m.group(1);
        String[] values = m.group(2).split(",");

        // Trim leading/trailing whitespace from values
        values = Arrays.stream(values).map(String::trim).toArray(String[]::new);

        // Insert row into table
        tables.get(tableName).insert(values);

        // Saving to files
        onExecutionSaving(tableName);
    }


    /**
     * extract the lines according to the query from the table<
     *
     * @param sql   sql query with the WHERE
     * @param table the table to extract lines from
     * @return
     */
    private List<String[]> handleWhere(String sql, Table table) {
        // Extract conditions in a List of String[]
        String[] conditionsString = Pattern.compile(WHERE_REGEX).matcher(sql).results().map(ma -> ma.group(1)).findFirst().orElse("").split("AND");
        List<String[]> conditions = new ArrayList<>();
        for (String condition : conditionsString) {
            conditions.add(Arrays.stream(condition.split("=")).map(c -> c.replaceAll("^[ ']+|[ ']+$", "")).toArray(String[]::new));
        }

        // Extract columns indexes
        String[] columns = conditions.stream().map(c -> c[0]).toArray(String[]::new);
        int[] columnsIndex = table.getColumnsIndex(columns);

        // Do the actual filtering
        List<String[]> filteredRows = new ArrayList<>();
        for (String[] row : table.getRows()) {
            boolean isValid = true;
            for (int i = 0; i < columnsIndex.length; i++) {
                if (!row[columnsIndex[i]].equals(conditions.get(i)[1])) {
                    isValid = false;
                    break;
                }
            }
            if (isValid) {
                filteredRows.add(row.clone());
            }

        }

        return filteredRows;
    }

    private void handleUpdate(String sql) {
        // Parse table name, column name, value, and WHERE clause from SQL
        Matcher m = Pattern.compile(UPDATE_REGEX).matcher(sql);
        m.find();
        String tableName = m.group(1);
        String[] updates = m.group(2).split(",");
        String[] updateColumns = Arrays.stream(updates).map(c -> c.split("=")[0].trim()).toArray(String[]::new);
        String[] updateValues = Arrays.stream(updates).map(c -> c.split("=")[1].trim().replace("'", "")).toArray(String[]::new);

        // TODO : extract WHERE reading
        String[] conditions = Pattern.compile(WHERE_REGEX).matcher(sql).results().map(ma -> ma.group(1)).findFirst().orElse("").split("AND");
        String[] whereColumns = Arrays.stream(conditions).map(c -> c.split("=")[0].trim()).toArray(String[]::new);
        String[] whereValues = Arrays.stream(conditions).map(c -> c.split("=")[1].trim().replace("'", "")).toArray(String[]::new);

        // Update rows in table
        tables.get(tableName).update(updateColumns, updateValues, whereColumns, whereValues);

        onExecutionSaving(tableName);
    }

    private void handleDelete(String sql) {
        // Parse table name and WHERE clause from SQL
        Matcher m = Pattern.compile(DELETE_REGEX).matcher(sql);
        m.find();
        String tableName = m.group(1);
        String whereColumn = m.group(2);
        String whereValue = m.group(3);

        // TODO : multiple WHERE cond
        // Delete rows from table
        tables.get(tableName).delete(whereColumn, whereValue);

        onExecutionSaving(tableName);
    }

    private void handleSelect(String sql) {
        // Parse table name and WHERE clause from SQL
        Matcher m = Pattern.compile(SELECT_REGEX).matcher(sql);
        m.find();
        String tableName = m.group(2);
        String columns = m.group(1);

        // TODO : multiple WHERE cond
        if (sql.contains("WHERE")) {
            Matcher mbis = Pattern.compile(WHERE_REGEX).matcher(sql);
            mbis.find();
            String whereColumn = m.group(2);
            String whereValue = m.group(3);
            // Select rows from table
            Table table = tables.get(tableName);
            for (String[] row : table.getRows()) {
                if (evaluateWhere(row, whereColumn, whereValue)) {
                    System.out.println(Arrays.toString(row));
                }
            }
        }


        // Select rows from table
        Table table = tables.get(tableName);
        for (String[] row : table.getRows()) {
            System.out.println(Arrays.toString(row));
        }
    }

    private boolean evaluateWhere(String[] row, String column, String value) throws IllegalArgumentException {
        // Get index of column in row
        int index = -1;
        for (int i = 0; i < row.length; i++) {
            if (column.equals(row[i])) {
                index = i;
                break;
            }
        }
        if (index == -1) {
            throw new IllegalArgumentException("Column not found: " + column);
        }

        // Compare value at index to WHERE value
        return value.equals(row[index]);
    }

    private boolean evaluateWhere(String[] row, String[] columns, String[] values) throws IllegalArgumentException {
        if (columns.length != values.length) {
            throw new IllegalArgumentException("Number of columns and values not matching");
        }
        for (int i = 0; i < columns.length; i++) {
            if (!evaluateWhere(row, columns[i], values[i])) {
                return false;
            }
        }
        return true;
    }


    private void loadFromFile(String folderName) throws Exception {
        // Get list of CSV files in directory
        File dir = new File("." + folderName + "\\");
        File[] csvFiles = dir.listFiles(new FilenameFilter() {
            public boolean accept(File dir, String name) {
                return name.endsWith(".csv");
            }
        });
        if (csvFiles != null) {
            // Load tables from CSV files
            for (File csvFile : csvFiles) {
                Table table = Table.loadFromCSV(csvFile.getAbsolutePath());
                tables.put(csvFile.getName().replace(".csv", ""), table);
            }
        }
    }

    private void onExecutionSaving(String tableName) {
        try {
            tables.get(tableName).saveToCSV(tableName + ".csv");
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }
}
