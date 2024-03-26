package Controller.ServerSide;

import Controller.SocketHandler;

import java.io.Serializable;

/**
 * Class memorizing all of a client's data known to the {@link Server} class.<br>
 * Associates a client with its connection via a {@link SocketHandler} instance and contains both the client's nickname and id.<br>
 * This class also contains the current id of the lobby the client is in at all times.
 */
public class ClientData implements Serializable {
    public final int clientID;
    public final String nickname;
    public transient SocketHandler socket_handler;
    private int lobbyID;

    /**
     * Constructs an instance of this class for a client not inside a lobby, hence with an uninitialized lobbyId.
     *
     * @param clientID id of the client represented by this class
     * @param nickname nickname of the client represented by this class
     * @param socket_handler {@link SocketHandler} wrapping this client's connection
     */
    public ClientData(int clientID, String nickname, SocketHandler socket_handler) {
        this.clientID = clientID;
        this.nickname = nickname;
        this.socket_handler = socket_handler;
        this.lobbyID = 0;
    }

    /**
     * Constructs an instance of this class for a client.
     *
     * @param clientID id of the client represented by this class
     * @param nickname nickname of the client represented by this class
     * @param socket_handler {@link SocketHandler} wrapping this client's connection
     * @param lobbyID the id of the lobby the client is in
     */
    public ClientData(int clientID, String nickname, SocketHandler socket_handler, int lobbyID) {
        this.clientID = clientID;
        this.nickname = nickname;
        this.socket_handler = socket_handler;
        this.lobbyID = lobbyID;
    }

    /**
     * Provides the id for the current lobby this client is in.
     *
     * @return the client's current lobbyId
     */
    public int getLobbyID() {
        return lobbyID;
    }

    /**
     * Changes the client's associated lobbyId as the client joins or leaves lobbies.
     *
     * @param lobbyID new lobbyId
     */
    public void setLobbyID(int lobbyID) {
        this.lobbyID = lobbyID;
    }

    /**
     * Removes the {@link SocketHandler} associated this client due to its connection being terminated and not
     * being valid anymore. Used only in a lobby.<br>
     * An instance of this class without a socket handler will be deleted as soon as the lobby the client was in will end its current game.
     */
    public void removeSocketHandler() {
        socket_handler = null;
    }

    /**
     * Compares two instances of {@link ClientData}
     *
     * @param o other object for the comparison
     * @return true if the two instances are the same
     */
    @Override
    public boolean equals(Object o) {
        if(o == this) {
            return true;
        }
        if(!(o instanceof ClientData)) {
            return false;
        }
        return this.clientID == ((ClientData) o).clientID;
    }

    /**
     * Provides a deep copy of this class.
     *
     * @return a deep copy of this class
     */
    @Override
    public ClientData clone() {
        return new ClientData (clientID, nickname, socket_handler, lobbyID);
    }
}
