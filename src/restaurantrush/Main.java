package restaurantrush;

import java.io.FileNotFoundException;
import java.io.File;
import java.util.Scanner;

import restaurantrush.controller.GameConfig;
import restaurantrush.controller.GameController;
import restaurantrush.view.ConsoleView;

/**
 * Entry point. Run with no arguments to play with the keyboard, or pass a
 * script file of commands (one per line) to replay a recorded game:
 * <pre>java -cp out restaurantrush.Main runs/stage1-victory.txt</pre>
 */
public class Main {

    /**
     * @param args optional path to a command script
     */
    public static void main(String[] args) {
        Scanner in;
        boolean scripted = args.length > 0;
        if (scripted) {
            try {
                in = new Scanner(new File(args[0]));
            } catch (FileNotFoundException e) {
                System.err.println("Script file not found: " + args[0]);
                return;
            }
        } else {
            in = new Scanner(System.in);
        }
        ConsoleView view = new ConsoleView(in, System.out, scripted);
        new GameController(GameConfig.baseSetting(), view).play();
    }
}
