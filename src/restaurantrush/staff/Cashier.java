package restaurantrush.staff;

import java.util.List;
import java.util.Optional;

import restaurantrush.model.ActionResult;
import restaurantrush.model.Restaurant;
import restaurantrush.model.customer.Customer;
import restaurantrush.util.Money;

/**
 * The Cashier works automatically: each turn it charges at most one
 * READY_TO_PAY customer, oldest first.
 */
public class Cashier extends Staff {

    @Override
    public String getRole() {
        return "Cashier";
    }

    /**
     * Charges the next customer who may pay this turn, if any.
     *
     * @param restaurant the restaurant
     * @param turn       current turn
     * @return the result, or empty if nobody could pay
     */
    public Optional<ActionResult> collectNextPayment(Restaurant restaurant, int turn) {
        Optional<Customer> next = restaurant.nextCustomerToPay(turn);
        if (next.isEmpty()) {
            return Optional.empty();
        }
        Customer customer = next.get();
        return Optional.of(perform(() -> {
            int bill = restaurant.collectPayment(customer);
            return ActionResult.ok("Cashier collected " + Money.format(bill) + " from " + customer.getLabel()
                    + ". PAID with satisfaction " + customer.getSatisfaction() + "; table freed.");
        }));
    }

    @Override
    public List<String> availableTasks(Restaurant restaurant, int turn) {
        return restaurant.nextCustomerToPay(turn)
                .map(c -> List.of("(automatic) charge " + c.getLabel()))
                .orElse(List.of("(automatic) nobody ready to pay"));
    }
}
