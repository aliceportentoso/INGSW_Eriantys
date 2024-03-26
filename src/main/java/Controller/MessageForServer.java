package Controller;

import Controller.ServerSide.Server;
import Controller.ServerSide.ServerLobby;

import java.io.Serializable;

/**
 * Abstract class consisting only of static methods.<br>
 * The methods of this class construct and return the associated implementations of {@link Message}.<br>
 * The {@link Message Messages} constructed via this class are intended to be received by a {@link Server}.
 */
public abstract class MessageForServer extends Message implements Serializable {

    /**
     * Sent by the client to confirm that the connection is still up.
     */
    public static Message ping() {
        return new MessagePing();
    }

    /**
     * Sent by the client upon connection to request an ID or to reconnect to its previous state.
     *
     * @param old_clientID previous clientId of the {@link Client}, 0 if none
     * @param nickname nickname for the client
     * @return the constructed {@link Message}
     */
    public static Message hello(int old_clientID, String nickname) {
        return new MessageHello(old_clientID, nickname);
    }

    /**
     * Requests the creation of a new lobby, with the associated options.
     *
     * @param size size of the new lobby
     * @param expert_mode mode for the new lobby
     * @return the constructed {@link Message}
     */
    public static Message createLobby(int size, boolean expert_mode) {
        return new MessageCreateLobby(size, expert_mode);
    }

    /**
     * Allows the sender to join the specified lobby, if possible.
     *
     * @param lobbyID id of the lobby to join
     * @return the constructed {@link Message}
     */
    public static Message joinLobby(int lobbyID) {
        return new MessageJoinLobby(lobbyID);
    }

    /**
     * Requests a list of all the server’s lobbies.
     *
     * @return the constructed {@link Message}
     */
    public static Message getLobbies() {
        return new MessageGetLobbies();
    }
}

/**
 * See: {@link Message}
 * Sent by the client to confirm that the connection is still up.
 */
class MessagePing extends MessageForServer implements Serializable {

    /**
     * Sent by the client to confirm that the connection is still up.
     *
     * @return the constructed {@link Message}
     */
    public MessagePing() {}

    /**
     * {@inheritDoc}
     */
    public void execute(Server server, SocketHandler socket_handler) {}

    /**
     * {@inheritDoc}
     */
    public void execute(ServerLobby serverLobby, SocketHandler socket_handler) {}
}

/**
 * See: {@link Message}
 * Sent by the client upon connection to request an ID or to reconnect to its previous state.
 */
class MessageHello extends MessageForServer implements Serializable {
    public final int old_clientID;
    public final String nickname;

    /**
     * Sent by the client upon connection to request an ID or to reconnect to its previous state.
     *
     * @param old_clientID previous clientId of the {@link Client}, 0 if none
     * @param nickname nickname for the client
     * @return the constructed {@link Message}
     */
    public MessageHello(int old_clientID, String nickname) {
        this.old_clientID = old_clientID;
        this.nickname = nickname;
    }

    /**
     * {@inheritDoc}
     */
    public void execute(Server server, SocketHandler socket_handler) {
        server.hello(socket_handler, old_clientID, nickname);
    }
}

/**
 * See: {@link Message}
 * Requests the creation of a new lobby, with the associated options.
 */
class MessageCreateLobby extends MessageForServer implements Serializable {
    public final int size;
    public final boolean expert_mode;

    /**
     * Requests the creation of a new lobby, with the associated options.
     *
     * @param size size of the new lobby
     * @param expert_mode mode for the new lobby
     * @return the constructed {@link Message}
     */
    public MessageCreateLobby(int size, boolean expert_mode) {
        this.size = size;
        this.expert_mode = expert_mode;
    }

    /**
     * {@inheritDoc}
     */
    public void execute(Server server, SocketHandler socket_handler) {
        server.createLobby(socket_handler, size, expert_mode);
    }
}

/**
 * See: {@link Message}
 * Allows the sender to join the specified lobby, if possible.
 */
class MessageJoinLobby extends MessageForServer implements Serializable {
    public final int lobbyID;

    /**
     * Allows the sender to join the specified lobby, if possible.
     *
     * @param lobbyID id of the lobby to join
     * @return the constructed {@link Message}
     */
    public MessageJoinLobby(int lobbyID) {
        this.lobbyID = lobbyID;
    }

    /**
     * {@inheritDoc}
     */
    public void execute(Server server, SocketHandler socket_handler) {
        server.joinLobby(socket_handler, lobbyID);
    }
}

/**
 * See: {@link Message}
 * Requests a list of all the server’s lobbies.
 */
class MessageGetLobbies extends MessageForServer implements Serializable {

    /**
     * Requests a list of all the server’s lobbies.
     *
     * @return the constructed {@link Message}
     */
    public MessageGetLobbies() {}

    /**
     * {@inheritDoc}
     */
    public void execute(Server server, SocketHandler socket_handler) {
        server.getLobbies(socket_handler);
    }
}