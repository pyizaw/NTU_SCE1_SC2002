package restaurantrush.model;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.Optional;
import java.util.OptionalDouble;

import restaurantrush.enums.CustomerState;
import restaurantrush.model.customer.Customer;

/**
 * The restaurant floor: tables, the waiting queue, every customer who has
 * arrived, and the running results (revenue, paid satisfaction, departures).
 * Staff objects act on the restaurant through these methods, so the rules for
 * seating, payment and abandonment live in one place.
 */
public class Restaurant {

    /** The tables, numbered from 1. */
    private final List<Table> tables = new ArrayList<>();

    /** Customers waiting for a table, in arrival order. */
    private final List<Customer> waitingQueue = new ArrayList<>();

    /** Every customer who has arrived, in arrival order. */
    private final List<Customer> customers = new ArrayList<>();

    /** Satisfaction recorded at the moment each customer paid. */
    private final List<Integer> paidSatisfactions = new ArrayList<>();

    /** Revenue from paid orders only, in cents. */
    private int revenueCents;

    /** Number of dishes served. */
    private int servedCount;

    /** Customers who left because satisfaction reached 0. */
    private int unhappyDepartures;

    /**
     * @param tableCount number of tables
     */
    public Restaurant(int tableCount) {
        for (int i = 1; i <= tableCount; i++) {
            tables.add(new Table(i));
        }
    }

    /**
     * A newly arrived customer joins the waiting queue.
     *
     * @param customer the arriving customer
     */
    public void admit(Customer customer) {
        customers.add(customer);
        waitingQueue.add(customer);
    }

    /**
     * Finds a customer by number.
     *
     * @param id customer number
     * @return the customer, if one with that number has arrived
     */
    public Optional<Customer> findCustomer(int id) {
        return customers.stream().filter(c -> c.getId() == id).findFirst();
    }

    /**
     * @return the lowest-numbered free table, if any
     */
    public Optional<Table> findFreeTable() {
        return tables.stream().filter(Table::isFree).findFirst();
    }

    /**
     * Seats a queued customer at the first free table. Used by any staff member
     * who is allowed to seat customers.
     *
     * @param customerId customer number
     * @return the result, with a reason if seating was not possible
     */
    public ActionResult seatCustomer(int customerId) {
        Optional<Customer> found = findCustomer(customerId);
        if (found.isEmpty()) {
            return ActionResult.fail("There is no customer #" + customerId + ".");
        }
        Customer customer = found.get();
        if (customer.getState() != CustomerState.WAITING) {
            return ActionResult.fail("Customer " + customer.getLabel() + " is not in the waiting queue (" + customer.getState() + ").");
        }
        Optional<Table> table = findFreeTable();
        if (table.isEmpty()) {
            return ActionResult.fail("No free table: every table is occupied until its customer pays or leaves.");
        }
        customer.seatAt(table.get());
        waitingQueue.remove(customer);
        return ActionResult.ok("Customer " + customer.getLabel() + " is seated at Table " + table.get().getNumber() + ".");
    }

    /** Counts one more served dish. */
    public void recordServed() {
        servedCount++;
    }

    /**
     * Customers served on an earlier turn finish eating.
     *
     * @param turn current turn
     * @return customers who became READY_TO_PAY this turn
     */
    public List<Customer> finishEating(int turn) {
        List<Customer> finished = new ArrayList<>();
        for (Customer c : customers) {
            if (c.finishEatingIfDone(turn)) {
                finished.add(c);
            }
        }
        return finished;
    }

    /**
     * The oldest customer allowed to pay this turn: earliest to become
     * READY_TO_PAY, ties broken by arrival order.
     *
     * @param turn current turn
     * @return the next customer to charge, if any
     */
    public Optional<Customer> nextCustomerToPay(int turn) {
        return customers.stream()
                .filter(c -> c.canPay(turn))
                .min(Comparator.comparingInt(Customer::getReadyToPayTurn).thenComparingInt(Customer::getId));
    }

    /**
     * Charges a customer: adds the bill to revenue, records satisfaction and frees the table.
     *
     * @param customer a customer who may pay this turn
     * @return the amount collected in cents
     */
    public int collectPayment(Customer customer) {
        int bill = customer.getOrder().getPriceCents();
        customer.pay();
        revenueCents += bill;
        paidSatisfactions.add(customer.getSatisfaction());
        return bill;
    }

    /**
     * End-of-turn patience: every unserved customer loses satisfaction, and
     * those who reach 0 leave without paying.
     *
     * @return customers who left this turn
     */
    public List<Customer> applyEndOfTurnPatience() {
        List<Customer> leaving = new ArrayList<>();
        for (Customer c : customers) {
            if (c.losePatience()) {
                leaving.add(c);
            }
        }
        for (Customer c : leaving) {
            c.leave();
            waitingQueue.remove(c);
            unhappyDepartures++;
        }
        return leaving;
    }

    /**
     * Every customer still waiting for a table or food recovers satisfaction.
     *
     * @param points points to recover
     * @return customers who recovered
     */
    public List<Customer> recoverWaitingCustomers(int points) {
        List<Customer> recovered = new ArrayList<>();
        for (Customer c : customers) {
            if (c.recover(points)) {
                recovered.add(c);
            }
        }
        return recovered;
    }

    // ---------------------------------------------------------------------
    // Read-only views and statistics
    // ---------------------------------------------------------------------

    public List<Table> getTables() {
        return Collections.unmodifiableList(tables);
    }

    public List<Customer> getWaitingQueue() {
        return Collections.unmodifiableList(waitingQueue);
    }

    public List<Customer> getCustomers() {
        return Collections.unmodifiableList(customers);
    }

    /**
     * @return customers currently at a table, in table order
     */
    public List<Customer> getSeatedCustomers() {
        List<Customer> seated = new ArrayList<>();
        for (Table t : tables) {
            if (!t.isFree()) {
                seated.add(t.getOccupant());
            }
        }
        return seated;
    }

    public int getRevenueCents() {
        return revenueCents;
    }

    public int getPaidCount() {
        return paidSatisfactions.size();
    }

    public int getServedCount() {
        return servedCount;
    }

    public int getUnhappyDepartures() {
        return unhappyDepartures;
    }

    /**
     * @return average satisfaction recorded at payment, empty if nobody has paid
     */
    public OptionalDouble getAveragePaidSatisfaction() {
        return paidSatisfactions.stream().mapToInt(Integer::intValue).average();
    }
}
