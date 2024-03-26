package Model;

import Exceptions.InvalidMoveException;
import org.jetbrains.annotations.TestOnly;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;

/**
 * Representation of a player in the game.
 * Holds all the properties and items specific to a player during a game, those being:
 * <li> His unique clientId given by the {@link Server}.
 * <li> His player index relative to the players array inside {@link Game}.
 * <li> His {@link Dashboard} instance.
 * <li> The remaining {@link Card Cards} in the player's hands.
 * <li> The last {@link Card} the player played.
 * <li> The number of coins owned by the player.
 */
public class Player implements Serializable {
    public final int clientID;
    public final int player_index;
    private Dashboard dashboard;
    //it is assumed that, due to decks being known, it is not an issue for players to know each other's hands!
    //otherwise, this field would need to be transient and subsequently stored on the server by other means than serialization
    private List<Card> cards;
    private Card last_card_played;
    private int coins;

    /**
     * Constructor for a player as it should be at the beginning of a game.
     *
     * @param clientID id given to the player by the {@link Server}
     * @param initial_dashboard_entrance list of students constituting the initial {@link Dashboard} entrance of the player
     * @param players_number number of players in the game, used to determine parameter during construction
     * @param player_index index of this player inside the players list in {@link Game}
     * @param coins number of coins the player starts with
     */
    public Player(int clientID, List<Colors> initial_dashboard_entrance, int players_number, int player_index, int coins) {
        this.clientID = clientID;
        this.player_index = player_index;
        this.dashboard = new Dashboard(initial_dashboard_entrance, players_number);
        this.cards = new ArrayList<> (List.of(
                new Card( 1, 1),
                new Card( 2, 1),
                new Card( 3, 2),
                new Card( 4, 2),
                new Card( 5, 3),
                new Card( 6, 3),
                new Card( 7, 4),
                new Card( 8, 4),
                new Card( 9, 5),
                new Card( 10, 5)
        ));
        this.last_card_played = null;
        this.coins = coins;
    }

    /**
     * Constructs a {@link Player} as specified with the parameters.
     * @param clientID player's clientID
     * @param dashboard reference to the player's {@link Dashboard}
     * @param players_number number of in-game players
     * @param player_index this player's index
     * @param coins number of owned coins
     * @param cards list of owned {@link Card}s
     * @implNote This constructor is meant to be used for testing purposes only.
     */
    @TestOnly
    public Player(int clientID, Dashboard dashboard, int players_number, int player_index, int coins, List<Card> cards) {
        this.clientID = clientID;
        this.player_index = player_index;
        this.dashboard = dashboard;
        this.cards = cards;
        this.last_card_played = null;
        this.coins = coins;
    }

    /**
     * Getter for the player's {@link Dashboard}.
     *
     * @return the player's {@link Dashboard}
     */
    public Dashboard getDashboard() {
        return dashboard;
    }

    /**
     * Getter for the {@link Card Cards} left in the hand of the player
     *
     * @return list of {@link Card Cards} still held by the player
     */
    public List<Card> getCards() {
        return new ArrayList<Card>(cards);
    }

    /**
     * Plays the card indicated by the index from the player's hand, placing it as the last played card.
     *
     * @param card_index index of the card to play relative to the cards list from {@link Player#getCards}
     */
    protected void playCard(int card_index) {
        last_card_played = cards.remove(card_index);
    }

    /**
     * Getter for the player's last played card.
     *
     * @return the player's last played card
     */
    public Card getLastCardPlayed() {
        return last_card_played;
    }

    /**
     * Getter for the coins owned by the player.
     *
     * @return coins the player currently has
     */
    public int getCoins() {
        return coins;
    }

    /**
     * Increases by one the count of coins owned by the player.
     */
    protected void addCoin() {
        coins++;
    }

    /**
     * Makes the player pay a specified amount of coins.<br>
     * Used during {@link Npc#activateEffect(Game, EffectParameters)}.
     *
     * @param cost number of coins to pay
     * @throws InvalidMoveException thrown if the number of coins requested cannot be paid due to the player owning fewer coins
     */
    protected void pay(int cost) throws InvalidMoveException {
        if(cost > coins)
            throw new InvalidMoveException("You don't have enough coins.");
        coins -= cost;
    }
}
