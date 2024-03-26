package Model;

import Exceptions.InvalidMoveException;
import org.jetbrains.annotations.TestOnly;

import java.io.*;
import java.util.*;
import java.util.stream.IntStream;

//NB: outside of Game, only clientID should be used to refer to specific players

//Since every deck has the exact same cards, each player is already constructed with his deck in hand

/*
HOW TO PLAY:
DEPRECATED - now game starts with game_turn at 1 - in game_turn 0 each player (order is irrelevant) must choose a deck -> pickDeck()
- each subsequent game_turn, is handled with phase 0, planning, and phase 1, action
- in phase 0 each player, in the order given in current_phase_order, has to play one card from their hand ->playCard()
- in phase 1 each player proceeds to execute each of the 3 steps:
    - set students 3 (or 4) times from his entrance to islands or his hall -> setStudentToHall() and setStudentToIsland()
    - move mother nature by a number of steps between 1 and the maximum allowed by his last played assistant card -> moveMotherNature()
    - choose a cloud from which to take his new students -> chooseCloud()
- at the end of step 3 everything starts back from phase 0 and game_turn goes up by 1
 */

/**
 * Main model class, contains the global state of the game and all the references to the specific classes complementing it.
 * All the game logic is handled here, with a dedicated exception: {@link InvalidMoveException} being thrown whenever an illegal move is requested.
 * <br><br>
 * <p>
 * <strong>How a game evolves:</strong>
 * <ul>
 * <li>
 * A newly created game has every player's hand of cards and dashboard already initialized, the 12 islands with the respective students
 * *      and mother nature, the Npcs and clouds are also already in place when the constructor returns.
 * </li>
 * <li>
 * Every game turn begins with phase 0, when each player, in order, has to play a card from his hand with the {@link Game#playCard}.<br>
 * The current game turn is accessible via {@link Game#getGameTurn}, and the current phase via {@link Game#getPhase}.
 * </li>
 * <li>
 * Following everyone playing a card, phase 1 begins, with its 3 steps:<br>
 * The current step is retrievable with {@link Game#getStep()}.
 *     <ul>
 *          <li>Step 0, the currently playing player has to set 3 students from his hall to either an island, via the {@link Game#setStudentToIsland} method,
 *             or his hall, with the {@link Game#setStudentToHall} method.<br>
 *             The number of students that have been set up until now is obtainable via {@link Game#getMovedStudents}.
 *         <li>Step 1, the currently playing player has to move mother nature, by calling the {@link Game#mother_nature} method.
 *         <li>Step 2, the currently playing player has to choose one of the clouds, using the {@link Game#chooseCloud} method.
 *             Everything is then repeated from step 0 for each player, only after everyone has played through all 3 steps phase 1.
 *     </ul></li>
 *     <li>
 *         After phase 1 the next game turn begins with its phase 0.
 *     </li>
 *     <li>
 *         Whenever a winning condition is verified, the winnerId is set, and is obtainable with {@link Game#getWinnerID},
 *         and every further move is met with an {@link InvalidMoveException}.
 *     </li>
 * </p>
 * </ul>
 * <p>
 *     <strong>Notes:</strong><br><br>
 *     From the outside each player is to be uniquely addressed by his clientId, meanwhile inside this package each player is addressed with its index inside in
 *     the list of players local to this class, this is done to make it possible to use more efficient algorithms that take advantage of a progressive index, instead of
 *     needing to convert a random clientId to an index beforehand for example. Conversion from internal addressing with indexes to external with clientIds is however
 *     handled in the getter methods, to prevent confusion outside this package.<br><br>
 *     The possibility for each player to choose a deck of cards has been omitted since every deck is entirely equivalent to the other, hence the logic for such choice was
 *     not of use to the game's functionality, and only added wasted memory usage.
 * </p>
 */
public class Game implements Serializable {
    //maybe make this transient
    private final List<Player> players;
    protected List<Colors> random_students; //pre-computed order in which students are extracted form the pouch
    public final boolean expert_mode;

    private int game_turn; //current turn in the whole game, turn 0 is just card-deck selection and general setup
    private int phase; //0 is planning phase (each player plays a card from his hand) and 1 is action phase
    private int player_turn; //index of the players list indicating the currently playing player, 0, 1 or 2
    private int step; //step, only used in the action phase, goes from 0 to 2
    private int moved_students; //used only in action phase, step 0, it counts how many students have already been moved by the acting player

    private final List<Integer> current_phase_order; //this is ordered from the player who played the LOWEST value card to the one who played the HIGHEST

    private List[] clouds;
    private int mother_nature;
    protected List<Island> islands; //The graphics for the island is chosen based on the index of the island in this array + its number of merged islands
    private final List<Colors> unclaimed_professors; //professors which are still not claimed by any player
    private Npc[] npcs;
    protected int npc_effect; //is usually 0, except when a Npc effect is activated and is has not resolved upon activation, but instead is due to resolve at some point during this turn, in which case it is the UID of that effect's npc
    protected int bank;

    private boolean last_game_turn;
    private boolean game_ended;
    private int winnerID;

    private transient byte[] serialized_game_copy; //contains a copy of this class's instance adjourned at the end of each turn, used to revert changes

    //those constructors are called only by ServerLobby after the Game settings are decided and every player is present
    //alternatively the class can be constructed by deserialization in the client

    //Create new fresh Game

    /**
     * Creates fresh and ready-to-start instance of Game.<br>
     * The number of provided players alters the game's setting according to the rules for 2 or 3 players.
     *
     * @param expert_mode flag to enable expert mode in the newly created game
     * @param clientIDs   list of the clientsId of the players which will take part in the game
     */
    public Game(boolean expert_mode, List<Integer> clientIDs) {
        this.random_students = new ArrayList<Colors>();
        for (int i = 0; i < 5; i++) {
            //24 is indeed correct, see temp variable down the line
            for (int j = 0; j < 24; j++) {
                this.random_students.add(Colors.fromColorIndex(i));
            }
        }
        Collections.shuffle(random_students);

        this.players = new ArrayList<Player>();
        for (int i = 0; i < clientIDs.size(); i++)
            this.players.add(new Player(clientIDs.get(i), extractStudents(clientIDs.size() == 2 ? 7 : 9), clientIDs.size(), i, expert_mode ? 1 : 0));

        this.expert_mode = expert_mode;
        this.game_turn = 1;
        this.phase = 0;
        this.player_turn = 0;
        this.step = 0;
        this.moved_students = 0;
        this.current_phase_order = clientIDs.size() == 2 ? new ArrayList<>(List.of(0, 1)) : new ArrayList<>(List.of(0, 1, 2));
        this.clouds = new List[clientIDs.size()];
        for (int i = 0; i < clientIDs.size(); i++) {
            //sets up 3 students if there are only 2 players, otherwise sets up 4
            this.clouds[i] = new ArrayList<Colors>(extractStudents(players.size() + 1));
        }

        //must be done to allow only 2 students for each color to be set on islands
        List<Colors> temp = new ArrayList<Colors>();
        for (int i = 0; i < 5; i++) {
            for (int j = 0; j < 2; j++) {
                temp.add(Colors.fromColorIndex(i));
            }
        }
        Collections.shuffle(temp);
        this.mother_nature = (new Random()).nextInt(12);
        this.islands = new ArrayList<Island>();
        for (int i = 0; i < 12; i++) {
            if (i == (this.mother_nature + 6) % 12 || i == this.mother_nature)
                this.islands.add(new Island(null, i));
            else
                this.islands.add(new Island(temp.remove(0), i));
        }

        this.unclaimed_professors = new ArrayList<Colors>();
        this.unclaimed_professors.addAll(Arrays.asList(Colors.values()));

        if (expert_mode) {
            this.npcs = NpcFactory.factoryMethod(this);
        }
        this.npc_effect = 0;

        this.bank = expert_mode ? (this.players.size() == 2 ? 18 : 17) : 0;

        this.last_game_turn = false;
        this.game_ended = false;
        this.winnerID = 0;

        this.serialized_game_copy = null;

        updateSerializedGameCopy();
    }

    /**
     * Creates a {@link Game} built as specified with the parameters.
     * @param expert_mode flag to enable expert mode in the newly created game
     * @param players list containing the {@link Player}s for this game
     * @param clientIDs list of the clientsId of the players which will take part in the game
     * @param students students list to be used for this fame
     * @param mother_nature initial Mother Nature position
     * @param islands list containing the {@link Island}s to be used for this game
     * @param npcs list containing the {@link Npc}s to be used for this game (eventually null if this game is not in expert mode)
     * @param unclaimed_professors list of {@link Colors} representing the still unclaimed professors.
     * @implNote This constructor is meant to be used for testing purposes only.
     */
    @TestOnly
    public Game(boolean expert_mode, List<Player> players, List<Integer> clientIDs, List<Colors> students, int mother_nature, List<Island> islands, Npc[] npcs, List<Colors> unclaimed_professors) {
        this.random_students = students;
        this.players = players;
        this.expert_mode = expert_mode;
        this.game_turn = 1;
        this.phase = 0;
        this.player_turn = 0;
        this.step = 0;
        this.moved_students = 0;
        this.current_phase_order = clientIDs.size() == 2 ? new ArrayList<>(List.of(0, 1)) : new ArrayList<>(List.of(0, 1, 2));
        this.clouds = new List[clientIDs.size()];
        for (int i = 0; i < clientIDs.size(); i++) {
            //sets up 3 students if there are only 2 players, otherwise sets up 4
            this.clouds[i] = new ArrayList<Colors>(extractStudents(players.size() + 1));
        }

        this.mother_nature = mother_nature;
        this.islands = new ArrayList<Island>(islands);

        this.unclaimed_professors = unclaimed_professors;

        if(expert_mode)
            this.npcs = npcs.clone();
        this.npc_effect = 0;

        this.bank = expert_mode ? (this.players.size() == 2 ? 18 : 17) : 0;

        this.last_game_turn = false;
        this.game_ended = false;
        this.winnerID = 0;

        this.serialized_game_copy = null;

        updateSerializedGameCopy();
    }

    //If the turn is correct, allows the player to play one of his cards

    /**
     * Allows the player associated to the clientId to play a card from his hand, specified by its index in the list returned form {@link Player#getCards}.<br><br>
     * {@link InvalidMoveException} is thrown if:
     * <li>It's not the player's turn,</li>
     * <li>The game is not in phase 0,</li>
     * <li>The game has ended,</li>
     * <li>The provided card index is not valid due to the rules prohibiting two identical cards to be played by different players during the same turn (except
     * if there is no alternative) or due to a bad index with respect to the list provided by {@link Player#getCards}.</li>
     *
     * @param clientID   id of the player performing the move
     * @param card_index index of the card to play, relative to {@link Player#getCards}
     * @throws InvalidMoveException thrown whenever the move cannot be performed
     */
    public void playCard(int clientID, int card_index) throws InvalidMoveException {
        if (!isMyTurn(clientID)) throw new InvalidMoveException("It's not your turn.");
        if (phase != 0 || game_ended) throw new InvalidMoveException("Cannot be performed in the current game state.");
        if (card_index < 0 || card_index >= players.get(playerIndexFromID(clientID)).getCards().size())
            throw new InvalidMoveException("Invalid card index.");

        //case of 3 players, all with the same 2 cards in hand
        boolean rare_skip = false;
        if (players.size() == 3 && players.get(playerIndexFromID(clientID)).getCards().size() == 2 && player_turn == 2) {
            rare_skip = true;
            for (Card card : players.get(playerIndexFromID(clientID)).getCards()) {
                if (card.order_value != players.get(current_phase_order.get(0)).getLastCardPlayed().order_value &&
                        card.order_value != players.get(current_phase_order.get(1)).getLastCardPlayed().order_value) {
                    rare_skip = false;
                    break;
                }
            }
        }
        //no-one must have played a card with the same order_value, except if that's the last card available.
        if (players.get(playerIndexFromID(clientID)).getCards().size() > 1 && !rare_skip) {
            boolean temp = false;
            for (int i = 0; i < player_turn; i++)
                if (players.get(current_phase_order.get(i)).getLastCardPlayed().order_value == players.get(playerIndexFromID(clientID)).getCards().get(card_index).order_value)
                    temp = true;
            if (temp)
                throw new InvalidMoveException("A card with the same value has already been played.");
        }

        players.get(playerIndexFromID(clientID)).playCard(card_index);

        player_turn++;
        //if everyone has played a card, computes the order in which players will play and then goes to the next phase!
        if (player_turn == players.size()) {
            player_turn = 0;
            phase = 1;
            step = 0;
            moved_students = 0;
            List<Integer> temp_phase_order = new ArrayList<Integer>();
            for (int i = 0; i < players.size(); i++) {
                int min = 0;
                for (int j = 1; j < players.size(); j++) {
                    //handles the case where you must play 2 cards with the same value because no other one is left
                    if (temp_phase_order.contains(current_phase_order.get(min)))
                        min = j;
                    else if (!temp_phase_order.contains(current_phase_order.get(j))) {
                        if (players.get(current_phase_order.get(j)).getLastCardPlayed().order_value < players.get(current_phase_order.get(min)).getLastCardPlayed().order_value)
                            min = j;
                    }
                }
                temp_phase_order.add(current_phase_order.get(min));
            }
            current_phase_order.clear();
            current_phase_order.addAll(temp_phase_order);

            //it's arbitrary that this is done on the player with index 0
            if (players.get(0).getCards().size() == 0)
                last_game_turn = true;

            updateSerializedGameCopy();
        }
        //System.out.println("played card " + card_index + " by " + clientID);
    }

    //Allows the player to set the indicated student from his entrance to his hall

    /**
     * Allows the player associated to the clientId to set one of his students to his hall, the student is selected with his index
     * relative to the list returned by {@link Dashboard#getEntrance}.<br><br>
     * {@link InvalidMoveException} is thrown if:
     * <li>It's not the player's turn,</li>
     * <li>The game is not in phase 1 or the step is not 0,</li>
     * <li>The game has ended,</li>
     * <li>The provided student index is not valid with respect of the list given by {@link Dashboard#getEntrance}.</li>
     *
     * @param clientID      id of the player performing the move
     * @param student_index index of the student to set, relative to {@link Dashboard#getEntrance}
     * @throws InvalidMoveException thrown whenever the move cannot be performed
     */
    public void setStudentToHall(int clientID, int student_index) throws InvalidMoveException {
        if (!isMyTurn(clientID)) throw new InvalidMoveException("It's not your turn.");
        if (phase != 1 || step != 0 || game_ended)
            throw new InvalidMoveException("Cannot be performed in the current game state.");
        if (student_index >= players.get(playerIndexFromID(clientID)).getDashboard().getEntrance().size() || student_index < 0)
            throw new InvalidMoveException("Invalid student index.");

        Colors student = players.get(playerIndexFromID(clientID)).getDashboard().removeFromEntrance(student_index);
        players.get(playerIndexFromID(clientID)).getDashboard().addStudentToHall(student);
        if (players.get(playerIndexFromID(clientID)).getDashboard().getHallRow(student) % 3 == 0 && expert_mode && bank > 0) {
            players.get(playerIndexFromID(clientID)).addCoin();
            bank--;
        }
        updateProfessors();

        moved_students++;
        if ((moved_students == 3 && players.size() == 2) || (moved_students == 4 && players.size() == 3)) {
            step++;
            moved_students = 0;
        }
        //System.out.println("set student to hall " + student_index + " by " + clientID);
    }

    //Allows the player to set the indicated student from his entrance to the given island

    /**
     * Allows the player associated to the clientId to set one of his students on an island, the student is selected with his index
     * relative to the list returned by {@link Dashboard#getEntrance}, while the island with his index relative to the list from {@link Game#getIslands}.<br><br>
     * {@link InvalidMoveException} is thrown if:
     * <li>It's not the player's turn,</li>
     * <li>The game is not in phase 1 or the step is not 0,</li>
     * <li>The game has ended,</li>
     * <li>The provided student index is not valid with respect of the list given by {@link Dashboard#getEntrance}.</li>
     * <li>The provided island index is not valid with respect of the list given by {@link Game#getIslands}.</li>
     *
     * @param clientID      id of the player performing the move
     * @param student_index index of the student to set, relative to {@link Dashboard#getEntrance}
     * @param island_index  index of the island where to put the student on, relative to {@link Game#getIslands}
     * @throws InvalidMoveException thrown whenever the move cannot be performed
     */
    public void setStudentToIsland(int clientID, int student_index, int island_index) throws InvalidMoveException {
        if (!isMyTurn(clientID)) throw new InvalidMoveException("It's not your turn.");
        if (phase != 1 || step != 0 || game_ended)
            throw new InvalidMoveException("Cannot be performed in the current game state.");
        if (student_index >= players.get(playerIndexFromID(clientID)).getDashboard().getEntrance().size() || student_index < 0)
            throw new InvalidMoveException("Invalid student index.");
        if (island_index >= islands.size() || island_index < 0) throw new InvalidMoveException("Invalid island index.");

        islands.get(island_index).addStudent(players.get(playerIndexFromID(clientID)).getDashboard().removeFromEntrance(student_index));

        moved_students++;
        if ((moved_students == 3 && players.size() == 2) || (moved_students == 4 && players.size() == 3)) {
            step++;
            moved_students = 0;
        }
        //System.out.println("set student " + student_index + " to island " + island_index + " by " + clientID);
    }

    //Allows the player to move mother nature of his desired number of steps

    /**
     * Allows the player associated to the clientId to move mother nature by the given amount of steps.<br><br>
     * {@link InvalidMoveException} is thrown if:
     * <li>It's not the player's turn,</li>
     * <li>The game is not in phase 1 or the step is not 1,</li>
     * <li>The game has ended,</li>
     * <li>The provided number of moves is less than zero or is above what allowed by the movement value
     * of the player's last played card's {@link Card#movements_value movement value}.</li>
     *
     * @param clientID id of the player performing the move
     * @param moves    number of steps to move mother nature by
     * @throws InvalidMoveException thrown whenever the move cannot be performed
     */
    public void moveMotherNature(int clientID, int moves) throws InvalidMoveException {
        if (!isMyTurn(clientID)) throw new InvalidMoveException("It's not your turn.");
        if (phase != 1 || step != 1 || game_ended)
            throw new InvalidMoveException("Cannot be performed in the current game state.");
        if ((moves > players.get(playerIndexFromID(clientID)).getLastCardPlayed().movements_value && npc_effect != 4) ||
                (moves > players.get(playerIndexFromID(clientID)).getLastCardPlayed().movements_value + 2 && npc_effect == 4) || moves < 1)
            throw new InvalidMoveException("Invalid moves number.");

        mother_nature = (mother_nature + moves) % islands.size();
        disputeIsland(mother_nature);

        step++;

        if (last_game_turn) {
            //check for winner if there are either no more cards to play or no more students to set on clouds
            if (player_turn == players.size() - 1) {
                game_ended = true;
                winnerID = computeWinnerBasedOnTowers();
                return;
            }
            step = 0;
            player_turn++;
            npc_effect = 0;

            updateSerializedGameCopy();
        }
        //System.out.println("moved MN " + moves + " by " + clientID);
    }

    //Allows the player to move the students from a cloud to his entrance on his dashboard

    /**
     * Allows the player associated to the clientId to select one of the clouds from {@link Game#getClouds} by providing its index.<br><br>
     * {@link InvalidMoveException} is thrown if:
     * <li>It's not the player's turn,</li>
     * <li>The game is not in phase 1 or the step is not 2,</li>
     * <li>The game has ended,</li>
     * <li>The provided cloud index is not acceptable for the {@link Game#getClouds} array.</li>
     *
     * @param clientID    id of the player performing the move
     * @param cloud_index index of the cloud to choose relative to {@link Game#getClouds}
     * @throws InvalidMoveException thrown whenever the move cannot be performed
     */
    public void chooseCloud(int clientID, int cloud_index) throws InvalidMoveException {
        if (!isMyTurn(clientID)) throw new InvalidMoveException("It's not your turn.");
        if (phase != 1 || step != 2 || game_ended)
            throw new InvalidMoveException("Cannot be performed in the current game state.");
        if (cloud_index >= players.size() || cloud_index < 0 || clouds[cloud_index].size() == 0)
            throw new InvalidMoveException("Invalid cloud index.");

        players.get(playerIndexFromID(clientID)).getDashboard().addToEntrance(clouds[cloud_index]);
        clouds[cloud_index].clear();

        step = 0;
        player_turn++;
        npc_effect = 0;
        if (player_turn >= players.size()) {
            goToNextGameTurn();
        }

        updateSerializedGameCopy();
        //System.out.println("chosen cloud " + cloud_index + " by " + clientID);
    }

    //Allows the player to activate one of the Npcs effects this turn. Only one activation per turn is permitted.

    /**
     * Allows the player associated to the clientId to activate the effect of one of the available Npcs.<br>
     * The Npc is selected via its index inside {@link Game#getNpcs}, while the exact composition of its parameters depends on the specific Npc.<br><br>
     * For details regarding how to construct {@link EffectParameters} for every specific Npc, see the {@link EffectParameters} documentation.<br><br>
     * {@link InvalidMoveException} is thrown if:
     * <li>It's not the player's turn,</li>
     * <li>The game is not in phase 1,</li>
     * <li>The game has ended,</li>
     * <li>Expert mode is not enabled,</li>
     * <li>The provided Npc index is not acceptable for the {@link Game#getNpcs} array.</li>
     * <li>The provided {@link EffectParameters} instance is incompatible with the chosen Npc.</li>
     *
     * @param clientID          id of the player performing the move
     * @param npc_index         index of the Npc to activate relative to {@link Game#getNpcs}
     * @param effect_parameters instance of {@link EffectParameters} constructed in accordance to the specific Npc to activate
     * @throws InvalidMoveException thrown whenever the move cannot be performed
     */
    public void activateEffect(int clientID, int npc_index, EffectParameters effect_parameters) throws InvalidMoveException {
        if (!isMyTurn(clientID)) throw new InvalidMoveException("It's not your turn.");
        if (phase != 1) throw new InvalidMoveException("It's not the action phase.");
        if (npc_index < 0 || npc_index > 2) throw new InvalidMoveException("Invalid npc index.");
        if (!expert_mode) throw new InvalidMoveException("Expert mode is not active.");
        if (effect_parameters == null) throw new InvalidMoveException("Null effect parameters.");
        if (npc_effect != 0) throw new InvalidMoveException("There has already been an effect activation this turn.");

        //in Npc are handled the exceptions for 'not enough coins' and eventual invalid parameters
        npcs[npc_index].activateEffect(this, effect_parameters);
    }

    //Skips the current turn, assumes that everything that was done in this turn has already been undone by the caller via the revertToPreviousState static method, use only if a player disconnects

    /**
     * Skips the turn of the currently playing player, allowing the next player to go ahead and play.<br>
     * If it has to be called after a player has partially completed its turn, requires the caller to execute before revertToPreviousState
     * on its instance of Game.<br>
     * (Example: <code>game = Game.revertToPreviousState(game)</code>)
     * <br><br><p>
     * <strong>Behaviour:</strong>
     * <ul>
     *     <li>If the current phase is 0, the currently playing player is forced to play the first card it can.
     *     <li>If the current phase is 1, the currently playing player's turn is entirely skipped without any other change to the game's state.
     * </ul>
     * </p>
     *
     * @throws InvalidMoveException thrown whenever the move cannot be performed
     */
    public void skipTurn() throws InvalidMoveException {
        if (phase == 0) {
            //since a card must be played to determine the order in which everyone will play, a card is forcefully played
            boolean condition = false;
            int card_index = players.get(current_phase_order.get(player_turn)).getCards().size() - 1;
            do {
                try {
                    playCard(currentlyPlayingPlayer(), card_index);
                    condition = false;
                } catch (InvalidMoveException ex) {
                    condition = true;
                    card_index--;
                }
            } while (condition && card_index >= 0);
        } else {
            moved_students = 0;
            step = 0;
            player_turn++;
            if (player_turn >= players.size()) {
                if (last_game_turn)
                    game_ended = true;
                else
                    goToNextGameTurn();
            }

            updateSerializedGameCopy();
        }
    }

    //Allows the clouds' content to be assigned from outside, used only client-side

    /**
     * Allows the clouds' content to be assigned from outside.<br><br>
     * It's intended ot be used only by {@link Controller.ClientSide.Client}, after the end of a game turn, to synchronize the students shown
     * it knows are on the clouds with the ones provided by the server.
     * <br><br>
     *
     * @param clouds updated array of clouds
     */
    public void updateClouds(List<Colors>[] clouds) {
        this.clouds = clouds;
    }

    //Undoes, if possible, everything that was done during this turn and returns a new instance of Game which is set back to the beginning of the current turn

    /**
     * Returns the given instance of game to its last complete and valid state, exactly after the last turn correctly completed by a player,
     * this implies undoing any turn that was not completed all the way through.<br>
     * Consequently, game turn remains constant, and so do phase and player turn, meanwhile step and moved students are reset.
     *
     * @param game the instance of Game to revert to the previous state
     * @return the given instance of Game returned to its state exactly at the end of the last player's turn
     */
    public static Game revertToPreviousState(Game game) {
        Game result = null;
        if (game.serialized_game_copy == null)
            return game;
        try {
            ObjectInputStream object_in_steam = new ObjectInputStream(new ByteArrayInputStream(game.serialized_game_copy));
            result = (Game) object_in_steam.readObject();
        } catch (IOException | ClassNotFoundException e) {
            e.printStackTrace();
        }
        return result;
    }

    //Returns the currently playing player's clientID

    /**
     * Getter for the id of the player who is entitled to play right now.
     *
     * @return id of the currently playing player
     */
    public int currentlyPlayingPlayer() {
        return players.get(current_phase_order.get(player_turn)).clientID;
    }

    //Returns a list of the player's clientIDs in the order in which they are to playing in the current phase

    /**
     * Getter for a list of all the player ids in the exact order in which their associated players are supposed to play in the current phase.
     *
     * @return list of player ids ordered as per the order they will play in
     */
    public List<Integer> currentPlayersTurnOrder() {
        List<Integer> result = new ArrayList<Integer>();
        for (int player_index : current_phase_order)
            result.add(players.get(player_index).clientID);
        return result;
    }

    /**
     * Getter for a list of the players taking part in the game.
     *
     * @return list of all the Players in the game
     */
    public List<Player> getPlayers() {
        return new ArrayList<Player>(players);
    }

    /**
     * Getter for the current game turn.<br>
     * Game turn starts at 1 and increments by 1 every time both phases, 0 and 1, are played through by every player.
     *
     * @return game turn
     */
    public int getGameTurn() {
        return game_turn;
    }

    /**
     * Getter for the current game phase.<br>
     * The phase varies from 0 to 1 during the course of a game turn, in phase 0 each player has to play one of his cards,
     * in phase 1 the rest of the game takes place with students to move, mother nature to move and a cloud to be chosen.
     *
     * @return phase
     */
    public int getPhase() {
        return phase;
    }

    /**
     * Getter for the current player turn.<br>
     * The player turn indicates which player has to play right now, the returned index is in reference to the order specified by {@link Game#currentPlayersTurnOrder}.
     * This index is incremented after each player concludes its turn and is reset to 0 at the beginning of each phase.
     *
     * @return player turn
     */
    public int getPlayerTurn() {
        return player_turn;
    }

    /**
     * Getter for the current step.<br>
     * The step indicates which task the currently playing player has currently complete to allow the game to proceed, and is relevant only during phase 1.
     * In said phase it indicates respectively that:
     * <bl>
     * <li> 0 - the player has to move students
     * <li> 1 - the player has to move mother nature
     * <li> 2 - the player has to choose a cloud
     * </bl>
     *
     * @return step
     */
    public int getStep() {
        return step;
    }

    /**
     * Getter for the number of moved students.<br>
     * Used only during phase 1 step 1, counts the number of students that the currently playing player has already moved.
     * Outside of phase 1 step 1 is always 0.
     *
     * @return number of moved students
     */
    public int getMovedStudents() {
        return moved_students;
    }

    /**
     * Getter for an array of lists, each representing a cloud.<br>
     * Each list in the array is a cloud, and the elements in such lists are instances of {@link Colors} representing students on said cloud.
     * The length of the array, as the number of islands, is equal to the number of player in the game.
     *
     * @return array of lists acting as clouds
     */
    public List<Colors>[] getClouds() {
        List<Colors>[] result = new List[3];
        for (int i = 0; i < players.size(); i++)
            result[i] = new ArrayList<Colors>(clouds[i]);
        return result;
    }

    /**
     * Getter for the current position of mother nature.<br>
     * The given position is an index referred to the list of islands provided by {@link Game#getIslands}.
     *
     * @return mother nature's position
     */
    public int getMotherNature() {
        return mother_nature;
    }

    /**
     * Getter for the islands in the game.<br>
     * The returned list's size can diminish during the curse of a game, with 2 islands getting merged implicating
     * the removal of an element from the list, however the list's order is always preserved.
     *
     * @return list of {@link Island Islands}
     */
    public List<Island> getIslands() {
        return new ArrayList<Island>(islands);
    }

    /**
     * Getter for the yet unclaimed professors.<br>
     * Gives back a list of professors, identified by their color via the {@link Colors} enum, that are yet to be
     * claimed by any player in the game.
     *
     * @return list of professors indicated as colors
     */
    public List<Colors> getUnclaimedProfessors() {
        return new ArrayList<Colors>(unclaimed_professors);
    }

    /**
     * Getter for the {@link Npc Npcs} available in this game.<br>
     * Returns null unless expert mode is enabled.
     *
     * @return array of {@link Npc Npcs}
     */
    public Npc[] getNpcs() {
        if (!expert_mode) return null;
        return npcs.clone();
    }

    /**
     * Getter for the id of the {@link Npc} that got activated in this turn.<br>
     * If no {@link Npc} has been activated yet, or expert mode is not active, 0 is always returned.
     *
     * @return the id of the {@link Npc} that got activated during this turn, 0 if no {@link Npc} has been activated
     */
    public int getNpcEffect() {
        return npc_effect;
    }

    /**
     * Getter for the current amount of coins still available in the game.
     * If expert mode is not active 0 is always returned.
     *
     * @return the number of coins still available
     */
    public int getBank() {
        return bank;
    }

    /**
     * Getter for the number of students left to be extracted from the pouch.
     *
     * @return the number of students left in the pouch
     */
    public int getRemainingStudentsNum() {
        return random_students.size();
    }

    /**
     * Getter for the flag indicating whether after the current turn the game will forcibly finish due to various conditions
     * such as:
     * <ul>
     *     <li> there are no more cards in the hand of players
     *     <li> there are no more students available in the pouch
     * </ul>
     *
     * @return last game turn flag, true if the game will end at the end of this turn
     */
    public boolean isLastGameTurn() {
        return last_game_turn;
    }

    /**
     * Getter indicating if this instance of game has finished.
     *
     * @return true if the current game has ended, false otherwise
     */
    public boolean isGameEnded() {
        return game_ended;
    }

    /**
     * Getter for the id of the player who won the game.<br>
     * Always 0 unless the game has ended.
     *
     * @return id of the winner player
     */
    public int getWinnerID() {
        return winnerID;
    }

    /**
     * Getter for the serialization of this instance of Game.
     *
     * @return bytearray with the serialized instance of this class
     */
    public byte[] getGameSerialization() {
        try {
            ByteArrayOutputStream bytearray_output_stream = new ByteArrayOutputStream();
            ObjectOutputStream object_out_steam = new ObjectOutputStream(bytearray_output_stream);
            object_out_steam.writeObject(this);
            object_out_steam.flush();
            object_out_steam.close();
            return bytearray_output_stream.toByteArray();
        } catch (IOException e) {
            e.printStackTrace();
            return null;
        }
    }

    //returns true only if it's the given player's turn

    /**
     * Returns true if the given player id coincides with the one of the currently playing player.
     *
     * @param clientID to check if coincides with the one of the player which plays this turn.
     * @return boolean result of the comparison
     */
    public boolean isMyTurn(int clientID) {
        return players.get(current_phase_order.get(player_turn)).clientID == clientID;
    }

    //Converts from clientID to the player index inside this.players

    /**
     * Helper method which converts the given player id into the index of such player inside the players list.<br>
     * The returned index is what is used inside the model to conveniently identify players.
     *
     * @param clientID the client id to convert into an index
     * @return the index of the requested player inside the players list.
     */
    protected int playerIndexFromID(int clientID) {
        return IntStream.range(0, players.size())
                .filter(i -> players.get(i).clientID == clientID)
                .findAny().orElseThrow();
    }

    //Extracts the given number of students from the randomized list, if there are not enough students available returns as many as there are still available, eventually returns an empty list if none is available

    /**
     * Extracts the requested number of students, in the from of {@link Colors colors}, from the pouch and returns them.<br>
     * If there are not enough students left in the pouch the last game turn flag is set and as many students as possible are returned.
     *
     * @param num number of students to extract and return
     * @return list of the extracted students
     */
    protected List<Colors> extractStudents(int num) {
        List<Colors> result = new ArrayList<Colors>(random_students.subList(0, Math.min(num, random_students.size())));
        random_students.subList(0, Math.min(num, random_students.size())).clear();
        if (random_students.size() == 0) {
            last_game_turn = true;
        }
        return result;
    }

    //Computes the dispute on the island specified by the index inside the islands list, if the ownership changes proceeds to call checkForMerges

    /**
     * Computes a dispute on the {@link Island island} selected via the given index.<br>
     * A dispute is computed every time mother nature lands on an {@link Island island}, or via specific npc effects.<br>
     * Computing a dispute implies transferring ownership of the {@link Island island} if someone else rather than the current owner has more points on the {@link Island island},
     * or if there isn't yet a current owner it means directly giving the ownership to the player with more points. A tie implies no changes except in
     * the case of specific npc effects.<br>
     * 1 point is given by each student for which the player has the corresponding professor, 1 point is given by each rook and
     * other points can be given by npc effects.
     *
     * @param index index of the {@link Island island} where to compute the dispute, relative to the {@link Island islands} list
     */
    protected void disputeIsland(int index) {
        //handle interdiction effect
        if (islands.get(index).getInterdiction()) {
            islands.get(index).setInterdiction(false);
            for (int i = 0; i < 3; i++) {
                if (npcs[i].getId() == 5) {
                    npcs[i].extraEffect();
                    break;
                }
            }
            return;
        }

        int[] per_player_total = new int[players.size()];
        int[] professor_owners = new int[5];

        for (int i = 0; i < 5; i++) {
            for (int j = 0; j < players.size(); j++) {
                if (players.get(j).getDashboard().getProfessor(i))
                    professor_owners[i] = j;
            }
        }

        if (npc_effect != 6 && islands.get(index).getOwnerIndex() != null)
            per_player_total[islands.get(index).getOwnerIndex()] += islands.get(index).getNumOfMergedIslands();

        int temp = -1;
        if (npc_effect == 9) {
            for (Npc npc : npcs) if (npc.getId() == 9) temp = npc.getExtraProperty().get(0);
        }
        for (int i = 0; i < 5; i++) {
            if (temp == i)
                continue;
            if (!unclaimed_professors.contains(Colors.fromColorIndex(i)))
                per_player_total[professor_owners[i]] += islands.get(index).getStudents(i);
        }

        if (npc_effect == 8)
            per_player_total[current_phase_order.get(player_turn)] += 2;

        int max_index = 0;
        boolean tie = false;
        for (int i = 1; i < per_player_total.length; i++) {
            if (per_player_total[i] > per_player_total[max_index]) {
                max_index = i;
                tie = false;
            } else if (per_player_total[i] == per_player_total[max_index])
                tie = true;
        }

        if ((islands.get(index).getOwnerIndex() == null ||
                max_index != islands.get(index).getOwnerIndex()) && !tie) {
            if (islands.get(index).getOwnerIndex() != null)
                players.get(islands.get(index).getOwnerIndex()).getDashboard().increaseRooks(islands.get(index).getNumOfMergedIslands());
            islands.get(index).setOwnerIndex(max_index);
            players.get(max_index).getDashboard().decreaseRooks(islands.get(index).getNumOfMergedIslands());
            checkForMerges();
            //check for winner if he used his last tower
            for (Player player : players) {
                if (player.getDashboard().getRooks() <= 0) {
                    game_ended = true;
                    winnerID = player.clientID;
                    break;
                }
            }
        }
    }

    //Checks for potential merges and if any is found, it is computed, repeats until no potential merges are left

    /**
     * Scans all the {@link Island islands} executing each merge that's possible between them.<br>
     * When 2 {@link Island islands} with adjacent index (wrap-around included) share a common owner, the first {@link Island island} merges into
     * itself the second one, which in turn gets deleted from the {@link Island islands} list.<br>
     * <br>
     * This method is executed after every change to the ownership of an {@link Island island}.
     */
    protected void checkForMerges() {
        boolean repeat = true;
        while (repeat && islands.size() > 3) {
            repeat = false;
            for (int i = 0; i < islands.size(); i++) {
                if (islands.get(i).merge(islands.get((i + 1) % islands.size()))) {
                    if (mother_nature >= (i + 1) % islands.size())
                        mother_nature = (mother_nature - 1 + (islands.size() - 1)) % (islands.size() - 1);

                    islands.remove((i + 1) % islands.size());

                    repeat = true;
                    break;
                }
            }
        }

        //check for winner if there are only 3 islands left
        if (islands.size() == 3) {
            game_ended = true;
            winnerID = computeWinnerBasedOnTowers();
        }
    }

    //Checks if any professor has to be allocated from the table to a player or re-allocated from a player to another
    /**
     * Computes the rightful owner of each professor, updating them if needed.<br>
     * For each {@link Colors color} of professor, the number of students of that {@link Colors color} in each of the
     * players's dashboards is considered, and if a player owns more students that the current professor owner, than
     * the ownership is transferred to him. Exceptions made in the case of npc effects.<br>
     * <br>
     * This method is called every time a student is set to a player's hall.
     */
    protected void updateProfessors() {
        for (int i = 0; i < unclaimed_professors.size(); i++) {
            for (Player player : players) {
                if (player.getDashboard().getHallRow(unclaimed_professors.get(i)) > 0) {
                    player.getDashboard().setProfessor(unclaimed_professors.remove(i), true);
                    break;
                }
            }
        }
        for (Colors color : Colors.values()) {
            if (!unclaimed_professors.contains(color)) {
                int current_owner;
                //j could start from 1
                for (current_owner = 0; current_owner < players.size(); current_owner++) {
                    if (players.get(current_owner).getDashboard().getProfessor(color)) {
                        break;
                    }
                }
                int rightful_owner = current_owner;
                for (int j = 0; j < players.size(); j++) {
                    if (current_owner == j) continue;
                    if (players.get(j).getDashboard().getHallRow(color) > players.get(current_owner).getDashboard().getHallRow(color))
                        rightful_owner = j;
                }
                if (npc_effect == 2 && players.get(current_phase_order.get(player_turn)).getDashboard().getHallRow(color) >= players.get(current_owner).getDashboard().getHallRow(color))
                    rightful_owner = current_phase_order.get(player_turn);
                if (current_owner != rightful_owner) {
                    players.get(current_owner).getDashboard().setProfessor(color, false);
                    players.get(rightful_owner).getDashboard().setProfessor(color, true);
                }
            }
        }
    }

    //Computes the winner in the case that it has to be decided based on the number of islands and/or professors controlled
    /**
     * Chooses a winner after the last game turn has ended, choosing the player who has the most towers, and, in case
     * of a tie, the one with the most professors under his control.
     * If there is a tie even with professors factored in, then the player with the lowest internal index (that being
     * the first one to join the lobby among the tied ones) is considered the winner.
     *
     * @return clientId of the winner
     */
    protected int computeWinnerBasedOnTowers() {
        int[] scores = new int[players.size()];
        for (Island island : islands)
            if (island.getOwnerIndex() != null)
                scores[island.getOwnerIndex()] += island.getNumOfMergedIslands();
        int winner = 0;
        boolean contested = false;
        for (int i = 1; i < players.size(); i++) {
            if (scores[winner] < scores[i]) {
                winner = i;
                contested = false;
            } else if (scores[winner] == scores[i])
                contested = true;
        }

        for (int i = 0; i < players.size(); i++)
            if (scores[winner] != scores[i])
                scores[i] = -5;

        //NOTE: if there is a tie, even with professors counted in, the lower-index player wins
        if (contested) {
            for (Colors color : Colors.values()) {
                for (int i = 0; i < players.size(); i++)
                    if (players.get(i).getDashboard().getProfessor(color))
                        scores[i]++;
            }

            winner = 0;
            for (int i = 1; i < players.size(); i++) {
                if (scores[winner] < scores[i])
                    winner = i;
            }
        }

        return players.get(winner).clientID;
    }

    //Concludes the current game_turn and sets up everything for the next one
    /**
     * Concludes the current game turn and sets up the next one, by resetting the clouds, npcs, and turn order for the players.
     */
    protected void goToNextGameTurn() {
        player_turn = 0;
        phase = 0;
        game_turn++;

        //Collections.reverse(current_phase_order); would have been cool but...
        int temp = current_phase_order.get(0);
        current_phase_order.clear();
        //Assuming the order of "players" represents a clockwise turn...
        for (int i = 0; i < players.size(); i++)
            current_phase_order.add((temp + i) % players.size());

        for (int i = 0; i < players.size(); i++) {
            if (clouds[i].size() == 0)
                clouds[i].addAll(extractStudents(players.size() + 1));
        }
    }

    //Backs up the current game state in a serialized version of this class inside itself

    /**
     * Updates the serialized copy that every instance of {@link Game} holds of itself with a new serialization computed within this method.
     */
    protected void updateSerializedGameCopy() {
        try {
            ByteArrayOutputStream bytearray_output_stream = new ByteArrayOutputStream();
            ObjectOutputStream object_out_steam = new ObjectOutputStream(bytearray_output_stream);
            object_out_steam.writeObject(this);
            object_out_steam.flush();
            object_out_steam.close();
            this.serialized_game_copy = bytearray_output_stream.toByteArray();
        } catch (IOException e) {
            e.printStackTrace();
            this.serialized_game_copy = null;
        }
    }

    /**
     * Costume deserialization, initializes adequately the transient fields.
     *
     * @param in source stream for the serialized instance of this class
     */
    @Serial
    private void readObject(java.io.ObjectInputStream in) throws IOException, ClassNotFoundException {
        in.defaultReadObject();

        //deserialize with just as many students as you serialized with
        /*random_students = new ArrayList<Colors>();
        for(int i = 0; i < 5; i++) {
            for(int j = 0; j < 24; j++) {
                this.random_students.add(Colors.fromColorIndex(i));
            }
        }
        Collections.shuffle(random_students);
        */
    }

    /**
     * Returns a deep copy of this class.
     *
     * @return deep copy of this instance of {@link Game}
     */
    public Game copy() {
        Game result = null;
        byte[] serialized_game = getGameSerialization();
        try {
            ObjectInputStream object_in_steam = new ObjectInputStream(new ByteArrayInputStream(serialized_game));
            result = (Game) object_in_steam.readObject();
        } catch (IOException | ClassNotFoundException e) {
            e.printStackTrace();
        }
        return result;
    }
}
