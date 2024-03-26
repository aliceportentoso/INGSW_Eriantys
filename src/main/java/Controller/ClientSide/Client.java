package Controller.ClientSide;

import Controller.*;
import Controller.ServerSide.ClientData;
import Controller.ServerSide.LobbyData;
import Exceptions.InvalidMoveException;
import Model.Colors;
import Model.EffectParameters;
import Model.Game;
import Model.GameState;
import View.CLI.CLI;
import View.GUI.GUI;
import View.UI;
import View.UIColors;
import org.jetbrains.annotations.TestOnly;

import java.io.*;
import java.net.InetAddress;
import java.net.Socket;
import java.net.URI;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.Scanner;

/*
This class contains the connection to the server, in an instance of SocketHandler, and the Game instance for the client,
this class's main is the starting point of the program and starts both a connection to the server and the UI,
subsequently its thread will pass execution to the former.
This class is the gate for the UI to get data from the model, Game, and to send data to send data to the server.
 */

/**
 * Main class for the client side.<br>
 * This class contains the connection to the {@link Server}, in an instance of {@link SocketHandler}, and the {@link Game} instance for the client side,
 * this class's main is the starting point of the program and starts both a connection to the {@link Server} and the {@link UI},
 * in this order, subsequently passing the execution to the second.
 * Through this class the UI can get data from the model, {@link Game}, and send data to send data to the {@link Server}.
 * <br><br>
 * In here are memorized the clientId and nickname unique to the current session, with the associated functionality
 * to reconnect to the server to recover the previous session, both with and without a client restart in between.
 * This class retains a few contents provided by the server for easy access without new requests, those being:
 * <ul>
 *     <li> The current lobby the client is in
 *     <li> A list of the available lobbies as for the time of the last request
 *     <li> A copy of the entire instance of the model for the current game
 * </ul>
 * This class has a few different states that can be reached during execution:
 * <ul>
 *    <li> <i>reconnecting</i> - after the connection to the server has been lost and reconnections attempt are being made every second
 *    <li> <i>loading_storage</i> - while an attempt to recover a session from a previous execution of the program started by this client is on going
 *    <li> <i>waiting_for_move_successful</i> - reached after a request for a move being performed has been sent to the server, this state is maintained
 *          until the server acknowledges the move
 * </ul>
 * <br><br>
 * Since it implements {@link Controller}, it is an <strong>OBSERVER</strong> to its {@link SocketHandler} connected to the server.
 */
public class Client extends Controller {
    public final static int RECONNECTION_TRY_INTERVAL = 1000;
    public final static int PING_INTERVAL = 5000;
    public final static String LOCAL_STORAGE_NAME = "client";
    public final static String LOCAL_STORAGE_EXTENSION = "dat";
    public final static int STORAGE_DURATION = 3600 * 24; //in seconds
    public static boolean ENABLE_STORAGE = true;

    private Integer myClientID;
    private LobbyData lobby;
    private String nickname;
    private final String server_ip;
    private final int server_port;
    private SocketHandler socket_handler;
    private Thread socket_receiver_thread, socket_sender_thread, ping_thread;
    private List<LobbyData> lobbiesList;
    private boolean[] ready_flags;
    //true when the client lost connection and is trying to reconnect
    private boolean reconnecting;
    //true when the client has read the previous id and nickname from local store and is trying to log back in with them
    private boolean loading_storage;
    //true when a move has been sent to the server, and the client is waiting for the moveSuccessful message
    private boolean waiting_for_move_successful;
    private int waiting_for_move_successful_attempts;
    //contains the arguments of the last performed move, used by the GUI to show animations.
    private final List<Integer> last_move_data;

    private final UI ui;

    private Game game;

    /**
     * Constructs a new client, setting its connection parameters and providing it with its UI.<br>
     * A new client is always constructed with every retained content <strong>null</strong> and in a blank state.
     * <br><br>
     * <strong>Note:</strong> a client is not started after its construction, to start a client call {@link Client#start()}, which is a blocking call.
     *
     * @param server_ip   the IP for the {@link Server}, in a format like "X.X.X.X"
     * @param server_port the port of the {@link Server} to connect to
     * @param ui          the instance of {@link UI} to use
     */
    public Client(String server_ip, int server_port, UI ui) {
        this.server_ip = server_ip;
        this.server_port = server_port;
        this.nickname = null;
        this.ui = ui;
        this.lobbiesList = null;
        this.ready_flags = null;
        this.reconnecting = false;
        this.loading_storage = false;
        this.waiting_for_move_successful = false;
        this.waiting_for_move_successful_attempts = 0;
        this.last_move_data = new ArrayList<Integer>();
    }

    /**
     * Starts the {@link Client}, establishing a connection to the {@link Server}, eventually reloading a previous session,
     * and as a last thing passing execution to the {@link UI}.
     * <br><br>
     * A call to this method is <strong>blocking</strong>, this method is not intended to return unless the connection to the server
     * failed to be established or the user chose to terminate the UI and the program consequently.<br>
     * Hence, this method returning must be quickly followed by the whole program terminating.
     */
    public void start() {
        try {
            Socket socket = new Socket(server_ip, server_port);
            this.socket_handler = new SocketHandler(socket, this);
            this.socket_receiver_thread = new Thread(this.socket_handler.getReceiver());
            this.socket_receiver_thread.start();
            this.socket_sender_thread = new Thread(this.socket_handler.getSender());
            this.socket_sender_thread.start();
            this.ping_thread = new Thread(this::pingServer);
            this.ping_thread.start();

            //load previous credentials from local storage
            if (ENABLE_STORAGE && new File(LOCAL_STORAGE_NAME + "." + LOCAL_STORAGE_EXTENSION).isFile()) {
                try {
                    ObjectInputStream ois = new ObjectInputStream(new FileInputStream(LOCAL_STORAGE_NAME + "." + LOCAL_STORAGE_EXTENSION));
                    FileStorage file_storage = (FileStorage) ois.readObject();

                    if (file_storage.isValid()) {
                        this.loading_storage = true;
                        this.myClientID = file_storage.getId();
                        this.setNickname(file_storage.getNickname());
                        //ui quick start
                        //is undone by ui.resetState() in the case of an error
                        ui.nicknameConfirmed();
                        //getLobbies();
                    } else
                        this.myClientID = 0;

                    ois.close();
                } catch (Exception e) {
                    this.myClientID = 0;
                }
            } else {
                this.myClientID = 0;
            }

        } catch (java.net.ConnectException e) {
            System.out.println("The server failed to respond, verify the given IP and try again.");
            return;
        } catch (IOException e) {
            e.printStackTrace();
            return;
        }

        ui.start(this);
    }

    /**
     * Sets the necessary parameters for this client to be used.
     * @param socket_handler this client's {@link SocketHandler}
     * @implNote This method is meant to be used for testing purposes only.
     */
    @TestOnly
    public void start(SocketHandler socket_handler) {
        this.socket_handler = socket_handler;
        this.socket_receiver_thread = null;
        this.socket_sender_thread = null;
        this.myClientID = 0;

        ENABLE_STORAGE = false;

        //the ui is intentionally not started here
    }

    //Called by socketHandler after it looses the connection

    /**
     * Method invoked by the {@link SocketHandler} holding the client's connection whenever the connection drops.
     *
     * @param clientID id of this client
     */
    public void handleDisconnect(int clientID) {
        synchronized (this) {
            ui.showMessage("Disconnected!", UIColors.RED);
            reconnecting = true;
        }

        while (socket_handler.isClosed()) {
            try {
                Thread.sleep(RECONNECTION_TRY_INTERVAL);
            } catch (InterruptedException e) {
                e.printStackTrace();
            }

            synchronized (this) {
                try {
                    Socket socket = new Socket(server_ip, server_port);
                    this.socket_handler = new SocketHandler(socket, this);
                    this.socket_receiver_thread = new Thread(this.socket_handler.getReceiver());
                    this.socket_receiver_thread.start();
                    this.socket_sender_thread = new Thread(this.socket_handler.getSender());
                    this.socket_sender_thread.start();
                    ui.resetState();
                    if (nickname != null) {
                        if (myClientID != 0)
                            this.socket_handler.send(MessageForServer.hello(myClientID, nickname));
                        else
                            this.socket_handler.send(MessageForServer.hello(0, nickname));
                    }
                    ui.showMessage("Reconnected!", UIColors.BLUE);
                } catch (IOException e) {
                    ui.showMessage("Reconnection failed...", UIColors.RED);
                }
            }
        }

        lobbiesList = null;
        //getLobbies();
    }

    /**
     * While connected to the server, sends it a ping every {@link Client#PING_INTERVAL}, informing it that the connection is still alive.<br>
     */
    private void pingServer() {
        while(true) {
            try {
                Thread.sleep(PING_INTERVAL);
            } catch (InterruptedException e) {
                e.printStackTrace();
            }

            if(!socket_handler.isClosed() && socket_handler != null)
                socket_handler.send(MessageForServer.ping());
        }
    }

    /**
     * {@inheritDoc}
     *
     * @param socket_handler the {@link SocketHandler} which received the {@link Message} and is dispatching the update
     * @param message the {@link Message} received
     */
    public synchronized void update(SocketHandler socket_handler, Message message) {
        try {
            //System.out.println("Update handled.");
            //System.out.println(message.getClass());

            message.execute(this, socket_handler);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }



    /*
        The following functions are meant to receive data from the server and update the local data accordingly
     */


    //Function that after a successful login receives the current clientID

    /**
     * Informs the client that its connection has been authenticated with the server and its nickname approved.<br>
     * Consequence of {@link MessageForClient#ack}.
     *
     * @param handler  {@link SocketHandler} which caused the update resulting in this method's invocation
     * @param clientID new id of this client
     */
    public synchronized void ack(SocketHandler handler, int clientID) {
        if (clientID != this.myClientID && this.myClientID != 0) {
            this.lobby = null;
            ui.resetState();
            ui.showMessage("Reconnection with previous session failed, new session created", UIColors.YELLOW);
        }
        this.myClientID = clientID;
        reconnecting = false;
        loading_storage = false;
        ui.nicknameConfirmed();
        ui.refresh();

        //save credentials to file
        try {
            if (ENABLE_STORAGE && new File(LOCAL_STORAGE_NAME + "." + LOCAL_STORAGE_EXTENSION).isFile()) {
                ObjectOutputStream oos = new ObjectOutputStream(new FileOutputStream(LOCAL_STORAGE_NAME + "." + LOCAL_STORAGE_EXTENSION));
                FileStorage file_storage = new FileStorage(clientID, nickname);
                oos.writeObject(file_storage);

                oos.close();
            }
        } catch (IOException e) {
            ui.showMessage("Unable to update local credential storage...", UIColors.YELLOW);
        }
        //System.out.println("Logged with clientID: " + clientID);
    }

    //Function that receives the current lobby's data

    /**
     * Updates the information the client has on the lobby it's in on the server.<br>
     * Consequence of {@link MessageForClient#setLobby}.
     *
     * @param handler {@link SocketHandler} which caused the update resulting in this method's invocation
     * @param lobby   {@link LobbyData} containing the updated current lobby's info
     */
    public synchronized void setLobby(SocketHandler handler, LobbyData lobby) {
        if (lobby != null) {
            this.lobby = lobby;
            ui.inLobby();

            /*String output = "Current lobby:\nId: " + lobby.lobbyID + " Expert mode: " + lobby.expert_mode + " Size: " + lobby.size;
            output += "\nParticipants:";
            for(ClientData client : lobby.clients)
                output += "\n" + client.nickname;
            ui.showMessage(output);*/
        } else {
            this.lobby = null;
            ui.noLobby();
            //updates the old lobbies list after you leave your lobby
            getLobbies();
            //ui.showMessage("Lobby left");
        }

        ui.refresh();
    }

    //Function which receives the list of available lobbies from the server

    /**
     * Updates the known list of lobbies available on the server.<br>
     * Consequence of {@link MessageForClient#lobbiesList}.
     *
     * @param handler {@link SocketHandler} which caused the update resulting in this method's invocation
     * @param lobbies list of {@link LobbyData}, one for each lobby available on the server
     */
    public synchronized void lobbiesList(SocketHandler handler, List<LobbyData> lobbies) {
        lobbiesList = lobbies;

        /*String output = "Lobbies list:";
        for (LobbyData lobbyData : lobbiesList) {
            output += "\n\nId: " + lobbyData.lobbyID + " Expert mode: " + lobbyData.expert_mode + " Size: " + lobbyData.size;
            output += "\nParticipants:";
            for(ClientData client : lobbyData.clients)
                output += "\n" + client.nickname;
        }*/

        ui.refresh();
        //ui.showMessage(output);
    }

    //Function which gives the current readiness status of players inside a lobby, used in 2 occasions,
    //before the lobby's game starts, to see who is ready and who is not, and during a game, to mark permanently disconnected player

    /**
     * Updates the known readiness of the players in the current lobby.<br>
     * Consequence of {@link MessageForClient#readiness}.
     *
     * @param handler     {@link SocketHandler} which caused the update resulting in this method's invocation
     * @param ready_flags updated array of ready flags
     */
    public synchronized void readiness(SocketHandler handler, boolean[] ready_flags) {
        /*String output = "Players readiness status:";
        for(int i = 0; i < lobby.clients.size(); i++)
            output += "\n" + lobby.clients.get(i).nickname + ": " + (ready_flags[i] ? "true" : "false");
        */
        this.ready_flags = ready_flags;

        ui.refresh();
        //ui.showMessage(output);
    }

    /**
     * Updates the local copy of the model according to a card played by another player.<br>
     * Consequence of {@link MessageForClient#cardPlayed}.
     * @see Game#playCard(int, int)
     *
     * @param handler    {@link SocketHandler} which caused the update resulting in this method's invocation
     * @param clientID   id of the opponent performing the move
     * @param card_index index of the played card
     */
    public synchronized void cardPlayed(SocketHandler handler, int clientID, int card_index) {
        try {
            game.playCard(clientID, card_index);
        } catch (InvalidMoveException e) {
            ui.showMessage("error: the move could not be applied", UIColors.RED);
        }
        ui.showMessage("card chosen by " + clientIDToNickname(clientID), UIColors.WHITE);

        last_move_data.clear();
        last_move_data.add(card_index);
    }

    /**
     * Updates the local copy of the model according to a student moved to another player to his hall.<br>
     * Consequence of {@link MessageForClient#studentSetToHall}.
     * @see Game#setStudentToHall(int, int)
     *
     * @param handler       {@link SocketHandler} which caused the update resulting in this method's invocation
     * @param clientID      id of the opponent performing the move
     * @param student_index index of the moved student
     */
    public synchronized void studentSetToHall(SocketHandler handler, int clientID, int student_index) {
        try {
            game.setStudentToHall(clientID, student_index);
        } catch (InvalidMoveException e) {
            ui.showMessage("error: the move could not be applied", UIColors.RED);
        }
        ui.showMessage("student set to hall by " + clientIDToNickname(clientID), UIColors.WHITE);

        last_move_data.clear();
        last_move_data.add(student_index);
    }

    /**
     * Updates the local copy of the model according to a student moved to another player to an island.<br>
     * Consequence of {@link MessageForClient#studentSetToIsland}.
     * @see Game#setStudentToIsland(int, int, int)
     *
     * @param handler       {@link SocketHandler} which caused the update resulting in this method's invocation
     * @param clientID      id of the opponent performing the move
     * @param student_index index of the moved student
     * @param island_index  index of the target island
     */
    public synchronized void studentSetToIsland(SocketHandler handler, int clientID, int student_index, int island_index) {
        try {
            game.setStudentToIsland(clientID, student_index, island_index);
        } catch (InvalidMoveException e) {
            ui.showMessage("error: the move could not be applied", UIColors.RED);
        }
        ui.showMessage("student set by " + clientIDToNickname(clientID) + " to island " + island_index, UIColors.WHITE);

        last_move_data.clear();
        last_move_data.add(student_index);
        last_move_data.add(island_index);
    }

    /**
     * Updates the local copy of the model according to mother nature being moved by another player.<br>
     * Consequence of {@link MessageForClient#motherNatureMoved}.
     * @see Game#moveMotherNature(int, int)
     *
     * @param handler  {@link SocketHandler} which caused the update resulting in this method's invocation
     * @param clientID id of the opponent performing the move
     * @param moved    steps mother nature was moved by
     */
    public synchronized void motherNatureMoved(SocketHandler handler, int clientID, int moved) {
        try {
            game.moveMotherNature(clientID, moved);
        } catch (InvalidMoveException e) {
            ui.showMessage("error: the move could not be applied", UIColors.RED);
        }
        ui.showMessage("mother nature moved by " + moved + " steps by " + clientIDToNickname(clientID), UIColors.WHITE);

        last_move_data.clear();
        last_move_data.add(moved);
    }

    /**
     * Updates the local copy of the model according to a cloud being chosen by another player.<br>
     * Consequence of {@link MessageForClient#cloudChosen}.
     * @see Game#chooseCloud(int, int)
     *
     * @param handler     {@link SocketHandler} which caused the update resulting in this method's invocation
     * @param clientID    id of the opponent performing the move
     * @param cloud_index index of the cloud chosen
     */
    public synchronized void cloudChosen(SocketHandler handler, int clientID, int cloud_index) {
        try {
            game.chooseCloud(clientID, cloud_index);
        } catch (InvalidMoveException e) {
            ui.showMessage("error: the move could not be applied", UIColors.RED);
        }
        ui.showMessage("cloud " + cloud_index + " chosen by " + clientIDToNickname(clientID), UIColors.WHITE);

        last_move_data.clear();
        last_move_data.add(cloud_index);
    }

    /**
     * Updates the local copy of the model according an Npc being being activated by another player.<br>
     * Consequence of {@link MessageForClient#npcActivated}.
     * @see Game#activateEffect(int, int, EffectParameters)
     *
     * @param handler           {@link SocketHandler} which caused the update resulting in this method's invocation
     * @param clientID          id of the opponent performing the move
     * @param npc_index         index of the activated Npc
     * @param effect_parameters parameters for the Npc's activation
     */
    public synchronized void npcActivated(SocketHandler handler, int clientID, int npc_index, EffectParameters effect_parameters) {
        try {
            game.activateEffect(clientID, npc_index, effect_parameters);
        } catch (InvalidMoveException e) {
            ui.showMessage("error: the move could not be applied", UIColors.RED);
        }
        ui.showMessage("npc " + npc_index + " activated by " + clientIDToNickname(clientID), UIColors.WHITE);

        last_move_data.clear();
        last_move_data.add(npc_index);
    }

    /**
     * Skips the current turn of the model according to the {@link Controller.ServerSide.ServerLobby}'s handling of another player's disconnection.<br>
     * Consequence of {@link MessageForClient#skipTurn}.
     * @see Game#skipTurn()
     *
     * @param handler {@link SocketHandler} which caused the update resulting in this method's invocation
     */
    public synchronized void skipTurn(SocketHandler handler) {
        try {
            game.skipTurn();
            ui.refresh();
        } catch (InvalidMoveException e) {
            ui.showMessage("error: the move could not be applied", UIColors.RED);
        }
        ui.showMessage("Player turn skipped due to disconnection...", UIColors.WHITE);
    }

    /**
     * Acknowledgment that the previous move requested the server was approved and applied lobby-wide.<br>
     * Consequence of {@link MessageForClient#moveSuccessful}.
     *
     * @param handler {@link SocketHandler} which caused the update resulting in this method's invocation
     */
    public synchronized void moveSuccessful(SocketHandler handler) {
        waiting_for_move_successful = false;
        waiting_for_move_successful_attempts = 0;
        ui.refresh();
        ui.showMessage("move validated by the server", UIColors.CYAN);
    }

    /**
     * Updates the local model's clouds after a game turn.<br>
     * Consequence of {@link MessageForClient#cloudsUpdate}.
     *
     * @param handler {@link SocketHandler} which caused the update resulting in this method's invocation
     * @param clouds updated clouds
     */
    public synchronized void cloudsUpdated(SocketHandler handler, List<Colors>[] clouds) {
        this.game.updateClouds(clouds);
        ui.showMessage("clouds updated", UIColors.GRAY);
    }

    /**
     * Updates the students on of the local model's Npcs after its activation.<br>
     * Consequence of {@link MessageForClient#npcUpdated}.
     *
     * @param handler {@link SocketHandler} which caused the update resulting in this method's invocation
     * @param npc_index index of the updated Npc
     * @param students students for the updated Npc
     */
    public synchronized void npcUpdated(SocketHandler handler, int npc_index, List<Colors> students) {
        this.game.getNpcs()[npc_index].setExtraProperty(students);
        ui.showMessage("npc number " + npc_index + " updated", UIColors.GRAY);
    }

    /**
     * Reverts the local model to its last known good state.<br>
     * Consequence of {@link MessageForClient#revert}.
     * @see Game#revertToPreviousState(Game)
     *
     * @param handler {@link SocketHandler} which caused the update resulting in this method's invocation
     */
    public synchronized void revert(SocketHandler handler) {
        waiting_for_move_successful = false;
        waiting_for_move_successful_attempts = 0;
        game = Game.revertToPreviousState(game);
    }

    /**
     * Notifies the client that it is now in a match, providing the associated model instance.<br>
     * Consequence of {@link MessageForClient#gameStarted}.
     *
     * @param handler {@link SocketHandler} which caused the update resulting in this method's invocation
     * @param game copy of the model for the new game
     */
    public synchronized void gameStart(SocketHandler handler, Game game) {
        this.ui.gameStart();
        this.game = game;
        waiting_for_move_successful = false;
        waiting_for_move_successful_attempts = 0;
        last_move_data.clear();
        //refresh only AFTER game has been set
        ui.refresh();
        ui.showMessage("--> game started", UIColors.GREEN);
    }

    /**
     * Notifies the client that its current game has ended, providing the winner's id.<br>
     * Consequence of {@link MessageForClient#gameEnded}.
     *
     * @param handler {@link SocketHandler} which caused the update resulting in this method's invocation
     * @param winnerId id of the client who won the match
     */
    public synchronized void gameEnd(SocketHandler handler, int winnerId) {
        this.ui.gameEnd(winnerId);
        waiting_for_move_successful = true;
        waiting_for_move_successful_attempts = 0;
        //this.game = null;
        ui.showMessage("--> game ended, winner: " + clientIDToNickname(winnerId), winnerId == myClientID ? UIColors.GREEN : UIColors.RED);
    }

    //Function which handles "error" as an incoming message, in the future will hand them to the UI for display
    /**
     * Presents the client with an error from the server.<br>
     * Consequence of {@link MessageForClient#error}.
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
     * @param handler {@link SocketHandler} which caused the update resulting in this method's invocation
     * @param msg error message
     * @param errorCode error code
     */
    public synchronized void error(SocketHandler handler, String msg, int errorCode) {
        //your nickname was taken while you were reconnecting, choose another one
        if (errorCode == 3) {
            if (reconnecting) {
                ui.showMessage("Your nickname was taken while you were reconnecting, please choose another one...", UIColors.YELLOW);
                reconnecting = false;
            } else if (loading_storage) {
                ui.showMessage("Reconnection with previous credentials failed,\n" +
                        "press \"enter\" to continue and please choose a new nickname...", UIColors.YELLOW);
                loading_storage = false;
            }
            myClientID = 0;
            nickname = null;
            ui.resetState();
        }
        ui.showMessage("Error: " + msg, errorCode != 1 && errorCode != 6 ? UIColors.RED : UIColors.YELLOW);
        //ui.showMessage("Error: " + msg + " \nCode: " + errorCode, errorCode != 1 && errorCode != 6 ? UIColors.RED : UIColors.YELLOW);
    }



    /*
        The following functions are meant for the client's UI to send its moves and requests to the server
     */


    /**
     * Allows the client to request the creation of a lobby on the server.<br>
     * If the request is successful a {@link MessageForClient#setLobby} is received.
     *
     * @param size size for the new lobby
     * @param expert_mode flag for expert mode in the new lobby
     */
    public void createLobby(int size, boolean expert_mode) {
        socket_handler.send(MessageForServer.createLobby(size, expert_mode));
    }

    /**
     * Makes the client request to the server to join the lobby with the specified id.<br>
     * If the request is successful a {@link MessageForClient#setLobby} is received.
     *
     * @param lobbyID id of the lobby to join, based on {@link Client#getLobbiesList}
     */
    public void joinLobby(int lobbyID) {
        socket_handler.send(MessageForServer.joinLobby(lobbyID));
    }

    /**
     * Requests the server un updated list of the available lobbies.<br>
     * If the request is successful a {@link MessageForClient#lobbiesList} is received.<br>
     * It is important to know that {@link Client#getLobbiesList}} won't immediately return the updated list.
     */
    public void getLobbies() {
        socket_handler.send(MessageForServer.getLobbies());
    }

    /**
     * Allows the client to leave its current lobby, if any.
     */
    public void leaveLobby() {
        if (game != null) {
            game = null;
            ui.resetState();
        }
        socket_handler.send(MessageForServerLobby.leaveLobby());
    }

    /**
     * Toggles the client's readiness state in its current lobby, if any.<br>
     * When every client is ready the game starts.
     */
    public void toggleReady() {
        socket_handler.send(MessageForServerLobby.toggleReady());
    }

    /**
     * Allows the client to play a card in the game that's currently going on.
     * @see Game#playCard(int, int)
     * 
     * @param card_index id of the card to play.
     */
    public void playCard(int card_index) {
        if (game != null) {
            if (waiting_for_move_successful) {
                ui.showMessage("Waiting for the server to validate your previous move...", UIColors.CYAN);
                waiting_for_move_successful_attempts += 1;
                if(waiting_for_move_successful_attempts >= 3)
                    resync();
                return;
            }

            try {
                game.playCard(myClientID, card_index);
            } catch (InvalidMoveException e) {
                ui.showMessage(e.getMessage(), UIColors.RED);
                return;
            }
            socket_handler.send(MessageForServerLobby.cardPlayed(card_index));
            waiting_for_move_successful = true;
            waiting_for_move_successful_attempts = 0;
        }
    }

    /**
     * Allows the client to set a student to his hall in the game that is currently going on.
     * @see Game#setStudentToHall(int, int)
     * 
     * @param student_index index of the student to move relative to {@link GameState#myPlayer}'s dashboard entrance
     */
    public void setStudentToHall(int student_index) {
        if (game != null) {
            if (waiting_for_move_successful) {
                ui.showMessage("Waiting for the server to validate your previous move...", UIColors.CYAN);
                waiting_for_move_successful_attempts += 1;
                if(waiting_for_move_successful_attempts >= 3)
                    resync();
                return;
            }

            try {
                game.setStudentToHall(myClientID, student_index);
            } catch (InvalidMoveException e) {
                ui.showMessage(e.getMessage(), UIColors.RED);
                return;
            }
            socket_handler.send(MessageForServerLobby.studentSetToHall(student_index));
            waiting_for_move_successful = true;
            waiting_for_move_successful_attempts = 0;
        }
    }

    /**
     * Allows the client to set a student to an island in the game that is currently going on.
     * @see Game#setStudentToIsland(int, int, int)
     *
     * @param student_index index of the student to move relative to {@link GameState#myPlayer}'s dashboard entrance
     * @param island_index index of the island where to put the student relative to {@link GameState#getIslands}
     */
    public void setStudentToIsland(int student_index, int island_index) {
        if (game != null) {
            if (waiting_for_move_successful) {
                ui.showMessage("Waiting for the server to validate your previous move...", UIColors.CYAN);
                waiting_for_move_successful_attempts += 1;
                if(waiting_for_move_successful_attempts >= 3)
                    resync();
                return;
            }

            try {
                game.setStudentToIsland(myClientID, student_index, island_index);
            } catch (InvalidMoveException e) {
                ui.showMessage(e.getMessage(), UIColors.RED);
                return;
            }
            socket_handler.send(MessageForServerLobby.studentSetToIsland(student_index, island_index));
            waiting_for_move_successful = true;
            waiting_for_move_successful_attempts = 0;
        }
    }

    /**
     * Allows the client to move mother nature in the game that is currently going on.
     * @see Game#moveMotherNature(int, int)
     *
     * @param moves steps to move mother nature by
     */
    public void moveMotherNature(int moves) {
        if (game != null) {
            if (waiting_for_move_successful) {
                ui.showMessage("Waiting for the server to validate your previous move...", UIColors.CYAN);
                waiting_for_move_successful_attempts += 1;
                if(waiting_for_move_successful_attempts >= 3)
                    resync();
                return;
            }

            try {
                game.moveMotherNature(myClientID, moves);
            } catch (InvalidMoveException e) {
                ui.showMessage(e.getMessage(), UIColors.RED);
                return;
            }
            socket_handler.send(MessageForServerLobby.motherNatureMoved(moves));
            waiting_for_move_successful = true;
            waiting_for_move_successful_attempts = 0;
        }
    }

    /**
     * Allows the client to choose a cloud in the game that is currently going on.
     * @see Game#chooseCloud(int, int)
     *
     * @param cloud_index index of the chosen cloud
     */
    public void chooseCloud(int cloud_index) {
        if (game != null) {
            if (waiting_for_move_successful) {
                ui.showMessage("Waiting for the server to validate your previous move...", UIColors.CYAN);
                waiting_for_move_successful_attempts += 1;
                if(waiting_for_move_successful_attempts >= 3)
                    resync();
                return;
            }

            try {
                game.chooseCloud(myClientID, cloud_index);
            } catch (InvalidMoveException e) {
                ui.showMessage(e.getMessage(), UIColors.RED);
                return;
            }
            socket_handler.send(MessageForServerLobby.cloudChosen(cloud_index));
            waiting_for_move_successful = true;
            waiting_for_move_successful_attempts = 0;
        }
    }

    /**
     * Allows the client to activate an Npc's effect in the game that is currently going on.
     * @see Game#activateEffect(int, int, EffectParameters)
     *
     * @param npc_index index of the activated Npc
     * @param effect_parameters parameters for the effect's activation
     */
    public void activateEffect(int npc_index, EffectParameters effect_parameters) {
        if (game != null) {
            if (waiting_for_move_successful) {
                ui.showMessage("Waiting for the server to validate your previous move...", UIColors.CYAN);
                waiting_for_move_successful_attempts += 1;
                if(waiting_for_move_successful_attempts >= 3)
                    resync();
                return;
            }

            try {
                game.activateEffect(myClientID, npc_index, effect_parameters);
            } catch (InvalidMoveException e) {
                ui.showMessage(e.getMessage(), UIColors.RED);
                return;
            }
            socket_handler.send(MessageForServerLobby.npcActivated(npc_index, effect_parameters));
            waiting_for_move_successful = true;
            waiting_for_move_successful_attempts = 0;
        }
    }

    /**
     * Allows the client to re-fetch the lobby's model instance from the server.
     */
    public void resync() {
        if (game != null) {
            socket_handler.send(MessageForServerLobby.resync());
        }
        waiting_for_move_successful_attempts = 0;
    }


    /*
         The following functions are meant to be used exclusively by the UI
     */

    /**
     * Getter for the {@link UI}, lets it have a proxy of the Model, in order to show its content to the user.
     *
     * @return a copy of {@link GameState} acting as a read-only proxy for the actual Model
     */
    public GameState getGameState() {
        if (game == null) return null;
        return new GameState(game);
    }

    /**
     * Provides the client's clientId.
     *
     * @return this client's clientId
     */
    public Integer getClientID() {
        if (myClientID == null) return null;
        return myClientID;
    }

    /**
     * Provides the client's nickname.
     *
     * @return this client's nickname
     */
    public String getNickname() {
        if (nickname == null) return null;
        return nickname;
    }

    /**
     * Provides the client's current lobby's {@link LobbyData}.
     *
     * @return this client's lobby's {@link LobbyData}
     */
    public LobbyData getLobby() {
        if (lobby == null) return null;
        return lobby.clone();
    }

    //getLobbies must first be called to get back a non-null list
    /**
     * Provides the list of available lobbies provided by the server after the last {@link Client#lobbiesList} call.
     * Importantly: getLobbies must first be called to get back a non-null and up-to-date list.
     *
     * @return the known list of available lobbies on the server
     */
    public List<LobbyData> getLobbiesList() {
        if (lobbiesList == null)
            return null;
        return new ArrayList<LobbyData>(lobbiesList);
    }

    /**
     * Provides the readiness status of all the players the current lobby.<br>
     * Its ordered the same as {@link LobbyData#clients}.
     *
     * @return the readiness of each player in the current lobby
     */
    public boolean[] getReadyFlags() {
        if (ready_flags == null)
            return new boolean[0];
        return ready_flags.clone();
    }

    //Used by the UI, before .start() is called, to set the nickname, also in case of a nickname already in use.
    /**
     * Method used by the UI to set the client's nickname and register it on the server.
     *
     * @param nickname the client's new nickname
     */
    public void setNickname(String nickname) {
        if (nickname == null) return;
        //if(this.nickname != null) return;

        this.nickname = nickname;
        if (socket_handler != null)
            socket_handler.send(MessageForServer.hello(this.myClientID, this.nickname));
    }

    /**
     * Utility method that converts a clientId in the corresponding client's nickname.<br>
     * Works for players in the current lobby only.
     *
     * @param clientID id to convert to nickname
     * @return corresponding nickname
     */
    public String clientIDToNickname(int clientID) {
        for (ClientData clientdata : this.getLobby().clients)
            if (clientdata.clientID == clientID)
                return clientdata.nickname;
        return "<disconnected>";
    }

    /**
     * Provides the current reconnection status, true when this client lost its connection and is currently attempting to recover it.
     *
     * @return true if the client has lost its connection and is attempting to recover it
     */
    public boolean getReconnection() {
        return reconnecting;
    }

    /**
     * Provides the parameters regarding the last move performed on the model by any other player besides this client.
     *
     * @return parameters for the last performed move on the model
     */
    public List<Integer> getLastMoveData() {
        return new ArrayList<Integer>(last_move_data);
    }

    /**
     * Invalidates the local storage of credentials, preventing their recovery with a future run of the program.<br>
     * Called before the program terminates normally.
     */
    public void deleteLocalStorage() {
        try {
            if (ENABLE_STORAGE && new File(LOCAL_STORAGE_NAME + "." + LOCAL_STORAGE_EXTENSION).isFile()) {
                ObjectOutputStream oos = new ObjectOutputStream(new FileOutputStream(LOCAL_STORAGE_NAME + "." + LOCAL_STORAGE_EXTENSION));
                FileStorage file_storage = new FileStorage(0, null);
                oos.writeObject(file_storage);

                oos.close();
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    /**
     * Terminates the client's connection and immediately after exits the program normally.
     */
    public void stop() {
        socket_receiver_thread.stop();
        socket_sender_thread.stop();
        ping_thread.stop();
        System.exit(0);
    }

    /**
     * The starting point for the client, prints the game's logo and proceeds to let the user enter the server's address.<br>
     * Immediately afterwards prompts the user for which {@link UI} to use.<br>
     * After everything has been decided starts the client and tries to establish a connection to the server.
     *
     * @param argv <i>unused</i>
     */
    public static void main(String[] argv) {
        Client client = null;

        Scanner console = new Scanner(System.in);

        System.out.print("""
                    
                    .-''-.   .-------.     .-./`)     ____     ,---.   .--. ,---------.     ____     __     .-'''-. \s
                  .'_ _   \\  |  _ _   \\    \\ .-.')  .'  __ `.  |    \\  |  | \\          \\    \\   \\   /  /   / _     \\\s
                 / ( ` )   ' | ( ' )  |    / `-' \\ /   '  \\  \\ |  ,  \\ |  |  `--.  ,---'     \\  _. /  '   (`' )/`--'\s
                . (_ o _)  | |(_ o _) /     `-'`"` |___|  /  | |  |\\_ \\|  |     |   \\         _( )_ .'   (_ o _).   \s
                |  (_,_)___| | (_,_).' __   .---.     _.-`   | |  _( )_\\  |     :_ _:     ___(_ o _)'     (_,_). '. \s
                '  \\   .---. |  |\\ \\  |  |  |   |  .'   _    | | (_ o _)  |     (_I_)    |   |(_,_)'     .---.  \\  :\s
                 \\  `-'    / |  | \\ `'   /  |   |  |  _( )_  | |  (_,_)\\  |    (_(=)_)   |   `-'  /      \\    `-'  |\s
                  \\       /  |  |  \\    /   |   |  \\ (_ o _) / |  |    |  |     (_I_)     \\      /        \\       / \s
                   `'-..-'   ''-'   `'-'    '---'   '.(_,_).'  '--'    '--'     '---'      `-..-'          `-...-'  \s
                                                                                                                    \s
                                        
                Hey there! You have to choose a server via:
                    * the server URL as \"www.example.url\" or \"https://www.text.org\"
                    * the server IP as X.X.X.X
                    * directly press ENTER for localhost
                                
                >""");

        String ip = console.nextLine();

        if (ip.matches("(http://|https://|[a-zA-Z])[a-zA-Z0-9]*\\.[a-zA-Z0-9\\.]+")) {
            if (!(ip.startsWith("http://") || ip.startsWith("https://")))
                ip = "https://" + ip;
            try {
                ip = InetAddress.getByName(new URI(ip).getHost()).getHostAddress();
                System.out.println("Retrieved IP: " + ip);
            } catch (Exception e) {
                ip = "127.0.0.1";
                System.out.println("Failed to recover the IP, defaulted to localhost...");
            }
        } else if (ip.matches("[0-9]{1,3}\\.[0-9]{1,3}\\.[0-9]{1,3}\\.[0-9]{1,3}")) {
            System.out.println("IP: " + ip);
        } else {
            ip = "127.0.0.1";
            System.out.println("Defaulted to localhost...");
        }

        System.out.print("\nSelect you user interface, type \"c\" for the CLI or \"g\" for the GUI:\n>");
        String input = console.nextLine();

        System.out.println("Connecting...");

        if (input.equals("g"))
            client = new Client(ip, 31234, new GUI());
        else
            client = new Client(ip, 31234, new CLI());

        //the commented lines have been moved in the client's constructor and .start method.
        //client.ui = new CLI();
        client.start();
        //client.ui.start(client);
    }
}

//auxiliary class for the only data that needs to be written to a file
/**
 * Auxiliary immutable class representing the data stored locally in order to recover the previous session even after a restart of the program.<br>
 * The data has a timestamp associated to it, it won't be valid for more than 24h.<br>
 * This class is intended to be serialized and stored locally in a file.
 */
class FileStorage implements Serializable {
    private final int id;
    private final String nickname;
    private final Instant time;

    /**
     * Constructs an instance of data to be stored locally.<br>
     * The data has a timestamp associated to it, it won't be valid for more than 24h.
     *
     * @param id clientId to store
     * @param nickname associated client nickname to store
     */
    public FileStorage(int id, String nickname) {
        this.id = id;
        this.nickname = nickname;
        this.time = Instant.now();
    }

    /**
     * Retrieves the stored clientId.
     *
     * @return the stored clientId
     */
    public int getId() {
        return id;
    }

    /**
     * Retrieves the stored nickname.
     *
     * @return the stored nickname
     */
    public String getNickname() {
        return nickname;
    }

    /**
     * Checks whether the stored data is valid, that being true if the data is less than 24h old and both the id and the nickname are valid.
     *
     * @return true if the stored data is valid
     */
    public boolean isValid() {
        return id != 0 && nickname != null && Instant.now().getEpochSecond() - time.getEpochSecond() < Client.STORAGE_DURATION;
    }
}