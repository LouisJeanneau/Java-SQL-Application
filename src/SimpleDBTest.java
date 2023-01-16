import org.junit.Before;
import org.junit.Test;

import java.io.File;

import static org.junit.Assert.*;

public class SimpleDBTest {
    private static final String DB_FILE = "test";
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
    public void testCreateTable(){
        db.executeSQL("CREATE TABLE test (col1, col2)");
        assertEquals(1, db.tables.size());
        Table table = db.tables.get("test");
        assertNotNull(table);
        assertArrayEquals(new String[]{"col1", "col2"}, table.getColumns());
        assertEquals(0, table.getRows().size());
    }
}