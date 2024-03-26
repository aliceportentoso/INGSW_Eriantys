package Controller;

import Controller.ServerSide.ServerLobby;
import Model.EffectParameters;

import java.io.Serializable;

/**
 * Abstract class consisting only of static methods.<br>
 * The methods of this class construct and return the associated implementations of {@link Message}.<br>
 * The {@link Message Messages} constructed via this class are intended to be received by a {@link ServerLobby}.
 */
public abstract class MessageForServerLobby extends Message implements Serializable {

    /**
     * Allows the client to leave its current lobby and go back to choosing/creating one.
     *
     * @return the constructed {@link Message}
     */
    public static Message leaveLobby() {
        return new MessageLeaveLobby();
    }

    /**
     * Allows the client to let the lobby know when it is ready to start the game.
     *
     * @return the constructed {@link Message}
     */
    public static Message toggleReady() {
        return new MessageToggleReady();
    }

    /**
     * Used by the server to send the update to the other clients.
     *
     * @param card_index index of the played card
     * @return the constructed {@link Message}
     */
    public static Message cardPlayed(int card_index) {
        return new MessageCardPlayedLB(card_index);
    }

    /**
     * Used by the server to send the update to the other clients.
     *
     * @param student_index index of the moved student
     * @return the constructed {@link Message}
     */
    public static Message studentSetToHall(int student_index) {
        return new MessageStudentSetToHallLB(student_index);
    }

    /**
     * Used by the server to send the update to the other clients.
     *
     * @param student_index index of the moved student
     * @param island index of the target island
     * @return the constructed {@link Message}
     */
    public static Message studentSetToIsland(int student_index, Integer island) {
        return new MessageStudentSetToIslandLB(student_index, island);
    }

    /**
     * Used by the server to send the update to the other clients.
     *
     * @param moved steps mother nature has been moved by
     * @return the constructed {@link Message}
     */
    public static Message motherNatureMoved(int moved) {
        return new MessageMotherNatureMovedLB(moved);
    }

    /**
     * Used by the server to send the update to the other clients.
     *
     * @param cloud_index index of the chosen cloud
     * @return the constructed {@link Message}
     */
    public static Message cloudChosen(int cloud_index) {
        return new MessageCloudChosenLB(cloud_index);
    }

    /**
     * Used by the server to send the update to the other clients.
     *
     * @param npc_index index of the activated Npc
     * @param effect_parameters paramters for teh activated Npc
     * @return the constructed {@link Message}
     */
    public static Message npcActivated(int npc_index, EffectParameters effect_parameters) {
        return new MessageNpcActivatedLB(npc_index, effect_parameters);
    }

    /**
     * Used by a client to request the server a fresh copy of the model.
     */
    public static Message resync() {
        return new MessageResync();
    }
}

/**
 * See: {@link Message}
 * Allows the client to leave its current lobby and go back to choosing/creating one.
 */
class MessageLeaveLobby extends MessageForServerLobby implements Serializable {

    /**
     * Allows the client to leave its current lobby and go back to choosing/creating one.
     *
     * @return the constructed {@link Message}
     */
    public MessageLeaveLobby() {}

    /**
     * {@inheritDoc}
     */
    public void execute(ServerLobby lobby, SocketHandler socket_handler) {
        lobby.leaveLobby(socket_handler);
    }
}

/**
 * See: {@link Message}
 * Allows the client to let the lobby know when it is ready to start the game.
 */
class MessageToggleReady extends MessageForServerLobby implements Serializable {

    /**
     * Allows the client to let the lobby know when it is ready to start the game.
     *
     * @return the constructed {@link Message}
     */
    public MessageToggleReady() {}

    /**
     * {@inheritDoc}
     */
    public void execute(ServerLobby lobby, SocketHandler socket_handler) {
        lobby.toggleReady(socket_handler);
    }
}

/**
 * See: {@link Message}
 * Allows the currently playing player to play the selected card during its turn (playCard).
 */
class MessageCardPlayedLB extends MessageForServerLobby implements Serializable {
    public final int card_index;

    /**
     * Allows the currently playing player to play the selected card during its turn (playCard).
     *
     * @param card_index index of the played card
     * @return the constructed {@link Message}
     */
    public MessageCardPlayedLB(int card_index) {
        this.card_index = card_index;
    }

    /**
     * {@inheritDoc}
     */
    public void execute(ServerLobby lobby, SocketHandler socket_handler) {
        lobby.cardPlayed(socket_handler, card_index);
    }
}

/**
 * See: {@link Message}
 * Allows the current playing client to set a student to its hall.
 */
class MessageStudentSetToHallLB extends MessageForServerLobby implements Serializable {
    public final int student_index;

    /**
     * Allows the current playing client to set a student to its hall.
     *
     * @param student_index index of the moved student
     * @return the constructed {@link Message}
     */
    public MessageStudentSetToHallLB(int student_index) {
        this.student_index = student_index;
    }

    /**
     * {@inheritDoc}
     */
    public void execute(ServerLobby lobby, SocketHandler socket_handler) {
        lobby.studentSetToHall(socket_handler, student_index);
    }
}

/**
 * See: {@link Message}
 * Allows the current playing client to set a student to an island.
 */
class MessageStudentSetToIslandLB extends MessageForServerLobby implements Serializable {
    public final int student_index;
    public final Integer island;

    /**
     * Allows the current playing client to set a student to an island.
     *
     * @param student_index index of the moved student
     * @param island index of the target island
     * @return the constructed {@link Message}
     */
    public MessageStudentSetToIslandLB(int student_index, Integer island) {
        this.student_index = student_index;
        this.island = island;
    }

    /**
     * {@inheritDoc}
     */
    public void execute(ServerLobby lobby, SocketHandler socket_handler) {
        lobby.studentSetToIsland(socket_handler, student_index, island);
    }
}

/**
 * See: {@link Message}
 * Allows the client to move mother nature of his desired number of steps.
 */
class MessageMotherNatureMovedLB extends MessageForServerLobby implements Serializable {
    public final int moved;

    /**
     * Allows the client to move mother nature of his desired number of steps.
     *
     * @param moved steps mother nature has been moved by
     * @return the constructed {@link Message}
     */
    public MessageMotherNatureMovedLB(int moved) {
        this.moved = moved;
    }

    /**
     * {@inheritDoc}
     */
    public void execute(ServerLobby lobby, SocketHandler socket_handler) {
        lobby.motherNatureMoved(socket_handler, moved);
    }
}

/**
 * See: {@link Message}
 * Allows the client to move the students form a cloud to his entrance on its dashboard.
 */
class MessageCloudChosenLB extends MessageForServerLobby implements Serializable {
    public final int cloud_index;

    /**
     * Allows the client to move the students form a cloud to his entrance on its dashboard.
     *
     * @param cloud_index index of the chosen cloud
     * @return the constructed {@link Message}
     */
    public MessageCloudChosenLB(int cloud_index) {
        this.cloud_index = cloud_index;
    }

    /**
     * {@inheritDoc}
     */
    public void execute(ServerLobby lobby, SocketHandler socket_handler) {
        lobby.cloudChosen(socket_handler, cloud_index);
    }
}

/**
 * See: {@link Message}
 * Allows the client to activate an NPC effect.
 */
class MessageNpcActivatedLB extends MessageForServerLobby implements Serializable {
    public final int npc_index;
    public final EffectParameters effect_parameters;

    /**
     * Allows the client to activate an NPC effect.
     *
     * @param npc_index index of the activated Npc
     * @param effect_parameters paramters for teh activated Npc
     * @return the constructed {@link Message}
     */
    public MessageNpcActivatedLB(int npc_index, EffectParameters effect_parameters) {
        this.npc_index = npc_index;
        this.effect_parameters = effect_parameters;
    }

    /**
     * {@inheritDoc}
     */
    public void execute(ServerLobby lobby, SocketHandler socket_handler) {
        lobby.npcActivated(socket_handler, npc_index, effect_parameters);
    }
}

/**
 * See: {@link Message}
 * Used by a client to request the server a fresh copy of the model.
 */
class MessageResync extends MessageForServerLobby implements Serializable {

    /**
     * Used by a client to request the server a fresh copy of the model.
     */
    public MessageResync() {
    }

    /**
     * {@inheritDoc}
     */
    public void execute(ServerLobby lobby, SocketHandler socket_handler) {
        lobby.resync(socket_handler);
    }
}