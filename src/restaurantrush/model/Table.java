package restaurantrush.model;

import restaurantrush.model.customer.Customer;

/**
 * A table in the restaurant. It stays occupied until its customer pays or leaves.
 */
public class Table {

    /** Table number shown to the manager (1-based). */
    private final int number;

    /** The customer sitting here, or null if free. */
    private Customer occupant;

    /**
     * @param number table number
     */
    public Table(int number) {
        this.number = number;
    }

    /**
     * Seats a customer at this table.
     *
     * @param customer the customer to seat
     */
    public void seat(Customer customer) {
        if (occupant != null) {
            throw new IllegalStateException("Table " + number + " is occupied");
        }
        occupant = customer;
    }

    /** Frees the table after the customer pays or leaves. */
    public void vacate() {
        occupant = null;
    }

    public boolean isFree() {
        return occupant == null;
    }

    public int getNumber() {
        return number;
    }

    public Customer getOccupant() {
        return occupant;
    }
}
