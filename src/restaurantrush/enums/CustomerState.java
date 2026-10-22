package restaurantrush.enums;

/**
 * The stages a customer goes through, from arrival until they pay or leave.
 */
public enum CustomerState {
    /** In the waiting queue, no table yet. */
    WAITING,
    /** Seated at a table, order not yet taken. */
    SEATED,
    /** Order taken, waiting for the food. */
    ORDERED,
    /** Food served, customer is eating. */
    EATING,
    /** Finished eating, waiting for the Cashier. */
    READY_TO_PAY,
    /** Paid and left happy. */
    PAID,
    /** Satisfaction reached 0 and the customer left without paying. */
    LEFT;

    /**
     * Customers in these states are still waiting for service and lose
     * satisfaction at the end of each turn.
     *
     * @return true if the customer has not been served yet
     */
    public boolean isAwaitingService() {
        return this == WAITING || this == SEATED || this == ORDERED;
    }
}
