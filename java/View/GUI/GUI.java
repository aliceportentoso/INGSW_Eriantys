package View.GUI;

import Controller.ClientSide.Client;
import View.UI;
import View.UIColors;
import com.formdev.flatlaf.FlatDarculaLaf;

import javax.inject.Singleton;
import javax.swing.*;
import javax.swing.text.*;
import java.awt.*;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
import java.io.InputStream;
import java.io.PrintWriter;
import java.io.StringWriter;

//Graphic user interface class, contains the data used by the rest of the GUI classes and receives updated on the client's state

/**
 * Graphical user interface, allows the user to interact with the program via a windows with suitable graphics and interactable elements.
 * <br><br>
 * The GUI is implemented with Java Swing and AWT.<br>
 * The GUI has 3 main panels:
 * <ul>
 *     <li> {@link GUIWelcomePanel The welcome panel}, which allows for the choice of the nickname
 *     <li> {@link GUILobbyPanel The lobby panel}, which displays the available lobbies on the server and allows the user to either create one or join one
 *     <li> {@link GUIGamePanel The game panel}, whose purpose is to display a game and handle the inputs needed to play
 * </ul>
 * As soon as its {@link GUI#start started}, the GUI displays the {@link GUIWelcomePanel welcome panel}, transitioning to the {@link GUILobbyPanel lobby panel}
 * as soon as a suitable nickname has been chosen. Later a transition to the {@link GUIGamePanel game panel} is made if the user is in a lobby where everyone
 * declared himself ready. At the end of a game, the user is prompted with a button to switch back to the {@link GUILobbyPanel lobby panel}.
 * <br><br>
 * The GUI has 5 states:
 * <ul>
 *     <li> Before a proper nickname has been chosen, still in login, {@link GUI#inLogin} is true
 *     <li> After login, in the lobby selection screen ({@link GUILobbyPanel lobby panel}), {@link GUI#inLogin} false, {@link GUI#inLobby} false and {@link GUI#inGame} false
 *     <li> In a lobby, before a game starts, {@link GUI#inLobby} true and {@link GUI#inGame} false
 *     <li> In a game, {@link GUI#inGame} true
 *     <li> After a game, before the users goes back to the {@link GUILobbyPanel lobby panel}, {@link GUI#afterGame} true
 * </ul>
 * <br><br>
 * This class is a <strong>SINGLETON</strong>.
 */
@Singleton
public class GUI implements UI {
    public static boolean inLogin;
    public static boolean inLobby;
    public static boolean inGame;
    public static boolean afterGame;
    public static GUIFrame frame;
    public static Font font;

    /*private JDialog login_dialog;
    private JTextField text_field;
    private JLabel error_label;*/
    //private JFrame log_frame;
    private Document doc;
    private Client client;

    private static GUI gui;

    /**
     * Construct and initializes the {@link GUI} but doesn't start it.<br>
     * The GUI is only started by a later call to {@link GUI#start}.
     */
    public GUI() {
        inGame = false;
        inLobby = false;
        inLogin = true;
        afterGame = false;

        GUI.gui = this;
    }

    /**
     * {@inheritDoc}
     *
     * @param client reference {@link Client} for this user interface
     */
    public synchronized void start(Client client) {
        this.client = client;

        try {
            UIManager.setLookAndFeel(new FlatDarculaLaf());
        } catch (Exception e) {
            e.printStackTrace();
        }

        InputStream is = GUI.class.getResourceAsStream("Barlow-Medium.ttf");
        try {
            assert is != null;
            font = Font.createFont(Font.TRUETYPE_FONT, is);
        } catch (Exception e) {
            font = new Font("serif", Font.BOLD, 22);
        }

        SwingUtilities.invokeLater(() -> Thread.currentThread().setUncaughtExceptionHandler(
                (thread, t) -> {
                    StringWriter sw = new StringWriter();
                    t.printStackTrace(new PrintWriter(sw));
                    String message = sw.toString();
                    this.showMessage(message.length() > 100 ? message.substring(0, 99) + " ..." : message, UIColors.YELLOW);
                }
        ));

        frame = new GUIFrame(client);
        frame.setAlwaysOnTop(false);
        frame.setFont(font.deriveFont(22f));

        frame.addWindowListener(new WindowAdapter() {
            public void windowClosing(WindowEvent e) {
                client.deleteLocalStorage();
                client.stop();
            }
        });

        //LOGIN DIALOG

        /*login_dialog = new JDialog(frame, "Login", Dialog.ModalityType.APPLICATION_MODAL);
        login_dialog.setAlwaysOnTop(true);
        JPanel dialog_panel = new JPanel();
        dialog_panel.setLayout(new BoxLayout(dialog_panel, BoxLayout.Y_AXIS));

        JLabel nickname_label = new JLabel("Choose your nickname:");
        dialog_panel.add(nickname_label);

        text_field = new JTextField();
        text_field.setColumns(15);
        dialog_panel.add(text_field);
        text_field.addActionListener(this::inputPerformed);

        error_label = new JLabel(" ");
        error_label.setForeground(Color.RED);
        dialog_panel.add(error_label);

        login_dialog.setDefaultCloseOperation(WindowConstants.DO_NOTHING_ON_CLOSE);
        login_dialog.addWindowListener(new WindowAdapter() {
            public void windowClosing(WindowEvent we) {
                error_label.setText("You must choose a nickname!");
            }
        });

        login_dialog.add(dialog_panel);
        login_dialog.pack();
        login_dialog.setLocation(GUIFrame.WIDTH/2, GUIFrame.HEIGHT/2);
        //setVisible is blocking for JDialogs!!!
        SwingUtilities.invokeLater(() -> login_dialog.setVisible(client.getNickname() == null));
        */

        //frame.show();

        System.out.println("GUI Started...");
    }

    /**
     * Since this class is a singleton, returns its only instance.
     *
     * @return the only instance of this class
     */
    public static GUI getGUI() {
        if(gui == null)
            gui = new GUI();
        return gui;
    }

    //Login dialog input event handler
    /*private void inputPerformed(ActionEvent evt) {
        String text = text_field.getText();
        if(text.matches("^[A-Za-z][A-Za-z0-9_]{3,29}$")) {
            client.setNickname(text);
            error_label.setText(" ");
        } else {
            error_label.setText("Invalid nickname format...");
        }
        text_field.selectAll();
    }*/

    /**
     * {@inheritDoc}
     */
    public synchronized void nicknameConfirmed() {
        inLogin = false;
        /*if(login_dialog != null)
            login_dialog.setVisible(false);*/
        if (frame != null)
            SwingUtilities.invokeLater(() -> frame.showLobbyPanel());

        client.getLobbies();
        showMessage("Hello " + client.getNickname(), UIColors.GREEN);
    }

    //Those methods are synchronized to prevent their execution in parallel with start()

    /**
     * {@inheritDoc}
     */
    public synchronized void inLobby() {
        inLobby = true;
    }

    /**
     * {@inheritDoc}
     */
    public synchronized void noLobby() {
        inLobby = false;
    }

    /**
     * {@inheritDoc}
     */
    public synchronized void gameStart() {
        inGame = true;
        afterGame = false;
        SwingUtilities.invokeLater(() -> frame.getGamePanel().hideGameEnd());
    }

    /**
     * {@inheritDoc}
     *
     * @param winnerId id of the player who won the last game
     */
    public synchronized void gameEnd(int winnerId) {
        afterGame = true;
        SwingUtilities.invokeLater(() -> frame.getGamePanel().showGameEnd(winnerId));
        refresh();
    }

    /**
     * {@inheritDoc}
     */
    public synchronized void resetState() {
        if (client.getNickname() != null) {
            inLogin = false;
            //login_dialog.setVisible(false);
            frame.showLobbyPanel();
        } else {
            inLogin = true;
            /*if(login_dialog != null)
                //setVisible is blocking for JDialogs!!!
                SwingUtilities.invokeLater(() -> login_dialog.setVisible(true));*/
            if (frame != null)
                frame.showWelcomePanel();
        }

        inLobby = false;
        inGame = false;
        afterGame = false;
    }

    /**
     * {@inheritDoc}
     *
     * @param message string to show to the user
     * @param color   color for the displayed string
     */
    public void showMessage(String message, UIColors color) {
        if (this.doc == null)
            doc = new DefaultStyledDocument();

        //prevent log to grow indefinitely
        if (doc.getLength() > 500) {
            try {
                doc.remove(
                        0, (doc.getText(0, 200)).lastIndexOf("\n")
                );
            } catch (BadLocationException e) {
                doc = new DefaultStyledDocument();
            }
        }

        if (inLogin && frame != null) {
            //error_label.setForeground(color.awtColor);
            //error_label.setText(message);
            frame.getWelcomePanel().showError(message, color.awtColor);
        } else if (frame != null && (frame.getGamePanel() != null || (!inGame && frame.getLobbyPanel() != null))) {
            SimpleAttributeSet attributes = new SimpleAttributeSet();
            /*if(client.getReconnection()) {
                StyleConstants.setBold(attributes, true);
                StyleConstants.setForeground(attributes, Color.red);
            } else
                StyleConstants.setForeground(attributes, Color.decode("#ccffff"));*/
            //StyleConstants.setBackground(attributes, Color.blue);
            StyleConstants.setForeground(attributes, color.awtColor);

            try {
                doc.insertString(doc.getLength(), "\n" + message, attributes);
            } catch (BadLocationException e) {
                e.printStackTrace();
            }

            SwingUtilities.invokeLater(() -> frame.getGamePanel().updateLog(doc));
            SwingUtilities.invokeLater(() -> frame.getLobbyPanel().updateLog(doc));
        } else {
            System.out.println(message);
        }
    }

    /**
     * {@inheritDoc}
     */
    public void refresh() {
        SwingUtilities.invokeLater(() -> {
            if (frame != null) {
                if (!inLobby) { //you are not in any lobby
                    frame.showLobbyPanel();
                    frame.getLobbyPanel().showLobbySelection();
                    frame.getLobbyPanel().refreshTable();
                } else if (!inGame) { //in a lobby, before the game starts
                    frame.showLobbyPanel();
                    frame.getLobbyPanel().showLobbyStatus();
                    frame.getLobbyPanel().refreshLobby();
                } else { //in a lobby, with a game going on
                    frame.showGamePanel();
                }
            }
        });
    }
}
