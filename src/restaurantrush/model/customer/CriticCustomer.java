package restaurantrush.model.customer;

import java.util.Comparator;

import restaurantrush.model.Menu;
import restaurantrush.model.MenuItem;
import restaurantrush.model.Order;

/**
 * A food critic: impatient (loses 12 satisfaction per unserved turn) and pays full price.
 * <ul>
 *   <li><b>Order preference:</b> always tests the kitchen with the most
 *       demanding item - the one with the most preparation units (ties go to
 *       the cheaper item).</li>
 *   <li><b>Reaction to service:</b> hates cold food. If the dish was left
 *       READY for more than one turn before it was served, the critic loses
 *       {@value #COLD_FOOD_PENALTY} satisfaction straight away.</li>
 * </ul>
 */
public class CriticCustomer extends Customer {

    /** Satisfaction lost per waiting turn. */
    private static final int LOSS_PER_TURN = 12;

    /** Satisfaction lost when served a dish that has gone cold. */
    public static final int COLD_FOOD_PENALTY = 20;

    /**
     * @param id          customer number
     * @param arrivalTurn turn of arrival
     */
    public CriticCustomer(int id, int arrivalTurn) {
        super(id, arrivalTurn);
    }

    @Override
    public String getTypeName() {
        return "Critic";
    }

    @Override
    public int getSatisfactionLossPerTurn() {
        return LOSS_PER_TURN;
    }

    /**
     * Picks the item with the most preparation units; ties go to the cheaper item.
     */
    @Override
    public MenuItem chooseDish(Menu menu) {
        return menu.getItems().stream()
                .max(Comparator.comparingInt(MenuItem::getPrepUnits)
                        .thenComparing(Comparator.comparingInt(MenuItem::getPriceCents).reversed()))
                .orElseThrow();
    }

    /**
     * A dish served on the turn right after it became READY is fresh; any later is cold.
     */
    @Override
    protected String reactToService(Order servedOrder, int turn) {
        int turnsLeftWaiting = turn - servedOrder.getReadyTurn();
        if (turnsLeftWaiting > 1) {
            reduceSatisfaction(COLD_FOOD_PENALTY);
            return "The Critic complains the " + servedOrder.getItem().getName() + " went cold: -"
                    + COLD_FOOD_PENALTY + " satisfaction (now " + getSatisfaction() + ").";
        }
        return "The Critic approves: the dish arrived hot.";
    }
}
