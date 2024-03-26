package View;

import Controller.ClientSide.Client;

/**
 * Interface modelling a UI, implemented by {@link View.GUI.GUI GUI} and {@link View.CLI.CLI CLI}.<br>
 * A UI allows the user full access to the functionalities of the game via adequate input methods and visuals.
 */
public interface UI {
    //Takes control of the execution of the thread after its call, run the user interface using the provided Client
    /**
     * <strong>Takes control of the execution of the calling thread</strong>, runs the user interface using the provided Client.
     *
     * @param client reference {@link Client} for this user interface
     */
    public void start(Client client);

    //Signals the UI that the server accepted its username
    /**
     * Signals the UI that the server accepted its username, hence it can move on.
     */
    public void nicknameConfirmed();

    //Makes the UI change from choosing a lobby to being in a lobby
    /**
     * Makes the UI change from "choose a lobby" to "you are in a lobby".
     */
    public void inLobby();

    //Makes the UI know that you are not anymore in a lobby
    /**
     * Makes the UI know that you are not anymore in a lobby, hence going back to "choose a lobby".
     */
    public void noLobby();

    //Makes the UI change from the lobby and readiness selection screen to the game screen
    /**
     * Signals the UI to change from the lobby and readiness selection screen to the game screen.
     */
    public void gameStart();

    //Makes the UI show the winner and then go back to the lobby screen
    /**
     * Signals the UI to show the winner and then go back to the lobby screen, since the current game ended.
     * @param winnerId id of the player who won the last game
     */
    public void gameEnd(int winnerId);

    //Resets the UI to it's starting state, as immediately after login
    /**
     * Resets the UI to it's starting state, as immediately after its construction.
     */
    public void resetState();

    //Used to show messages to the user
    /**
     * Displays the given message to the user via the UI.
     *
     * @param message string to show to the user
     * @param color color for the displayed string
     */
    public void showMessage(String message, UIColors color);

    //Forces the UI to re-output the current Model state after re-fetching it.
    /**
     * Forces the UI to re-output the current Model state after re-fetching it from the client.
     */
    public void refresh();
}
