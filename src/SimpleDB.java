import java.io.File;
import java.util.*;
import java.util.concurrent.CancellationException;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.IntStream;

public class SimpleDB {
    private static final String CREATE_TABLE_REGEX = "CREATE TABLE (\\w+) \\(([\\w, ]+)\\)";
    private static final String INSERT_REGEX = "INSERT INTO (\\w+) VALUES \\(([\\w ,']+)\\)";
    private static final String UPDATE_REGEX = "UPDATE (\\w+) SET (((\\w+) ?= ?'([\\w ]+)' *,* *)+).*";
    private static final String DELETE_REGEX = "DELETE FROM (\\w+)(.*)";
    private static final String SELECT_REGEX = "SELECT ([\\w, ]+|\\*) FROM (\\w+)(.*)";
    private static final String WHERE_REGEX = "WHERE (((\\w+)*= *'(\\w+)' *(AND)* *)+)";

    private static final String GROUP_REGEX = "GROUP BY ((\\w+) *,* *)+";
    private static final String TRIM_REGEX = "^[ '\"]+|[ '\"]+$";

    Map<String, Table> tables;

    public SimpleDB(String folderName) throws Exception {
        tables = new HashMap<>();
        // Load existing tables from file
        loadFromFile(folderName);
    }

    /**
     * @param sql the SQL query to execute
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
        values = Arrays.stream(values).map(c -> c.replaceAll(TRIM_REGEX, "")).toArray(String[]::new);

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
     * @return the line filtered from the table according to the conditions
     */
    private List<String[]> handleWhere(String sql, Table table) {
        // Extract conditions in a List of String[]
        String[] conditionsString = Pattern.compile(WHERE_REGEX).matcher(sql).results().map(ma -> ma.group(1)).findFirst().orElse("").split("AND");
        List<String[]> conditions = new ArrayList<>();
        for (String condition : conditionsString) {
            conditions.add(Arrays.stream(condition.split("=")).map(c -> c.replaceAll(TRIM_REGEX, "")).toArray(String[]::new));
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

    /**
     * Update the table
     * @param sql a valid UPDATE query
     */
    private void handleUpdate(String sql) {
        // Parse table name, column name, value, and WHERE clause from SQL
        Matcher m = Pattern.compile(UPDATE_REGEX).matcher(sql);
        m.find();
        String tableName = m.group(1);
        String[] updates = m.group(2).split(",");
        String[] updateColumns = Arrays.stream(updates).map(c -> c.split("=")[0].trim()).toArray(String[]::new);
        String[] updateValues = Arrays.stream(updates).map(c -> c.split("=")[1].trim().replace("'", "")).toArray(String[]::new);

        Table table = tables.get(tableName);
        List<String[]> rows = handleWhere(sql, table);
        // Update rows in table
         if (table.update(rows, updateColumns, updateValues)){
             System.out.println(rows.size() + " row(s) updated");
         }
         else {
             System.out.println("0 row updated");
         }

        onExecutionSaving(tableName);
    }

    /**
     * Delete element matching the WHERE condition. If no WHERE condition, delete everything
     * @param sql valid DELETE FROM query
     */
    private void handleDelete(String sql) {
        // Parse table name and WHERE clause from SQL
        Matcher m = Pattern.compile(DELETE_REGEX).matcher(sql);
        m.find();
        String tableName = m.group(1);
        String whereSQL = m.group(2);

        // Get table
        Table table = tables.get(tableName);

        List<String[]> selectedRows;
        // Delete everything ?
        if (whereSQL.equals("")){
            System.out.println("You are about to delete the whole " + tableName + " table.");
            if (!Main.userConfirmation()) {
                throw new CancellationException("Canceled deletion of the whole table.");
            }
            selectedRows = table.getRows();

        } // Select the line according to conditions
        else {
            selectedRows = handleWhere(whereSQL, table);
        }

        // Delete rows from table
        if (table.deleteRows(selectedRows)){
            System.out.println(selectedRows.size() + " row(s) deleted");
        }
        else {
            System.out.println("0 row deleted");
        }

        // Saving
        onExecutionSaving(tableName);
    }

    /**
     * Print the result of your query
     * @param sql a SELECT query
     */
    private void handleSelect(String sql) {
        // Parse table name and WHERE clause from SQL
        Matcher m = Pattern.compile(SELECT_REGEX).matcher(sql);
        m.find();
        String tableName = m.group(2);
        String columnsString = m.group(1);
        String whereSQL = m.group(3);

        // Get table
        Table table = tables.get(tableName);

        // Select rows if WHERE condition
        List<String[]> rows;
        m = Pattern.compile(WHERE_REGEX).matcher(sql);
        if (m.find()) {
            rows = handleWhere(whereSQL, table);
        }
        else {
            rows = tables.get(tableName).getRows();
        }

        // GROUP BY
        m = Pattern.compile(GROUP_REGEX).matcher(sql);
        if (m.find()){
            rows = handleGroupBy(m.group(1), rows, table);
        }

        // Extract columns indexes if not *
        int[] columnsIndex;
        if (columnsString.equals("*")){
            columnsIndex = IntStream.range(0, table.getColumns().length).toArray();
        }
        else {
            String[] columns = Arrays.stream(columnsString.split(",")).map(c -> c.replaceAll(TRIM_REGEX, "")).toArray(String[]::new);
            columnsIndex = table.getColumnsIndex(columns);
        }

        // Print the table
        // Columns
        String[] col = table.getColumns();
        for (int index : columnsIndex) {
            System.out.printf(col[index] + ", ");
        }
        System.out.print("\b\b  \n");
        // Rows
        for (String[] row : rows) {
            for (int index : columnsIndex) {
                System.out.printf(row[index] + ", ");
            }
            System.out.print("\b\b  \n");
        }
    }

    private List<String[]> handleGroupBy(String columnsString, List<String[]> rows, Table table){
        List<String[]> resultRows = new ArrayList<>();
               
        // Extract columns indexes
        String[] columns = Arrays.stream(columnsString.split(",")).map(c -> c.replaceAll(TRIM_REGEX, "")).toArray(String[]::new);
        int[] columnsIndex = table.getColumnsIndex(columns);

        // TODO : multiple columns
        List<String> encountered = new ArrayList<>();
        boolean doCopy;
        for (String[] row : rows) {
            doCopy = true;
            for (int index : columnsIndex) {
                if (encountered.contains(row[index])) {
                    doCopy = false;
                    break;
                }
                encountered.add(row[index]);
            }
            if (doCopy){
                resultRows.add(row);
            }
        }
        
        return  resultRows;
    }

    private void loadFromFile(String folderName) throws Exception {
        // Get list of CSV files in directory
        File dir = new File("." + folderName + "\\");
        File[] csvFiles = dir.listFiles((dir1, name) -> name.endsWith(".csv"));
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
