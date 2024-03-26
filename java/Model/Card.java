package Model;

import java.io.Serializable;

//immutable
//each one is uniquely identifiable by only its order value

/**
 * Immutable class representing a card.
 * Each card is uniquely identified by its {@link Card#order_value}, which as per the game rules is used to determine
 * the turn order players will proceed in after each one has played a card.
 * The other value characterizing a card is its {@link Card#movements_value}, indicating the maximum number of steps
 * mother nature can be moved by the player who played this card.
 *
 * Each player is given an instance of each of the 10 unique cards at the beginning of the game.
 */
public class Card implements Serializable {
    public final int order_value;
    public final int movements_value;

    /**
     * Constructor for an immutable instance of {@link Card}.
     *
     * @param order_value the order value unique to this card
     * @param movements_value the maximum movement value offered by this card
     */
    public Card(int order_value, int movements_value) {
        this.order_value = order_value;
        this.movements_value = movements_value;
    }

    /**
     * Compares two cards.
     *
     * @param obj
     * @return true if the two cards are equal in both {@link Card#order_value} and {@link Card#movements_value}
     */
    @Override
    public boolean equals(Object obj) {
        if(!(obj instanceof Card)) return false;
        return ((Card) obj).order_value == this.order_value && ((Card) obj).movements_value == this.movements_value;
    }

}
