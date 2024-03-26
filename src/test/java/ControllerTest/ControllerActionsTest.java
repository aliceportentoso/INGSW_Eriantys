package ControllerTest;

import Controller.ClientSide.Client;
import Controller.ServerSide.ServerLobby;
import Model.Game;
import Model.GameState;
import Model.Player;
import ModelTest.GameTests;
import org.junit.jupiter.api.*;

import java.util.*;
import java.util.stream.IntStream;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Test class designated to test the Controller package.
 * This class uses customized {@link Controller.Controller}s object in order to allow their testing. Specifically, it
 * uses:
 * <li>The {@link TestServer} object instead of a standard {@link Controller.ServerSide.Server}</li>
 * <li>The {@link TestClient} object instead of a standard {@link Client}</li>
 * <li>The {@link TestServerLobby} object instead of a standard {@link ServerLobby}</li>
 * To simulate the functioning of their {@link Controller.SocketHandler}s, used to connect one another, the
 * {@link SocketHandlerMockup} socket is used instead (which allows to manually send and receive each message exchanged,
 * see its documentation for more information).
 * <p>
 * This class tests the general functioning of the message mechanics between the three actors stated above, performing
 * a series of tests with specific order:
 * <ol>
 *     <li>It creates the {@link ControllerActionsTest#server} with the class constructor ({@link ControllerActionsTest}).</li>
 *     <li>It creates the  number of {@link ControllerActionsTest#clients} specified with the static property {@link ControllerActionsTest#num_of_clients}
 *     within the {@link ControllerActionsTest#registration()} test and checks the client-server first connection behavior.</li>
 *     <li>It creates the {@link ControllerActionsTest#lobbies} with the {@link ControllerActionsTest#createAndJoinLobby}
 *     test (as specified within the latter) and checks the </li>
 *     <li>It plays each lobby's game until it ends, using the test helpers:
 *     <ul>
 *         <li>{@link ControllerActionsTest#playCard}</li>
 *         <li>{@link ControllerActionsTest#moveStudents}</li>
 *         <li>{@link ControllerActionsTest#moveMotherNature}</li>
 *         <li>{@link ControllerActionsTest#chooseCloud}</li>
 *         <li>{@link ControllerActionsTest#activateNpc}</li>
 *     </ul>
 *     checking each move's exchanged messages and behavior.
 *     </li>
 *     <li>It makes each clients leave their lobby at the end of game with the {@link ControllerActionsTest#leaveLobby()} test.</li>
 *     <li>It tries making a client to leave its lobby while the game is still playing, within the latter test.</li>
 *     <li>It tries to disconnect a client form the lobby, within the latter test.</li>
 *     <li>It tests the choerence of the {@link GameState} class with the {@link ControllerActionsTest#gameState} test.</li>
 * </ol>
 * See each test's documentation for details about what is tested.
 * </p>
 */
@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
//FIXME: sometime it fails
public class ControllerActionsTest {
    static final Random random = new Random(-1);
    /**
     * Number of clients that will be created for the running test.
     */
    private static final int num_of_clients = 16;

    /**
     * The {@link TestServer} for the running test.
     */
    static TestServer server;

    /**
     * Array containing the {@link TestServerLobby}s of the running test.
     */
    static TestServerLobby[] lobbies;

    /**
     * Array containing the {@link TestClient}s of the running test.
     */
    static TestClient[] clients;

    /**
     * Initialises the test class, instantiating a new {@link TestServer} and a new (empty) array for the {@link TestClient}s.
     */
    @BeforeAll
    void prepareTest() {
        server = new TestServer();
        clients = new TestClient[num_of_clients];
    }

    /**
     * Returns a random string of the specified length with A_z and 0-9 characters.
     * @param length The length of the random string.
     * @return The random string.
     */
    static String randomString(final int length) {
        return random.ints(48, 123)
                .filter(i -> (i <= 57 || i >= 65) && (i <= 90 || i >= 97))
                .limit(length)
                .collect(StringBuilder::new, StringBuilder::appendCodePoint, StringBuilder::append)
                .toString();
    }

    /**
     * Key method used by this test class ({@link ControllerActionsTest}) deputed to exchange messages between the two
     * specified targets.
     * <p>
     *     It forwards the last sent message by the "from" specified target to the "to" specified
     *     target, and vice-versa for the receiver reply. It also asserts the correctness of the messages exchanged.
     *     These operations are carried out using the {@link Exchanger#receiveAndAssertLastMessageFrom} method on both targets.
     * </p>
     * @param from The target initiating the message exchange.
     * @param expected_sent The message expected to be sent from the "from" target.
     * @param to The target receiving the message from the "from" target.
     * @param expected_received The message expected to be sent as the reply from the "to" target to the "from" target.
     * @param context Description of the context of the message exchange (used by the assertion methods when failing).
     */
    protected void exchangeAndAssert(Exchanger from, String expected_sent, Exchanger to, String expected_received, String context) {
        to.receiveAndAssertLastMessageFrom(from, expected_sent, context);
        from.receiveAndAssertLastMessageFrom(to, expected_received, context);
    }

    /**
     * Finds the {@link ServerLobby} for the specified {@link Client}.
     * @param client The {@link Client} to find the {@link ServerLobby} for.
     * @return The client's lobby.
     */
    protected static TestServerLobby lobbyOf(TestClient client) {
        return client .getLobby() != null ? (TestServerLobby) server.getLobbies().get(client.getLobby().lobbyID) : null;
    }

    /**
     * Finds the playing client5 in the specified {@link Game}.
     * @param game The {@link Game} where to find the playing client.
     * @return The playing {@link Client}.
     */
    protected static TestClient playingClient(Game game) {
        return Arrays.stream(clients).filter(c -> c.getClientID() == GameTests.TestGame.playingPlayer(game).clientID).findFirst().orElseThrow();
    }

    /**
     * Returns the corresponding {@link Player} for the specified {@link Client} in the specified {@link Game}.
     * @param game The game from which to find the corresponding {@link Player}.
     * @param client The {@link Client} for which to find the player.
     * @return The {@link Client} corresponding {@link Player}.
     */
    protected static Player clientToPlayer(Game game, TestClient client) {
        return game.getPlayers().stream().filter(p -> p.clientID == client.getClientID()).findFirst().orElseThrow();
    }

    /**
     * Returns a list containing the specified lobby's clients.
     * @param lobby The lobby for to find the clients for.
     * @return The list of the specified lobby's clients.
     */
    protected static List<TestClient> clientsOf(ServerLobby lobby) {
        return lobby.getClients().stream()
                .map(cd -> Arrays.stream(clients)
                        .filter(c -> c.getClientID() == cd.clientID)
                        .findFirst().orElseThrow())
                .toList();
    }

    /**
     * Checks that the phase of first connection between the client and the server.
     * Specifically, this test asserts that:
     * <li>The server returns an error when trying to connect with a wrong nickname</li>
     * <li>The server returns an error when trying to connect with a nickname already in use</li>
     * <li>The server accepts the connect when when the nickname is properly formatted.</li>
     */
    @Test
    @Order(1)
    @DisplayName("Client-Server first connection")
    void registration() {
        for (int client_counter = 0; client_counter < num_of_clients; client_counter++) {
            //create the client
            final TestClient client = new TestClient(server);
            //check wrong nickname
            client.setNickname("");
            exchangeAndAssert(client, "MessageHello", server,
                    "MessageError", "tyring to connect with an incorrect nickname");
            assertEquals(0, client.getClientID());

            //check already taken nickname
            for (int j = 0; j < client_counter; j++) {
                client.setNickname(clients[j].getNickname());
                exchangeAndAssert(client, "MessageHello", server,
                        "MessageError", "tyring to connect with an already taken nickname");
                assertEquals(0, client.getClientID());
            }

            //check correct nickname
            final String random_nickname = randomString(random.nextInt(4, 12)); //arbitrary bounds
            client.setNickname(random_nickname);
            exchangeAndAssert(client, "MessageHello", server, "MessageAck", "attempting first connection");
            assertNotEquals(0, client.getClientID());
            //add the client to the clients list
            clients[client_counter] = client;
        }

    }

    /**
     * Checks the creation of lobbies and the joining of lobbies.
     * Specifically, this test:
     * <li>Creates the specified lobbies and asserts they creations succeeded</li>
     * <li>Asserts that each new lobby is visible to all not-in-a-lobby clients</li>
     * <li>Makes the clients join the lobbies and asserts these operations</li>
     * <li>Asserts that it is not possible to join an already full lobby</li>
     * <li>Asserts that each lobby which is full has started its game (and its clients').</li>
     */
    @Test
    @Order(2)
    @DisplayName("Creation and joining lobbies")
    void createAndJoinLobby() {
        //parameters for lobbies to be created
        final int[][] lobbies_matrix = new int[][] {
                //  {players, expert mode}
                new int[] {2, 0},
                new int[] {3, 0},
                new int[] {2, 1},
                new int[] {3, 1},
                new int[] {2, random.nextInt(2)},
                new int[] {random.nextInt(2, 4), random.nextInt(2)},
        };
        //create lobbies
        final  List<Integer> assigned_clients = new ArrayList<>();
        lobbies = new TestServerLobby[lobbies_matrix.length];
        for (int i = 0; i < lobbies_matrix.length; i++) {
            int[] lobby_params = lobbies_matrix[i];
            int index;
            do index = random.nextInt(clients.length);
            while (assigned_clients.contains(index));
            assigned_clients.add(index);

            //client that will create a new lobby
            final TestClient client = clients[index];
            //                  num of players, expert mode
            client.createLobby(lobby_params[0], lobby_params[1] == 1);
            final Set<Integer> lobby_ids = Set.copyOf(server.getLobbies().keySet());
            //create the lobby
            server.receiveAndAssertLastMessageFrom(client, "MessageCreateLobby", "creating a lobby");
            assertEquals(lobby_ids.size() + 1, server.getLobbies().size(), "The lobby was not created");
            //retrieve the just created lobby
            final ServerLobby just_created_lobby = server.getLobbies()
                    .get(server.getLobbies().keySet().stream()
                            .filter(id -> !lobby_ids.contains(id))
                            .findFirst().orElseThrow());
            lobbies[i] = (TestServerLobby) just_created_lobby;
            //set the client to the lobby
            client.receiveAndAssertLastMessageFrom((Exchanger) just_created_lobby,
                    "MessageSetLobby", "creating a lobby");
            assertTrue(client.getLobby().lobbyID == just_created_lobby.lobbyID, "The new lobby has not been assigned to the client");
            client.receiveAndAssertLastMessageFrom((Exchanger) just_created_lobby, "MessageReadiness", "receiving readiness message after creating a lobby");
            //check that the new lobby is visible to all the clients not in the lobby
            for (TestClient c : clients) {
                if (c.getLobby() != null)
                    continue;
                c.getLobbies();
                exchangeAndAssert(c, "MessageGetLobbies", server, "MessageLobbiesList", "checking lobbies list");
                assertEquals(i + 1, c.getLobbiesList().size(), "The new lobby is not visible to clients not connected to any lobby");
            }
        }
        //check that it is not possible to connect to an already full lobby
        for(TestClient client : Arrays.stream(clients).filter(client -> client.getLobby() == null).toList()) {
            for(ServerLobby lobby : server.getLobbies().values().stream().filter(lobby -> lobby.size == lobby.getClients().size()).toList()) {
                client.joinLobby(lobby.lobbyID);
                exchangeAndAssert(client, "MessageJoinLobby", server, "MessageError", "trying to connect to an already full lobby");
                assertNull(client.getLobby());
            }
            //if there is a lobby not full, fill it with clients not in a lobby to this lobby
            ServerLobby lobby = null;
            for (ServerLobby l : server.getLobbies().values()) {
                if (l.getClients().size() < l.size) {
                    lobby = l;
                    break;
                }
            }
            if(lobby != null) {
                client.joinLobby(lobby.lobbyID);
                server.receiveAndAssertLastMessageFrom(client, "MessageJoinLobby", "joining a lobby");
                assertTrue(lobby.getClients().stream().anyMatch(CD -> CD.clientID == client.getClientID()), "The client has not been added to the lobby");
                for(TestClient lobby_client : clientsOf(lobby)) {
                    lobby_client.receiveAndAssertLastMessageFrom((Exchanger) lobby, "MessageSetLobby", "joining a lobby");
                    lobby_client.receiveAndAssertLastMessageFrom((Exchanger) lobby, "MessageReadiness", "joining a lobby");
                }
            }
        }
        //start the game in all created lobbies
        for(TestServerLobby lobby : lobbies) {
            final List<TestClient> lobby_clients = clientsOf(lobby);
            for(TestClient client : lobby_clients) {
                client.toggleReady();
                lobby.receiveAndAssertLastMessageFrom(client, "MessageToggleReady", "toggling readiness");
                for(TestClient client2 : lobby_clients)
                    client2.receiveAndAssertLastMessageFrom(lobby, "MessageReadiness", "receiving message readiness after someone has toggled");
            }
            for(TestClient client : lobby_clients) {
                client.receiveAndAssertLastMessageFrom(lobby, "MessageGameStarted", "game is starting");
                assertNotNull(client.getGameState(), "game in client has not been started");
            }
            assertNotNull(lobby.getGame(), "game in lobby ahs not been started");
        }
    }

    /**
     * Performs and asserts the functioning of the {@link Client#leaveLobby} method within the
     * specified lobby (with a lobby's randomly chosen client).
     * Specifically it asserts that:
     * <li>The lobby no longer contains the leaving client</li>
     * <li>The client no longer has a lobby.</li>
     * @param lobby The lobby where to perform {@link Client#leaveLobby leaveLobby}.
     */
    void leaveLobby(TestServerLobby lobby) {
        final boolean game_was_null = lobby.getGame() == null;
        //choose a random client based on the current lobby state (in or not in game)
        final TestClient client = game_was_null ? clientsOf(lobby).get(random.nextInt(clientsOf(lobby).size())) : playingClient(lobby.getGame());
        //actually leave the lobby and assert
        client.leaveLobby();
        lobby.receiveAndAssertLastMessageFrom(client, "MessageLeaveLobby", "leaving a lobby");
        assertFalse(lobby.getClients().stream().anyMatch(CD -> CD.clientID == client.getClientID()), "The client has not been removed from the lobby");
        //check all the related messages for all lobby's clients (except the one that has just left)
        for(TestClient lobby_client : clientsOf(lobby)) {
            if(lobby_client.equals(client))
                continue;
            if(lobby.getGame() == null) {
                if(!game_was_null && lobby.size == 2) {
                    lobby_client.receiveAndAssertLastMessageFrom(lobby, "MessageReadiness", "the other client has left the game");
                    lobby_client.receiveAndAssertLastMessageFrom(lobby, "MessageGameEnded", "the other client has left the game");
                    lobby_client.receiveAndAssertLastMessageFrom(lobby, "MessageSetLobby", "the other client has left the game");
                }
                else
                    lobby_client.receiveAndAssertLastMessageFrom(lobby, "MessageSetLobby", "updating after a client has left the lobby");
            }
            lobby_client.receiveAndAssertLastMessageFrom(lobby, "MessageReadiness", "updating after a client has left the lobby");
        }
        client.receiveAndAssertLastMessageFrom(server, "MessageSetLobby", "exiting a lobby while not in game");
        assertNull(client.getLobby(), "The client's lobby has not been reset to 0");
    }

    /**
     * Performs and asserts the functioning of the {@link Client#playCard} method for all the in-lobby clients.
     * @param lobby The lobby where to perform {@link Client#playCard}.
     */
    void playCard(TestServerLobby lobby) {
        for (int i = 0; i < lobby.size; i++) {
            Game game = lobby.getGame();
            TestClient client = playingClient(game);
            //find a valid random card index
            int card_index;
            if(GameTests.TestGame.playingPlayer(game).getCards().size() > 1) {
                if(game.getPlayerTurn() == 2 && GameTests.TestGame.playingPlayer(game).getCards().size() == 2 && game.getPlayers().size() == 3
                        && GameTests.TestGame.playingPlayer(game).getCards().stream().allMatch(card ->
                        game.getPlayers().stream().filter(player -> player != GameTests.TestGame.playingPlayer(game)).anyMatch(player -> card.order_value == player.getLastCardPlayed().order_value)))
                    card_index = random.nextInt(2); //2 == playing_player.getCards().size()
                else {
                    boolean condition;
                    do {
                        card_index = random.nextInt(GameTests.TestGame.playingPlayer(game).getCards().size());
                        condition = false;
                        for(int j = 0; j < game.getPlayerTurn(); j++) {
                            if (GameTests.TestGame.orderedPayers(game).get(j).getLastCardPlayed().order_value == GameTests.TestGame.playingPlayer(game).getCards().get(card_index).order_value) {
                                condition = true;
                                break;
                            }
                        }
                    } while (condition);
                }
            }
            else card_index = 0;
            //actually play the card
            client.playCard(card_index);
            exchangeAndAssert(client, "MessageCardPlayedLB", lobby, "MessageMoveSuccessful", "playing a card");
            //check that the move's message has been received for all lobby's clients (except the one that has just played)
            for(TestClient other_client : clientsOf(lobby)) {
                if(other_client.equals(client))
                    continue;
                other_client.receiveAndAssertLastMessageFrom(lobby, "MessageCardPlayed", "updating after someone have played a card");
            }
        }
    }

    /**
     * Performs and asserts the functioning of the {@link Client#setStudentToHall} method for the playing client in the
     * specified lobby.
     * @param lobby The lobby where to perform {@link Client#setStudentToHall}.
     */
    void moveStudents(TestServerLobby lobby) {
        Game game = lobby.getGame();
        //find the playing client
        TestClient client = playingClient(game);
        //actually move a student to hall
        client.setStudentToHall(random.nextInt(clientToPlayer(game, client).getDashboard().getEntrance().size()));
        exchangeAndAssert(client, "MessageStudentSetToHallLB", lobby, "MessageMoveSuccessful", "moving a student to hall");
        //check that the move's message has been received for all lobby's clients (except the one that has just played)
        for(TestClient other_client : clientsOf(lobby)) {
            if(other_client.equals(client))
                continue;
            other_client.receiveAndAssertLastMessageFrom(lobbyOf(other_client), "MessageStudentSetToHall", "updating after someone has moved a student");
        }
        game = lobby.getGame();
        //actually move a student to an island
        client.setStudentToIsland(random.nextInt(clientToPlayer(game, client).getDashboard().getEntrance().size()), random.nextInt(game.getIslands().size()));
        exchangeAndAssert(client, "MessageStudentSetToIslandLB", lobby, "MessageMoveSuccessful", "moving a student to hall");
        //check that the move's message has been received for all lobby's clients (except the one that has just played)
        for(TestClient other_client : clientsOf(lobby)) {
            if(other_client.equals(client))
                continue;
            other_client.receiveAndAssertLastMessageFrom(lobby, "MessageStudentSetToIsland", "updating after someone has moved a student");
        }
        //actually move the student(s) left to be moved
        for (int j = 0; j < (lobby.size == 2 ? 1 : 2); j++) {
            game = lobby.getGame();
            //randomly choose if to move the student to an island or to the hall
            if(random.nextBoolean()) {
                client.setStudentToHall(random.nextInt(clientToPlayer(game, client).getDashboard().getEntrance().size()));
                exchangeAndAssert(client, "MessageStudentSetToHallLB", lobby, "MessageMoveSuccessful", "moving a student to hall");
                //check that the move's message has been received for all lobby's clients (except the one that has just played)
                for(TestClient other_client : clientsOf(lobby)) {
                    if(other_client.equals(client))
                        continue;
                    other_client.receiveAndAssertLastMessageFrom(lobbyOf(other_client), "MessageStudentSetToHall", "updating after someone has moved a student");
                }
            } else {
                client.setStudentToIsland(random.nextInt(clientToPlayer(game, client).getDashboard().getEntrance().size()), random.nextInt(game.getIslands().size()));
                exchangeAndAssert(client, "MessageStudentSetToIslandLB", lobby, "MessageMoveSuccessful", "moving a student to hall");
                //check that the move's message has been received for all lobby's clients (except the one that has just played)
                for(TestClient other_client : clientsOf(lobby)) {
                    if(other_client.equals(client))
                        continue;
                    other_client.receiveAndAssertLastMessageFrom(lobbyOf(other_client), "MessageStudentSetToIsland", "updating after someone has moved a student");
                }
            }
        }
    }

    /**
     * Performs and asserts the functioning of {@link Client#moveMotherNature} method for the playing client in the
     * specified lobby.
     * @param lobby The lobby where to perform {@link Client#moveMotherNature}.
     */
    void moveMotherNature(TestServerLobby lobby) {
        Game game = lobby.getGame();
        //find the playing client
        TestClient client = playingClient(game);
        //actually move mother nature
        client.moveMotherNature(random.nextInt(1, clientToPlayer(game, client).getLastCardPlayed().movements_value + 1));
        exchangeAndAssert(client, "MessageMotherNatureMovedLB", lobby, "MessageMoveSuccessful", "moving mother nature");
        //check that the move's message has been received for all lobby's clients (except the one that has just played)
        for(TestClient other_client : clientsOf(lobby)) {
            if(other_client.equals(client))
                continue;
            other_client.receiveAndAssertLastMessageFrom(lobbyOf(other_client), "MessageMotherNatureMoved", "updating after someone has moved mother nature");
        }
    }

    /**
     * Performs and asserts the functioning of {@link Client#chooseCloud} method for the playing client in the
     * specified lobby.
     * @param lobby The lobby where to perform {@link Client#chooseCloud}.
     */
    void chooseCloud(TestServerLobby lobby) {
        Game game = lobby.getGame();
        //find the playing client
        TestClient client = playingClient(game);
        int random_cloud;
        if(Arrays.stream(game.getClouds()).allMatch(List::isEmpty))
            random_cloud = 0;
        else {
            do random_cloud = random.nextInt(game.getPlayers().size());
            while (game.getClouds()[random_cloud].size() == 0);
        }
        //actually choose the cloud
        client.chooseCloud(random_cloud);
        exchangeAndAssert(client, "MessageCloudChosenLB", lobby, "MessageMoveSuccessful", "choosing a cloud");
        client.receiveAndAssertLastMessageFrom(lobbyOf(client), "MessageCloudsUpdated", "choosing a cloud");
        //check that the move's message has been received for all lobby's clients (except the one that has just played)
        for(TestClient other_client : clientsOf(lobby)) {
            if(other_client.equals(client))
                continue;
            other_client.receiveAndAssertLastMessageFrom(lobbyOf(other_client), "MessageCloudChosen", "updating after someone has chosen a cloud");
        }
    }

    /**
     * Performs and asserts the functioning of {@link Client#activateEffect} method for the playing client in the
     * specified lobby.
     * @param lobby The lobby where to perform {@link Client#activateEffect}.
     */
    void activateNpc(TestServerLobby lobby) {
        Game game = lobby.getGame();
        //exit if the lobby's game is not in "expert mode"
        if(!game.expert_mode)
            return;
        //find the playing client
        final TestClient client = playingClient(game);
        //find the npc's index which is cheaper to activate
        final int npc_index = IntStream.range(0, 3).reduce((i1, i2) -> game.getNpcs()[i1].getCost() < game.getNpcs()[i2].getCost() ? i1 : i2).orElseThrow();
        //actually (try to) activate the effect
        client.activateEffect(npc_index, GameTests.TestGame.prepareEffectParameters(game, npc_index));
        if(client.sent_messages.isEmpty()) {
            return;
        }
        //find out if the chosen npc has extra properties
        final boolean npc_with_extra_properties = switch(game.getNpcs()[npc_index].getId()) {
            case 1, 7, 11 -> true;
            default -> false;
        };
        lobby.receiveAndAssertLastMessageFrom(client, "MessageNpcActivatedLB", "activating an npc");
        //if it was impossible to activate the ncp
        if(lobby.sentTo(client).peek().toString().contains("Error")) {
            client.receiveLastMessageFrom(lobby);
            client.receiveLastMessageFrom(lobby);
            return;
        }
        if(npc_with_extra_properties)
            client.receiveAndAssertLastMessageFrom(lobby, "MessageNpcUpdated", "activating an npc with extra properties");
        client.receiveAndAssertLastMessageFrom(lobby, "MessageMoveSuccessful", "activating an npc");
        //check that the move's message has been received for all lobby's clients (except the one that has just played)
        for(TestClient other_client : clientsOf(lobby)) {
            if(other_client.equals(client))
                continue;
            other_client.receiveAndAssertLastMessageFrom(lobby, "MessageNpcActivated", "updating after someone has activated an npc");
            if(npc_with_extra_properties)
                other_client.receiveAndAssertLastMessageFrom(lobby, "MessageNpcUpdated", "updating after someone has activated an npc with extra properties");
        }
    }

    /**
     * Test that runs the entire game for all the previously created lobbies (by the {@link ControllerActionsTest#createAndJoinLobby}
     * method) and asserts the functioning of the {@link GameState} class (with the {@link ControllerActionsTest#gameState}
     * method) for each of them.
     */
    @Test
    @Order(3)
    @DisplayName("Play games")
    void play() {
        //for each lobby...
        for(int i = 0; i < lobbies.length - 2; i++) {
            final TestServerLobby lobby = lobbies[i];
            //...play the game until it ends
            while (lobby.getGame() != null) {
                gameState(lobby);
                playCard(lobby);
                for (int j = 0; j < lobby.size; j++) {
                    activateNpc(lobby);
                    if(lobby.getGame() == null) //if game is ended
                        break;
                    moveStudents(lobby);
                    activateNpc(lobby);
                    if(lobby.getGame() == null) //if game is ended
                        break;
                    moveMotherNature(lobby);
                    if(lobby.getGame() == null) //if game is ended
                        break;
                    if(!lobby.getGame().isLastGameTurn())
                        chooseCloud(lobby);
                }
            }
            assertNull(lobby.getGame(), "Lobby was not reset after the game has ended");
            for(TestClient client : clientsOf(lobby)) {
                client.receiveAndAssertLastMessageFrom(lobby, "MessageGameEnded", "game has ended");
                client.receiveAndAssertLastMessageFrom(lobby, "MessageSetLobby", "game has ended");
                client.receiveAndAssertLastMessageFrom(lobby, "MessageReadiness", "game has ended");
            }
        }

    }

    /**
     * Performs {@link Client#leaveLobby} using the {@link ControllerActionsTest#leaveLobby(TestServerLobby)} method within
     * the previously created lobbies (by the {@link ControllerActionsTest#createAndJoinLobby} method).
     * <p>
     *      This test make clients leave both after the game has ended and while still in game. This test also try to
     *      disconnect the playing client from one the lobbies.
     * </p>
     */
    @Test
    @Order(4)
    @DisplayName("Leave lobby")
    void leaveLobby() {
        //for each lobby expect the last two...
        for (int i = 0; i < lobbies.length - 2; i++) {
            TestServerLobby lobby = lobbies[i];
            //...make each client to leave (after the game is ended)
            for (int j = 0; j < lobby.size; j++) {
                leaveLobby(lobby);
            }
        }
        //test the leaving of a client while the lobby's game is still playing
        final TestServerLobby leaving_lobby = lobbies[lobbies.length - 2];
        playCard(leaving_lobby);
        leaveLobby(leaving_lobby);
        //test a client disconnection (while the lobby's game is still playing)
        final TestServerLobby disconnecting_lobby = lobbies[lobbies.length - 1];
        playCard(disconnecting_lobby);
        moveStudents(disconnecting_lobby);
        disconnecting_lobby.handleDisconnect(playingClient(disconnecting_lobby.getGame()).getClientID());
    }

    /**
     * Asserts the functioning of the {@link GameState} class. This test calls all the class methods and asserts
     * their correctness.
     * @param lobby The lobby where to test the {@link GameState} class.
     */
    void gameState(TestServerLobby lobby) {
        Game game = lobby.getGame();
        GameState gs = new GameState(game);
        //myPlayer and otherPlayers
        for(TestClient client : clientsOf(lobby)) {
            assertEquals(lobby.getGame().getPlayers().stream()
                    .filter(player -> player.clientID == client.getClientID())
                    .map(p -> p.clientID)
                    .findFirst().orElseThrow(),
                    gs.myPlayer(client).clientID, "myPlayer hasn't returned the correct player"
            );
            assertEquals(lobby.getGame().getPlayers().stream()
                            .filter(player -> player.clientID != client.getClientID())
                            .map(p -> p.clientID)
                            .toList(),
                    gs.otherPlayers(client).stream().map(p -> p.clientID).toList(), "otherPlayers hasn't returned the correct list of other players"
            );
        }
        //isMyTurn
        for(TestClient client : clientsOf(lobby)) {
            assertEquals(client.getClientID() == GameTests.TestGame.playingPlayer(lobby.getGame()).clientID,
                    gs.isMyTurn(client.getClientID()), "isMyTurn has returned the correct answer");
        }
        //currentlyPlayingPlayer
        assertEquals(GameTests.TestGame.playingPlayer(lobby.getGame()).clientID, gs.currentlyPlayingPlayer(), "currentlyPlayingPlayer has not returned the correct id");
        //currentPlayersTurnOrder
        assertEquals(GameTests.TestGame.orderedPayers(lobby.getGame()).stream()
                .map(player -> player.clientID)
                .toList(), gs.currentPlayersTurnOrder(), "currentPlayersTurnOrder has not returned the correct list");
        //getPlayers
        assertEquals(game.getPlayers(), gs.getPlayers(), "getPlayers has not returned the correct players' list");
        //getGameTurn
        assertEquals(game.getGameTurn(), gs.getGameTurn(), "getGameTurn has not returned the correct turn");
        //getStep
        assertEquals(game.getStep(), gs.getStep(), "getStep has not returned the correct step");
        //getMovedStudents
        assertEquals(game.getMovedStudents(), gs.getMovedStudents(), "getMovedStudents ahs not returned the correct amount of moved students");
        //getClouds
        assertArrayEquals(Arrays.stream(game.getClouds()).toArray(), Arrays.stream(gs.getClouds()).toArray(), "getClouds has not returned the correct list of clouds");
        //getIslands
        assertEquals(game.getIslands(), gs.getIslands(), "getIslands has not returned the correct list of islands");
        //getUnclaimedProfessors
        assertEquals(game.getUnclaimedProfessors(), gs.getUnclaimedProfessors(), "getUnclaimedProfessors has not returned the correct list of unclaimed professors");
        if(game.expert_mode) {
            //getNpcs
            assertArrayEquals(Arrays.stream(game.getNpcs()).toArray(), Arrays.stream(gs.getNpcs()).toArray(), "getNpcs has not returned the correct array of npcs");
            //getNpcsWithId
            for (int i = 0; i < 12; i++) {
                final int index = i;
                assertEquals(Arrays.stream(game.getNpcs())
                        .filter(npc -> npc.getId() == index)
                        .findFirst().orElse(null), gs.getNpcWithId(index), "getNpcWithId has not returned the correct npc");
            }
            //getNpcEffect
            assertEquals(game.getNpcEffect(), gs.getNpcEffect(), "getNpcEffect has not returned the correct npc effect");
        }
        //getBank
        assertEquals(game.getBank(), gs.getBank(), "getBank has not returned the correct amount of coins in the bank");
        //getRemainingStudentsNum
        assertEquals(game.getRemainingStudentsNum(), gs.getRemainingStudentsNum(), "getRemainingStudentsNum has not returned the  correct amount of students left");
        //isLastGameTurn
        assertEquals(game.isLastGameTurn(), gs.isLastGameTurn(), "isLastGameTurn has not returned the correct last game turn state");
        //isGameEnded
        assertEquals(game.isGameEnded(), gs.isGameEnded(), "isGameEnded has not returned the correct game ended state");
        //getWinnerId
        assertEquals(game.getWinnerID(), gs.getWinnerID(), "getWinnerId has not returned the correct winner id");
        //CLI source
        for(TestClient client : clientsOf(lobby))
            assertNotNull(gs.toString(client), "toString for client " + client.getClientID() + " is null");
    }

    //TODO: to be removed (for debugging only)
}