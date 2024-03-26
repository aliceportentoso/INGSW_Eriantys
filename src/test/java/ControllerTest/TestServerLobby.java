package ControllerTest;

import Controller.ClientSide.Client;
import Controller.Message;
import Controller.ServerSide.Server;
import Controller.ServerSide.ServerLobby;
import Controller.SocketHandler;
import Model.*;

import java.util.*;

import static org.junit.jupiter.api.Assertions.assertEquals;

/**
 * A {@link ServerLobby} extension designed for testing purposes.
 * <p>
 *     This lobby overrides the necessary methods from {@link ServerLobby} in order to be usable in testing context where
 *     all the {@link Controller.Controller Controller}s involved are not run on different machines and they are not using a real
 *     network socket.
 *     To fulfill this intent, this class implements make use fo the {@link SocketHandlerMockup} socket and the {@link Exchanger}
 *     interface, in order to easily send and receive {@link Message}s with other {@link Controller.Controller Controller}s.
 * </p>
 * <p>
 *     Specifically, this class implements its own constructor (so to set up the network connection mockup using the
 *     {@link SocketHandlerMockup} socket as state above) and overrides some of {@link ServerLobby} methods
 *     in order to properly work within a test context.
 * </p>
 * @implNote This class is meant to be used within a test context along with other controllers' test versions, meaning
 * {@link TestClient} and {@link TestServer}.
 */
public class TestServerLobby extends ServerLobby implements Exchanger {
    /**
     * Contains all this lobby's sockets mapped with their respective {@link Client}s' ids to which this is connected to.
     */
    protected Map<Integer, SocketHandlerMockup> clients;

    /**
     * Constructor of {@link TestServerLobby}.
     * @implNote This constructor is meant to be called only by the overrider method
     * {@link TestServer#buildLobby(int, int, boolean) buildLobby(int, int, boolean)}. See its documentation for more information.
     */
    public TestServerLobby(Server server, int lobbyID, int size, boolean expert_mode) {
        super(server, lobbyID, size, expert_mode);
        clients = new HashMap<>();
    }

    /**
     * Builds {@link Game} as intended within a test context.
     * @implNote This method is called by private methods of {@link ServerLobby} and it is not meant to be
     * called in any other circumstances.
     * @see ServerLobby#buildGame(boolean, List) buildGame(boolean, List)
     * @see ServerLobby#toggleReady(SocketHandler) toggleReady(SocketHandler)
     */
    @Override
    public synchronized Game buildGame(boolean expert_mode, List<Integer> clientIDs) {
        //construct students list
        final List<Colors> random_students = new ArrayList<>();
        for(int k = 0; k < 5; k++) {
            for(int j = 0; j < 24; j++) {
                random_students.add(Colors.fromColorIndex(k));
            }
        }
        //choose mother nature position
        final int mother_nation_position = ControllerActionsTest.random.nextInt(12); //12 == number of initial islands
        //prepare students pool from where to extract the students to be placed on islands
        final List<Colors> temp = new ArrayList<>();
        for(int k = 0; k < 5; k++)
            for (int j = 0; j < 2; j++)
                temp.add(Colors.fromColorIndex(k));
        //prepare islands
        final List<Island> islands = new ArrayList<>();
        for(int k = 0; k < 12; k++)
            islands.add(new Island(
                    k == (mother_nation_position + 6) % 12 || k == mother_nation_position
                            ? null : temp.remove(0), k));
        //prepare npcs
        final Npc[] current_npcs;
        if(expert_mode) {
            current_npcs = new Npc[3];
            current_npcs[0] = NpcFactory.factoryTestMethod(random_students, ControllerActionsTest.random.nextInt(12));
            for (int j = 1; j <= 2; j++) {
                int random_npc;
                do random_npc = ControllerActionsTest.random.nextInt(12);
                while (random_npc == current_npcs[0].getId() || (j == 2 && random_npc == current_npcs[1].getId()));
                current_npcs[j] = NpcFactory.factoryTestMethod(random_students, random_npc);
            }
        }
        else current_npcs = null;

        //build players
        final List<Player> players = new ArrayList<>(clientIDs.size());
        for(int k = 0; k < clientIDs.size(); k++) {
            final List<Colors> temp_students = new ArrayList<>();
            for (int l = 0; l < (clientIDs.size() == 2 ? 7 : 9); l++)
                temp_students.add(random_students.remove(0));
            players.add(new Player(clientIDs.get(k), temp_students, clientIDs.size(), k, 10)); //give each players 10 coins to ensure all npcs can be activated to be tested
        }
        //construct game
        return new Game(expert_mode, players, clientIDs, random_students, mother_nation_position, islands, current_npcs, new ArrayList<>(Arrays.asList(Colors.values())));
    }

    /**
     * Returns the reference to the list containing the {@link Message}s sent to the specified {@link TestClient}.
     * @param client The {@link TestClient} towards this lobby has sent {@link Message}s to.
     * @return The reference to the sent {@link Message}s list.
     */
    public Queue<Message> sentTo(TestClient client) {
        return clients.get(client.getClientID()).sent_messages;
    }

    /**
     * Consumes last received {@link Message} still not read by this lobby from the sender.
     * @param sender The sender of the message.
     * @return The last received message.
     */
    @Override
    public Message receiveLastMessageFrom(Exchanger sender) {
        if(!(sender instanceof TestClient client))
            return null;
        Message m = client.sent_messages.poll();
        clients.get(client.getClientID()).receive(m);
        return m;
    }

    /**
     * Consumes and asserts the correctness of last received {@link Message} still not read by this lobby from the sender.
     * @param sender The sender of the message.
     * @param expected_message The type of message that should be consumed (used by the assertion method).
     * @param context Description of the context in which the message it has been exchanged (used by the assertion method).
     */
    @Override
    public void receiveAndAssertLastMessageFrom(Exchanger sender, String expected_message, String context) {
        assertEquals(expected_message, receiveLastMessageFrom(sender).toString(), "Wrong message received while " + context);
    }
}
