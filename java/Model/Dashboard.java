package Model;

import org.jetbrains.annotations.TestOnly;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;

/**
 * Dashboard of a player.
 * Contains all the assets specific to a player's dashboard during a game, those being:
 * <ul>
 * <li> The {@link Colors students} located in the player's entrance,
 * <li> The number of students in each row of the player's hall.
 * <li> The {@link Colors professors} controlled by the player.
 * <li> The number of rooks left available to the player.
 * </ul>
 * NB: the color of the rooks is determined in accordance to the index of the player inside the players list in {@link Game},
 *     the first player is White, followed by Black and Gray.
 */
public class Dashboard implements Serializable {
    private List<Colors> entrance;
    private int[] hall; //five element array, indicating the total of students present per-color
    private boolean[] professors;
    private int rooks; //count of available rooks

    /**
     * Constructor that allows the dashboard's initialization at the beginnning of a game.
     *
     * @param entrance initial entrance students
     * @param players_number number of players in the game, used to determine other parameters
     */
    public Dashboard(List<Colors> entrance, int players_number) {
        this.entrance = entrance;
        this.hall = new int[5];
        this.professors = new boolean[5];
        for(int i = 0; i < 5; i++)
            this.professors[i] = false;
        if(players_number == 2)
            this.rooks = 8;
        else
            this.rooks = 6;
    }

    /**
     * Constructs a {@link Dashboard} as specified with the parameters.
     * @param entrance list of {@link Colors} representing the students in this {@link Dashboard}'s entrance.
     * @param rooks number of available rooks
     * @param hall five element array indicating the total of students present per-color
     * @param professors five element array indicating the already claimed professors
     * @implNote This constructor is meant to be used for testing purposes only.
     */
    @TestOnly
    public Dashboard(List<Colors> entrance, int rooks, int[] hall, boolean[] professors) {
        this.entrance = entrance;
        this.hall = hall;
        this.professors = professors;
        this.rooks = rooks;
    }

    /**
     * Getter for the students in the entrance, represents by a list of {@link Colors}, each element in the list
     * being a different student in the entrance.
     *
     * @return the list of students in the entrance
     */
    public List<Colors> getEntrance() {
        return new ArrayList<Colors>(entrance);
    }

    /**
     * Removes the specified {@link Colors student} from the entrance and returns it, allowing it to be placed on an island or in the player's hall.
     *
     * @param index index of the student to remove, based upon the list from {@link Dashboard#getEntrance}
     * @return the removed {@link Colors student}
     */
    protected Colors removeFromEntrance(int index) {
        return entrance.remove(index);
    }

    /**
     * Adds the provided {@link Colors student} to the entrance.
     *
     * @param to_add {@link Colors student} to add to the entrance
     */
    protected void addToEntrance(Colors to_add) {
        entrance.add(to_add);
    }

    /**
     * Adds all the provided {@link Colors students} to the entrance.
     *
     * @param to_add list of students to add in bulk to the entrance
     */
    protected void addToEntrance(List<Colors> to_add) {
        entrance.addAll(to_add);
    }

    /**
     * Getter for the number of students in the requested hall row.
     * The hall row is here specified by the color of students it contains.
     *
     * @param color color of the row to read
     * @return number of students in the requested row
     */
    public int getHallRow(Colors color) {
        return hall[color.index];
    }

    /**
     * Getter for the number of students in the requested hall row.
     * The hall row is here specified by its index. Indexes are handled as if they were the matching {@link Colors},
     * since the hall rows are ordered with the same indexes as the colors.
     *
     * @param index of the row to read
     * @return number of students in the requested row
     */
    public int getHallRow(int index) {
        return hall[index];
    }

    /**
     * Increments the count of the specified hall row by one, that is equivalent to adding a student to said row.
     *
     * @param color the color of the row to which to add a student
     */
    protected void addStudentToHall(Colors color) {
        hall[color.index]++;
    }

    //If the number of students of that color is already 0, it does not go below that and returns false, otherwise true
    /**
     * Decrements the count of the specified hall row by one, that is equivalent to removing a student from said row.
     * If the count of students in the row is already 0, this method doesn't decrement it any further and return false,
     * otherwise returning true after a successful decrement.
     *
     * @param color the color of the row from which to remove a student
     * @return true if the student was actually removed, false if the row was empty and no student was removed
     */
    protected boolean removeStudentFromHall(Colors color) {
        if(hall[color.index] > 0) {
            hall[color.index]--;
            return true;
        }
        return false;
    }

    /**
     * Getter for whether a {@link Colors professor} is owned by the player who owns this dashboard or not.
     * The professor is here identified by its {@link Colors color}.
     *
     * @param color {@link Colors professor} whose ownership is being tested
     * @return true if the tested professor is on this dashboard
     */
    public boolean getProfessor(Colors color) {
        return professors[color.index];
    }

    /**
     * Getter for whether a {@link Colors professor} is owned by the player who owns this dashboard or not.
     * The professor is here identified by its color index.
     *
     * @param index index of the color of the professor whose ownership is being tested
     * @return true if the tested professor is on this dashboard
     */
    public boolean getProfessor(int index) {
        return professors[index];
    }

    /**
     * Changes the ownership status of the indicated professor to the new provided one.
     *
     * @param color {@link Colors professor} to which to change the ownership status
     * @param status new ownership status, true for owned, false for now owned
     */
    protected void setProfessor(Colors color, boolean status) {
        professors[color.index] = status;
    }

    /**
     * Getter for the number of rooks left available on this dashboard.
     *
     * @return number of remaining rooks
     */
    public int getRooks() {
        return rooks;
    }

    /**
     * Increases the number of rooks available on the dashboard by the given amount.
     *
     * @param quantity the amount of rooks to increase the available quantity by
     */
    protected void increaseRooks(int quantity) {
        rooks += quantity;
    }

    /**
     * Decreases the number of rooks available on the dashboard by the given amount.
     * If the quantity of available rooks were to go negative the operation is not executed.
     *
     * @param quantity the amount of rooks to decrease the available quantity by
     */
    protected void decreaseRooks(int quantity) {
        if(rooks > 0)
            rooks -= quantity;
    }

}
