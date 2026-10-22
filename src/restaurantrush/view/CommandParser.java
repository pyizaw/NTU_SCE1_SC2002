package restaurantrush.view;

import restaurantrush.controller.StaffCommand;

/**
 * Turns a typed command such as "seat 2" or "c 3" into a {@link StaffCommand}.
 * Shared by every user interface. It only checks syntax; whether the command
 * is allowed is decided by the staff and domain objects.
 */
public final class CommandParser {

    private CommandParser() {
    }

    /**
     * @param line typed text
     * @return true if the text asks for the help screen
     */
    public static boolean isHelp(String line) {
        String word = line.trim().toLowerCase();
        return word.equals("help") || word.equals("h") || word.equals("?");
    }

    /**
     * Parses a command.
     *
     * @param line typed text
     * @return the command
     * @throws IllegalArgumentException with a message for the manager if the syntax is wrong
     */
    public static StaffCommand parse(String line) {
        String[] parts = line.trim().toLowerCase().split("\\s+");
        if (parts[0].isEmpty()) {
            throw new IllegalArgumentException("Please type a command (type 'help' for the list).");
        }
        StaffCommand.Action action;
        switch (parts[0]) {
            case "seat": case "s":
                action = StaffCommand.Action.SEAT;
                break;
            case "order": case "o": case "take":
                action = StaffCommand.Action.ORDER;
                break;
            case "serve": case "v":
                action = StaffCommand.Action.SERVE;
                break;
            case "cook": case "c":
                action = StaffCommand.Action.COOK;
                break;
            case "wait": case "w":
                return StaffCommand.waitCommand();
            default:
                throw new IllegalArgumentException("Unknown command '" + parts[0] + "'. Type 'help' for the list.");
        }
        if (parts.length != 2) {
            throw new IllegalArgumentException("'" + parts[0] + "' needs a customer number, e.g. '" + parts[0] + " 1'.");
        }
        try {
            return new StaffCommand(action, Integer.parseInt(parts[1]));
        } catch (NumberFormatException e) {
            throw new IllegalArgumentException("'" + parts[1] + "' is not a customer number.");
        }
    }
}
