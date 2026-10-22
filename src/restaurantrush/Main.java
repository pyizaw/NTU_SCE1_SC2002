package restaurantrush;

import java.io.FileNotFoundException;
import java.io.File;
import java.util.Scanner;

import restaurantrush.controller.GameConfig;
import restaurantrush.controller.GameController;
import restaurantrush.view.ConsoleView;
import restaurantrush.view.SwingView;

/**
 * Entry point.
 * <ul>
 *   <li>No arguments: play in the terminal.</li>
 *   <li>{@code --gui}: play in a window.</li>
 *   <li>A file path: replay a recorded game from a command script, e.g.
 *       <pre>java -cp out restaurantrush.Main runs/stage3-victory.txt</pre></li>
 * </ul>
 */
public class Main {

    /**
     * @param args optional {@code --gui} or path to a command script
     */
    public static void main(String[] args) throws Exception {
        if (args.length > 0 && args[0].equalsIgnoreCase("--gui")) {
            startWindow();
            return;
        }
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

    /**
     * Opens the window and runs the game on a separate thread, so the window
     * stays responsive while the controller waits for the player.
     */
    private static void startWindow() throws Exception {
        SwingView view = SwingView.create();
        Runnable newGame = () -> {
            Thread game = new Thread(() -> new GameController(GameConfig.baseSetting(), view).play(), "game");
            game.setDaemon(true);
            game.start();
        };
        view.setRestartAction(() -> {
            view.reset();
            newGame.run();
        });
        newGame.run();
    }
}
