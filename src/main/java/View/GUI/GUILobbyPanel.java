package View.GUI;

import Controller.ClientSide.Client;
import Controller.ServerSide.LobbyData;

import javax.swing.*;
import javax.swing.table.DefaultTableCellRenderer;
import javax.swing.table.DefaultTableModel;
import javax.swing.text.Document;
import javax.swing.text.SimpleAttributeSet;
import javax.swing.text.StyleConstants;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.KeyEvent;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.stream.Collectors;

/**
 * Panel intended to show the available lobbies on the server, via a table, and to allow the user to either create a lobby or join one.<br>
 * For this purpose this panel has 2 states:
 * <ul>
 *     <li> Before the user is in a lobby ({@link GUI#inLobby} false), it displays the table of available lobbies and the options to search for a user,
 *     create a new lobby or refresh the current lobbies list
 *     <li> After the user joins a lobby ({@link GUI#inLobby} true), it displays the lobby's status, that being the players in it and their readiness,
 *     with options for the user to change its readiness or leave the lobby
 * </ul>
 */
public class GUILobbyPanel extends JPanel {
    private final int FONT_SIZE = 16;

    private int width, height;

    private final DefaultTableModel table_model;
    private final JScrollPane table_scroll_panel;
    private final JTable table;
    private final Action join;
    private final JButton refresh_button;
    private final JButton create_lobby;
    private final JLabel search_label;
    private final JTextField search_field;
    private final JDialog create_lobby_dialog;
    private final JComboBox create_lobby_mode;
    //private final JLabel lobby_label;
    private final JPanel lobby_panel;
    private final ImageIcon lobby_2p_background_image;
    private final ImageIcon lobby_3p_background_image;
    private final JLabel[] lobby_players;
    private final JLabel lobby_details;
    private final JLabel lobby_background;
    private final JButton toggleready_button;
    private final JButton leavelobby_button;
    private final JTextPane log;
    private final Client client;

    /**
     * Constructs the panel and all its components.
     *
     * @param client {@link Client} which is using the UI
     */
    public GUILobbyPanel(Client client) {
        this.client = client;

        width = GUIFrame.BASE_WIDTH;
        height = GUIFrame.BASE_HEIGHT;

        this.setLayout(new BorderLayout());

        //NB: each BorderLayout zone can only contain ONE element, adding more than one overwrites the previous one!

        JPanel center_panel = new JPanel();
        center_panel.setLayout(new BoxLayout(center_panel, BoxLayout.Y_AXIS));
        center_panel.setBorder(BorderFactory.createLineBorder(Color.gray));
        this.add(center_panel, BorderLayout.CENTER);

        JPanel line_end_panel = new JPanel();
        line_end_panel.setLayout(new BoxLayout(line_end_panel, BoxLayout.Y_AXIS));
        this.add(line_end_panel, BorderLayout.LINE_END);

        //CHOOSE-LOBBY UI COMPONENTS

        //String[] columnNames = {"ID", "Size", "Exp. Mode", "Players", "Join"};
        String[] columnNames = {"Size", "Exp. Mode", "Players", "Join"};

        //removes editing from the table except where the button is
        table_model = new DefaultTableModel(getTableData(), columnNames) {
            @Override
            public boolean isCellEditable(int row, int column) {
                return column == 3;
            }
        };
        table = new JTable(table_model);
        //default is 20
        int TABLE_ROW_HEIGHT = 40;
        table.setRowHeight(TABLE_ROW_HEIGHT);

        //center text in all columns except the button one
        DefaultTableCellRenderer centerRenderer = new DefaultTableCellRenderer();
        centerRenderer.setHorizontalAlignment( JLabel.CENTER );
        table.getColumnModel().getColumn(0).setCellRenderer( centerRenderer );
        table.getColumnModel().getColumn(1).setCellRenderer( centerRenderer );
        table.getColumnModel().getColumn(2).setCellRenderer( centerRenderer );

        join = new AbstractAction() {
            public void actionPerformed(ActionEvent e) {
                JTable table = (JTable)e.getSource();
                int row_index = Integer.parseInt(e.getActionCommand());
                if(!table.getModel().getValueAt(row_index, 0).equals("-")) {
                    client.joinLobby(client.getLobbiesList().get(row_index).lobbyID);
                }
            }
        };

        ButtonColumn buttonColumn = new ButtonColumn(table, join, 3); //4
        buttonColumn.setMnemonic(KeyEvent.VK_J);

        //set the fonts
        table.getTableHeader().setFont(new Font("Segoe UI", Font.BOLD, FONT_SIZE));
        table.setFont(new Font("Segoe UI", Font.PLAIN, FONT_SIZE));
        buttonColumn.setFont(new Font("Segoe UI", Font.PLAIN, FONT_SIZE));

        table_scroll_panel = new JScrollPane(table);
        center_panel.add(table_scroll_panel);
        table_scroll_panel.setVisible(true);

        refresh_button = new JButton("REFRESH");
        refresh_button.addActionListener(this::refreshTableButtonHandler);
        line_end_panel.add(refresh_button);
        refresh_button.setVisible(true);
        refresh_button.setFont(new Font("Segoe UI", Font.PLAIN, FONT_SIZE));

        create_lobby = new JButton("CREATE LOBBY");
        create_lobby.addActionListener(this::createLobbyButtonHandler);
        line_end_panel.add(create_lobby);
        create_lobby.setVisible(true);
        create_lobby.setFont(new Font("Segoe UI", Font.PLAIN, FONT_SIZE));

        search_label = new JLabel("<html><br>Search user:</html>");
        line_end_panel.add(search_label);
        search_label.setVisible(true);
        search_label.setFont(new Font("Segoe UI", Font.PLAIN, FONT_SIZE));

        search_field = new JTextField();
        search_field.setColumns(8);
        search_field.setMaximumSize(new Dimension(1000, 20));
        search_field.addActionListener(this::searchTable);
        line_end_panel.add(search_field);
        search_field.setVisible(true);

        //IN-LOBBY UI COMPONENTS

        /*lobby_label = new JLabel(" ");
        lobby_label.setVisible(false);
        center_panel.add(lobby_label);
        lobby_label.setFont(new Font("Segoe UI", Font.PLAIN, FONT_SIZE));*/
        lobby_panel = new JPanel();
        lobby_panel.setLayout(null);
        lobby_panel.setVisible(false);

        lobby_players = new JLabel[] {
                new JLabel(" "),
                new JLabel(" "),
                new JLabel(" ")
        };
        lobby_players[0].setLocation(284 - GUIFrame.BASE_WIDTH/12, 366 - GUIFrame.BASE_HEIGHT/12);
        lobby_players[0].setSize(new Dimension(GUIFrame.BASE_WIDTH/6, GUIFrame.BASE_HEIGHT/6));
        lobby_players[0].setFont(new Font("Segoe UI", Font.PLAIN, FONT_SIZE * 2));
        lobby_players[0].setHorizontalAlignment(SwingConstants.CENTER);
        lobby_players[0].setForeground(Color.black);
        //lobby_players[0].setBorder(new LineBorder(Color.red));
        lobby_panel.add(lobby_players[0]);

        lobby_players[1].setLocation(534 - GUIFrame.BASE_WIDTH/12, 366 - GUIFrame.BASE_HEIGHT/12);
        lobby_players[1].setSize(new Dimension(GUIFrame.BASE_WIDTH/6, GUIFrame.BASE_HEIGHT/6));
        lobby_players[1].setFont(new Font("Segoe UI", Font.PLAIN, FONT_SIZE * 2));
        lobby_players[1].setHorizontalAlignment(SwingConstants.CENTER);
        lobby_players[1].setForeground(Color.black);
        lobby_panel.add(lobby_players[1]);

        lobby_players[2].setLocation(414 - GUIFrame.BASE_WIDTH/12, 574 - GUIFrame.BASE_HEIGHT/12);
        lobby_players[2].setSize(new Dimension(GUIFrame.BASE_WIDTH/6, GUIFrame.BASE_HEIGHT/6));
        lobby_players[2].setFont(new Font("Segoe UI", Font.PLAIN, FONT_SIZE * 2));
        lobby_players[2].setHorizontalAlignment(SwingConstants.CENTER);
        lobby_players[2].setForeground(Color.black);
        lobby_panel.add(lobby_players[2]);

        lobby_details = new JLabel(" ");
        lobby_details.setLocation(894 - GUIFrame.BASE_WIDTH/10, 174 - GUIFrame.BASE_HEIGHT/4);
        lobby_details.setSize(new Dimension(GUIFrame.BASE_WIDTH/5, GUIFrame.BASE_HEIGHT/2));
        lobby_details.setFont(new Font("Segoe UI", Font.PLAIN, FONT_SIZE * 2));
        lobby_details.setHorizontalAlignment(SwingConstants.CENTER);
        lobby_details.setForeground(Color.black);
        //lobby_details.setBorder(new LineBorder(Color.red));
        lobby_panel.add(lobby_details);

        ClassLoader loader = ClassLoader.getSystemClassLoader();
        lobby_2p_background_image = new ImageIcon (Objects.requireNonNull(loader.getResource("LobbyBg2.png")));
        lobby_3p_background_image = new ImageIcon (Objects.requireNonNull(loader.getResource("LobbyBg3.png")));

        lobby_background = new JLabel(new ImageIcon(lobby_2p_background_image.getImage().getScaledInstance(GUIFrame.BASE_WIDTH - GUIFrame.BASE_WIDTH/6, GUIFrame.BASE_HEIGHT, Image.SCALE_SMOOTH)));
        lobby_background.setLocation(0, 0);
        lobby_background.setSize(new Dimension(GUIFrame.BASE_WIDTH - GUIFrame.BASE_WIDTH/6, GUIFrame.BASE_HEIGHT));
        lobby_panel.add(lobby_background);
        lobby_background.setVisible(true);

        center_panel.add(lobby_panel);

        toggleready_button = new JButton("TOGGLE READY");
        toggleready_button.addActionListener(this::togglereadyButtonHandler);
        toggleready_button.setVisible(false);
        toggleready_button.setFont(new Font("Segoe UI", Font.PLAIN, FONT_SIZE));
        line_end_panel.add(toggleready_button);

        leavelobby_button = new JButton("LEAVE LOBBY");
        leavelobby_button.addActionListener(this::leavelobbyButtonHandler);
        leavelobby_button.setVisible(false);
        leavelobby_button.setFont(new Font("Segoe UI", Font.PLAIN, FONT_SIZE));
        line_end_panel.add(leavelobby_button);

        //CREATE-LOBBY UI COMPONENTS

        JPanel create_lobby_dialog_panel = new JPanel();

        create_lobby_mode = new JComboBox(Modes.modes());
        create_lobby_dialog_panel.add(create_lobby_mode);
        create_lobby_mode.setFont(new Font("Segoe UI", Font.PLAIN, FONT_SIZE));

        JButton create_lobby_confirm = new JButton("CONFIRM");
        create_lobby_confirm.addActionListener(this::confirmCreateLobbyButtonHandler);
        create_lobby_dialog_panel.add(create_lobby_confirm);
        create_lobby_confirm.setFont(new Font("Segoe UI", Font.PLAIN, FONT_SIZE));

        create_lobby_dialog = new JDialog(GUI.frame, "Create new lobby:");
        create_lobby_dialog.add(create_lobby_dialog_panel);
        create_lobby_dialog.pack();
        create_lobby_dialog.setLocation(GUIFrame.WIDTH/2, GUIFrame.HEIGHT/2);
        create_lobby_dialog.setVisible(false);

        //ALWAYS PRESENT COMPONENTS

        log = new JTextPane();
        log.setEditable(false);
        log.setFont(GUI.font.deriveFont(14f));

        SimpleAttributeSet attributes = new SimpleAttributeSet();
        StyleConstants.setBold(attributes, true);
        log.setCharacterAttributes(attributes, true);
        log.setPreferredSize(new Dimension(GUIFrame.WIDTH/6, GUIFrame.HEIGHT/2));
        log.setText("Log start :)\n");
        //spacing...
        line_end_panel.add(new JLabel("<html><br><br></html>"));
        line_end_panel.add(new JScrollPane(log));

        this.setFocusable(true);
        this.setVisible(true);

        setPreferredSize(new Dimension((int) (GUIFrame.BASE_WIDTH*GUIFrame.STARTUP_RESOLUTION_MULTIPLIER),
                (int) (GUIFrame.BASE_HEIGHT*GUIFrame.STARTUP_RESOLUTION_MULTIPLIER)));
    }

    //HANDLERS

    /**
     * Handler for the "refresh" button on the table listing the available lobbies.
     * @param evt click event triggering the handler
     */
    private void refreshTableButtonHandler(ActionEvent evt) {
        client.getLobbies();
    }

    /**
     * Handler for the "toggle ready" button which allows a user to change its readiness while inside a lobby.
     * @param evt click event triggering the handler
     */
    private void togglereadyButtonHandler(ActionEvent evt) {
        client.toggleReady();
    }

    /**
     * Handler for the "leave lobby" button which allows a user leave its current lobby.
     * @param evt click event triggering the handler
     */
    private void leavelobbyButtonHandler(ActionEvent evt) {
        client.leaveLobby();
    }

    /**
     * Handler for the "create lobby" button which allows a user to create his own lobby.
     * @param evt click event triggering the handler
     */
    private void createLobbyButtonHandler(ActionEvent evt) {
        //setVisible is blocking for JDialogs!!!
        create_lobby_dialog.setLocationRelativeTo(GUI.frame);
        SwingUtilities.invokeLater(() -> create_lobby_dialog.setVisible(true));
    }

    /**
     * Handler for the "confirm" button in the lobby creation dialog which allows a user to confirm its lobby settings, completing the creation of the lobby.
     * @param evt click event triggering the handler
     */
    private void confirmCreateLobbyButtonHandler(ActionEvent evt) {
        switch(Modes.fromString((String) create_lobby_mode.getSelectedItem())) {
            case P2N -> client.createLobby(2, false);
            case P2E -> client.createLobby(2, true);
            case P3N -> client.createLobby(3, false);
            case P3E -> client.createLobby(3, true);
            default -> {return;}
        }

        create_lobby_dialog.setVisible(false);
    }

    //GENERIC METHODS

    //re-fills the table with a newly received lobbies list
    /**
     * Re-fills the table of lobbies with a newly received lobbies list.
     */
    public void refreshTable() {
        //String[] columnNames = {"ID", "Size", "Exp. Mode", "Players", "Join"};
        String[] columnNames = {"Size", "Exp. Mode", "Players", "Join"};
        table_model.setDataVector(getTableData(), columnNames);

        //center text in all columns except the button one
        DefaultTableCellRenderer centerRenderer = new DefaultTableCellRenderer();
        centerRenderer.setHorizontalAlignment( JLabel.CENTER );
        table.getColumnModel().getColumn(0).setCellRenderer( centerRenderer );
        table.getColumnModel().getColumn(1).setCellRenderer( centerRenderer );
        table.getColumnModel().getColumn(2).setCellRenderer( centerRenderer );

        ButtonColumn buttonColumn = new ButtonColumn(table, join, 3); //4
        buttonColumn.setMnemonic(KeyEvent.VK_J);

        buttonColumn.setFont(new Font("Segoe UI", Font.PLAIN, FONT_SIZE));
    }

    //re-writes the current lobby data text with a newly received one
    /**
     * Updates the current lobby data text with a newly received one.
     */
    public void refreshLobby() {
        if(client.getLobby().size == 2)
            lobby_background.setIcon(new ImageIcon(lobby_2p_background_image.getImage().getScaledInstance(width - width / 6, height, Image.SCALE_SMOOTH)));
        else
            lobby_background.setIcon(new ImageIcon(lobby_3p_background_image.getImage().getScaledInstance(width - width / 6, height, Image.SCALE_SMOOTH)));


        for(int i = 0; i < lobby_players.length; i++) {
            if(i < client.getLobby().clients.size() && i < client.getReadyFlags().length) {
                lobby_players[i].setText("<html>" + client.getLobby().clients.get(i).nickname + "<br>" + (client.getReadyFlags()[i] ? "READY!" : "") + "</html>");
            } else if(i < client.getLobby().size) {
                lobby_players[i].setText("<empty>");
            } else {
                lobby_players[i].setText(" ");
            }
        }

        lobby_details.setText("<html>Lobby size: " + client.getLobby().size +
                (client.getLobby().expert_mode ? "<br>Expert mode" : "<br>Normal mode") +
                "<br><br>" + (client.getLobby().size == client.getLobby().clients.size() ? "Waiting for everyone's readiness!" : "Waiting for " +
                (client.getLobby().size - client.getLobby().clients.size() == 1 ?
                ("one more player! ") : ("two more players!"))) + "</html>");
    }

    //prepares the lobbies list data to be inserted in the table
    /**
     * Utility method, prepares the lobbies list data to be inserted in the table.
     * @return prepared data for the table
     */
    private Object[][] getTableData() {
        List<LobbyData> lobbiesList = client.getLobbiesList();
        //Object[][] result = {{"---", "-", "--", "--- ---", "----"}};
        Object[][] result = {{"-", "--", "--- ---", "----"}};

        if(lobbiesList == null || lobbiesList.size() == 0) {
            return result;
        }

        result = new Object[lobbiesList.size()][5];

        for(int i = 0; i < lobbiesList.size(); i++) {
            //result[i][0] = lobbiesList.get(i).lobbyID;
            result[i][0] = lobbiesList.get(i).size;
            result[i][1] = lobbiesList.get(i).expert_mode ? "yes" : "no";
            result[i][2] = lobbiesList.get(i).clients.stream().map(client -> client.nickname)
                    .collect(Collectors.joining(", "));
            result[i][3] = "JOIN";
        }

        return result;
    }

    //prepares only the matching lobbies list entries to be inserted in the table
    /**
     * Similarly to {@link GUILobbyPanel#getTableData}, prepares data to be displayed in the lobbies table,
     * however prepares only the lobbies list entries matching the search string to be inserted in the table.
     * @param evt enter event on the search input
     */
    private void searchTable(ActionEvent evt) {
        List<LobbyData> lobbiesList = client.getLobbiesList();
        ArrayList<Object[]> data = new ArrayList<Object[]>();

        if(lobbiesList != null) {
            for(int i = 0; i < lobbiesList.size(); i++) {
                if(lobbiesList.get(i).clients.stream().map(client -> client.nickname)
                        .anyMatch(nickname -> nickname.toLowerCase().contains(search_field.getText().toLowerCase()))) {
                    data.add(new Object[]{
                            //lobbiesList.get(i).lobbyID,
                            lobbiesList.get(i).size,
                            lobbiesList.get(i).expert_mode ? "yes" : "no",
                            lobbiesList.get(i).clients.stream().map(client -> client.nickname)
                                    .collect(Collectors.joining(", ")),
                            "JOIN"
                    });
                }
            }
        }

        if(data.size() == 0)
            data.add(new Object[]{"-", "--", "--- ---", "----"});
            //data.add(new Object[]{"---", "-", "--", "--- ---", "----"});

        //String[] columnNames = {"ID", "Size", "Exp. Mode", "Players", "Join"};
        String[] columnNames = {"Size", "Exp. Mode", "Players", "Join"};
        table_model.setDataVector(data.toArray(new Object[0][]), columnNames);

        //center text in all columns except the button one
        DefaultTableCellRenderer centerRenderer = new DefaultTableCellRenderer();
        centerRenderer.setHorizontalAlignment( JLabel.CENTER );
        table.getColumnModel().getColumn(0).setCellRenderer( centerRenderer );
        table.getColumnModel().getColumn(1).setCellRenderer( centerRenderer );
        table.getColumnModel().getColumn(2).setCellRenderer( centerRenderer );

        ButtonColumn buttonColumn = new ButtonColumn(table, join, 3); //4
        buttonColumn.setMnemonic(KeyEvent.VK_J);

        buttonColumn.setFont(new Font("Segoe UI", Font.PLAIN, FONT_SIZE));
    }

    //shows the part of the panel dedicated to lobby selection
    /**
     * Shows the part of the panel dedicated to lobby selection.
     */
    public void showLobbySelection() {
        table_scroll_panel.setVisible(true);
        refresh_button.setVisible(true);
        create_lobby.setVisible(true);
        search_label.setVisible(true);
        search_field.setVisible(true);
        lobby_panel.setVisible(false);
        toggleready_button.setVisible(false);
        leavelobby_button.setVisible(false);

        create_lobby_dialog.setVisible(false);
    }

    //shows the part of the panel dedicated to the current lobby's status
    /**
     * Shows the part of the panel dedicated to the current lobby's status.
     */
    public void showLobbyStatus() {
        table_scroll_panel.setVisible(false);
        refresh_button.setVisible(false);
        create_lobby.setVisible(false);
        search_label.setVisible(false);
        search_field.setVisible(false);
        lobby_panel.setVisible(true);
        toggleready_button.setVisible(true);
        leavelobby_button.setVisible(true);
        for(int i = 0; i < lobby_players.length; i++) {
            lobby_players[i].setText(" ");
        }
        lobby_details.setText(" ");

        create_lobby_dialog.setVisible(false);
    }

    /**
     * {@inheritDoc}
     * @param width new component width
     * @param height new component height
     */
    @Override
    public void setSize(int width, int height) {
        super.setSize(width, height);
        log.setPreferredSize(new Dimension(width/6, height/2));

        this.width = width;
        this.height = height;

        float width_factor = (float) width/(float) GUIFrame.BASE_WIDTH;
        float height_factor = (float) height/(float) GUIFrame.BASE_HEIGHT;
        float factor = Math.min(width_factor, height_factor);

        lobby_players[0].setLocation((int) (286 * width_factor - width/12), (int) (366 * height_factor - height/12));
        lobby_players[0].setSize(new Dimension(width/6, height/6));
        lobby_players[0].setFont(new Font("Segoe UI", Font.PLAIN, (int) (FONT_SIZE * 2 * factor)));

        lobby_players[1].setLocation((int) (534 * width_factor - width/12), (int) (366 * height_factor - height/12));
        lobby_players[1].setSize(new Dimension(width/6, height/6));
        lobby_players[1].setFont(new Font("Segoe UI", Font.PLAIN, (int) (FONT_SIZE * 2 * factor)));

        lobby_players[2].setLocation((int) (414 * width_factor - width/12), (int) (574 * height_factor - height/12));
        lobby_players[2].setSize(new Dimension(width/6, height/6));
        lobby_players[2].setFont(new Font("Segoe UI", Font.PLAIN, (int) (FONT_SIZE * 2 * factor)));

        lobby_details.setLocation((int) (894 * width_factor - width/10), (int) (174 * height_factor - height/4));
        lobby_details.setSize(new Dimension(width/5, height/2));
        lobby_details.setFont(new Font("Segoe UI", Font.PLAIN, (int) (FONT_SIZE * 2 * factor)));

        if(client.getLobby() != null) {
            if(client.getLobby().size == 2)
                lobby_background.setIcon(new ImageIcon(lobby_2p_background_image.getImage().getScaledInstance(width - width / 6, height, Image.SCALE_SMOOTH)));
            else
                lobby_background.setIcon(new ImageIcon(lobby_3p_background_image.getImage().getScaledInstance(width - width / 6, height, Image.SCALE_SMOOTH)));
        } else
            lobby_background.setIcon(null);

        lobby_background.setLocation(0, 0);
        lobby_background.setSize(new Dimension(width - width/6, height));
    }

    //updates the integrated log
    /**
     * Updates the integrated log's text.
     * @param doc {@link Document} containing the updated text
     */
    public void updateLog(Document doc) {
        log.setDocument(doc);
        log.setCaretPosition(doc.getLength());
    }
}

//utility enum for lobby creation
/**
 * Utility enum with the 4 available configuration for a new lobby's settings.<br>
 * It is used to aid the lobby creation process. The possible configuration are:
 * <ul>
 *     <li> 2 players - normal mode
 *     <li> 2 players - expert mode
 *     <li> 3 players - normal mode
 *     <li> 3 players - expert mode
 * </ul>
 */
enum Modes {
    P2N("2 players - normal mode"), P2E("2 players - expert mode"),
    P3N("3 players - normal mode"), P3E("3 players - expert mode");

    public final String text;

    /**
     * Constructs each element of the enum.
     *
     * @param text the description associated with each element
     */
    Modes(String text) {
        this.text = text;
    }

    /**
     * Returns an array of strings listing the 4 available modes in this enum.
     *
     * @return array of strings representing the available modes
     */
    public static String[] modes() {
        return new String[]{
                P2N.text,
                P2E.text,
                P3N.text,
                P3E.text
        };
    }

    /**
     * From a given string returns the corresponding enum element.
     *
     * @param string string to match to an enum element
     * @return the string's corresponding enum element
     */
    public static Modes fromString(String string) {
        return switch (string) {
            case "2 players - normal mode" -> P2N;
            case "2 players - expert mode" -> P2E;
            case "3 players - normal mode" -> P3N;
            case "3 players - expert mode" -> P3E;
            default -> null;
        };
    }
}