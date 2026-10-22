package restaurantrush.model;

import restaurantrush.enums.OrderStatus;
import restaurantrush.model.customer.Customer;

/**
 * One customer's order. The bill is locked in when the order is taken, so later
 * price changes never affect it. The order tracks its own preparation progress
 * and only allows legal status changes.
 */
public class Order {

    /** The customer who placed this order. */
    private final Customer customer;

    /** What was ordered. */
    private final MenuItem item;

    /** Bill in cents, fixed when the order was taken. */
    private final int priceCents;

    /** Turn on which the Waiter took the order. */
    private final int takenTurn;

    /** Preparation units completed so far. */
    private int unitsDone;

    /** Current status. */
    private OrderStatus status = OrderStatus.PREPARING;

    /** Turn on which the last preparation unit was done, or -1. */
    private int readyTurn = -1;

    /**
     * Creates an order with a locked price.
     *
     * @param customer   who ordered
     * @param item       what was ordered
     * @param priceCents bill in cents
     * @param takenTurn  turn the order was taken
     */
    public Order(Customer customer, MenuItem item, int priceCents, int takenTurn) {
        this.customer = customer;
        this.item = item;
        this.priceCents = priceCents;
        this.takenTurn = takenTurn;
    }

    /**
     * Does one preparation unit. When the last unit is done the order becomes READY.
     *
     * @param turn current turn
     * @return true if this unit made the order READY
     */
    public boolean prepareOneUnit(int turn) {
        if (status != OrderStatus.PREPARING) {
            throw new IllegalStateException("Order is not being prepared");
        }
        unitsDone++;
        if (unitsDone == item.getPrepUnits()) {
            status = OrderStatus.READY;
            readyTurn = turn;
            return true;
        }
        return false;
    }

    /**
     * A dish can only be served if it was already READY before this turn's
     * Waiter action, i.e. it became READY on an earlier turn.
     *
     * @param turn current turn
     * @return true if the Waiter may serve it now
     */
    public boolean canBeServed(int turn) {
        return status == OrderStatus.READY && readyTurn < turn;
    }

    /** Marks the order as served. */
    public void markServed() {
        if (status != OrderStatus.READY) {
            throw new IllegalStateException("Only READY orders can be served");
        }
        status = OrderStatus.SERVED;
    }

    /** Marks the order as paid. */
    public void markPaid() {
        if (status != OrderStatus.SERVED) {
            throw new IllegalStateException("Only SERVED orders can be paid");
        }
        status = OrderStatus.PAID;
    }

    /** Cancels an unfinished order when its customer leaves. */
    public void cancel() {
        if (status == OrderStatus.PREPARING || status == OrderStatus.READY) {
            status = OrderStatus.CANCELLED;
        }
    }

    public int getId() {
        return customer.getId();
    }

    public Customer getCustomer() {
        return customer;
    }

    public MenuItem getItem() {
        return item;
    }

    public int getPriceCents() {
        return priceCents;
    }

    public int getTakenTurn() {
        return takenTurn;
    }

    public int getUnitsDone() {
        return unitsDone;
    }

    public int getReadyTurn() {
        return readyTurn;
    }

    public OrderStatus getStatus() {
        return status;
    }

    /**
     * @return a short progress label, e.g. "PREPARING 1/2"
     */
    public String describeStatus() {
        if (status == OrderStatus.PREPARING) {
            return status + " " + unitsDone + "/" + item.getPrepUnits();
        }
        return status.toString();
    }
}
