package Controller;

import Controller.ServerSide.Server;

import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.net.Socket;
import java.util.ArrayList;
import java.util.List;

/*
This class represents what an endpoint has to see of a socket connection, that being a way to receive updates when something
arrives via the connection, which is accomplished by the update method in the Controller abstract class, and a method to send
out messages to the other end of the connection, made possible in this case by the send method.
 */

/**
 * <strong>Wrapper class for everything {@link Socket} related</strong>.<br>
 * This class represents what a {@link Controller} has to see of a socket connection, that being a way to receive updates when something
 * arrives via the connection, which is accomplished by a call to {@link Controller#update}, and a method to send
 * out {@link Message Messages} to the other end of the connection, made possible in this case by the {@link SocketHandler#send} method.
 * <br><br>
 * This class is intended to be <strong>OBSERVED</strong> by a {@link Controller}, either the one who created it, or the one which received it
 * via {@link SocketHandler#changeUpdatesTarget}.
 * <br><br>
 * This class is based on two threads, the first running {@link SocketReceiver}, and the second {@link SocketSender},
 * the constructor of this class is entitled to run those threads after recovering them via {@link SocketHandler#getReceiver} and
 * {@link SocketHandler#getSender}.<br>
 * Those threads terminate automatically once the wrapped connection closes.
 */
public class SocketHandler {
    protected Socket socket;
    protected Controller controller;
    protected int clientID;
    private final SocketReceiver socket_receiver;
    private final SocketSender socket_sender;

    /**
     * Constructor that produces a new {@link SocketHandler} wrapped around the provided {@link Socket},
     * attaching it to the provided {@link Controller} which will act as its <strong>OBSERVER</strong>, becoming the
     * target of any updated coming from this instance of SocketHandler.
     *
     * @param socket {@link Socket} to wrap
     * @param controller {@link Controller} acting as an OBSERVER, target of any upcoming update
     */
    public SocketHandler(Socket socket, Controller controller) {
        this.socket = socket;
        this.controller = controller;
        this.clientID = 0;
        this.socket_receiver = new SocketReceiver(this);
        this.socket_sender = new SocketSender(this);
    }

    /**
     * Method used to complete a correct closure of the connection wrap inside this class.<br>
     * After the connection's closure has been completed, {@link Controller#handleDisconnect} is the invoked on the observing {@link Controller}.
     */
    protected void closeSocket() {
        if (!socket.isClosed()) {
            try {
                socket.close();
                socket_sender.send(null);
                controller.handleDisconnect(clientID);
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
    }

    /**
     * Retrieves the clientId associated to this connection, 0 if none has been associated yet.
     *
     * @return clientId associated with this connection
     */
    public int getClientID() {
        return clientID;
    }

    /**
     * Stores a clientId in this wrapper, associating it with this connection.
     *
     * @param clientID id to associate to this connection
     */
    public void setClientID(int clientID) {
        this.clientID = clientID;
    }

    /**
     * Checks if the wrapped socket connection is closed.<br>
     *
     * @return false if the connection is closed, true if it's still open
     */
    public boolean isClosed() {
        return socket.isClosed();
    }

    /**
     * Provides the thread that handles the receiving of {@link Message Messages}, it's up to the caller
     * how to run said thread, but unless it runs no {@link Message} will be received.
     *
     * @return the {@link Runnable} thread which will receive messages
     */
    public Runnable getReceiver() {
        return socket_receiver;
    }

    /**
     * Provides the thread that handles the sending of {@link Message Messages}, it's up to the caller
     * how to run said thread, but unless it runs no {@link Message} will be sent.
     *
     * @return the {@link Runnable} thread which will send messages
     */
    public Runnable getSender() {
        return socket_sender;
    }

    /**
     * Queues a new message to be sent to the other side of the wrapped connection by the dedicated thread.
     *
     * @param message {@link Message} to send
     */
    public void send(Message message) {
        if (!socket.isClosed())
            socket_sender.send(message);
    }

    /**
     * Allows the <strong>OBSERVER</strong>, recipient of the updates, of this class to be changed to a new {@link Controller}.<br>
     * Mainly used between {@link Server} and {@link Controller.ServerSide.ServerLobby ServerLobby}.
     *
     * @param controller new target of the updates and new OBSERVER
     */
    public void changeUpdatesTarget(Controller controller) {
        this.controller = controller;
    }
}

//handles the receiving of messages from the client it is referred to and consequently triggers a server update

/**
 * Local class used by {@link SocketHandler} to house the thread that receives messages.
 */
class SocketReceiver implements Runnable {
    private final SocketHandler socket_handler;

    /**
     * Constructor that memorizes a reference to the associated {@link SocketHandler}, whose connection the one where {@link Message Messages} are received.
     *
     * @param socket_handler associated {@link SocketHandler}
     */
    public SocketReceiver(SocketHandler socket_handler) {
        this.socket_handler = socket_handler;
    }

    /**
     * {@inheritDoc}
     */
    public void run() {
        ObjectInputStream in;
        try {
            in = new ObjectInputStream(socket_handler.socket.getInputStream());
        } catch (IOException e) {
            e.printStackTrace();
            return;
        }

        while (!socket_handler.socket.isClosed()) {
            try {
                Message message = (Message) in.readUnshared();
                //System.out.println("SocketHandler - Input read.");
                socket_handler.controller.update(socket_handler, message);
            } catch (ClassNotFoundException e) {
                //socket_handler.send(new Message("error", 0, 0, "improper message format".getBytes()));
                e.printStackTrace();
            } catch (IOException e) {
                //e.printStackTrace();
                socket_handler.closeSocket();
            }
        }
    }
}

//handles the sending of messages to the client it is referred to
/**
 * Local class used by {@link SocketHandler} to house the thread that sends messages.
 */
class SocketSender implements Runnable {
    private final SocketHandler socket_handler;
    private final List<Message> messages;

    /**
     * Constructor that memorizes a reference to the associated {@link SocketHandler}, whose connection is used to send {@link Message Messages}.
     *
     * @param socket_handler associated {@link SocketHandler}
     */
    public SocketSender(SocketHandler socket_handler) {
        this.socket_handler = socket_handler;
        messages = new ArrayList<Message>();
    }

    /**
     * Queues a {@link Message} to be sent.
     *
     * @param message {@link Message} to send
     */
    public synchronized void send(Message message) {
        messages.add(message);
        this.notifyAll();
    }

    /**
     * {@inheritDoc}
     */
    public synchronized void run() {
        ObjectOutputStream out;
        try {
            out = new ObjectOutputStream(socket_handler.socket.getOutputStream());
        } catch (IOException e) {
            e.printStackTrace();
            return;
        }

        while (!socket_handler.socket.isClosed()) {
            while (messages.size() == 0) {
                try {
                    this.wait();
                    if (socket_handler.socket.isClosed())
                        return;
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }
            }
            Message message = messages.remove(0);

            try {
                out.writeObject(message);
                out.flush();
                out.reset();
                //System.out.println("SocketHandler - Message sent.");
            } catch (IOException e) {
                //e.printStackTrace();
                socket_handler.closeSocket();
            }
        }
    }
}