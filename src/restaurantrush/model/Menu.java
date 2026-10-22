package restaurantrush.model;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * The restaurant's menu: an ordered list of the items customers can choose.
 * The order matters because customers choose by position (see
 * {@link restaurantrush.model.customer.Customer#chooseDish(Menu)}).
 */
public class Menu {

    /** Items in menu order. */
    private final List<MenuItem> items = new ArrayList<>();

    /**
     * Builds the base-setting menu: Salad, Burger, Pasta.
     *
     * @return the standard menu
     */
    public static Menu standard() {
        Menu menu = new Menu();
        menu.add(new MenuItem("Salad", 900, 1));
        menu.add(new MenuItem("Burger", 1500, 1));
        menu.add(new MenuItem("Pasta", 1800, 2));
        return menu;
    }

    /**
     * Builds the Stage 3 menu: the standard dishes followed by every two-item
     * combo, in the order Salad + Burger, Salad + Pasta, Burger + Pasta.
     *
     * @return the menu with combos
     */
    public static Menu withCombos() {
        Menu menu = standard();
        int dishCount = menu.size();
        for (int i = 0; i < dishCount; i++) {
            for (int j = i + 1; j < dishCount; j++) {
                menu.add(new ComboMeal(menu.get(i), menu.get(j)));
            }
        }
        return menu;
    }

    /**
     * Adds an item to the end of the menu.
     *
     * @param item item to add
     */
    public void add(MenuItem item) {
        items.add(item);
    }

    /**
     * @return a read-only view of the items in menu order
     */
    public List<MenuItem> getItems() {
        return Collections.unmodifiableList(items);
    }

    /**
     * @param index position in the menu (0-based)
     * @return the item at that position
     */
    public MenuItem get(int index) {
        return items.get(index);
    }

    public int size() {
        return items.size();
    }
}
