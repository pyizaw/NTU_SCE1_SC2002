package restaurantrush.model.customer;

import restaurantrush.enums.CustomerState;
import restaurantrush.model.Menu;
import restaurantrush.model.MenuItem;
import restaurantrush.model.Order;
import restaurantrush.model.Table;

/**
 * A restaurant customer. This class owns the customer's life cycle
 * (WAITING -> SEATED -> ORDERED -> EATING -> READY_TO_PAY -> PAID, or LEFT)
 * and protects it: every state change goes through a method that checks the
 * change is legal. Subclasses only decide how the customer behaves (how fast
 * they lose patience, how they choose food, how they are billed).
 */
public abstract class Customer {

    /** Highest possible satisfaction; every customer arrives with this. */
    public static final int MAX_SATISFACTION = 100;

    /** Unique number, given in order of arrival (1, 2, 3 ...). */
    private final int id;

    /** Turn the customer arrived. */
    private final int arrivalTurn;

    /** Current satisfaction, 0..100. */
    private int satisfaction = MAX_SATISFACTION;

    /** Where the customer is in their visit. */
    private CustomerState state = CustomerState.WAITING;

    /** Table the customer is sitting at, or null. */
    private Table table;

    /** The customer's order, or null before ordering. */
    private Order order;

    /** Turn the food was served, or -1. */
    private int servedTurn = -1;

    /** Turn the customer became READY_TO_PAY, or -1. */
    private int readyToPayTurn = -1;

    /**
     * @param id          customer number
     * @param arrivalTurn turn of arrival
     */
    protected Customer(int id, int arrivalTurn) {
        this.id = id;
        this.arrivalTurn = arrivalTurn;
    }

    // ---------------------------------------------------------------------
    // Behaviour that differs between customer types
    // ---------------------------------------------------------------------

    /**
     * @return short type label shown to the manager, e.g. "Regular"
     */
    public abstract String getTypeName();

    /**
     * @return satisfaction lost at the end of each turn while unserved
     */
    public abstract int getSatisfactionLossPerTurn();

    /**
     * Chooses what to eat. The manager never chooses the food.
     * Default rule: customer number n picks the menu item at position
     * (n - 1) mod menuSize, so customers cycle through the menu in order.
     *
     * @param menu the menu on offer
     * @return the chosen item
     */
    public MenuItem chooseDish(Menu menu) {
        return menu.get((id - 1) % menu.size());
    }

    /**
     * Works out this customer's bill for an item. Default: full price.
     *
     * @param item the item ordered
     * @return bill in cents
     */
    public int billFor(MenuItem item) {
        return item.getPriceCents();
    }

    /**
     * Hook called when the customer's dish is served. Default: no reaction.
     *
     * @param servedOrder the order just served
     * @param turn        current turn
     * @return a description of the reaction, or an empty string
     */
    protected String reactToService(Order servedOrder, int turn) {
        return "";
    }

    // ---------------------------------------------------------------------
    // Life cycle (state changes are validated here)
    // ---------------------------------------------------------------------

    /**
     * Seats the customer at a table.
     *
     * @param newTable a free table
     */
    public void seatAt(Table newTable) {
        requireState(CustomerState.WAITING);
        newTable.seat(this);
        this.table = newTable;
        state = CustomerState.SEATED;
    }

    /**
     * Records the order the Waiter took.
     *
     * @param newOrder the order
     */
    public void placeOrder(Order newOrder) {
        requireState(CustomerState.SEATED);
        this.order = newOrder;
        state = CustomerState.ORDERED;
    }

    /**
     * The Waiter serves the customer's READY dish.
     *
     * @param turn current turn
     * @return the customer's reaction to the service, or an empty string
     */
    public String serve(int turn) {
        requireState(CustomerState.ORDERED);
        order.markServed();
        servedTurn = turn;
        state = CustomerState.EATING;
        return reactToService(order, turn);
    }

    /**
     * A customer served on an earlier turn finishes eating and becomes READY_TO_PAY.
     *
     * @param turn current turn
     * @return true if the customer just finished eating
     */
    public boolean finishEatingIfDone(int turn) {
        if (state == CustomerState.EATING && servedTurn < turn) {
            state = CustomerState.READY_TO_PAY;
            readyToPayTurn = turn;
            return true;
        }
        return false;
    }

    /**
     * A customer who became READY_TO_PAY this turn pays next turn at the earliest.
     *
     * @param turn current turn
     * @return true if the Cashier may charge this customer now
     */
    public boolean canPay(int turn) {
        return state == CustomerState.READY_TO_PAY && readyToPayTurn < turn;
    }

    /**
     * Completes payment: the order is PAID and the table is freed.
     * Satisfaction stops changing from here, so it is the recorded value.
     */
    public void pay() {
        requireState(CustomerState.READY_TO_PAY);
        order.markPaid();
        releaseTable();
        state = CustomerState.PAID;
    }

    /**
     * End-of-turn patience check: an unserved customer loses satisfaction.
     *
     * @return true if satisfaction reached 0 and the customer must leave
     */
    public boolean losePatience() {
        if (!state.isAwaitingService()) {
            return false;
        }
        reduceSatisfaction(getSatisfactionLossPerTurn());
        return satisfaction == 0;
    }

    /**
     * The customer leaves without paying: unfinished order cancelled, table freed.
     */
    public void leave() {
        if (order != null) {
            order.cancel();
        }
        releaseTable();
        state = CustomerState.LEFT;
    }

    /**
     * How many more unserved turns this customer can take before leaving, at
     * their own loss rate (ignoring any Happy Hour recovery).
     *
     * @return turns left, or -1 if the customer is no longer waiting for service
     */
    public int turnsUntilLeaving() {
        if (!state.isAwaitingService()) {
            return -1;
        }
        int loss = getSatisfactionLossPerTurn();
        return (satisfaction + loss - 1) / loss;
    }

    /**
     * A customer still waiting for a table or food recovers satisfaction
     * (used by Happy Hour), never above {@link #MAX_SATISFACTION}.
     *
     * @param points points to recover
     * @return true if the customer was waiting and recovered
     */
    public boolean recover(int points) {
        if (!state.isAwaitingService()) {
            return false;
        }
        satisfaction = Math.min(MAX_SATISFACTION, satisfaction + points);
        return true;
    }

    /**
     * Lowers satisfaction, never below 0.
     *
     * @param points points to lose
     */
    protected void reduceSatisfaction(int points) {
        satisfaction = Math.max(0, satisfaction - points);
    }

    private void releaseTable() {
        if (table != null) {
            table.vacate();
            table = null;
        }
    }

    private void requireState(CustomerState expected) {
        if (state != expected) {
            throw new IllegalStateException("Customer #" + id + " is " + state + ", expected " + expected);
        }
    }

    // ---------------------------------------------------------------------
    // Read-only accessors
    // ---------------------------------------------------------------------

    public int getId() {
        return id;
    }

    public int getArrivalTurn() {
        return arrivalTurn;
    }

    public int getSatisfaction() {
        return satisfaction;
    }

    public CustomerState getState() {
        return state;
    }

    public Table getTable() {
        return table;
    }

    public Order getOrder() {
        return order;
    }

    public int getReadyToPayTurn() {
        return readyToPayTurn;
    }

    /**
     * @return e.g. "#3 Regular"
     */
    public String getLabel() {
        return "#" + id + " " + getTypeName();
    }
}
