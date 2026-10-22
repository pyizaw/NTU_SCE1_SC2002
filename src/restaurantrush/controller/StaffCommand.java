package restaurantrush.controller;

/**
 * A manager's decision for one staff member, as collected by the UI.
 * It is only a request: the staff and domain objects decide if it is allowed.
 */
public final class StaffCommand {

    /** The kinds of task a manager can ask for. */
    public enum Action {
        /** Seat a queued customer. */
        SEAT,
        /** Take a seated customer's order. */
        ORDER,
        /** Serve a READY dish. */
        SERVE,
        /** Do one preparation unit on an order. */
        COOK,
        /** Do nothing this turn. */
        WAIT
    }

    /** Requested task. */
    private final Action action;

    /** Customer / order number the task targets (unused for WAIT). */
    private final int targetId;

    /**
     * @param action   requested task
     * @param targetId customer or order number
     */
    public StaffCommand(Action action, int targetId) {
        this.action = action;
        this.targetId = targetId;
    }

    /**
     * @return a WAIT command
     */
    public static StaffCommand waitCommand() {
        return new StaffCommand(Action.WAIT, 0);
    }

    public Action getAction() {
        return action;
    }

    public int getTargetId() {
        return targetId;
    }
}
