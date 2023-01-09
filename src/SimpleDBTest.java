import org.junit.Before;
import org.junit.Test;

import java.io.File;

import static org.junit.Assert.*;

public class SimpleDBTest {
    private static final String DB_FILE = "test.db";
    private static final String CSV_FILE = "test.csv";
    private SimpleDB db;

    @Before
    public void setUp() throws Exception {
        // Create a new database for each test
        File dbFile = new File(DB_FILE);
        if (dbFile.exists()) {
            dbFile.delete();
        }
        db = new SimpleDB(DB_FILE);
    }

    @Test
    public void testCreateTable() throws Exception {
        db.executeSQL("CREATE TABLE test (col1, col2)");
        assertEquals(1, db.tables.size());
        SimpleDB.Table table = db.tables.get("test");
        assertNotNull(table);
        assertArrayEquals(new String[]{"col1", "col2"}, table.getColumns());
        assertEquals(0, table.getRows().size());
    }

    @Test
    public void testInsert() throws Exception {
        db.executeSQL("CREATE TABLE test (col1, col2)");
        db.executeSQL("INSERT INTO test VALUES ('1', 'test')");
        SimpleDB.Table table = db.tables.get("test");
        assertEquals(1, table.getRows().size());
        assertArrayEquals(new String[]{"1", "test"}, table.getRows().get(0));
    }

    @Test
    public void testUpdate() throws Exception {
        db.executeSQL("CREATE TABLE test (col1, col2)");
        db.executeSQL("INSERT INTO test VALUES ('1', 'test')");
        db.executeSQL("UPDATE test SET col2 = 'test2' WHERE col1 = '1'");
        SimpleDB.Table table = db.tables.get("test");
        assertEquals(1, table.getRows().size());
        String[] a = table.getRows().get(0);
        assertArrayEquals(new String[]{"1", "test2"}, a);
    }

    @Test
    public void testDelete() throws Exception {
        db.executeSQL("CREATE TABLE test (col1, col2)");
        db.executeSQL("INSERT INTO test VALUES ('1', 'test')");
        db.executeSQL("INSERT INTO test VALUES ('2', 'test')");
        db.executeSQL("DELETE FROM test WHERE col1 = '1'");
        SimpleDB.Table table = db.tables.get("test");
        assertEquals(1, table.getRows().size());
        assertArrayEquals(new String[]{"2", "test"}, table.getRows().get(0));
    }
}