package restaurantrush.staff;

/**
 * A manager's decision for one staff member, as collected by the UI.
 * It is only a request: the staff and domain objects decide if it is allowed.
 */
public final class StaffCommand {

    /** The kinds of task a manager can ask for. */
    public enum Action {
        /** Seat a queued customer. */
        SEAT("seat"),
        /** Take a seated customer's order. */
        ORDER("order"),
        /** Serve a READY dish. */
        SERVE("serve"),
        /** Do one preparation unit on an order. */
        COOK("cook"),
        /** Do nothing this turn. */
        WAIT("wait");

        /** The word the manager types for this action. */
        private final String keyword;

        Action(String keyword) {
            this.keyword = keyword;
        }

        public String getKeyword() {
            return keyword;
        }
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

    /**
     * @return the text a manager would type for this command, e.g. "seat 2" or "wait"
     */
    public String toCommandText() {
        return action == Action.WAIT ? action.getKeyword() : action.getKeyword() + " " + targetId;
    }

    @Override
    public String toString() {
        return toCommandText();
    }
}
