package restaurantrush.model;

import restaurantrush.util.Money;

/**
 * A combo made of two existing menu items. It is still a {@link MenuItem}, so
 * orders, the Chef and pricing treat it like any other dish:
 * <ul>
 *   <li>preparation units = sum of the two components;</li>
 *   <li>price = 90% of the components' combined price, rounded to the nearest cent.</li>
 * </ul>
 */
public class ComboMeal extends MenuItem {

    /** Percentage of the combined price that a combo costs. */
    private static final int COMBO_PRICE_PERCENT = 90;

    /** First component. */
    private final MenuItem first;

    /** Second component. */
    private final MenuItem second;

    /**
     * @param first  first component
     * @param second second component
     */
    public ComboMeal(MenuItem first, MenuItem second) {
        super(first.getName() + " + " + second.getName() + " Combo",
                Money.percentOf(first.getPriceCents() + second.getPriceCents(), COMBO_PRICE_PERCENT),
                first.getPrepUnits() + second.getPrepUnits());
        this.first = first;
        this.second = second;
    }

    public MenuItem getFirst() {
        return first;
    }

    public MenuItem getSecond() {
        return second;
    }
}
