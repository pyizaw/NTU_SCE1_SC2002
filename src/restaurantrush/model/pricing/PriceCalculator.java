package restaurantrush.model.pricing;

import restaurantrush.model.MenuItem;
import restaurantrush.model.customer.Customer;

/**
 * Works out the bill when an order is taken: the customer's own pricing
 * first (e.g. VIP 10% off), then Happy Hour if it is active. The result is
 * locked into the order, so later changes never affect it.
 * <p>Example: a VIP orders a Burger during Happy Hour: $15 x 0.9 x 0.8 = $10.80.</p>
 */
public class PriceCalculator {

    /** The Happy Hour policy. */
    private final HappyHour happyHour;

    /**
     * @param happyHour the Happy Hour policy
     */
    public PriceCalculator(HappyHour happyHour) {
        this.happyHour = happyHour;
    }

    /**
     * @param customer who is ordering
     * @param item     what they ordered
     * @param turn     turn the order is taken
     * @return the bill in cents
     */
    public int priceFor(Customer customer, MenuItem item, int turn) {
        int customerPrice = customer.billFor(item);
        return happyHour.apply(customerPrice, turn);
    }
}
