package Model;

import com.google.inject.internal.util.ImmutableList;

import java.io.Serializable;
import java.util.List;

//immutable
//has to implement serializable and this class will be sent with the activate_effect move, so that it specifies the parameters for every effect!
/**
 * Immutable class used to contain the parameters used in the activation of every Npc.<br><br>
 * <p>
 * <strong>What follows are the specific functions and required arguments for every {@link Npc} (referenced by its id):</strong><br>
 * <br>
 * <ol>
 * <li>Take 1 student from this card and place it on an island, then add 1 student from the pouch to this card
 *     <code>args.get(0)</code> is the target student index, <code>args.get(1)</code> is the target island.
 * <li>During this turn take control of professors even if you are tied with their current owner
 *     simply set npc_effect in game to this card's UID.
 * <li>Calculate the dispute on a given island as if mother nature landed there (here it is assumed that this triggers any interdiction)
 *     <code>args.get(0)</code> is the island index, the passed to game.disputeIsland(<code>args.get(0)</code>) via the game instance in activateEffect.
 * <li>You can move mother nature by 2 additional islands
 *     simply set npc_effect in game to this card's UID.
 * <li>Place an interdiction card on an island, the first time mother nature lands there you do not dispute the island
 *     <code>args.get(0)</code> is the index of the island where to put the interdiction.
 * <li>During this turn's dispute the number of towers is not considered
 *     simply set npc_effect in game to this card's UID.
 * <li>Take up to 3 students from this card and swap them for the same number from your dashboard's entrance
 *     <code>args.get(0 ... 6)</code>, index 0 is the number of students to swap, then from index 1 to number_of_students are the indexes of the students to take from the card,
 *     then from number_of_students + 1 to number_of_students*2 it contains the students in the entrance to replace.
 * <li>During this turn's dispute you have 2 extra points in your favour.
 * <li>Choose a color (<code>args.get(0)</code> from 0 to 4), during this turn's dispute that color doesn't count
 *     towards the total score of any player.
 * <li>You can swap between them 2 students, one from your hall and one from your entrance on your dashboard
 *     <code>args.get(0)</code> is the entrance's student index and <code>args.get(1)</code> is the hall's student color.
 * <li>Take 1 student from this card (<code>args.get(0)</code>) and place it in your hall, then place one student from the punch
 *     back on this card.
 * <li>Choose a color (<code>args.get(0)</code> from 0 to 4), each player, you included, has to put back in the pouch 3
 *     students of that color from his hall on his dashboard, if there are less, he only puts back as many as possible.
 * </ol>
 * </p>
 */
public class EffectParameters implements Serializable {
    public final ImmutableList<Integer> args;

    /**
     * Constructor that takes an arbitrary number of arguments and converts them into a list
     *
     * @param args integers that will become the arguments for an effect activation
     */
    public EffectParameters(Integer ... args) {
        this.args = ImmutableList.of(args);
    }

    /**
     * Constructor that takes a list as parameter
     *
     * @param args list of arguments for an effect activation
     */
    public EffectParameters(List<Integer> args) {
        this.args = ImmutableList.copyOf(args);
    }
}

/*
NOTES FOR PECULIAR EFFECTS:
1 -> Take 1 student from this card and place it on an island, then add 1 student from the pouch to this card
    args.get(0) is the target student index, args.get(1) is the target island
2 -> During this turn take control of professors even if you are tied with their current owner
    simply set npc_effect in game to this card's UID
3 -> Calculate the dispute on a given island as if mother nature landed there (here it is assumed that this triggers any interdiction)
    args.get(0) is the island index, the passed to game.disputeIsland(args.get(0)) via the game instance in activateEffect
4 -> You can move mother nature by 2 additional islands
    simply set npc_effect in game to this card's UID
5 -> Place an interdiction card on an island, the first time mother nature lands there you do not dispute the island
    args.get(0) is the index of the island where to put the interdiction
6 -> During this turn's dispute the number of towers is not considered
    simply set npc_effect in game to this card's UID
7 -> Take up to 3 students from this card and swap them for the same number from your dashboard's entrance
    args.get(0 ... 6), index 0 is the number of students to swap, then from index 1 to number_of_students are the indexes of the students to take from the card,
    then from number_of_students + 1 to number_of_students*2 it contains the students in the entrance to replace
8 -> During this turn's dispute you have 2 extra points in your favour
9 -> Choose a color (args.get(0) from 0 to 4), during this turn's dispute that color doesn't count
    towards the total score of any player
10 -> You can swap between them 2 students, one from your hall and one from your entrance on your dashboard
    args.get(0) is the entrance's student index and args.get(1) is the hall's student color
11 -> Take 1 student from this card (args.get(0)) and place it in your hall, then place one student from the punch
    back on this card
12 -> Choose a color (args.get(0) from 0 to 4), each player, you included, has to put back in the pouch 3
    students of that color from his hall on his dashboard, if there are less, he only puts back as many as possible
 */