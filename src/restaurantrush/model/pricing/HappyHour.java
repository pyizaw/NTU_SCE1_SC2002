package restaurantrush.model.pricing;

import restaurantrush.util.Money;

/**
 * Happy Hour: a pricing policy the manager may switch on once per game, at
 * the start of a turn. It lasts for that turn and the next. Orders taken
 * while it is active get 20% off, and customers still waiting for a table or
 * food recover satisfaction at the start of each active turn.
 */
public class HappyHour {

    /** Number of turns Happy Hour lasts (the activation turn and the next). */
    public static final int DURATION = 2;

    /** Percentage of the price paid during Happy Hour (20% off). */
    public static final int PRICE_PERCENT = 80;

    /** Satisfaction recovered at the start of each active turn. */
    public static final int RECOVERY = 15;

    /** Turn it was switched on, or -1 if never. */
    private int activatedTurn = -1;

    /**
     * @return true if it has not been used yet this game
     */
    public boolean canActivate() {
        return activatedTurn < 0;
    }

    /**
     * Switches Happy Hour on for this turn and the next.
     *
     * @param turn current turn
     */
    public void activate(int turn) {
        if (!canActivate()) {
            throw new IllegalStateException("Happy Hour can only be used once per game");
        }
        activatedTurn = turn;
    }

    /**
     * @param turn a turn
     * @return true if Happy Hour is active during that turn
     */
    public boolean isActive(int turn) {
        return activatedTurn > 0 && turn >= activatedTurn && turn < activatedTurn + DURATION;
    }

    /**
     * @param turn current turn
     * @return active turns left including this one, or 0 if not active
     */
    public int turnsRemaining(int turn) {
        return isActive(turn) ? activatedTurn + DURATION - turn : 0;
    }

    /**
     * Applies the Happy Hour discount to a bill if it is active.
     *
     * @param cents bill so far
     * @param turn  turn the order is taken
     * @return the discounted bill, or the same bill if not active
     */
    public int apply(int cents, int turn) {
        return isActive(turn) ? Money.percentOf(cents, PRICE_PERCENT) : cents;
    }

    /**
     * @param turn current turn
     * @return a status line for the display
     */
    public String describe(int turn) {
        if (isActive(turn)) {
            int left = turnsRemaining(turn);
            return "Happy Hour ACTIVE - " + left + " turn" + (left > 1 ? "s" : "") + " remaining (including this one)";
        }
        return canActivate() ? "Happy Hour available (can be used once)" : "Happy Hour already used";
    }
}
