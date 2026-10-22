package restaurantrush.model;

import restaurantrush.util.Money;

/**
 * A dish that customers can order. Its price and preparation units are fixed
 * when it is created and cannot be changed afterwards.
 */
public class MenuItem {

    /** Display name, e.g. "Pasta". */
    private final String name;

    /** Price in cents. */
    private final int priceCents;

    /** How many Chef turns the dish needs. */
    private final int prepUnits;

    /**
     * Creates a menu item.
     *
     * @param name       display name
     * @param priceCents price in cents, must be positive
     * @param prepUnits  preparation units, must be at least 1
     */
    public MenuItem(String name, int priceCents, int prepUnits) {
        if (priceCents <= 0 || prepUnits < 1) {
            throw new IllegalArgumentException("Invalid menu item: " + name);
        }
        this.name = name;
        this.priceCents = priceCents;
        this.prepUnits = prepUnits;
    }

    public String getName() {
        return name;
    }

    public int getPriceCents() {
        return priceCents;
    }

    public int getPrepUnits() {
        return prepUnits;
    }

    @Override
    public String toString() {
        return name + " (" + Money.format(priceCents) + ", " + prepUnits + " unit" + (prepUnits > 1 ? "s" : "") + ")";
    }
}
