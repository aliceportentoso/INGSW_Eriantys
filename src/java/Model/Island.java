package Model;

import org.jetbrains.annotations.TestOnly;

import java.io.Serializable;

/**
 * Island of the game, with the following properties:
 * <ul>
 * <li> The number of students per {@link Colors color} on the island.
 * <li> The owner of the island (could be null).
 * <li> The number of island that got merged into this single one.
 * <li> A potential interdiction placed on this island via a {@link Npc} effect.
 * </ul><br>
 * This class is better described as an <em>islands set</em>, since it is predisposed to act like both a single island and
 * any number of islands merged together.<br>
 * At the beginning of each game every island counts only as a single one, but as the game progresses and islands get merged,
 * a single instance of this class can represent more than a single original island, all merged in one.
 */
public class Island implements Serializable {
    private int[] students; //five element array, indicating the total of students present per-color
    private Integer owner_index; //from which rooks color is deducible
    private int num_of_merged_islands;
    private boolean interdiction;

    public final int index;

    /**
     * Construct for a new island initialized ready for the start of a game.
     *
     * @param starting_student the initial student placed on the island at the beginning of each game, null for no pre-placed student
     * @param index index of this island among the 12 in the game
     */
    public Island(Colors starting_student, int index) {
        this.students = new int[5];
        if(starting_student != null)
            this.students[starting_student.index]++;
        this.owner_index = null;
        this.num_of_merged_islands = 1;
        this.interdiction = false;
        this.index = index;
    }

    /**
     * Constructs an {@link Island} as specified with the parameters.
     * @param starting_student the initial students placed on the island (null if none)
     * @param owner_index index of this island among the others in game
     * @param number_of_merged_islands number of "contained" islands
     * @param interdiction flag indicting if this island is or it is not interdicted (see {@link Npc5})
     * @implNote This constructor is meant to be used for testing purposes only.
     */
    @TestOnly
    public Island(int[] starting_student, Integer owner_index, int number_of_merged_islands, boolean interdiction) {
        this.students = starting_student;
        this.owner_index = owner_index;
        this.num_of_merged_islands = number_of_merged_islands;
        this.interdiction = interdiction;
        this.index = 0;
    }

    /**
     * Getter for the number of students of the requested {@link Colors color} on this island.
     *
     * @param color {@link Colors color} of students for which is requested the number of the on the island
     * @return the number of students on the island of the required color
     */
    public int getStudents(Colors color) {
        return students[color.index];
    }

    /**
     * Getter for the number of students of the requested {@link Colors color} index on this island.
     *
     * @param index index of the {@link Colors color} of students for which is requested the number of the on the island
     * @return the number of students on the island of the required color index
     */
    public int getStudents(int index) {
        return students[index];
    }

    /**
     * Adds the given student to the island.
     *
     * @param color of the student to add to the island
     */
    protected void addStudent(Colors color) {
        students[color.index]++;
    }

    /**
     * Getter for the owner of this island, in the for of the index of the player who owns this island
     * relative to the players array in {@link Game}.
     * The returned value is null if the island doesn't have an owner yet.
     *
     * @return the index of the island's owner, null if there isn't onw
     */
    public Integer getOwnerIndex() {
        return owner_index;
    }

    /**
     * Sets the ownership of this island to the provided player.
     *
     * @param index index of the player which will become the new owner, relative to the players array in {@link Game}
     */
    protected void setOwnerIndex(int index) {
        owner_index = index;
    }

    /**
     * Getter for the number of merged islands this instance counts as.
     *
     * @return number of merged islands inside this on
     */
    public int getNumOfMergedIslands() {
        return num_of_merged_islands;
    }

    /**
     * Getter for whether an interdiction is currently placed on this island or not.
     *
     * @return true if there is an interdiction on this island, otherwise false
     */
    public boolean getInterdiction() {
        return interdiction;
    }

    /**
     * Sets the interdiction on this island to the specified value.
     *
     * @param interdiction new value for the interdiction flag
     */
    protected void setInterdiction(boolean interdiction) {
        this.interdiction = interdiction;
    }

    //return true if it merges the given island successfully with this one and assumes that the given island is then discarded, false if the merges is not possible
    /**
     * Merges the provided island into this one.
     * A merge is executed only if the owner of the islands is the same, and neither is null.
     * If a merge is not possible due to invalid conditions this method returns false, otherwise true is returned after the merge has completed.
     * A merge adds every student, interdiction and number of merged islands from the provided island to this one.
     * <br><br>
     * To complete the merge without inconsistencies, after this method completes it is required for the caller to then delete instance
     * of island provided as a parameter from the collection of islands it has.
     *
     * @param island the island to merge into the current one (and then delete afterwards)
     * @return true if the merge was successful, false if the merge could not be executed
     */
    protected boolean merge(Island island) {
        if(owner_index == null || island.owner_index == null || owner_index.intValue() != island.owner_index.intValue())
            return false;

        for(int i = 0; i < 5; i++)
            students[i] += island.students[i];
        num_of_merged_islands += island.num_of_merged_islands;
        interdiction = interdiction || island.interdiction;
        return true;
    }
}
