package ControllerTest;

import Controller.ClientSide.Client;
import View.UI;
import View.UIColors;

/**
 * {@link UI} implementation meant to be used exclusively within a test context, where no no UI is actually shown.
 * @implNote This class does nothing but override all {@link UI} interface methods so that any call to them will do nothing.
 */
public class TestUI implements UI {

    @Override
    public void start(Client client) {

    }

    @Override
    public void nicknameConfirmed() {

    }

    @Override
    public void inLobby() {

    }

    @Override
    public void noLobby() {

    }

    @Override
    public void gameStart() {

    }

    @Override
    public void gameEnd(int winnerId) {

    }

    @Override
    public void resetState() {

    }

    @Override
    public void showMessage(String message, UIColors color) {

    }

    @Override
    public void refresh() {

    }
}
