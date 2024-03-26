package View.GUI;

import Controller.ClientSide.Client;

import javax.swing.*;
import java.awt.*;
import java.awt.event.ComponentAdapter;
import java.awt.event.ComponentEvent;

/**
 * Main frame for the {@link GUI}.<br>
 * This frame contains 3 {@link JPanel JPanels} which are overlayed via {@link OverlayLayout}, those JPanels are then
 * shown one at a time depending on the current state of the {@link GUI}, the JPanels are:
 * <ul>
 *     <li> A {@link GUIGamePanel}, obtainable via {@link GUIFrame#getGamePanel}
 *     <li> A {@link GUILobbyPanel}, obtainable via {@link GUIFrame#getLobbyPanel}
 *     <li> A {@link GUIWelcomePanel}, obtainable via {@link GUIFrame#getWelcomePanel}
 * </ul>
 * Here are also configured the {@link GUIFrame#BASE_WIDTH} and {@link GUIFrame#BASE_HEIGHT} for the application, those determining
 * the resolution it starts at, even tho it's entirely resizable.
 */
public class GUIFrame extends JFrame {
    public static final double STARTUP_RESOLUTION_MULTIPLIER = 1;

    public static final int BASE_WIDTH = 1280;
    public static final int BASE_HEIGHT = 720;

    public static final int WIDTH = (int) (BASE_WIDTH * STARTUP_RESOLUTION_MULTIPLIER) + 20;
    public static final int HEIGHT = (int) (BASE_HEIGHT * STARTUP_RESOLUTION_MULTIPLIER) + 40;

    private GUIGamePanel game_panel;
    private GUILobbyPanel lobby_panel;
    private GUIWelcomePanel welcome_panel;

    /**
     * Constructs the frame and its 3 panels, passing each a reference to the UI's client.
     *
     * @param client {@link Client} which is using the UI
     */
    public GUIFrame(Client client) {
        this.setLayout(new BorderLayout());

        JPanel overlay_panel = new JPanel();
        overlay_panel.setLayout(new OverlayLayout(overlay_panel));

        try {
            //panel used only during a game
            game_panel = new GUIGamePanel(client);
            overlay_panel.add(game_panel, BorderLayout.CENTER);
            game_panel.addComponentListener(new ComponentAdapter() {
                public void componentResized(ComponentEvent event) {
                    game_panel.setSize(event.getComponent().getSize().width, event.getComponent().getSize().height);
                    GUIGamePanel.resized(event.getComponent().getSize().width, event.getComponent().getSize().height);
                    game_panel.updateResized();
                }
            });

            //panel used for lobby selection and lobby wait before readiness
            lobby_panel = new GUILobbyPanel(client);
            overlay_panel.add(lobby_panel, BorderLayout.CENTER);
            lobby_panel.addComponentListener(new ComponentAdapter() {
                public void componentResized(ComponentEvent event) {
                    SwingUtilities.invokeLater(() -> lobby_panel.setSize(event.getComponent().getSize().width, event.getComponent().getSize().height));
                }
            });

            //login panel
            welcome_panel = new GUIWelcomePanel(client);
            overlay_panel.add(welcome_panel, BorderLayout.CENTER);
            welcome_panel.addComponentListener(new ComponentAdapter() {
                public void componentResized(ComponentEvent event) {
                    SwingUtilities.invokeLater(() -> welcome_panel.resized(event.getComponent().getSize().width, event.getComponent().getSize().height));
                }
            });

        } catch (java.lang.NullPointerException e) {
            System.out.println("Could not load resources! Terminating...");
            client.stop();
        }

        this.add(overlay_panel);

        showWelcomePanel();

        this.setTitle("ERIANTYS");
        this.setSize(WIDTH, HEIGHT);

        //pack();
        this.setLocationRelativeTo(null);
        this.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        this.setResizable(true);
        this.setVisible(true);
    }

    /**
     * Provides the {@link GUIGamePanel} of this frame
     *
     * @return this frame's {@link GUIGamePanel}
     */
    public GUIGamePanel getGamePanel() {
        return game_panel;
    }

    /**
     * Provides the {@link GUILobbyPanel} of this frame
     *
     * @return this frame's {@link GUILobbyPanel}
     */
    public GUILobbyPanel getLobbyPanel() {
        return lobby_panel;
    }

    /**
     * Provides the {@link GUIWelcomePanel} of this frame
     *
     * @return this frame's {@link GUIWelcomePanel}
     */
    public GUIWelcomePanel getWelcomePanel() {
        return welcome_panel;
    }

    /**
     * Makes the {@link GUIFrame} display only its {@link GUIGamePanel}, hiding everything else.
     */
    public void showGamePanel() {
        game_panel.setVisible(true);
        lobby_panel.setVisible(false);
        welcome_panel.setVisible(false);
    }

    /**
     * Makes the {@link GUIFrame} display only its {@link GUILobbyPanel}, hiding everything else.
     */
    public void showLobbyPanel() {
        game_panel.setVisible(false);
        lobby_panel.setVisible(true);
        welcome_panel.setVisible(false);
    }

    /**
     * Makes the {@link GUIFrame} display only its {@link GUIWelcomePanel}, hiding everything else.
     */
    public void showWelcomePanel() {
        game_panel.setVisible(false);
        lobby_panel.setVisible(false);
        welcome_panel.setVisible(true);
    }
}
