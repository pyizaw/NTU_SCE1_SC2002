package restaurantrush.staff;

/**
 * One task a staff member could usefully do right now, offered to the manager
 * as a choice: the command itself plus a plain-language description.
 * Views list these options; they never have to build or parse command text.
 */
public final class TaskOption {

    /** The command this option stands for. */
    private final StaffCommand command;

    /** What the option does, e.g. "Seat #2 Regular at a free table". */
    private final String description;

    /**
     * @param command     the command this option stands for
     * @param description what the option does
     */
    public TaskOption(StaffCommand command, String description) {
        this.command = command;
        this.description = description;
    }

    /**
     * @return the "do nothing this turn" option every manager-directed staff member has
     */
    public static TaskOption waitOption() {
        return new TaskOption(StaffCommand.waitCommand(), "Do nothing this turn");
    }

    public StaffCommand getCommand() {
        return command;
    }

    public String getDescription() {
        return description;
    }

    /**
     * @return true if this option is "wait"
     */
    public boolean isWait() {
        return command.getAction() == StaffCommand.Action.WAIT;
    }
}
