package ModelTest;

import Exceptions.InvalidMoveException;
import Model.*;
import org.junit.jupiter.api.*;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.MethodSource;

import java.util.*;
import java.util.stream.IntStream;
import java.util.stream.Stream;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Test class designed to assert the correctness of the game moves and mechanisms.
 * <p>
 *      All its tests are divided in 3 test classes:
 *      <li>{@link Phase0Tests Phase0Tests}: tests related to the available moves for phase 0;</li>
 *      <li>{@link Phase1Tests Phase1Tests}: tests related to the available moves for phase 1 and game ending and winner tests;</li>
 *      <li>{@link TransversalTests TransversalTests}: tests related to the general game state.</li>
 * </p>
 * <p>
 *     All tests play the passed game until it ends (or until it is possible), checking the related assertions in every turn.
 *     See their documentation for the detailed list of what is checked.
 * </p>
 * <p>
 *     This test class builds the following test cases (defined within the
 *     {@link GameCreationTest#createGameTests createGameTests} method), created to test both standard and edge conditions:
 *     <li>A game with 2 players in base mode;</li>
 *     <li>A game with 3 players in base mode;</li>
 *     <li>24 games specifically created in order to include all the possible npcs, randomly grouped, for both games with 2 and 3 players;</li>
 *     <li>2 games specifically constructed to test the condition where all players have the same 1 card left in their decks,
 *          for both games with 2 and 3 players;</li>
 *     <li>A game with 3 players specifically constructed to test the condition where all players having the same 2 card left in their decks;</li>
 *     <li>A game to test the condition where the match ends because there are three or less groups of islands;</li>
 *     <li>A game to test the condition where the match ends because a player has placed its last rook;</li>
 *     <li>A game to test the condition where the match ends with all players having the same number of towers (tie condition);</li>
 *     <li>A game to test the condition where the match ends with all players having the same number of towers (tie condition)
 *          and the same number of professors (double tie condition);</li>
 *     <li>A number of standard random generated games. The quantity is defined with the {@link GameActionsTest#num_of_random_games} field.</li>
 *     <p>
 *     Specific games are created using the test dedicated {@link Game#Game(boolean, List, List, List, int, List, Npc[], List) Game(...)},
 *     {@link Dashboard#Dashboard(List, int, int[], boolean[]) Dashboard(...)}, {@link Player#Player(int, Dashboard, int, int, int, List) Player(...)},
 *     {@link Island#Island(int[], Integer, int, boolean) Island(...)} and {@link NpcFactory#factoryTestMethod(List, int) NpcFactory.factoryTestMethod(...)}
 *     constructors.
 *     </p>
 * </p>
 * <p>All these test cases are passed as a parameter (therefore tested) to all tests in this class (see below).</p>
 * @implNote This class uses the {@link GameTests} class to run all its tests as <code>@ParameterizedTest</code>s
 * (see its documentation for more information).
 */
@DisplayName("Actions test")
@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
@TestClassOrder(ClassOrderer.OrderAnnotation.class)
@Order(3)
public class GameActionsTest extends GameTests {

    final static int num_of_random_games = 50;

    //TODO: documentare o rimuovere
    protected interface GameMoves {
        void exec(Game game) throws InvalidMoveException;
    }

    //TODO: documentare o rimuovere
    TestGame buildDesignatedGame(String test_name, List<Colors> students, List<Colors> islands_students, List<Integer> clientIDs, int MN_position, int[] npcs, GameMoves moves) {

        //prepare islands
        List<Island> islands = new ArrayList<>();
        for(int i = 0; i < 12; i++)
            islands.add(new Island(
                    i == (MN_position + 6) % 12 || i == MN_position
                            ? null : islands_students.remove(0), i));
        //extract npcs
        Npc[] current_npcs = new Npc[3];
        current_npcs[0] = NpcFactory.factoryTestMethod(students, npcs[0]);
        current_npcs[1] = NpcFactory.factoryTestMethod(students, npcs[1]);
        current_npcs[2] = NpcFactory.factoryTestMethod(students, npcs[2]);
        //build players
        List<Player> players = new ArrayList<>(clientIDs.size());
        for(int i = 0; i < clientIDs.size(); i++) {
            final List<Colors> temp_students = new ArrayList<>();
            for (int j = 0; j < (clientIDs.size() == 2 ? 7 : 9); j++)
                temp_students.add(students.remove(0));
            players.add(new Player(clientIDs.get(i), temp_students, clientIDs.size(), i, 1));
        }
        //construct game
        Game game = new Game(true, players, clientIDs, students, MN_position, islands, current_npcs, new ArrayList<>(Arrays.asList(Colors.values())));
        try {
            moves.exec(game);
        } catch (InvalidMoveException e) {
            fail("internal error: cannot prebuild game: " + e.getMessage());
        }
        return new TestGame(test_name, true, clientIDs.size(), game, clientIDs);
    }

    /**
     * {@inheritDoc}
     * @return The list of <code>TestGame</code>(s) to be tested.
     * @see GameTests
     */
    @Override
    List<TestGame> createGameTests() {
        List<TestGame> tests = new ArrayList<>();

        List<Card> cards =  new ArrayList<> (List.of(
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

        //add both a game with 2 and a game with 3 players in base mode
        tests.add(new TestGame(false, 2));
        tests.add(new TestGame(false, 3));
        //construct games for both with 2 and 3 players...
        for (int i = 2; i <= 3; i++) {
            final List<Integer> npcs = new ArrayList<>(List.of(1, 2, 3, 4, 5, 6, 7, 8, 9, 10, 11, 12));
            Collections.shuffle(npcs);
            while (npcs.size() > 0) {//...with all npcs
                //construct random students list
                final List<Colors> random_students = new ArrayList<>();
                for(int k = 0; k < 5; k++) {
                    for(int j = 0; j < 24; j++) {
                        random_students.add(Colors.fromColorIndex(k));
                    }
                }
                Collections.shuffle(random_students);
                //build randomized client ids list
                final List<Integer> clientIDs = TestGame.buildRandomizedClientIDsList(i);
                //randomly choose mother nature position
                final int mother_nation_position = random.nextInt(12); //12 == number of initial islands
                //prepare random students pool from where to extract the students to be placed on islands
                List<Colors> temp = new ArrayList<>();
                for(int k = 0; k < 5; k++)
                    for (int j = 0; j < 2; j++)
                        temp.add(Colors.fromColorIndex(k));
                Collections.shuffle(temp);
                //prepare islands
                List<Island> islands = new ArrayList<>();
                for(int k = 0; k < 12; k++)
                    islands.add(new Island(
                            k == (mother_nation_position + 6) % 12 || k == mother_nation_position
                            ? null : temp.remove(0), k));
                //extract npcs
                final Npc[] current_npcs = new Npc[3];
                current_npcs[0] = NpcFactory.factoryTestMethod(random_students, npcs.remove(0));
                for (int j = 1; j <= 2; j++) {
                    int random_npc;
                    do random_npc = random.nextInt(12);
                    while (random_npc == current_npcs[0].getId() || (j == 2 && random_npc == current_npcs[1].getId()));
                    current_npcs[j] = NpcFactory.factoryTestMethod(random_students, random_npc);
                }
                //build players
                final List<Player> players = new ArrayList<>(i);
                for(int k = 0; k < clientIDs.size(); k++) {
                    final List<Colors> temp_students = new ArrayList<>();
                    for (int l = 0; l < (i == 2 ? 7 : 9); l++)
                        temp_students.add(random_students.remove(0));
                    players.add(new Player(clientIDs.get(k), temp_students, i, k, 10)); //give each players 10 coins to ensure all npcs can be activated to be tested
                }
                //construct game
                final Game game = new Game(true, players, clientIDs, random_students, mother_nation_position, islands, current_npcs, new ArrayList<>(Arrays.asList(Colors.values())));
                //wrap game with TestGame
                tests.add(new TestGame(true, i, game, clientIDs));
            }

            //construct random students list
            List<Colors> random_students = new ArrayList<>();
            for(int k = 0; k < 5; k++) {
                for(int j = 0; j < 24; j++) {
                    random_students.add(Colors.fromColorIndex(k));
                }
            }
            Collections.shuffle(random_students);
            //build randomized client ids list
            List<Integer> clientIDs = TestGame.buildRandomizedClientIDsList(i);
            //randomly choose mother nature position
            int mother_nation_position = random.nextInt(12); //12 == number of initial islands
            //prepare random students pool from where to extract the students to be placed on islands
            List<Colors> temp = new ArrayList<>();
            for(int k = 0; k < 5; k++)
                for (int j = 0; j < 2; j++)
                    temp.add(Colors.fromColorIndex(k));
            Collections.shuffle(temp);
            //prepare islands
            List<Island> islands = new ArrayList<>();
            for(int k = 0; k < 12; k++)
                islands.add(new Island(
                        k == (mother_nation_position + 6) % 12 || k == mother_nation_position
                                ? null : temp.remove(0), k));
            //extract npcs
            Npc[] current_npcs = new Npc[3];
            for (int j = 0; j < 3; j++) {
                int random_npc;
                do random_npc = random.nextInt(0, 12);
                while ((j != 0 && random_npc == current_npcs[0].getId()) || (j == 2 && random_npc == current_npcs[1].getId()));
                current_npcs[j] = NpcFactory.factoryTestMethod(random_students, random_npc);
            }
            //build players
            List<Player> players = new ArrayList<>(i);
            for(int k = 0; k < clientIDs.size(); k++) {
                final List<Colors> temp_students = new ArrayList<>();
                for (int l = 0; l < (i == 2 ? 7 : 9); l++)
                    temp_students.add(random_students.remove(0));
                players.add(new Player(clientIDs.get(k), temp_students, i, k, 1));
            }
            //construct game
            Game game = new Game(true, players, clientIDs, random_students, mother_nation_position, islands, current_npcs, new ArrayList<>(Arrays.asList(Colors.values())));
            //wrap game with TestGame
            tests.add(new TestGame(i + " players, same last card", true, i, game, clientIDs));
        }

        /*--- 3 players, 2 cards all different ---*/
        //construct random students list
        List<Colors> random_students = new ArrayList<>();
        for(int k = 0; k < 5; k++) {
            for(int j = 0; j < 24; j++) {
                random_students.add(Colors.fromColorIndex(k));
            }
        }
        Collections.shuffle(random_students);
        //build randomized client ids list
        List <Integer> clientIDs = TestGame.buildRandomizedClientIDsList(3);
        //randomly choose mother nature position
        int mother_nation_position = random.nextInt(12); //12 == number of initial islands
        //prepare random students pool from where to extract the students to be placed on islands
        List<Colors> temp = new ArrayList<>();
        for(int k = 0; k < 5; k++)
            for (int j = 0; j < 2; j++)
                temp.add(Colors.fromColorIndex(k));
        Collections.shuffle(temp);
        //prepare islands
        List<Island> islands = new ArrayList<>();
        for(int k = 0; k < 12; k++)
            islands.add(new Island(
                    k == (mother_nation_position + 6) % 12 || k == mother_nation_position
                            ? null : temp.remove(0), k));
        //extract npcs
        Npc[] current_npcs = new Npc[3];
        for (int j = 0; j < 3; j++) {
            int random_npc;
            do random_npc = random.nextInt(0, 12);
            while ((j != 0 && random_npc == current_npcs[0].getId()) || (j == 2 && random_npc == current_npcs[1].getId()));
            current_npcs[j] = NpcFactory.factoryTestMethod(random_students, random_npc);
        }
        //cards single use
        List<Card> cards_single_use =  new ArrayList<> (List.of(
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
        Collections.shuffle(cards_single_use);
        //build players
        List<Player> players = new ArrayList<>(3);
        for(int k = 0; k < clientIDs.size(); k++) {
            final List<Colors> temp_students = new ArrayList<>();
            for (int l = 0; l < 9; l++)
                temp_students.add(random_students.remove(0));
            players.add(new Player(clientIDs.get(k), new Dashboard(temp_students, 6, new int[] {0, 0, 0, 0, 0}, new boolean[]{false, false, false, false, false}), 3, k, 1, new ArrayList<>(List.of(cards_single_use.remove(0), cards_single_use.remove(0)))));
        }
        //construct game
        Game game = new Game(true, players, clientIDs, random_students, mother_nation_position, islands, current_npcs, new ArrayList<>(Arrays.asList(Colors.values())));
        //wrap game with TestGame
        tests.add(new TestGame("3 players, 2 cards all different", true, 3, game, clientIDs));

        /*--- 3 players, same 2 last cards ---*/
        //construct random students list
        random_students = new ArrayList<>();
        for(int k = 0; k < 5; k++) {
            for(int j = 0; j < 24; j++) {
                random_students.add(Colors.fromColorIndex(k));
            }
        }
        Collections.shuffle(random_students);
        //build randomized client ids list
        clientIDs = TestGame.buildRandomizedClientIDsList(3);
        //randomly choose mother nature position
        mother_nation_position = random.nextInt(12); //12 == number of initial islands
        //prepare random students pool from where to extract the students to be placed on islands
        temp = new ArrayList<>();
        for(int k = 0; k < 5; k++)
            for (int j = 0; j < 2; j++)
                temp.add(Colors.fromColorIndex(k));
        Collections.shuffle(temp);
        //prepare islands
        islands = new ArrayList<>();
        for(int k = 0; k < 12; k++)
            islands.add(new Island(
                    k == (mother_nation_position + 6) % 12 || k == mother_nation_position
                            ? null : temp.remove(0), k));
        //extract npcs
        current_npcs = new Npc[3];
        for (int j = 0; j < 3; j++) {
            int random_npc;
            do random_npc = random.nextInt(0, 12);
            while ((j != 0 && random_npc == current_npcs[0].getId()) || (j == 2 && random_npc == current_npcs[1].getId()));
            current_npcs[j] = NpcFactory.factoryTestMethod(random_students, random_npc);
        }
        //build players
        players = new ArrayList<>(3);
        int random_card_index_1 = random.nextInt(0, cards.size());
        final Card random_card_1 = cards.get(random_card_index_1);
        int random_card_index_2;
        do random_card_index_2 = random.nextInt(0, cards.size());
        while (random_card_index_2 == random_card_index_1);
        final Card random_card_2 = cards.get(random_card_index_2);
        for(int k = 0; k < clientIDs.size(); k++) {
            final List<Colors> temp_students = new ArrayList<>();
            for (int l = 0; l < 9; l++)
                temp_students.add(random_students.remove(0));
            players.add(new Player(clientIDs.get(k), new Dashboard(temp_students, 6, new int[] {0, 0, 0, 0, 0}, new boolean[]{false, false, false, false, false}), 3, k, 1,
                            new ArrayList<>(List.of(
                                    new Card(random_card_1.order_value, random_card_1.movements_value),
                                    new Card(random_card_2.order_value, random_card_2.movements_value)))
                    )
            );
        }
        //construct game
        game = new Game(true, players, clientIDs, random_students, mother_nation_position, islands, current_npcs,new ArrayList<>(Arrays.asList(Colors.values())));
        //wrap game with TestGame
        tests.add(new TestGame("3 players, same 2 last cards", true, 3, game, clientIDs));

        /*--- 2 players, 3 islands ending ---*/
        //construct random students list
        random_students = new ArrayList<>();
        for(int k = 0; k < 5; k++) {
            for(int j = 0; j < 24; j++) {
                random_students.add(Colors.fromColorIndex(k));
            }
        }
        Collections.shuffle(random_students);
        //build randomized client ids list
        clientIDs = TestGame.buildRandomizedClientIDsList(2);
        //randomly choose mother nature position
        mother_nation_position = 0;
        //prepare islands
        islands = new ArrayList<>();
        islands.add(new Island(new int[] {1, 1, 0, 1, 1}, 0, 3, false));
        islands.add(new Island(new int[] {1, 1, 0, 1, 1}, null, 1, false));
        islands.add(new Island(new int[] {1, 1, 0, 1, 1}, 0, 3, false));
        islands.add(new Island(new int[] {0, 0, 1, 0, 0}, 1, 5, false));
        //extract npcs
        current_npcs = new Npc[3];
        for (int j = 0; j < 3; j++) {
            int random_npc;
            do random_npc = random.nextInt(0, 12);
            while ((j != 0 && random_npc == current_npcs[0].getId()) || (j == 2 && random_npc == current_npcs[1].getId()));
            current_npcs[j] = NpcFactory.factoryTestMethod(random_students, random_npc);
        }
        //build players
        players = new ArrayList<>(2);
        for(int k = 0; k < clientIDs.size(); k++) {
            final List<Colors> temp_students = new ArrayList<>();
            for (int l = 0; l < 7; l++)
                temp_students.add(random_students.remove(0));
            players.add(new Player(clientIDs.get(k), new Dashboard(temp_students, k== 0 ? 2 : 3, k == 0 ? new int[] {4, 4, 0, 4, 4} : new int[] {0, 0, 1, 0, 0}, k == 0 ? new boolean[] {true, true, false, true, true} : new boolean[]{false, false, true, false, false}), 2, k, 1, new ArrayList<>(k == 0 ? List.of(new Card(1, 1), new Card(2, 1)) : List.of(new Card(3, 2), new Card(4, 2)))));
        }
        //construct game
        game = new Game(true, players, clientIDs, random_students, mother_nation_position, islands, current_npcs, new ArrayList<>(0));
        //wrap game with TestGame
        tests.add(new TestGame("2 players, 3 islands ending", true, 2, game, clientIDs));

        /*--- 2 players, towers finished ending ---*/
        //construct random students list
        random_students = new ArrayList<>();
        for(int k = 0; k < 5; k++) {
            for(int j = 0; j < 24; j++) {
                random_students.add(Colors.fromColorIndex(k));
            }
        }
        Collections.shuffle(random_students);
        //build randomized client ids list
        clientIDs = TestGame.buildRandomizedClientIDsList(2);
        //prepare islands
        islands = new ArrayList<>();
        islands.add(new Island(new int[] {1, 1, 0, 1, 1}, 0, 3, false));
        islands.add(new Island(new int[] {0, 0, 5, 0, 0}, null, 1, false));
        islands.add(new Island(new int[] {1, 1, 0, 1, 1}, 0, 2, false));
        islands.add(new Island(new int[] {0, 0, 1, 0, 0}, 1, 5, false));
        //extract npcs
        current_npcs = new Npc[3];
        for (int j = 0; j < 3; j++) {
            int random_npc;
            do random_npc = random.nextInt(0, 12);
            while ((j != 0 && random_npc == current_npcs[0].getId()) || (j == 2 && random_npc == current_npcs[1].getId()));
            current_npcs[j] = NpcFactory.factoryTestMethod(random_students, random_npc);
        }
        //build players
        players = new ArrayList<>(2);
        for(int k = 0; k < clientIDs.size(); k++) {
            final List<Colors> temp_students = new ArrayList<>();
            for (int l = 0; l < 7; l++)
                temp_students.add(random_students.remove(0));
            players.add(new Player(clientIDs.get(k), new Dashboard(temp_students, 1, k == 0 ? new int[] {4, 4, 0, 4, 4} : new int[] {0, 0, 1, 0, 0}, k == 0 ? new boolean[] {true, true, false, true, true} : new boolean[]{false, false, true, false, false}), 2, k, 1, new ArrayList<>(k != 0 ? List.of(new Card(1, 1), new Card(2, 1)) : List.of(new Card(3, 2), new Card(4, 2)))));
        }
        //construct game
        game = new Game(true, players, clientIDs, random_students, mother_nation_position, islands, current_npcs, new ArrayList<>(0));
        //wrap game with TestGame
        tests.add(new TestGame("2 players, towers finished ending", true, 2, game, clientIDs));

        /*--- towers tie ---*/
        List<Colors> students = new ArrayList<>(Arrays.asList(
                Colors.GREEN, Colors.RED, Colors.MAGENTA, Colors.MAGENTA, Colors.GREEN, Colors.RED, Colors.RED,
                Colors.RED, Colors.BLUE, Colors.MAGENTA, Colors.MAGENTA, Colors.GREEN, Colors.BLUE, Colors.GREEN,
                Colors.YELLOW, Colors.RED, Colors.GREEN, Colors.MAGENTA, Colors.RED, Colors.RED, Colors.BLUE,
                Colors.BLUE, Colors.MAGENTA, Colors.YELLOW, Colors.YELLOW, Colors.GREEN, Colors.GREEN, Colors.BLUE,
                Colors.BLUE, Colors.RED, Colors.MAGENTA, Colors.YELLOW, Colors.GREEN, Colors.GREEN, Colors.RED,
                Colors.YELLOW, Colors.BLUE, Colors.RED, Colors.GREEN, Colors.YELLOW, Colors.RED, Colors.RED,
                Colors.MAGENTA, Colors.MAGENTA, Colors.GREEN, Colors.RED, Colors.MAGENTA, Colors.RED, Colors.RED,
                Colors.MAGENTA, Colors.RED, Colors.BLUE, Colors.RED, Colors.GREEN, Colors.BLUE, Colors.MAGENTA,
                Colors.MAGENTA, Colors.YELLOW, Colors.GREEN, Colors.YELLOW, Colors.YELLOW, Colors.YELLOW, Colors.BLUE,
                Colors.GREEN, Colors.BLUE, Colors.GREEN, Colors.GREEN, Colors.BLUE, Colors.GREEN, Colors.YELLOW,
                Colors.RED, Colors.BLUE, Colors.BLUE, Colors.MAGENTA, Colors.YELLOW, Colors.BLUE, Colors.RED,
                Colors.MAGENTA, Colors.YELLOW, Colors.GREEN, Colors.YELLOW, Colors.BLUE, Colors.BLUE, Colors.YELLOW,
                Colors.YELLOW, Colors.RED, Colors.MAGENTA, Colors.MAGENTA, Colors.MAGENTA, Colors.BLUE, Colors.BLUE,
                Colors.GREEN, Colors.RED, Colors.YELLOW, Colors.MAGENTA, Colors.MAGENTA, Colors.BLUE, Colors.GREEN,
                Colors.YELLOW, Colors.MAGENTA, Colors.BLUE, Colors.RED, Colors.BLUE, Colors.RED, Colors.YELLOW,
                Colors.GREEN, Colors.GREEN, Colors.MAGENTA, Colors.BLUE, Colors.YELLOW, Colors.GREEN, Colors.GREEN,
                Colors.YELLOW, Colors.YELLOW, Colors.YELLOW, Colors.BLUE, Colors.MAGENTA, Colors.MAGENTA, Colors.RED,
                Colors.YELLOW
        ));
        List<Colors> islands_students = new ArrayList<>(Arrays.asList(
                Colors.GREEN, Colors.YELLOW, Colors.RED, Colors.YELLOW, Colors.BLUE, Colors.BLUE, Colors.MAGENTA,
                Colors.MAGENTA, Colors.RED, Colors.GREEN
        ));
        GameMoves moves = (g) -> {
            g.playCard(-1494447874, 5);
            g.playCard(-1534994648, 1);
            g.playCard(1232297098, 9);
            g.setStudentToHall(-1534994648, 4);
            g.setStudentToHall(-1534994648, 2);
            g.setStudentToHall(-1534994648, 0);
            g.setStudentToHall(-1534994648, 0);
            g.moveMotherNature(-1534994648, 1);
            g.chooseCloud(-1534994648, 0);
            g.setStudentToHall(-1494447874, 3);
            g.setStudentToHall(-1494447874, 7);
            g.setStudentToHall(-1494447874, 2);
            g.setStudentToHall(-1494447874, 0);
            g.moveMotherNature(-1494447874, 3);
            g.chooseCloud(-1494447874, 1);
            g.setStudentToIsland(1232297098, 8, 1);
            g.setStudentToHall(1232297098, 4);
            g.setStudentToHall(1232297098, 1);
            g.setStudentToIsland(1232297098, 1, 10);
            g.moveMotherNature(1232297098, 4);
            g.chooseCloud(1232297098, 2);
            g.playCard(-1534994648, 8);
            g.playCard(1232297098, 6);
            g.playCard(-1494447874, 3);
            g.setStudentToHall(-1494447874, 5);
            g.setStudentToHall(-1494447874, 1);
            g.setStudentToHall(-1494447874, 0);
            g.setStudentToIsland(-1494447874, 0, 3);
            g.moveMotherNature(-1494447874, 1);
            g.chooseCloud(-1494447874, 0);
            g.setStudentToIsland(1232297098, 6, 9);
            g.setStudentToIsland(1232297098, 6, 5);
            g.setStudentToHall(1232297098, 5);
            g.setStudentToHall(1232297098, 1);
            g.moveMotherNature(1232297098, 4);
            g.chooseCloud(1232297098, 1);
            g.setStudentToIsland(-1534994648, 1, 8);
            g.setStudentToIsland(-1534994648, 6, 3);
            g.setStudentToHall(-1534994648, 5);
            g.setStudentToIsland(-1534994648, 1, 4);
            g.moveMotherNature(-1534994648, 4);
            g.chooseCloud(-1534994648, 2);
            g.playCard(-1494447874, 7);
            g.playCard(-1534994648, 5);
            g.playCard(1232297098, 5);
            g.setStudentToHall(1232297098, 5);
            g.setStudentToHall(1232297098, 2);
            g.setStudentToHall(1232297098, 6);
            g.setStudentToHall(1232297098, 0);
            g.moveMotherNature(1232297098, 3);
            g.chooseCloud(1232297098, 2);
            g.setStudentToHall(-1534994648, 3);
            g.setStudentToHall(-1534994648, 7);
            g.setStudentToHall(-1534994648, 4);
            g.setStudentToIsland(-1534994648, 1, 3);
            g.moveMotherNature(-1534994648, 1);
            g.chooseCloud(-1534994648, 1);
            g.setStudentToIsland(-1494447874, 5, 2);
            g.setStudentToIsland(-1494447874, 7, 9);
            g.setStudentToHall(-1494447874, 6);
            g.setStudentToIsland(-1494447874, 1, 2);
            g.moveMotherNature(-1494447874, 1);
            g.chooseCloud(-1494447874, 0);
            g.playCard(1232297098, 6);
            g.playCard(-1494447874, 1);
            g.playCard(-1534994648, 4);
            g.setStudentToIsland(-1494447874, 4, 9);
            g.setStudentToIsland(-1494447874, 4, 10);
            g.setStudentToHall(-1494447874, 6);
            g.setStudentToHall(-1494447874, 4);
            g.moveMotherNature(-1494447874, 1);
            g.chooseCloud(-1494447874, 2);
            g.setStudentToIsland(-1534994648, 0, 4);
            g.setStudentToIsland(-1534994648, 0, 2);
            g.setStudentToHall(-1534994648, 2);
            g.setStudentToIsland(-1534994648, 0, 4);
            g.moveMotherNature(-1534994648, 1);
            g.chooseCloud(-1534994648, 1);
            g.setStudentToHall(1232297098, 4);
            g.setStudentToHall(1232297098, 3);
            g.setStudentToIsland(1232297098, 0, 5);
            g.setStudentToIsland(1232297098, 0, 5);
            g.moveMotherNature(1232297098, 4);
            g.chooseCloud(1232297098, 0);
            g.playCard(-1494447874, 5);
            g.playCard(-1534994648, 3);
            g.playCard(1232297098, 5);
            g.setStudentToIsland(-1534994648, 5, 8);
            g.setStudentToIsland(-1534994648, 0, 1);
            g.setStudentToIsland(-1534994648, 6, 2);
            g.setStudentToHall(-1534994648, 3);
            g.moveMotherNature(-1534994648, 3);
            g.chooseCloud(-1534994648, 1);
            g.setStudentToHall(1232297098, 4);
            g.setStudentToIsland(1232297098, 3, 1);
            g.setStudentToIsland(1232297098, 1, 7);
            g.setStudentToIsland(1232297098, 2, 8);
            g.moveMotherNature(1232297098, 3);
            g.chooseCloud(1232297098, 2);
            g.setStudentToIsland(-1494447874, 3, 10);
            g.setStudentToHall(-1494447874, 1);
            g.setStudentToIsland(-1494447874, 4, 2);
            g.setStudentToIsland(-1494447874, 4, 6);
            g.moveMotherNature(-1494447874, 4);
            g.chooseCloud(-1494447874, 0);
            g.playCard(-1534994648, 2);
            g.playCard(1232297098, 2);
            g.playCard(-1494447874, 4);
            g.setStudentToHall(1232297098, 8);
            g.setStudentToIsland(1232297098, 1, 7);
            g.setStudentToHall(1232297098, 3);
            g.setStudentToHall(1232297098, 0);
            g.moveMotherNature(1232297098, 2);
            g.chooseCloud(1232297098, 2);
            g.setStudentToIsland(-1534994648, 1, 9);
            g.setStudentToHall(-1534994648, 5);
            g.setStudentToHall(-1534994648, 4);
            g.setStudentToHall(-1534994648, 3);
            g.moveMotherNature(-1534994648, 1);
            g.chooseCloud(-1534994648, 0);
            g.setStudentToIsland(-1494447874, 2, 5);
            g.setStudentToIsland(-1494447874, 1, 1);
            g.setStudentToIsland(-1494447874, 5, 0);
            g.setStudentToIsland(-1494447874, 3, 2);
            g.moveMotherNature(-1494447874, 4);
            g.chooseCloud(-1494447874, 1);
            g.playCard(1232297098, 0);
            g.playCard(-1494447874, 1);
            g.playCard(-1534994648, 3);
            g.setStudentToHall(1232297098, 4);
            g.setStudentToIsland(1232297098, 3, 4);
            g.setStudentToIsland(1232297098, 5, 1);
            g.setStudentToHall(1232297098, 1);
            g.moveMotherNature(1232297098, 1);
            g.chooseCloud(1232297098, 2);
            g.setStudentToHall(-1494447874, 1);
            g.setStudentToIsland(-1494447874, 0, 1);
            g.setStudentToIsland(-1494447874, 5, 1);
            g.setStudentToIsland(-1494447874, 4, 9);
            g.moveMotherNature(-1494447874, 2);
            g.chooseCloud(-1494447874, 0);
            g.setStudentToIsland(-1534994648, 0, 6);
            g.setStudentToHall(-1534994648, 4);
            g.setStudentToIsland(-1534994648, 1, 2);
            g.setStudentToHall(-1534994648, 4);
            g.moveMotherNature(-1534994648, 5);
            g.chooseCloud(-1534994648, 1);
            g.playCard(1232297098, 2);
            g.playCard(-1494447874, 2);
            g.playCard(-1534994648, 0);
            g.setStudentToHall(-1534994648, 5);
            g.setStudentToIsland(-1534994648, 0, 5);
            g.setStudentToIsland(-1534994648, 1, 8);
            g.setStudentToHall(-1534994648, 0);
            g.moveMotherNature(-1534994648, 1);
            g.setStudentToIsland(1232297098, 0, 1);
            g.setStudentToIsland(1232297098, 3, 4);
            g.setStudentToIsland(1232297098, 2, 3);
            g.setStudentToIsland(1232297098, 4, 4);
            g.moveMotherNature(1232297098, 1);
            g.setStudentToIsland(-1494447874, 3, 1);
            g.setStudentToIsland(-1494447874, 5, 1);
            g.setStudentToIsland(-1494447874, 5, 0);
            g.setStudentToIsland(-1494447874, 2, 3);
            g.moveMotherNature(-1494447874, 2);

        };
        tests.add(buildDesignatedGame("Towers tie", students, islands_students, List.of(-1494447874, -1534994648, 1232297098), 3, new int[] {9, 8, 7}, moves));

        /*--- towers and professors tie ---*/
        students = new ArrayList<>(Arrays.asList(
                Colors.BLUE, Colors.BLUE, Colors.RED, Colors.BLUE, Colors.YELLOW, Colors.YELLOW, Colors.YELLOW,
                Colors.YELLOW, Colors.GREEN, Colors.MAGENTA, Colors.RED, Colors.GREEN, Colors.RED, Colors.RED,
                Colors.YELLOW, Colors.YELLOW, Colors.BLUE, Colors.RED, Colors.GREEN, Colors.BLUE, Colors.GREEN,
                Colors.RED, Colors.MAGENTA, Colors.GREEN, Colors.BLUE, Colors.RED, Colors.BLUE, Colors.BLUE,
                Colors.BLUE, Colors.YELLOW, Colors.GREEN, Colors.MAGENTA, Colors.RED, Colors.GREEN, Colors.MAGENTA,
                Colors.YELLOW, Colors.RED, Colors.MAGENTA, Colors.MAGENTA, Colors.RED, Colors.GREEN, Colors.YELLOW,
                Colors.GREEN, Colors.GREEN, Colors.BLUE, Colors.MAGENTA, Colors.YELLOW, Colors.GREEN, Colors.MAGENTA,
                Colors.BLUE, Colors.MAGENTA, Colors.RED, Colors.GREEN, Colors.RED, Colors.GREEN, Colors.YELLOW,
                Colors.MAGENTA, Colors.BLUE, Colors.GREEN, Colors.RED, Colors.MAGENTA, Colors.MAGENTA, Colors.RED,
                Colors.MAGENTA, Colors.RED, Colors.YELLOW, Colors.RED, Colors.BLUE, Colors.BLUE, Colors.YELLOW,
                Colors.YELLOW, Colors.GREEN, Colors.RED, Colors.YELLOW, Colors.YELLOW, Colors.BLUE, Colors.YELLOW,
                Colors.BLUE, Colors.YELLOW, Colors.MAGENTA, Colors.BLUE, Colors.RED, Colors.MAGENTA, Colors.BLUE,
                Colors.GREEN, Colors.MAGENTA, Colors.YELLOW, Colors.RED, Colors.MAGENTA, Colors.GREEN, Colors.GREEN,
                Colors.YELLOW, Colors.RED, Colors.YELLOW, Colors.MAGENTA, Colors.MAGENTA, Colors.RED, Colors.GREEN,
                Colors.BLUE, Colors.BLUE, Colors.GREEN, Colors.BLUE, Colors.RED, Colors.RED, Colors.GREEN,
                Colors.YELLOW, Colors.BLUE, Colors.MAGENTA, Colors.MAGENTA, Colors.GREEN, Colors.MAGENTA, Colors.YELLOW,
                Colors.BLUE, Colors.MAGENTA, Colors.GREEN, Colors.GREEN, Colors.YELLOW, Colors.RED,
                Colors.BLUE, Colors.MAGENTA
        ));
        islands_students = new ArrayList<>(Arrays.asList(
                Colors.YELLOW, Colors.BLUE, Colors.BLUE, Colors.MAGENTA, Colors.MAGENTA, Colors.YELLOW, Colors.RED,
                Colors.GREEN, Colors.GREEN, Colors.RED
        ));
        moves = (g) -> {
            g.playCard(-195888619, 0);
            g.playCard(-336797321, 7);
            g.playCard(50507052, 5);
            g.setStudentToHall(-195888619, 6);
            g.setStudentToIsland(-195888619, 2, 4);
            g.setStudentToIsland(-195888619, 6, 1);
            g.setStudentToIsland(-195888619, 5, 10);
            g.moveMotherNature(-195888619, 1);
            g.chooseCloud(-195888619, 1);
            g.setStudentToHall(50507052, 7);
            g.setStudentToIsland(50507052, 3, 3);
            g.setStudentToHall(50507052, 0);
            g.setStudentToHall(50507052, 0);
            g.moveMotherNature(50507052, 3);
            g.chooseCloud(50507052, 2);
            g.setStudentToHall(-336797321, 2);
            g.setStudentToHall(-336797321, 4);
            g.setStudentToIsland(-336797321, 6, 10);
            g.setStudentToIsland(-336797321, 2, 8);
            g.moveMotherNature(-336797321, 4);
            g.chooseCloud(-336797321, 0);
            g.playCard(-195888619, 1);
            g.playCard(-336797321, 5);
            g.playCard(50507052, 1);
            g.setStudentToIsland(50507052, 2, 10);
            g.setStudentToIsland(50507052, 4, 7);
            g.setStudentToHall(50507052, 2);
            g.setStudentToIsland(50507052, 5, 2);
            g.moveMotherNature(50507052, 1);
            g.chooseCloud(50507052, 0);
            g.setStudentToIsland(-195888619, 4, 7);
            g.setStudentToHall(-195888619, 1);
            g.setStudentToIsland(-195888619, 6, 2);
            g.setStudentToIsland(-195888619, 0, 11);
            g.moveMotherNature(-195888619, 1);
            g.chooseCloud(-195888619, 2);
            g.setStudentToHall(-336797321, 1);
            g.setStudentToHall(-336797321, 3);
            g.setStudentToHall(-336797321, 0);
            g.setStudentToIsland(-336797321, 1, 8);
            g.moveMotherNature(-336797321, 3);
            g.chooseCloud(-336797321, 1);
            g.playCard(50507052, 0);
            g.playCard(-195888619, 2);
            g.playCard(-336797321, 2);
            g.setStudentToHall(50507052, 3);
            g.setStudentToHall(50507052, 2);
            g.setStudentToIsland(50507052, 4, 9);
            g.setStudentToHall(50507052, 3);
            g.moveMotherNature(50507052, 1);
            g.chooseCloud(50507052, 0);
            g.setStudentToIsland(-336797321, 8, 9);
            g.setStudentToIsland(-336797321, 6, 1);
            g.setStudentToHall(-336797321, 4);
            g.setStudentToIsland(-336797321, 3, 0);
            g.moveMotherNature(-336797321, 1);
            g.chooseCloud(-336797321, 2);
            g.setStudentToIsland(-195888619, 3, 2);
            g.setStudentToIsland(-195888619, 2, 7);
            g.setStudentToHall(-195888619, 5);
            g.setStudentToIsland(-195888619, 5, 2);
            g.moveMotherNature(-195888619, 1);
            g.chooseCloud(-195888619, 1);
            g.playCard(50507052, 3);
            g.playCard(-195888619, 0);
            g.playCard(-336797321, 5);
            g.setStudentToIsland(-195888619, 0, 1);
            g.setStudentToIsland(-195888619, 7, 6);
            g.setStudentToHall(-195888619, 6);
            g.setStudentToIsland(-195888619, 5, 0);
            g.moveMotherNature(-195888619, 1);
            g.chooseCloud(-195888619, 0);
            g.setStudentToIsland(50507052, 1, 7);
            g.setStudentToHall(50507052, 4);
            g.setStudentToIsland(50507052, 3, 2);
            g.setStudentToHall(50507052, 2);
            g.moveMotherNature(50507052, 1);
            g.chooseCloud(50507052, 1);
            g.setStudentToIsland(-336797321, 4, 1);
            g.setStudentToHall(-336797321, 6);
            g.setStudentToHall(-336797321, 3);
            g.setStudentToIsland(-336797321, 5, 0);
            g.moveMotherNature(-336797321, 1);
            g.chooseCloud(-336797321, 2);
            g.playCard(-195888619, 4);
            g.playCard(-336797321, 1);
            g.playCard(50507052, 0);
            g.setStudentToIsland(-336797321, 7, 2);
            g.setStudentToHall(-336797321, 0);
            g.setStudentToIsland(-336797321, 6, 7);
            g.setStudentToHall(-336797321, 3);
            g.moveMotherNature(-336797321, 1);
            g.chooseCloud(-336797321, 0);
            g.setStudentToHall(50507052, 2);
            g.setStudentToIsland(50507052, 7, 6);
            g.setStudentToIsland(50507052, 6, 2);
            g.setStudentToIsland(50507052, 2, 8);
            g.moveMotherNature(50507052, 2);
            g.chooseCloud(50507052, 1);
            g.setStudentToHall(-195888619, 0);
            g.setStudentToHall(-195888619, 2);
            g.setStudentToHall(-195888619, 0);
            g.setStudentToIsland(-195888619, 1, 8);
            g.moveMotherNature(-195888619, 1);
            g.chooseCloud(-195888619, 2);
            g.playCard(-336797321, 0);
            g.playCard(50507052, 0);
            g.playCard(-195888619, 4);
            g.setStudentToHall(-336797321, 3);
            g.setStudentToHall(-336797321, 4);
            g.setStudentToHall(-336797321, 3);
            g.setStudentToIsland(-336797321, 1, 3);
            g.moveMotherNature(-336797321, 1);
            g.chooseCloud(-336797321, 0);
            g.setStudentToIsland(50507052, 4, 9);
            g.setStudentToIsland(50507052, 1, 2);
            g.setStudentToIsland(50507052, 5, 5);
            g.setStudentToIsland(50507052, 3, 8);
            g.moveMotherNature(50507052, 2);
            g.chooseCloud(50507052, 2);
            g.setStudentToIsland(-195888619, 6, 4);
            g.setStudentToIsland(-195888619, 5, 2);
            g.setStudentToIsland(-195888619, 0, 3);
            g.setStudentToHall(-195888619, 3);
            g.moveMotherNature(-195888619, 3);
            g.chooseCloud(-195888619, 1);
            g.playCard(-336797321, 1);
            g.playCard(50507052, 2);
            g.playCard(-195888619, 1);
            g.setStudentToHall(-336797321, 8);
            g.setStudentToIsland(-336797321, 1, 1);
            g.setStudentToHall(-336797321, 0);
            g.setStudentToIsland(-336797321, 1, 2);
            g.moveMotherNature(-336797321, 2);
            g.chooseCloud(-336797321, 2);
            g.setStudentToIsland(-195888619, 4, 3);
            g.setStudentToIsland(-195888619, 4, 6);
            g.setStudentToHall(-195888619, 4);
            g.setStudentToIsland(-195888619, 0, 0);
            g.moveMotherNature(-195888619, 1);
            g.chooseCloud(-195888619, 0);
            g.setStudentToHall(50507052, 0);
            g.setStudentToHall(50507052, 6);
            g.setStudentToHall(50507052, 3);
            g.setStudentToHall(50507052, 3);
            g.moveMotherNature(50507052, 4);
            g.chooseCloud(50507052, 1);
            g.playCard(-336797321, 1);
            g.playCard(50507052, 1);
            g.playCard(-195888619, 0);
            g.setStudentToHall(-195888619, 8);
            g.setStudentToHall(-195888619, 3);
            g.setStudentToIsland(-195888619, 6, 2);
            g.setStudentToHall(-195888619, 1);
            g.moveMotherNature(-195888619, 1);
            g.setStudentToHall(-336797321, 4);
            g.setStudentToHall(-336797321, 5);
            g.setStudentToIsland(-336797321, 0, 6);
            g.setStudentToIsland(-336797321, 3, 1);
            g.moveMotherNature(-336797321, 1);
            g.setStudentToIsland(50507052, 2, 0);
            g.setStudentToIsland(50507052, 7, 0);
            g.setStudentToHall(50507052, 3);
            g.setStudentToIsland(50507052, 5, 2);
            g.moveMotherNature(50507052, 2);
        };
        tests.add(buildDesignatedGame("Towers and professors tie", students, islands_students, List.of(-195888619, -336797321, 50507052), 9, new int[] {5, 12, 10}, moves));

        /*--- randomly generated games ---*/
        for (int i = 0; i < num_of_random_games; i++)
            tests.add(new TestGame(random.nextBoolean(), random.nextInt(2, 4)));

        return tests;
    }

    /**
     * Test class that checks phase 0 related moves:
     * <li>{@link Phase0Tests#playCard}</li>
     * <p>
     *     See its documentation for the detailed list of what is checked.
     * </p>
     */
    @Nested
    @DisplayName("Phase 0")
    @TestMethodOrder(MethodOrderer.OrderAnnotation.class)
    @TestClassOrder(ClassOrderer.OrderAnnotation.class)
    @Order(1)
    public class Phase0Tests {
        /**
         * Checks the {@link Game#playCard} method.
         * <p>
         *     Specifically it asserts:
         *     <li>The method throws an {@link InvalidMoveException} for all the expected instances:
         *     <ul>
         *         <li>A player is trying to play while not being its turn;</li>
         *         <li>Trying to play this action during wrong game state;</li>
         *         <li>A card with the same value has already been played by someone else in the current turn (when appropriate).</li>
         *     </ul>
         *     </li>
         *     <li>The method does not throw when playing in proper conditions;</li>
         *     <li>The played card is actually registered as the last card played for the correct player and the card is
         *     no longer in its deck;</li>
         *     <li>The method allows a player to play a card with the same value as another already played one while
         *     in all the proper conditions for that to be allowed, meaning:
         *     <ul>
         *         <li>When players only have 1 card left to play and it matches at least one of the previously played cards</li>
         *         <li>In a 3 players game, when players only have 2 cards left to play and both matches one of the previously played cards</li>
         *     </ul>
         *     </li>
         *     <li>The game turn is marked as the last one if a player plays its last card;</li>
         *     <li>All other game properties have not changed if not supposed to.</li>
         * </p>
         * @implNote This test plays the game until it ends and checks all its related assertions for every turn up to that moment.
         */
        @DisplayName("Play card")
        @ParameterizedTest(name = "{0}")
        @Order(1)
        @MethodSource(arguments_supplier)
        void playCard(final TestGame test) {
            final Game game = test.game;
            boolean special_condition = false; //conditions where playing_player have to play a card with the same value of an already played card
            //make sure game is in the correct state
            test.autoplayUpTo(0, 0);

            while(!game.isGameEnded()) {
                //prepare the TestGame
                test.updateOldGameCopy();
                //keep track of already played cards during the turn
                final List<Integer> played_values = new ArrayList<>();

                //for every player, ordered by turn order
                for (int i = test.game.getPlayerTurn(); i < test.orderedPlayers().size(); i++) {
                    Player playing_player = test.orderedPlayers().get(i);
                    //for every not playing player check the related exception
                    final List<Player> not_playing_players = game.getPlayers().stream().filter(x -> x.clientID != playing_player.clientID).toList();
                    for (final Player not_playing_player : not_playing_players) {
                        Exception e = assertThrows(InvalidMoveException.class,
                                () -> game.playCard(not_playing_player.clientID, random.nextInt()),
                                "playCard has not throw exception while trying to be used by a non playing player");
                        assertEquals("It's not your turn.", e.getMessage(), "The exception has been thrown, but for the wrong reason");
                    }
                    int random_card_index;
                    //conditions where playing_player have to play a card with the same value of an already played card
                    if (playing_player.getCards().size() == 1
                            || (game.getPlayerTurn() == 2 && playing_player.getCards().size() == 2
                                && playing_player.getCards().stream()
                                            .allMatch(card -> test.game.getPlayers().stream()
                                            .filter(player -> player != playing_player)
                                            .anyMatch(player -> card.order_value == player.getLastCardPlayed().order_value)))) {
                        random_card_index = random.nextInt(playing_player.getCards().size());
                        special_condition = true;
                    } else {
                        //for every already played card check the related exception
                        List<Integer> already_played_cards_indexes = new ArrayList<>();
                        for (Integer inter : played_values) {
                            if (playing_player.getCards().stream().anyMatch(x -> x.order_value == inter)) {
                                already_played_cards_indexes.add(playing_player.getCards().indexOf(
                                        playing_player.getCards().stream()
                                                .filter(x -> x.order_value == inter)
                                                .findAny().orElseThrow()));
                            }
                        }
                        for (final Integer index : already_played_cards_indexes) {
                            Exception e = assertThrows(InvalidMoveException.class,
                                    () -> game.playCard(playing_player.clientID, index),
                                    "playCard has not thrown exception while trying to play a card with the same value of a card already played by another player");
                            assertEquals("A card with the same value has already been played.", e.getMessage(), "The exception has been thrown, but for the wrong reason");
                        }
                        //pick a random, not already played, card to play
                        do random_card_index = random.nextInt(playing_player.getCards().size());
                        while (played_values.contains(playing_player.getCards().get(random_card_index).order_value));
                    }

                    //prepare to play a card
                    List<Card> player_original_deck = playing_player.getCards();
                    final int finalRandom_card_index = random_card_index;
                    //actually play the card
                    assertDoesNotThrow(() -> game.playCard(playing_player.clientID, finalRandom_card_index),
                            "playCard has thrown an exception while player " + playing_player.clientID + " was trying to play card " + finalRandom_card_index);

                    //update already played cards list
                    played_values.add(player_original_deck.remove(finalRandom_card_index).order_value);
                    //check that the move has been correctly performed
                    assertEquals(played_values.get(played_values.size() - 1), playing_player.getLastCardPlayed().order_value, "Last card played by player " + playing_player.clientID + " has not been updated correctly");
                    assertEquals(player_original_deck, playing_player.getCards(), "Deck of player " + playing_player.clientID + " has not been updated correctly");
                    assertThrows(InvalidMoveException.class,
                            () -> game.playCard(playing_player.clientID, 0),
                            "playCard has not thrown exception while trying to play another card right after playing one");
                    test.hasNothingChangedExcept(0, 2);
                }
                //post play checks
                if(test.game.getPlayers().stream().anyMatch(player -> player.getCards().isEmpty()))
                    assertTrue(game.isLastGameTurn(), "last_game_turn has not been set after a player played its last card");
                assertEquals(1, game.getPhase(), "Game phase not updated correctly after all players have played a card");
                if(!special_condition) {
                    int[] just_played_order_values = game.getPlayers().stream().mapToInt(player -> player.getLastCardPlayed().order_value).distinct().toArray();
                    assertEquals(just_played_order_values.length, game.getPlayers().size(), "Some player has played a card with the same number as another one");
                }
                final Exception e = assertThrows(InvalidMoveException.class,
                        () -> test.game.playCard(test.game.currentlyPlayingPlayer(), 0),
                        "playCard has not thrown an exception while trying to play during the wrong game state");
                assertEquals("Cannot be performed in the current game state.", e.getMessage(), "The exception has been thrown, but for the wrong reason");

                //prepare for next turn
                test.autoplayUpTo(0, 0);
            }
        }
    }

    /**
     * Test class that checks phase 1 related moves:
     * <li>{@link Phase1Tests#setStudentToHall}</li>
     * <li>{@link Phase1Tests#setStudentToIsland}</li>
     * <li>{@link Phase1Tests#moveMotherNature}</li>
     * <li>{@link Phase1Tests#chooseCloud}</li>
     * <li>{@link Phase1Tests#activateEffect}</li>
     * <li>Game ending and winner computation with {@link Phase1Tests#gameEndingAndWinner} test.</li>
     * <p>
     *     See their documentations for the detailed list of what is checked.
     * </p>
     */
    @Nested
    @DisplayName("Phase 1")
    @TestMethodOrder(MethodOrderer.OrderAnnotation.class)
    @TestClassOrder(ClassOrderer.OrderAnnotation.class)
    @Order(2)
    public class Phase1Tests {

        /**
         * Checks the {@link Game#setStudentToHall} method.
         * <p>
         *     Specifically it asserts:
         *     <li>The method throws an {@link InvalidMoveException} for all the expected instances:
         *     <ul>
         *         <li>A player is trying to play while not being its turn;</li>
         *         <li>Trying to play this action during wrong game state;</li>
         *         <li>Trying to pass an incorrect student index.</li>
         *     </ul>
         *     </li>
         *     <li>The method does not throw when playing in proper conditions;</li>
         *     <li>The student is actually moved from entrance and placed in the correct hall row;</li>
         *     <li>The player received a coin when it is supposed to;</li>
         *     <li>At the end of the turn all the professors are correctly attributed;</li>
         *     <li>All other game properties have not changed if not supposed to.</li>
         * </p>
         * @implNote This test runs for all movable students (3 or 4) and plays the game until it ends,
         * checking all its assertions for every turn up to that moment.
         */
        @DisplayName("Move student to hall")
        @ParameterizedTest(name = "{0}")
        @Order(1)
        @MethodSource(arguments_supplier)
        void setStudentToHall(TestGame test) {
            //prepare the TestGame and its game
            test.autoplayUpTo(1, 0);

            while (!test.game.isGameEnded()) {
                test.updateOldGameCopy();
                final Player playing_player = test.playingPlayer();

                //for every not playing player check the related exception
                final List<Player> not_playing_players = test.game.getPlayers().stream().filter(x -> x.clientID != playing_player.clientID).toList();
                for (final Player not_playing_player : not_playing_players) {
                    for (final Colors student : not_playing_player.getDashboard().getEntrance()) {
                        final Exception e = assertThrows(InvalidMoveException.class,
                                () -> test.game.setStudentToHall(not_playing_player.clientID, not_playing_player.getDashboard().getEntrance().indexOf(student)),
                                "setStudentToHall has not thrown exception while trying to play when was not the player's turn");
                        assertEquals("It's not your turn.", e.getMessage(), "The exception has been thrown, but for the wrong reason");
                    }
                }

                //move 3 or 4 students
                for(int i = test.game.getMovedStudents(); i < (test.designated_num_of_players == 2 ? 3 : 4); i++) {
                    assertEquals(i, test.game.getMovedStudents(), "Wrong number of moved students");
                    //check bad student index exception
                    int bad_student_index;
                    do bad_student_index = random.nextInt();
                    while (bad_student_index >= 0 && bad_student_index < playing_player.getDashboard().getEntrance().size());
                    final int finalBad_student_index = bad_student_index;
                    final Exception e = assertThrows(InvalidMoveException.class,
                            () -> test.game.setStudentToHall(playing_player.clientID, finalBad_student_index),
                            "setStudentHall has not thrown exception with an invalid student index");
                    assertEquals("Invalid student index.", e.getMessage(), "The exception has been thrown, but for the wrong reason");
                    //save values for the following assertions
                    final int random_student = random.nextInt(playing_player.getDashboard().getEntrance().size());
                    final Colors row = playing_player.getDashboard().getEntrance().get(random_student);
                    final int original_entrance_students = playing_player.getDashboard().getEntrance().size();
                    final int original_row_students = playing_player.getDashboard().getHallRow(row);
                    final int original_coins = playing_player.getCoins();
                    final Player original_professor_possessor = test.game.getPlayers().stream().filter(x -> x.getDashboard().getProfessor(row)).findFirst().orElse(playing_player);

                    //actually move the student
                    assertDoesNotThrow(() -> test.game.setStudentToHall(playing_player.clientID, random_student),
                            "setStudentToHall has thrown exception while player " + playing_player.clientID + " was trying to move a student to row " + row);

                    //prepare for assertions
                    final int updated_entrance_students = playing_player.getDashboard().getEntrance().size();
                    final int updated_row_students = playing_player.getDashboard().getHallRow(row);
                    final int updated_coins = playing_player.getCoins();
                    //check if the students where moved correctly
                    assertEquals(original_entrance_students - 1, updated_entrance_students, "The student has not been removed from the entrance");
                    assertEquals(original_row_students + 1, updated_row_students, "The student has not been added to the correct row");
                    //check if the coin was given, if needed
                    if (test.designated_expert_mode && test.game.getBank() > 0 && updated_row_students % 3 == 0)
                        assertEquals(original_coins + 1, updated_coins, "The additional coin has not been given to the player");
                    //check the assignment of the professors
                    Player updated_professor_possessor = original_professor_possessor;
                    for (final Player player : test.game.getPlayers()) { //find the rightful professor owner
                        if (player.getDashboard().getHallRow(row) > updated_professor_possessor.getDashboard().getHallRow(row))
                            updated_professor_possessor = player;
                    }
                    for (final Player player : test.game.getPlayers()) {
                        if (player.equals(updated_professor_possessor))
                            assertTrue(player.getDashboard().getProfessor(row), "The " + row + " professor has not been assigned to the correct player");
                        else
                            assertFalse(player.getDashboard().getProfessor(row), "The " + row + " professor has not been moved to the correct player");
                    }

                    test.hasNothingChangedExcept(0, 2, 4);
                }
                //post play checks
                assertEquals(1, test.game.getPhase(), "Phase not updated after students have been moved");
                assertEquals(1, test.game.getStep(), "Step not updated after students have been moved");
                final Exception e = assertThrows(InvalidMoveException.class,
                        () -> test.game.setStudentToHall(playing_player.clientID, random.nextInt()),
                        "setStudentToHall has not thrown exception while trying to play during the wrong game state");
                assertEquals("Cannot be performed in the current game state.", e.getMessage(), "The exception has been thrown, but for the wrong reason");
                //prepare for next turn
                test.autoplayUpTo(1, 0);
            }

        }

        /**
         * Checks the {@link Game#setStudentToIsland} method.
         * <p>
         *     Specifically it asserts:
         *     <li>The method throws an {@link InvalidMoveException} for all the expected instances:
         *     <ul>
         *         <li>A player is trying to play while not being its turn;</li>
         *         <li>Trying to play this action during wrong game state;</li>
         *         <li>Trying to pass an incorrect student index and/or island index.</li>
         *     </ul>
         *     </li>
         *     <li>The method does not throw when playing in proper conditions;</li>
         *     <li>The student is actually moved from entrance and placed on the correct island;</li>
         *     <li>All other game properties have not changed if not supposed to.</li>
         * </p>
         * @implNote This test runs for all movable students (3 or 4) and plays the game until it ends,
         * checking all its assertions for every turn up to that moment.
         */
        @DisplayName("Move student to Island")
        @ParameterizedTest(name = "{0}")
        @Order(2)
        @MethodSource(arguments_supplier)
        void setStudentToIsland(TestGame test) {
            //prepare the TestGame and its game
            test.autoplayUpTo(1, 0);

            while (!test.game.isGameEnded()) {
                test.updateOldGameCopy();
                final Player playing_player = test.playingPlayer();

                //if it is not the specified player's turn check the related exception
                final List<Player> not_playing_players = test.game.getPlayers().stream().filter(x -> x.clientID != playing_player.clientID).toList();
                for (final Player not_playing_player : not_playing_players) {
                    final Exception e = assertThrows(InvalidMoveException.class,
                            () -> test.game.setStudentToIsland(not_playing_player.clientID, random.nextInt(), random.nextInt()),
                            "setStudentToIsland has not thrown exception while trying to play when was not the player's turn");
                    assertEquals("It's not your turn.", e.getMessage(), "The exception has been thrown, but for the wrong reason");
                }

                //actually move 3 or 4 students
                for(int i = test.game.getMovedStudents(); i < (test.designated_num_of_players == 2 ? 3 : 4); i++) {
                    //check bad student index exception
                    int bad_student_index;
                    do bad_student_index = random.nextInt();
                    while (bad_student_index >= 0 && bad_student_index < playing_player.getDashboard().getEntrance().size());
                    final int finalBad_student_index = bad_student_index;
                    Exception e = assertThrows(InvalidMoveException.class,
                            () -> test.game.setStudentToIsland(playing_player.clientID, finalBad_student_index, random.nextInt()),
                            "setStudentToIsland has not thrown exception with an invalid student index");
                    assertEquals("Invalid student index.", e.getMessage(), "The exception has been thrown, but for the wrong reason");
                    final int random_student = random.nextInt(playing_player.getDashboard().getEntrance().size());
                    //check bad island index exception
                    int bad_island_index;
                    do bad_island_index = random.nextInt();
                    while (bad_island_index >= 0 && bad_island_index < test.game.getIslands().size());
                    final int finalBad_island_index = bad_island_index;
                    e = assertThrows(InvalidMoveException.class,
                            () -> test.game.setStudentToIsland(playing_player.clientID, random_student, finalBad_island_index),
                            "setStudentToIsland has not thrown exception with an invalid student index");
                    assertEquals("Invalid island index.", e.getMessage(), "The exception has been thrown, but for the wrong reason");
                    final int random_island = random.nextInt(test.game.getIslands().size());
                    //store values for the following assertions
                    final int original_entrance_students = playing_player.getDashboard().getEntrance().size();
                    final Colors student_color = playing_player.getDashboard().getEntrance().get(random_student);
                    final int original_island_students = test.game.getIslands().get(random_island).getStudents(student_color);

                    //actually move the student
                    assertDoesNotThrow(() -> test.game.setStudentToIsland(playing_player.clientID, random_student, random_island),
                            "setStudentToIsland has thrown exception while player " + playing_player.clientID + " was trying to move a student to island " + random_island);

                    //prepare for assertions
                    final int updated_entrance_students = playing_player.getDashboard().getEntrance().size();
                    final int updated_island_students = test.game.getIslands().get(random_island).getStudents(student_color);
                    //check if the student has been moved correctly
                    assertEquals(original_entrance_students - 1, updated_entrance_students, "The student has not been removed from the entrance");
                    assertEquals(original_island_students + 1, updated_island_students, "Wrong number of students on island " + random_island + " after the move");

                    test.hasNothingChangedExcept(0, 2, 4);
                }
                //post move checks
                assertEquals(1, test.game.getPhase(), "Phase not updated after students have been moved");
                assertEquals(1, test.game.getStep(), "Step not updated after students have been moved");
                final Exception e = assertThrows(InvalidMoveException.class,
                        () -> test.game.setStudentToIsland(playing_player.clientID, random.nextInt(), random.nextInt()),
                        "setStudentToIsland has not thrown exception while trying to play during the wrong game state");
                assertEquals("Cannot be performed in the current game state.", e.getMessage(), "The exception has been thrown, but for the wrong reason");
                //prepare for next turn
                test.autoplayUpTo(1, 0);
            }

        }

        /**
         * Checks the {@link Game#moveMotherNature} method.
         * <p>
         *     Specifically it asserts:
         *     <li>The method throws an {@link InvalidMoveException} for all the expected instances:
         *     <ul>
         *         <li>A player is trying to play while not being its turn;</li>
         *         <li>Trying to play this action during wrong game state;</li>
         *         <li>Trying to pass an incorrect number of moves.</li>
         *     </ul>
         *     </li>
         *     <li>The method does not throw when playing in proper conditions;</li>
         *     <li>Mother nature has been moved to the correct island;</li>
         *     <li>The island(s) state has been updated correctly;</li>
         *     <li>All other game properties have not changed if not supposed to.</li>
         * </p>
         * @implNote This test plays the game until it ends, checking all its assertions for every turn up to that moment.
         */
        @DisplayName("Move mother nature")
        @ParameterizedTest(name = "{0}")
        @Order(3)
        @MethodSource(arguments_supplier)
        void moveMotherNature(TestGame test) {
            //prepare the TestGame and its game
            test.autoplayUpTo(1, 1);

            while (!test.game.isGameEnded()) {
                test.updateOldGameCopy();
                final Player playing_player = test.playingPlayer();
                //if it is not the specified player's turn check the related exception
                final List<Player> not_playing_players = test.game.getPlayers().stream().filter(x -> x.clientID != playing_player.clientID).toList();
                for (final Player not_playing_player : not_playing_players) {
                    final Exception e = assertThrows(InvalidMoveException.class,
                            () -> test.game.moveMotherNature(not_playing_player.clientID, random.nextInt()),
                            "moveMotherNature has not thrown exception while trying to play when was not the player's turn");
                    assertEquals("It's not your turn.", e.getMessage(), "The exception has been thrown, but for the wrong reason");
                }
                //check 0 moves exception
                int bad_number_of_moves = 0;
                final int finalBad_number_of_moves = bad_number_of_moves;
                Exception e = assertThrows(InvalidMoveException.class,
                        () -> test.game.moveMotherNature(playing_player.clientID, finalBad_number_of_moves),
                        "moveMotherNature has not thrown exception with 0 moves");
                assertEquals("Invalid moves number.", e.getMessage(), "The exception has been thrown, but for the wrong reason");
                //check bad number of moves exception
                do bad_number_of_moves = random.nextInt();
                while (bad_number_of_moves >= 1 && bad_number_of_moves < playing_player.getLastCardPlayed().movements_value);
                final int finalBad_number_of_moves_2 = bad_number_of_moves;
                e = assertThrows(InvalidMoveException.class,
                        () -> test.game.moveMotherNature(playing_player.clientID, finalBad_number_of_moves_2),
                        "moveMotherNature has not thrown exception with an invalid moves number");
                assertEquals("Invalid moves number.", e.getMessage(), "The exception has been thrown, but for the wrong reason");

                //prepare to move mother nature
                final int random_moves = random.nextInt(1, playing_player.getLastCardPlayed().movements_value + 1);
                Game pre_activation = test.game.copy();
                //actually move mother nature
                assertDoesNotThrow(() -> test.game.moveMotherNature(playing_player.clientID, random_moves),
                        "moveMotherNature has thrown exception while player " + playing_player.clientID + " was trying to move mother nature from position "
                                + test.game.getMotherNature() + " by " + random_moves + " island" + (random_moves > 1 ? "s" : ""));

                //prepare for assertions
                final int mother_nature_new_position = (pre_activation.getMotherNature() + random_moves) % pre_activation.getIslands().size();
                Map<String, Object> result = TestGame.disputeIsland(pre_activation, mother_nature_new_position);
                if((int) result.get("result") == 3) //game is ended due to islands.size() <= 3
                    break;
                //check if mother nature has been moved correctly and the island(s) have been updated correctly
                assertEquals(result.get("index"), test.game.getMotherNature(), "Mother nature has not been moved correctly");
                Island new_island = test.game.getIslands().get(test.game.getMotherNature());
                assertAll("New island(s) state",
                        () -> assertEquals((Integer) result.get("owner_index"), new_island.getOwnerIndex(), "Wrong owner index"),
                        () -> assertEquals((int) result.get("num_of_merged_islands"), new_island.getNumOfMergedIslands(), "wrong number of merged island(s)"),
                        () -> assertEquals((boolean) result.get("interdiction"), new_island.getInterdiction(), "Wrong interdiction value"),
                        () -> {
                            final int[] students = (int[]) result.get("students");
                            for(Colors color : Colors.values())
                                assertEquals(students[color.index], new_island.getStudents(color), "Wrong students");
                        }
                );

                test.hasNothingChangedExcept(0, 1, 2, 6, 8);
                //post move checks
                assertEquals(1, test.game.getPhase(), "Phase not updated after mother nature has been moved");
                if(!test.game.isGameEnded())
                    assertEquals(test.game.isLastGameTurn() ? 0 : 2, test.game.getStep(), "Step not updated correctly after mother nature has been moved");
                    e = assertThrows(InvalidMoveException.class,
                        () -> test.game.moveMotherNature(test.game.currentlyPlayingPlayer(), random.nextInt()),
                        "moveMotherNature has not thrown exception while trying to play during the wrong game state");
                assertEquals("Cannot be performed in the current game state.", e.getMessage(), "The exception has been thrown, but for the wrong reason");
                //prepare for next turn
                test.autoplayUpTo(1, 1);
            }
        }

        /**
         * Checks the {@link Game#chooseCloud} method.
         * <p>
         *     Specifically it asserts:
         *     <li>The method throws an {@link InvalidMoveException} for all the expected instances:
         *     <ul>
         *         <li>A player is trying to play while not being its turn;</li>
         *         <li>Trying to play this action during wrong game state;</li>
         *         <li>Trying to pass an incorrect cloud index or the index refers to an empty cloud.</li>
         *     </ul>
         *     </li>
         *     <li>The method does not throw when playing in proper conditions;</li>
         *     <li>The students have been moved form the cloud to the player's entrance;</li>
         *     <li>The cloud is now empty;</li>
         *     <li>All other game properties have not changed if not supposed to.</li>
         * </p>
         * @implNote This test plays the game until it ends, checking all its assertions for every
         * turn up to that moment.
         */
        @DisplayName("Choose cloud")
        @ParameterizedTest(name = "{0}")
        @Order(4)
        @MethodSource(arguments_supplier)
        void chooseCloud(TestGame test) {
            //prepare the TestGame and its game
            test.autoplayUpTo(1, 2);

            while (!test.game.isLastGameTurn() && !test.game.isGameEnded()) {
                final Player playing_player = test.playingPlayer();
                test.updateOldGameCopy();
                //if it is not the specified player's turn check the related exception
                final List<Player> not_playing_players = test.game.getPlayers().stream().filter(x -> x.clientID != playing_player.clientID).toList();
                for (final Player not_playing_player : not_playing_players) {
                    final Exception e = assertThrows(InvalidMoveException.class,
                            () -> test.game.chooseCloud(not_playing_player.clientID, random.nextInt()),
                            "chooseCloud has not thrown exception while trying to play when was not the player's turn");
                    assertEquals("It's not your turn.", e.getMessage(), "The exception has been thrown, but for the wrong reason");
                }
                //check bad index exception
                int bad_cloud_index;
                do bad_cloud_index = random.nextInt();
                while (bad_cloud_index >= 0 && bad_cloud_index < test.designated_num_of_players);
                final int finalBad_cloud_index = bad_cloud_index;
                Exception e = assertThrows(InvalidMoveException.class,
                        () -> test.game.chooseCloud(playing_player.clientID, finalBad_cloud_index),
                        "chooseCloud has not thrown exception with an invalid cloud index");
                assertEquals("Invalid cloud index.", e.getMessage(), "The exception has been thrown, but for the wrong reason");
                //check empty cloud chosen exception, if possible
                for (int i = 0; i < test.designated_num_of_players; i++) {
                    if(test.game.getClouds()[i].isEmpty()) {
                        final int finalBad_cloud_index_2 = i;
                        e = assertThrows(InvalidMoveException.class,
                                () -> test.game.chooseCloud(playing_player.clientID, finalBad_cloud_index_2),
                                "chooseCloud has not thrown exception while choosing an empty island");
                        assertEquals("Invalid cloud index.", e.getMessage(), "The exception has been thrown, but for the wrong reason");
                    }
                }
                //store value for the following assertions
                int random_cloud;
                do random_cloud = random.nextInt(test.designated_num_of_players);
                while (test.game.getClouds()[random_cloud].isEmpty());
                final int finalRandom_cloud = random_cloud;
                final List<Colors> students_on_cloud = test.game.getClouds()[random_cloud];
                final List<Colors> pre_entrance = playing_player.getDashboard().getEntrance();
                final boolean last_turn = test.game.getPlayerTurn() == test.designated_num_of_players - 1;

                //actually choose cloud
                assertDoesNotThrow(() -> test.game.chooseCloud(playing_player.clientID, finalRandom_cloud), "chooseCloud has thrown exception while player " + playing_player.clientID + " was trying to choose cloud " + random_cloud);

                //check if the students on the chosen cloud have been moved correctly
                final List<Colors> new_entrance = new ArrayList<>(pre_entrance);
                new_entrance.addAll(students_on_cloud);
                assertTrue(playing_player.getDashboard().getEntrance().containsAll(new_entrance), "The students on the chosen cloud have not been moved to the entrance of the playing player");
                //check if the cloud has been emptied (when appropriate)
                if(!last_turn) assertEquals(0, test.game.getClouds()[random_cloud].size(), "The chosen cloud has not been emptied");

                test.hasNothingChangedExcept(0, 2, 3, 5);
                //post move checks
                assertEquals(last_turn ? 0 : 1, test.game.getPhase(), "Phase not updated after the cloud has been chosen");
                assertEquals(0, test.game.getStep(), "Step not updated after the cloud has been chosen");
                e = assertThrows(InvalidMoveException.class,
                        () -> test.game.chooseCloud(test.game.currentlyPlayingPlayer(), random.nextInt()),
                        "moveMotherNature has not thrown exception while trying to play during the wrong game state");
                assertEquals("Cannot be performed in the current game state.", e.getMessage(), "The exception has been thrown, but for the wrong reason");
                //prepare for next turn
                test.autoplayUpTo(1, 2);
            }

        }

        /**
         * Checks the {@link Game#activateEffect} method.
         * <p>
         *     Specifically it asserts:
         *     <li>The method throws an {@link InvalidMoveException} for all the expected instances:
         *     <ul>
         *         <li>A player is trying to play while not being its turn;</li>
         *         <li>Trying to play this action during wrong game state;</li>
         *         <li>Trying to pass an incorrect npc index;</li>
         *         <li>Trying to play this action while in base mode;</li>
         *         <li>Trying to pass a null EffectParameters parameter;</li>
         *         <li>Trying to activated another npc while one is already active;</li>
         *         <li>Trying to play this action with not enough coins for the specific npc;</li>
         *         <li>Trying to pass incorrectly constructed EffectParameters parameters for each specific npc, like:
         *         <ul>
         *             <li>For all npcs (when applicable):</li>
         *             <ul>
         *                 <li>Less arguments than needed (including empty and null);</li>
         *                 <li>Each argument's boundaries based on the current player and game situation (e.g. the
         *                 passed index refers to a hall row where the player has no students currently placed);</li>
         *                 <li>Combinations of correct and incorrect parameters (for npcs which take more then one parameter).</li>
         *             </ul>
         *             <li>Npc 7:</li>
         *             <ul>
         *                 <li>Incongruent number of students provided between the two sets of arguments and the declared quantity;</li>
         *                 <li>Duplicated indexes within the same set of students;</li>
         *                 <li>Students are correctly moved even with passed indexes not naturally ordered.</li>
         *             </ul>
         *         </ul>
         *         </li>
         *     </ul>
         *     </li>
         *     <li>The method does not throw when playing in proper conditions;</li>
         *     <li>The effect of the specific npc is correctly activated and behaves properly, meaning:
         *     <ul>
         *         <li>Npc 1: the chosen is moved to the correct island and the students on the card are refilled;</li>
         *         <li>Npc 2: the professor is correctly assigned in all circumstances;</li>
         *         <li>Npc 3: the islands dispute is properly carried out as if mother nature had been moved to the
         *              specified island right after the npc's activation;</li>
         *         <li>Npc 4: mother nature can actually be moved by 1 or 2 additional moves and not more;</li>
         *         <li>Npc 5: the interdiction is correctly placed on the chosen island, and the next time that
         *              mother nature is placed on that island no islands dispute is carried out;</li>
         *         <li>Npc 6, 8, 9 (npcs that change how the influence is computed): the islands dispute is properly
         *              carried out the next time that  mother nature is moved after this npc's activation;</li>
         *         <li>Npc 7: the students are properly swapped between the card and the player's dashboard;</li>
         *         <li>Npc 10: the students are correctly swapped between the entrance and the related hall row;</li>
         *         <li>Npc 11: the chosen student is correctly moved from the card to the player's dashboard's proper hall
         *              and the students on the card are refilled;</li>
         *         <li>Npc 12: each player gives back the correct number of students of the chosen color.</li>
         *     </ul>
         *     </li>
         *     <li>The cost of the npc has been increased;</li>
         *     <li>Player's coins has been properly decreased;</li>
         *     <li>All other game properties have not changed if not supposed to.</li>
         * </p>
         * @implNote This test runs for all in game npcs and plays the game until it ends,
         * checking all its assertions for every turn up to that moment.
         */
        @DisplayName("Activate effect")
        @ParameterizedTest(name = "{0}")
        @Order(5)
        @MethodSource(arguments_supplier)
        void activateEffect(TestGame test) {
            final boolean[] test_record = new boolean[3];
            boolean no_coins_exception = false;

            while (!test.game.isGameEnded() && !test.game.isLastGameTurn()) {
                test.autoplayUpTo(1, 0);
                if(test.game.isGameEnded()) break;
                //for every not playing player check the related exception
                final List<Player> not_playing_players = test.game.getPlayers().stream().filter(x -> x.clientID != test.game.currentlyPlayingPlayer()).toList();
                for (final Player not_playing_player : not_playing_players) {
                    final Exception e = assertThrows(InvalidMoveException.class,
                            () -> test.game.activateEffect(not_playing_player.clientID, random.nextInt(), null),
                            "activateEffect has not thrown exception while trying to play when was not the player's turn");
                    assertEquals("It's not your turn.", e.getMessage(), "The exception has been thrown, but for the wrong reason");
                }
                //check npc index exception
                int bad_npc_index;
                do bad_npc_index = random.nextInt();
                while(bad_npc_index >= 0 && bad_npc_index <= 2);
                final int finalBad_invalid_npc_index = bad_npc_index;
                Exception e = assertThrows(InvalidMoveException.class,
                        () -> test.game.activateEffect(test.game.currentlyPlayingPlayer(), finalBad_invalid_npc_index, null),
                        "activateEffect has not thrown exception with an invalid npc index");
                assertEquals("Invalid npc index.", e.getMessage(), "The exception has been thrown, but for the wrong reason");
                //check expert mode exception
                if(!test.designated_expert_mode) {
                    e = assertThrows(InvalidMoveException.class,                                            //3 == number of npcs in game
                            () -> test.game.activateEffect(test.game.currentlyPlayingPlayer(), random.nextInt(3), null),
                            "activateEffect has not thrown exception while game was not in expert mode");
                    assertEquals("Expert mode is not active.", e.getMessage(), "The exception has been thrown, but for the wrong reason");
                    return; // quit if not in expert mode
                }
                //check EffectParameters exception
                e = assertThrows(InvalidMoveException.class,                                            //3 == number of npcs in game
                        () -> test.game.activateEffect(test.game.currentlyPlayingPlayer(), random.nextInt(3), null),
                        "activateEffect has not thrown exception with null effect_parameters");
                assertEquals("Null effect parameters.", e.getMessage(), "The exception has been thrown, but for the wrong reason");

                //select the npc to be tested
                boolean all_tested = true;
                for (final boolean npc : test_record) { //check if all in game npcs have been tested
                    if (!npc) {
                        all_tested = false;
                        break;
                    }
                }
                final int npc_index;
                if(test.game.getNpcEffect() != 0) { //if an npc has been already activated in this turn
                    npc_index = IntStream.range(0, test.game.getNpcs().length)
                            .filter(i -> test.game.getNpcs()[i].getId() == test.game.getNpcEffect())
                            .findAny().orElseThrow();
                } else if(all_tested) { //if all in game npcs have been test, choose a random npc to be activated...
                    npc_index = random.nextInt(3); //3 == number of npcs in game
                } else { //...otherwise, pick the first not already tested npc
                    npc_index = IntStream.range(0, test_record.length).filter(i -> !test_record[i]).findFirst().orElseThrow();
                }
                final Npc current_npc = test.game.getNpcs()[npc_index];

                //get enough coins to activate the current npc
                while (test.playingPlayer().getCoins() < current_npc.getCost()) {
                    if(test.game.isGameEnded() || test.game.isLastGameTurn()) break;
                    //move all possible students in the best way possible in order to maximise the chances of getting a new coin
                    do {
                        if(test.playingPlayer().getCoins() < current_npc.getCost()) {
                            //check not enough coins exception
                            final EffectParameters effect_parameters = test.prepareEffectParameters(npc_index);
                            if(effect_parameters != null) {
                                e = assertThrows(InvalidMoveException.class,
                                        () -> test.game.activateEffect(test.game.currentlyPlayingPlayer(), npc_index, effect_parameters),
                                        "Npc activation (npc " + test.game.getNpcs()[npc_index].getId() + ") has not thrown an exception while the playing player was trying to activate the effect having not enough coins");
                                assertEquals("You don't have enough coins.", e.getMessage(), "The exception has been thrown, but for the wrong reason");
                                no_coins_exception = true;
                            }
                        }
                        //calculate the number of students needed to obtain the next coin for every color
                        final Dashboard dashboard = test.playingPlayer().getDashboard();
                        final List<Integer> differences = new ArrayList<>();
                        for (Colors color : Colors.values()) {
                            final int difference = dashboard.getEntrance().contains(color)
                                    //      number of student of color 'color' in player's entrance                        - students needed to obtain the next coin in row
                                    ? (int) (dashboard.getEntrance().stream().filter(student -> student == color).count()) - (3 - dashboard.getHallRow(color) % 3)
                                    : -20; //arbitrary value
                            differences.add(difference);
                        }
                        final int algebraic_max = Collections.max(differences);
                        final int best_score = algebraic_max > 0
                                ? differences.stream().filter(difference -> difference > 0).min(Comparator.naturalOrder()).orElseThrow()
                                : algebraic_max;
                        final List<Colors> candidate_colors = IntStream.range(0, differences.size())
                                .filter(i -> differences.get(i) == best_score)
                                .mapToObj(Colors::fromColorIndex).toList();
                        final Colors chosen_color = candidate_colors.get(random.nextInt(candidate_colors.size()));
                        try {
                            test.game.setStudentToHall(test.game.currentlyPlayingPlayer(), dashboard.getEntrance().indexOf(chosen_color));
                        } catch (InvalidMoveException ex) {
                            fail("internal error: " + ex.getMessage());
                        }
                    } while (test.game.getMovedStudents() > 0);
                    //go to next player's turn if still the currently playing player has not enough coins to activate the selected npc
                    if(test.playingPlayer().getCoins() < current_npc.getCost() || current_npc.getId() == 2)
                        test.autoplayUpTo(1, 0);
                }
                if(test.game.isGameEnded() || test.game.isLastGameTurn()) break;
                if(Stream.of(Colors.values()).allMatch(color -> test.playingPlayer().getDashboard().getHallRow(color) == 0)) {
                    //needed because otherwise some effects would not be testable
                    try {
                        test.randomlyMoveStudentToHall();
                    } catch (InvalidMoveException ex) {
                        fail("internal error: " + ex.getMessage());
                    }
                }

                //check exception for trying to activate the effect with various bad parameters, like empty effectParameters, more arguments then needed...
                final EffectParameters[] bad_effect_parameters = switch (current_npc.getId()) {
                    case 1 -> {
                        int bad_student_index;
                        do bad_student_index = random.nextInt();
                        while (bad_student_index >= 0 && bad_student_index < test.game.getNpcs()[npc_index].getExtraProperty().size());
                        int bad_random_island_index;
                        do bad_random_island_index = random.nextInt();
                        while (bad_random_island_index >= 0 && bad_random_island_index < test.game.getIslands().size());
                        yield new EffectParameters[]{
                                new EffectParameters(),
                                new EffectParameters(bad_student_index, random.nextInt(test.game.getIslands().size())),
                                new EffectParameters(random.nextInt(test.game.getNpcs()[npc_index].getExtraProperty().size()), bad_random_island_index)
                        };
                    }
                    case 3, 5 -> {
                        int bad_random_island_index;
                        do bad_random_island_index = random.nextInt();
                        while (bad_random_island_index >= 0 && bad_random_island_index < test.game.getIslands().size());
                        yield new EffectParameters[]{new EffectParameters(), new EffectParameters(bad_random_island_index)};
                    }
                    case 7 -> {
                        //preparation
                        final List<Integer> students_from_card = new ArrayList<>();
                        final List<Integer> students_from_entrance = new ArrayList<>();
                        final List<Integer> params = new ArrayList<>();
                        //wrong number of students (arg 0)
                        final int bad_number_of_students = random.nextInt(4, 7); //arbitrary upper bound
                        for (int i = 0; i < bad_number_of_students; i++) {
                            students_from_card
                                    .add(random.nextInt(
                                            test.game.getNpcs()[npc_index].getExtraProperty().size()));
                            students_from_entrance
                                    .add(random.nextInt(
                                            test.playingPlayer().getDashboard().getEntrance().size()));
                        }
                        params.add(bad_number_of_students);
                        params.addAll(students_from_card);
                        params.addAll(students_from_entrance);
                        final EffectParameters EP1 = new EffectParameters(params.toArray(new Integer[0]));
                        students_from_card.clear();
                        students_from_entrance.clear();
                        params.clear();
                        //duplicated indexes + last indexes
                        int number_of_students = random.nextInt(2, 4);
                        for (int i = 0; i < number_of_students; i++) {
                            students_from_card
                                    .add(test.game.getNpcs()[npc_index].getExtraProperty().size() - 1);
                            students_from_entrance
                                    .add(test.playingPlayer().getDashboard().getEntrance().size() - 1);
                        }
                        params.add(number_of_students);
                        params.addAll(students_from_card);
                        params.addAll(students_from_entrance);
                        final EffectParameters EP2 = new EffectParameters(params.toArray(new Integer[0]));
                        students_from_card.clear();
                        students_from_entrance.clear();
                        params.clear();
                        //inconsistent number of students (smaller)
                        number_of_students = random.nextInt(1, 4);
                        for (int i = 0; i < number_of_students; i++) {
                            students_from_card
                                    .add(random.nextInt(
                                            test.game.getNpcs()[npc_index].getExtraProperty().size()));
                            students_from_entrance
                                    .add(random.nextInt(
                                            test.playingPlayer().getDashboard().getEntrance().size()));
                        }
                        params.add(number_of_students);
                        params.addAll(students_from_card);
                        params.addAll(students_from_entrance);
                        params.remove(random.nextInt(1, params.size()));
                        final EffectParameters EP3 = new EffectParameters(params.toArray(new Integer[0]));
                        students_from_card.clear();
                        students_from_entrance.clear();
                        params.clear();
                        //invalid student indexes
                        number_of_students = random.nextInt(1, 4);
                        for (int i = 0; i < number_of_students; i++) {
                            students_from_card.add(random.nextInt());
                            students_from_entrance.add(random.nextInt());
                        }
                        params.add(number_of_students);
                        params.addAll(students_from_card);
                        params.addAll(students_from_entrance);
                        final EffectParameters EP4 = new EffectParameters(params.toArray(new Integer[0]));
                        yield new EffectParameters[]{new EffectParameters(), EP1, EP2, EP3, EP4};
                    }
                    case 9, 12 -> {
                        int bad_index_color;
                        do bad_index_color = random.nextInt();
                        while (bad_index_color >= 0 && bad_index_color < Colors.values().length);
                        yield new EffectParameters[]{new EffectParameters(), new EffectParameters(bad_index_color)};
                    }
                    case 10 -> {
                        int bad_entrance_index;
                        do bad_entrance_index = random.nextInt();
                        while (bad_entrance_index >= 0 && bad_entrance_index < test.playingPlayer().getDashboard().getEntrance().size());
                        int bad_row_index;
                        do bad_row_index = random.nextInt();
                        while (bad_row_index >= 0 && bad_row_index < Colors.values().length);
                        final Colors bad_row_color = Stream.of(Colors.values())
                                .filter(color -> test.playingPlayer().getDashboard().getHallRow(color) == 0)
                                .findAny().orElse(null);
                        yield new EffectParameters[]{
                                new EffectParameters(),
                                new EffectParameters(bad_entrance_index, Stream.of(Colors.values()).filter(color -> test.playingPlayer().getDashboard().getHallRow(color) != 0).findAny().orElseThrow().index),
                                new EffectParameters(random.nextInt(test.playingPlayer().getDashboard().getEntrance().size()), bad_row_index),
                                bad_row_color != null
                                        ? new EffectParameters(random.nextInt(test.playingPlayer().getDashboard().getEntrance().size()), bad_row_color.index)
                                        : new EffectParameters()
                        };
                    }
                    case 11 -> {
                        int bad_student_index;
                        do bad_student_index = random.nextInt();
                        while (bad_student_index >= 0 && bad_student_index < test.game.getNpcs()[npc_index].getExtraProperty().size());
                        yield new EffectParameters[]{new EffectParameters(), new EffectParameters(bad_student_index)};
                    }
                    default -> null;
                };
                if(bad_effect_parameters != null) {
                    for (final EffectParameters badEPs : bad_effect_parameters) {
                        e = assertThrows(InvalidMoveException.class,
                                () -> test.game.activateEffect(test.game.currentlyPlayingPlayer(), npc_index, badEPs));
                        assertTrue(e.getMessage().contains("The effect could not be activated due to bad parameters"),
                                "The exception has been thrown, but for the wrong reason");
                    }
                }

                //prepare for activation
                final Game pre_activation = test.game.copy();
                final int playing_clientID = test.game.currentlyPlayingPlayer();
                final EffectParameters effect_parameters = test.prepareEffectParameters(npc_index);
                //activate the npc effect
                assertDoesNotThrow(() -> test.game.activateEffect(playing_clientID, npc_index, effect_parameters),
                        "activateEffect has thrown an exception while player " + playing_clientID + " was trying to activate npc " + current_npc.getId() + " with effects: " + effect_parameters.args);

                //post activation assertions
                assertEquals(test.game.getNpcEffect(), current_npc.getId(), "The npc has not been activated");
                assertEquals(pre_activation.getNpcs()[npc_index].getCost() + 1, current_npc.getCost(), "Cost of npc " + current_npc.getId() + " has not been increased");
                assertEquals(pre_activation.getPlayers().get(TestGame.playingPlayer(pre_activation).player_index).getCoins() - pre_activation.getNpcs()[npc_index].getCost(), test.playingPlayer().getCoins(),
                        "Player coins not updated correctly after activating npc " + current_npc.getId());
                //check exception if a npcs has already been activated
                e = assertThrows(InvalidMoveException.class,
                        () -> test.game.activateEffect(playing_clientID, npc_index, effect_parameters),
                        "activateEffect has not thrown exception while another player has already activated an effect");
                assertEquals("There has already been an effect activation this turn.", e.getMessage(), "The exception has been thrown, but for the wrong reason");

                //test the correct effects based on the activated npc
                switch (test.game.getNpcEffect()) {
                    case 1 -> {
                        final int color = pre_activation.getNpcs()[npc_index].getExtraProperty().get(effect_parameters.args.get(0));
                        assertEquals(pre_activation.getIslands().get(effect_parameters.args.get(1)).getStudents(color) + 1,
                                test.game.getIslands().get(effect_parameters.args.get(1)).getStudents(color), "Npc 1: the student has not been moved to the correct island");
                        if(!test.game.isLastGameTurn())
                            assertEquals(4, current_npc.getExtraProperty().size(), "Npc 1: students on the card has not been replaced after its use");
                        test_record[npc_index] = true;
                    }
                    case 2 -> {
                        if(test.game.getStep() == 1)
                            break;
                        test.autoplayUpTo(1, 1);
                        for(final Colors color : Colors.values()) {
                            if(pre_activation.getPlayers()
                                        .get(TestGame.playingPlayer(pre_activation).player_index)
                                        .getDashboard().getHallRow(color)
                                    == test.playingPlayer().getDashboard().getHallRow(color))
                                continue;
                            final Player owner = pre_activation.getPlayers().stream()
                                    .filter(player -> player.getDashboard().getProfessor(color))
                                    .findAny().orElse(null);
                            if(owner != null && test.playingPlayer().getDashboard().getHallRow(color) == owner.getDashboard().getHallRow(color))
                                assertTrue(test.playingPlayer().getDashboard().getProfessor(color),
                                        "Npc 2: professor has not been assigned to the playing player even if it has the same number of students of color " + color + " as previous owner");
                        }
                        test_record[npc_index] = true;
                    }
                    case 3 -> {
                        test.autoplayUpTo(1, 1);
                        if(test.game.isGameEnded())
                            break;
                        final Game post_activation = test.game.copy();
                        int mother_nature_moves = -1;
                        try {
                            mother_nature_moves = test.randomlyMoveMotherNature();
                        } catch (InvalidMoveException ex) {
                            fail("internal error: " + ex.getMessage());
                        }
                        int mother_nature_new_position = (post_activation.getMotherNature() + mother_nature_moves) % post_activation.getIslands().size();
                        Map<String, Object> result = TestGame.disputeIsland(post_activation, mother_nature_new_position);
                        if((int) result.get("result") == 3) //game is ended due to islands.size() <= 3
                            break;
                        Island new_island = test.game.getIslands().get((int) result.get("index"));
                        assertAll("Npc " + test.game.getNpcEffect() + ": new island(s) state",
                                () -> assertEquals((Integer) result.get("owner_index"), new_island.getOwnerIndex(), "Wrong owner index"),
                                () -> assertEquals((int) result.get("num_of_merged_islands"), new_island.getNumOfMergedIslands(), "wrong number of merged island(s)"),
                                () -> assertEquals((boolean) result.get("interdiction"), new_island.getInterdiction(), "Wrong interdiction value"),
                                () -> {
                                    int[] students = (int[]) result.get("students");
                                    for(Colors color : Colors.values())
                                        assertEquals(students[color.index], new_island.getStudents(color), "Wrong students");
                                });
                        test_record[npc_index] = true;
                    }
                    case 4 -> {
                        test.autoplayUpTo(1, 1);
                        if(test.game.isGameEnded())
                            break;
                        e = assertThrows(InvalidMoveException.class,
                                () -> test.game.moveMotherNature(playing_clientID, test.playingPlayer().getLastCardPlayed().movements_value + 3),
                                "Npc 4: exception has not been thrown while trying to move mother nature by 3 additional moves");
                        assertEquals("Invalid moves number.", e.getMessage(), "The exception has been thrown, but for the wrong reason");
                        final int additional_moves = random.nextInt(1, 3);
                        assertDoesNotThrow(() -> test.game.moveMotherNature(playing_clientID, test.playingPlayer().getLastCardPlayed().movements_value + additional_moves),
                                "Npc 4: moveMotherNature has thrown an exception while trying to move mother nature by " + additional_moves + " additional move" + (additional_moves > 1 ? "s" : ""));
                        test_record[npc_index] = true;
                    }
                    case 5 -> {
                        int interdicted_island_index = effect_parameters.args.get(0);
                        assertEquals(pre_activation.getNpcs()[npc_index].getExtraProperty().get(0) - 1, test.game.getNpcs()[npc_index].getExtraProperty().get(0), "Npc 5: interdictions on the npc card have not been decreased");
                        assertTrue(test.game.getIslands().get(interdicted_island_index).getInterdiction(), "Npc 5: interdiction has not been set on island " + interdicted_island_index);
                        test.autoplayUpTo(1, 1);
                        if(test.game.isGameEnded())
                            break;
                        int required_moves = (interdicted_island_index - test.game.getMotherNature() + test.game.getIslands().size())
                                % test.game.getIslands().size();
                        if(required_moves == 0)
                            required_moves = test.game.getIslands().size();
                        int available_moves = test.playingPlayer().getLastCardPlayed().movements_value;
                        /*
                        while the interdicted island is 'to far away' (or it is the same island where mother nature is currently placed and the currently playing player
                        has not enough available moves to move mother nature back to the same island), try playing maximising the available mother nature moves
                         */
                        while (required_moves > available_moves) {
                            try {
                                test.game.moveMotherNature(test.game.currentlyPlayingPlayer(), available_moves);
                            } catch (InvalidMoveException ex) {
                                fail("internal error: " + ex.getMessage());
                            }
                            if(test.game.isGameEnded())
                                break;
                            interdicted_island_index = IntStream.range(0, test.game.getIslands().size())
                                    .filter(i -> test.game.getIslands().get(i).getInterdiction())
                                    .findFirst().orElseThrow();
                            if(!test.game.isLastGameTurn()) {
                                try {
                                    test.randomlyChooseCloud();
                                } catch (InvalidMoveException ex) {
                                    fail("internal error: " + ex.getMessage());
                                }
                            }
                            if(test.game.getPhase() == 0) {
                                for(final Player playing_player : test.orderedPlayers()) {
                                    boolean condition = false;
                                    int max_card = playing_player.getCards().size() - 1;
                                    do {
                                        try {
                                            test.game.playCard(playing_player.clientID, max_card);
                                            condition = false;
                                        } catch (InvalidMoveException ex) {
                                            if(!ex.getMessage().equals("A card with the same value has already been played."))
                                                fail("internal error: " + ex.getMessage());
                                            condition = true;
                                            max_card--;
                                        }
                                    } while(condition && max_card >= 0);
                                }
                            }
                            test.autoplayUpTo(1, 1);
                            if(test.game.isGameEnded())
                                break;
                            required_moves = (interdicted_island_index - test.game.getMotherNature() + test.game.getIslands().size())
                                    % test.game.getIslands().size();
                            if(required_moves == 0)
                                required_moves = test.game.getIslands().size();
                            available_moves = test.playingPlayer().getLastCardPlayed().movements_value;
                        }
                        if(test.game.isGameEnded())
                            break;
                        Island interdicted_island = test.game.getIslands().get(interdicted_island_index);
                        try {
                            test.game.moveMotherNature(test.game.currentlyPlayingPlayer(), required_moves);
                        } catch (InvalidMoveException ex) {
                            fail("moveMotherNature has thrown an exception: " + ex.getMessage());
                        }
                        assertEquals(interdicted_island_index, test.game.getMotherNature(), "Internal error: mother was not moved correctly");
                        assertEquals(interdicted_island.getOwnerIndex(), test.game.getIslands().get(interdicted_island_index).getOwnerIndex(),
                                "Npc 5: the influence has been computed even with the interdiction");
                        test_record[npc_index] = true;
                    }
                    case 6, 8, 9 -> {
                        test.autoplayUpTo(1, 1);
                        if(test.game.isGameEnded())
                            break;
                        final Game post_activation = test.game.copy();
                        int mother_nature_moves = -1;
                        try {
                            mother_nature_moves = test.randomlyMoveMotherNature();
                        } catch (InvalidMoveException ex) {
                            fail("internal error: " + ex.getMessage());
                        }
                        int mother_nature_new_position = (pre_activation.getMotherNature() + mother_nature_moves)
                                % pre_activation.getIslands().size();
                        Map<String, Object> result = TestGame.disputeIsland(post_activation, mother_nature_new_position);
                        if((int) result.get("result") == 3) //game is ended due to islands.size() <= 3
                            break;
                        Island new_island = test.game.getIslands().get((int) result.get("index"));
                        assertAll("Npc " + test.game.getNpcEffect() + ": new island(s) state",
                                () -> assertEquals((Integer) result.get("owner_index"), new_island.getOwnerIndex(), "Wrong owner index"),
                                () -> assertEquals((int) result.get("num_of_merged_islands"), new_island.getNumOfMergedIslands(), "wrong number of merged island(s)"),
                                () -> assertEquals((boolean) result.get("interdiction"), new_island.getInterdiction(), "Wrong interdiction value"),
                                () -> {
                                    int[] students = (int[]) result.get("students");
                                    for(Colors color : Colors.values())
                                        assertEquals(students[color.index], new_island.getStudents(color), "Wrong students");
                                });
                        test_record[npc_index] = true;
                    }
                    case 7 -> {
                        final List<Colors> new_dashboard_entrance = pre_activation.getPlayers().get(TestGame.playingPlayer(pre_activation).player_index).getDashboard().getEntrance();
                        final List<Integer> new_npc_students = pre_activation.getNpcs()[npc_index].getExtraProperty();
                        final List<Integer> args = new ArrayList<>(effect_parameters.args);
                        final int number_of_students = args.remove(0);
                        final List<Integer> npc_to_entrance = args.subList(0, number_of_students);
                        final List<Integer> entrance_to_npc = args.subList(number_of_students, args.size());
                        Collections.sort(npc_to_entrance);
                        Collections.sort(entrance_to_npc);
                        for (int i = number_of_students - 1; i >= 0; i--) {
                            new_dashboard_entrance.remove(entrance_to_npc.get(i).intValue());
                            new_npc_students.remove(npc_to_entrance.get(i).intValue());
                        }
                        new_dashboard_entrance.addAll(
                                npc_to_entrance.stream()
                                        .map(x -> Colors.fromColorIndex(
                                                pre_activation.getNpcs()[npc_index].getExtraProperty().get(x)))
                                        .toList());
                        new_npc_students.addAll(
                                entrance_to_npc.stream()
                                        .map(x -> pre_activation.getPlayers()
                                                .get(TestGame.playingPlayer(pre_activation).player_index)
                                                .getDashboard().getEntrance()
                                                .get(x).index)
                                        .toList());
                        assertTrue(test.playingPlayer().getDashboard().getEntrance().containsAll(new_dashboard_entrance), "Npc 7: player's entrance has not been updated correctly");
                        assertTrue(new_dashboard_entrance.containsAll(test.playingPlayer().getDashboard().getEntrance()), "Npc 7: player's entrance has not been updated correctly");
                        assertTrue(test.game.getNpcs()[npc_index].getExtraProperty().containsAll(new_npc_students), "Npc 7: students on card have not been updated correctly");
                        assertTrue(new_npc_students.containsAll(test.game.getNpcs()[npc_index].getExtraProperty()), "Npc 7: students on card have not been updated correctly");
                        test_record[npc_index] = true;
                    }
                    case 10 -> {
                        final int entrance_index_to_be_removed = effect_parameters.args.get(0);
                        final int hall_color_to_be_removed = effect_parameters.args.get(1);
                        final List<Colors> new_entrance = pre_activation.getPlayers().get(TestGame.playingPlayer(pre_activation).player_index).getDashboard().getEntrance();
                        Colors color_for_hall_2 = new_entrance.remove(entrance_index_to_be_removed);
                        new_entrance.add(Colors.fromColorIndex(hall_color_to_be_removed));
                        int new_hall_1 = pre_activation.getPlayers().get(TestGame.playingPlayer(pre_activation).player_index).getDashboard().getHallRow(hall_color_to_be_removed);
                        int new_hall_2 = pre_activation.getPlayers().get(TestGame.playingPlayer(pre_activation).player_index).getDashboard().getHallRow(color_for_hall_2);
                        assertTrue(test.playingPlayer().getDashboard().getEntrance().containsAll(new_entrance), "Npc 10: player's entrance has not been updated correctly");
                        assertTrue(new_entrance.containsAll(test.playingPlayer().getDashboard().getEntrance()), "Npc 10: player's entrance has not been updated correctly");
                        if(hall_color_to_be_removed == color_for_hall_2.index) {
                            assertEquals(new_hall_1, test.playingPlayer().getDashboard().getHallRow(hall_color_to_be_removed),
                                    "Npc 10: player's hall of color " + Colors.fromColorIndex(hall_color_to_be_removed) + " has not been updated correctly");
                        } else {
                            assertEquals(new_hall_1 - 1, test.playingPlayer().getDashboard().getHallRow(hall_color_to_be_removed),
                                    "Npc 10: player's hall of color " + Colors.fromColorIndex(hall_color_to_be_removed) + " has not been updated correctly");
                            assertEquals(new_hall_2 + 1, test.playingPlayer().getDashboard().getHallRow(color_for_hall_2),
                                    "Npc 10: player's hall of color " + Colors.fromColorIndex(hall_color_to_be_removed) + " has not been updated correctly");
                        }
                        test_record[npc_index] = true;
                    }
                    case 11 -> {
                        final int color_to_add = pre_activation.getNpcs()[npc_index].getExtraProperty().get(effect_parameters.args.get(0));
                        assertEquals(pre_activation.getPlayers().get(TestGame.playingPlayer(pre_activation).player_index).getDashboard().getHallRow(color_to_add) + 1, test.playingPlayer().getDashboard().getHallRow(color_to_add),
                                "Npc 11: the student has not been placed in the related player's hall");
                        if(!test.game.isLastGameTurn())
                            assertEquals(4, current_npc.getExtraProperty().size(), "Npc 10: students on card have not been refilled after use");
                        test_record[npc_index] = true;
                    }
                    case 12 -> {
                        final int color = effect_parameters.args.get(0);
                        for (int i = 0; i < test.designated_num_of_players; i++) {
                            int pre_number = pre_activation.getPlayers().get(i).getDashboard().getHallRow(color);
                            int removed = Math.min(pre_number, 3);
                            assertEquals(pre_number - removed, test.game.getPlayers().get(i).getDashboard().getHallRow(color),
                                    "Npc 12: player " + i + " has not given back the correct amount of students");
                        }
                        test_record[npc_index] = true;
                    }
                }
                //go to next step
                test.autoplayUpTo(1, 2);
            }
            if(debug) {
                for (int i = 0; i < test_record.length; i++)
                    if (!test_record[i]) System.err.println("Could not test npc " + test.game.getNpcs()[i].getId());
                if(!no_coins_exception)
                    System.err.println("Could not test not enough coins exception");
            }
        }

        /**
         * Checks the game ending and the winner computation.
         * <p>
         *     Specifically it asserts:
         *     <li>The game ends when it should do so, meaning:
         *     <ul>
         *         <li>At the end of the turn where a players has played its last card;</li>
         *         <li>Immediately when a player builds its last rook;</li>
         *         <li>At the end of the turn where there are no more available students in the pouch;</li>
         *         <li>Immediately when there are three or less groups of islands left.</li>
         *     </ul>
         *     </li>
         *     <li>The winner is correctly computed in all the circumstances, meaning:
         *     <ul>
         *         <li>There is no tie on the number of owned islands</li>
         *         <li>There is a tie on the number of owned islands</li>
         *         <li>There is a tie on the number of owned islands and on the number of owned professors.</li>
         *     </ul>
         *     </li>
         * </p>
         * @implNote This test plays the game until it should end, checks its assertions and then computes the winner.
         */
        @DisplayName("Game ending and winner")
        @ParameterizedTest(name = "{0}")
        @Order(6)
        @MethodSource(arguments_supplier)
        void gameEndingAndWinner(TestGame test) {
            //list of in game npcs that require students on them
            boolean game_ends_at_next_loop = false;
            List<Npc> npcs_with_students = test.designated_expert_mode
                    ? Arrays.stream(test.game.getNpcs())
                        .filter(npc -> switch (npc.getId()) {
                            case 1, 7, 11 -> true;
                            default -> false;
                        }).toList()
                    : new ArrayList<>(0);

            //cycle until one of the conditions for the game to be ending is reached
            while(!test.game.isGameEnded()) {
                //check if there are no more available students at the beginning of the turn,  checking... if
                if (test.game.getPhase() == 0) {
                        //...a cloud has not been completely refilled or...
                    if(Arrays.stream(test.game.getClouds()).limit(test.designated_num_of_players)
                                .anyMatch(cloud -> cloud.size() < (test.designated_num_of_players == 3 ? 4 : 3))
                            //...a npc with students has not been completely refilled
                            || npcs_with_students.stream()
                                .anyMatch(npc -> npc.getExtraProperty().size() < (npc.getId() == 7 ? 6 : 4)))
                    {
                        assertTrue(test.game.isLastGameTurn(), "Last turn has not been set while having no more students");
                    }
                }

                //if a player has no more cards to play
                if(test.game.getPlayers().stream()
                        .anyMatch(player -> player.getCards().isEmpty()))
                {
                    assertTrue(test.game.isLastGameTurn(), "Last turn has not been set while players have played their last card");
                }

                //check if the game is in a state in which it should have been ended
                //if a player has 0 rooks left on its dashboard
                if(test.game.getPlayers().stream()
                            .anyMatch(player -> player.getDashboard().getRooks() <= 0)
                        //there are 3 or fewer groups of islands
                        || test.game.getIslands().size() <= 3
                        //it is the last turn and all the players have played
                        || game_ends_at_next_loop)
                {
                    assertTrue(test.game.isGameEnded(),
                            "Game is not ended while a player has placed its last rook or there are 3 or fewer group of islands or it was the last turn");
                    break;
                }
                //memorize if the next loop (== game move) should be the last one
                //if the turn is marked as the last one...
                if(test.game.isLastGameTurn() && test.game.getStep() == 1
                        //...and it is last player's turn
                        && test.game.currentPlayersTurnOrder().get(test.designated_num_of_players - 1) == test.game.currentlyPlayingPlayer())
                {
                    game_ends_at_next_loop = true;
                }
                //play next move (based on the current game state)
                switch (test.game.getPhase()) {
                    case 0 -> test.autoplayUpTo(1, 0);
                    case 1 -> {
                        switch (test.game.getStep()) {
                            case 0 -> test.autoplayUpTo(1, 1);
                            case 1 -> test.autoplayUpTo(1, test.game.isLastGameTurn() ? 0 : 2);
                            case 2 -> {
                                final int next_phase = test.game.currentPlayersTurnOrder().get(test.designated_num_of_players - 1) == test.game.currentlyPlayingPlayer()
                                        ? 0 : 1;
                                test.autoplayUpTo(next_phase, 0);
                            }
                        }
                    }
                }
            }

            //check winner id
            final int winner_id;
            //count number of owned islands per player
            int[] owned_islands = new int[test.designated_num_of_players];
            for (int i = 0; i < owned_islands.length; i++) {
                for (final Island island : test.game.getIslands()) {
                    if (Objects.equals(island.getOwnerIndex(), i))
                        owned_islands[i] += island.getNumOfMergedIslands();
                }
            }
            //check if there is a tie on the number of owned islands
            final int max_islands = Arrays.stream(owned_islands).max().orElseThrow();
            final List<Player> candidate_winners = IntStream.range(0, owned_islands.length)
                    .filter(i -> owned_islands[i] == max_islands) //only if the score matches max_islands
                    .mapToObj(i -> test.game.getPlayers().get(i)) //map each score to its player
                    .toList();
            //if there is no tie (on the number of owned islands)
            if(candidate_winners.size() == 1) {
                winner_id = candidate_winners.get(0).clientID;
                assertEquals(winner_id, test.game.getWinnerID(), "The winner has not been computed correctly");
            }
            //if there is a tie...
            else {
                //...count the number of owned professors per player
                List<Long> owned_professors = candidate_winners.stream()
                        .map(player -> Arrays.stream(Colors.values())                       //map each candidate player
                                .filter(color -> player.getDashboard().getProfessor(color)) //to its number of owned professors
                                .count())
                        .toList();
                final long max_owned_professors = Collections.max(owned_professors);
                //if the is no tie (on the number of owned professors)
                if(owned_professors.stream().filter(x -> x == max_owned_professors).count() == 1) {
                    winner_id = candidate_winners.get(owned_professors.indexOf(max_owned_professors)).clientID;
                    assertEquals(winner_id, test.game.getWinnerID(),
                            "The winner has not been computed correctly while there was a tie on the number islands owned");
                }
                //if there is a tie even on the number of owned professors...
                else {
                    //...check that the game computed winner corresponds to one of the candidate players
                    assertTrue(candidate_winners.stream()
                            .map(player -> player.clientID)
                            .toList()
                            .contains(test.game.getWinnerID()),
                            "The winner was not one of the correct candidates " +
                                    "while there was a tie both on the number of owned islands and the number of owned professor");
                }
            }

        }

    }

    /**
     * Test class that checks between game turns:
     * <li>The proper players order in both phase 0 and phase 1 with the {@link TransversalTests#playersOrder};</li>
     * <li>The proper game serialization and reversion throughout all game states with the {@link TransversalTests#serializationAndReversion};</li>
     * <li>Everything is prepared for the next turn with the {@link TransversalTests#nextTurn};</li>
     * <li>The internal action of skipping a turn when a player disconnects with the {@link TransversalTests#skipTurn}.</li>
     * <p>
     *     See their documentations for details.
     * </p>
     */
    @Nested
    @DisplayName("General state")
    @TestMethodOrder(MethodOrderer.OrderAnnotation.class)
    @TestClassOrder(ClassOrderer.OrderAnnotation.class)
    @Order(3)
    public class TransversalTests {

        /**
         * Checks the correct players ordering both at the beginning of a new turn and during the game phase 1.
         * @implNote This test plays the game until it ends and checks all its related assertions for every
         * turn up to that moment.
         */
        @DisplayName("Players order")
        @ParameterizedTest(name = "{0}")
        @MethodSource(arguments_supplier)
        void playersOrder(TestGame test) {
            List<Player> correct_order = null;
            List<Player> order_from_phase_0 = test.orderedPlayers();

            while(!test.game.isGameEnded()) {
                //check players order in phase 1 for all players
                test.autoplayUpTo(1, 0);
                for (int i = test.orderedPlayers().indexOf(test.playingPlayer()); i < test.designated_num_of_players; i++) {
                    correct_order = order_from_phase_0.stream()
                            .sorted(Comparator.comparingInt(player -> player.getLastCardPlayed().order_value)).toList();
                    assertEquals(correct_order.stream().map(player -> player.clientID).toList(), test.game.currentPlayersTurnOrder(), "Incorrect players order during phase 1, step 0");
                    test.autoplayUpTo(1, 0);
                }
                //check players order in phase 0
                test.autoplayUpTo(0,0);
                if(test.game.isGameEnded() || test.game.isLastGameTurn()) return;
                order_from_phase_0.clear();
                for(int j = 0; j < test.designated_num_of_players; j++)
                    order_from_phase_0.add(test.game.getPlayers().get((correct_order.get(0).player_index + j) % test.designated_num_of_players));
                assertEquals(order_from_phase_0.stream().map(player -> player.clientID).toList(), test.game.currentPlayersTurnOrder(), "Incorrect players order during phase 0");
            }
        }

        /**
         * Checks both the game serialization and the reversion (meaning the undoing all the possible actions
         * and moves played in the current state, bringing the game back to a designated state) throughout all game states,
         * using respectively the {@link Game#getGameSerialization} and the {@link Game#revertToPreviousState} methods.
         * @implNote This test plays the game until it ends and checks all its related assertions for every turn up to
         * that moment.
         */
        @DisplayName("Serialization and reversion")
        @ParameterizedTest(name = "{0}")
        @MethodSource(arguments_supplier)
        void serializationAndReversion(TestGame test) {
            //prepare the TestGame and its game
            if(test.game.getPhase() == 0) test.autoplayUpTo(1, 0);
            test.autoplayUpTo(0, 0);

            while(!test.game.isGameEnded()) {
                //prepare for test
                test.updateOldGameCopy();
                byte[] internal_copy = test.game.getGameSerialization();

                //serialization in phase 0
                try {
                    test.randomlyPlayCard();
                } catch (InvalidMoveException e) {
                    fail("Could not change game state to verify game serialization: " + e.getMessage());
                }
                assertNotEquals(internal_copy, test.game.getGameSerialization(), "Serialization not updated after something has changed");
                Game original_game = test.game;
                Game reverted_game = Game.revertToPreviousState(test.game);
                assertArrayEquals(internal_copy, reverted_game.getGameSerialization(), "Wrong reverted game serialization in phase 0");
                test.game = reverted_game;
                test.hasNothingChangedExcept();
                //prepare for next state
                test.game = original_game;
                test.autoplayUpTo(1, 0);
                internal_copy = test.game.getGameSerialization();
                test.updateOldGameCopy();

                //serialization in phase 1 step 0
                try {
                    test.randomlyMoveStudentToIsland();
                    test.randomlyMoveStudentToIsland();
                } catch (InvalidMoveException e) {
                    fail("Could not change game state to verify game serialization: " + e.getMessage());
                }
                assertNotEquals(internal_copy, test.game.getGameSerialization(), "Serialization not updated after something has changed");
                original_game = test.game;
                reverted_game = Game.revertToPreviousState(test.game);
                assertArrayEquals(internal_copy, reverted_game.getGameSerialization(), "Wrong reverted game serialization in phase 1 step 0");
                test.game = reverted_game;
                test.hasNothingChangedExcept();
                //prepare for next state
                test.game = original_game;
                test.autoplayUpTo(1, 1);

                //serialization in phase 1 step 1
                try {
                    test.randomlyMoveMotherNature();
                } catch (InvalidMoveException e) {
                    fail("Could not change game state to verify game serialization: " + e.getMessage());
                }
                if(test.game.isGameEnded() || test.game.isLastGameTurn()) return;
                assertNotEquals(internal_copy, test.game.getGameSerialization(), "Serialization not updated after something has changed");
                original_game = test.game;
                reverted_game = Game.revertToPreviousState(test.game);
                assertArrayEquals(internal_copy, reverted_game.getGameSerialization(), "Wrong reverted game serialization in phase 1 step 1");
                test.game = reverted_game;
                test.hasNothingChangedExcept();
                //prepare for next state
                test.game = original_game;
                test.autoplayUpTo(1, 2);

                //serialization in phase 1 step 2
                try {
                    test.randomlyChooseCloud();
                } catch (InvalidMoveException e) {
                    fail("Could not change game state to verify game serialization: " + e.getMessage());
                }
                assertNotEquals(internal_copy, test.game.getGameSerialization(), "Serialization not updated after something has changed");
                assertArrayEquals(test.game.getGameSerialization(), Game.revertToPreviousState(test.game).getGameSerialization(), "Wrong reverted game serialization in phase 1 step 2");

                //double reverting
                reverted_game = Game.revertToPreviousState(test.game);
                assertEquals(reverted_game, Game.revertToPreviousState(reverted_game), "revertToPreviousState has not returned same game while having a null serialized_game_copy");

                //prepare for next turn
                test.autoplayUpTo(0, 0);
            }
        }

        /**
         * Checks that all the clouds have been refilled (when possible and needed) at the end of a turn and
         * that the turn counter has been increased after all players have played all phase 1's steps.
         * @implNote This test plays the game until it ends and checks all its related assertions for every
         * turn up to that moment.
         */
        @DisplayName("Next turn")
        @ParameterizedTest(name = "{0}")
        @MethodSource(arguments_supplier)
        void nextTurn(TestGame test) {
            while (!test.game.isGameEnded()) {
                final int original_turn = test.game.getGameTurn();
                //prepare game
                test.autoplayUpTo(1, 0);
                test.autoplayUpTo(0, 0);
                if(test.game.isGameEnded())
                    return;

                assertEquals(original_turn + 1, test.game.getGameTurn(), "Game turn not updated correctly after every player have played");
                if (!test.game.isLastGameTurn()) {
                    for (int i = 0; i < test.designated_num_of_players; i++)
                        assertEquals(test.designated_num_of_players == 2 ? 3 : 4, test.game.getClouds()[i].size(), "Wrong number of students on cloud " + i);
                }
            }
        }

        /**
         * Checks the behaving of the {@link Game#skipTurn} mechanism, designated to
         * go to the next step/phase/turn, throughout all game states.
         * @implNote This test plays the game until it ends and checks all its related assertions for every
         * turn up to that moment.
         */
        @DisplayName("Skip turn")
        @ParameterizedTest(name = "{0}")
        @MethodSource(arguments_supplier)
        void skipTurn(TestGame test) {
            while (!test.game.isGameEnded()) {
                switch (test.game.getPhase()) {
                    case 0 -> {
                        final boolean going_to_step_1 = test.game.currentlyPlayingPlayer() == test.orderedPlayers().get(test.designated_num_of_players - 1).clientID;
                        final int previous_player = test.game.currentlyPlayingPlayer();
                        final int cards_size = test.playingPlayer().getCards().size();
                        try {
                            test.game.skipTurn();
                        } catch (InvalidMoveException e) {
                            fail("skipTurn returned an exception: " + e.getMessage());
                        }
                        assertEquals(cards_size - 1, test.orderedPlayers().get(0).getCards().size(), "skipTurn has not automatically played a card for the previous playing player");
                        if(!going_to_step_1)
                            assertNotEquals(previous_player, test.game.currentlyPlayingPlayer(), "skipTurn has not moved to the next player");
                        else
                            assertEquals(1, test.game.getPhase(), "skipTurn has not moved to the phase");
                    }
                    case 1 -> {
                        final int expected_turn = test.game.getGameTurn() +
                                (test.game.currentlyPlayingPlayer() == test.orderedPlayers().get(test.designated_num_of_players - 1).clientID
                                ? 1 : 0);
                        final int prev_player = test.game.currentlyPlayingPlayer();
                        try {
                            test.game.skipTurn();
                        } catch (InvalidMoveException e) {
                            fail("skipTurn returned an exception: " + e.getMessage());
                        }

                        if(!test.game.isGameEnded()) {
                            assertNotEquals(prev_player, test.game.currentlyPlayingPlayer(), "skipTurn has not moved to the next player");
                            assertEquals(0, test.game.getMovedStudents(), "moved_students counter has not been reset after skipping turn");
                            assertEquals(expected_turn, test.game.getGameTurn(), "skipTurn: wrong turn");
                        }
                    }
                }
            }
        }
    }
}
