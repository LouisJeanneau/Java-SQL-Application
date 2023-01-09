import FileDatabase.Database;

import java.io.File;
import java.util.Arrays;
import java.util.List;

public class Main {
    public static void main(String[] args) {
        try {
            // db CREATE
            Database db = new Database("Students", new String[]{"Name", "Surname", "Age"});
            // db INSERT
            db.addRecord(new String[]{"John", "Smith", "45"});
            db.addRecord(new String[]{"Pablo", "Picasso", "104"});
            db.addRecord(new String[]{"Pablo", "Dali", "104"});
            // db SELECT *
            printQueryResult(db.SelectByColumnNumber(new int[]{}));
            // db DELETE
            db.deleteRecord(new String[]{"Pablo", "Dali", "104"});
            // db SELECT
            printQueryResult(db.SelectByColumnNumber(new int[]{1,2,0,1}));
            // db UPDATE
            db.addRecord(new String[]{"Pablo", "Dali", "104"});
            db.updateRecord(new String[]{"Pablo", "Dali", "104"}, new String[]{"Jean", "Michel", "22"});
            printQueryResult(db.SelectByColumnNumber(new int[]{}));


        }
        catch(Exception e) {
            System.out.println(e);
        }

        File f = new File("Students");
        f.delete();
    }

    private static void printQueryResult(List<String[]> res) {
        System.out.println("== Table Print ==");
        for (String[] array : res) {
            System.out.println(Arrays.toString(array));
        }
        System.out.println("");
    }
}