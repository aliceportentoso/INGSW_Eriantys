package ControllerTest;

import Controller.Controller;
import Controller.Message;
import Controller.SocketHandler;

import java.util.*;

/**
 * A {@link SocketHandler} mockup that allows to test the {@link Message}s exchange between {@link Controller} objects.
 * <p>
 *   This socket stores the messages sent by the controller which is using the socket (via the {@link SocketHandlerMockup#send})
 *    method in order to allow the implementing tests to manually deliver the messages to the set {@link SocketHandlerMockup#controller}
 *    when desired, using the {@link SocketHandlerMockup#receive} method.
 * </p>
 */
public class SocketHandlerMockup extends SocketHandler {
    /**
     * Stores the messages sent by the {@link SocketHandlerMockup#controller} util they are delivered.
     */
    public Queue<Message> sent_messages;
    /**
     * Stores all the messages sent by the {@link SocketHandlerMockup#controller}.
     */
    public List<Message> read;

    public SocketHandlerMockup(Controller controller) {
        super(null, controller);
        read = new ArrayList<>();
        sent_messages = new LinkedList<>();
    }

    /**
     * Always returns true since this is a {@link SocketHandler} designed for testing purposes.
     * @return false.
     */
    public boolean isClosed() {
        return false;
    }

    /**
     * Always returns null since this is a {@link SocketHandler} designed for testing purposes.
     * @return null.
     */
    public Runnable getReceiver() {
        return null;
    }

    /**
     * Always returns null since this is a {@link SocketHandler} designed for testing purposes.
     * @return null.
     */
    public Runnable getSender() {
        return null;
    }

    /**
     * Stores the outgoing {@link Message}.
     * @param message {@link Message} to send.
     */
    public void send(Message message) {
        sent_messages.add(message);
    }

    /**
     * Makes the set {@link SocketHandlerMockup#controller} receive the specified {@link Message}.
     * @param message The {@link Message} to be received.
     */
    public void receive(Message message) {
        controller.update(this, message);
        read.add(message);
    }

    /**
     * Changes the target of the {@link Controller} which is using this socket by moving itself to the target Controller.
     * @param controller The new {@link Controller}.
     */
    public void changeUpdatesTarget(Controller controller) {
        if(controller instanceof TestServer server)
            server.client_sockets.put(Arrays.stream(ControllerActionsTest.clients).filter(client -> client.getClientID() == clientID).findAny().orElseThrow(), ((TestServerLobby) this.controller).clients.remove(clientID));
        else if(controller instanceof TestServerLobby lobby)
            lobby.clients.put(clientID, ((TestServer) this.controller).client_sockets.remove(Arrays.stream(ControllerActionsTest.clients).filter(client -> client.getClientID() == clientID).findAny().orElseThrow()));
        this.controller = controller;
    }

}