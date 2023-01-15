import org.apache.commons.lang3.ArrayUtils;

import java.util.Objects;
import java.util.Scanner;

public class Main {
    static Scanner scanner = new Scanner(System.in);

    public static void main(String[] args) {
        // Welcome message
        System.out.println("Welcome to SimpleDB ! A simple file-based database application developed in Java.");

        // Valid key inputs selection and user selection menu #1
        char[] correctKeys = {100, 68, 32};
        char pressedKey = 0;
        String line;
        while (!ArrayUtils.contains(correctKeys, pressedKey)) {
            System.out.println("""
                    Type an option (and then 'Enter' key) :
                    \t Spacebar : start command-line interface
                    \t D : Demo mode
                    """);
            line = scanner.nextLine();
            if (!Objects.equals(line, "")) {
                pressedKey = line.charAt(0);
            }
        }

        // Execute user choice
        switch (pressedKey){
            // Demo
            case 100:
            case 68:
                try {
                    demoQueries();
                }catch (Exception e){
                    System.out.println("Something went wrong in the demo ! :(");
                    System.out.printf(e.toString());
                }
                break;
            case 32:
                try {
                    cli();
                }catch (Exception e){
                    System.out.printf(e.toString());
                }
                break;
        }
    }

    private static void demoQueries() throws Exception {
        System.out.println("Launching the demo !\n");
        SimpleDB db = new SimpleDB("");
        db.executeSQL("CREATE TABLE stud (name, surname, age)");
        db.executeSQL("CREATE TABLE teacher (name, surname, age)");
        db.executeSQL("INSERT INTO stud VALUES (Pierre, Papin, 58)");
        db.executeSQL("INSERT INTO teacher VALUES (Yo, wtf, 50)");
        db.executeSQL("SELECT * FROM stud");
        db.executeSQL("UPDATE stud SET name = 'Jean', surname = 'Michel' WHERE name = 'Pierre'");
        db.executeSQL("SELECT * FROM teacher");
    }

    private static void cli() throws Exception {
        System.out.println("Command-line Interface mode. Type 'exit' to exit.");

        SimpleDB db = new SimpleDB("");

        String enteredLine;
        while (true){
            switch (enteredLine = scanner.nextLine()) {
                case "" -> System.out.println("Empty query");
                case "exit" -> {
                    System.out.println("Exiting...");
                    return;
                }
                default -> db.executeSQL(enteredLine);
            }
        }
    }

    public static boolean userConfirmation(){
        System.out.println("Type 'y' to confirm. Anything else to cancel.");
        char pressedKey = scanner.nextLine().charAt(0);
        return pressedKey == 'y';
    }
}