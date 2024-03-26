package View.GUI;

import Controller.ClientSide.Client;
import Model.*;

import javax.swing.*;
import javax.swing.Timer;
import javax.swing.text.Document;
import javax.swing.text.SimpleAttributeSet;
import javax.swing.text.StyleConstants;
import java.awt.*;
import java.awt.event.*;
import java.awt.geom.AffineTransform;
import java.util.*;
import java.util.List;

//Game panel, where the game takes place, handles both rendering and inputs

/**
 * Main panel where a game takes place.<br>
 * It incorporates numerous {@link GUIElement GUIElements} and redraws itself at 30 FPS (frames per second) via a {@link JPanel#paintComponent} override and a {@link Timer}.
 * <br><br>
 * Here are permanently instantiated all the {@link GUIElement GUIElements} required for a game, and none of those instance is ever deleted, they are always reused, since
 * they do not represent a specific element in the game, but they dynamically map to one in the model depending on the model's current state. This way this panel doesn't need
 * to construct any further {@link GUIElement} after its construction.<br>
 * The position of all the {@link GUIElement GUIElements} is decided with respect of {@link GUIFrame#BASE_WIDTH} and {@link GUIFrame#BASE_HEIGHT}, and is the up to every element
 * to draw itself with respect of {@link GUIGamePanel#SCREEN_WIDTH_SCALE} and {@link GUIGamePanel#SCREEN_HEIGHT_SCALE} for its X and Y, and {@link GUIGamePanel#SCREEN_SCALE} for its dimensions.
 * <br><br>
 * This class also handles the calls to the {@link Client}'s methods to proceed in game, depending on the various user inputs on the different {@link GUIElement GUIElements}.
 */
public class GUIGamePanel extends JPanel implements ActionListener {
    //NEVER CHANGE THOSE HERE, since they are being used to locate elements on the screen, change them at the end of the constructor
    public static int SCREEN_WIDTH = GUIFrame.BASE_WIDTH;
    public static int SCREEN_HEIGHT = GUIFrame.BASE_HEIGHT;
    public static double SCREEN_WIDTH_SCALE = 1*GUIFrame.STARTUP_RESOLUTION_MULTIPLIER;
    public static double SCREEN_HEIGHT_SCALE = 1*GUIFrame.STARTUP_RESOLUTION_MULTIPLIER;
    public static double SCREEN_SCALE = 1*GUIFrame.STARTUP_RESOLUTION_MULTIPLIER;
    public static int PARTICLE_SIZE = 100;
    private static int FRAME_DELAY = 30;

    private final Client client;
    private final JButton game_ended_return_to_lobby_button;
    private final JButton leavelobby_button;
    private final JDialog leave_dialog;
    private final JDialog npc_dialog;
    private final JTextArea npc_text_area;
    private final JTextArea npc_overlay_text_area;
    private final JTextPane log;
    private final JScrollPane log_pane;
    private int invisible_npc_overlay_text_area_requests;
    private int winnerId;

    private int previous_playing_player_id;
    private int previous_game_phase;
    private int previous_game_step;
    private int previous_game_moved_students;

    private final List<GUIElement> elements;
    private final List<GUIIsland> islands;
    private final List<GUICloud> clouds;
    private final List<GUINpc> npcs;
    private final List<GUIText> texts;
    private final List<GUIParticle> particles;

    //used only during phase 1 step 0, currently selected student from the entrance
    private int selected_student_index;
    //used to hide all the cards inside only
    private boolean compact_cards;
    //used to track the activation of an NPC and allow the selection of its parameters
    private int activated_npc;

    //lists of the selected elements for an NPC's activation
    private final List<Integer> selected_npc_students;
    private final List<Integer> selected_dashboard_colors;
    private int selected_npc_island;
    private final List<Integer> selected_npc_entrance_students;

    private final Image opponent_dashboard;
    private final Image coin_image;
    public static Image[] card_images;
    public static Image[] rotated_left_card_images;
    public static Image[] rotated_right_card_images;
    public static Image[] npc_images;
    public static Image[] bridge_images;
    public static Map<Colors, Image> student_images;
    public static Map<Colors, Image> professor_images;
    public static Image[] rook_images;
    public static Image mother_nature_image;
    public static Image start_particle;
    public static Image cloud_particle;
    public static Image dust_particle;

    /**
     * Constructs the panel and all its components and {@link GUIElement GUIElements}.<br>
     * Also preloads all the resources later used by the various {@link GUIElement GUIElements} as images.
     *
     * @param client {@link Client} which is using the UI
     * @throws java.lang.NullPointerException thrown if some resources could not be loaded
     */
    public GUIGamePanel(Client client) throws java.lang.NullPointerException {
        this.setLayout(null);

        this.client = client;
        selected_student_index = -1;
        compact_cards = true;
        activated_npc = -1;
        selected_npc_students = new ArrayList<Integer>();
        selected_dashboard_colors = new ArrayList<Integer>();
        selected_npc_island = -1;
        selected_npc_entrance_students = new ArrayList<Integer>();
        winnerId = 0;

        Timer timer = new Timer(FRAME_DELAY, this);
        timer.start();

        this.elements = new ArrayList<GUIElement>();
        this.islands = new ArrayList<GUIIsland>();
        this.clouds = new ArrayList<GUICloud>();
        this.npcs = new ArrayList<GUINpc>();
        this.texts = new ArrayList<GUIText>();
        this.particles = new ArrayList<GUIParticle>();

        previous_playing_player_id = 0;
        previous_game_phase = 0;
        previous_game_step = 0;
        previous_game_moved_students = 0;

        ClassLoader loader = ClassLoader.getSystemClassLoader();

        //create main player's dashboard
        GUIDashboard dashboard = new GUIDashboard(client, this, (new ImageIcon(Objects.requireNonNull(loader.getResource("Dashboard/Dashboard.png")))).getImage(),
                SCREEN_WIDTH/2 - 3352/9, (int) (SCREEN_HEIGHT - 1454/4.5), (int) (3352/4.5), (int) (1454/4.5));
        this.elements.add(dashboard);
        this.addMouseListener(dashboard);

        //create clouds
        for(int i = 0; i < 3; i++) {
            GUICloud gui_cloud = new GUICloud(client, this, (new ImageIcon(Objects.requireNonNull(loader.getResource("Clouds/Cloud.png")))).getImage(),
                    700 + 80*i, 150 , 80, 80, i);
            this.elements.add(gui_cloud);
            this.clouds.add(gui_cloud);
            this.addMouseListener(gui_cloud);
        }

        //create entrance students
        for(int i = 0; i < 9; i++) {
            GUIEntranceStudent gui_entrance_student = new GUIEntranceStudent(client, this, SCREEN_WIDTH / 2 - 3352 / 9 + 10 + 40 * (i / 5),
                    (int) (SCREEN_HEIGHT - 1454 / 4.5 + 40 * ((i % 5) + 1)), 30, i);
            this.elements.add(gui_entrance_student);
            this.addMouseListener(gui_entrance_student);
        }

        //create islands (clockwise, properly)
        for(int i = 0; i < 6; i++) {
            GUIIsland gui_island = new GUIIsland(client, this, (new ImageIcon(Objects.requireNonNull(loader.getResource("Islands/Island (" + ((i % 3) + 1) + ").png")))).getImage(),
                    SCREEN_WIDTH/2 + 135*((i % 6) - 3) + 25, (i == 0 || i == 5) ? 95 : 60, 85, 85, i);
            this.elements.add(gui_island);
            this.islands.add(gui_island);
            this.addMouseListener(gui_island);
        }
        for(int i = 6; i < 12; i++) {
            GUIIsland gui_island = new GUIIsland(client, this, (new ImageIcon(Objects.requireNonNull(loader.getResource("Islands/Island (" + ((i % 3) + 1) + ").png")))).getImage(),
                    SCREEN_WIDTH/2 + 135*(((17 - i) % 6) - 3) + 25, 110 + 150*(((17 - i) / 6)) - ((i == 6 || i == 11) ? 35 : 0), 85, 85, i);
            this.elements.add(gui_island);
            this.islands.add(gui_island);
            this.addMouseListener(gui_island);
        }

        //create cards
        for(int i = 0; i < 10; i++) {
            GUICard gui_card = new GUICard(client, this, SCREEN_WIDTH/2 + 100*(i - 5), SCREEN_HEIGHT - 1710/15, 1164/15, 1710/15, i);
            this.elements.add(gui_card);
            this.addMouseListener(gui_card);
        }

        //create compacted card
        for(int i = 0; i < 10; i++) {
            GUICardCompacted gui_card_compacted = new GUICardCompacted(client, this, 10*(i), SCREEN_HEIGHT - 1710/15, 1164/15, 1710/15, i);
            this.elements.add(gui_card_compacted);
            this.addMouseListener(gui_card_compacted);
        }

        //create NPCs
        for(int i = 0; i < 3; i++) {
            GUINpc gui_npc = new GUINpc(client, this, 400 + (1164/22 + 10)*i, 150, 1164/22, 1710/22, i);
            this.elements.add(gui_npc);
            this.npcs.add(gui_npc);
            this.addMouseListener(gui_npc);
            //create students above NPCs
            for(int j = 0; j < 6; j++) {
                GUINpcStudent gui_npc_student = new GUINpcStudent(client, this,
                        (int)((400 + (1164/22 + 10)*i + 1164/88 + (1164/44)*(j%2) - 7)*SCREEN_WIDTH_SCALE),
                        (int)((150 + 30*(j/2))*SCREEN_HEIGHT_SCALE), 15, j, i);
                this.elements.add(gui_npc_student);
                this.addMouseListener(gui_npc_student);
            }
        }

        //create Text area main player
        texts.add(new GUIText(0, (int) (SCREEN_HEIGHT - (1454 / 4.1) * SCREEN_HEIGHT_SCALE),
                SCREEN_WIDTH, 30, 22, false, Color.black));

        //create Text area left player
        texts.add(new GUIText(0, -5 , (int) ((1454 / 7) * SCREEN_SCALE), 30,22, false, Color.black));

        //create Text area right player
        texts.add(new GUIText((SCREEN_WIDTH - (int) ((1454 / 7) * SCREEN_SCALE)), -5,
                (int) ((1454 / 7) * SCREEN_SCALE), 30,22, false, Color.black));

        //create Text area next player
        texts.add(new GUIText( SCREEN_WIDTH/2, -5,
                (int) (SCREEN_WIDTH/2 - (1454 / 7) * GUIGamePanel.SCREEN_SCALE), 30, 22, false, Color.darkGray));

        //create Text area gamestate for 3 players
        texts.add(new GUIText ( (int) ((1454 / 7) * SCREEN_SCALE), -5,
                (int) (SCREEN_WIDTH/2 - (1454 / 7) * GUIGamePanel.SCREEN_SCALE), 30, 22, false, Color.black));

        //create Text area gamestate for 2 players
        texts.add(new GUIText (0, -5, SCREEN_WIDTH, 30, 22, false, Color.black));

        //preloading of images
        this.opponent_dashboard = (new ImageIcon(Objects.requireNonNull(loader.getResource("Dashboard/DashboardR.png")))).getImage();
        this.coin_image = (new ImageIcon (Objects.requireNonNull(loader.getResource("Coin.png")))).getImage();

        card_images = new Image[10];
        for(int i = 0; i < 10; i++)
            card_images[i] = (new ImageIcon(Objects.requireNonNull(loader.getResource("Cards/Card (" + (i + 1) + ").png")))).getImage();

        rotated_left_card_images = new Image[10];
        for(int i = 0; i < 10; i++)
            rotated_left_card_images[i] = (new ImageIcon(Objects.requireNonNull(loader.getResource("CardsLeft/Card (" + (i + 1) + ").png")))).getImage();

        rotated_right_card_images = new Image[10];
        for(int i = 0; i < 10; i++)
            rotated_right_card_images[i] = (new ImageIcon(Objects.requireNonNull(loader.getResource("CardsRight/Card (" + (i + 1) + ").png")))).getImage();

        npc_images = new Image[12];
        for(int i = 0; i < 12; i++)
            npc_images[i] = (new ImageIcon(Objects.requireNonNull(loader.getResource("Npcs/Npc (" + (i + 1) + ").jpg")))).getImage();

        student_images = new EnumMap<Colors, Image>(Colors.class);
        student_images.put(Colors.YELLOW, (new ImageIcon(Objects.requireNonNull(loader.getResource("Students/3D/Student (1).png")))).getImage());
        student_images.put(Colors.BLUE, (new ImageIcon(Objects.requireNonNull(loader.getResource("Students/3D/Student (2).png")))).getImage());
        student_images.put(Colors.GREEN, (new ImageIcon(Objects.requireNonNull(loader.getResource("Students/3D/Student (3).png")))).getImage());
        student_images.put(Colors.RED, (new ImageIcon(Objects.requireNonNull(loader.getResource("Students/3D/Student (4).png")))).getImage());
        student_images.put(Colors.MAGENTA, (new ImageIcon(Objects.requireNonNull(loader.getResource("Students/3D/Student (5).png")))).getImage());

        professor_images = new EnumMap<Colors, Image>(Colors.class);
        professor_images.put(Colors.YELLOW, (new ImageIcon(Objects.requireNonNull(loader.getResource("Professors/3D/Professor (1).png")))).getImage());
        professor_images.put(Colors.BLUE, (new ImageIcon(Objects.requireNonNull(loader.getResource("Professors/3D/Professor (2).png")))).getImage());
        professor_images.put(Colors.GREEN, (new ImageIcon(Objects.requireNonNull(loader.getResource("Professors/3D/Professor (3).png")))).getImage());
        professor_images.put(Colors.RED, (new ImageIcon(Objects.requireNonNull(loader.getResource("Professors/3D/Professor (4).png")))).getImage());
        professor_images.put(Colors.MAGENTA, (new ImageIcon(Objects.requireNonNull(loader.getResource("Professors/3D/Professor (5).png")))).getImage());

        rook_images = new Image[3];
        rook_images[0] = (new ImageIcon(Objects.requireNonNull(loader.getResource("Rooks/Black_Rook.png")))).getImage();
        rook_images[1] = (new ImageIcon(Objects.requireNonNull(loader.getResource("Rooks/White_Rook.png")))).getImage();
        rook_images[2] = (new ImageIcon(Objects.requireNonNull(loader.getResource("Rooks/Gray_Rook.png")))).getImage();

        mother_nature_image = (new ImageIcon(Objects.requireNonNull(loader.getResource("MotherNature.png")))).getImage();

        bridge_images = new Image[4];
        for(int i = 0; i < 4; i++)
        bridge_images[i] = (new ImageIcon(Objects.requireNonNull(loader.getResource("Bridges/Hamtaro_v2 (" + (i + 1) + ").png")))).getImage();

        start_particle = (new ImageIcon(Objects.requireNonNull(loader.getResource("Star.png")))).getImage();
        cloud_particle = (new ImageIcon(Objects.requireNonNull(loader.getResource("Cloud.png")))).getImage();
        dust_particle = (new ImageIcon(Objects.requireNonNull(loader.getResource("Dust.png")))).getImage();

        //button + dialog window in the bottom right used to leave the lobby

        leavelobby_button = new JButton("LEAVE LOBBY");
        leavelobby_button.setFont(new Font("serif", Font.PLAIN, (int) (10 * SCREEN_SCALE)));
        leavelobby_button.addActionListener(this::leavelobbyButtonHandler);
        leavelobby_button.setLocation(SCREEN_WIDTH - SCREEN_WIDTH/9, SCREEN_HEIGHT - SCREEN_HEIGHT/24);
        leavelobby_button.setSize(SCREEN_WIDTH/10, SCREEN_HEIGHT/30);
        this.add(leavelobby_button);
        leavelobby_button.setVisible(true);

        leave_dialog = new JDialog(GUI.frame, "Leaving the lobby...");
        JLabel label = new JLabel("Are you sure you want to leave?");
        label.setHorizontalAlignment(SwingConstants.CENTER);
        JButton yes_button = new JButton("YES");
        yes_button.addActionListener(this::yesLeaveButtonHandler);
        JButton no_button = new JButton("NO");
        no_button.addActionListener(this::noLeaveButtonHandler);

        JPanel dialog_panel = new JPanel();
        dialog_panel.setLayout(new BorderLayout());
        dialog_panel.add(label, BorderLayout.CENTER);
        dialog_panel.setPreferredSize(new Dimension(SCREEN_WIDTH/6, SCREEN_HEIGHT/12));
        JPanel dialog_bottom_panel = new JPanel();
        dialog_bottom_panel.setLayout(new BoxLayout(dialog_bottom_panel, BoxLayout.X_AXIS));
        dialog_bottom_panel.add(yes_button);
        dialog_bottom_panel.add(no_button);
        dialog_panel.add(dialog_bottom_panel, BorderLayout.PAGE_END);
        leave_dialog.add(dialog_panel);
        leave_dialog.pack();
        leave_dialog.setLocation(GUIFrame.WIDTH/2, GUIFrame.HEIGHT/2);
        leave_dialog.setVisible(false);

        //button shown when the game has ended to go back to the lobby

        game_ended_return_to_lobby_button = new JButton("BACK TO LOBBY");
        game_ended_return_to_lobby_button.setFont(new Font("serif", Font.PLAIN, 30));
        game_ended_return_to_lobby_button.addActionListener(this::gameEndedButtonHandler);
        game_ended_return_to_lobby_button.setLocation(SCREEN_WIDTH/2 - SCREEN_WIDTH/6, SCREEN_HEIGHT/2 - SCREEN_HEIGHT/32);
        game_ended_return_to_lobby_button.setSize(SCREEN_WIDTH/3, SCREEN_HEIGHT/16);
        this.add(game_ended_return_to_lobby_button);
        game_ended_return_to_lobby_button.setVisible(false);

        //dialog to confirm or cancel an NPC's activation

        npc_dialog = new JDialog(GUI.frame, "NPC activation!");
        npc_text_area = new JTextArea("");
        npc_text_area.setLineWrap(true);
        npc_text_area.setWrapStyleWord(true);
        JButton confirm_button = new JButton("CONFIRM ACTIVATION");
        confirm_button.addActionListener(this::confirmActivationButtonHandler);
        JButton cancel_button = new JButton("CANCEL ACTIVATION");
        cancel_button.addActionListener(this::cancelActivationButtonHandler);
        npc_dialog.addWindowListener(new WindowAdapter() {
            public void windowClosing(WindowEvent we) {
                cancelActivationButtonHandler(null);
            }
        });

        dialog_panel = new JPanel();
        dialog_panel.setLayout(new BoxLayout(dialog_panel, BoxLayout.Y_AXIS));
        JPanel horizonta_dialog_panel = new JPanel();
        horizonta_dialog_panel.setLayout(new BoxLayout(horizonta_dialog_panel, BoxLayout.X_AXIS));
        dialog_panel.add(npc_text_area);
        horizonta_dialog_panel.add(confirm_button);
        horizonta_dialog_panel.add(cancel_button);
        dialog_panel.add(horizonta_dialog_panel);
        npc_dialog.add(dialog_panel);
        npc_dialog.setAlwaysOnTop(true);
        npc_dialog.setMinimumSize(new Dimension(GUIFrame.WIDTH/3, (int) (GUIFrame.HEIGHT/3.5)));
        npc_dialog.setLocation(GUIFrame.WIDTH/2, GUIFrame.HEIGHT/2);
        npc_dialog.setVisible(false);

        //overlay text on NPCs

        npc_overlay_text_area = new JTextArea("");
        npc_overlay_text_area.setLineWrap(true);
        npc_overlay_text_area.setWrapStyleWord(true);
        npc_overlay_text_area.setVisible(false);
        npc_overlay_text_area.setEditable(false);
        npc_overlay_text_area.setEnabled(false);
        npc_overlay_text_area.getCaret().deinstall(npc_overlay_text_area);
        //pass through of the events on the JTextArea to the GUIGamePanel
        npc_overlay_text_area.addMouseListener( new MouseListener() {
                @Override
                public void mouseClicked(MouseEvent e) {
                    GUI.frame.getGamePanel().dispatchEvent(new MouseEvent((Component) e.getSource(), e.getID(), e.getWhen(), e.getModifiers(),
                            e.getComponent().getLocation().x  + e.getX(), e.getComponent().getLocation().y  + e.getY(),
                            e.getClickCount(), e.isPopupTrigger()));
                }
                @Override
                public void mousePressed(MouseEvent e) {}
                @Override
                public void mouseReleased(MouseEvent e) {}
                @Override
                public void mouseEntered(MouseEvent e) {}
                @Override
                public void mouseExited(MouseEvent e) {}
            }
        );
        //npc_overlay_text_area.setBorder(new LineBorder(Color.blue, (int) (2 * SCREEN_SCALE)));
        npc_overlay_text_area.setFont(new Font("Segoe UI", Font.PLAIN, (int) (12 * SCREEN_SCALE)));
        this.add(npc_overlay_text_area);

        //log

        log = new JTextPane();
        log.setEditable(false);
        log.setFont(GUI.font.deriveFont(14f));

        SimpleAttributeSet attributes = new SimpleAttributeSet();
        StyleConstants.setBold(attributes, true);
        log.setCharacterAttributes(attributes, true);
        log.setBackground(new Color(0, 0, 0, 20));
        log.setText("Game mode, let's go :)\n");
        log.setFocusable(false);
        log_pane = new JScrollPane(log);
        log_pane.setFocusable(false);
        log_pane.setBorder(null);
        log_pane.setEnabled(false);

        log_pane.setSize(new Dimension((int) (250 * SCREEN_SCALE), (int) (50 * SCREEN_SCALE)));
        log_pane.setLocation((SCREEN_WIDTH * 7) / 12, (int) (SCREEN_HEIGHT - ((1454 / 4.1) + 22) * SCREEN_HEIGHT_SCALE));
        this.add(log_pane);

        this.setFocusable(true);
        this.setVisible(true);

        //change the screen resolution here, if needed
        SCREEN_WIDTH = (int) (GUIFrame.BASE_WIDTH*GUIFrame.STARTUP_RESOLUTION_MULTIPLIER);
        SCREEN_HEIGHT = (int) (GUIFrame.BASE_HEIGHT*GUIFrame.STARTUP_RESOLUTION_MULTIPLIER);
        setPreferredSize(new Dimension(SCREEN_WIDTH, SCREEN_HEIGHT));
    }

    /**
     * {@inheritDoc}
     * <br><br>
     * In {@link GUIGamePanel} it gets called every 33 milliseconds, to render the game at 30 FPS.
     *
     * @param g instance of {@link Graphics} in charge ot the repaint
     */
    @Override
    public void paintComponent(Graphics g) {
        Graphics2D g2d = (Graphics2D) g;
        super.paintComponent(g);

        if (!GUI.inGame && !GUI.afterGame)
            return;

        g.setColor(new Color(87, 191, 255));
        g.fillRect(0, 0, SCREEN_WIDTH, SCREEN_HEIGHT);

        g2d.setRenderingHint(RenderingHints.KEY_ANTIALIASING,
                RenderingHints.VALUE_ANTIALIAS_ON);
        g2d.setRenderingHint(RenderingHints.KEY_RENDERING,
                RenderingHints.VALUE_RENDER_QUALITY);

        Font font = GUI.font.deriveFont((float) (22 * SCREEN_SCALE));

        g.setFont(font);
        GameState gs = client.getGameState();

        invisible_npc_overlay_text_area_requests = 0;
        int dashboardWidth = (int) ((1454 / 7) * SCREEN_SCALE);
        int dashboardHeight = (int) ((3352 / 7) * SCREEN_SCALE);
        int dashboardL_X = 0;
        int dashboardL_Y =  (int)(25 * SCREEN_HEIGHT_SCALE);
        int dashboardR_X = SCREEN_WIDTH - dashboardWidth;
        int dashboardR_Y = (int)(25 * SCREEN_HEIGHT_SCALE);

        if(GUI.inGame) {

            //draws second player, on the left
            g.drawImage(opponent_dashboard, dashboardL_X, dashboardL_Y, dashboardWidth, dashboardHeight, null);

            //draws third player, on the right
            if(gs.otherPlayers(client).size() > 1)
                g.drawImage(opponent_dashboard, dashboardR_X, dashboardR_Y, dashboardWidth, dashboardHeight, null);

            //draw coins
            for (int i = 0; i < gs.myPlayer(client).getCoins(); i++) {
                g.drawImage(coin_image,
                        (int) ((SCREEN_WIDTH/2 + (- 3352/9 - 1164 / 15) * GUIGamePanel.SCREEN_WIDTH_SCALE) + (399 / 15 + 10) * (i % 2) * GUIGamePanel.SCREEN_SCALE),
                        (int) (SCREEN_HEIGHT - ((1710 / 15) + (399 / 19) * (2 + i / 2)) * GUIGamePanel.SCREEN_HEIGHT_SCALE),
                        (int) (399 / 10 * GUIGamePanel.SCREEN_SCALE),
                        (int) (399 / 10 * GUIGamePanel.SCREEN_SCALE), null);
            }
            for (int i = 0; i < gs.otherPlayers(client).get(0).getCoins(); i++) {
                g.drawImage(coin_image,
                        (int) (dashboardL_X + (1710 / 15 + (399 / 15 + 10) * (i % 2)) * GUIGamePanel.SCREEN_SCALE),
                        (int) (dashboardHeight + dashboardL_Y + ((399 / 19) * (i / 2)) * GUIGamePanel.SCREEN_SCALE),
                        (int) (399 / 10 * GUIGamePanel.SCREEN_SCALE),
                        (int) (399 / 10 * GUIGamePanel.SCREEN_SCALE), null);
            }
            if (gs.otherPlayers(client).size() > 1) {
                for (int i = 0; i < gs.otherPlayers(client).get(1).getCoins(); i++) {
                    g.drawImage(coin_image,
                            (int) (SCREEN_WIDTH - (1710 / 15 + 399 / 10 + (399 / 15 + 10) * (i % 2)) * GUIGamePanel.SCREEN_SCALE),
                            (int) (dashboardHeight + (dashboardL_Y) + ((399 / 19) * (i / 2)) * GUIGamePanel.SCREEN_SCALE),
                            (int) (399 / 10 * GUIGamePanel.SCREEN_SCALE),
                            (int) (399 / 10 * GUIGamePanel.SCREEN_SCALE), null);
                }
            }

            //draw unclaimed professors
            for (int i = 0; i < gs.getUnclaimedProfessors().size(); i++){
                g.drawImage(professor_images.get(gs.getUnclaimedProfessors().get(i)),
                        (int) (SCREEN_WIDTH - (40 + 25*i) * GUIGamePanel.SCREEN_SCALE),
                        (int) (SCREEN_HEIGHT - 70 * GUIGamePanel.SCREEN_SCALE),
                        (int) (20 * GUIGamePanel.SCREEN_SCALE),
                        (int) (20 * GUIGamePanel.SCREEN_SCALE), null);
            }

            //draw unused coins
            if(client.getLobby().expert_mode) {
                g.setColor(Color.black);
                g.drawString("" + gs.getBank(), (int)(dashboardR_X + 15 * GUIGamePanel.SCREEN_WIDTH_SCALE),
                        (int) (dashboardR_Y + dashboardHeight + 120 * GUIGamePanel.SCREEN_HEIGHT_SCALE));

                for (int i = 0; (gs.getBank() > 5 ? i < 5 : i < gs.getBank()); i++) {
                    g.drawImage(coin_image,
                            (int) (dashboardR_X + 10 * GUIGamePanel.SCREEN_WIDTH_SCALE),
                            (int) (dashboardR_Y + dashboardHeight + (125 + 5*i) * GUIGamePanel.SCREEN_HEIGHT_SCALE),
                            (int) (399 / 12 * GUIGamePanel.SCREEN_SCALE),
                            (int) (399 / 12 * GUIGamePanel.SCREEN_SCALE), null);
                }
            }

            //draw unused students
            for (int i = 0; i < 5; i++) {
                g.drawImage(GUIGamePanel.student_images.get(Colors.fromColorIndex(i)),
                        (int) (SCREEN_WIDTH - (40 + 10*i) * GUIGamePanel.SCREEN_SCALE),
                        (int) (dashboardR_Y + dashboardHeight + 125 * GUIGamePanel.SCREEN_HEIGHT_SCALE),
                        (int) (20 * GUIGamePanel.SCREEN_SCALE),
                        (int) (20 * GUIGamePanel.SCREEN_SCALE), null);
            }

            //draw number of unused students
            g.setColor(Color.black);
            g.drawString( "" + gs.getRemainingStudentsNum(),
                    (int) (SCREEN_WIDTH - 40 * GUIGamePanel.SCREEN_SCALE),
                    (int) (dashboardR_Y + dashboardHeight + 120 * GUIGamePanel.SCREEN_HEIGHT_SCALE));

            //draw everything else on the "table"
            for (GUIElement element : elements)
                element.show(g);

            //draw bridges
            for (int i = 0; i < islands.size(); i++) {
                int first_actual_index;
                int islands_total = 0;
                if (gs.getIslands().get(0).index > i) {
                    first_actual_index = gs.getIslands().size() - 1;
                } else {
                    for (first_actual_index = 0; first_actual_index < gs.getIslands().size(); first_actual_index++) {
                        islands_total += gs.getIslands().get(first_actual_index).getNumOfMergedIslands();
                        if (islands_total >= i + 1 - gs.getIslands().get(0).index) {
                            break;
                        }
                    }
                }

                int second_actual_index;
                islands_total = 0;
                if (gs.getIslands().get(0).index > (i + 1) % islands.size()) {
                    second_actual_index = gs.getIslands().size() - 1;
                } else {
                    for (second_actual_index = 0; second_actual_index < gs.getIslands().size(); second_actual_index++) {
                        islands_total += gs.getIslands().get(second_actual_index).getNumOfMergedIslands();
                        if (islands_total >= (i + 1) % islands.size() + 1 - gs.getIslands().get(0).index) {
                            break;
                        }
                    }
                }

                if (first_actual_index == second_actual_index) {
                    switch (i) {
                        case 0 -> g.drawImage(bridge_images[1],
                                (int) ((islands.get(0).x) * SCREEN_WIDTH_SCALE + (islands.get(0).width - 20) * SCREEN_SCALE),
                                (int) ((islands.get(1).y) * SCREEN_HEIGHT_SCALE + (islands.get(1).height / 6) * SCREEN_SCALE),
                                (int) ((islands.get(1).x) * SCREEN_WIDTH_SCALE - ((islands.get(0).x) * SCREEN_WIDTH_SCALE + (islands.get(0).width - 40) * SCREEN_SCALE)),
                                (int) ((islands.get(0).y) * SCREEN_HEIGHT_SCALE + (islands.get(0).height) * SCREEN_SCALE - ((islands.get(1).y) * SCREEN_HEIGHT_SCALE + (islands.get(1).height / 1.5) * SCREEN_SCALE)), null);
                        case 1, 2, 3 -> g.drawImage(bridge_images[0],
                                (int) ((islands.get(i).x) * SCREEN_WIDTH_SCALE + (islands.get(i).width - 18) * SCREEN_SCALE),
                                (int) ((islands.get(i).y) * SCREEN_HEIGHT_SCALE + (islands.get(i).height / 5) * SCREEN_SCALE),
                                (int) ((islands.get(i + 1).x) * SCREEN_WIDTH_SCALE + (36) * SCREEN_SCALE - ((islands.get(i).x) * SCREEN_WIDTH_SCALE + (islands.get(i).width) * SCREEN_SCALE)),
                                (int) ((islands.get(i).height / 2.5) * SCREEN_SCALE), null);
                        case 4 -> g.drawImage(bridge_images[2],
                                (int) ((islands.get(4).x) * SCREEN_WIDTH_SCALE + (islands.get(4).width - 20) * SCREEN_SCALE),
                                (int) ((islands.get(4).y) * SCREEN_HEIGHT_SCALE + (islands.get(4).height / 6) * SCREEN_SCALE),
                                (int) ((islands.get(5).x) * SCREEN_WIDTH_SCALE - ((islands.get(4).x) * SCREEN_WIDTH_SCALE + (islands.get(4).width - 40) * SCREEN_SCALE)),
                                (int) ((islands.get(5).y) * SCREEN_HEIGHT_SCALE + (islands.get(5).height) * SCREEN_SCALE - ((islands.get(4).y) * SCREEN_HEIGHT_SCALE + (islands.get(4).height / 1.5) * SCREEN_SCALE)), null);
                       case 5 -> g.drawImage(bridge_images[3],
                                (int) ((islands.get(5).x) * SCREEN_WIDTH_SCALE + (islands.get(5).width/3) * SCREEN_SCALE),
                                (int) ((islands.get(5).y) * SCREEN_HEIGHT_SCALE + (islands.get(5).height - 32) * SCREEN_SCALE),
                                (int) ((islands.get(5).width/3) * SCREEN_SCALE),
                                (int) ((islands.get(6).y) * SCREEN_HEIGHT_SCALE + (42) * SCREEN_SCALE - ((islands.get(5).y) * SCREEN_HEIGHT_SCALE + (islands.get(5).height) * SCREEN_SCALE)), null);
                        case 6 -> g.drawImage(bridge_images[1],
                                (int) ((islands.get(7).x) * SCREEN_WIDTH_SCALE + (islands.get(7).width - 20) * SCREEN_SCALE),
                                (int) ((islands.get(6).y) * SCREEN_HEIGHT_SCALE + (islands.get(6).height / 6) * SCREEN_SCALE),
                                (int) ((islands.get(6).x) * SCREEN_WIDTH_SCALE - ((islands.get(7).x) * SCREEN_WIDTH_SCALE + (islands.get(7).width - 40) * SCREEN_SCALE)),
                                (int) ((islands.get(7).y) * SCREEN_HEIGHT_SCALE + (islands.get(7).height) * SCREEN_SCALE - ((islands.get(6).y) * SCREEN_HEIGHT_SCALE + (islands.get(6).height / 1.5) * SCREEN_SCALE)), null);
                        case 7, 8, 9 -> g.drawImage(bridge_images[0],
                                (int) ((islands.get(i + 1).x) * SCREEN_WIDTH_SCALE + (islands.get(i + 1).width - 18) * SCREEN_SCALE),
                                (int) ((islands.get(i + 1).y) * SCREEN_HEIGHT_SCALE + (islands.get(i + 1).height / 5) * SCREEN_SCALE),
                                (int) ((islands.get(i).x) * SCREEN_WIDTH_SCALE + (36) * SCREEN_SCALE - ((islands.get(i + 1).x) * SCREEN_WIDTH_SCALE + (islands.get(i + 1).width) * SCREEN_SCALE)),
                                (int) ((islands.get(i + 1).height / 2.5) * SCREEN_SCALE), null);
                        case 10 -> g.drawImage(bridge_images[2], //!!!!!!
                                (int) ((islands.get(11).x) * SCREEN_WIDTH_SCALE + (islands.get(11).width - 20) * SCREEN_SCALE),
                                (int) ((islands.get(11).y) * SCREEN_HEIGHT_SCALE + (islands.get(11).height / 6) * SCREEN_SCALE),
                                (int) ((islands.get(10).x) * SCREEN_WIDTH_SCALE - ((islands.get(11).x) * SCREEN_WIDTH_SCALE + (islands.get(11).width - 40) * SCREEN_SCALE)),
                                (int) ((islands.get(10).y) * SCREEN_HEIGHT_SCALE + (islands.get(10).height) * SCREEN_SCALE - ((islands.get(11).y) * SCREEN_HEIGHT_SCALE + (islands.get(11).height / 1.5) * SCREEN_SCALE)), null);
                        case 11 -> g.drawImage(bridge_images[3],
                                (int) ((islands.get(0).x) * SCREEN_WIDTH_SCALE + (islands.get(0).width/3) * SCREEN_SCALE),
                                (int) ((islands.get(0).y) * SCREEN_HEIGHT_SCALE + (islands.get(0).height - 32) * SCREEN_SCALE),
                                (int) ((islands.get(0).width/3) * SCREEN_SCALE),
                                (int) ((islands.get(11).y) * SCREEN_HEIGHT_SCALE + (42) * SCREEN_SCALE - ((islands.get(0).y) * SCREEN_HEIGHT_SCALE + (islands.get(0).height) * SCREEN_SCALE)), null);
                    }
                }
            }


            //draw everything else on the islands
            for (GUIIsland island : islands)
                island.showDataOnTop(g);

            //draws last played card
            if (gs.myPlayer(client).getLastCardPlayed() != null && (compact_cards || gs.isLastGameTurn()) && (gs.getPhase() == 1 ||
                    (gs.getPhase() == 0 && gs.currentPlayersTurnOrder().indexOf(client.getClientID()) < gs.getPlayerTurn())))
                g.drawImage(card_images[gs.myPlayer(client).getLastCardPlayed().order_value - 1],
                        (int) (SCREEN_WIDTH/2 + (- 3352/9 - 1164 / 15) * SCREEN_WIDTH_SCALE), (int) (SCREEN_HEIGHT - (1710 / 15) * SCREEN_HEIGHT_SCALE),
                        (int) (1164 / 15 * SCREEN_SCALE), (int) (1710 / 15 * SCREEN_SCALE), null);

            if (gs.otherPlayers(client).get(0).getLastCardPlayed() != null && (gs.getPhase() == 1 ||
                    (gs.getPhase() == 0 && gs.currentPlayersTurnOrder().indexOf((gs.otherPlayers(client).get(0).clientID)) < gs.getPlayerTurn())))
                g.drawImage(rotated_left_card_images[gs.otherPlayers(client).get(0).getLastCardPlayed().order_value - 1],
                        (dashboardL_X), (dashboardHeight + (dashboardL_Y)),
                        (int) (1710 / 15 * SCREEN_SCALE), (int) (1164 / 15 * SCREEN_SCALE), null);

            if (gs.otherPlayers(client).size() > 1) {
                if (gs.otherPlayers(client).get(1).getLastCardPlayed() != null && (gs.getPhase() == 1 ||
                        (gs.getPhase() == 0 && gs.currentPlayersTurnOrder().indexOf(Integer.valueOf(gs.otherPlayers(client).get(1).clientID)) < gs.getPlayerTurn()))) {
                    g.drawImage(rotated_right_card_images[gs.otherPlayers(client).get(1).getLastCardPlayed().order_value - 1],
                            (int) (SCREEN_WIDTH - 1710 / 15 * SCREEN_SCALE), (dashboardHeight + (dashboardR_Y)),
                            (int) (1710 / 15 * SCREEN_SCALE), (int) (1164 / 15 * SCREEN_SCALE), null);
                }
            }

            int[] mapping = new int[]{2, 3, 0, 4, 1};

            //draw students in the entrance other players
            for (int j = 0; j < gs.otherPlayers(client).size(); j++) {
                for (int i = 0; i < gs.otherPlayers(client).get(j).getDashboard().getEntrance().size(); i++) {
                    g.drawImage(student_images.get(gs.otherPlayers(client).get(j).getDashboard().getEntrance().get(i)),
                            (int) ((j == 0 ? dashboardL_X : dashboardR_X) + ((30 * ((i % 5) + 1)) * GUIGamePanel.SCREEN_SCALE)),
                            (int) ((j == 0 ? dashboardL_Y : dashboardR_Y) + dashboardHeight + (-60 + 28 * (i / 5)) * GUIGamePanel.SCREEN_SCALE),
                            (int) (30 / 7 * 4.5 * 1.2 * GUIGamePanel.SCREEN_SCALE), (int) (30 / 7 * 4.5 * 1.2 * GUIGamePanel.SCREEN_SCALE), null);
                }
            }

            //draw students in the hall other players
            for (int j = 0; j < gs.otherPlayers(client).size(); j++) {
                for (int i = 0; i < 5; i++) {
                    for (int k = 0; k < gs.otherPlayers(client).get(j).getDashboard().getHallRow(mapping[i]); k++) {
                        g.drawImage(student_images.get(Colors.fromColorIndex(mapping[i])),
                                (int) ((j == 0 ? dashboardL_X + 24 * GUIGamePanel.SCREEN_SCALE : dashboardR_X + 23 * GUIGamePanel.SCREEN_SCALE) + (34 * i) * GUIGamePanel.SCREEN_SCALE),
                                (int) ((j == 0 ? dashboardL_Y : dashboardR_Y) + (368 - 22 * k) * GUIGamePanel.SCREEN_SCALE),
                                (int) (30 / 7 * 4.5 * 1.2 * GUIGamePanel.SCREEN_SCALE),
                                (int) (30 / 7 * 4.5 * 1.2 * GUIGamePanel.SCREEN_SCALE), null);
                    }
                }
            }

            //draw professor other players
            for (int j = 0; j < gs.otherPlayers(client).size(); j++) {
                for (int i = 0; i < 5; i++) {
                    if (gs.otherPlayers(client).get(j).getDashboard().getProfessor(mapping[i])) {
                        g.drawImage(professor_images.get(Colors.fromColorIndex(mapping[i])),
                                (int) ((j == 0 ? dashboardL_X + 25 * GUIGamePanel.SCREEN_SCALE : dashboardR_X + 23 * GUIGamePanel.SCREEN_SCALE) + (33.5 * i) * GUIGamePanel.SCREEN_SCALE),
                                (int) ((j == 0 ? dashboardL_Y : dashboardR_Y) + 115 * GUIGamePanel.SCREEN_SCALE),
                                (int) (30 / 7 * 4.5 * 1.2 * GUIGamePanel.SCREEN_SCALE),
                                (int) (30 / 7 * 4.5 * 1.2 * GUIGamePanel.SCREEN_SCALE), null);
                    }
                }
            }

            //draw other players rooks
            int k = 0;
            for (int i = 0; i < gs.getPlayers().size(); i++) {
                if (gs.getPlayers().get(i) != gs.myPlayer(client)) {
                    for (int j = 0; j < gs.otherPlayers(client).get(k).getDashboard().getRooks(); j++) {
                        g.drawImage(rook_images[i],
                                (int) ((k == 0 ? dashboardL_X : dashboardR_X) + (55 + 25 * (j % 4)) * GUIGamePanel.SCREEN_SCALE),
                                (int) ((k == 0 ? dashboardL_Y : dashboardR_Y) + (25 + 40 * ((j / 4) % 2)) * GUIGamePanel.SCREEN_SCALE),
                                (int) (52 / 4 * GUIGamePanel.SCREEN_SCALE),
                                (int) (99 / 4 * GUIGamePanel.SCREEN_SCALE), null);
                    }
                    k++;
                }
            }

            //draw particles for other players' moves
            if(previous_game_phase == 1 && previous_playing_player_id != client.getClientID()) {
                //star on dashboard
                if(previous_game_step == 0 && previous_game_moved_students != gs.getMovedStudents() && client.getLastMoveData().size() == 1) {
                    if(previous_playing_player_id == gs.otherPlayers(client).get(0).clientID)
                        addParticlesGrayTriad((1454 / 7) / 2,
                                25 + (3352 / 7) / 2);
                    else
                        addParticlesGrayTriad(GUIFrame.BASE_WIDTH - (1454 / 7) / 2,
                                25 + (3352 / 7) / 2);
                }
                //star on island
                if(previous_game_step == 0 && previous_game_moved_students != gs.getMovedStudents() && client.getLastMoveData().size() == 2) {
                    addParticlesGrayTriad(islands.get(gs.getIslands().get(client.getLastMoveData().get(1)).index).x + islands.get(gs.getIslands().get(client.getLastMoveData().get(1)).index).width / 2,
                            islands.get(gs.getIslands().get(client.getLastMoveData().get(1)).index).y + islands.get(gs.getIslands().get(client.getLastMoveData().get(1)).index).height / 2);
                }
                //star on mother nature
                if(previous_game_step == 1 && gs.getStep() == 2) {
                    addParticlesGrayTriad(islands.get(gs.getIslands().get(gs.getMotherNature()).index).x + islands.get(gs.getIslands().get(gs.getMotherNature()).index).width / 2,
                            islands.get(gs.getIslands().get(gs.getMotherNature()).index).y + islands.get(gs.getIslands().get(gs.getMotherNature()).index).height / 2);
                }
                //star on cloud
                if(previous_game_step == 2 && (gs.getPhase() == 0 || gs.getStep() == 0) && client.getLastMoveData().size() == 1 && client.getLastMoveData().get(0) < clouds.size()) {
                    addParticlesGrayTriad(clouds.get(client.getLastMoveData().get(0)).x + clouds.get(client.getLastMoveData().get(0)).width / 2,
                            clouds.get(client.getLastMoveData().get(0)).y + clouds.get(client.getLastMoveData().get(0)).height / 2);
                }
            }

            //update memory of the current game state
            previous_playing_player_id = gs.currentlyPlayingPlayer();
            previous_game_phase = gs.getPhase();
            previous_game_step = gs.getStep();
            previous_game_moved_students = gs.getMovedStudents();

            //draw winner banner
            if(winnerId != 0) {
                //draw text with outline
                Shape textShape = (GUI.font.deriveFont((float) (60 * SCREEN_SCALE))).createGlyphVector(g2d.getFontRenderContext(),
                        "Game ended, winner: " + client.clientIDToNickname(winnerId)).getOutline();
                textShape = (AffineTransform.getTranslateInstance((double) SCREEN_WIDTH / 5, (double) SCREEN_HEIGHT / 3)).createTransformedShape(textShape);

                g2d.setColor(Color.black);
                g2d.setStroke(new BasicStroke((float) (4 * GUIGamePanel.SCREEN_SCALE)));
                g2d.draw(textShape);

                if (winnerId == client.getClientID()) {
                    Random ran = new Random();

                    particles.add(new GUIParticle(start_particle, ran.nextInt(PARTICLE_SIZE, GUIFrame.BASE_WIDTH - PARTICLE_SIZE),
                            ran.nextInt(PARTICLE_SIZE, GUIFrame.BASE_HEIGHT - PARTICLE_SIZE), PARTICLE_SIZE, PARTICLE_SIZE, 0.96f, true));
                    g2d.setColor(Color.green);
                } else {
                    g2d.setColor(Color.red);
                }
                g2d.fill(textShape);

                g2d.translate(0, 0);
            }

            //draw npcs description overlay
            if(client.getLobby().expert_mode)
                for(GUINpc npc : npcs)
                    npc.showOverlay(g);

            //handle particles
            for(GUIParticle particle : particles) {
                particle.show(g);
                particle.update();
            }
            for(int i = 0; i < particles.size(); ) {
                if(particles.get(i).toDelete())
                    particles.remove(i);
                else
                    i++;
            }

            //if none of the NPCs has the mouse on it, set the overlay panel invisible
            if(invisible_npc_overlay_text_area_requests >= 3 || activated_npc != -1) {
                npc_overlay_text_area.setVisible(false);
            }

            //draw players connection status
            List<Integer> notMyIndex = new ArrayList<Integer>();
            for(int i = 0; i < client.getLobby().clients.size(); i++)
                if(client.getLobby().clients.get(i).clientID != client.getClientID())
                    notMyIndex.add(i);

            if(client.getReconnection())
                g2d.setColor(Color.red);
            else
                g2d.setColor(Color.green);
            g2d.fillOval(SCREEN_WIDTH/3, (int) (SCREEN_HEIGHT - (1454 / 4.3) * SCREEN_HEIGHT_SCALE), (int) (10*SCREEN_SCALE), (int) (10*SCREEN_SCALE));

            if(notMyIndex.size() > 0) {
                if (client.getReadyFlags()[notMyIndex.get(0)])
                    g2d.setColor(Color.green);
                else
                    g2d.setColor(Color.red);
                g2d.fillOval((int) (10 * SCREEN_WIDTH_SCALE), (int) (10 * SCREEN_HEIGHT_SCALE), (int) (10 * SCREEN_SCALE), (int) (10 * SCREEN_SCALE));
            }

            if(notMyIndex.size() > 1) {
                if (client.getReadyFlags()[notMyIndex.get(1)])
                    g2d.setColor(Color.green);
                else
                    g2d.setColor(Color.red);
                g2d.fillOval((int) (SCREEN_WIDTH - 20 * SCREEN_WIDTH_SCALE), (int) (10 * SCREEN_HEIGHT_SCALE), (int) (10 * SCREEN_SCALE), (int) (10 * SCREEN_SCALE));
            }

            //draw last game turn red text
            if(gs.isLastGameTurn()) {
                g.setColor(Color.red);
                g.drawString("!! LAST GAME TURN !!", ((int) (SCREEN_WIDTH * 3.2)) / 8, (int) (55 * SCREEN_HEIGHT_SCALE));
            }

            //draw texts
            texts.get(0).setText(client.getNickname());
            texts.get(1).setText(client.clientIDToNickname(gs.otherPlayers(client).get(0).clientID));
            if (gs.otherPlayers(client).size() > 1) texts.get(2).setText(client.clientIDToNickname(gs.otherPlayers(client).get(1).clientID));
            texts.get(3).setText("Next player: " + client.clientIDToNickname(gs.currentPlayersTurnOrder().get((gs.getPlayerTurn() + 1) % gs.getPlayers().size())));

            String state;
            if (gs.isMyTurn(client.getClientID())) {
                if (gs.getPhase() == 0) state = "Play a card";
                else {
                    switch (gs.getStep()) {
                        case 0 -> state = "Set students, " + ((gs.getPlayers().size() == 2 ? 3 : 4) - gs.getMovedStudents()) + " left";
                        case 1 -> state = "Move mother nature";
                        case 2 -> state = "Choose a cloud";
                        default -> state = "";
                    }
                }
                texts.get(4).setText("Your turn! " + state);
                texts.get(5).setText("Your turn! " + state);

            } else {
                if (gs.getPhase() == 0) {
                    state = " is choosing the card";
                }
                else {
                    state = switch (gs.getStep()) {
                        case 0 -> " is moving students";
                        case 1 -> " is moving mother nature";
                        default -> " is choosing the cloud";
                    };
                }
                texts.get(4).setText("" + client.clientIDToNickname(gs.currentlyPlayingPlayer()) + state);
                texts.get(5).setText("" + client.clientIDToNickname(gs.currentlyPlayingPlayer()) + state);
            }

            texts.get(0).show(g);
            texts.get(1).show(g);
            if (gs.otherPlayers(client).size() > 1)
                texts.get(2).show(g);
            if (gs.otherPlayers(client).size() > 1 && !(gs.getPhase() == 0 && gs.getPlayerTurn() == gs.getPlayers().size() - 1))
                texts.get(3).show(g);
            if (gs.otherPlayers(client).size() > 1)
                texts.get(4).show(g);
            else
                texts.get(5).show(g);
        }
    }

    //triggers at every tick of the timer

    /**
     * {@inheritDoc}
     * <br><br>
     * Mainly invoked by the {@link Timer} that dictates a repaint every 33 milliseconds.
     * @param e event describing the action
     */
    @Override
    public void actionPerformed(ActionEvent e) {
        repaint();
    }

    //HANDLERS

    /**
     * Handler for the "leave lobby" button which prompts the user with the option to leave its current lobby.
     *
     * @param evt click event triggering the handler
     */
    private void leavelobbyButtonHandler(ActionEvent evt) {
        //setVisible is blocking for JDialogs!!!
        leave_dialog.setLocationRelativeTo(GUI.frame);
        SwingUtilities.invokeLater(() -> leave_dialog.setVisible(true));
    }

    /**
     * Handler for the "yes" button which confirms the user's will to leave its current lobby, consequently doing it and forfeiting the game.
     *
     * @param evt click event triggering the handler
     */
    private void yesLeaveButtonHandler(ActionEvent evt) {
        leave_dialog.setVisible(false);
        client.leaveLobby();
    }

    /**
     * Handler for the "no" button which denies the user's will to leave its current lobby, hiding the prompt asking if he wanted to do so.
     *
     * @param evt click event triggering the handler
     */
    private void noLeaveButtonHandler(ActionEvent evt) {
        leave_dialog.setVisible(false);
    }

    /**
     * Handler for the "confirm activation" button on an Npc's activation dialog, it reads the {@link EffectParameters} for said Npc from the user choices and
     * proceeds to try the activation.
     *
     * @param evt click event triggering the handler
     */
    private void confirmActivationButtonHandler(ActionEvent evt) {
        switch(client.getGameState().getNpcs()[activated_npc].getId()) {
            case 2, 4, 6, 8 -> client.activateEffect(activated_npc, new EffectParameters());
            case 1 -> {
                List<Integer> args = new ArrayList<>(selected_npc_students);
                args.add(selected_npc_island);
                client.activateEffect(activated_npc, new EffectParameters(args));
            }
            case 3, 5 -> client.activateEffect(activated_npc, new EffectParameters(selected_npc_island));
            case 7 -> {
                List<Integer> args = new ArrayList<>();
                args.add(selected_npc_students.size());
                args.addAll(selected_npc_students);
                args.addAll(selected_npc_entrance_students);
                client.activateEffect(activated_npc, new EffectParameters(args));
            }
            case 9, 12 -> client.activateEffect(activated_npc, new EffectParameters(selected_dashboard_colors));
            case 10 -> {
                List<Integer> args = new ArrayList<>(selected_npc_entrance_students);
                args.addAll(selected_dashboard_colors);
                client.activateEffect(activated_npc, new EffectParameters(args));
            }
            case 11 -> client.activateEffect(activated_npc, new EffectParameters(selected_npc_students));
        }
        //reset everything
        activated_npc = -1;
        selected_npc_students.clear();
        selected_dashboard_colors.clear();
        selected_npc_island = -1;
        selected_npc_entrance_students.clear();
        npc_dialog.setVisible(false);
    }

    /**
     * Handler for the "cancel activation" button on an Npc's activation dialog, it hides the activation prompt, canceling it.
     *
     * @param evt click event triggering the handler
     */
    private void cancelActivationButtonHandler(ActionEvent evt) {
        activated_npc = -1;
        selected_npc_students.clear();
        selected_dashboard_colors.clear();
        selected_npc_island = -1;
        selected_npc_entrance_students.clear();
        npc_dialog.setVisible(false);
    }

    /**
     * Handler for the "return to lobby" button displayed after a game, during the {@link GUI#afterGame} state.<br>
     * When pressed hides the {@link GUIGamePanel} and brings the user back to the {@link GUILobbyPanel}.
     *
     * @param evt click event triggering the handler
     */
    private void gameEndedButtonHandler(ActionEvent evt) {
        game_ended_return_to_lobby_button.setVisible(false);
        GUI.afterGame = false;
        GUI.inGame = false;
        this.winnerId = 0;
        GUI.getGUI().refresh();
    }

    //GENERIC METHODS

    /**
     * Method which re-calculates {@link GUIGamePanel#SCREEN_WIDTH_SCALE}, {@link GUIGamePanel#SCREEN_HEIGHT_SCALE} and {@link GUIGamePanel#SCREEN_SCALE}
     * according to the ratio between {@link GUIFrame#BASE_WIDTH} and {@link GUIFrame#BASE_HEIGHT} and the new width and height.
     *
     * @param new_width new component width
     * @param new_height new component height
     */
    public static void resized(int new_width, int new_height) {
        SCREEN_WIDTH = new_width;
        SCREEN_HEIGHT = new_height;
        SCREEN_WIDTH_SCALE = ((float) new_width) / ((float) GUIFrame.BASE_WIDTH);
        SCREEN_HEIGHT_SCALE = ((float) new_height) / ((float) GUIFrame.BASE_HEIGHT);
        SCREEN_SCALE = Math.min(SCREEN_WIDTH_SCALE, SCREEN_HEIGHT_SCALE);
    }

    /**
     * Method which signals this component that a resize has happened, consequently adjusting the font size and location of the elements inside this panel.<br>
     */
    public void updateResized() {
        leavelobby_button.setLocation(SCREEN_WIDTH - SCREEN_WIDTH/9, SCREEN_HEIGHT - SCREEN_HEIGHT/24);
        leavelobby_button.setSize(SCREEN_WIDTH/10, SCREEN_HEIGHT/30);
        leavelobby_button.setFont(new Font("serif", Font.PLAIN, (int) (10 * SCREEN_SCALE)));
        npc_overlay_text_area.setFont(new Font("Segoe UI", Font.PLAIN, (int) (12 * SCREEN_SCALE)));
        game_ended_return_to_lobby_button.setFont(new Font("serif", Font.PLAIN, (int) (30 * SCREEN_SCALE)));
        game_ended_return_to_lobby_button.setLocation(SCREEN_WIDTH/2 - SCREEN_WIDTH/6, SCREEN_HEIGHT/2 - SCREEN_HEIGHT/32);
        game_ended_return_to_lobby_button.setSize(SCREEN_WIDTH/3, SCREEN_HEIGHT/16);
        //npc_overlay_text_area.setBorder(new LineBorder(Color.black, (int) (2 * SCREEN_SCALE)));

        log.setFont(GUI.font.deriveFont((float) (14 * SCREEN_SCALE)));
        log_pane.setSize(new Dimension((int) (250 * SCREEN_SCALE), (int) (50 * SCREEN_SCALE)));
        log_pane.setLocation((SCREEN_WIDTH * 7) / 12, (int) (SCREEN_HEIGHT - ((1454 / 4.1) + 22) * SCREEN_HEIGHT_SCALE));
    }

    /**
     * Signals the panel that a game has ended, showing the winner's nickname and the button to go back to the lobby.
     *
     * @param winnerId if of the client who won
     */
    public void showGameEnd(int winnerId) {
        this.winnerId = winnerId;
        game_ended_return_to_lobby_button.setVisible(true);
    }

    /**
     * Makes the panel reset its state after it showed that a player won.
     */
    public void hideGameEnd() {
        particles.clear();
        game_ended_return_to_lobby_button.setVisible(false);
        this.winnerId = 0;
    }

    /**
     * Handler the beginning of an Npc's activation, showing the dialog that guides the user to activate it, or cancel the activation.
     *
     * @param index index of the activated Npc with respect of {@link GameState#getNpcs}
     */
    public void activateNpc(int index) {
        activated_npc = index;
        npc_text_area.setText(getGUINpcString(index));
        npc_dialog.setLocationRelativeTo(GUI.frame);
        //setVisible is blocking for JDialogs!!!
        SwingUtilities.invokeLater(() -> npc_dialog.setVisible(true));
    }

    /**
     * Method used by the {@link GUINpc GUINpcs} to signal that they don't need the prompt with their description.<br>
     * If all 3 Npcs don't need it, then it gets hidden.
     */
    public void requestInvisibleNpcOverlayTextArea() {
        invisible_npc_overlay_text_area_requests += 1;
    }

    /**
     * Inserts the provided particle in the structures that handle its drawing and deletion when needed.
     *
     * @param particles particle to add
     */
    public void addParticles(List<GUIParticle> particles) {
        this.particles.addAll(particles);
    }

    /**
     * Generates 3 particles that propagate with trajectories 120 degrees apart at the provided coordinates and adds them to the structures to handle the particles.<br>
     * Those particles have a gray star as the image, and are intended to be used to signal other player's moves.
     *
     * @param x initial x for the 3 particles
     * @param y initial y for the 3 particles
     */
    public void addParticlesGrayTriad(int x, int y) {
        particles.add(new GUIParticle(GrayFilter.createDisabledImage(start_particle), x, y,
                PARTICLE_SIZE, PARTICLE_SIZE, 0, 8, 0, 0.2f, 0.96f, false));
        particles.add(new GUIParticle(GrayFilter.createDisabledImage(start_particle), x, y,
                PARTICLE_SIZE, PARTICLE_SIZE, (int) (Math.cos(Math.PI / 6) * 8), - (int) (Math.sin(Math.PI / 6) * 8), 0, 0.2f, 0.96f, false));
        particles.add(new GUIParticle(GrayFilter.createDisabledImage(start_particle), x, y,
                PARTICLE_SIZE, PARTICLE_SIZE, - (int) (Math.cos(Math.PI / 6) * 8), - (int) (Math.sin(Math.PI / 6) * 8), 0, 0.2f, 0.96f, false));
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

    //GETTERS AND SETTERS

    /**
     * Provides the index of the currently selected entrance student, for when the {@link Game} is in phase 1, step 0.
     *
     * @return selected student ready to be moved
     */
    public int getSelectedStudentIndex() {
        return selected_student_index;
    }

    /**
     * Sets the index of the currently selected entrance student, for when the {@link Game} is in phase 1, step 0.
     *
     * @param selected_student_index newly selected student index in the player's dashboard's entrance
     */
    public void setSelectedStudentIndex(int selected_student_index) {
        this.selected_student_index = selected_student_index;
    }

    /**
     * Provides the flag indicating if the player's cards are to be shown compacted or opened.
     *
     * @return true if the cards are to be shown compacted
     */
    public boolean getCompactCards() {
        return compact_cards;
    }

    /**
     * Sets the flag indicating if the player's cards are to be shown compacted or opened.
     *
     * @param compact_cards new value for the flag
     */
    public void setCompactCards(boolean compact_cards) {
        this.compact_cards = compact_cards;
    }

    //returns the index of the currently activating npc
    /**
     * Provides the index of the currently activating npc, the one the current Npc activation dialog is relative to.
     *
     * @return index of the currently activating Npc
     */
    public int getActivatedNpc() {
        return activated_npc;
    }

    //returns the id of the currently activating npc
    /**
     * Provides the id of the currently activating npc, the one the current Npc activation dialog is relative to.
     *
     * @return id of the currently activating Npc
     */
    public int getActivatedNpcId() {
        if(activated_npc == -1)
            return -1;
        return client.getGameState().getNpcs()[activated_npc].getId();
    }

    /**
     * Given an Npc index relative to {@link GameState#getNpcs}, provides the string with its description.
     *
     * @param index index of the Npc requested, relative to {@link GameState#getNpcs}
     * @return string with the Npc's description
     */
    public String getGUINpcString(int index) {
        return switch(client.getGameState().getNpcs()[index].getId()) {
            case 1 -> "Select 1 student from this card and an island for the student to be placed on, then add 1 student from the pouch to this card.";
            case 2 -> "During this turn take control of professors even if you are tied with their current owner.";
            case 3 -> "Select and island and compute it's dispute as if mother nature landed there.";
            case 4 -> "You can move mother nature by 2 additional islands this turn.";
            case 5 -> "Place an interdiction card on the selected island, the first time mother nature lands there you do not dispute the island.";
            case 6 -> "During this turn's island dispute the number of towers is not considered.";
            case 7 -> "Select up to 3 students to take from this card and swap them for the same number of selected students from your dashboard's entrance.";
            case 8 -> "During this turn's island dispute you have 2 extra points in your favour.";
            case 9 -> "Choose a color by selecting its corresponding dashboard row, during this turn's island dispute that color doesn't count towards the total score of any player.";
            case 10 -> "You can swap between them 2 students, one selected from your hall and one selected from your dashboard's entrance.";
            case 11 -> "Select 1 student from this card, and place it in your hall, then place one student from the punch back on this card.";
            default -> "Choose a color by selecting its corresponding dashboard row, each player, you included, has to put back in the pouch 3 students of that color from his dashboard's hall, if he has less than 3, he just puts back as many as he has.";
        }  + "\nCost: " + client.getGameState().getNpcs()[index].getCost();
    }

    /**
     * Provides the list of selected students among those on top of an Npc, for that Npc's activation.
     *
     * @return list of selected students among those on top of an Npc
     */
    public List<Integer> getSelectedNpcStudents() {
        return new ArrayList<>(selected_npc_students);
    }

    /**
     * Selects a student among those on top of an Npc, for that Npc's activation.
     *
     * @param student_index index of the student to select
     */
    public void selectNpcStudent(int student_index) {
        selected_npc_students.add(student_index);
    }

    /**
     * Unselects a student among those on top of an Npc, for that Npc's activation.
     *
     * @param student_index index of the student to unselect
     */
    public void unselectNpcStudent(int student_index) {
        selected_npc_students.remove((Integer) student_index);
    }

    /**
     * Provides the list of selected colors for an Npc's activation.
     *
     * @return list of selected colors
     */
    public List<Integer> getSelectedDashboardColors() {
        return new ArrayList<>(selected_dashboard_colors);
    }

    /**
     * Selects a color for an Npc's activation.
     *
     * @param color_index index of the color to select
     */
    public void selectDashboardColor(int color_index) {
        selected_dashboard_colors.add(color_index);
    }

    /**
     * Unselects a color for an Npc's activation.
     *
     * @param color_index index of the color to unselect
     */
    public void unselectDashboardColor(int color_index) {
        selected_dashboard_colors.remove((Integer) color_index);
    }

    /**
     * Provides the index of the selected island for an Npc's activation.
     *
     * @return index of the selected island
     */
    public int getSelectedNpcIsland() {
        return selected_npc_island;
    }

    /**
     * Selects an island for an Npc's activation. Replaces any previously selected island.
     *
     * @param selected_npc_island island to select
     */
    public void setSelectedNpcIsland(int selected_npc_island) {
        this.selected_npc_island = selected_npc_island;
    }

    /**
     * Provides the list of selected entrance students for an Npc's activation.
     *
     * @return list of selected entrance students
     */
    public List<Integer> getSelectedEntranceStudents() {
        return new ArrayList<>(selected_npc_entrance_students);
    }

    /**
     * Selects an entrance student for an Npc's activation.
     *
     * @param student_index index of the entrance student to select
     */
    public void selectEntranceStudent(int student_index) {
        selected_npc_entrance_students.add(student_index);
    }

    /**
     * Unselects an entrance student for an Npc's activation.
     *
     * @param student_index index of the entrance student to unselect
     */
    public void unselectEntranceStudent(int student_index) {
        selected_npc_entrance_students.remove((Integer) student_index);
    }

    /**
     * Provides the {@link JTextArea} dedicated to showing the Npc's description.
     *
     * @return {@link JTextArea} dedicated to the Npc's descriptions
     */
    public JTextArea getNpcOverlayTextArea() {
        return npc_overlay_text_area;
    }
}
