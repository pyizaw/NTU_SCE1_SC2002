package restaurantrush.staff;

import java.util.ArrayList;
import java.util.List;

import restaurantrush.model.ActionResult;
import restaurantrush.model.Restaurant;
import restaurantrush.model.customer.Customer;

/**
 * The Host seats at most one queued customer per turn, in addition to the
 * Waiter's task. The Host acts before the Waiter.
 */
public class Host extends Staff {

    @Override
    public String getRole() {
        return "Host";
    }

    /**
     * Seats a queued customer at a free table.
     *
     * @param restaurant the restaurant
     * @param customerId customer number
     * @return the result
     */
    public ActionResult seat(Restaurant restaurant, int customerId) {
        return perform(() -> restaurant.seatCustomer(customerId));
    }

    @Override
    public List<String> availableTasks(Restaurant restaurant, int turn) {
        List<String> tasks = new ArrayList<>();
        if (restaurant.findFreeTable().isPresent()) {
            for (Customer c : restaurant.getWaitingQueue()) {
                tasks.add("seat " + c.getId() + "   - seat " + c.getLabel());
            }
        }
        tasks.add("wait     - do nothing this turn");
        return tasks;
    }
}
