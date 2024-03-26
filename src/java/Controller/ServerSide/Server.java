package Controller.ServerSide;

import Controller.ClientSide.Client;
import Controller.*;
import Exceptions.LobbyException;
import org.jetbrains.annotations.TestOnly;

import java.io.IOException;
import java.net.InetAddress;
import java.net.ServerSocket;
import java.net.Socket;
import java.net.UnknownHostException;
import java.util.*;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.stream.Collectors;

/*
In the long term, this Server will become the welcome server, where clients connect,
but there will exist also a game server (both will inherit from a common abstract class) specific
for each lobby and game instance, clients will be moved to the game server by substituting the
current server attribute in the client handlers with the specific game server when they join a lobby,
so that it will be the game server the one receiving the updates.
Essentially joining a lobby redirects the updates to that lobby's game server.
 */

/*
Important: losing a connection before sending a "hello" to the server has the server simply ignoring that lost connection.
 */

/*
Use whenever there is a serialized payload to extract:

Object payload_object = null;
if(payload != null)
    payload_object = new ObjectInputStream(new ByteArrayInputStream(payload)).readObject();
 */

/**
 * Main Server class, both containing the welcome socket and handling the requests from those clients that not yet in a lobby.<br>
 * This class handles the connection of every new client and their subsequent registration via and hello message.<br>
 * Here are stored all the {@link ClientData} for every registered client, and all the lobbies currently available.<br>
 * The procedure for a connecting client is as follows:
 * <ul>
 *     <li> The client opens a connection with the server, being now simply connected and unable to do anything except registering
 *     <li> The client registers via an {@link MessageForServer#hello hello message}, choosing a nickname in the process
 *     <li> After the registration, a client can now access all the server's functions, suck as {@link Server#createLobby}, {@link Server#joinLobby} and {@link Server#getLobbies(SocketHandler) getLobbies}
 * </ul>
 * This class handles client requests through an additional queue to cope with potentially massive traffic, every {@link Message} received is queued and not executed immediately,
 * with a dedicated thread that consumes the queue separately.<br>
 * Each new socket connection has a read timeout of {@link Server#PING_TIMEOUT}, after which is severed if no message is received in the meantime.
 * Consequently {@link Client} implements a ping thread that always sends atleast a message to the server withing before the timout expires, unless the connection is lost.
 * <br><br>
 * Since it implements {@link Controller}, it is an <strong>OBSERVER</strong> to every {@link SocketHandler} of a connected client.
 */
public class Server extends Controller {
    public final static int PERMANENT_DISCONNECTION_TIME = 45000; //1 minute
    public final static int PING_TIMEOUT = 15000;

    private final int port;

    private final Map<Integer, ClientData> clients;
    private final Map<Integer, Thread> client_deletion_threads;
    private final List<Update> incoming_updates;

    /**
     * Returns the reference to this server's {@link ServerLobby}s map ({@link Server#lobbies}).
     * @return the lobbies map
     * @implNote This method is meant to be used for testing purposes only.
     */
    @TestOnly
    public Map<Integer, ServerLobby> getLobbies() {
        return lobbies;
    }

    private final LinkedHashMap<Integer, ServerLobby> lobbies;
    private final Random random_number_generator;

    /**
     * Prepares an instance of this class, configuring its welcome socket endpoint.
     *
     * @param port port the server will be available on
     */
    public Server(int port) {
        this.port = port;
        this.clients = new HashMap<Integer, ClientData>();
        this.client_deletion_threads = new HashMap<Integer, Thread>();
        this.incoming_updates = new ArrayList<Update>();
        this.lobbies = new LinkedHashMap<Integer, ServerLobby>();
        this.random_number_generator = new Random(1);
    }

    //Accepts incoming client connections and sets up a Client instance for each new connection
    /**
     * Starts the {@link Server} and opens its welcome socket, now accepting new clients.
     *
     * A call to this method is <strong>blocking</strong>, this method is not intended to return unless the program is forcefully terminated.
     */
    public void startServer() {
        ServerSocket server_socket;
        ExecutorService exec = Executors.newCachedThreadPool();

        try {
            server_socket = new ServerSocket(port);
        } catch(IOException e) {
            return;
        }

        exec.submit(this::updatesHandler);

        while(true) {
            try {
                Socket socket = server_socket.accept();
                socket.setSoTimeout(PING_TIMEOUT);
                SocketHandler client_handler = new SocketHandler(socket, this);
                exec.submit(client_handler.getReceiver());
                exec.submit(client_handler.getSender());
            } catch(IOException e) {
                e.printStackTrace();
                break;
            }
        }
        exec.shutdown();
    }

    //Routes incoming messages to the appropriate methods in charge of handling the proper reply
    /**
     * Consumes the queue of incoming {@link Message} by routing them to the appropriate methods in charge of delivering a proper
     * reply, using that messages's {@link Message#execute(Server, SocketHandler)}.
     */
    private synchronized void updatesHandler() {
        while(true) {
            while(incoming_updates.size() == 0) {
                try {
                    this.wait();
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }
            }

            Update update = incoming_updates.remove(0);

            try {
                //System.out.println("Update handled.");

                update.message.execute(this, update.socket_handler);
            } catch(Exception e) {
                e.printStackTrace();
            }
        }
    }

    //Receive updates and puts them in queue to be handled
    /**
     * {@inheritDoc}
     *
     * @param client_handler the {@link SocketHandler} which received the {@link Message} and is dispatching the update
     * @param message the {@link Message} received
     */
    public synchronized void update(SocketHandler client_handler, Message message) {
        incoming_updates.add(new Update(client_handler, message));
        this.notifyAll();
    }

    //Called whenever a client's connection is closed
    /**
     * {@inheritDoc}<br>
     * Before a client is completely forgotten tho, there is a window of {@link Server#PERMANENT_DISCONNECTION_TIME} seconds in which a new connection which registers itself with
     * the same id and nickname is recognized a the same client getting reconnected, and that prevents the deletion of that client.
     *
     * @param clientID the Id of the client, remembered by {@link SocketHandler}, losing the connection
     */
    public synchronized void handleDisconnect(int clientID) {
        if(clientID != 0)
           System.out.println("Disconnected player: " + clientID);

        if (clientID != 0) {
            Thread thread = new Thread(() -> deleteClient(clientID));
            thread.start();
            client_deletion_threads.put(clientID, thread);
        }
    }

    //Used by handleDisconnect to remove clients after PERMANENT_DISCONNECTION_TIME that they dropped their connection
    /**
     * Ran in a separate thread by {@link Client#handleDisconnect} to remove clients after {@link Server#PERMANENT_DISCONNECTION_TIME} that they dropped their connection.<br>
     * This thread is interrupted if the target client reconnects.
     *
     * @param clientID id of the client that got permanently disconnected
     */
    private void deleteClient(int clientID) {
        try {
            Thread.sleep(PERMANENT_DISCONNECTION_TIME);
        } catch(InterruptedException e) {
            e.printStackTrace();
        }
        synchronized(this) {
            if(clients.get(clientID).socket_handler.isClosed()) {
                //this lets automatically continue a potential game the client was in, simply without him
                if(lobbies.get(clients.get(clientID).getLobbyID()) != null)
                    lobbies.get(clients.get(clientID).getLobbyID()).permanentDisconnectedPlayer(clientID);
                clients.remove(clientID);
                client_deletion_threads.remove(clientID);
            }
        }
    }

    //MAYBE: saver each connection that does not send an hello withing a set timeout
    //Welcomes new clients providing them with their clientID if needed
    //if old_clientID is 0 it means there is none;

    /**
     * Registers a newly connected client, with the nickname it provides.
     * Consequence of {@link MessageForServer#hello}.
     *
     * @param client_handler {@link SocketHandler} which caused the update resulting in this method's invocation
     * @param old_clientID old client id for a reconnecting client, 0 for completely new clients
     * @param nickname nickname chosen by the client
     */
    public synchronized void hello(SocketHandler client_handler, int old_clientID, String nickname) {
        //checks if the client has already performed the hello
        if(client_handler.getClientID() != 0) {
            client_handler.send(MessageForClient.error("you are already logged in", 1));
            return;
        }

        if(old_clientID != 0 && clients.get(old_clientID) != null && clients.get(old_clientID).nickname.equals(nickname) && clients.get(old_clientID).socket_handler.isClosed()) {
            //if possible, reconnect a client with and old_clientID to his old lobby
            client_deletion_threads.get(old_clientID).stop();
            client_deletion_threads.remove(old_clientID);

            int oldLobbyID = clients.get(old_clientID).getLobbyID();
            clients.put(old_clientID, new ClientData(old_clientID, clients.get(old_clientID).nickname, client_handler));
            client_handler.setClientID(old_clientID);
            client_handler.send(MessageForClient.ack(old_clientID));

            //put the player back in its lobby
            if(oldLobbyID != 0) {
                try {
                    lobbies.get(oldLobbyID).reconnectPlayer(clients.get(old_clientID));
                } catch(LobbyException | NullPointerException e) {
                    client_handler.send(MessageForClient.error("unable to reconnect to previous lobby", 2));
                    System.out.println("unable to reconnect to previous lobby");
                    lobbies.get(oldLobbyID).permanentDisconnectedPlayer(old_clientID);
                }
            }

            System.out.println("Reconnected user with nickname: " + nickname + ", and clientID: " + old_clientID);
        } else if(nickname == null || nickname.length() == 0 || clients.values().stream().anyMatch(client -> client.nickname.equals(nickname))) {
            //prevents two distinct users from having the same nickname
            client_handler.send(MessageForClient.error("nickname already in use", 3));
        } else {
            //assigns a new clientID to a new user
            int newClientID = random_number_generator.nextInt();
            while(newClientID == 0 || clients.get(newClientID) != null)
                newClientID = random_number_generator.nextInt();

            clients.put(newClientID, new ClientData(newClientID, nickname, client_handler));
            client_handler.setClientID(newClientID);
            client_handler.send(MessageForClient.ack(newClientID));
            System.out.println("Registered user with nickname: " + nickname + ", and clientID: " + newClientID);
        }
    }

    //Creates a new lobby and puts in it its creator
    /**
     * Creates a new lobby and puts in it its creator.
     * Consequence of {@link MessageForServer#createLobby}.
     *
     * @param client_handler {@link SocketHandler} which caused the update resulting in this method's invocation
     * @param size number of players allowed and required by the new lobby
     * @param expert_mode flag indicating if expert mode is enabled or not in the new lobby
     */
    public synchronized void createLobby(SocketHandler client_handler, int size, boolean expert_mode) {
        if(!checkIDPresent(client_handler)) return;
        if ((size != 2 && size != 3)) {
            client_handler.send(MessageForClient.error("invalid parameters", 4));
        } else if(clients.get(client_handler.getClientID()).getLobbyID() != 0) {
            client_handler.send(MessageForClient.error("you cannot create a lobby while being in one", 5));
        } else {
            int newLobbyID = random_number_generator.nextInt();
            while(newLobbyID == 0 || lobbies.get(newLobbyID) != null)
                newLobbyID = random_number_generator.nextInt();

            ServerLobby lobby = buildLobby(newLobbyID, size, expert_mode);
            try {
                lobby.addPlayer(clients.get(client_handler.getClientID()));
            } catch(LobbyException e) {
                client_handler.send(MessageForClient.error("error while creating lobby", 6));
                return;
            }
            //client_handler.send(MessageForClient.setLobby(lobby.getLobbyData()));
            lobbies.put(newLobbyID, lobby);
        }
    }

    public synchronized ServerLobby buildLobby(int lobbyID, int size, boolean expert_mode) {
        return new ServerLobby(this, lobbyID, size, expert_mode);
    }

    //Allows the client to join a specific lobby, given its ID
    /**
     * Allows the client to join a specific lobby, given its ID.
     * Consequence of {@link MessageForServer#joinLobby}.
     *
     * @param client_handler {@link SocketHandler} which caused the update resulting in this method's invocation
     * @param lobbyID id of the lobby to join
     */
    public synchronized void joinLobby(SocketHandler client_handler, int lobbyID) {
        if(!checkIDPresent(client_handler)) return;
        if(lobbies.get(lobbyID) == null) {
            client_handler.send(MessageForClient.error("invalid lobby id", 4));
        } else if(clients.get(client_handler.getClientID()).getLobbyID() != 0) {
            client_handler.send(MessageForClient.error("you cannot join a lobby while being already in one", 5));
        } else {
            try {
                lobbies.get(lobbyID).addPlayer(clients.get(client_handler.getClientID()));
                //client_handler.send(MessageForClient.setLobby(lobbies.get(lobbyID).getLobbyData()));
            } catch(LobbyException e) {
                client_handler.send(MessageForClient.error(e.getMessage(), 6));
            }
        }
    }

    //Sends to the client a list of all available lobbies
    /**
     * Sends to the client a list of all available lobbies.
     * Consequence of {@link MessageForServer#getLobbies}.
     *
     * @param client_handler {@link SocketHandler} which caused the update resulting in this method's invocation
     */
    public synchronized void getLobbies(SocketHandler client_handler) {
        if(!checkIDPresent(client_handler)) return;
        //consider returning only lobbies with a free player slot
        client_handler.send(MessageForClient.lobbiesList(lobbies.values().stream().map(lobby -> lobby.getLobbyData()).collect(Collectors.toList())));
    }

    /**
     * Utility method, checks if the specified client is already registered or still has to send an {@link MessageForServer#hello hello message} to register.
     *
     * @param client_handler {@link SocketHandler} of the connected client
     * @return true if the client is properly registered
     */
    private synchronized boolean checkIDPresent(SocketHandler client_handler) {
        if(client_handler.getClientID() == 0) {
           client_handler.send(MessageForClient.error("you must first send and hello message", 5));
            return false;
        }
        if(clients.get(client_handler.getClientID()) == null) {
            client_handler.send(MessageForClient.error("you are not correctly logged in", 5));
            return false;
        }
        return true;
    }

    /**
     * The provided lobby gets deleted, it goes to dust, ceases to exist in every possibly meaningful way...<br>
     * Or at least that's what we all hope for, a quick end to its life, but it's up to the garbage collector to make it happen with no pain!
     *
     * @param lobbyID id of the lobby to delete
     */
    protected synchronized void deleteLobby(int lobbyID) {
        lobbies.remove(lobbyID);
    }

    /**
     * The main that starts the server, providing its LAN IP address on the console.
     *
     * @param argv <i>unused</i>
     */
    public static void main(String[] argv) {
        Server server = new Server(31234);
        try {
            System.out.println("Starting server on " + InetAddress.getLocalHost() + ":31234...");
        } catch (UnknownHostException e) {
            System.out.println("Starting server on COULD-NOT-GET-IP:31234...");
        }

        server.startServer();

        System.out.println("Server stopped...\n\n" +
                "If this continues to happen at startup probably another process is using port 31234 on your device!");
    }
}

class Update {
    public final SocketHandler socket_handler;
    public final Message message;

    public Update(SocketHandler client_handler, Message message) {
        this.socket_handler = client_handler;
        this.message = message;
    }
}