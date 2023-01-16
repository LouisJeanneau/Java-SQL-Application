import org.apache.commons.lang3.ArrayUtils;

import java.util.Arrays;
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
        // TODO more demo
        String demo = """
                help
                SELECT * FROM notable
                CREATE TABLE stud (name, surname, age)
                CREATE TABLE stud (name, surname, age, country)
                INSERT INTO stud VALUES (Louis, Jeanneau, 22, France)
                INSERT INTO stud VALUES (Alexis, Moreau, 22, France), (Antoine, Lucien, 23, France)
                INSERT INTO stud VALUES (not, enough, values)
                INSERT INTO stud VALUES (Alexis, Perrier, 21, France), (Alexis, Gravier, 22, France), (Alxse, typo, 20, France), (Fernando, Alonso, 44, Spain), (Michael, Schumacher, 45, Germany)
                SELECT * FROM stud
                UPDATE stud SET name = 'Alexis', surname = 'Dupont' WHERE surname = 'typo'
                SELECT * FROM stud WHERE name = 'Alexis'
                SELECT surname, age FROM stud WHERE name = 'Alexis'
                UPDATE stud SET name = 'Pierre' WHERE name = 'Alexis'
                SELECT name, surname, age FROM stud GROUP BY name
                DELETE FROM stud WHERE country = Spain
                DELETE FROM stud WHERE age = 44
                SELECT * FROM stud
                
                CREATE TABLE pilot (name, surname, age)
                INSERT INTO pilot VALUES (Fernando, Alonso, 44), (Lewis, Hamilton, 41)
                SELECT * FROM pilot
                """;

        Arrays.stream(demo.split("\\r?\\n")).forEach(db::executeSQL);
    }

    private static void cli() throws Exception {
        System.out.println("Command-line Interface mode. Type 'exit' to exit.");

        SimpleDB db = new SimpleDB("");

        String enteredLine;
        while (true){
            switch (enteredLine = scanner.nextLine()) {
                case "" -> System.out.println("Empty query");
                case "help" -> System.out.println("""
                        You have access to the following commands :
                        \tCREATE TABLE name (column1, column2, ...)
                        \tINSERT INTO name VALUES (value1, value2, ...),  ...
                        \tUPDATE name SET column = 'newValue' WHERE condition
                        \tDELETE FROM name WHERE condition
                        \tSELECT * FROM name WHERE condition GROUP BY column1
                        \t\tWHERE clause, GROUP BY clause are optionals
                        \t\tYou can specify columns desired by replacing '*'
                        """);
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