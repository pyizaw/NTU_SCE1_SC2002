package restaurantrush.staff;

import java.util.List;
import java.util.function.Supplier;

import restaurantrush.model.ActionResult;
import restaurantrush.model.Restaurant;

/**
 * A member of staff. Every staff member can do at most one task per turn;
 * this class enforces that limit so no caller can bypass it.
 */
public abstract class Staff {

    /** Whether this staff member has already used this turn's task. */
    private boolean taskUsed;

    /**
     * @return role name, e.g. "Waiter"
     */
    public abstract String getRole();

    /**
     * Lists the tasks this staff member could usefully do right now, as
     * command hints for the manager.
     *
     * @param restaurant the restaurant
     * @param turn       current turn
     * @return task descriptions (may be empty)
     */
    public abstract List<String> availableTasks(Restaurant restaurant, int turn);

    /** Gives the staff member a fresh task for the new turn. */
    public void startTurn() {
        taskUsed = false;
    }

    /**
     * @return true if the staff member already did a task this turn
     */
    public boolean hasActed() {
        return taskUsed;
    }

    /**
     * Uses this turn's task to do nothing.
     *
     * @return the result
     */
    public ActionResult waitTurn() {
        return perform(() -> ActionResult.ok(getRole() + " waits this turn."));
    }

    /**
     * Runs a task if the staff member is still free this turn. The task only
     * counts as used when it succeeds, so an invalid choice costs nothing.
     *
     * @param task the task to attempt
     * @return the task's result
     */
    protected ActionResult perform(Supplier<ActionResult> task) {
        if (taskUsed) {
            return ActionResult.fail(getRole() + " has already completed a task this turn.");
        }
        ActionResult result = task.get();
        if (result.isSuccess()) {
            taskUsed = true;
        }
        return result;
    }
}
