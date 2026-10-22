package restaurantrush.model;

/**
 * The outcome of a staff action. A failed result carries the reason so the
 * manager can be told why the choice was rejected and choose again.
 */
public final class ActionResult {

    /** Whether the action was carried out. */
    private final boolean success;

    /** Explanation shown to the manager. */
    private final String message;

    private ActionResult(boolean success, String message) {
        this.success = success;
        this.message = message;
    }

    /**
     * Creates a successful result.
     *
     * @param message what happened
     * @return a successful result
     */
    public static ActionResult ok(String message) {
        return new ActionResult(true, message);
    }

    /**
     * Creates a failed result. Failed actions never change the game state.
     *
     * @param reason why the action could not be carried out
     * @return a failed result
     */
    public static ActionResult fail(String reason) {
        return new ActionResult(false, reason);
    }

    public boolean isSuccess() {
        return success;
    }

    public String getMessage() {
        return message;
    }
}
