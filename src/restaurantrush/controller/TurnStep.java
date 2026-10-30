package restaurantrush.controller;

/**
 * The steps of one turn, in the order the controller runs them. Views use
 * these to label what is happening; the rules of each step live in the
 * staff and domain objects.
 */
public enum TurnStep {
    /** The scheduled customer joins the waiting queue. */
    ARRIVALS("1", "Arrivals"),
    /** The manager gives each staff member one task; the Waiter acts before the Chef. */
    MANAGER_CHOICES("2-3", "Manager choices, kitchen and serving"),
    /** Served customers finish eating; the Cashier charges one customer. */
    EATING_AND_PAYMENT("4", "Eating and payment"),
    /** Unserved customers lose satisfaction; anyone at 0 leaves without paying. */
    WAITING_AND_ABANDONMENT("5", "Waiting and abandonment");

    /** Step number(s) as written in the brief. */
    private final String number;

    /** Short title. */
    private final String title;

    TurnStep(String number, String title) {
        this.number = number;
        this.title = title;
    }

    public String getNumber() {
        return number;
    }

    public String getTitle() {
        return title;
    }
}
