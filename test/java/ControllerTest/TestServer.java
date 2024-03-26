package ControllerTest;

import Controller.ClientSide.Client;
import Controller.Message;
import Controller.ServerSide.Server;
import Controller.ServerSide.ServerLobby;
import Controller.SocketHandler;

import java.util.HashMap;
import java.util.Map;
import java.util.Queue;

import static org.junit.jupiter.api.Assertions.assertEquals;

/**
 * A {@link Server} extension designed for testing purposes.
 * <p>
 *     This server overrides the necessary methods from {@link Server} in order to be usable in testing context where
 *     all the {@link Controller.Controller Controller}s involved are not run on different machines and they are not using a real
 *     network socket.
 *     To fulfill this intent, this class implements make use fo the {@link SocketHandlerMockup} socket and the {@link Exchanger}
 *     interface, in order to easily send and receive {@link Message}s with other {@link Controller.Controller Controller}s.
 * </p>
 * <p>
 *     Specifically, this class implements its own constructor (so to set up the network connection mockup using the
 *     {@link SocketHandlerMockup} socket as state above) and overrides some of {@link Server} methods
 *     in order to properly work within a test context and to create the test version of {@link ServerLobby} ({@link TestServerLobby})
 *     instead of the default one.
 * </p>
 * @implNote This class is meant to be used within a test context along with other controllers' test versions, meaning
 * {@link TestClient} and {@link TestServerLobby}.
 */
public class TestServer extends Server implements Exchanger {

    /**
     * Contains all this server's sockets mapped with their respective {@link Client}s to which this is connected to.
     */
    protected final Map<TestClient, SocketHandlerMockup> client_sockets;

    /**
     * Only constructor for this class.
     * <p>
     *     It sets up all the necessary fields for this server to communicate with {@link Client}s
     *     (which have implemented {@link Exchanger} interface).
     * </p>
     */
    public TestServer() {
        super(0);
        client_sockets = new HashMap<>();
    }

    /**
     * Updates this server upon the receipt of the specified {@link Message}.
     * @see Server#update(SocketHandler, Message) update(SocketHandler, Message)
     * @implNote This method is called by the private methods of the {@link Server} and it is not meant to be
     * called later in any circumstances.
     */
   @Override
    public synchronized void update(SocketHandler client_handler, Message message) {
        message.execute(this, client_handler);
    }

    /**
     * Builds {@link TestServerLobby}s for this server.
     * @see Server#buildLobby(int, int, boolean) buildLobby(int, int, boolean)
     * @see Server#createLobby(SocketHandler, int, boolean) createLobby(SocketHandler, int, boolean)
     * @implNote This method is called by private methods of {@link Server} and it is not meant to be
     * called later in other circumstances.
     */
    @Override
    public synchronized ServerLobby buildLobby(int lobbyID, int size, boolean expert_mode) {
        return new TestServerLobby(this, lobbyID, size, expert_mode);
    }

    /**
     * Registers the specified {@link TestClient} onto this server.
     * <p>
     *     It sets up a new socket {@link SocketHandlerMockup} for the connecting {@link TestClient} in order to
     *     initiate communication with it.
     * </p>
     * @param client The {@link TestClient} that is trying to connect to this server for the first time.
     */
    public void registerClient(TestClient client) {
        client_sockets.put(client, new SocketHandlerMockup(this));
    }

    /**
     * Returns the reference to the list containing the {@link Message}s sent to the specified {@link TestClient}.
     * @param client The {@link TestClient} towards this server has sent {@link Message}s to.
     * @return The reference to the sent {@link Message}s list.
     */
    public Queue<Message> sentTo(TestClient client) {
        return client_sockets.get(client).sent_messages;
    }

    /**
     * Consumes last received {@link Message} still not read by this server from the sender.
     * @param sender The sender of the message.
     * @return The last received message.
     */
    @Override
    public Message receiveLastMessageFrom(Exchanger sender) {
        if(!(sender instanceof TestClient client))
            return null;
        Message m = client.sent_messages.poll();
        client_sockets.get(client).receive(m);
        return m;
    }

    /**
     * Consumes and asserts the correctness of last received {@link Message} still not read by this server from the sender.
     * @param sender The sender of the message.
     * @param expected_message The type of message that should be consumed (used by the assertion method).
     * @param context Description of the context in which the message it has been exchanged (used by the assertion method).
     */
    @Override
    public void receiveAndAssertLastMessageFrom(Exchanger sender, String expected_message, String context) {
        assertEquals(expected_message, receiveLastMessageFrom(sender).toString(), "Wrong message received from client while "+ context);
    }
}
