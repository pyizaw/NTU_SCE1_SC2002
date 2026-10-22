package restaurantrush.staff;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

import restaurantrush.enums.OrderStatus;
import restaurantrush.model.ActionResult;
import restaurantrush.model.Order;
import restaurantrush.model.Restaurant;
import restaurantrush.model.customer.Customer;

/**
 * The Chef advances one order by one preparation unit per turn.
 */
public class Chef extends Staff {

    @Override
    public String getRole() {
        return "Chef";
    }

    /**
     * Does one preparation unit on an order. The Chef may work on an order the
     * Waiter took earlier in the same turn.
     *
     * @param restaurant the restaurant
     * @param orderId    order number (same as the customer number)
     * @param turn       current turn
     * @return the result
     */
    public ActionResult cook(Restaurant restaurant, int orderId, int turn) {
        return perform(() -> {
            Optional<Customer> found = restaurant.findCustomer(orderId);
            Order order = found.map(Customer::getOrder).orElse(null);
            if (order == null) {
                return ActionResult.fail("There is no order #" + orderId + ".");
            }
            if (order.getStatus() != OrderStatus.PREPARING) {
                return ActionResult.fail("Order #" + orderId + " does not need cooking (" + order.getStatus() + ").");
            }
            boolean ready = order.prepareOneUnit(turn);
            String dish = order.getItem().getName();
            return ActionResult.ok(ready
                    ? "Chef finished " + dish + " for order #" + orderId + ": READY (can be served next turn)."
                    : "Chef worked on " + dish + " for order #" + orderId + ": " + order.describeStatus() + ".");
        });
    }

    /**
     * @param restaurant the restaurant
     * @return orders still being prepared, in arrival order
     */
    public List<Order> ordersToCook(Restaurant restaurant) {
        List<Order> orders = new ArrayList<>();
        for (Customer c : restaurant.getCustomers()) {
            Order o = c.getOrder();
            if (o != null && o.getStatus() == OrderStatus.PREPARING) {
                orders.add(o);
            }
        }
        return orders;
    }

    @Override
    public List<String> availableTasks(Restaurant restaurant, int turn) {
        List<String> tasks = new ArrayList<>();
        for (Order o : ordersToCook(restaurant)) {
            tasks.add("cook " + o.getId() + "   - " + o.getItem().getName() + " for " + o.getCustomer().getLabel() + " (" + o.describeStatus() + ")");
        }
        tasks.add("wait     - do nothing this turn");
        return tasks;
    }
}
