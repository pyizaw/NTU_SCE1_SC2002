package restaurantrush.controller;

import java.util.List;
import java.util.function.Function;

import restaurantrush.model.ActionResult;
import restaurantrush.model.Menu;
import restaurantrush.model.Restaurant;
import restaurantrush.model.customer.Customer;
import restaurantrush.model.pricing.HappyHour;
import restaurantrush.model.pricing.PriceCalculator;
import restaurantrush.staff.Cashier;
import restaurantrush.staff.Chef;
import restaurantrush.staff.Host;
import restaurantrush.staff.Staff;
import restaurantrush.staff.Waiter;
import restaurantrush.view.GameView;

/**
 * Runs the game: plays every turn's six steps in order and checks the result
 * after the last turn. It asks the {@link GameView} for the manager's choices
 * but all rules are applied by the staff and domain objects.
 */
public class GameController {

    /** Game settings. */
    private final GameConfig config;

    /** The restaurant being managed. */
    private final Restaurant restaurant;

    /** The menu. */
    private final Menu menu;

    /** Happy Hour pricing policy (one use per game). */
    private final HappyHour happyHour;

    /** Staff. */
    private final Host host;
    private final Waiter waiter;
    private final Chef chef;
    private final Cashier cashier;

    /** User interface. */
    private final GameView view;

    /** Number given to the next arriving customer. */
    private int nextCustomerId = 1;

    /**
     * Sets up a new game.
     *
     * @param config game settings
     * @param view   user interface
     */
    public GameController(GameConfig config, GameView view) {
        this.config = config;
        this.view = view;
        this.restaurant = new Restaurant(config.getTableCount());
        this.menu = Menu.withCombos();
        this.happyHour = new HappyHour();
        this.host = new Host();
        this.waiter = new Waiter(menu, new PriceCalculator(happyHour));
        this.chef = new Chef();
        this.cashier = new Cashier();
    }

    /**
     * Plays all turns and shows the result. There is no early ending.
     *
     * @return the final result
     */
    public GameResult play() {
        view.showWelcome(config, menu);
        for (int turn = 1; turn <= config.getMaxTurns(); turn++) {
            playTurn(turn);
        }
        GameResult result = GameResult.evaluate(config, restaurant);
        view.showResult(result);
        return result;
    }

    /**
     * One turn: arrivals, manager choices, kitchen and serving, eating and
     * payment, waiting and abandonment.
     *
     * @param turn current turn
     */
    private void playTurn(int turn) {
        view.showTurnHeader(turn, config.getMaxTurns());
        for (Staff s : allStaff()) {
            s.startTurn();
        }

        // Step 1: arrivals
        CustomerFactory arriving = config.getArrivals().get(turn);
        if (arriving != null) {
            Customer customer = arriving.create(nextCustomerId++, turn);
            restaurant.admit(customer);
            view.showEvent("Customer " + customer.getLabel() + " arrives and joins the queue (satisfaction "
                    + customer.getSatisfaction() + ").");
        }
        view.showStatus(restaurant, turn, allStaff());
        startOfTurnHappyHour(turn);

        // Steps 2-3: manager choices; Host, then Waiter, then Chef
        runStaffChoice(host, this::hostTask, turn);
        runStaffChoice(waiter, cmd -> waiterTask(cmd, turn), turn);
        runStaffChoice(chef, cmd -> chefTask(cmd, turn), turn);

        // Step 4: eating and payment (a customer who becomes READY_TO_PAY now pays next turn)
        cashier.collectNextPayment(restaurant, turn).ifPresent(r -> view.showEvent(r.getMessage()));
        for (Customer c : restaurant.finishEating(turn)) {
            view.showEvent("Customer " + c.getLabel() + " finished eating and is READY_TO_PAY.");
        }

        // Step 5: waiting and abandonment
        for (Customer c : restaurant.applyEndOfTurnPatience()) {
            view.showEvent("Customer " + c.getLabel() + " ran out of patience and LEFT without paying.");
        }
        view.showTurnEnd(restaurant, turn, config.getMaxTurns());
    }

    /**
     * Asks for a staff member's task until a valid one is given. A rejected
     * choice is explained and does not use up the staff member's task.
     */
    private void runStaffChoice(Staff staff, Function<StaffCommand, ActionResult> handler, int turn) {
        while (!staff.hasActed()) {
            StaffCommand cmd = view.askCommand(staff, staff.availableTasks(restaurant, turn));
            ActionResult result = handler.apply(cmd);
            if (result.isSuccess()) {
                view.showEvent(result.getMessage());
            } else {
                view.showRejected(result.getMessage());
            }
        }
    }

    /**
     * Offers Happy Hour if it is unused, then applies its recovery if it is
     * active this turn.
     */
    private void startOfTurnHappyHour(int turn) {
        if (happyHour.canActivate() && view.askActivateHappyHour(turn)) {
            happyHour.activate(turn);
            view.showEvent("Happy Hour switched on: 20% off orders taken this turn and next.");
        }
        view.showHappyHourStatus(happyHour.describe(turn));
        if (happyHour.isActive(turn)) {
            for (Customer c : restaurant.recoverWaitingCustomers(HappyHour.RECOVERY)) {
                view.showEvent("Happy Hour: " + c.getLabel() + " recovers to satisfaction " + c.getSatisfaction() + ".");
            }
        }
    }

    private ActionResult hostTask(StaffCommand cmd) {
        switch (cmd.getAction()) {
            case SEAT:
                return host.seat(restaurant, cmd.getTargetId());
            case WAIT:
                return host.waitTurn();
            default:
                return ActionResult.fail("The Host can only seat customers or wait.");
        }
    }

    private ActionResult waiterTask(StaffCommand cmd, int turn) {
        switch (cmd.getAction()) {
            case SEAT:
                return waiter.seat(restaurant, cmd.getTargetId());
            case ORDER:
                return waiter.takeOrder(restaurant, cmd.getTargetId(), turn);
            case SERVE:
                return waiter.serve(restaurant, cmd.getTargetId(), turn);
            case WAIT:
                return waiter.waitTurn();
            default:
                return ActionResult.fail("The Waiter cannot do that. Use seat, order, serve or wait.");
        }
    }

    private ActionResult chefTask(StaffCommand cmd, int turn) {
        switch (cmd.getAction()) {
            case COOK:
                return chef.cook(restaurant, cmd.getTargetId(), turn);
            case WAIT:
                return chef.waitTurn();
            default:
                return ActionResult.fail("The Chef can only cook or wait.");
        }
    }

    /**
     * @return staff in the order they act
     */
    private List<Staff> allStaff() {
        return List.of(host, waiter, chef, cashier);
    }
}
