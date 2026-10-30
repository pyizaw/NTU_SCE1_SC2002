package restaurantrush.view;

import java.util.List;

import restaurantrush.controller.GameConfig;
import restaurantrush.controller.GameResult;
import restaurantrush.controller.TurnStep;
import restaurantrush.model.Menu;
import restaurantrush.model.Restaurant;
import restaurantrush.staff.Staff;
import restaurantrush.staff.StaffCommand;
import restaurantrush.staff.TaskOption;

/**
 * Everything the game controller needs from a user interface. The controller
 * depends only on this abstraction, so it never touches the console directly
 * and the same rules could drive a different front end.
 */
public interface GameView {

    /**
     * Shows the title, menu, targets and how to play.
     *
     * @param config game settings
     * @param menu   the menu
     */
    void showWelcome(GameConfig config, Menu menu);

    /**
     * Shows the turn banner.
     *
     * @param turn     current turn
     * @param maxTurns last turn
     */
    void showTurnHeader(int turn, int maxTurns);

    /**
     * Announces which step of the turn is running.
     *
     * @param step the step
     */
    void showStep(TurnStep step);

    /**
     * Shows the start-of-turn state: revenue, average satisfaction, queue,
     * tables, customers' satisfaction, order status and staff tasks.
     *
     * @param restaurant the restaurant
     * @param turn       current turn
     * @param staff      all staff, in the order they act
     */
    void showStatus(Restaurant restaurant, int turn, List<Staff> staff);

    /**
     * Asks the manager what a staff member should do.
     *
     * @param staff   the staff member
     * @param options the tasks the staff member could do now
     * @return the manager's (syntactically valid) command
     */
    StaffCommand askCommand(Staff staff, List<TaskOption> options);

    /**
     * Asks whether to switch on Happy Hour at the start of this turn.
     *
     * @param turn current turn
     * @return true to activate it now
     */
    boolean askActivateHappyHour(int turn);

    /**
     * Shows the Happy Hour status and remaining duration.
     *
     * @param status status line, e.g. "Happy Hour ACTIVE - 1 turn remaining"
     */
    void showHappyHourStatus(String status);

    /**
     * Reports something that happened.
     *
     * @param message description
     */
    void showEvent(String message);

    /**
     * Explains why a choice was rejected; the manager will be asked again.
     *
     * @param staff  the staff member whose choice was rejected
     * @param reason why it failed
     */
    void showRejected(Staff staff, String reason);

    /**
     * Shows the end-of-turn results.
     *
     * @param restaurant the restaurant
     * @param turn       turn just finished
     * @param maxTurns   last turn
     */
    void showTurnEnd(Restaurant restaurant, int turn, int maxTurns);

    /**
     * Shows victory or defeat.
     *
     * @param result final result
     */
    void showResult(GameResult result);
}
