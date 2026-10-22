package restaurantrush.controller;

import java.util.Collections;
import java.util.Map;
import java.util.TreeMap;

import restaurantrush.model.customer.CriticCustomer;
import restaurantrush.model.customer.RegularCustomer;
import restaurantrush.model.customer.VIPCustomer;

/**
 * The settings of a game: length, tables, arrival schedule and victory targets.
 * {@link #baseSetting()} is the fixed setting a TA can use to reproduce runs.
 */
public final class GameConfig {

    /** Number of turns; the game always plays all of them. */
    private final int maxTurns;

    /** Number of tables. */
    private final int tableCount;

    /** Turn -> who arrives at the start of that turn. */
    private final Map<Integer, CustomerFactory> arrivals;

    /** Victory target: minimum paid customers. */
    private final int minPaidCustomers;

    /** Victory target: minimum revenue in cents. */
    private final int minRevenueCents;

    /** Victory target: minimum average satisfaction of paid customers. */
    private final int minAverageSatisfaction;

    private GameConfig(int maxTurns, int tableCount, Map<Integer, CustomerFactory> arrivals,
                       int minPaidCustomers, int minRevenueCents, int minAverageSatisfaction) {
        this.maxTurns = maxTurns;
        this.tableCount = tableCount;
        this.arrivals = Collections.unmodifiableMap(new TreeMap<>(arrivals));
        this.minPaidCustomers = minPaidCustomers;
        this.minRevenueCents = minRevenueCents;
        this.minAverageSatisfaction = minAverageSatisfaction;
    }

    /**
     * Base setting from the brief: 18 turns, 2 tables, one customer at the
     * start of turns 1, 4, 7, 10, 13 and 16; win with at least 4 paid
     * customers, $45 revenue and average satisfaction 60. The turn 4 arrival
     * is a VIP and the turn 7 arrival is a Critic; the rest are Regular.
     *
     * @return the base setting
     */
    public static GameConfig baseSetting() {
        Map<Integer, CustomerFactory> arrivals = new TreeMap<>();
        arrivals.put(1, RegularCustomer::new);
        arrivals.put(4, VIPCustomer::new);
        arrivals.put(7, CriticCustomer::new);
        arrivals.put(10, RegularCustomer::new);
        arrivals.put(13, RegularCustomer::new);
        arrivals.put(16, RegularCustomer::new);
        return new GameConfig(18, 2, arrivals, 4, 4500, 60);
    }

    public int getMaxTurns() {
        return maxTurns;
    }

    public int getTableCount() {
        return tableCount;
    }

    /**
     * @return read-only arrival schedule, ordered by turn
     */
    public Map<Integer, CustomerFactory> getArrivals() {
        return arrivals;
    }

    /**
     * Tells the player which kind of customer is scheduled for a turn, so a
     * view can preview upcoming arrivals. A sample customer is created only to
     * ask its type; it never enters the game.
     *
     * @param turn a scheduled arrival turn
     * @return the type name, e.g. "Critic", or an empty string if nobody arrives
     */
    public String arrivalTypeName(int turn) {
        CustomerFactory factory = arrivals.get(turn);
        return factory == null ? "" : factory.create(0, turn).getTypeName();
    }

    public int getMinPaidCustomers() {
        return minPaidCustomers;
    }

    public int getMinRevenueCents() {
        return minRevenueCents;
    }

    public int getMinAverageSatisfaction() {
        return minAverageSatisfaction;
    }
}
