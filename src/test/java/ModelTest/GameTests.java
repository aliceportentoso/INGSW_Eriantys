package ModelTest;

import Exceptions.InvalidMoveException;
import Model.*;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Named;
import org.junit.jupiter.api.TestInstance;
import org.junit.jupiter.params.provider.Arguments;

import java.util.*;
import java.util.random.RandomGenerator;
import java.util.stream.Collectors;
import java.util.stream.IntStream;
import java.util.stream.Stream;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Abstract class designed to be implemented by a test class to provide support to run
 * <code>@ParameterizedTest</code>(s) related to {@link Game} objects.
 * <p>
 * This class provides the <code>Game</code> wrapper {@link TestGame TestGame}, which is passed as the parameter for the test
 * methods, instead of <code>Game</code> itself directly. This wrapper provides useful testing functionalities,
 * see its documentation for more information.
 * </p>
 * <p>
 * To provide the test cases to be passed as the parameter for the tests, the extending class must implement the
 * {@link GameTests#createGameTests} method, which is used to initialize the private list which holds the test cases.
 * </p>
 * @implNote This class technically works as follows: the extending class implements the <code>createGameTests</code>
 * method -> this method is used by method {@link GameTests#prepare} (which is run once before all tests) to initialize
 * the private {@link GameTests#game_tests} list, which will hold the <code>TestGame</code>(s) to be passed as the
 * parameter for all the <code>@ParameterizedTest</code>s -> this list is then used by the {@link GameTests#argumentsSupplier}
 * method to provide the actual <code>Stream</code> of <code>Arguments</code> as required by <code>@ParameterizedTest</code>s
 * as the source.
 * @implSpec All the parametrized tests must be annotated as follows:
 * <pre>{@code
 *         @ParameterizedTest(name = "{0}")
 *         @MethodSource(arguments_supplier)
 *         }</pre>
 */
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
public abstract class GameTests {

    /**
     * Controls games' moves console logging while running tests
     */
    static final boolean debug = false;

    /**
     * holder of the TestGame(s) to be used as the parameter for the tests.
     */
    static private List<TestGame> game_tests;
    /**
     * Refers to the {@link GameTests#argumentsSupplier} method.
     * <p>
     *     Defined for convenience as the argument of the <code>MethodSource</code> annotation.
     * </p>
     */
    static final String arguments_supplier = "ModelTest.GameTests#argumentsSupplier";
    static final RandomGenerator random = new Random(1);

    /**
     * Initializes the {@link GameTests#game_tests} list once before all tests are run, using the {@link GameTests#createGameTests}
     * method implemented by the extending class.
     */
    @BeforeAll
    void prepare() {
        game_tests = createGameTests();
    }

    /**
     * Provides the list containing the {@link TestGame TestGame} object(s) which will be used as the parameter for all the tests.
     */
    abstract List<TestGame> createGameTests();

    /**
     * Formats the list of <code>game_tests</code>. This method is the designed one
     * as the source for all the <code>@ParameterizedTest</code>.
     *
     * @return The <code>game_tests</code> list as a <code>Stream</code> of <code>Arguments</code>, as required
     * by <code>@ParameterizedTest</code>s.
     * @see GameTests
     * @implNote A new {@link TestGame TestGame} containing a copy of the original {@link Game} is passed to each
     * <code>@ParameterizedTest</code> instead of the same object for all the tests.
     */
    @SuppressWarnings("unused")
    static Stream<Arguments> argumentsSupplier() {
        return game_tests.stream().map(game_test -> Arguments.of(Named.of(game_test.toString(), game_test.clone())));
    }

    /**
     * Wrapper for a {@link Game} object useful to run tests.
     * <p>
     *      <code>TestGame</code> has the following properties which can be used to define the <code>Game</code> and therefore establish the truth for the test:
     *      <li><code>designated_expert_mode</code>: <code>boolean</code> that distinguishes between expert (true) and basic (false) game modes.</li>
     *      <li><code>designated_num_of_players</code>: the desired number of players for the game.</li>
     *      <li><code>clientIDs</code>: the clientIDs list provided to the <code>Game</code>.</li>
     * </p>
     * <p>
     *     This class provides useful methods to prepare, investigate and manipulate <code>Game</code> object:
     *     <li>{@link TestGame#buildRandomizedClientIDsList}</li>
     *     <li>{@link TestGame#playingPlayer}</li>
     *     <li>{@link TestGame#orderedPayers}</li>
     *     <li>{@link TestGame#randomlyPlayCard}</li>
     *     <li>{@link TestGame#randomlyMoveStudentToHall}</li>
     *     <li>{@link TestGame#randomlyMoveStudentToIsland}</li>
     *     <li>{@link TestGame#randomlyMoveMotherNature}</li>
     *     <li>{@link TestGame#randomlyChooseCloud}</li>
     *     <li>{@link TestGame#autoplayUpTo}</li>
     *     <li>{@link TestGame#disputeIsland}</li>
     *     <li>{@link TestGame#prepareEffectParameters}</li>
     *     <li>{@link TestGame#hasNothingChangedExcept}</li>
     *     Most of them comes with both a static and a non static version (therefore manipulating directly the contained <code>Game</code>
     *     without the need to pass it as a method parameter). See their documentation for more information.
     * </p>
     * <p>
     *      There are constructors to both autogenerate a game, based on the given properties, and to use an already
     *      instanced game (which will still require to pass all the properties above to correctly assert their
     *      correctness by the tests.
     * </p>
     * <p>
     *      It is also possibile to assign a custom name to the <code>TestGame</code> (with specific constructors) that
     *      will be shown while tests are run (otherwise an autogenerated name will be used, see {@link TestGame#toString}).
     * </p>
     */
    static public class TestGame {
        public final boolean designated_expert_mode;
        public final int designated_num_of_players;
        public final List<Integer> clientIDs;
        private final String custom_game_name;

        private Game old_game = null; //see hasNothingChangedExcept method
        public Game game;

        /**
         * Constructor of <code>gameTest</code> that will automatically create both the {@link TestGame#game} (
         * and its {@link TestGame#clientIDs}) and the {@link TestGame#custom_game_name}.
         */
        public TestGame(final boolean designated_expert_mode, final int designated_num_of_players) {
            this.designated_expert_mode = designated_expert_mode;
            this.designated_num_of_players = designated_num_of_players;

            this.clientIDs = buildRandomizedClientIDsList(designated_num_of_players);

            this.game = new Game(designated_expert_mode, this.clientIDs);

            this.custom_game_name = null;

            updateOldGameCopy();
        }

        /**
         * Constructor of <code>gameTest</code> that will automatically create the
         * {@link TestGame#custom_game_name}.
         *
         * @param game      An already instanced game to be used for the <code>gameTest</code>.
         * @param clientIDs clientID list needs to be provided separately to che constructor in order
         *                  to be correctly asserted by the tests.
         */
        public TestGame(final boolean designated_expert_mode, final int designated_num_of_players, final Game game, final List<Integer> clientIDs) {
            this.designated_expert_mode = designated_expert_mode;
            this.designated_num_of_players = designated_num_of_players;
            this.clientIDs = clientIDs;

            this.game = game;

            this.custom_game_name = null;

            updateOldGameCopy();
        }

        /**
         * Constructor of <code>gameTest</code>.
         *
         * @param game      An already instanced game to be used for the <code>gameTest</code>.
         * @param clientIDs clientID list needs to be provided separately to che constructor in order
         *                  to be correctly asserted by the tests.
         */
        public TestGame(final String custom_game_name, final boolean designated_expert_mode, final int designated_num_of_players, final Game game, final List<Integer> clientIDs) {
            this.designated_expert_mode = designated_expert_mode;
            this.designated_num_of_players = designated_num_of_players;
            this.clientIDs = clientIDs;

            this.game = game;

            this.custom_game_name = custom_game_name;

            updateOldGameCopy();
        }

        /**
         * Provides a randomized ClientIDs list.
         * @param num_of_players The number of clientIDs that should be in the list.
         * @return the randomized list, ready to be assigned to the {@link TestGame#clientIDs} list.
         */
        static public List<Integer> buildRandomizedClientIDsList(int num_of_players) {
            List<Integer> result = new ArrayList<>(num_of_players);
            while (num_of_players-- != 0) {
                int new_clientID;
                do new_clientID = random.nextInt();
                while (new_clientID == 0 || result.contains(new_clientID));
                result.add(new_clientID);
            }
            return result;
        }

        /**
         * Updates the game copy used by the {@link TestGame#hasNothingChangedExcept} method to the current game
         * state at the moment of the call.
         */
        public void updateOldGameCopy() {
            old_game = game.copy();
        }

        /**
         * Compares an old copy of the game asserting that nothing between that and the current
         * game state has changed, except at most the specified scope(s), which are excluded.
         * <p>
         *     Each scope is identified with a number:
         *     <li><code>0</code> - each player and its dashboard</li>
         *     <li><code>1</code> - game ended status</li>
         *     <li><code>2</code> - general game status</li>
         *     <li><code>3</code> - students</li>
         *     <li><code>4</code> - bank</li>
         *     <li><code>5</code> - clouds</li>
         *     <li><code>6</code> - mother nature</li>
         *     <li><code>7</code> - npcs costs</li>
         *     <li><code>8</code> - winner id</li>
         * </p>
         * <p>
         *     Technically, the method checks, for every specified scope, if the result of every related game getter method is
         *     equal between the two game instances. It uses junit assertions, behaving like a test.
         * </p>
         * @param excluded_scopes The numbers that identify the scopes to be excluded (as specified above), as a variable
         * list of int arguments.
         * @implNote The old game copy is automatically instanced with the creation of the <code>GameState</code>, and can be
         * updated using the {@link TestGame#updateOldGameCopy} method.
         */
        public void hasNothingChangedExcept(final int... excluded_scopes) {
            if (old_game == null) return;

            //player's (and its dashboard) getters
            if (Arrays.stream(excluded_scopes).noneMatch(x -> x == 0)) {
                assertEquals(old_game.getPlayers().size(), game.getPlayers().size(), "Players list has changed unexpectedly");
                assertAll(() -> {
                    for (int i = 0; i < game.getPlayers().size(); i++) {
                        final Player player = game.getPlayers().get(i);
                        final Player old_player = old_game.getPlayers().get(i);
                        assertEquals(old_player.getCoins(), player.getCoins(), "Coins of player #" + i + " have changed unexpectedly");
                        assertEquals(old_player.getLastCardPlayed(), player.getLastCardPlayed(), "Last card played for player #" + i + " has changed unexpectedly");
                        assertEquals(old_player.getCards(), player.getCards(), "Cards of player #" + i + " has changed unexpectedly");
                        assertEquals(old_player.getDashboard().getRooks(), player.getDashboard().getRooks(), "Rooks of player #" + i + " have changed unexpectedly");
                        assertEquals(old_player.getDashboard().getEntrance(), player.getDashboard().getEntrance(), "Entrance of player #" + i + " has changed unexpectedly");
                        for (int j = 0; j < Colors.values().length; j++) {
                            assertEquals(old_player.getDashboard().getProfessor(j), player.getDashboard().getProfessor(j), "Professor " + Colors.fromColorIndex(j) + " of player #" + i + " has changed unexpectedly");
                            assertEquals(old_player.getDashboard().getHallRow(j), player.getDashboard().getHallRow(j), "Number of students in hall row " + j + " for player #" + i + " has changed unexpectedly");
                        }
                    }
                });
            }
            //game ended status
            if(Arrays.stream(excluded_scopes).noneMatch(x -> x == 1))
                assertEquals(old_game.isGameEnded(), game.isGameEnded(), "Game is ended status has changed unexpectedly");
            //general game status
            if (Arrays.stream(excluded_scopes).noneMatch(x -> x == 2)) {
                assertAll(() -> {
                    assertEquals(old_game.currentlyPlayingPlayer(), game.currentlyPlayingPlayer(), "Currently playing player has changed unexpectedly");
                    assertEquals(old_game.getPlayerTurn(), game.getPlayerTurn(), "Player turn has changed unexpectedly");
                    assertEquals(old_game.getPhase(), game.getPhase(), "Game phase has changed unexpectedly");
                    assertEquals(old_game.getStep(), game.getStep(), "Game step has changed unexpectedly");
                    assertEquals(old_game.getGameTurn(), game.getGameTurn(), "Game turn has changed unexpectedly");
                    assertEquals(old_game.getMovedStudents(), game.getMovedStudents(), "Moved students have changed unexpectedly");
                });
            }
            //students
            if(Arrays.stream(excluded_scopes).noneMatch(x -> x == 3))
                assertEquals(old_game.getRemainingStudentsNum(), game.getRemainingStudentsNum(), "Number of available students has changed unexpectedly");
            //bank
            if (Arrays.stream(excluded_scopes).noneMatch(x -> x == 4))
                assertEquals(old_game.getBank(), game.getBank(), "Bank has changed unexpectedly");
            //clouds
            if (Arrays.stream(excluded_scopes).noneMatch(x -> x == 5))
                assertArrayEquals(old_game.getClouds(), game.getClouds(), "Clouds have changed unexpectedly");
            //mother nature
            if (Arrays.stream(excluded_scopes).noneMatch(x -> x == 6))
                assertEquals(old_game.getMotherNature(), game.getMotherNature(), "Mother nature position has changed unexpectedly");
            //npcs costs
            if (old_game.expert_mode && Arrays.stream(excluded_scopes).noneMatch(x -> x == 7))
                assertEquals(Arrays.stream(old_game.getNpcs()).map(Npc::getId).toList(), Arrays.stream(game.getNpcs()).map(Npc::getId).toList(), "Npcs costs have changed unexpectedly");
            //winner
            if (Arrays.stream(excluded_scopes).noneMatch(x -> x == 8))
                assertEquals(old_game.getWinnerID(), game.getWinnerID(), "Winner has changed unexpectedly");
        }

        /**
         * Orders the players list based on the current playing order.
         * @param game The game from which to compute the ordered list.
         * @return The ordered players list.
         */
        static public List<Player> orderedPayers(final Game game) {
            final List<Player> ordered_list = new ArrayList<>();
            for (final Integer clientID : game.currentPlayersTurnOrder()) {
                Player player = game.getPlayers().stream().filter(p -> p.clientID == clientID).findAny().orElseThrow();
                ordered_list.add(player);
            }
            return ordered_list;
        }

        /**
         * Non-static version of {@link TestGame#orderedPayers} method.
         */
        public List<Player> orderedPlayers() {
            return orderedPayers(this.game);
        }

        /**
         * Returns the currently playing player in the specified game.
         * @param game The game from which to determinate the currently playing player.
         * @return The currently playing player.
         */
        static public Player playingPlayer(final Game game) {
            return game.getPlayers().stream().filter(player -> player.clientID == game.currentlyPlayingPlayer())
                    .findAny().orElseThrow();
        }

        /**
         * Non-static version of {@link TestGame#playingPlayer}.
         */
        public Player playingPlayer() {
            return playingPlayer(this.game);
        }

        /**
         * Randomly performs the action of playing a card by the currently playing player on the specified game.
         * @param game The game where to play.
         * @return The index of the randomly played card.
         * @throws InvalidMoveException It elevates the exception eventually thrown by the Game method used.
         */
        static public int randomlyPlayCard(final Game game) throws InvalidMoveException {
            final Player playing_player = playingPlayer(game);
            int card_index;
            if(playing_player.getCards().size() > 1) {
                if(game.getPlayerTurn() == 2 && playing_player.getCards().size() == 2 && game.getPlayers().size() == 3
                    && playing_player.getCards().stream().allMatch(card ->
                        game.getPlayers().stream().filter(player -> player != playing_player).anyMatch(player -> card.order_value == player.getLastCardPlayed().order_value)))
                    card_index = random.nextInt(2); //2 == playing_player.getCards().size()
                else {
                    boolean condition;
                    do {
                        card_index = random.nextInt(playing_player.getCards().size());
                        condition = false;
                        for(int i = 0; i < game.getPlayerTurn(); i++) {
                            if (orderedPayers(game).get(i).getLastCardPlayed().order_value == playing_player.getCards().get(card_index).order_value) {
                                condition = true;
                                break;
                            }
                        }
                    } while (condition);
                }
            }
            else card_index = 0;
            game.playCard(playing_player.clientID, card_index);
            if(debug) System.out.println("game.playCard(" + playing_player.clientID + ", " + card_index + ");");
            return card_index;
        }

        /**
         * Non-static version of {@link TestGame#randomlyPlayCard}.
         */
        public int randomlyPlayCard() throws InvalidMoveException {
            return randomlyPlayCard(this.game);
        }

        /**
         * Randomly performs the action of moving a student into the hall by the currently playing player on the specified game.
         * @param game The game where to play.
         * @return The color index of the randomly chosen student.
         * @throws InvalidMoveException It elevates the exception eventually thrown by the Game method used.
         */
        static public int randomlyMoveStudentToHall(final Game game) throws InvalidMoveException {
            final Player playing_player = playingPlayer(game);
            final int random_student = random.nextInt(playing_player.getDashboard().getEntrance().size());
            final int random_student_color_index = playing_player.getDashboard().getEntrance().get(random_student).index;
            game.setStudentToHall(playing_player.clientID, random_student);
            if(debug) System.out.println("game.setStudentToHall(" + playing_player.clientID + ", " + random_student + ");");
            return random_student_color_index;
        }

        /**
         * Non-static version of {@link TestGame#randomlyMoveStudentToHall}.
         */
        public int randomlyMoveStudentToHall() throws InvalidMoveException {
            return randomlyMoveStudentToHall(this.game);
        }

        /**
         * Randomly performs the action of moving a student to an island by the currently playing player on the specified game.
         * @param game The game where to play.
         * @return the index of the randomly chosen island where the randomly chosen student has been moved.
         * @throws InvalidMoveException It elevates the exception eventually thrown by the Game method used.
         */
        static public int randomlyMoveStudentToIsland(final Game game) throws InvalidMoveException {
            final int random_student = random.nextInt(playingPlayer(game).getDashboard().getEntrance().size());
            final int random_island = random.nextInt(game.getIslands().size());
            if(debug) System.out.println("game.setStudentToIsland(" + playingPlayer(game).clientID + ", " + random_student + ", " + random_island + ");");
            game.setStudentToIsland(playingPlayer(game).clientID, random_student, random_island);
            return random_island;
        }

        /**
         * Non-static version of {@link TestGame#randomlyMoveStudentToIsland}.
         */
        public int randomlyMoveStudentToIsland() throws InvalidMoveException {
            return randomlyMoveStudentToIsland(this.game);
        }

        /**
         * Randomly performs the action of moving mother nature by the currently playing player on the specified game.
         * @param game The game where to play.
         * @return The number of moves randomly chosen.
         * @throws InvalidMoveException It elevates the exception eventually thrown by the Game method used.
         */
        static public int randomlyMoveMotherNature(final Game game) throws InvalidMoveException{
            final int random_moves = random.nextInt(1, playingPlayer(game).getLastCardPlayed().movements_value + 1);
            if(debug) System.out.println("game.moveMotherNature(" + playingPlayer(game).clientID + ", " + random_moves + ");");
            game.moveMotherNature(playingPlayer(game).clientID, random_moves);
            return random_moves;
        }

        /**
         * Non-static version of {@link TestGame#randomlyMoveMotherNature}.
         */
        public int randomlyMoveMotherNature() throws InvalidMoveException {
            return randomlyMoveMotherNature(this.game);
        }

        /**
         * Randomly performs the action of choosing a cloud by the currently playing player on the specified game.
         * @param game The game where to play.
         * @return The index of the randomly chosen cloud.
         * @throws InvalidMoveException It elevates the exception eventually thrown by the Game method used.
         */
        static public int randomlyChooseCloud(final Game game) throws InvalidMoveException {
            int random_cloud;
            if(Arrays.stream(game.getClouds()).allMatch(List::isEmpty))
                random_cloud = 0;
            else {
                do random_cloud = random.nextInt(game.getPlayers().size());
                while (game.getClouds()[random_cloud].size() == 0);
            }
            if(debug) System.out.println("game.chooseCloud(" + playingPlayer(game).clientID + ", " + random_cloud + ");");
            game.chooseCloud(playingPlayer(game).clientID, random_cloud);
            return random_cloud;
        }

        /**
         * Non-static version of {@link TestGame#randomlyChooseCloud}.
         */
        public int randomlyChooseCloud() throws InvalidMoveException {
            return randomlyChooseCloud(this.game);
        }

        /**
         * Determines the new owner of the specified island as if mother nature just moved there.
         * @param game The Game where to simulate the computation.
         * @param island_index The island index of which to determinate the new owner.
         * @return The player's index of the computed rightful owner (eventually <code>null</code> if the island should still have no owner).
         *          <p>Returns <code>-1</code> when failing to determinate the new owner.</p>
         * @implNote This method simulates mother nature movement, without actually playing, assuming that the
         * mother nature movement would be the next action played.
         * @see TestGame#disputeIsland
         */
        static private Integer computeIslandOwner(final Game game, final int island_index) {
            final Island island = game.getIslands().get(island_index);
            if(island.getInterdiction())
                return island.getOwnerIndex();

            int[] influences = new int[game.getPlayers().size()];
            List<Colors> colors = Arrays.stream(Colors.values()).collect(Collectors.toList());
            switch (game.getNpcEffect()) {
                case 6 -> {
                    if(island.getOwnerIndex() != null)
                        influences[island.getOwnerIndex()] -= island.getNumOfMergedIslands();
                }
                case 8 -> influences[playingPlayer(game).player_index] += 2;
                case 9 -> IntStream.range(0, game.getNpcs().length)
                        .filter(i -> game.getNpcs()[i].getId() == 9)
                        .findFirst()
                        .ifPresent(i -> colors.remove(game.getNpcs()[i].getExtraProperty().get(0).intValue()));
            }
            for(final Player player : game.getPlayers()) {
                for(final Colors color : colors)
                    if (player.getDashboard().getProfessor(color))
                        influences[player.player_index] += island.getStudents(color);
                if(island.getOwnerIndex() != null && island.getOwnerIndex() == player.player_index)
                    influences[player.player_index] += island.getNumOfMergedIslands();
            }

            final int max = Arrays.stream(influences).max().orElseThrow();
            int new_owner_index = -1;
            for (int i = 0; i < influences.length; i++) {
                if (influences[i] == max) {
                    if (new_owner_index == -1)
                        new_owner_index = i;
                    else return island.getOwnerIndex();
                }
            }

            if(new_owner_index == -1)
                fail("Internal error: something went wrong while computing the influence");
            return new_owner_index;
        }

        /**
         * Non-static version of {@link TestGame#computeIslandOwner}.
         */
        private Integer computeIslandOwner(final int island_index) {
            return computeIslandOwner(this.game, island_index);
        }

        /**
         * Determines the new state of the specified island as if mother nature just moved there.
         * @param game The Game where to simulate the computation.
         * @param island_index The island index of which to determinate the new state.
         * @return A dictionary containing the new island properties as entries.
         * <p>
         *     The dictionary has the following key-value pairs:
         *     <ol>
         *          <li><code>result</code> - <code>Integer</code> value specifying the changes that have been predicted:</li>
         *          <ul>
         *              <li><code>0</code> - nothing should have changed</li>
         *              <li><code>1</code> - the owner should have changed, but no merging should have occurred</li>
         *              <li><code>2</code> - the owner should have changed and at least one island should have been merged</li>
         *              <li><code>3</code> - the owner should have changed, at least one island should have been merged and
         *                                   because of it the game is now ended (islands.size() <= 3). In this case the
         *                                   other parameters of the dictionary may not be correct;</li>
         *          </ul>
         *          <li><code>owner_index</code> - <code>Integer</code> value of the computed owner (eventually <code>null</code>);</li>
         *          <li><code>students</code> - <code>int</code> array of 5 elements each indicating the number of students
         *              per color who should be on the island;</li>
         *          <li><code>num_of_merged_islands</code> - <code>Integer</code> value specifying the number of merged island;</li>
         *          <li><code>interdiction</code> - <code>boolean</code> value specifying if the interdiction is set;</li>
         *          <li><code>index</code> - <code>Integer</code> value specifying the index that the original island now has.</li>
         *     </ol>
         * </p>
         * Returns a dictionary containing only the <code>result</code> key, set to <code>-1</code>, when
         * {@link TestGame#computeIslandOwner} fails to provide the new island owner index.
         * @implNote This method simulates mother nature's movement and the eventual island merging, without actually playing,
         * assuming that moving mother nature would be the next action played.
         */
        static public Map<String, Object> disputeIsland(final Game game, final int island_index) {
            final Island island = game.getIslands().get(island_index);
            final Integer computed_owner = computeIslandOwner(game, island_index);
            if(computed_owner != null && computed_owner == -1)
                return new HashMap<>(Map.of("result", -1));

            final HashMap<String, Object> result = new HashMap<>(6);
            result.put("result", Objects.equals(computed_owner, island.getOwnerIndex()) ? 0 : 1);
            result.put("owner_index", computed_owner);
            final int[] students = new int[Colors.values().length];
            for (Colors color : Colors.values())
                students[color.index] = island.getStudents(color);
            result.put("students", students);
            result.put("num_of_merged_islands", island.getNumOfMergedIslands());
            result.put("interdiction", island.getInterdiction());
            result.put("index", island_index);
            if(computed_owner == null) return result;

            final int prev_index = island_index - 1 >= 0 ? island_index - 1 : game.getIslands().size() - 1;
            boolean prev_merged = false;
            if(Objects.equals(game.getIslands().get(prev_index).getOwnerIndex(), computed_owner)) {
                result.replace("result", 2);
                for (Colors color : Colors.values())
                    students[color.index] += game.getIslands().get(prev_index).getStudents(color);
                result.replace("num_of_merged_islands", (int) result.get("num_of_merged_islands") + game.getIslands().get(prev_index).getNumOfMergedIslands());
                result.replace("interdiction", (boolean) result.get("interdiction") || game.getIslands().get(prev_index).getInterdiction());
                result.replace("index", prev_index == game.getIslands().size() - 1 ? prev_index - 1 : prev_index);
                prev_merged = true;
                if(game.getIslands().size() - 1 <= 3) {
                    result.replace("result", 3);
                    return result;
                }
            }

            final int next_index = (island_index + 1) % game.getIslands().size();
            boolean next_merged = false;
            if(Objects.equals(game.getIslands().get(next_index).getOwnerIndex(), computed_owner)) {
                result.replace("result", 2);
                for (Colors color : Colors.values())
                    students[color.index] += game.getIslands().get(next_index).getStudents(color);
                result.replace("num_of_merged_islands", (int) result.get("num_of_merged_islands") + game.getIslands().get(next_index).getNumOfMergedIslands());
                result.replace("interdiction", (boolean) result.get("interdiction") || game.getIslands().get(next_index).getInterdiction());
                if(next_index == 0) result.replace("index", prev_index);
                next_merged = true;
            }

            if(prev_merged && next_merged && prev_index > next_index)
                result.replace("index", (int) result.get("index") - 1);
            return result;
        }

        /**
         * Constructs a {@link EffectParameters} object with the correct number and values of
         * the parameters based on the given npc game index.
         * @param game The game from where to acquire the necessary information.
         * @param npc_index The in-game npc_index for which to construct the related EffectParameters.
         * @return The properly constructed EffectParameters object.
         */
        static public EffectParameters prepareEffectParameters(final Game game, final int npc_index) {
            return switch (game.getNpcs()[npc_index].getId()) {
                case 1 -> new EffectParameters(random.nextInt(4), random.nextInt(game.getIslands().size()));
                case 3, 5 -> new EffectParameters(random.nextInt(game.getIslands().size()));
                case 7 -> {
                    final int number_of_students = random.nextInt(1, 4);
                    final List<Integer> card_student_indexes = new ArrayList<>(number_of_students);
                    for (int i = 0; i < number_of_students; i++) {
                        int random_student;
                        do random_student = random.nextInt(6);
                        while (card_student_indexes.contains(random_student));
                        card_student_indexes.add(random_student);
                    }
                    final List<Integer> entrance_student_indexes = new ArrayList<>(number_of_students);
                    for (int i = 0; i < number_of_students; i++) {
                        int random_student;
                        do random_student = random.nextInt(playingPlayer(game).getDashboard().getEntrance().size());
                        while (entrance_student_indexes.contains(random_student));
                        entrance_student_indexes.add(random_student);
                    }
                    final List<Integer> params = new ArrayList<>(1 + card_student_indexes.size() + entrance_student_indexes.size());
                    params.add(number_of_students);
                    params.addAll(card_student_indexes);
                    params.addAll(entrance_student_indexes);
                    yield new EffectParameters(params.toArray(new Integer[0]));
                }
                case 9, 12 -> new EffectParameters(random.nextInt(Colors.values().length));
                case 10 -> {
                    if(Stream.of(Colors.values()).allMatch(color -> playingPlayer(game).getDashboard().getHallRow(color) == 0))
                        yield null; //if there are no students on playing player's dashboard
                    int random_row;
                    do random_row = random.nextInt(Colors.values().length);
                    while (playingPlayer(game).getDashboard().getHallRow(random_row) == 0);
                    yield new EffectParameters(random.nextInt(playingPlayer(game).getDashboard().getEntrance().size()), random_row);
                }
                case 11 -> new EffectParameters(random.nextInt(4));
                default -> new EffectParameters(List.of());
            };
        }

        /**
         * Non-static version of {@link TestGame#prepareEffectParameters}.
         */
        public EffectParameters prepareEffectParameters(final int npc_index) {
            return prepareEffectParameters(this.game, npc_index);
        }

        /**
         * Randomly plays the moves required to reach the state specified with the parameters (phase and
         * step). It does nothing if the game is already in the specified state.
         * @param game The game where to autoplay.
         * @param phase The phase to be reached.
         * @param step The step to be reached.
         * @implNote Fails (throws an {@code AssertionFailedError} via junit) if the parameters were incorrect or when
         * a required move could not be performed due to an unexpected {@link InvalidMoveException}.
         */
        static public void autoplayUpTo(final Game game, final int phase, int step) {
            //check parameters validity
            switch (phase) {
                case 0 -> step = 0;
                case 1 -> {
                    switch (step) {
                        case 0, 1 -> {}
                        case 2 -> {
                            if(game.isLastGameTurn())
                                step = 1;
                        }
                        default -> fail("Autoplay failed: invalid step number: " + step);
                    }
                }
                default -> fail("Autoplay failed: invalid phase number: " + phase);
            }

            //cycle until the required state is reached
            while(!game.isGameEnded() && (game.getPhase() != phase || game.getStep() != step)) {
                switch (game.getPhase()) {
                    case 0: //planning phase: play a card for every player
                        do {
                            try {
                                randomlyPlayCard(game);
                            } catch (InvalidMoveException e) {
                                fail("Autoplay playCard failed: " + e.getMessage());
                            }
                        } while (game.getPhase() == 0);
                        if(game.getStep() == step) break;

                    case 1: //action phase
                        switch (game.getStep()) {
                            case 0: //move students randomly between islands and dashboard
                                do {
                                    final boolean random_boolean = random.nextBoolean();
                                    try {
                                        if (random_boolean) randomlyMoveStudentToHall(game);
                                        else randomlyMoveStudentToIsland(game);
                                    } catch (InvalidMoveException e) {
                                        fail("Autoplay " + (random_boolean ? "setStudentToHall" : "setStudentToIsland") + " failed: " + e.getMessage());
                                    }
                                } while (game.getMovedStudents() != 0);
                                if(game.getPhase() == phase && game.getStep() == step) break;

                            case 1: //move mother nature
                                try {
                                    randomlyMoveMotherNature(game);
                                } catch (InvalidMoveException e) {
                                    fail("Autoplay moveMotherNature failed: " + e.getMessage());
                                }
                                if(game.isGameEnded()) return;
                                if(game.isLastGameTurn()) break;
                                if(game.getPhase() == phase && game.getStep() == step) break;

                            case 2: //choose cloud
                                try {
                                    randomlyChooseCloud(game);
                                } catch (InvalidMoveException e) {
                                    fail("Autoplay chooseCloud failed: " + e.getMessage());
                                }
                                break;

                            default:
                                fail("Autoplay failed: unknown game state:\nphase: " + game.getPhase() + ", step: " + game.getStep());
                        }
                        break;

                    default:
                        fail("Autoplay failed: unknown game state:\nphase: " + game.getPhase() + ", step: " + game.getStep());
                }
            }
        }

        /**
         * Non-static version of {@link TestGame#autoplayUpTo}.
         */
        public void autoplayUpTo(final int phase, int step) {
            autoplayUpTo(this.game, phase, step);
        }

        /**
         * Returns an exact copy of this <code>TestGame</code>.
         * @return The copy of this <code>TestGame</code>.
         * @implNote The internal <code>Game</code> copy is created using {@link Game#copy()}.
         */
        @Override
        public TestGame clone() {
            return new TestGame(custom_game_name, designated_expert_mode, designated_num_of_players, game.copy(), List.copyOf(clientIDs));
        }

        /**
         * If not null, <code>toString</code> returns the {@link TestGame#custom_game_name}, otherwise it
         * generates a name for the test like the following:
         * <pre> {@code
         *     "players: 2 - npcs: [3, 1, 10]"
         * }</pre>
         */
        @Override
        public String toString() {
            return Objects.requireNonNullElseGet(custom_game_name,
                    () -> "players: " + designated_num_of_players +
                          " - " + (designated_expert_mode ? ("npcs: " + Arrays.stream(game.getNpcs()).map(Npc::getId).toList()) : "base mode"));
        }
    }

}
