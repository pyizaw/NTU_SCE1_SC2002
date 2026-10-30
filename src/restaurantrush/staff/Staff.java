package restaurantrush.staff;

import java.util.List;
import java.util.function.Supplier;

import restaurantrush.model.ActionResult;
import restaurantrush.model.Restaurant;

/**
 * A member of staff. Every staff member can do at most one task per turn;
 * this class enforces that limit so no caller can bypass it.
 * <p>
 * The controller hands every manager command to {@link #carryOut}; each
 * subclass decides in {@link #handle} which commands its role accepts. Adding
 * a new role therefore means adding a subclass, not changing the controller.
 * </p>
 */
public abstract class Staff {

    /** Whether this staff member has already used this turn's task. */
    private boolean taskUsed;

    /**
     * @return role name, e.g. "Waiter"
     */
    public abstract String getRole();

    /**
     * Lists the tasks this staff member could usefully do right now, so the
     * manager can choose one.
     *
     * @param restaurant the restaurant
     * @param turn       current turn
     * @return the options (may be empty)
     */
    public abstract List<TaskOption> availableTasks(Restaurant restaurant, int turn);

    /**
     * One line for the start-of-turn display, e.g. "seat 2 | order 1 | wait".
     * Staff who work automatically override this to describe what they will do.
     *
     * @param restaurant the restaurant
     * @param turn       current turn
     * @return the summary
     */
    public String summariseTasks(Restaurant restaurant, int turn) {
        StringBuilder sb = new StringBuilder();
        for (TaskOption option : availableTasks(restaurant, turn)) {
            sb.append(sb.length() == 0 ? "" : " | ").append(option.getCommand().toCommandText());
        }
        return sb.toString();
    }

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
     * Carries out the manager's command. "wait" is the same for every role;
     * anything else is handed to the role's own {@link #handle}.
     *
     * @param command    the manager's command
     * @param restaurant the restaurant
     * @param turn       current turn
     * @return the result; a failed result has not used up the task
     */
    public final ActionResult carryOut(StaffCommand command, Restaurant restaurant, int turn) {
        if (command.getAction() == StaffCommand.Action.WAIT) {
            return waitTurn();
        }
        return handle(command, restaurant, turn);
    }

    /**
     * Role-specific commands. By default a role accepts none; subclasses
     * override this for the tasks their role can do.
     *
     * @param command    the manager's command (never WAIT)
     * @param restaurant the restaurant
     * @param turn       current turn
     * @return the result
     */
    protected ActionResult handle(StaffCommand command, Restaurant restaurant, int turn) {
        return ActionResult.fail("The " + getRole() + " cannot '" + command.getAction().getKeyword()
                + "'. " + describeCommands());
    }

    /**
     * @return a one-line reminder of the commands this role accepts
     */
    public abstract String describeCommands();

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
