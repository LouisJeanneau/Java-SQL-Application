import FileDatabase.Database;

import java.util.Arrays;
import java.util.List;

public class Main {
    public static void main(String[] args) {
        System.out.println("Hello world!");
        try {
            Database db = new Database("Students", new String[]{"Name", "Surname", "Age"});
            db.addRecord(new String[]{"John", "Smith", "45"});
            List<String[]> res = db.SelectByColumnNumber(new int[]{1,2,0,1});
            printQueryResult(res);
        }
        catch(Exception e) {
            System.out.println(e);
        }
    }

    private static void printQueryResult(List<String[]> res) {
        for (String[] array : res) {
            System.out.println(Arrays.toString(array));
        }
    }
}