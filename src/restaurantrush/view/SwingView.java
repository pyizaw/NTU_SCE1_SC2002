package restaurantrush.view;

import java.awt.BasicStroke;
import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Component;
import java.awt.Dimension;
import java.awt.FlowLayout;
import java.awt.Font;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.Container;
import java.awt.GridLayout;
import java.awt.Insets;
import java.awt.KeyboardFocusManager;
import java.awt.RenderingHints;
import java.awt.event.KeyEvent;
import java.util.ArrayList;
import java.util.List;
import java.util.OptionalDouble;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.LinkedBlockingQueue;

import javax.swing.BorderFactory;
import javax.swing.Box;
import javax.swing.BoxLayout;
import javax.swing.JButton;
import javax.swing.JCheckBox;
import javax.swing.JComponent;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTextField;
import javax.swing.JTextPane;
import javax.swing.SwingUtilities;
import javax.swing.WindowConstants;
import javax.swing.border.Border;
import javax.swing.text.BadLocationException;
import javax.swing.text.SimpleAttributeSet;
import javax.swing.text.StyleConstants;
import javax.swing.text.StyledDocument;

import restaurantrush.controller.GameConfig;
import restaurantrush.controller.GameResult;
import restaurantrush.controller.StaffCommand;
import restaurantrush.enums.CustomerState;
import restaurantrush.model.Menu;
import restaurantrush.model.MenuItem;
import restaurantrush.model.Order;
import restaurantrush.model.Restaurant;
import restaurantrush.model.Table;
import restaurantrush.model.customer.Customer;
import restaurantrush.staff.Staff;
import restaurantrush.util.Money;

/**
 * Window (Swing) user interface. It implements the same {@link GameView} as
 * the console, so {@link restaurantrush.controller.GameController} runs the
 * game unchanged. Like the console it only displays state and collects
 * commands; every rule is still checked by the staff and domain objects.
 * <p>
 * Threading: the controller runs on its own "game" thread and calls these
 * methods there. Each call takes a read-only snapshot of the restaurant on
 * that thread, then hands it to the Swing event thread to draw. When the
 * controller needs a choice, the game thread waits on a queue that the
 * buttons and the command box fill.
 * </p>
 */
public class SwingView implements GameView {

    // Colours
    private static final Color BACKGROUND = new Color(0xF4, 0xF1, 0xEC);
    private static final Color CARD = Color.WHITE;
    private static final Color HEADER = new Color(0x2B, 0x2D, 0x42);
    private static final Color ACCENT = new Color(0xD9, 0x5D, 0x39);
    private static final Color TEXT = new Color(0x22, 0x22, 0x22);
    private static final Color MUTED = new Color(0x6B, 0x6B, 0x6B);
    private static final Color BORDER = new Color(0xDD, 0xD6, 0xCC);
    private static final Color GOOD = new Color(0x2E, 0x8B, 0x57);
    private static final Color WARN = new Color(0xE0, 0x9A, 0x00);
    private static final Color BAD = new Color(0xC0, 0x39, 0x2B);
    private static final Color INFO = new Color(0x2F, 0x6D, 0xB5);
    private static final Color PURPLE = new Color(0x7B, 0x4F, 0xA6);

    private static final String FONT = "Segoe UI";

    /** Text typed or clicked by the player, consumed by the game thread. */
    private final BlockingQueue<String> input = new LinkedBlockingQueue<>();

    // Swing components (only touched on the event thread)
    private JFrame frame;
    private JLabel turnLabel;
    private JLabel revenueLabel;
    private JLabel satisfactionLabel;
    private JLabel paidLabel;
    private JLabel happyHourLabel;
    private JLabel arrivalsLabel;
    private JPanel queuePanel;
    private JPanel tablesPanel;
    private JPanel staffPanel;
    private JLabel promptLabel;
    private JPanel actionButtons;
    private JTextField commandField;
    private JTextPane log;
    private final List<JButton> choiceButtons = new ArrayList<>();

    /** When on, a staff member whose only option is "wait" is skipped automatically. */
    private volatile boolean autoSkip = true;

    // Game state seen by the game thread
    private Restaurant restaurant;
    private List<Staff> staff = new ArrayList<>();
    private int turn;
    private int maxTurns;
    private String helpText = "";
    private GameConfig config;
    private Runnable restartAction;

    private SwingView() {
    }

    /**
     * Builds and shows the window.
     *
     * @return the view, ready to be given to a GameController
     * @throws Exception if the window cannot be created
     */
    public static SwingView create() throws Exception {
        SwingView view = new SwingView();
        SwingUtilities.invokeAndWait(view::buildWindow);
        return view;
    }

    /**
     * @param action what the "Play again" button does
     */
    public void setRestartAction(Runnable action) {
        this.restartAction = action;
    }

    /** Clears the log and any leftover input before a new game. */
    public void reset() {
        input.clear();
        restaurant = null;
        SwingUtilities.invokeLater(() -> {
            log.setText("");
            clearActions();
        });
    }

    // ---------------------------------------------------------------------
    // GameView
    // ---------------------------------------------------------------------

    @Override
    public void showWelcome(GameConfig config, Menu menu) {
        this.config = config;
        StringBuilder menuText = new StringBuilder();
        for (MenuItem item : menu.getItems()) {
            menuText.append("  - ").append(item).append('\n');
        }
        helpText = "You are the manager. Each turn, choose one task for each member of staff.\n\n"
                + "Goal after turn " + config.getMaxTurns() + ":\n"
                + "  - at least " + config.getMinPaidCustomers() + " paid customers\n"
                + "  - revenue at least " + Money.format(config.getMinRevenueCents()) + "\n"
                + "  - average satisfaction of paid customers at least " + config.getMinAverageSatisfaction() + "\n\n"
                + "Customers arrive on turns " + config.getArrivals().keySet() + ".\n\n"
                + "Menu:\n" + menuText + "\n"
                + "Click a task button (or press its number key 1-9), or type a command in the box:\n"
                + "  Host  : seat n | wait   (acts before the Waiter)\n"
                + "  Waiter: seat n | order n | serve n | wait\n"
                + "  Chef  : cook n | wait\n"
                + "  The Cashier charges one READY_TO_PAY customer per turn automatically.\n"
                + "  Happy Hour can be switched on once, at the start of a turn.\n"
                + "  Staff with nothing to do are skipped automatically (untick the box to choose yourself).\n\n"
                + "Regular customers lose 8 satisfaction per unserved turn, VIPs lose 5 and pay 10% less,\n"
                + "Critics lose 12, order the most demanding dish and hate cold food.\n"
                + "The badge on a card shows how many more unserved turns the customer can take.";
        logLine("Welcome to Restaurant Rush!", Style.HEADER);
        logLine("Serve customers quickly: " + config.getMinPaidCustomers() + "+ paid, "
                + Money.format(config.getMinRevenueCents()) + "+ revenue, average satisfaction "
                + config.getMinAverageSatisfaction() + "+. Click \"How to play\" for the rules.", Style.NORMAL);
    }

    @Override
    public void showTurnHeader(int turn, int maxTurns) {
        this.turn = turn;
        this.maxTurns = maxTurns;
        logLine("", Style.NORMAL);
        logLine("Turn " + turn + " / " + maxTurns, Style.HEADER);
        refresh();
    }

    @Override
    public void showStatus(Restaurant restaurant, int turn, List<Staff> staff) {
        this.restaurant = restaurant;
        this.staff = new ArrayList<>(staff);
        refresh();
    }

    @Override
    public StaffCommand askCommand(Staff member, List<String> tasks) {
        if (autoSkip && tasks.size() == 1 && splitTask(tasks.get(0))[0].equals("wait")) {
            return StaffCommand.waitCommand();
        }
        while (true) {
            refresh();
            List<String[]> options = new ArrayList<>();
            for (String task : tasks) {
                options.add(splitTask(task));
            }
            String role = member.getRole();
            SwingUtilities.invokeLater(() -> showChoices(role + ": choose a task", options));
            String line = take();
            if (CommandParser.isHelp(line)) {
                SwingUtilities.invokeLater(this::showHelp);
                continue;
            }
            try {
                return CommandParser.parse(line);
            } catch (IllegalArgumentException e) {
                logLine(e.getMessage(), Style.REJECTED);
            }
        }
    }

    @Override
    public boolean askActivateHappyHour(int turn) {
        List<String[]> options = List.of(
                new String[] {"y", "Activate Happy Hour (this turn and next)"},
                new String[] {"n", "Not now"});
        SwingUtilities.invokeLater(() -> showChoices("Start of turn: switch on Happy Hour? (once per game)", options));
        String answer = take().trim().toLowerCase();
        return answer.equals("y") || answer.equals("yes");
    }

    @Override
    public void showHappyHourStatus(String status) {
        SwingUtilities.invokeLater(() -> {
            happyHourLabel.setText(status);
            happyHourLabel.setForeground(status.contains("ACTIVE") ? new Color(0xFF, 0xD1, 0x66) : new Color(0xCC, 0xCC, 0xD6));
        });
    }

    @Override
    public void showEvent(String message) {
        Style style = Style.NORMAL;
        if (message.contains("arrives")) {
            style = Style.ARRIVAL;
        } else if (message.contains("LEFT") || message.contains("cold")) {
            style = Style.REJECTED;
        } else if (message.startsWith("Cashier") || message.startsWith("Served")) {
            style = Style.GOOD;
        } else if (message.startsWith("Happy Hour")) {
            style = Style.HAPPY;
        }
        logLine(message, style);
        refresh();
    }

    @Override
    public void showRejected(String reason) {
        logLine("Not allowed: " + reason + " Choose again - the task was not used up.", Style.REJECTED);
    }

    @Override
    public void showTurnEnd(Restaurant restaurant, int turn, int maxTurns) {
        logLine("End of turn " + turn + ": served " + restaurant.getServedCount()
                + ", unhappy departures " + restaurant.getUnhappyDepartures()
                + ", revenue " + Money.format(restaurant.getRevenueCents())
                + ", avg satisfaction " + formatAverage(restaurant.getAveragePaidSatisfaction())
                + ", turns used " + turn + "/" + maxTurns, Style.SUMMARY);
        refresh();
    }

    @Override
    public void showResult(GameResult result) {
        refresh();
        GameConfig cfg = result.getConfig();
        String[] lines = {
            "Paid customers: " + result.getPaidCustomers() + " (target " + cfg.getMinPaidCustomers() + ") " + mark(result.isPaidTargetMet()),
            "Revenue: " + Money.format(result.getRevenueCents()) + " (target " + Money.format(cfg.getMinRevenueCents()) + ") "
                    + mark(result.isRevenueTargetMet()),
            "Avg satisfaction: " + formatAverage(result.getAverageSatisfaction()) + " (target "
                    + cfg.getMinAverageSatisfaction() + ") " + mark(result.isSatisfactionTargetMet()),
        };
        boolean victory = result.isVictory();
        logLine("", Style.NORMAL);
        logLine(victory ? "VICTORY!" : "DEFEAT", victory ? Style.GOOD : Style.REJECTED);
        for (String line : lines) {
            logLine(line, Style.NORMAL);
        }
        SwingUtilities.invokeLater(() -> showResultPanel(victory, lines));
    }

    // ---------------------------------------------------------------------
    // Snapshots (taken on the game thread, drawn on the event thread)
    // ---------------------------------------------------------------------

    /** What one customer card shows. */
    private record CustomerCard(int id, String type, int satisfaction, String state, String orderText, String orderStatus,
                                int turnsUntilLeaving) {
    }

    /** What one table card shows; customer is null when the table is free. */
    private record TableCard(int number, CustomerCard customer) {
    }

    /** Everything drawn on screen for one refresh. */
    private record Snapshot(String turnText, String revenue, String average, String paid,
                            List<CustomerCard> queue, List<TableCard> tables, List<String[]> staffLines, String arrivals) {
    }

    private void refresh() {
        Snapshot snapshot = takeSnapshot();
        SwingUtilities.invokeLater(() -> render(snapshot));
    }

    private Snapshot takeSnapshot() {
        String turnText = turn == 0 ? "Turn -" : "Turn " + turn + " / " + maxTurns;
        if (restaurant == null) {
            return new Snapshot(turnText, "$0.00", "N/A", "0", List.of(), List.of(), List.of(), upcomingArrivals());
        }
        List<CustomerCard> queue = new ArrayList<>();
        for (Customer c : restaurant.getWaitingQueue()) {
            queue.add(card(c));
        }
        List<TableCard> tables = new ArrayList<>();
        for (Table t : restaurant.getTables()) {
            tables.add(new TableCard(t.getNumber(), t.isFree() ? null : card(t.getOccupant())));
        }
        List<String[]> staffLines = new ArrayList<>();
        for (Staff s : staff) {
            List<String> labels = new ArrayList<>();
            for (String task : s.availableTasks(restaurant, turn)) {
                labels.add(splitTask(task)[0]);
            }
            staffLines.add(new String[] {s.getRole(), String.join("  |  ", labels)});
        }
        return new Snapshot(turnText, Money.format(restaurant.getRevenueCents()),
                formatAverage(restaurant.getAveragePaidSatisfaction()), String.valueOf(restaurant.getPaidCount()),
                queue, tables, staffLines, upcomingArrivals());
    }

    /** e.g. "Coming up: turn 7 Critic, turn 10 Regular". */
    private String upcomingArrivals() {
        if (config == null) {
            return "";
        }
        List<String> next = new ArrayList<>();
        for (int t : config.getArrivals().keySet()) {
            if (t > turn) {
                next.add("turn " + t + " " + config.arrivalTypeName(t));
            }
        }
        return (next.isEmpty() ? "No more arrivals" : "Coming up: " + String.join(",  ", next)) + "    ";
    }

    private static CustomerCard card(Customer c) {
        Order o = c.getOrder();
        String orderText = o == null ? "No order yet" : o.getItem().getName() + "  " + Money.format(o.getPriceCents());
        String orderStatus = o == null ? "" : o.describeStatus();
        return new CustomerCard(c.getId(), c.getTypeName(), c.getSatisfaction(), c.getState().toString(), orderText, orderStatus,
                c.turnsUntilLeaving());
    }

    // ---------------------------------------------------------------------
    // Window construction and drawing (event thread only)
    // ---------------------------------------------------------------------

    private void buildWindow() {
        frame = new JFrame("Restaurant Rush");
        frame.setDefaultCloseOperation(WindowConstants.EXIT_ON_CLOSE);
        frame.getContentPane().setBackground(BACKGROUND);
        frame.setLayout(new BorderLayout());

        frame.add(buildHeader(), BorderLayout.NORTH);

        JPanel centre = new JPanel(new BorderLayout(12, 12));
        centre.setOpaque(false);
        centre.setBorder(BorderFactory.createEmptyBorder(12, 12, 0, 6));

        queuePanel = column();
        JScrollPane queueScroll = scroll(queuePanel);
        queueScroll.setPreferredSize(new Dimension(250, 300));
        centre.add(section("Waiting queue", queueScroll), BorderLayout.WEST);

        tablesPanel = new JPanel(new GridLayout(1, 2, 12, 0));
        tablesPanel.setOpaque(false);
        staffPanel = column();
        JPanel middle = new JPanel(new BorderLayout(0, 12));
        middle.setOpaque(false);
        middle.add(section("Tables", tablesPanel), BorderLayout.NORTH);
        JPanel staffHolder = new JPanel(new BorderLayout());
        staffHolder.setOpaque(false);
        staffHolder.add(section("Staff tasks available", staffPanel), BorderLayout.NORTH);
        middle.add(staffHolder, BorderLayout.CENTER);
        centre.add(middle, BorderLayout.CENTER);

        frame.add(centre, BorderLayout.CENTER);

        log = new JTextPane();
        log.setEditable(false);
        log.setFont(new Font(FONT, Font.PLAIN, 13));
        log.setBorder(BorderFactory.createEmptyBorder(8, 10, 8, 10));
        JScrollPane logScroll = new JScrollPane(log);
        logScroll.setBorder(BorderFactory.createLineBorder(BORDER));
        logScroll.setPreferredSize(new Dimension(400, 300));
        JPanel east = section("Game log", logScroll);
        east.setBorder(BorderFactory.createEmptyBorder(12, 6, 0, 12));
        frame.add(east, BorderLayout.EAST);

        frame.add(buildActionArea(), BorderLayout.SOUTH);

        // Number keys 1-9 press the matching choice button (unless typing in the command box)
        KeyboardFocusManager.getCurrentKeyboardFocusManager().addKeyEventDispatcher(e -> {
            if (e.getID() != KeyEvent.KEY_PRESSED || commandField.isFocusOwner()) {
                return false;
            }
            int index = e.getKeyChar() - '1';
            if (index >= 0 && index < 9 && index < choiceButtons.size()) {
                choiceButtons.get(index).doClick();
                return true;
            }
            return false;
        });

        frame.setMinimumSize(new Dimension(1100, 700));
        frame.setSize(1280, 780);
        frame.setLocationRelativeTo(null);
        frame.setVisible(true);
    }

    private JPanel buildHeader() {
        JPanel header = new JPanel(new BorderLayout());
        header.setBackground(HEADER);
        header.setBorder(BorderFactory.createEmptyBorder(12, 18, 12, 18));

        JLabel title = new JLabel("Restaurant Rush");
        title.setFont(new Font(FONT, Font.BOLD, 22));
        title.setForeground(Color.WHITE);
        header.add(title, BorderLayout.WEST);

        JPanel stats = new JPanel(new FlowLayout(FlowLayout.RIGHT, 22, 0));
        stats.setOpaque(false);
        turnLabel = stat("Turn -");
        revenueLabel = stat("Revenue $0.00");
        satisfactionLabel = stat("Avg satisfaction N/A");
        paidLabel = stat("Paid 0");
        happyHourLabel = stat("");
        happyHourLabel.setFont(new Font(FONT, Font.PLAIN, 13));
        for (JLabel l : new JLabel[] {turnLabel, revenueLabel, satisfactionLabel, paidLabel}) {
            stats.add(l);
        }
        header.add(stats, BorderLayout.EAST);
        arrivalsLabel = stat("");
        arrivalsLabel.setFont(new Font(FONT, Font.PLAIN, 13));
        arrivalsLabel.setForeground(new Color(0xCC, 0xCC, 0xD6));
        happyHourLabel.setBorder(BorderFactory.createEmptyBorder(0, 0, 0, 6));
        JPanel second = new JPanel(new BorderLayout());
        second.setOpaque(false);
        second.setBorder(BorderFactory.createEmptyBorder(6, 0, 0, 0));
        second.add(arrivalsLabel, BorderLayout.WEST);
        second.add(happyHourLabel, BorderLayout.EAST);
        header.add(second, BorderLayout.SOUTH);
        return header;
    }

    private JPanel buildActionArea() {
        JPanel area = new JPanel(new BorderLayout(0, 8));
        area.setBackground(CARD);
        area.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createMatteBorder(1, 0, 0, 0, BORDER),
                BorderFactory.createEmptyBorder(12, 18, 14, 18)));

        promptLabel = new JLabel("Starting...");
        promptLabel.setFont(new Font(FONT, Font.BOLD, 16));
        promptLabel.setForeground(TEXT);
        area.add(promptLabel, BorderLayout.NORTH);

        actionButtons = new JPanel(new WrapLayout(FlowLayout.LEFT, 8, 4));
        actionButtons.setOpaque(false);
        area.add(actionButtons, BorderLayout.CENTER);

        JPanel typed = new JPanel(new FlowLayout(FlowLayout.LEFT, 8, 0));
        typed.setOpaque(false);
        JLabel or = new JLabel("or type a command:");
        or.setForeground(MUTED);
        commandField = new JTextField(16);
        commandField.setFont(new Font(Font.MONOSPACED, Font.PLAIN, 14));
        commandField.setEnabled(false);
        commandField.addActionListener(e -> submit(commandField.getText()));
        JButton help = button("How to play", false);
        help.addActionListener(e -> showHelp());
        typed.add(or);
        typed.add(commandField);
        typed.add(Box.createHorizontalStrut(12));
        typed.add(help);
        JCheckBox skip = new JCheckBox("Skip staff with nothing to do", true);
        skip.setOpaque(false);
        skip.setFocusable(false);
        skip.addActionListener(e -> autoSkip = skip.isSelected());
        typed.add(Box.createHorizontalStrut(12));
        typed.add(skip);
        area.add(typed, BorderLayout.SOUTH);
        return area;
    }

    private void render(Snapshot s) {
        turnLabel.setText(s.turnText());
        revenueLabel.setText("Revenue " + s.revenue());
        satisfactionLabel.setText("Avg satisfaction " + s.average());
        paidLabel.setText("Paid " + s.paid());
        arrivalsLabel.setText(s.arrivals());

        queuePanel.removeAll();
        if (s.queue().isEmpty()) {
            queuePanel.add(muted("Nobody is waiting."));
        }
        for (CustomerCard c : s.queue()) {
            queuePanel.add(customerPanel(c));
            queuePanel.add(Box.createVerticalStrut(8));
        }
        queuePanel.revalidate();
        queuePanel.repaint();

        tablesPanel.removeAll();
        tablesPanel.setLayout(new GridLayout(1, Math.max(1, s.tables().size()), 12, 0));
        for (TableCard t : s.tables()) {
            tablesPanel.add(tablePanel(t));
        }
        tablesPanel.revalidate();
        tablesPanel.repaint();

        staffPanel.removeAll();
        for (String[] line : s.staffLines()) {
            JPanel row = new JPanel(new BorderLayout(10, 0));
            row.setOpaque(false);
            row.setAlignmentX(Component.LEFT_ALIGNMENT);
            JLabel role = new JLabel(line[0]);
            role.setFont(new Font(FONT, Font.BOLD, 13));
            role.setPreferredSize(new Dimension(130, 20));
            JLabel tasks = new JLabel(line[1]);
            tasks.setFont(new Font(Font.MONOSPACED, Font.PLAIN, 13));
            tasks.setForeground(MUTED);
            row.add(role, BorderLayout.WEST);
            row.add(tasks, BorderLayout.CENTER);
            staffPanel.add(row);
            staffPanel.add(Box.createVerticalStrut(4));
        }
        staffPanel.revalidate();
        staffPanel.repaint();
    }

    private JPanel tablePanel(TableCard t) {
        JPanel panel = new JPanel(new BorderLayout(0, 8));
        panel.setBackground(new Color(0xFB, 0xF8, 0xF3));
        panel.setBorder(BorderFactory.createCompoundBorder(
                new RoundedBorder(t.customer() == null ? BORDER : ACCENT, 14),
                BorderFactory.createEmptyBorder(12, 12, 12, 12)));
        JLabel name = new JLabel("Table " + t.number() + (t.customer() == null ? "  -  free" : ""));
        name.setFont(new Font(FONT, Font.BOLD, 15));
        name.setForeground(t.customer() == null ? MUTED : TEXT);
        panel.add(name, BorderLayout.NORTH);
        if (t.customer() != null) {
            JPanel holder = column();
            holder.add(customerPanel(t.customer()));
            panel.add(holder, BorderLayout.CENTER);
        } else {
            JLabel empty = muted("Ready for the next customer.");
            empty.setBorder(BorderFactory.createEmptyBorder(20, 0, 20, 0));
            panel.add(empty, BorderLayout.CENTER);
        }
        return panel;
    }

    private JPanel customerPanel(CustomerCard c) {
        JPanel card = new JPanel();
        card.setLayout(new BoxLayout(card, BoxLayout.Y_AXIS));
        card.setBackground(CARD);
        card.setBorder(BorderFactory.createCompoundBorder(new RoundedBorder(BORDER, 12),
                BorderFactory.createEmptyBorder(8, 10, 8, 10)));
        card.setAlignmentX(Component.LEFT_ALIGNMENT);

        JPanel top = new JPanel(new FlowLayout(FlowLayout.LEFT, 6, 0));
        top.setOpaque(false);
        top.setAlignmentX(Component.LEFT_ALIGNMENT);
        JLabel id = new JLabel("#" + c.id());
        id.setFont(new Font(FONT, Font.BOLD, 15));
        top.add(id);
        top.add(chip(c.type(), typeColour(c.type())));
        top.add(chip(c.state(), stateColour(c.state())));
        card.add(top);
        card.add(Box.createVerticalStrut(6));

        SatisfactionBar bar = new SatisfactionBar(c.satisfaction());
        bar.setAlignmentX(Component.LEFT_ALIGNMENT);
        card.add(bar);
        if (c.turnsUntilLeaving() > 0) {
            int left = c.turnsUntilLeaving();
            JPanel row = new JPanel(new FlowLayout(FlowLayout.LEFT, 0, 0));
            row.setOpaque(false);
            row.setAlignmentX(Component.LEFT_ALIGNMENT);
            String text = left == 1 ? "Leaves at the end of this turn if not served!" : "Leaves in " + left + " turns if not served  ";
            if (left <= 4) {
                row.add(chip(text, left <= 2 ? BAD : WARN));
            } else {
                JLabel calm = new JLabel(text);
                calm.setFont(new Font(FONT, Font.PLAIN, 12));
                calm.setForeground(MUTED);
                row.add(calm);
            }
            row.setToolTipText("Unserved turns this customer can still take before walking out");
            card.add(Box.createVerticalStrut(3));
            card.add(row);
        }
        card.add(Box.createVerticalStrut(6));

        JLabel order = new JLabel(c.orderText() + "  ");
        order.setFont(new Font(FONT, Font.PLAIN, 13));
        order.setForeground(TEXT);
        order.setAlignmentX(Component.LEFT_ALIGNMENT);
        card.add(order);
        if (!c.orderStatus().isEmpty()) {
            JPanel status = new JPanel(new FlowLayout(FlowLayout.LEFT, 0, 0));
            status.setOpaque(false);
            status.setAlignmentX(Component.LEFT_ALIGNMENT);
            status.add(chip(c.orderStatus(), orderColour(c.orderStatus())));
            card.add(Box.createVerticalStrut(4));
            card.add(status);
        }
        card.setMaximumSize(new Dimension(Integer.MAX_VALUE, card.getPreferredSize().height));
        return card;
    }

    private void showChoices(String prompt, List<String[]> options) {
        clearActions();
        promptLabel.setText(prompt);
        promptLabel.setForeground(TEXT);
        boolean first = true;
        int number = 1;
        for (String[] option : options) {
            String command = option[0];
            boolean isWait = command.equals("wait") || command.equals("n");
            String key = number <= 9 ? number + "   " : "";
            JButton b = button(key + capitalise(option[1]) + (isWait || command.equals("y") ? "" : "   [" + command + "]"), !isWait && first);
            b.addActionListener(e -> submit(command));
            actionButtons.add(b);
            choiceButtons.add(b);
            number++;
            if (!isWait) {
                first = false;
            }
        }
        commandField.setEnabled(true);
        frame.getRootPane().requestFocusInWindow();
        actionButtons.revalidate();
        actionButtons.repaint();
    }

    private void showResultPanel(boolean victory, String[] lines) {
        clearActions();
        promptLabel.setText(victory ? "VICTORY! All three targets met." : "DEFEAT - not every target was met.");
        promptLabel.setForeground(victory ? GOOD : BAD);
        JLabel summary = new JLabel("<html>" + String.join("&nbsp;&nbsp;<br>", lines) + "&nbsp;&nbsp;</html>");
        summary.setFont(new Font(FONT, Font.PLAIN, 14));
        actionButtons.add(summary);
        if (restartAction != null) {
            JButton again = button("Play again", true);
            again.addActionListener(e -> restartAction.run());
            actionButtons.add(again);
        }
        actionButtons.revalidate();
        actionButtons.repaint();
        JOptionPane.showMessageDialog(frame, String.join("\n", lines),
                victory ? "Victory!" : "Defeat", victory ? JOptionPane.INFORMATION_MESSAGE : JOptionPane.WARNING_MESSAGE);
    }

    private void showHelp() {
        javax.swing.JTextArea text = new javax.swing.JTextArea(helpText);
        text.setEditable(false);
        text.setFont(new Font(Font.MONOSPACED, Font.PLAIN, 13));
        text.setBorder(BorderFactory.createEmptyBorder(8, 8, 8, 8));
        JOptionPane.showMessageDialog(frame, text, "How to play", JOptionPane.PLAIN_MESSAGE);
    }

    private void submit(String text) {
        clearActions();
        commandField.setText("");
        promptLabel.setText("...");
        input.offer(text);
    }

    private void clearActions() {
        actionButtons.removeAll();
        choiceButtons.clear();
        actionButtons.revalidate();
        actionButtons.repaint();
        commandField.setEnabled(false);
    }

    // ---------------------------------------------------------------------
    // Log
    // ---------------------------------------------------------------------

    /** Log text styles. */
    private enum Style { NORMAL, HEADER, ARRIVAL, GOOD, REJECTED, HAPPY, SUMMARY }

    private void logLine(String text, Style style) {
        SwingUtilities.invokeLater(() -> {
            SimpleAttributeSet a = new SimpleAttributeSet();
            StyleConstants.setFontFamily(a, FONT);
            StyleConstants.setFontSize(a, 13);
            StyleConstants.setForeground(a, TEXT);
            switch (style) {
                case HEADER:
                    StyleConstants.setBold(a, true);
                    StyleConstants.setFontSize(a, 15);
                    StyleConstants.setForeground(a, HEADER);
                    break;
                case ARRIVAL:
                    StyleConstants.setForeground(a, INFO);
                    break;
                case GOOD:
                    StyleConstants.setForeground(a, GOOD);
                    break;
                case REJECTED:
                    StyleConstants.setForeground(a, BAD);
                    break;
                case HAPPY:
                    StyleConstants.setForeground(a, PURPLE);
                    break;
                case SUMMARY:
                    StyleConstants.setForeground(a, MUTED);
                    StyleConstants.setItalic(a, true);
                    break;
                default:
                    break;
            }
            StyledDocument doc = log.getStyledDocument();
            try {
                doc.insertString(doc.getLength(), text + "\n", a);
            } catch (BadLocationException e) {
                throw new IllegalStateException(e);
            }
            log.setCaretPosition(doc.getLength());
        });
    }

    // ---------------------------------------------------------------------
    // Small helpers
    // ---------------------------------------------------------------------

    /** Waits on the game thread for the player's next click or typed command. */
    private String take() {
        try {
            return input.take();
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            return "wait";
        }
    }

    /** Splits a task hint "seat 2   - seat #2 Regular" into {"seat 2", "seat #2 Regular"}. */
    private static String[] splitTask(String task) {
        int dash = task.indexOf(" - ");
        if (dash < 0) {
            return new String[] {task.trim(), task.trim()};
        }
        return new String[] {task.substring(0, dash).trim(), task.substring(dash + 3).trim()};
    }

    private static String capitalise(String s) {
        return s.isEmpty() ? s : Character.toUpperCase(s.charAt(0)) + s.substring(1);
    }

    private static String formatAverage(OptionalDouble avg) {
        return avg.isPresent() ? String.format("%.1f", avg.getAsDouble()) : "N/A";
    }

    private static String mark(boolean met) {
        return met ? "- met" : "- NOT met";
    }

    private static Color typeColour(String type) {
        switch (type) {
            case "VIP":
                return new Color(0xB8, 0x86, 0x0B);
            case "Critic":
                return PURPLE;
            default:
                return INFO;
        }
    }

    private static Color orderColour(String status) {
        if (status.startsWith("READY")) {
            return GOOD;
        }
        if (status.startsWith("PREPARING")) {
            return WARN;
        }
        if (status.startsWith("CANCELLED")) {
            return BAD;
        }
        return INFO;
    }

    private static Color stateColour(String state) {
        if (state.equals(CustomerState.READY_TO_PAY.toString()) || state.equals(CustomerState.EATING.toString())) {
            return GOOD;
        }
        return MUTED;
    }

    private static JLabel stat(String text) {
        JLabel l = new JLabel(text);
        l.setFont(new Font(FONT, Font.BOLD, 15));
        l.setForeground(Color.WHITE);
        return l;
    }

    private static JLabel muted(String text) {
        JLabel l = new JLabel(text);
        l.setForeground(MUTED);
        l.setFont(new Font(FONT, Font.ITALIC, 13));
        l.setAlignmentX(Component.LEFT_ALIGNMENT);
        return l;
    }

    private static JLabel chip(String text, Color colour) {
        JLabel l = new JLabel(" " + text + " ");
        l.setOpaque(true);
        l.setBackground(colour);
        l.setForeground(Color.WHITE);
        l.setFont(new Font(FONT, Font.BOLD, 11));
        return l;
    }

    private static JButton button(String text, boolean primary) {
        JButton b = new JButton(text);
        b.setFont(new Font(FONT, primary ? Font.BOLD : Font.PLAIN, 14));
        b.setFocusPainted(false);
        if (primary) {
            b.setBackground(ACCENT);
            b.setForeground(Color.WHITE);
        }
        return b;
    }

    private static JPanel column() {
        JPanel p = new JPanel();
        p.setLayout(new BoxLayout(p, BoxLayout.Y_AXIS));
        p.setOpaque(false);
        return p;
    }

    private static JScrollPane scroll(JComponent content) {
        JScrollPane s = new JScrollPane(content);
        s.setBorder(BorderFactory.createEmptyBorder());
        s.setOpaque(false);
        s.getViewport().setOpaque(false);
        return s;
    }

    private static JPanel section(String title, JComponent content) {
        JPanel p = new JPanel(new BorderLayout(0, 6));
        p.setOpaque(false);
        JLabel l = new JLabel(title.toUpperCase());
        l.setFont(new Font(FONT, Font.BOLD, 12));
        l.setForeground(MUTED);
        p.add(l, BorderLayout.NORTH);
        p.add(content, BorderLayout.CENTER);
        return p;
    }

    /** Horizontal 0..100 satisfaction bar: green above 60, amber above 30, red below. */
    private static final class SatisfactionBar extends JComponent {
        private static final long serialVersionUID = 1L;
        private final int value;

        SatisfactionBar(int value) {
            this.value = value;
            setPreferredSize(new Dimension(200, 18));
            setMaximumSize(new Dimension(Integer.MAX_VALUE, 18));
        }

        @Override
        protected void paintComponent(Graphics g) {
            Graphics2D g2 = (Graphics2D) g.create();
            g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
            int labelWidth = 36;
            int w = Math.max(10, getWidth() - labelWidth - 4);
            int h = 10;
            int y = (getHeight() - h) / 2;
            g2.setColor(new Color(0xEC, 0xE7, 0xDF));
            g2.fillRoundRect(0, y, w, h, h, h);
            g2.setColor(value >= 60 ? GOOD : value >= 30 ? WARN : BAD);
            g2.fillRoundRect(0, y, Math.max(h, w * value / 100), h, h, h);
            g2.setColor(TEXT);
            g2.setFont(new Font(FONT, Font.BOLD, 12));
            g2.drawString(String.valueOf(value), w + 6, y + h);
            g2.dispose();
        }
    }

    /** A FlowLayout that wraps onto extra rows and grows taller instead of cutting buttons off. */
    private static final class WrapLayout extends FlowLayout {
        private static final long serialVersionUID = 1L;

        WrapLayout(int align, int hgap, int vgap) {
            super(align, hgap, vgap);
        }

        @Override
        public Dimension preferredLayoutSize(Container target) {
            return layoutSize(target, true);
        }

        @Override
        public Dimension minimumLayoutSize(Container target) {
            return layoutSize(target, false);
        }

        private Dimension layoutSize(Container target, boolean preferred) {
            synchronized (target.getTreeLock()) {
                int maxWidth = target.getWidth() > 0 ? target.getWidth() : Integer.MAX_VALUE / 2;
                Insets insets = target.getInsets();
                maxWidth -= insets.left + insets.right + getHgap() * 2;
                int width = 0;
                int height = 0;
                int rowWidth = 0;
                int rowHeight = 0;
                for (Component c : target.getComponents()) {
                    if (!c.isVisible()) {
                        continue;
                    }
                    Dimension d = preferred ? c.getPreferredSize() : c.getMinimumSize();
                    if (rowWidth + d.width > maxWidth && rowWidth > 0) {
                        width = Math.max(width, rowWidth);
                        height += rowHeight + getVgap();
                        rowWidth = 0;
                        rowHeight = 0;
                    }
                    rowWidth += d.width + getHgap();
                    rowHeight = Math.max(rowHeight, d.height);
                }
                width = Math.max(width, rowWidth);
                height += rowHeight + getVgap() * 2 + insets.top + insets.bottom;
                return new Dimension(width + insets.left + insets.right, Math.max(height, 40));
            }
        }
    }

    /** Thin rounded outline used for cards. */
    private static final class RoundedBorder implements Border {
        private final Color colour;
        private final int radius;

        RoundedBorder(Color colour, int radius) {
            this.colour = colour;
            this.radius = radius;
        }

        @Override
        public void paintBorder(Component c, Graphics g, int x, int y, int width, int height) {
            Graphics2D g2 = (Graphics2D) g.create();
            g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
            g2.setColor(colour);
            g2.setStroke(new BasicStroke(1.5f));
            g2.drawRoundRect(x + 1, y + 1, width - 3, height - 3, radius, radius);
            g2.dispose();
        }

        @Override
        public java.awt.Insets getBorderInsets(Component c) {
            return new java.awt.Insets(3, 3, 3, 3);
        }

        @Override
        public boolean isBorderOpaque() {
            return false;
        }
    }
}
