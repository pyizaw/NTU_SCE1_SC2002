package restaurantrush.model.customer;

import restaurantrush.model.MenuItem;
import restaurantrush.util.Money;

/**
 * A VIP customer: more patient (loses 5 satisfaction per unserved turn) and
 * gets 10% off the bill.
 */
public class VIPCustomer extends Customer {

    /** Satisfaction lost per waiting turn. */
    private static final int LOSS_PER_TURN = 5;

    /** Percentage of the price a VIP pays (10% off). */
    private static final int PRICE_PERCENT = 90;

    /**
     * @param id          customer number
     * @param arrivalTurn turn of arrival
     */
    public VIPCustomer(int id, int arrivalTurn) {
        super(id, arrivalTurn);
    }

    @Override
    public String getTypeName() {
        return "VIP";
    }

    @Override
    public int getSatisfactionLossPerTurn() {
        return LOSS_PER_TURN;
    }

    /**
     * VIPs pay 90% of the price, rounded to the nearest cent.
     */
    @Override
    public int billFor(MenuItem item) {
        return Money.percentOf(super.billFor(item), PRICE_PERCENT);
    }
}
