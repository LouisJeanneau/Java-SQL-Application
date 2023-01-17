import java.io.File;
import java.util.*;
import java.util.concurrent.CancellationException;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.IntStream;

public class SimpleDB {
    //region REGEX
    private static final String CREATE_TABLE_REGEX = "CREATE TABLE (\\w+) \\(([\\w, ]+)\\)";
    private static final String INSERT_REGEX = "INSERT INTO (\\w+) VALUES ((\\(([\\w ,']+)\\),* *)+)";
    private static final String UPDATE_REGEX = "UPDATE (\\w+) SET (((\\w+) ?= ?'([\\w ]+)' *,* *)+).*";
    private static final String DELETE_REGEX = "DELETE FROM (\\w+)(.*)";
    private static final String SELECT_REGEX = "SELECT ([\\w, ]+|\\*) FROM (.+) ?(?:(WHERE)|(GROUP BY))?";
    private static final String WHERE_REGEX = "WHERE ((?:\\w+ ?= ?'\\w+' *(?:AND)* *)+)";
    private static final String GROUP_REGEX = "GROUP BY ((?:\\w+ *,* *)+)";
    private static final String TRIM_REGEX = "^[( '\"]+|[) '\"]+$";
    //endregion

    Map<String, Table> tables;

    public SimpleDB(String folderName) throws Exception {
        tables = new HashMap<>();
        // Load existing tables from file
        loadFromFile(folderName);
    }

    /** execute a SQL query
     * @param sql the SQL query to execute
     */
    public void executeSQL(String sql) {
        System.out.println("You typed : " + ConsoleColors.GREEN + sql + ConsoleColors.RESET);
        sql = sql.trim();
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
                System.out.println(ConsoleColors.RED +  "Statement not recognized" + ConsoleColors.RESET);
            }
        } catch (CancellationException e) {
            System.out.println(e.getMessage());
        } catch (IllegalArgumentException e) {
            System.out.println(ConsoleColors.RED_BOLD +  "Your prompt is invalid : " + ConsoleColors.RED + e.getMessage() + ConsoleColors.RESET);
        } catch (NullPointerException e) {
            System.out.println("This table does not exist");
        }
        System.out.println();
    }

    //region TRIVIAL HANDLES
    /**
     * Table creation, with overwrite check and confirmation
     *
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

        // Overwrite check and validation by user
        if (tables.containsKey(tableName)) {
            System.out.println("This table already exists. This command will overwrite the existing table. Do you agree ?");
            if (Main.userCancellation()) {
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
        String[] values = m.group(2).split("\\), *\\(");
        String[] singleValue;

        int i = 0;
        // for each tuple of value :
        for (String v : values) {
            // Trim leading/trailing whitespace from the tuple of value
            singleValue = Arrays.stream(v.split(",")).map(c -> c.replaceAll(TRIM_REGEX, "")).toArray(String[]::new);

            // Insert row into table
            if (tables.get(tableName).insert(singleValue))
                i += 1;
        }
        // Information
        System.out.println(i + " row(s) inserted");

        // Saving to files
        onExecutionSaving(tableName);
    }

    /**
     * Update the table
     *
     * @param sql a valid UPDATE query
     */
    private void handleUpdate(String sql) {
        // Parse table name, column name, value, and WHERE clause from SQL
        Matcher m = Pattern.compile(UPDATE_REGEX).matcher(sql);
        m.find();
        String tableName = m.group(1);
        String[] updates = m.group(2).split(",");

        // Put all the column name in an array, and new values in another
        String[] updateColumns = Arrays.stream(updates).map(c -> c.split("=")[0].trim()).toArray(String[]::new);
        String[] updateValues = Arrays.stream(updates).map(c -> c.split("=")[1].trim().replaceAll(TRIM_REGEX, "")).toArray(String[]::new);

        // Get the table
        Table table = tables.get(tableName);

        // Handle the WHERE clause
        List<String[]> rows = handleWhere(sql, table);

        // Update rows in table
        if (table.update(rows, updateColumns, updateValues)) {
            System.out.println(rows.size() + " row(s) updated");
        } else {
            System.out.println("0 row updated");
        }

        onExecutionSaving(tableName);
    }

    /**
     * Delete element matching the WHERE condition. If no WHERE condition, delete everything
     *
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
        if (Pattern.compile(WHERE_REGEX).matcher(whereSQL).find()) {
            // Select the line according to conditions
            selectedRows = handleWhere(whereSQL, table);
        }
        else {
            // Ask for confirmation
            System.out.println(ConsoleColors.RED + "You are about to delete the whole " + tableName + " table." + ConsoleColors.RESET);
            if (Main.userCancellation()) {
                throw new CancellationException("Canceled deletion of the whole table.");
            }
            selectedRows = table.getRows();

        }


        // Delete rows from table
        if (table.deleteRows(selectedRows)) {
            System.out.println(selectedRows.size() + " row(s) deleted");
        } else {
            System.out.println("0 row deleted");
        }

        // Saving
        onExecutionSaving(tableName);
    }

    /**
     * Print the result of your query
     *
     * @param sql a SELECT query
     */
    private void handleSelect(String sql) {
        // Extract selected columns from the sql
        Matcher m = Pattern.compile(SELECT_REGEX).matcher(sql);
        m.find();
        String columnsString = m.group(1);
        String otherPart = m.group(2);

        // Okay, so. This is a mess. I find the name of the table(s) by looking at the shortest string when I cut of the GROUP and WHERE regex
        String[] potentialTableName = {otherPart, Pattern.compile(WHERE_REGEX).split(otherPart)[0],  Pattern.compile(GROUP_REGEX).split(otherPart)[0]};
        String tableName = Arrays.stream(potentialTableName).min(Comparator.comparingInt(String::length)).get();

        // Get table, CROSS JOIN aware
        Table table;
        if (tableName.split(",").length > 1) {
            // CROSS JOIN
            table = handleCrossJoin(tableName);
        } else {
            table = tables.get(tableName.replaceAll(TRIM_REGEX, ""));
        }

        // Select rows if WHERE condition
        List<String[]> rows;
        if (Pattern.compile(WHERE_REGEX).matcher(otherPart).find()) {
            rows = handleWhere(otherPart, table);
        } else {
            rows = table.getRows();
        }

        // GROUP BY
        if (Pattern.compile(GROUP_REGEX).matcher(otherPart).find()) {
            rows = handleGroupBy(otherPart, rows, table);
        }

        // Extract columns indexes if not *
        int[] columnsIndex;
        if (columnsString.equals("*")) {
            columnsIndex = IntStream.range(0, table.getColumns().length).toArray();
        } else {
            String[] columns = Arrays.stream(columnsString.split(",")).map(c -> c.replaceAll(TRIM_REGEX, "")).toArray(String[]::new);
            columnsIndex = table.getColumnsIndex(columns);
        }

        // Print the table
        String[] col = table.getColumns();
        System.out.printf(ConsoleColors.WHITE_BACKGROUND + ConsoleColors.BLUE);
        for (int index : columnsIndex) {
            System.out.printf(col[index] + ", ");
        }
        System.out.print(ConsoleColors.RESET + "\b\b  \n" + ConsoleColors.BLACK_BACKGROUND);
        // Rows
        for (String[] row : rows) {
            for (int index : columnsIndex) {
                System.out.printf(row[index] + ", ");
            }
            System.out.print("\b\b  \n");
        }
        System.out.printf(ConsoleColors.RESET);
    }
    //endregion

    //region FILTERING handles
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

        // Check if we found conditions
        if (conditions.get(0)[0].equals("")){
            throw new IllegalArgumentException("There is no WHERE condition while it's required, or your condition is badly written");
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

    private List<String[]> handleGroupBy(String sql, List<String[]> rows, Table table) {
        List<String[]> resultRows = new ArrayList<>();

        Matcher m = Pattern.compile(GROUP_REGEX).matcher(sql);
        m.find();
        String columnsString = m.group(1);

        // Extract columns indexes
        String[] columns = Arrays.stream(columnsString.split(",")).map(c -> c.replaceAll(TRIM_REGEX, "")).toArray(String[]::new);
        int[] columnsIndex = table.getColumnsIndex(columns);

        // Loop over each row to determine if it's a 'repeated' one or a new one
        List<String[]> encountered = new ArrayList<>();
        for (String[] row : rows) {
            List<String> rowSelectedColumn = new ArrayList<>();
            for (int index : columnsIndex) {
                rowSelectedColumn.add(row[index]);
            }
            String[] rowSelectedColumnArray = rowSelectedColumn.toArray(String[]::new);
            if (encountered.stream().noneMatch(c -> Table.equalsRow(c, rowSelectedColumnArray))) {
                resultRows.add(row.clone());
                encountered.add(rowSelectedColumnArray);
            }
        }

        return resultRows;
    }
    //endregion
    private Table handleCrossJoin(String namesString) {
        List<String> columns = new ArrayList<>();
        List<String> tablesName = Arrays.stream(namesString.split(",")).map(c -> c.replaceAll(TRIM_REGEX, "")).toList();
        for (String t : tablesName) {
            columns.addAll(List.of(tables.get(t).getColumns()));
        }

        Table joinedTable = new Table(columns.toArray(new String[0]));
        List<String[]> products = tables.get(tablesName.get(0)).getRows();
        ArrayList<String[]> newProducts = new ArrayList<>();
        for (int i = 1; i < tablesName.size(); i++) {
            for (String[] oldRow : products) {
                for (String[] row : tables.get(tablesName.get(i)).getRows()) {
                    String[] concatenatedArray = Arrays.copyOf(oldRow, oldRow.length + row.length);
                    System.arraycopy(row, 0, concatenatedArray, oldRow.length, row.length);
                    newProducts.add(concatenatedArray);
                }
            }
            products = (List<String[]>) newProducts.clone();
            newProducts.clear();
        }

        for (String[] row : products) {
            joinedTable.insert(row);
        }

        return joinedTable;
    }

    //region FILE READ/WRITE
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
            if (!tables.get(tableName).saveToCSV(tableName + ".csv"))
                System.out.println("Something went wrong while saving table " + tableName);
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }
    //endregion
}
