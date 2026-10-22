package restaurantrush.controller;

import restaurantrush.model.customer.Customer;

/**
 * Creates the customer scheduled for an arrival slot. Lets the arrival
 * schedule refer to customer types by constructor (e.g. {@code RegularCustomer::new})
 * without the controller ever checking which type it is.
 */
@FunctionalInterface
public interface CustomerFactory {

    /**
     * @param id          customer number
     * @param arrivalTurn turn of arrival
     * @return the new customer
     */
    Customer create(int id, int arrivalTurn);
}
