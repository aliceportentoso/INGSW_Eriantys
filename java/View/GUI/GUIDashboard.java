package View.GUI;

import Controller.ClientSide.Client;
import Model.Colors;
import Model.GameState;

import java.awt.*;
import java.awt.event.MouseEvent;
import java.awt.event.MouseListener;
import java.util.ArrayList;

/**
 * {@link GUIElement} representing the client's associated player's dashboard (INTENDED ONLY FOR YOUR PLAYER).<br>
 * A click on the dashboard, if an {@link GUIEntranceStudent} is selected, attempts to move it if allowed by the client's model instance.
 * The dashboard shows on its own its content, that being:
 * <ul>
 *     <li> The rooks the player has on the dashboard
 *     <li> The students in the player's hall
 *     <li> The player's professors
 * </ul>
 * <br><br>
 * All the dashboard's content 1:1 with the model.
 */
public class GUIDashboard extends GUIElement implements MouseListener {
    //reference to the client, used for both choosing when to draw and when to handle events
    protected Client client;
    //reference to the game panel this element is in
    protected GUIGamePanel gui_panel;

    protected static int[] mapping = new int[] {2, 3, 0, 4, 1};

    /**
     * Instantiates a dashboard with the provided coordinates and dimensions, if the constructed dashboard's {@link GUIDashboard#show} method
     * is then called at each repaint, the dashboard will always be displayed on the GUI correctly.
     *
     * @param client reference client for the UI
     * @param gui_panel reference {@link GUIGamePanel} for this {@link GUIElement}
     * @param x to left corner X coordinate for the dashboard
     * @param y to left corner Y coordinate for the dashboard
     * @param width width for the dashboard
     * @param height height for the dashboard
     */
    public GUIDashboard(Client client, GUIGamePanel gui_panel, Image image, int x, int y, int width, int height) {
        this.client = client;
        this.gui_panel = gui_panel;
        this.image = image;
        this.x = x;
        this.y = y;
        this.width = width;
        this.height = height;
    }

    /**
     * {@inheritDoc}
     * @param g instance of {@link Graphics} handling the current repaint
     */
    public void show(Graphics g) {
        Graphics2D g2d = (Graphics2D) g;
        GameState gs = client.getGameState();

        if(gs != null) {
            //SET STUDENT PHASE
            if(gs.isMyTurn(client.getClientID()) && gs.getPhase() == 1 && gs.getStep() == 0 &&
                    gui_panel.getSelectedStudentIndex() != -1 && gui_panel.getActivatedNpc() == -1) {
                //hover -> border
                Point mouse_location = mouseLocation();
                if(inBounds((int) mouse_location.getX(), (int) mouse_location.getY())) {
                    g2d.setColor(Color.gray);
                    g2d.setStroke(new BasicStroke((float) (6 * GUIGamePanel.SCREEN_SCALE)));
                    g2d.drawRect((int) ((x - 5) * GUIGamePanel.SCREEN_WIDTH_SCALE), (int) ((y - 5) * GUIGamePanel.SCREEN_HEIGHT_SCALE),
                            (int) ((width + 10) * GUIGamePanel.SCREEN_WIDTH_SCALE), (int) ((height + 10) * GUIGamePanel.SCREEN_HEIGHT_SCALE));
                    g2d.setStroke(new BasicStroke());
                }

                //draw aura if a student is ready to be place in the hall
                g2d.setColor(Color.decode("#ccffff"));
                g2d.fillRect((int) ((x - 5) * GUIGamePanel.SCREEN_WIDTH_SCALE), (int) ((y - 5) * GUIGamePanel.SCREEN_HEIGHT_SCALE),
                        (int) ((width + 10) * GUIGamePanel.SCREEN_WIDTH_SCALE), (int) ((height + 10) * GUIGamePanel.SCREEN_HEIGHT_SCALE));
            }

            g.drawImage(image, (int) (x * GUIGamePanel.SCREEN_WIDTH_SCALE), (int) (y * GUIGamePanel.SCREEN_HEIGHT_SCALE),
                    (int) (width * GUIGamePanel.SCREEN_WIDTH_SCALE), (int) (height * GUIGamePanel.SCREEN_HEIGHT_SCALE), null);

            //aura around each row, only if there is active an NPCs that interacts with the dashboard's rows
            if(client.getLobby().expert_mode && gs.isMyTurn(client.getClientID()) &&(gui_panel.getActivatedNpcId() == 12 ||
                    gui_panel.getActivatedNpcId() == 9 || gui_panel.getActivatedNpcId() == 10)) {
                for(int i = 0; i < 5; i++) {
                    //if the row is already selected, draw a white aura
                    if(gui_panel.getSelectedDashboardColors().contains(mapping[i]))
                        g2d.setColor(Color.black);
                    else
                        g2d.setColor(Color.decode("#ccffff"));
                    g2d.setStroke(new BasicStroke((float) (3 * GUIGamePanel.SCREEN_SCALE)));
                    g.drawRect((int) ((x + 133) * GUIGamePanel.SCREEN_WIDTH_SCALE), (int) ((y + 40 + 53 * i) * GUIGamePanel.SCREEN_HEIGHT_SCALE),
                            (int) (30 * 12 * GUIGamePanel.SCREEN_WIDTH_SCALE), (int) (30 * GUIGamePanel.SCREEN_HEIGHT_SCALE));
                    g2d.setStroke(new BasicStroke());
                }
            }

            //draw students in the hall
            for(int i = 0; i < 5; i++) {
                for(int j = 0; j < gs.myPlayer(client).getDashboard().getHallRow(mapping[i]); j++) {    //per ogni riga
                    g.drawImage(GUIGamePanel.student_images.get(Colors.fromColorIndex(mapping[i])),
                            (int)((x + 140 + 35 * j) * GUIGamePanel.SCREEN_WIDTH_SCALE),
                            (int) ((y + 40 + 53 * i) * GUIGamePanel.SCREEN_HEIGHT_SCALE),
                            (int) (30 * GUIGamePanel.SCREEN_SCALE), (int) (30 * GUIGamePanel.SCREEN_SCALE), null);
                }
            }

            //draw professor
            for(int i = 0; i < 5; i++) {
                if(gs.myPlayer(client).getDashboard().getProfessor(mapping[i])) {
                    g.drawImage(GUIGamePanel.professor_images.get(Colors.fromColorIndex(mapping[i])),(int) ((x + 528) * GUIGamePanel.SCREEN_WIDTH_SCALE), (int) ((y + 40 + 53 * i) * GUIGamePanel.SCREEN_HEIGHT_SCALE),
                            (int) (30 * GUIGamePanel.SCREEN_SCALE), (int) (30 * GUIGamePanel.SCREEN_SCALE), null);
                }
            }

            //draw rooks
            for(int i = 0; i < gs.myPlayer(client).getDashboard().getRooks(); i++){
                g.drawImage(GUIGamePanel.rook_images[gs.myPlayer(client).player_index], (int)((x + width*3/4 + 55 + 55*((i/4)%2))* GUIGamePanel.SCREEN_WIDTH_SCALE),
                        (int)((y + 60 + 45*(i%4))* GUIGamePanel.SCREEN_HEIGHT_SCALE),
                        (int)(52/3 * GUIGamePanel.SCREEN_SCALE),(int)(99/3 *GUIGamePanel.SCREEN_SCALE), null);
            }
        }
    }

    //the only clickable are should be the hall, hence the scaling to avoid clicks on the other parts

    /**
     * {@inheritDoc}
     * 
     * @param test_x X coordinate to test
     * @param test_y Y coordinate to test
     * @return
     */
    @Override
    public boolean inBounds(int test_x, int test_y) {
        return test_x > x * GUIGamePanel.SCREEN_WIDTH_SCALE + (width * GUIGamePanel.SCREEN_WIDTH_SCALE)*0.16 && test_x < x * GUIGamePanel.SCREEN_WIDTH_SCALE + (width * GUIGamePanel.SCREEN_WIDTH_SCALE)*0.776 &&
                test_y > y * GUIGamePanel.SCREEN_HEIGHT_SCALE && test_y < y * GUIGamePanel.SCREEN_HEIGHT_SCALE + (height * GUIGamePanel.SCREEN_HEIGHT_SCALE);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void mouseClicked(MouseEvent e) {

    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void mousePressed(MouseEvent e) {
        GameState gs = client.getGameState();

        //set student to hall on click
        if(inBounds(e.getX(), e.getY()) && gs.isMyTurn(client.getClientID())) {
            if(gs.getPhase() == 1 && gs.getStep() == 0 && gui_panel.getSelectedStudentIndex() != -1 && gui_panel.getActivatedNpc() == -1) {

                client.setStudentToHall(gui_panel.getSelectedStudentIndex());
                gui_panel.setSelectedStudentIndex(-1);

                //create particles
                ArrayList<GUIParticle> particles = new ArrayList<GUIParticle>();
                for(int i = 0; i < 6; i++)
                    particles.add(new GUIParticle(GUIGamePanel.start_particle, x + width / 2, y + height / 2,
                            GUIGamePanel.PARTICLE_SIZE, GUIGamePanel.PARTICLE_SIZE, 0.92f, true));
                gui_panel.addParticles(particles);
            }

            //only if there is active an NPCs that interacts with the dashboard's rows
            if(client.getLobby().expert_mode && (gui_panel.getActivatedNpcId() == 12 ||
                    gui_panel.getActivatedNpcId() == 9 || gui_panel.getActivatedNpcId() == 10)) {
                //check each row for a click and act accordingly
                for(int i = 0; i < 5; i++) {
                    if(e.getY() > (y + 40 + 53 * i) * GUIGamePanel.SCREEN_HEIGHT_SCALE &&
                            e.getY() < (y + 40 + 53 * i) * GUIGamePanel.SCREEN_HEIGHT_SCALE + 30 * GUIGamePanel.SCREEN_HEIGHT_SCALE) {
                        //only one color has to be selected
                        for(int j : gui_panel.getSelectedDashboardColors())
                            gui_panel.unselectDashboardColor(j);
                        gui_panel.selectDashboardColor(mapping[i]);
                    }
                }
            }
        }
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void mouseReleased(MouseEvent e) {

    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void mouseEntered(MouseEvent e) {

    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void mouseExited(MouseEvent e) {

    }
}
