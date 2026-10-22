package restaurantrush.view;

import java.io.PrintStream;
import java.util.List;
import java.util.OptionalDouble;
import java.util.Scanner;

import restaurantrush.controller.GameConfig;
import restaurantrush.controller.GameResult;
import restaurantrush.controller.StaffCommand;
import restaurantrush.model.Menu;
import restaurantrush.model.MenuItem;
import restaurantrush.model.Restaurant;
import restaurantrush.model.Table;
import restaurantrush.model.customer.Customer;
import restaurantrush.staff.Staff;
import restaurantrush.util.Money;

/**
 * Command-line interface. It only displays state and turns typed text into
 * {@link StaffCommand}s; it never decides whether a command is allowed.
 */
public class ConsoleView implements GameView {

    private static final String LINE = "------------------------------------------------------------------";
    private static final String DOUBLE_LINE = "==================================================================";

    /** Where input comes from (keyboard or a script file). */
    private final Scanner in;

    /** Where output goes. */
    private final PrintStream out;

    /** Print each command read, so scripted runs show the choices made. */
    private final boolean echoInput;

    /** Set once input runs out; remaining choices default to waiting. */
    private boolean inputEnded;

    /**
     * @param in        input source
     * @param out       output stream
     * @param echoInput true to print each command read (script mode)
     */
    public ConsoleView(Scanner in, PrintStream out, boolean echoInput) {
        this.in = in;
        this.out = out;
        this.echoInput = echoInput;
    }

    @Override
    public void showWelcome(GameConfig config, Menu menu) {
        out.println(DOUBLE_LINE);
        out.println("                     RESTAURANT RUSH");
        out.println(DOUBLE_LINE);
        out.println("You are the manager. Staff time is limited: decide who is seated,");
        out.println("whose order is taken, what is served and what the kitchen cooks.");
        out.println();
        out.println("Menu:");
        for (MenuItem item : menu.getItems()) {
            out.println("  - " + item);
        }
        out.println();
        out.println("Goal after turn " + config.getMaxTurns() + ": at least " + config.getMinPaidCustomers()
                + " paid customers, revenue at least " + Money.format(config.getMinRevenueCents())
                + ", average satisfaction of paid customers at least " + config.getMinAverageSatisfaction() + ".");
        out.println("Customers arrive at the start of turns " + config.getArrivals().keySet() + ".");
        out.println();
        printHelp();
    }

    @Override
    public void showTurnHeader(int turn, int maxTurns) {
        out.println();
        out.println(DOUBLE_LINE);
        out.println(" TURN " + turn + " / " + maxTurns);
        out.println(DOUBLE_LINE);
    }

    @Override
    public void showStatus(Restaurant restaurant, int turn, List<Staff> staff) {
        out.println(LINE);
        out.println(" Revenue: " + Money.format(restaurant.getRevenueCents())
                + "   Avg satisfaction (paid): " + formatAverage(restaurant.getAveragePaidSatisfaction())
                + "   Paid customers: " + restaurant.getPaidCount());

        StringBuilder queue = new StringBuilder();
        for (Customer c : restaurant.getWaitingQueue()) {
            queue.append(queue.length() == 0 ? "" : ", ").append(c.getLabel()).append(" [sat ").append(c.getSatisfaction()).append("]");
        }
        out.println(" Waiting queue : " + (queue.length() == 0 ? "(empty)" : queue));

        out.println(" Tables:");
        for (Table t : restaurant.getTables()) {
            out.println("   Table " + t.getNumber() + ": " + describeTable(t));
        }

        out.println(" Staff tasks available:");
        for (Staff s : staff) {
            out.printf("   %-8s: %s%n", s.getRole(), String.join(" | ", shortTasks(s.availableTasks(restaurant, turn))));
        }
        out.println(LINE);
    }

    @Override
    public StaffCommand askCommand(Staff staff, List<String> tasks) {
        while (true) {
            out.println(staff.getRole() + " options:");
            for (String task : tasks) {
                out.println("   " + task);
            }
            out.print(staff.getRole() + " > ");
            String line = readLine();
            if (line == null) {
                return StaffCommand.waitCommand();
            }
            StaffCommand cmd = parse(line);
            if (cmd != null) {
                return cmd;
            }
        }
    }

    @Override
    public boolean askActivateHappyHour(int turn) {
        while (true) {
            out.print("Activate Happy Hour now? It lasts this turn and next (y/n) > ");
            String line = readLine();
            if (line == null) {
                return false;
            }
            String answer = line.trim().toLowerCase();
            if (answer.equals("y") || answer.equals("yes")) {
                return true;
            }
            if (answer.equals("n") || answer.equals("no")) {
                return false;
            }
            out.println(" !! Please answer y or n.");
        }
    }

    @Override
    public void showEvent(String message) {
        out.println(" >> " + message);
    }

    @Override
    public void showRejected(String reason) {
        out.println(" !! Not allowed: " + reason);
        out.println("    Please choose again (the task has not been used up).");
    }

    @Override
    public void showTurnEnd(Restaurant restaurant, int turn, int maxTurns) {
        out.println(LINE);
        out.println(" End of turn " + turn + ":");
        for (Customer c : restaurant.getCustomers()) {
            if (c.getState().isAwaitingService() || c.getTable() != null) {
                out.println("   " + c.getLabel() + " - " + c.getState() + ", satisfaction " + c.getSatisfaction());
            }
        }
        out.println(" Served: " + restaurant.getServedCount()
                + " | Unhappy departures: " + restaurant.getUnhappyDepartures()
                + " | Revenue: " + Money.format(restaurant.getRevenueCents())
                + " | Avg satisfaction (paid): " + formatAverage(restaurant.getAveragePaidSatisfaction())
                + " | Turns used: " + turn + "/" + maxTurns);
    }

    @Override
    public void showResult(GameResult result) {
        GameConfig cfg = result.getConfig();
        out.println();
        out.println(DOUBLE_LINE);
        out.println(result.isVictory() ? "                         VICTORY!" : "                          DEFEAT");
        out.println(DOUBLE_LINE);
        out.println(" Paid customers : " + result.getPaidCustomers() + " (target >= " + cfg.getMinPaidCustomers() + ") "
                + mark(result.isPaidTargetMet()));
        out.println(" Revenue        : " + Money.format(result.getRevenueCents()) + " (target >= "
                + Money.format(cfg.getMinRevenueCents()) + ") " + mark(result.isRevenueTargetMet()));
        out.println(" Avg satisfaction: " + formatAverage(result.getAverageSatisfaction()) + " (target >= "
                + cfg.getMinAverageSatisfaction() + ") " + mark(result.isSatisfactionTargetMet()));
        out.println(DOUBLE_LINE);
    }

    // ---------------------------------------------------------------------
    // Input (syntax is checked by CommandParser, rules by the domain)
    // ---------------------------------------------------------------------

    /**
     * Turns a typed line into a command, or explains the syntax error and returns null.
     */
    private StaffCommand parse(String line) {
        if (CommandParser.isHelp(line)) {
            printHelp();
            return null;
        }
        try {
            return CommandParser.parse(line);
        } catch (IllegalArgumentException e) {
            out.println(" !! " + e.getMessage());
            return null;
        }
    }

    /**
     * Reads the next non-comment line. Lines starting with '#' are notes in
     * script files and are skipped. Returns null once input has ended.
     */
    private String readLine() {
        while (!inputEnded && in.hasNextLine()) {
            String line = in.nextLine();
            if (line.trim().startsWith("#")) {
                continue;
            }
            if (echoInput) {
                out.println(line);
            }
            return line;
        }
        if (!inputEnded) {
            inputEnded = true;
            out.println();
            out.println(" (input ended - all remaining choices default to 'wait')");
        }
        return null;
    }

    private void printHelp() {
        out.println("Commands (n = customer / order number):");
        out.println("  Host  : seat n | wait   (acts before the Waiter)");
        out.println("  Waiter: seat n | order n | serve n | wait");
        out.println("  Chef  : cook n | wait");
        out.println("  Happy Hour: answer y/n at the start of a turn (once per game).");
        out.println("  Short forms: s, o, v, c, w.  Type 'help' at any prompt to see this again.");
    }

    // ---------------------------------------------------------------------
    // Formatting helpers
    // ---------------------------------------------------------------------

    private String describeTable(Table t) {
        if (t.isFree()) {
            return "free";
        }
        Customer c = t.getOccupant();
        String order = c.getOrder() == null ? "no order yet"
                : c.getOrder().getItem().getName() + " " + Money.format(c.getOrder().getPriceCents())
                + " - " + c.getOrder().describeStatus();
        return c.getLabel() + " [sat " + c.getSatisfaction() + "] " + c.getState() + " | " + order;
    }

    /** Keeps just the command part of each task hint, e.g. "seat 2". */
    private List<String> shortTasks(List<String> tasks) {
        return tasks.stream().map(t -> t.contains(" - ") ? t.substring(0, t.indexOf(" - ")).trim() : t).toList();
    }

    private static String formatAverage(OptionalDouble avg) {
        return avg.isPresent() ? String.format("%.1f", avg.getAsDouble()) : "N/A";
    }

    private static String mark(boolean met) {
        return met ? "[MET]" : "[NOT MET]";
    }
}
