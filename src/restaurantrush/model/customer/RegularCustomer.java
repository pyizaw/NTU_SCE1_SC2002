package restaurantrush.model.customer;

/**
 * An ordinary customer: loses 8 satisfaction per unserved turn and pays full price.
 */
public class RegularCustomer extends Customer {

    /** Satisfaction lost per waiting turn. */
    private static final int LOSS_PER_TURN = 8;

    /**
     * @param id          customer number
     * @param arrivalTurn turn of arrival
     */
    public RegularCustomer(int id, int arrivalTurn) {
        super(id, arrivalTurn);
    }

    @Override
    public String getTypeName() {
        return "Regular";
    }

    @Override
    public int getSatisfactionLossPerTurn() {
        return LOSS_PER_TURN;
    }
}
