package restaurantrush.staff;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

import restaurantrush.enums.CustomerState;
import restaurantrush.model.ActionResult;
import restaurantrush.model.Menu;
import restaurantrush.model.MenuItem;
import restaurantrush.model.Order;
import restaurantrush.model.Restaurant;
import restaurantrush.model.customer.Customer;
import restaurantrush.model.pricing.PriceCalculator;
import restaurantrush.util.Money;

/**
 * The Waiter does one task per turn: seat a queued customer, take a seated
 * customer's order, or serve a READY dish.
 */
public class Waiter extends Staff {

    /** The menu customers order from. */
    private final Menu menu;

    /** Works out the bill when an order is taken. */
    private final PriceCalculator pricing;

    /**
     * @param menu    the menu customers order from
     * @param pricing works out the bill when an order is taken
     */
    public Waiter(Menu menu, PriceCalculator pricing) {
        this.menu = menu;
        this.pricing = pricing;
    }

    @Override
    public String getRole() {
        return "Waiter";
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

    /**
     * Takes a seated customer's order. The customer chooses the dish and the
     * bill is locked in now.
     *
     * @param restaurant the restaurant
     * @param customerId customer number
     * @param turn       current turn
     * @return the result
     */
    public ActionResult takeOrder(Restaurant restaurant, int customerId, int turn) {
        return perform(() -> {
            Optional<Customer> found = restaurant.findCustomer(customerId);
            if (found.isEmpty()) {
                return ActionResult.fail("There is no customer #" + customerId + ".");
            }
            Customer customer = found.get();
            if (customer.getState() != CustomerState.SEATED) {
                return ActionResult.fail("Cannot take an order from " + customer.getLabel()
                        + ": the customer must be seated and not have ordered yet (currently " + customer.getState() + ").");
            }
            MenuItem dish = customer.chooseDish(menu);
            int bill = pricing.priceFor(customer, dish, turn);
            customer.placeOrder(new Order(customer, dish, bill, turn));
            return ActionResult.ok("Customer " + customer.getLabel() + " orders " + dish + ". Bill locked at " + Money.format(bill) + ".");
        });
    }

    /**
     * Serves a dish that was already READY before this turn.
     *
     * @param restaurant the restaurant
     * @param customerId customer number
     * @param turn       current turn
     * @return the result
     */
    public ActionResult serve(Restaurant restaurant, int customerId, int turn) {
        return perform(() -> {
            Optional<Customer> found = restaurant.findCustomer(customerId);
            if (found.isEmpty()) {
                return ActionResult.fail("There is no customer #" + customerId + ".");
            }
            Customer customer = found.get();
            Order order = customer.getOrder();
            if (customer.getState() != CustomerState.ORDERED || order == null) {
                return ActionResult.fail("Customer " + customer.getLabel() + " has no dish waiting to be served (currently " + customer.getState() + ").");
            }
            if (!order.canBeServed(turn)) {
                return ActionResult.fail("Order #" + order.getId() + " is not READY yet (" + order.describeStatus() + ").");
            }
            String reaction = customer.serve(turn);
            restaurant.recordServed();
            return ActionResult.ok("Served " + order.getItem().getName() + " to " + customer.getLabel() + "."
                    + (reaction.isEmpty() ? "" : " " + reaction));
        });
    }

    @Override
    public List<String> availableTasks(Restaurant restaurant, int turn) {
        List<String> tasks = new ArrayList<>();
        boolean tableFree = restaurant.findFreeTable().isPresent();
        for (Customer c : restaurant.getWaitingQueue()) {
            if (tableFree) {
                tasks.add("seat " + c.getId() + "   - seat " + c.getLabel());
            }
        }
        for (Customer c : restaurant.getSeatedCustomers()) {
            if (c.getState() == CustomerState.SEATED) {
                tasks.add("order " + c.getId() + "  - take " + c.getLabel() + "'s order");
            } else if (c.getState() == CustomerState.ORDERED && c.getOrder().canBeServed(turn)) {
                tasks.add("serve " + c.getId() + "  - serve " + c.getOrder().getItem().getName() + " to " + c.getLabel());
            }
        }
        tasks.add("wait     - do nothing this turn");
        return tasks;
    }
}
