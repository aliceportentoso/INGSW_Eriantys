package Controller;

import Controller.ClientSide.Client;
import Controller.ServerSide.Server;
import Controller.ServerSide.ServerLobby;

import java.io.Serializable;

//IMPORTANT: always duplicate/clone/copy each data structure being set into a message!

/**
 * Abstract message, every class implementing this one is intended to be serialized and exchanged over the network
 * between {@link Client} and {@link Server}.<br>
 * As suck instances of this class are fed to {@link Controller#update}, the {@link Controller} then reacts to the message by invoking
 * the override of {@link Message#execute} that is compatible with its implementation.<br>
 * {@link Message#execute} inside each message is then tailored to allow the message to perform the exact actions it on the specific
 * controller that is intended to.<br><br>
 * The main reason for the existence of this class and its implementations is to avoid parsing or identifying messages, simply using
 * the basic mechanism of Java overrides to do that instead.
 */
public abstract class Message implements Serializable {
    /**
     * Overload of executed used by the {@link Server} class.
     * Every specific implementation executes the intended methods on the {@link Server}.
     *
     * @param server the {@link Server} which received the message and reacts to it
     * @param socket_handler the {@link SocketHandler} which received the message
     */
    public void execute(Server server, SocketHandler socket_handler) {
        System.out.println("Invalid message handled on the server in Server.");
    }

    /**
     * Overload of executed used by the {@link Client} class.
     * Every specific implementation executes the intended methods on the {@link Client}.
     *
     * @param client the {@link Client} which received the message and reacts to it
     * @param socket_handler the {@link SocketHandler} which received the message
     */
    public void execute(Client client, SocketHandler socket_handler) {
        System.out.println("Invalid message handled on the client.");
    }

    /**
     * Overload of executed used by the {@link ServerLobby} class.
     * Every specific implementation executes the intended methods on the {@link ServerLobby}.
     *
     * @param lobby the {@link ServerLobby} which received the message and reacts to it
     * @param socket_handler the {@link SocketHandler} which received the message
     */
    public void execute(ServerLobby lobby, SocketHandler socket_handler) {
        System.out.println("Invalid message handled on the server in ServerLobby.");
    }

    @Override
    public String toString() {
        return this.getClass().getSimpleName();
    }
}


