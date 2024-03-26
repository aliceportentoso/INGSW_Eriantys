package Model;

import Exceptions.InvalidMoveException;

import java.util.List;

/**
 * Abstract class extended by each of the 12 specific Npcs.
 * Given the great differences in behaviour between the Npcs, a lot of methods are required despite
 * many being used by a fraction of the total Npcs.
 * The only methods having a purpose for each Npc are:
 * <li> {@link Npc#activateEffect(Game, EffectParameters)}
 * <li> {@link Npc#getId()}
 * <li> {@link Npc#getCost()}
 * <li> {@link Npc#getArgsNum()}
 */
public abstract class Npc {
    //unique id of the Npc
    public static int uid;

    //increases cost by 1 and resolves effects that can be resolved instantly upon activation, returns true only if the effect was able to activate, false otherwise
    /**
     * Activates this npc's effect, with the provided {@link EffectParameters effect parameters}.<br>
     * Each Npc requires a different amount of parameters with different purposes, see the {@link EffectParameters}'s documentation
     * or each specific npc's documentations to see which parameters are needed for each npc.
     * <br><br>
     * For npcs whose effect resolves instantly this method completes the activation entirely, while for npcs which alter other behaviours
     * in the subsequent game steps their activation just sets {@link Game#npc_effect} to their id.
     *
     * @param game instance of {@link Game} containing the npc
     * @param effect_parameters parameters for the effect's activation
     * @throws InvalidMoveException thrown whenever the parameters do not match the required specifics or when the player
     * requesting the activation doesn't have enough coins.
     */
    protected abstract void activateEffect(Game game, EffectParameters effect_parameters) throws InvalidMoveException;

    /**
     * Getter for the id of the specific npc, from 1 to 12 (extremes included).
     *
     * @return id of the specific npc
     */
    abstract public int getId();

    /**
     * Getter for the <strong>current</strong> cost of activation of the specific npc.
     * The activation cost is incremented by one after each activation, as per rules.
     *
     * @return the <strong>current</strong> npc activation cost
     */
    public abstract int getCost();

    /**
     * Getter for the minimum number of arguments required for this npc's activation.
     *
     * @return minimum number of arguments required for this npc's activation
     */
    public abstract int getArgsNum();

    /**
     * Executes a varying extra action correlated to the npc's effect.
     */
    protected void extraEffect()  {
        return;
    }

    /**
     * Getter for all the potential properties that a specific npc might have,
     * going from none to a maximum of 6.
     *
     * @return the npc's extra properties, if any
     */
    public List<Integer> getExtraProperty() {
        return null;
    }

    /**
     * Alters the npc's extra properties, those varying from one npc to another.
     *
     * @param prop new set of properties for the npc
     */
    public void setExtraProperty(List<Colors> prop) {
        return;
    }
}
