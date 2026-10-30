package restaurantrush.view;

import java.io.PrintStream;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.OptionalDouble;
import java.util.Scanner;

import restaurantrush.controller.GameConfig;
import restaurantrush.controller.GameResult;
import restaurantrush.controller.TurnStep;
import restaurantrush.model.Menu;
import restaurantrush.model.MenuItem;
import restaurantrush.model.Order;
import restaurantrush.model.Restaurant;
import restaurantrush.model.Table;
import restaurantrush.model.customer.Customer;
import restaurantrush.staff.Staff;
import restaurantrush.staff.StaffCommand;
import restaurantrush.staff.TaskOption;
import restaurantrush.util.Money;

/**
 * Command-line interface. It only displays state and turns typed text into
 * {@link StaffCommand}s; it never decides whether a command is allowed.
 * <p>
 * Each turn is printed as labelled steps that follow the brief (1 Arrivals,
 * 2-3 Manager choices, 4 Eating and payment, 5 Waiting and abandonment), with
 * the start-of-turn board after the arrivals. Choices are listed as numbered
 * options; the manager types the number or the command itself.
 * </p>
 */
public class ConsoleView implements GameView {

    /** Width of banners and rules. */
    private static final int WIDTH = 78;
    private static final String DOUBLE_LINE = "=".repeat(WIDTH);
    private static final String LINE = " " + "-".repeat(WIDTH - 1);

    /** Turns of patience left at or below which a customer is flagged with "!!". */
    private static final int AT_RISK_TURNS = 2;

    /** Where input comes from (keyboard or a script file). */
    private final Scanner in;

    /** Where output goes. */
    private final PrintStream out;

    /** Print each command read, so scripted runs show the choices made. */
    private final boolean echoInput;

    /** Set once input runs out; remaining choices default to waiting. */
    private boolean inputEnded;

    /** Settings, kept for the arrival preview. */
    private GameConfig config;

    /** Events printed since the last step heading (to mark quiet steps). */
    private int eventsInStep = -1;

    /** The step currently being printed, or null outside the steps. */
    private TurnStep currentStep;

    /** Satisfaction of each customer when the board was shown, to report this turn's change. */
    private final Map<Integer, Integer> satisfactionAtStart = new HashMap<>();

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

    // ---------------------------------------------------------------------
    // Welcome and help
    // ---------------------------------------------------------------------

    @Override
    public void showWelcome(GameConfig config, Menu menu) {
        this.config = config;
        out.println(DOUBLE_LINE);
        out.println(centre("R E S T A U R A N T   R U S H"));
        out.println(DOUBLE_LINE);
        out.println(" You are the manager. Staff time is limited, so each turn you decide what");
        out.println(" each member of staff does. Customers choose their own food and the Cashier");
        out.println(" charges customers automatically.");
        out.println();
        out.println(" GOAL - checked after turn " + config.getMaxTurns() + " (the game always plays all "
                + config.getMaxTurns() + " turns)");
        out.println("   * at least " + config.getMinPaidCustomers() + " paid customers");
        out.println("   * revenue of at least " + Money.format(config.getMinRevenueCents()));
        out.println("   * average satisfaction of paid customers of at least " + config.getMinAverageSatisfaction());
        out.println();
        out.printf(" %-34s %9s   %s%n", "MENU", "Price", "Preparation units");
        for (MenuItem item : menu.getItems()) {
            out.printf("   %-32s %9s   %d%n", item.getName(), Money.format(item.getPriceCents()), item.getPrepUnits());
        }
        out.println();
        out.println(" ARRIVALS - one customer at the start of each turn below (satisfaction "
                + Customer.MAX_SATISFACTION + ")");
        StringBuilder turns = new StringBuilder("   Turn     ");
        StringBuilder types = new StringBuilder("   Customer ");
        for (int t : config.getArrivals().keySet()) {
            turns.append(String.format("%-9s", t));
            types.append(String.format("%-9s", config.arrivalTypeName(t)));
        }
        out.println(turns.toString().stripTrailing());
        out.println(types.toString().stripTrailing());
        out.println();
        printTurnGuide();
        out.println();
        printHelp();
    }

    /** Explains the order of a turn and the timing rules the manager must plan around. */
    private void printTurnGuide() {
        out.println(" HOW A TURN WORKS");
        out.println("   Step 1    Arrivals: the scheduled customer joins the waiting queue.");
        out.println("   Step 2-3  Your choices: the Waiter does ONE task (seat, take an order or");
        out.println("             serve), then the Chef does ONE preparation unit.");
        out.println("   Step 4    Customers served on an earlier turn finish eating; the Cashier");
        out.println("             charges one READY_TO_PAY customer (oldest first).");
        out.println("   Step 5    Every customer not yet served loses satisfaction. A customer at");
        out.println("             0 leaves without paying and frees the table.");
        out.println("   Timing:   a dish finished this turn can be served next turn at the");
        out.println("             earliest; a customer pays the turn after finishing eating.");
    }

    /** Lists the commands. */
    private void printHelp() {
        out.println(" HOW TO CHOOSE (n = customer number; an order has its customer's number)");
        out.println("   Type the option number shown in the list, e.g.  1");
        out.println("   or type a command:");
        printCommandList();
        out.println("   Short forms: s = seat, o = order, v = serve, c = cook, w = wait.");
        out.println("   When 'wait' is the only option, just press Enter.");
        out.println("   Type 'help' at any prompt to see this again.");
    }

    /** One line per manager-directed role. */
    private void printCommandList() {
        out.println("     Waiter : seat n | order n | serve n | wait");
        out.println("     Chef   : cook n | wait");
    }

    // ---------------------------------------------------------------------
    // Turn display
    // ---------------------------------------------------------------------

    @Override
    public void showTurnHeader(int turn, int maxTurns) {
        out.println();
        out.println();
        out.println(DOUBLE_LINE);
        String left = " TURN " + turn + " of " + maxTurns;
        String right = turn == maxTurns ? "LAST TURN" : (maxTurns - turn) + " turn" + (maxTurns - turn == 1 ? "" : "s") + " left after this";
        out.println(left + " ".repeat(Math.max(1, WIDTH - 1 - left.length() - right.length())) + right);
        out.println(DOUBLE_LINE);
        eventsInStep = -1;
    }

    @Override
    public void showStep(TurnStep step) {
        closeStep();
        out.println();
        out.println(" STEP " + step.getNumber() + "  " + step.getTitle().toUpperCase());
        eventsInStep = 0;
        currentStep = step;
    }

    @Override
    public void showStatus(Restaurant restaurant, int turn, List<Staff> staff) {
        closeStep();
        eventsInStep = -1;
        satisfactionAtStart.clear();
        for (Customer c : restaurant.getCustomers()) {
            satisfactionAtStart.put(c.getId(), c.getSatisfaction());
        }

        out.println();
        out.println(LINE);
        out.println(" RESTAURANT AT THE START OF TURN " + turn);
        out.println("   Revenue " + Money.format(restaurant.getRevenueCents())
                + "    Paid customers " + restaurant.getPaidCount()
                + "    Avg satisfaction (paid) " + formatAverage(restaurant.getAveragePaidSatisfaction()));
        out.println("   " + nextArrivalText(turn));

        out.println();
        out.println("   WAITING QUEUE");
        if (restaurant.getWaitingQueue().isEmpty()) {
            out.println("     (empty)");
        }
        for (Customer c : restaurant.getWaitingQueue()) {
            out.printf("     %-12s sat %3d   %s%n", c.getLabel(), c.getSatisfaction(), patienceText(c));
        }

        out.println("   TABLES");
        for (Table t : restaurant.getTables()) {
            if (t.isFree()) {
                out.printf("     Table %d   free%n", t.getNumber());
                continue;
            }
            Customer c = t.getOccupant();
            out.printf("     Table %d   %-12s sat %3d   %s%n", t.getNumber(), c.getLabel(), c.getSatisfaction(), c.getState());
            out.println("               Order: " + orderText(c));
            String patience = patienceText(c);
            if (!patience.isEmpty()) {
                out.println("               " + patience);
            }
        }

        out.println("   STAFF TASKS AVAILABLE");
        for (Staff s : staff) {
            out.printf("     %-8s %s%n", s.getRole(), s.summariseTasks(restaurant, turn));
        }
        out.println(LINE);
    }

    @Override
    public StaffCommand askCommand(Staff staff, List<TaskOption> options) {
        if (eventsInStep >= 0) {
            eventsInStep++;
        }
        out.println();
        out.println("   " + staff.getRole().toUpperCase() + " - choose one task:");
        int width = 0;
        for (TaskOption option : options) {
            width = Math.max(width, option.getCommand().toCommandText().length());
        }
        int number = 1;
        for (TaskOption option : options) {
            out.printf("     %d) %-" + width + "s   %s%n", number++, option.getCommand().toCommandText(), option.getDescription());
        }
        boolean onlyWait = options.size() == 1 && options.get(0).isWait();
        while (true) {
            out.print("   " + staff.getRole() + (onlyWait ? " (Enter = wait) > " : " > "));
            String line = readLine();
            if (line == null) {
                return StaffCommand.waitCommand();
            }
            if (CommandParser.isHelp(line)) {
                printHelp();
                continue;
            }
            try {
                return CommandParser.parse(line, options);
            } catch (IllegalArgumentException e) {
                printWrapped("   !! ", e.getMessage());
            }
        }
    }

    @Override
    public void showEvent(String message) {
        if (eventsInStep >= 0) {
            eventsInStep++;
        }
        printWrapped("   >> ", message);
    }

    @Override
    public void showRejected(Staff staff, String reason) {
        printWrapped("   !! ", "Not allowed: " + reason);
        out.println("      Choose again - the " + staff.getRole() + "'s task has not been used up.");
    }

    @Override
    public void showTurnEnd(Restaurant restaurant, int turn, int maxTurns) {
        closeStep();
        eventsInStep = -1;
        out.println();
        out.println(LINE);
        out.println(" END OF TURN " + turn + " - RESULTS");
        out.println("   Served " + restaurant.getServedCount()
                + "    Unhappy departures " + restaurant.getUnhappyDepartures()
                + "    Turns used " + turn + "/" + maxTurns);
        out.println("   Revenue " + Money.format(restaurant.getRevenueCents())
                + "    Avg satisfaction (paid) " + formatAverage(restaurant.getAveragePaidSatisfaction()));
        boolean any = false;
        for (Customer c : restaurant.getCustomers()) {
            if (c.getState().isAwaitingService() || c.getTable() != null) {
                if (!any) {
                    out.println("   Customers still in the restaurant:");
                    any = true;
                }
                out.printf("     %-12s %-13s sat %3d%s%n", c.getLabel(), c.getState(), c.getSatisfaction(), changeText(c));
            }
        }
        if (!any) {
            out.println("   Nobody is in the restaurant.");
        }
        out.println(LINE);
    }

    @Override
    public void showResult(GameResult result) {
        GameConfig cfg = result.getConfig();
        out.println();
        out.println();
        out.println(DOUBLE_LINE);
        out.println(centre(result.isVictory() ? "V I C T O R Y !" : "D E F E A T"));
        out.println(DOUBLE_LINE);
        out.printf("   %-28s %-10s %-12s %s%n", "Target", "Result", "Needed", "");
        out.printf("   %-28s %-10s %-12s %s%n", "Paid customers", result.getPaidCustomers(),
                ">= " + cfg.getMinPaidCustomers(), mark(result.isPaidTargetMet()));
        out.printf("   %-28s %-10s %-12s %s%n", "Revenue", Money.format(result.getRevenueCents()),
                ">= " + Money.format(cfg.getMinRevenueCents()), mark(result.isRevenueTargetMet()));
        out.printf("   %-28s %-10s %-12s %s%n", "Avg satisfaction (paid)", formatAverage(result.getAverageSatisfaction()),
                ">= " + cfg.getMinAverageSatisfaction(), mark(result.isSatisfactionTargetMet()));
        out.println(LINE);
        out.println("   Dishes served " + result.getServedCount() + "    Unhappy departures " + result.getUnhappyDepartures()
                + "    Turns played " + cfg.getMaxTurns() + "/" + cfg.getMaxTurns());
        out.println(result.isVictory() ? "   All three targets met - well managed!"
                : "   Not every target was met - see the NOT MET rows above.");
        out.println(DOUBLE_LINE);
    }

    // ---------------------------------------------------------------------
    // Input (syntax is checked by CommandParser, rules by the domain)
    // ---------------------------------------------------------------------

    /**
     * Reads the next non-comment line. Lines starting with '#' are notes in
     * script files and are skipped. Returns null once input has ended.
     *
     * @return the line, or null
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
            out.println("   (input ended - all remaining choices default to 'wait')");
        }
        return null;
    }

    // ---------------------------------------------------------------------
    // Formatting helpers
    // ---------------------------------------------------------------------

    /**
     * Prints a message after a prefix, wrapping long text onto indented lines
     * so nothing runs past the banner width.
     */
    private void printWrapped(String prefix, String message) {
        String indent = " ".repeat(prefix.length());
        StringBuilder line = new StringBuilder(prefix);
        for (String word : message.split(" ")) {
            if (line.length() + word.length() + 1 > WIDTH && line.length() > prefix.length()) {
                out.println(line.toString().stripTrailing());
                line = new StringBuilder(indent);
            }
            line.append(word).append(' ');
        }
        out.println(line.toString().stripTrailing());
    }

    /** Says so when nothing happened in a step, so every step is visibly accounted for. */
    private void closeStep() {
        if (eventsInStep == 0 && currentStep != null) {
            out.println("   " + quietStepText(currentStep));
        }
        eventsInStep = -1;
        currentStep = null;
    }

    private static String quietStepText(TurnStep step) {
        switch (step) {
            case ARRIVALS:
                return "(no customer arrives this turn)";
            case EATING_AND_PAYMENT:
                return "(nobody finished eating or paid this turn)";
            case WAITING_AND_ABANDONMENT:
                return "(nobody walked out)";
            default:
                return "(nothing happened in this step)";
        }
    }

    private String nextArrivalText(int turn) {
        int next = config == null ? -1 : config.nextArrivalAfter(turn);
        return next < 0 ? "No more customers will arrive."
                : "Next arrival: turn " + next + " (" + config.arrivalTypeName(next) + ")";
    }

    /** e.g. "leaves in 3 turns if not served"; empty once the customer has been served. */
    private static String patienceText(Customer c) {
        int left = c.turnsUntilLeaving();
        if (left < 0) {
            return "";
        }
        if (left == 1) {
            return "!! LEAVES at the end of this turn if not served";
        }
        return (left <= AT_RISK_TURNS ? "!! " : "") + "leaves in " + left + " turns if not served";
    }

    private static String orderText(Customer c) {
        Order o = c.getOrder();
        if (o == null) {
            return "no order yet";
        }
        return o.getItem().getName() + " " + Money.format(o.getPriceCents()) + " [" + o.describeStatus() + "]";
    }

    private String changeText(Customer c) {
        Integer before = satisfactionAtStart.get(c.getId());
        if (before == null || before == c.getSatisfaction()) {
            return "";
        }
        int change = c.getSatisfaction() - before;
        return "   (" + (change > 0 ? "+" : "") + change + " this turn)";
    }

    private static String centre(String text) {
        return " ".repeat(Math.max(0, (WIDTH - text.length()) / 2)) + text;
    }

    private static String formatAverage(OptionalDouble avg) {
        return avg.isPresent() ? String.format("%.1f", avg.getAsDouble()) : "N/A";
    }

    private static String mark(boolean met) {
        return met ? "MET" : "NOT MET";
    }
}
