package Controller.ServerSide;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;

/**
 * Immutable class containing the data regarding a lobby.<br>
 * Those data being its setting, as per size and expert mode, the lobby id and the list of {@link ClientData clients} currently in the lobby.
 */
public class LobbyData implements Serializable {
    public final int lobbyID;
    public final int size;
    public final boolean expert_mode;
    public final List<ClientData> clients;

    /**
     * Constructs a new immutable instance of this class.
     *
     * @param lobbyID id of the represented lobby
     * @param size size of the represented lobby
     * @param expert_mode expert mode in the represented lobby
     * @param clients clients in the represented lobby
     */
    public LobbyData(int lobbyID, int size, boolean expert_mode, List<ClientData> clients) {
        this.lobbyID = lobbyID;
        this.size = size;
        this.expert_mode = expert_mode;
        this.clients = clients;
    }

    /**
     * Created a deep copy of this class.
     *
     * @return deep copy of this class
     */
    @Override
    public LobbyData clone() {
        return new LobbyData(lobbyID, size, expert_mode, new ArrayList<ClientData>(clients));
    }
}
