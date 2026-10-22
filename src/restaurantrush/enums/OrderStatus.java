package restaurantrush.enums;

/**
 * Status of an order in the kitchen-to-payment flow.
 */
public enum OrderStatus {
    /** Taken by the Waiter; the Chef still has preparation units to do. */
    PREPARING,
    /** All preparation units done; waiting for the Waiter to serve it. */
    READY,
    /** Served to the customer. */
    SERVED,
    /** Paid at the Cashier. */
    PAID,
    /** The customer left before the order was finished. */
    CANCELLED
}
