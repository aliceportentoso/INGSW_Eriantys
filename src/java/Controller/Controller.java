package Controller;

import Controller.ClientSide.Client;

/**
 * Abstraction of a controller, implemented by both the {@link Client} and {@link Server}.<br>
 * A {@link Controller} is a costume <strong>OBSERVER</strong> to {@link SocketHandler}, which is supposed to call:
 * <ul>
 *     <li> {@link Controller#update} whenever a new {@link Message} arrives and has to be dispatched to the controller in charge
 *     <li> {@link Controller#handleDisconnect} when {@link SocketHandler} lost its connection and the controller has to recover it
 * </ul>
 */
public abstract class Controller {
    /**
     * Main method of every {@link Controller} as an observer, it is called when a new {@link Message} arrives at the observed {@link SocketHandler SocketHanders}.
     * This method handles the {@link Message} mainly by calling its {@link Message#execute} method, which allows the actions
     * associated to the message to take place on the controller.
     *
     * @param socket_handler the {@link SocketHandler} which received the {@link Message} and is dispatching the update
     * @param message the {@link Message} received
     */
    public abstract void update(SocketHandler socket_handler, Message message);

    /**
     * Method called by an observed {@link SocketHandler} only after it permanently looses its connection.
     *
     * @param clientID the Id of the client, remembered by {@link SocketHandler}, losing the connection
     */
    public abstract void handleDisconnect(int clientID);
}
