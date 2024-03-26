package Model;

import java.io.Serializable;

/**
 * Enum for the colors of students and professors in the game.
 * There are 5 colors in the game:
 * <ol start="0">
 *      <li> {@link Colors#YELLOW},
 *      <li> {@link Colors#BLUE},
 *      <li> {@link Colors#GREEN},
 *      <li> {@link Colors#RED},
 *      <li> {@link Colors#MAGENTA}.
 * </ol>
 *
 * Each color is associated with a unique index as in the list above, which allows for colors to be addressed by both their enum constants
 * and their indexes with no distinction.
 * <br><br>
 * An instance of {@Colors} can be used in the model to represent either a professor or a student, with the only distinction between the two
 * being the location of that instance.
 */
public enum Colors implements Serializable {
    YELLOW(0), BLUE(1), GREEN(2), RED(3), MAGENTA(4);

    public final int index; //used when the enum value is used to index something

    /**
     * Constructor that associates at every color its index.
     *
     * @param index index uniquely associated with the color being constructed
     */
    Colors(int index) {
        this.index = index;
    }

    /**
     * Converts a given index in its corresponding color.
     *
     * @param index index to convert in a color
     * @return color associated to the provided index
     */
    public static Colors fromColorIndex(int index) {
        return switch (index) {
            case 0 -> YELLOW;
            case 1 -> BLUE;
            case 2 -> GREEN;
            case 3 -> RED;
            /*case 4, */ default -> MAGENTA;
        };
    }
}