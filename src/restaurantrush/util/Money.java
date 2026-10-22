package restaurantrush.util;

/**
 * Helper for money values. All amounts in the game are stored as integer
 * cents to avoid floating-point rounding errors, and are only turned into
 * dollars when they are displayed.
 */
public final class Money {

    private Money() {
    }

    /**
     * Formats an amount of cents as dollars with two decimals, e.g. 1830 -> "$18.30".
     *
     * @param cents amount in cents
     * @return formatted dollar string
     */
    public static String format(int cents) {
        return String.format("$%d.%02d", cents / 100, cents % 100);
    }

    /**
     * Applies a percentage to an amount of cents, rounding to the nearest cent
     * (halves round up).
     *
     * @param cents   amount in cents
     * @param percent percentage to keep, e.g. 90 for "10% off"
     * @return the rounded result in cents
     */
    public static int percentOf(int cents, int percent) {
        return (int) Math.round(cents * percent / 100.0);
    }
}
