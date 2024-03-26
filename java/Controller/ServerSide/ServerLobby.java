package Controller.ServerSide;

import Controller.*;
import Exceptions.InvalidMoveException;
import Exceptions.LobbyException;
import Model.Colors;
import Model.EffectParameters;
import Model.Game;
import org.jetbrains.annotations.TestOnly;

import java.util.*;
import java.util.stream.Collectors;

/*
Receives clients aka SocketHandlers for clients form the server along with their clientID and lets them join the lobby,
here is where the Game instance will reside for the lobby and where a match will be played.
 */

//the game starts only when every player is ready and there is a number of players equal to size

/*at each turn change, check if the player is connected (socket.isClosed()), if it is not, start a thread that sleeps for a given
   amount of time and then auto-plays for him. If a player is instead flagged as not ready DURING a game, its turn is skipped entirely
*/

/**
 * Class constituting a lobby on the server,<br>
 * a game can take place only inside a lobby, hence clients, after connecting to the {@link Server}, will create and join through it a lobby,
 * a process which will result in an instance of this class being created and clients being able to join it.<br>
 * The parameters essential for a lobby are the lobby's size and its expert mode flag. In particular a lobby must be full to its maximum capacity
 * indicated by size to allow a game to start.<br>
 * Each client has a readiness state inside the lobby, which starts at false, and can be toggled via {@link MessageForServerLobby#toggleReady}, the game inside
 * the lobby starts when every client is ready.<br>
 * During a game the lobby prevents a readiness change and handles eventual disconnections by skipping turns.<br>
 * After a game finishes the lobby continues existing and is reset to its initials state, keeping its clients but setting their readiness to false,
 * enabling fast rematches.
 * <br><br>
 * Since it implements {@link Controller}, it is an <strong>OBSERVER</strong> to every {@link SocketHandler} of a connected client.
 * For joining clients, it sets to itself the target of their {@link SocketHandler}, while it sets to its associated {@link Server} the target of the leaving ones.
 */
public class ServerLobby extends Controller {
    private static final int AUTOPLAY_TIMER = 32000;

    private final Server server;
    public final int lobbyID;

    //those 2 attributes must be set during the creation of the lobby, in the future it might be user-friendly to allow the lobby's creator to modify them
    public final int size; //the maximum number of players allowed to join the lobby and also the amount needed to start a game
    public final boolean expert_mode; //weather or not expert mode is activated for this lobby

    private List<ClientData> clients; //list of the clientIDs for the players in the lobby

    /**
     * @return reference to the list containing this lobby's {@link ClientData}s ({@link ServerLobby#clients})
     *  @implNote This method is meant to be used for testing purposes only.
     */
    @TestOnly
    public List<ClientData> getClients() {
        return clients;
    }

    private boolean[] ready;
    private Map<Integer, Thread> autoplay_threads;

    private Game game;

    /**
     * @return a copy of this lobby's {@link Game} using {@link Game#copy()}
     * @implNote This method is meant to be used for testing purposes only.
     */
    @TestOnly
    public synchronized Game getGame() {return game == null ? null : game.copy();}

    /**
     * Prepares a new lobby with the provided configuration of size and expert mode, after a lobby is constructed it can
     * already accept joining clients via the {@link ServerLobby#addPlayer} method.
     *
     * @param server {@link Server} which hosts the lobby
     * @param lobbyID new unique id given the new lobby
     * @param size maximum number of players allowed in the lobby, the lobby must be full for a game to start.
     * @param expert_mode flag indicating whether expert mode is enabled or not in this lobby
     */
    public ServerLobby(Server server, int lobbyID, int size, boolean expert_mode) {
        this.server = server;
        this.lobbyID = lobbyID;
        this.size = size;
        this.expert_mode = expert_mode;
        this.clients = new ArrayList<ClientData>();
        this.ready = new boolean[size];
        this.autoplay_threads = new HashMap<Integer, Thread>();
        this.game = null;
    }

    //Properly adds the given player to the lobby
    /**
     * Adds the given client to the lobby. Consequently, the player will "join" the lobby.
     * Joining a lobby means that the client's {@link SocketHandler} has its observer now set to this {@link ServerLobby} instance.
     *
     * @param client client to add to the lobby
     * @throws LobbyException thrown if the client cannot join due to the lobby being full or the client already being in it
     */
    public synchronized void addPlayer(ClientData client) throws LobbyException {
        if(clients.size() == size)
            throw new LobbyException("lobby already full");
        if(clients.stream().anyMatch(c -> c.equals(client)))
            throw new LobbyException("player already in lobby");

        client.socket_handler.changeUpdatesTarget(this);
        clients.add(client);
        client.setLobbyID(lobbyID);

        for(ClientData clientData : clients) {
            if(client.socket_handler != null) {
                clientData.socket_handler.send(MessageForClient.setLobby(this.getLobbyData()));
                clientData.socket_handler.send(MessageForClient.readiness(this.ready));
            }
        }
    }

    //Enables a player to join back the lobby after a reconnect
    /**
     * Allows a player to join back the lobby after it got disconnected and reconnected back with the server with the same
     * credentials as before. This method completes a session recovery be allowing the reconnection player
     * to get back into any ongoing game.
     *
     * @param client client being reconnected
     * @throws LobbyException thrown if the player was not in this lobby before disconnecting
     */
    public synchronized void reconnectPlayer(ClientData client) throws LobbyException {
        if(clients.stream().noneMatch(c -> c.clientID == client.clientID))
            throw new LobbyException("the player was not in this lobby");

        client.socket_handler.changeUpdatesTarget(this);
        for(int i = 0; i < clients.size(); i++) {
            if(clients.get(i).clientID == client.clientID) {
                clients.remove(i);
                clients.add(i, client);
                break;
            }
        }
        client.setLobbyID(lobbyID);

        for(ClientData clientData : clients) {
            if(clientData.socket_handler != null) {
                clientData.socket_handler.send(MessageForClient.setLobby(this.getLobbyData()));
                clientData.socket_handler.send(MessageForClient.readiness(this.ready));
            }
        }

        if(game != null) {
            if(autoplay_threads.get(client.clientID) != null) {
                autoplay_threads.get(client.clientID).stop();
                autoplay_threads.remove(client.clientID);
            }
            client.socket_handler.send(MessageForClient.gameStarted(game));
            System.out.println("Reconnected " + client.clientID + " to the game");
        }
    }

    //Called when a client loses his chance to reconnect, this either frees a slot in the lobby or lets the game continue without him
    /**
     * Method for when a client loses his chance to reconnect after {@link Server#PERMANENT_DISCONNECTION_TIME}, this either frees a slot in the lobby,
     * if there is no ongoing game, or lets the game continue without the disconnected player, skipping his turns.
     *
     * @param clientID permanently disconnected player
     */
    public synchronized void permanentDisconnectedPlayer(int clientID) {
        if(game == null) {
            for(int i = 0; i < clients.size(); i++) {
                if(clients.get(i).clientID == clientID) {
                    clients.remove(i);
                    break;
                }
            }
            Arrays.fill(ready, false);

            for(ClientData clientData : clients) {
                if(clientData.socket_handler != null) {
                    clientData.socket_handler.send(MessageForClient.setLobby(this.getLobbyData()));
                    clientData.socket_handler.send(MessageForClient.readiness(this.ready));
                }
            }

            //if there are no players left, delete the lobby
            if(clients.size() == 0) {
                //unfortunately under heavy load this may take a while...
                server.deleteLobby(lobbyID);
            }
        } else {
            for(int i = 0; i < clients.size(); i++) {
                if(clients.get(i).clientID == clientID) {
                    ready[i] = false;
                    clients.get(i).removeSocketHandler();
                    break;
                }
            }

            int ready_count = 0;
            int ready_index = 0;
            for(int i = 0; i < clients.size(); i++) {
                if(ready[i]) {
                    ready_count++;
                    ready_index = i;
                    clients.get(i).socket_handler.send(MessageForClient.readiness(ready));
                }
            }

            //if there is only a ready player, he wins and the lobby gets reset for a new game
            if(ready_count == 1) {
                for(ClientData client : clients)
                    if(client.socket_handler != null) {
                        client.socket_handler.send(MessageForClient.gameEnded(clients.get(ready_index).clientID));
                    }
                resetLobby();
            } else if(game.currentlyPlayingPlayer() == clientID) {
                //if the player who disconnected was the one who had to play, skip his turn

                if(autoplay_threads.get(clientID) != null) {
                    autoplay_threads.get(clientID).stop();
                    autoplay_threads.remove(clientID);
                }

                //do not revert to previous state if not needed
                boolean revert_needed = game.getPhase() == 1 && (game.getStep() != 0 || game.getMovedStudents() != 0 || game.getNpcEffect() != 0);
                if(revert_needed)
                    game = Game.revertToPreviousState(game);

                try {
                    game.skipTurn();

                    for (ClientData client : clients) {
                        if(client.socket_handler != null) {
                            if(revert_needed)
                                client.socket_handler.send(MessageForClient.revert());
                            client.socket_handler.send(MessageForClient.skipTurn());
                        }
                    }

                    checkWinner();
                } catch (InvalidMoveException e) {
                    e.printStackTrace();
                }
            }
        }

        System.out.println("Permanently disconnected player: " + clientID + ", from lobby: " + lobbyID);
    }

    /**
     * {@inheritDoc}
     *
     * @param client_handler the {@link SocketHandler} which received the {@link Message} and is dispatching the update
     * @param message the {@link Message} received
     */
    public synchronized void update(SocketHandler client_handler, Message message) {
        try {
            //System.out.println("Update handled.");

            message.execute(this, client_handler);
        } catch(Exception e) {
            e.printStackTrace();
        }
    }

    //Redirects the disconnect to the server
    /**
     * {@inheritDoc}<br>
     * Before a client is completely forgotten tho, there is a window of {@link Server#PERMANENT_DISCONNECTION_TIME} seconds in which a new connection which registers itself with
     * the same id and nickname is recognized a the same client getting reconnected, and that prevents the deletion of that client.<br>
     * While the client is disconnected every {@link ServerLobby#AUTOPLAY_TIMER} seconds the lobby skips it turn to keep the other players not waiting for too long.
     *
     * @param clientID the Id of the client, remembered by {@link SocketHandler}, losing the connection
     */
    public synchronized void handleDisconnect(int clientID) {
        server.handleDisconnect(clientID);

        System.out.println("Disconnected player: " + clientID + ", from lobby: " + lobbyID);
        if(game != null && game.currentlyPlayingPlayer() == clientID) {
            Thread thread = new Thread(() -> autoPlay(clientID));
            thread.start();
            autoplay_threads.put(clientID, thread);
        }
    }

    //Plays instead of the given player, skipping their turn if they do not reconnect in time
    /**
     * Plays instead of the given player, skipping their turn.<br>
     * This method gets executed in a separate thread after {@link ServerLobby#AUTOPLAY_TIMER} that the player lost its connection,
     * its execution is however canceled if the player reconnects in time.
     * @see Game#skipTurn()
     * @see Game#revertToPreviousState(Game)
     *
     * @param clientID
     */
    private void autoPlay(int clientID) {
        try {
            Thread.sleep(AUTOPLAY_TIMER);
        } catch(InterruptedException e) {
            e.printStackTrace();
        }

        System.out.println("Skipping turn for: " + clientID);
        synchronized (this) {
            if(game != null) {
                if (game.currentlyPlayingPlayer() == clientID) {
                    //do not revert to previous state if not needed
                    boolean revert_needed = game.getPhase() == 1 && (game.getStep() != 0 || game.getMovedStudents() != 0 || game.getNpcEffect() != 0);
                    if(revert_needed)
                        game = Game.revertToPreviousState(game);

                    try {
                        game.skipTurn();

                        for (ClientData client : clients) {
                            if(client.socket_handler != null) {
                                if(revert_needed)
                                    client.socket_handler.send(MessageForClient.revert());
                                client.socket_handler.send(MessageForClient.skipTurn());
                            }
                        }

                        checkWinner();
                    } catch (InvalidMoveException e) {
                        e.printStackTrace();
                    }
                }
            }
            autoplay_threads.remove(clientID);
        }
    }

    //Allows players to go back to lobby selection, if the last player leaves, the lobby is dismantled and any eventual game is ended forcefully
    /**
     * Allows players to leave this lobby and go back to lobby selection, if the last player leaves, the lobby is dismantled and any eventual game is forcefully ended.<br>
     * Leaving a lobby means that the client's {@link SocketHandler} has its observer set to the associated {@link Server} instance.
     * Consequence of {@link MessageForServerLobby#leaveLobby}
     *
     * @param client_handler
     */
    public synchronized void leaveLobby(SocketHandler client_handler) {
        //reset readiness states every time someone quits, but if a game is going, set it false only for who quit
        if(game == null) {
            //if there is no game, reset the readiness and remove the player
            Arrays.fill(ready, false);

            for(int i = 0; i < clients.size(); i++) {
                if(clients.get(i).clientID == client_handler.getClientID()) {
                    clients.get(i).setLobbyID(0);
                    clients.get(i).socket_handler.changeUpdatesTarget(server);
                    clients.remove(i);
                    break;
                }
            }

            for(ClientData clientData : clients) {
                if(clientData.socket_handler != null) {
                    clientData.socket_handler.send(MessageForClient.setLobby(this.getLobbyData()));
                    clientData.socket_handler.send(MessageForClient.readiness(this.ready));
                }
            }

            //if there are no players left, delete the lobby
            if(clients.size() == 0 || clients.stream().allMatch(client -> client.socket_handler == null)) {
                //unfortunately under heavy load this may take a while...
                server.deleteLobby(lobbyID);
            }
        } else {
            //if there is a game, set the leaving player's readiness to false and his socket to null
            for(int i = 0; i < clients.size(); i++) {
                if(clients.get(i).clientID == client_handler.getClientID()) {
                    ready[i] = false;
                    break;
                }
            }

            for(int i = 0; i < clients.size(); i++) {
                if(clients.get(i).clientID == client_handler.getClientID()) {
                    clients.get(i).setLobbyID(0);
                    clients.get(i).socket_handler.changeUpdatesTarget(server);
                    clients.set(i, clients.get(i).clone());
                    clients.get(i).removeSocketHandler();
                    break;
                }
            }

            int ready_count = 0;
            int ready_index = 0;
            for(int i = 0; i < clients.size(); i++) {
                if(ready[i]) {
                    ready_count++;
                    ready_index = i;
                    clients.get(i).socket_handler.send(MessageForClient.readiness(ready));
                }
            }

            //if there is only a ready player, he wins and the lobby gets reset for a new game
            if(ready_count == 1) {
                for(ClientData client : clients)
                    if(client.socket_handler != null)
                        client.socket_handler.send(MessageForClient.gameEnded(clients.get(ready_index).clientID));
                resetLobby();
            } else if(game.currentlyPlayingPlayer() == client_handler.getClientID()) {
                //if the player who disconnected was the one who had to play, skip his turn
                Thread thread = new Thread(() -> autoPlay(client_handler.getClientID()));
                thread.start();
                autoplay_threads.put(client_handler.getClientID(), thread);
            }
        }

        client_handler.send(MessageForClient.setLobby(null));
    }

    //Allows each player to change its readiness status
    /**
     * Allows each player to change its readiness status.<br>
     * Only after every player is ready the game automatically starts.
     * Consequence of {@link MessageForServerLobby#toggleReady}
     *
     * @param client_handler {@link SocketHandler} which caused the update resulting in this method's invocation
     */
    public synchronized void toggleReady(SocketHandler client_handler) {
        if(game != null) {
            client_handler.send(MessageForClient.error("You cannot change your readiness during a game", 5));
            return;
        }

        int clientIndex;
        for(clientIndex = 0; clientIndex < clients.size(); clientIndex++)
            if(clients.get(clientIndex).clientID == client_handler.getClientID())
                break;

        ready[clientIndex] = !ready[clientIndex];

        for(ClientData client_data : clients)
            if(client_data != null)
                client_data.socket_handler.send(MessageForClient.readiness(ready));

        for(int i = 0; i < ready.length; i++)
            if(!ready[i]) return;

        this.game = buildGame(expert_mode, clients.stream().map(client -> client.clientID).collect(Collectors.toList()));
        for(ClientData client_data : clients)
            if(client_data.socket_handler != null)
                client_data.socket_handler.send(MessageForClient.gameStarted(this.game));
    }

    /**
     * Auxiliary method that constructs the copy of game for this lobby.
     *
     * @param expert_mode flag for expert mode in the new game
     * @param clientIDs participants of this lobby and the new game
     * @return the newly constructed copy of game for this lobby
     */
    public synchronized Game buildGame(boolean expert_mode, List<Integer> clientIDs) {
        return new Game(expert_mode, clientIDs);
    }

    /**
     * Allows players to play a card, checking the validity of their move and eventually applying it to the lobby's instance
     * of game, forwarding the move to all the other players.
     * Consequence of {@link MessageForServerLobby#cardPlayed}
     * @see Game#playCard(int, int)
     *
     * @param client_handler {@link SocketHandler} which caused the update resulting in this method's invocation
     * @param card_index index of the played card
     */
    public synchronized void cardPlayed(SocketHandler client_handler, int card_index) {
        if(game != null) {
            try {
                game.playCard(client_handler.getClientID(), card_index);
            } catch(InvalidMoveException e) {
                client_handler.send(MessageForClient.error(e.getMessage(), 7));
                revert();
                return;
            }
            for(ClientData client : clients)
                if(client.clientID != client_handler.getClientID() && client.socket_handler != null)
                    client.socket_handler.send(MessageForClient.cardPlayed(client_handler.getClientID(), card_index));

            client_handler.send(MessageForClient.moveSuccessful());
            checkWinner();

            int index;
            for(index = 0; index < clients.size(); index++)
                if(clients.get(index).clientID == game.currentlyPlayingPlayer())
                    break;

            //handles the case of the next player being temporarily disconnected
            if(clients.get(index).socket_handler != null && clients.get(index).socket_handler.isClosed()) {
                int finalIndex = index;
                Thread thread = new Thread(() -> autoPlay(clients.get(finalIndex).clientID));
                thread.start();
                autoplay_threads.put(clients.get(index).clientID, thread);
            }

            //handles the case of the next player being completely disconnected
            if(!ready[index]) {
                try {
                    game.skipTurn();
                } catch (InvalidMoveException e) {
                    e.printStackTrace();
                    return;
                }

                for(ClientData client : clients)
                    if(client.socket_handler != null)
                        client.socket_handler.send(MessageForClient.skipTurn());

                checkWinner();
            }
        } else {
            client_handler.send(MessageForClient.error("There is no game currently going on", 5));
        }
    }

    /**
     * Allows players to set a student to his hall, checking the validity of their move and eventually applying it to the lobby's instance
     * of game, forwarding the move to all the other players.
     * Consequence of {@link MessageForServerLobby#studentSetToHall}
     * @see Game#setStudentToHall(int, int)
     *
     * @param client_handler {@link SocketHandler} which caused the update resulting in this method's invocation
     * @param student_index index of the student being moved
     */
    public synchronized void studentSetToHall(SocketHandler client_handler, int student_index) {
        if(game != null) {
            try {
                game.setStudentToHall(client_handler.getClientID(), student_index);
            } catch(InvalidMoveException e) {
                client_handler.send(MessageForClient.error(e.getMessage(), 7));
                revert();
                return;
            }
            for(ClientData client : clients)
                if(client.clientID != client_handler.getClientID() && client.socket_handler != null)
                    client.socket_handler.send(MessageForClient.studentSetToHall(client_handler.getClientID(), student_index));

            client_handler.send(MessageForClient.moveSuccessful());
            checkWinner();
        } else {
            client_handler.send(MessageForClient.error("There is no game currently going on", 5));
        }
    }

    /**
     * Allows players to set a student to and island, checking the validity of their move and eventually applying it to the lobby's instance
     * of game, forwarding the move to all the other players.
     * Consequence of {@link MessageForServerLobby#studentSetToIsland}
     * @see Game#setStudentToIsland(int, int, int)
     *
     * @param client_handler {@link SocketHandler} which caused the update resulting in this method's invocation
     * @param student_index index of the student being moved
     * @param island destination island
     */
    public synchronized void studentSetToIsland(SocketHandler client_handler, int student_index, int island) {
        if(game != null) {
            try {
                game.setStudentToIsland(client_handler.getClientID(), student_index, island);
            } catch(InvalidMoveException e) {
                client_handler.send(MessageForClient.error(e.getMessage(), 7));
                revert();
                return;
            }
            for(ClientData client : clients)
                if(client.clientID != client_handler.getClientID() && client.socket_handler != null)
                    client.socket_handler.send(MessageForClient.studentSetToIsland(client_handler.getClientID(), student_index, island));

            client_handler.send(MessageForClient.moveSuccessful());
            checkWinner();
        } else {
            client_handler.send(MessageForClient.error("There is no game currently going on", 5));
        }
    }

    /**
     * Allows players to move mother nature, checking the validity of their move and eventually applying it to the lobby's instance
     * of game, forwarding the move to all the other players.
     * Consequence of {@link MessageForServerLobby#motherNatureMoved}
     * @see Game#moveMotherNature(int, int)
     *
     * @param client_handler {@link SocketHandler} which caused the update resulting in this method's invocation
     * @param moves steps to move mother nature by
     */
    public synchronized void motherNatureMoved(SocketHandler client_handler, int moves) {
        if(game != null) {
            try {
                game.moveMotherNature(client_handler.getClientID(), moves);
            } catch(InvalidMoveException e) {
                client_handler.send(MessageForClient.error(e.getMessage(), 7));
                revert();
                return;
            }
            for(ClientData client : clients)
                if(client.clientID != client_handler.getClientID() && client.socket_handler != null)
                    client.socket_handler.send(MessageForClient.motherNatureMoved(client_handler.getClientID(), moves));

            client_handler.send(MessageForClient.moveSuccessful());
            checkWinner();
        } else {
            client_handler.send(MessageForClient.error("There is no game currently going on", 5));
        }
    }

    /**
     * Allows players to choose a cloud, checking the validity of their move and eventually applying it to the lobby's instance
     * of game, forwarding the move to all the other players.
     * Consequence of {@link MessageForServerLobby#cloudChosen}
     * @see Game#chooseCloud(int, int)
     *
     * @param client_handler {@link SocketHandler} which caused the update resulting in this method's invocation
     * @param cloud_index index of the cloud being chosen
     */
    public synchronized void cloudChosen(SocketHandler client_handler, int cloud_index) {
        if(game != null) {
            try {
                game.chooseCloud(client_handler.getClientID(), cloud_index);
            } catch(InvalidMoveException e) {
                client_handler.send(MessageForClient.error(e.getMessage(), 7));
                revert();
                return;
            }
            for(ClientData client : clients)
                if(client.clientID != client_handler.getClientID() && client.socket_handler != null)
                    client.socket_handler.send(MessageForClient.cloudChosen(client_handler.getClientID(), cloud_index));

            client_handler.send(MessageForClient.moveSuccessful());
            client_handler.send(MessageForClient.cloudsUpdate(game.getClouds()));
            checkWinner();

            int index;
            for(index = 0; index < clients.size(); index++)
                if(clients.get(index).clientID == game.currentlyPlayingPlayer())
                    break;

            //handles the case of the next player being temporarily disconnected
            if(clients.get(index).socket_handler != null && clients.get(index).socket_handler.isClosed()) {
                int finalIndex = index;
                Thread thread = new Thread(() -> autoPlay(clients.get(finalIndex).clientID));
                thread.start();
                autoplay_threads.put(clients.get(index).clientID, thread);
            }

            //handles the case of the next player being completely disconnected
            if(!ready[index]) {
                try {
                    game.skipTurn();
                } catch (InvalidMoveException e) {
                    e.printStackTrace();
                    return;
                }

                for(ClientData client : clients)
                    if(client.socket_handler != null)
                        client.socket_handler.send(MessageForClient.skipTurn());

                checkWinner();
            }
        } else {
            client_handler.send(MessageForClient.error("There is no game currently going on", 5));
        }
    }

    /**
     * Allows players to activate an Npc's effect, checking the validity of their activation and eventually applying it to the lobby's instance
     * of game, forwarding the move to all the other players.
     * Consequence of {@link MessageForServerLobby#npcActivated}
     * @see Game#activateEffect(int, int, EffectParameters)
     *
     * @param client_handler {@link SocketHandler} which caused the update resulting in this method's invocation
     * @param npc_index index of the activated Npc
     * @param effect_parameters parameters for the effect's activation
     */
    public synchronized void npcActivated(SocketHandler client_handler, int npc_index, EffectParameters effect_parameters) {
        if(game != null) {
            try {
                game.activateEffect(client_handler.getClientID(), npc_index, effect_parameters);
            } catch(InvalidMoveException e) {
                client_handler.send(MessageForClient.error(e.getMessage(), 7));
                revert();
                return;
            }
            for(ClientData client : clients)
                if(client.clientID != client_handler.getClientID() && client.socket_handler != null)
                    client.socket_handler.send(MessageForClient.npcActivated(client_handler.getClientID(), npc_index, effect_parameters));

            //update NPCs with students on them
            if(game.getNpcs()[npc_index].getId() == 1 ||
                game.getNpcs()[npc_index].getId() == 7 ||
                game.getNpcs()[npc_index].getId() == 11)
                for(ClientData client : clients)
                    if(client.socket_handler != null)
                        client.socket_handler.send(MessageForClient.npcUpdated(npc_index, game.getNpcs()[npc_index].getExtraProperty().stream().map(x -> Colors.fromColorIndex(x)).collect(Collectors.toList())));

            client_handler.send(MessageForClient.moveSuccessful());
            checkWinner();
        } else {
            client_handler.send(MessageForClient.error("There is no game currently going on", 5));
        }
    }

    /**
     * Allows the client to request a fresh copy of the lobby's local mode, which is sent to him.
     * Consequence of {@link MessageForServerLobby#resync}
     *
     * @param client_handler {@link SocketHandler} which caused the update resulting in this method's invocation
     */
    public synchronized void resync(SocketHandler client_handler) {
        if(game != null) {
            client_handler.send(MessageForClient.gameStarted(this.game));
        } else {
            client_handler.send(MessageForClient.error("There is no game currently going on", 5));
        }
    }

    //checks the model for a potential winner, and in case notifies the players and resets the lobby
    /**
     * Checks the model for a potential winner, and in case there is one notifies the players and resets the lobby
     */
    public synchronized void checkWinner() {
        if(game.getWinnerID() != 0) {
            for(ClientData client : clients)
                if(client.socket_handler != null)
                   client.socket_handler.send(MessageForClient.gameEnded(game.getWinnerID()));
            //keeping the lobby intact, so that another match can be played right away!
            resetLobby();
        }
    }

    //reverts the Model to its last known valid state, undoing unwanted changes
    /**
     * Reverts the Model to its last known valid state, undoing unwanted changes
     * @see Game#revertToPreviousState(Game)
     */
    private synchronized void revert() {
        game = Game.revertToPreviousState(game);
        for (ClientData client : clients) {
            if (client.socket_handler != null) {
                client.socket_handler.send(MessageForClient.revert());
            }
        }
    }

    //prepares the lobby for a new game with the same players, removing any who left and resetting the readiness
    /**
     * Disposes of the current game and prepares the lobby for a new game with the same players, removing any those who left and resetting the readiness
     */
    public void resetLobby() {
        this.game = null;
        Arrays.fill(ready, false);

        boolean stop;
        do {
            stop = true;
            for (int i = 0; i < clients.size(); i++) {
                if (clients.get(i).socket_handler == null) {
                    clients.remove(i);
                    stop = false;
                    break;
                }
            }
        } while(!stop);

        for(ClientData clientData : clients) {
            if(clientData.socket_handler != null) {
                clientData.socket_handler.send(MessageForClient.setLobby(this.getLobbyData()));
                clientData.socket_handler.send(MessageForClient.readiness(this.ready));
            }
        }

        for(Thread autoplay : autoplay_threads.values())
            autoplay.stop();
        autoplay_threads = new HashMap<Integer, Thread>();
    }

    /**
     * Provides this lobby's data.
     *
     * @return {@link LobbyData} instance relative to this lobby
     */
    public LobbyData getLobbyData() {
        return new LobbyData(lobbyID, size, expert_mode, clients);
    }

    /**
     * Returns this lobby's players and settings in a string format.
     *
     * @return string regarding this lobby
     */
    public String toString() {
        StringBuilder result = new StringBuilder("ID: " + lobbyID + "Size: " + size + " - ");
        for(ClientData client : clients)
            result.append(client.nickname).append(", ");
        return result.substring(0, result.length() - 3);
    }
}
