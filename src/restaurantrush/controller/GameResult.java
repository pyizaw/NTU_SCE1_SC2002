package restaurantrush.controller;

import java.util.OptionalDouble;

import restaurantrush.model.Restaurant;

/**
 * The final result after the last turn, checked against the three targets.
 */
public final class GameResult {

    /** Customers who paid. */
    private final int paidCustomers;

    /** Revenue in cents. */
    private final int revenueCents;

    /** Average satisfaction of paid customers, empty if none paid. */
    private final OptionalDouble averageSatisfaction;

    /** Whether each target was met. */
    private final boolean paidTargetMet;
    private final boolean revenueTargetMet;
    private final boolean satisfactionTargetMet;

    /** The settings the result was judged against. */
    private final GameConfig config;

    private GameResult(GameConfig config, Restaurant restaurant) {
        this.config = config;
        this.paidCustomers = restaurant.getPaidCount();
        this.revenueCents = restaurant.getRevenueCents();
        this.averageSatisfaction = restaurant.getAveragePaidSatisfaction();
        this.paidTargetMet = paidCustomers >= config.getMinPaidCustomers();
        this.revenueTargetMet = revenueCents >= config.getMinRevenueCents();
        this.satisfactionTargetMet = averageSatisfaction.isPresent()
                && averageSatisfaction.getAsDouble() >= config.getMinAverageSatisfaction();
    }

    /**
     * Judges the restaurant's results.
     *
     * @param config     targets
     * @param restaurant final state
     * @return the result
     */
    public static GameResult evaluate(GameConfig config, Restaurant restaurant) {
        return new GameResult(config, restaurant);
    }

    /**
     * @return true only if all three targets were met
     */
    public boolean isVictory() {
        return paidTargetMet && revenueTargetMet && satisfactionTargetMet;
    }

    public int getPaidCustomers() {
        return paidCustomers;
    }

    public int getRevenueCents() {
        return revenueCents;
    }

    public OptionalDouble getAverageSatisfaction() {
        return averageSatisfaction;
    }

    public boolean isPaidTargetMet() {
        return paidTargetMet;
    }

    public boolean isRevenueTargetMet() {
        return revenueTargetMet;
    }

    public boolean isSatisfactionTargetMet() {
        return satisfactionTargetMet;
    }

    public GameConfig getConfig() {
        return config;
    }
}
