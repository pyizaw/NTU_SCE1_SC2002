package restaurantrush.controller;

import java.util.ArrayList;
import java.util.List;

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
import restaurantrush.staff.StaffCommand;
import restaurantrush.staff.Waiter;
import restaurantrush.view.GameView;

/**
 * Runs the game: plays every turn's steps in order and checks the result
 * after the last turn. It asks the {@link GameView} for the manager's choices
 * and hands each one to the staff member it is for; all rules are applied by
 * the staff and domain objects.
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

    /** Staff the manager directs, in the order they act (Host, Waiter, then Chef). */
    private final List<Staff> directedStaff;

    /** The Cashier works automatically. */
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
        this.directedStaff = List.of(new Host(), new Waiter(menu, new PriceCalculator(happyHour)), new Chef());
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
        view.showStep(TurnStep.ARRIVALS);
        CustomerFactory arriving = config.getArrivals().get(turn);
        if (arriving != null) {
            Customer customer = arriving.create(nextCustomerId++, turn);
            restaurant.admit(customer);
            view.showEvent("Customer " + customer.getLabel() + " arrives and joins the queue (satisfaction "
                    + customer.getSatisfaction() + ").");
        }
        view.showStatus(restaurant, turn, allStaff());
        startOfTurnHappyHour(turn);

        // Steps 2-3: manager choices, kitchen and serving (in staff order)
        view.showStep(TurnStep.MANAGER_CHOICES);
        for (Staff s : directedStaff) {
            runStaffChoice(s, turn);
        }

        // Step 4: eating and payment (a customer who becomes READY_TO_PAY now pays next turn)
        view.showStep(TurnStep.EATING_AND_PAYMENT);
        cashier.collectNextPayment(restaurant, turn).ifPresent(r -> view.showEvent(r.getMessage()));
        for (Customer c : restaurant.finishEating(turn)) {
            view.showEvent("Customer " + c.getLabel() + " finished eating and is READY_TO_PAY (pays next turn at the earliest).");
        }

        // Step 5: waiting and abandonment
        view.showStep(TurnStep.WAITING_AND_ABANDONMENT);
        for (Customer c : restaurant.applyEndOfTurnPatience()) {
            view.showEvent("Customer " + c.getLabel() + " ran out of patience and LEFT without paying.");
        }
        view.showTurnEnd(restaurant, turn, config.getMaxTurns());
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

    /**
     * Asks for a staff member's task until a valid one is given. A rejected
     * choice is explained and does not use up the staff member's task.
     */
    private void runStaffChoice(Staff staff, int turn) {
        while (!staff.hasActed()) {
            StaffCommand cmd = view.askCommand(staff, staff.availableTasks(restaurant, turn));
            ActionResult result = staff.carryOut(cmd, restaurant, turn);
            if (result.isSuccess()) {
                view.showEvent(result.getMessage());
            } else {
                view.showRejected(staff, result.getMessage());
            }
        }
    }

    /**
     * @return all staff in the order they act, the Cashier last
     */
    private List<Staff> allStaff() {
        List<Staff> all = new ArrayList<>(directedStaff);
        all.add(cashier);
        return all;
    }
}
