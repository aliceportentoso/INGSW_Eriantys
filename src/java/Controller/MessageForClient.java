package Controller;

import Controller.ClientSide.Client;
import Controller.ServerSide.LobbyData;
import Model.Colors;
import Model.EffectParameters;
import Model.Game;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;

/**
 * Abstract class consisting only of static methods.<br>
 * The methods of this class construct and return the associated implementations of {@link Message}.<br>
 * The {@link Message Messages} constructed via this class are intended to be received by a {@link Client}.
 */
public abstract class MessageForClient extends Message implements Serializable {

    /**
     * Acknowledges the client’s connection and gives to it its ID.
     *
     * @param old_clientID id for the new client
     * @return the constructed {@link Message}
     */
    public static Message ack(int old_clientID) {
        return new MessageAck(old_clientID);
    }

    /**
     * Allows the client to know his current lobby.
     *
     * @param lobby {@link LobbyData} of the current lobby
     * @return the constructed {@link Message}
     */
    public static Message setLobby(LobbyData lobby) {
        return new MessageSetLobby(lobby);
    }

    /**
     * Contains a list of all the currently active lobbies on the server, both full and not.
     *
     * @param lobbies list of {@link LobbyData}, the available lobbies
     * @return the constructed {@link Message}
     */
    public static Message lobbiesList(List<LobbyData> lobbies) {
        return new MessageLobbiesList(lobbies);
    }

    /**
     * Returns to every client a list of flags, each one indicating if a member of the lobby is currently ready or not.
     *
     * @param ready_array radiness status of the lobby
     * @return the constructed {@link Message}
     */
    public static Message readiness(boolean[] ready_array) {
        return new MessageReadiness(ready_array);
    }

    /**
     * Allows the currently playing player to play the selected card during its turn (playCard).
     *
     * @param clientID if of the playing player (message creator)
     * @param card_index index of the played ard
     * @return the constructed {@link Message}
     */
    public static Message cardPlayed(int clientID, int card_index) {
        return new MessageCardPlayed(clientID, card_index);
    }

    /**
     * Allows the current playing client to set a student to its hall.
     *
     * @param clientID if of the playing player (message creator)
     * @param student_index moved student index
     * @return the constructed {@link Message}
     */
    public static Message studentSetToHall(int clientID, int student_index) {
        return new MessageStudentSetToHall(clientID, student_index);
    }

    /**
     * Allows the current playing client to set a student to an island.
     *
     * @param clientID if of the playing player (message creator)
     * @param student_index index of the moved student
     * @param island index of the target island
     * @return the constructed {@link Message}
     */
    public static Message studentSetToIsland(int clientID, int student_index, int island) {
        return new MessageStudentSetToIsland(clientID, student_index, island);
    }

    /**
     * Allows the client to move mother nature of his desired number of steps.
     *
     * @param clientID if of the playing player (message creator)
     * @param moved steps mother nature is moved by
     * @return the constructed {@link Message}
     */
    public static Message motherNatureMoved(int clientID, int moved) {
        return new MessageMotherNatureMoved(clientID, moved);
    }

    /**
     * Allows the client to move the students form a cloud to his entrance on its dashboard.
     *
     * @param clientID if of the playing player (message creator)
     * @param cloud_index index of the chosen cloud
     * @return the constructed {@link Message}
     */
    public static Message cloudChosen(int clientID, int cloud_index) {
        return new MessageCloudChosen(clientID, cloud_index);
    }

    /**
     * Allows the client to activate an NPC effect.
     *
     * @param clientID if of the playing player (message creator)
     * @param npc_index index of the activated Npc
     * @param effect_parameters parameters for the Npc's activation
     * @return the constructed {@link Message}
     */
    public static Message npcActivated(int clientID, int npc_index, EffectParameters effect_parameters) {
        return new MessageNpcActivated(clientID, npc_index, effect_parameters);
    }

    /**
     * Used by the server to notify the successfulness of the move to the client.
     *
     * @return the constructed {@link Message}
     */
    public static Message moveSuccessful() {
        return new MessageMoveSuccessful();
    }

    /**
     * Signals to clients that due to a disconnected player, the current turn must be skipped.
     *
     * @return the constructed {@link Message}
     */
    public static Message skipTurn() {
        return new MessageSkipTurn();
    }

    /**
     * Returns the currently available clouds (and their students).
     *
     * @param clouds updated clouds
     * @return the constructed {@link Message}
     */
    public static Message cloudsUpdate(List<Colors>[] clouds) {
        return new MessageCloudsUpdated(clouds);
    }

    /**
     * Specific massage to propagate the updated students on those npcs that have some student on it.
     *
     * @param npc_index index of the updated Npc
     * @param students updated students of the Npc
     * @return the constructed {@link Message}
     */
    public static Message npcUpdated(int npc_index, List<Colors> students) {
        return new MessageNpcUpdated(npc_index, students);
    }

    /**
     * Instructs the clients to revert the game at the previous valid state.
     *
     * @return the constructed {@link Message}
     */
    public static Message revert() {
        return new MessageRevert();
    }

    /**
     * Indicates to the clients in a lobby the beginning of a match.
     *
     * @param game instance of game of the lobby
     * @return the constructed {@link Message}
     */
    public static Message gameStarted(Game game) {
        return new MessageGameStarted(game);
    }

    /**
     * Indicates to the clients in a lobby the end of a match, with the winner.
     *
     * @param winnerId id of the player who won
     * @return the constructed {@link Message}
     */
    public static Message gameEnded(int winnerId) {
        return new MessageGameEnded(winnerId);
    }

    /**
     * Sent whenever the server can’t satisfy a client’s request.
     *
     * @param msg error message
     * @return the constructed {@link Message}
     */
    public static Message error(String msg) {
        return new MessageError(msg, 0);
    }

    /**
     * Sent whenever the server can’t satisfy a client’s request.
     * <br><br>
     * The error codes are:
     * <ol start="0">
     *     <li> Default error code, not normally used.
     *     <li> Unneeded action, nothing has changed.
     *     <li> Error during reconnection.
     *     <li> Login error.
     *     <li> Invalid arguments.
     *     <li> Action not permitted.
     *     <li> Server error, request not fulfilled, nothing has changed.
     *     <li> Invalid move.
     * </ol>
     *
     * @param msg error message
     * @param errorCode error code of the message
     * @return the constructed {@link Message}
     */
    public static Message error(String msg, int errorCode) {
        return new MessageError(msg, errorCode);
    }
}

/**
 * See: {@link Message}
 * Acknowledges the client’s connection and gives to it its ID.
 */
class MessageAck extends MessageForClient implements Serializable {
    public final int clientID;

    /**
     * Acknowledges the client’s connection and gives to it its ID.
     *
     * @param clientID id for the new client
     * @return the constructed {@link Message}
     */
    public MessageAck(int clientID) {
        this.clientID = clientID;
    }

    /**
     * {@inheritDoc}
     */
    public void execute(Client client, SocketHandler socket_handler) {
        client.ack(socket_handler, clientID);
    }
}

/**
 * See: {@link Message}
 * Allows the client to know his current lobby.
 */
class MessageSetLobby extends MessageForClient implements Serializable {
    public final LobbyData lobby;

    /**
     * Allows the client to know his current lobby.
     *
     * @param lobby {@link LobbyData} of the current lobby
     * @return the constructed {@link Message}
     */
    public MessageSetLobby(LobbyData lobby) {
        if(lobby == null)
            this.lobby = null;
        else
            this.lobby = lobby.clone();
    }

    /**
     * {@inheritDoc}
     */
    public void execute(Client client, SocketHandler socket_handler) {
        client.setLobby(socket_handler, lobby);
    }
}

/**
 * See: {@link Message}
 * Contains a list of all the currently active lobbies on the server, both full and not.
 */
class MessageLobbiesList extends MessageForClient implements Serializable {
    public final List<LobbyData> lobbies;

    /**
     * Contains a list of all the currently active lobbies on the server, both full and not.
     *
     * @param lobbies list of {@link LobbyData}, the available lobbies
     * @return the constructed {@link Message}
     */
    public MessageLobbiesList(List<LobbyData> lobbies) {
        this.lobbies = new ArrayList<LobbyData>(lobbies);
    }

    /**
     * {@inheritDoc}
     */
    public void execute(Client client, SocketHandler socket_handler) {
        client.lobbiesList(socket_handler, lobbies);
    }
}

/**
 * See: {@link Message}
 * Returns to every client a list of flags, each one indicating if a member of the lobby is currently ready or not.
 */
class MessageReadiness extends MessageForClient implements Serializable {
    public final boolean[] ready_array;

    /**
     * Returns to every client a list of flags, each one indicating if a member of the lobby is currently ready or not.
     *
     * @param ready_array radiness status of the lobby
     * @return the constructed {@link Message}
     */
    public MessageReadiness(boolean[] ready_array) {
        this.ready_array = ready_array.clone();
    }

    /**
     * {@inheritDoc}
     */
    public void execute(Client client, SocketHandler socket_handler) {
        client.readiness(socket_handler, ready_array);
    }
}

/**
 * See: {@link Message}
 * Used by the server to send the update to the other clients.
 */
class MessageCardPlayed extends MessageForClient implements Serializable {
    public final int clientID, card_index;

    /**
     * Used by the server to send the update to the other clients.
     *
     * @param clientID if of the playing player (message creator)
     * @param card_index index of the played ard
     * @return the constructed {@link Message}
     */
    public MessageCardPlayed(int clientID, int card_index) {
        this.clientID = clientID;
        this.card_index = card_index;
    }

    /**
     * {@inheritDoc}
     */
    public void execute(Client client, SocketHandler socket_handler) {
        client.cardPlayed(socket_handler, clientID, card_index);
    }
}

/**
 * See: {@link Message}
 * Used by the server to send the update to the other clients.
 */
class MessageStudentSetToHall extends MessageForClient implements Serializable {
    public final int clientID, student_index;

    /**
     * Used by the server to send the update to the other clients.
     *
     * @param clientID if of the playing player (message creator)
     * @param student_index moved student index
     * @return the constructed {@link Message}
     */
    public MessageStudentSetToHall(int clientID, int student_index) {
        this.clientID = clientID;
        this.student_index = student_index;
    }

    /**
     * {@inheritDoc}
     */
    public void execute(Client client, SocketHandler socket_handler) {
        client.studentSetToHall(socket_handler, clientID, student_index);
    }
}

/**
 * See: {@link Message}
 * Used by the server to send the update to the other clients.
 */
class MessageStudentSetToIsland extends MessageForClient implements Serializable {
    public final int clientID, student_index;
    public final int island;

    /**
     * Used by the server to send the update to the other clients.
     *
     * @param clientID if of the playing player (message creator)
     * @param student_index index of the moved student
     * @param island index of the target island
     * @return the constructed {@link Message}
     */
    public MessageStudentSetToIsland(int clientID, int student_index, int island) {
        this.clientID = clientID;
        this.student_index = student_index;
        this.island = island;
    }

    /**
     * {@inheritDoc}
     */
    public void execute(Client client, SocketHandler socket_handler) {
        client.studentSetToIsland(socket_handler, clientID, student_index, island);
    }
}

/**
 * See: {@link Message}
 * Used by the server to send the update to the other clients.
 */
class MessageMotherNatureMoved extends MessageForClient implements Serializable {
    public final int clientID, moved;

    /**
     * Used by the server to send the update to the other clients.
     *
     * @param clientID if of the playing player (message creator)
     * @param moved steps mother nature is moved by
     * @return the constructed {@link Message}
     */
    public MessageMotherNatureMoved(int clientID, int moved) {
        this.clientID = clientID;
        this.moved = moved;
    }

    /**
     * {@inheritDoc}
     */
    public void execute(Client client, SocketHandler socket_handler) {
        client.motherNatureMoved(socket_handler, clientID, moved);
    }
}

/**
 * See: {@link Message}
 * Used by the server to send the update to the other clients.
 */
class MessageCloudChosen extends MessageForClient implements Serializable {
    public final int clientID, cloud_index;

    /**
     * Used by the server to send the update to the other clients.
     *
     * @param clientID if of the playing player (message creator)
     * @param cloud_index index of the chosen cloud
     * @return the constructed {@link Message}
     */
    public MessageCloudChosen(int clientID, int cloud_index) {
        this.clientID = clientID;
        this.cloud_index = cloud_index;
    }

    /**
     * {@inheritDoc}
     */
    public void execute(Client client, SocketHandler socket_handler) {
        client.cloudChosen(socket_handler, clientID, cloud_index);
    }
}

/**
 * See: {@link Message}
 * Used by the server to send the update to the other clients.
 */
class MessageNpcActivated extends MessageForClient implements Serializable {
    public final int clientID, npc_index;
    public final EffectParameters effect_parameters;

    /**
     * Used by the server to send the update to the other clients.
     *
     * @param clientID if of the playing player (message creator)
     * @param npc_index index of the activated Npc
     * @param effect_parameters parameters for the Npc's activation
     * @return the constructed {@link Message}
     */
    public MessageNpcActivated(int clientID, int npc_index, EffectParameters effect_parameters) {
        this.clientID = clientID;
        this.npc_index = npc_index;
        this.effect_parameters = effect_parameters;
    }

    /**
     * {@inheritDoc}
     */
    public void execute(Client client, SocketHandler socket_handler) {
        client.npcActivated(socket_handler, clientID, npc_index, effect_parameters);
    }
}

/**
 * See: {@link Message}
 * Signals to clients that due to a disconnected player, the current turn must be skipped.
 */
class MessageSkipTurn extends MessageForClient implements Serializable {

    /**
     * Signals to clients that due to a disconnected player, the current turn must be skipped.
     *
     * @return the constructed {@link Message}
     */
    public MessageSkipTurn() {}

    /**
     * {@inheritDoc}
     */
    public void execute(Client client, SocketHandler socket_handler) {
        client.skipTurn(socket_handler);
    }
}

/**
 * See: {@link Message}
 * Used by the server to notify the successfulness of the move to the client.
 */
class MessageMoveSuccessful extends MessageForClient implements Serializable {

    /**
     * Used by the server to notify the successfulness of the move to the client.
     *
     * @return the constructed {@link Message}
     */
    public MessageMoveSuccessful() {}

    /**
     * {@inheritDoc}
     */
    public void execute(Client client, SocketHandler socket_handler) {
        client.moveSuccessful(socket_handler);
    }
}

/**
 * See: {@link Message}
 * Returns the currently available clouds (and their students).
 */
class MessageCloudsUpdated extends MessageForClient implements Serializable {
    public final List<Colors>[] clouds;

    /**
     * Returns the currently available clouds (and their students).
     *
     * @param clouds updated clouds
     * @return the constructed {@link Message}
     */
    public MessageCloudsUpdated(List<Colors>[] clouds) {
        this.clouds = clouds.clone();
    }

    /**
     * {@inheritDoc}
     */
    public void execute(Client client, SocketHandler socket_handler) {
        client.cloudsUpdated(socket_handler, clouds);
    }
}

/**
 * See: {@link Message}
 * Specific massage to propagate the updated students on those npcs that have some student on it.
 */
class MessageNpcUpdated extends MessageForClient implements Serializable {
    public final int npc_index;
    public final List<Colors> students;

    /**
     * Specific massage to propagate the updated students on those npcs that have some student on it.
     *
     * @param npc_index index of the updated Npc
     * @param students updated students of the Npc
     * @return the constructed {@link Message}
     */
    public MessageNpcUpdated(int npc_index, List<Colors> students) {
        this.npc_index = npc_index;
        this.students = new ArrayList<Colors>(students);
    }

    /**
     * {@inheritDoc}
     */
    public void execute(Client client, SocketHandler socket_handler) {
        client.npcUpdated(socket_handler, npc_index, students);
    }
}

/**
 * See: {@link Message}
 * Instructs the clients to revert the game at the previous valid state.
 */
class MessageRevert extends MessageForClient implements Serializable {

    /**
     * Instructs the clients to revert the game at the previous valid state.
     *
     * @return the constructed {@link Message}
     */
    public MessageRevert() {}

    /**
     * {@inheritDoc}
     */
    public void execute(Client client, SocketHandler socket_handler) {
        client.revert(socket_handler);
    }
}

/**
 * See: {@link Message}
 * Indicates to the clients in a lobby the beginning of a match.
 */
class MessageGameStarted extends MessageForClient implements Serializable {
    public final Game game;

    /**
     * Indicates to the clients in a lobby the beginning of a match.
     *
     * @param game instance of game of the lobby
     * @return the constructed {@link Message}
     */
    public MessageGameStarted(Game game) {
        this.game = game.copy();
    }

    /**
     * {@inheritDoc}
     */
    public void execute(Client client, SocketHandler socket_handler) {
        client.gameStart(socket_handler, game);
    }
}

/**
 * See: {@link Message}
 * Indicates to the clients in a lobby the end of a match, with the winner.
 */
class MessageGameEnded extends MessageForClient implements Serializable {
    public final int winnerId;

    /**
     * Indicates to the clients in a lobby the end of a match, with the winner.
     *
     * @param winnerId id of the player who won
     * @return the constructed {@link Message}
     */
    public MessageGameEnded(int winnerId) {
        this.winnerId = winnerId;
    }

    /**
     * {@inheritDoc}
     */
    public void execute(Client client, SocketHandler socket_handler) {
        client.gameEnd(socket_handler, winnerId);
    }
}

/**
 * See: {@link Message}
 * Sent whenever the server can’t satisfy a client’s request.
 */
class MessageError extends MessageForClient implements Serializable {
    public final String msg;
    public final int errorCode;

    /**
     * Sent whenever the server can’t satisfy a client’s request.
     * <br><br>
     * The error codes are:
     * <ol start="0">
     *     <li> Default error code, not normally used.
     *     <li> Unneeded action, nothing has changed.
     *     <li> Error during reconnection.
     *     <li> Login error.
     *     <li> Invalid arguments.
     *     <li> Action not permitted.
     *     <li> Server error, request not fulfilled, nothing has changed.
     *     <li> Invalid move.
     * </ol>
     *
     * @param msg error message
     * @param errorCode error code of the message
     * @return the constructed {@link Message}
     */
    public MessageError(String msg, int errorCode) {
        this.msg = msg;
        this.errorCode = errorCode;
    }

    /**
     * {@inheritDoc}
     */
    public void execute(Client client, SocketHandler socket_handler) {
        client.error(socket_handler, msg, errorCode);
    }
}