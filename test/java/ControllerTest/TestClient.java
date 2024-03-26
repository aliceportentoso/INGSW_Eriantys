package ControllerTest;

import Controller.ClientSide.Client;
import Controller.Message;

import java.util.Queue;

import static org.junit.jupiter.api.Assertions.assertEquals;

/**
 * A {@link Client} extension designed for testing purposes.
 * <p>
 *     This client overrides the necessary methods from {@link Client} in order to be usable in testing context where
 *     all the {@link Controller.Controller Controller}s involved are not run on different machines and they are not using a real
 *     network socket.
 *     To fulfill this intent, this class implements make use fo the {@link SocketHandlerMockup} socket and the {@link Exchanger}
 *     interface, in order to easily send and receive {@link Message}s with other {@link Controller.Controller Controller}s.
 * </p>
 * <p>
 *     Specifically, this class implements its own constructor (so to set up the network connection mockup using the
 *     {@link SocketHandlerMockup} socket as state above) and overrides the {@link Client#start start} method.
 * </p>
 * @implNote This class is meant to be used within a test context along with other controllers' test versions, meaning
 * {@link TestServer} and {@link TestServerLobby}.
 */
public class TestClient extends Client implements Exchanger {

    /**
     * This client's fake network socket.
     * @see TestClient
     */
    protected final SocketHandlerMockup socket_mockup;
    /**
     * A reference to this client's list of sent messages store in its fake socket {@link TestClient#socket_mockup}.
     */
    protected final Queue<Message> sent_messages;

    /**
     * Only constructor for this class.
     * <p>
     *     It sets up all the necessary fields for this client to communicate with other {@link Controller.Controller Controller}s
     *     (which have implemented {@link Exchanger} interface).
     * </p>
     * @param server The server to where connect.
     */
    public TestClient(TestServer server) {
        super("server_ip", 0, new TestUI());
        socket_mockup = new SocketHandlerMockup(this);
        sent_messages = socket_mockup.sent_messages;
        server.registerClient(this);
        start();
    }

    /**
     * Actually starts this client with the proper network socket, meaning the mockup socket {@link SocketHandlerMockup}
     * stored in {@link TestClient#socket_mockup}.
     * @implNote This method is already called by the {@link TestClient#TestClient} constructor and it is not meant to be
     * called later in any circumstances.
     */
    @Override
    public void start() {
        super.start(socket_mockup);
    }

    /**
     * Consumes last received {@link Message} still not read by this client from the sender.
     * @param sender The sender of the message.
     * @return The last received message.
     */
    @Override
    public Message receiveLastMessageFrom(Exchanger sender) {
        if (sender instanceof TestServer server) {
            Message m = server.sentTo(this).poll();
            socket_mockup.receive(m);
            return m;
        } else if (sender instanceof TestServerLobby lobby) {
            Message m = lobby.sentTo(this).poll();
            socket_mockup.receive(m);
            return m;
        } else {
            return null;
        }
    }

    /**
     * Consumes and asserts the correctness of last received {@link Message} still not read by this client from the sender.
     * @param sender The sender of the message.
     * @param expected_message The type of message that should be consumed (used by the assertion method).
     * @param context Description of the context in which the message it has been exchanged (used by the assertion method).
     */
    @Override
    public void receiveAndAssertLastMessageFrom(Exchanger sender, String expected_message, String context) {
        String ms = receiveLastMessageFrom(sender).toString();
        assertEquals(expected_message, ms, "Wrong message received from server while " + context);
    }
}
