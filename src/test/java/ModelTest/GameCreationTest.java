package ModelTest;

import Model.*;
import org.junit.jupiter.api.*;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.MethodSource;

import java.util.*;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Test class designed to test the initial state and properties of a <code>Game</code> just created.
 * <p>
 *     Specifically, it runs tests to assert:
 *     <li>The initial Game state, with the {@link GameStateTest GameStateTest} test class;</li>
 *     <li>The initial state for all players, with the {@link GameCreationTest#initializedPlayer} test;</li>
 *     <li>The dashboard for all players, with the {@link GameCreationTest#preparedDashboard} test.</li>
 * </p>
 * <p>
 *     See their documentation for the detailed list of what is checked.
 * </p>
 * <p>
 *     This test class builds the following test cases (defined within the {@link GameCreationTest#createGameTests} method):
 *     <li>A game with 2 players in base mode;</li>
 *     <li>A game with 3 players in base mode;</li>
 *     <li>A number of games until all the ncps have been included (since in game npcs are randomly chosen when
 *     the game is created by its default constructor).</li>
 * </p>
 * <p>All these test cases are passed as a parameter (therefore tested) to all tests in this class (see below).</p>
 * @implNote This class uses the {@link GameTests} class to run the tests as <code>@ParameterizedTest</code>s
 * (see its documentation for more information).
 */
@DisplayName("Game creation test")
@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
@TestClassOrder(ClassOrderer.OrderAnnotation.class)
@Order(1)
public class GameCreationTest extends GameTests {

    /**
     * {@inheritDoc}
     * @return The list of <code>TestGame</code>(s) to be tested.
     * @see GameTests
     */
    @Override
    List<TestGame> createGameTests() {
        List<TestGame> tests = new ArrayList<>();
        tests.add(new TestGame(false, 2));
        tests.add(new TestGame(false, 3));
        for (int i = 2; i <= 3; i++) {
            List<Integer> extracted = new ArrayList<>();
            while (extracted.size() < 12) {
                TestGame test;
                int counter;
                do {
                    test = new TestGame(true, i);
                    counter = 0;
                    for (int j = 0; j < 3; j++)
                        if (extracted.contains(test.game.getNpcs()[j].getId())) counter++;
                } while (counter == 3);
                for (int j = 0; j < 3; j++)
                    if (!extracted.contains(test.game.getNpcs()[j].getId())) extracted.add(test.game.getNpcs()[j].getId());
                tests.add(test);
            }
        }
        return tests;
    }

    /*
    remember: GameStateTest tests check that players, islands, npc... lists are not null and of the correct size,
    but the check that each element is not null is done inside the related test method (outside the GameStateTest class).
    Inside those methods it is checked again the not nullity of those list and/or their elements, if they are needed,
    in order to eventually generate an org.opentest4j.AssertionFailedError error instead of a java.lang one.
    */

    /**
     * Test class that checks the game initial state. Each check has a dedicated test method.
     * <p>
     *     What is checked:
     *     <li>{@link GameStateTest#mode Game mode}</li>
     *     <li>{@link GameStateTest#turnAndPhase Turn and phase}</li>
     *     <li>{@link GameStateTest#playingOrder Players list}</li>
     *     <li>{@link GameStateTest#playingOrder Playing order}</li>
     *     <li>{@link GameStateTest#coins Coins distribution}</li>
     *     <li>{@link GameStateTest#clouds Clouds configuration}</li>
     *     <li>{@link GameStateTest#students students distribution}</li>
     *     <li>{@link GameStateTest#motherNature Mather Nature position}</li>
     *     <li>{@link GameStateTest#professors Professor list}</li>
     *     <li>{@link GameStateTest#islands Islands list}</li>
     *     <li>{@link GameStateTest#npcs Npcs parameters}</li>
     * </p>
     * See their documentations for the detailed list of what is checked.
     */
    @Nested
    @DisplayName("Game state test")
    @TestClassOrder(ClassOrderer.OrderAnnotation.class)
    @Order(2)
    public class GameStateTest {

        /**
         * Asserts that the designated game mode has been set correctly.
         */
        @DisplayName("Game mode")
        @ParameterizedTest(name = "{0}")
        @MethodSource(arguments_supplier)
        void mode(final TestGame game_test) {
            assertEquals(game_test.designated_expert_mode, game_test.game.expert_mode, "Incorrect game mode");
        }

        /**
         * Asserts the correctness of parameters:
         * <li>turn</li>
         * <li>phase</li>
         * <li>step</li>
         * <li>game_ended</li>
         * <li>last_turn</li>
         */
        @DisplayName("Turn and phase")
        @ParameterizedTest(name = "{0}")
        @MethodSource(arguments_supplier)
        void turnAndPhase(final TestGame game_test) {
            final Game game = game_test.game;
            assertEquals(1, game.getGameTurn(), "Game is not at turn 1");
            assertTrue(game.getPlayerTurn() == 0 || game.getPlayerTurn() == 1 || game.getPlayerTurn() == 2,
                    "Wrong player_turn");
            assertEquals(0, game.getStep(), "Wrong Initial step");
            assertEquals(0, game.getPhase(), "Wrong Initial phase");
            assertFalse(game.isGameEnded(), "Game is marked as ended");
            assertFalse(game.isLastGameTurn(), "The initial turn is marked as the last one");
        }

        /**
         * Asserts that the initial players turn order is identical to the provided clientIDs list and that the currently playing
         * player matches one of the provided clientIDs.
         */
        @DisplayName("Playing order")
        @ParameterizedTest(name = "{0}")
        @MethodSource(arguments_supplier)
        void playingOrder(final TestGame game_test) {
            final Game game = game_test.game;
            assertTrue(game_test.clientIDs.contains(game.currentlyPlayingPlayer()),
                    "Currently playing player clientID does not match any real clientID");
            assertNotNull(game.currentPlayersTurnOrder(), "Players turn order list is null");
            assertEquals(game_test.clientIDs, game.currentPlayersTurnOrder(),
                    "Players turn order list does not match actual clientIDs list");
        }

        /**
         * Asserts that the players list is not null and that its size matches the designated one.
         */
        @DisplayName("Players list")
        @ParameterizedTest(name = "{0}")
        @MethodSource(arguments_supplier)
        void players(final TestGame game_test) {
            assertNotNull(game_test.game.getPlayers(), "List of players is null");
            assertEquals(game_test.designated_num_of_players, game_test.game.getPlayers().size(), "Incorrect number of players");
        }

        /**
         * Asserts that the bank has the correct amount of coins based on the game's specifics and that the total amount
         * of coins in game does not exceed 20.
         */
        @DisplayName("Coins distribution")
        @ParameterizedTest(name = "{0}")
        @MethodSource(arguments_supplier)
        void coins(final TestGame game_test) {
            final Game game = game_test.game;
            assertEquals(
                    game_test.designated_expert_mode ? (game_test.designated_num_of_players == 2 ? 18 : 17) : 0,
                    game.getBank(),
                    "Bank does not have the correct amount of coins");
            final int coins_sum = game.getPlayers().stream().mapToInt(Player::getCoins).sum();
            assertEquals(game_test.designated_expert_mode ? 20 : 0, game.getBank() + coins_sum, "Incorrect total number of coins");
        }

        /**
         * Asserts that:
         * <li>Clouds list is not null</li>
         * <li>There is the expected number of clouds</li>
         * <li>Each Cloud is not null</li>
         * <li>Each cloud has the appropriate number of students</li>
         */
        @DisplayName("Clouds configuration")
        @ParameterizedTest(name = "{0}")
        @MethodSource(arguments_supplier)
        void clouds(final TestGame game_test) {
            final Game game = game_test.game;
            assertNotNull(game.getClouds(), "Clouds list is null");
            assertEquals(3, game.getClouds().length, "Wrong size of clouds list");
            for(int i = 0; i < 2; i++)
                assertNotNull(game.getClouds()[i], "Cloud #" + i + " is null");
            if(game_test.designated_num_of_players == 2)
                assertNull(game.getClouds()[2], "Cloud #2 is not null while playing with 2 players");
            else
                assertNotNull(game.getClouds()[2], "Cloud #2 is null while playing with 3 players");
            final int expected_players_per_island = game_test.designated_num_of_players == 2 ? 3 : 4;
            for(int i = 0; i < (game_test.designated_num_of_players == 2 ? 2 : 3); i++)
                assertEquals(expected_players_per_island, game.getClouds()[i].size(),
                        "Wrong number of students on cloud #" + i);
        }

        /**
         * Asserts that students are correctly distributed between those placed and those available.
         */
        @DisplayName("Moved students")
        @ParameterizedTest(name = "{0}")
        @MethodSource(arguments_supplier)
        void students(final TestGame game_test) {
            if(game_test.designated_expert_mode) {
                final int npcs_students = Arrays.stream(game_test.game.getNpcs())
                        .filter(npc -> npc.getExtraProperty() != null)
                        .filter(npc -> npc.getId() != 5)
                        .mapToInt(npc -> npc.getExtraProperty().size())
                        .sum();
                assertEquals(
                        (game_test.designated_num_of_players == 2 ? 100 : 81) - npcs_students,
                        game_test.game.getRemainingStudentsNum(), "Wrong available number of students");
            } else
                assertEquals(game_test.designated_num_of_players == 2 ? 100 : 81, game_test.game.getRemainingStudentsNum(), "Wrong available number of students");
            assertEquals(0, game_test.game.getMovedStudents(), "There are moved students");
        }

        /**
         * Asserts that mother nature is correctly placed.
         */
        @DisplayName("Mother Nature position")
        @ParameterizedTest(name = "{0}")
        @MethodSource(arguments_supplier)
        void motherNature(final TestGame game_test) {
            final int mother_nature = game_test.game.getMotherNature();
            assertTrue(0 <= mother_nature && mother_nature <= 12, "Mother Nature is not on any of the 12 islands");
        }

        /**
         * Asserts that all the unclaimed professors list is not null and contains all the professors.
         */
        @DisplayName("Professors list")
        @ParameterizedTest(name = "{0}")
        @MethodSource(arguments_supplier)
        void professors(final TestGame game_test) {
            final Game game = game_test.game;
            assertNotNull(game.getUnclaimedProfessors(), "Unclaimed_professors is null");
            assertEquals(Colors.values().length, game.getUnclaimedProfessors().size(), "Unclaimed_professors wrongly initialized");
            assertTrue(game.getUnclaimedProfessors().containsAll(List.of(Colors.values())), "Some professor are already claimed");
        }

        /**
         * Asserts that the island list is not null and contains all the islands.
         */
        @DisplayName("Islands list")
        @ParameterizedTest(name = "{0}")
        @MethodSource(arguments_supplier)
        void islands(final TestGame game_test) {
            assertNotNull(game_test.game.getIslands(), "Islands list is null");
            assertEquals(12, game_test.game.getIslands().size(), "Wrong number of islands");
        }

        /**
         * Asserts that:
         * <li>no npc is currently activated</li>
         * <li>the npc list is null or not null based on game mode</li>
         * <li>if in expert mode, the npcs list contains 3 npcs</li>
         * <li>if in expert mode, each npc's properties is correctly initialized</li>
         */
        @DisplayName("Npcs parameters")
        @ParameterizedTest(name = "{0}")
        @MethodSource(arguments_supplier)
        void npcs(final TestGame game_test) {
            final Game game = game_test.game;
            assertEquals(0, game.getNpcEffect(), "Npcs effect is not 0");
            if (game_test.designated_expert_mode) {
                assertNotNull(game.getNpcs(), "Npcs IDs list is null");
                assertEquals(3, game.getNpcs().length, "Wrong number of npcs");
                for (Npc npc : game.getNpcs()) {
                    assertNotNull(npc.toString(), "Description of npc " + npc.getId() + " is empty");
                    final int expected_args = switch (npc.getId()) {
                        case 1, 10 -> 2;
                        case 3, 5, 9, 11, 12 -> 1;
                        case 7 -> 3;
                        default -> 0;
                    };
                    assertEquals(expected_args, npc.getArgsNum(), "Wrong returned number of arguments for npc " + npc.getId());
                    final Integer expected_length = switch (npc.getId()) {
                        case 1, 11 -> 4;
                        case 5 -> 1;
                        case 7 -> 6;
                        case 9 -> 0;
                        default -> null;
                    };
                    if(expected_length == null)
                        assertNull(npc.getExtraProperty(), "Extra property list of npc " + npc.getId() + " is not null");
                    else {
                        assertNotNull(npc.getExtraProperty(), "Extra property list of npc " + npc.getId() + " is null");
                        assertEquals(expected_length, npc.getExtraProperty().size(), "Extra property list of npc " + npc.getId() + " is wrong sized");
                    }
                }
            } else
                assertNull(game.getNpcs(), "Npcs list is not null while in base mode");
        }
    }

    /**
     * Checks the initial configuration of every island.
     * <p>
     *     What is checked:
     *     <li>Owner index</li>
     *     <li>Coherence between methods result</li>
     *     <li>Merged islands</li>
     *     <li>Interdiction</li>
     *     <li>Distribution of students</li>
     * </p>
     */
    @DisplayName("Island preparation")
    @ParameterizedTest(name = "{0}")
    @MethodSource(arguments_supplier)
    void initializedIslands(final TestGame game_test) {
        assertNotNull(game_test.game.getIslands(), "Islands list is null");
        final List<Island> islands = game_test.game.getIslands();
        //every island is checked
        for (int i = 0; i < islands.size(); i++) {
            final Island island = islands.get(i);
            assertNotNull(island, "Island #" + i + " is null");
            assertNull(island.getOwnerIndex(), "owner_index is not null for island #" + i);
            assertEquals(1, island.getNumOfMergedIslands(), "Wrong number of merged islands for island #" + i);
            assertFalse(island.getInterdiction(), "interdiction is not false for island #" + i);
            //number of students
            int sum = 0;
            for (int j = 0; j < Colors.values().length; j++) {
                //noinspection SimplifiableAssertion
                assertTrue(island.getStudents(j) == island.getStudents(Colors.fromColorIndex(j)),
                        "Incongruent " + Colors.fromColorIndex(j) + " students distribution in island #" + i + " when obtained by Color and by index");
                sum += island.getStudents(j);
            }
            final int mother_nature = game_test.game.getMotherNature();
            if((mother_nature + 6) % 12 == i || mother_nature == i)
                assertEquals(0, sum, "Island #" + i + (mother_nature == i ? " with" : " opposite to" ) + " mother nature has students");
            else
                assertEquals(1, sum, "Wrong number of students in island #" + i);
        }
    }

    /**
     * Checks the initial configuration of every player.
     * <p>
     *     What is checked:
     *     <li>ClientID</li>
     *     <li>Index</li>
     *     <li>Dashboard not null</li>
     *     <li>Cards</li>
     *     <li>Coins</li>
     * </p>
     */
    @DisplayName("Player preparation")
    @ParameterizedTest(name = "{0}")
    @MethodSource(arguments_supplier)
    void initializedPlayer(final TestGame game_test) {
        final List<Card> cards = new ArrayList<>(List.of(
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

        assertNotNull(game_test.game.getPlayers(), "Players list is null");
        for(int i = 0; i < game_test.game.getPlayers().size(); i++) {
            final Player player = game_test.game.getPlayers().get(i);
            assertEquals(game_test.clientIDs.get(i), player.clientID);
            assertEquals(i, player.player_index, "Wrong player index for player #" + player.player_index);
            assertNotNull(player.getDashboard(), "Dashboard for player #" + player.player_index + " is null");
            assertNotNull(player.getCards(), "Payer #" + player.player_index + " does not have cards");
            assertEquals(cards, player.getCards(), "player #" + player.player_index + " has wrong cards");
            assertNull(player.getLastCardPlayed(), "Last played card for player #" + player.player_index + " is not null");
            assertEquals(game_test.designated_expert_mode ? 1 : 0, player.getCoins(), "Coins for player #" + player.player_index + " is not 1");
        }
    }

    /**
     * Checks the initial configuration for the dashboard for all players.
     * <p>
     *     What is checked:
     *     <li>Number of students in the entrance</li>
     *     <li>Professors</li>
     *     <li>Halls</li>
     *     <li>Rooks for dashboard</li>
     *     <li>Coherence between methods result</li>
     * </p>
     */
    @DisplayName("Dashboard preparation")
    @ParameterizedTest(name = "{0}")
    @MethodSource(arguments_supplier)
    void preparedDashboard(final TestGame game_test) {
        //define how to Dashboard should be based on the configuration of the Game
        final int entrance_exp = game_test.designated_num_of_players == 2 ? 7 : 9;
        final int rooks_exp = game_test.designated_num_of_players == 2 ? 8 : 6;

        //every player's dashboard is checked
        assertNotNull(game_test.game.getPlayers(), "Players list is null");
        for(final Player player : game_test.game.getPlayers()) {
            assertNotNull(player, "A player is null");
            assertNotNull(player.getDashboard(), "Dashboard of player #" + player.player_index + " is null");
            final Dashboard dashboard = player.getDashboard();
            assertEquals(entrance_exp, dashboard.getEntrance().size(),
                    "Wrong number of students in the entrance for player #" + player.player_index);
            for (int i = 0; i < 5; i++) {
                final int num = dashboard.getHallRow(i);
                //noinspection SimplifiableAssertion
                assertTrue(num == dashboard.getHallRow(Colors.fromColorIndex(i)),
                        "Incongruent number of students in hallRow " + i + " when called by index and by color");
                assertEquals(0, num,
                        "Player #" + player.player_index + " already got " + num + " " + Colors.fromColorIndex(i) + " student" + (num > 1 ? "s" : ""));
            }
            for (int i = 0; i < Colors.values().length; i++) {
                //noinspection SimplifiableAssertion
                assertTrue(dashboard.getProfessor(i) == dashboard.getProfessor(Colors.fromColorIndex(i)),
                        "Incongruent professor " + Colors.fromColorIndex(i) + " presence");
                assertFalse(dashboard.getProfessor(i),
                        "Player #" + player.player_index + " already got professor " + Colors.fromColorIndex(i));
            }
            assertEquals(rooks_exp, dashboard.getRooks(),"Wrong number of rooks for player #" + player.player_index);
        }
    }
}
