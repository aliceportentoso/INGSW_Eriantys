package View.GUI;

import Controller.ClientSide.Client;
import Model.Colors;
import Model.GameState;

import java.awt.*;
import java.awt.event.MouseEvent;
import java.awt.event.MouseListener;
import java.util.List;

/**
 * {@link GUIElement} representing one of the entrance students in the client's associated player's dashboard.<br>
 * A click on a student, if possible depending on the game's phase and step, selects it to allow it to be moved.
 * <br><br>
 * Every student doesn't represent a specific entrance student of the model, but it represents the student in the provided
 * position (index) among those in the client's dashboard's entrance, as such the specific student referenced can change during a game.
 */
public class GUIEntranceStudent extends GUIElement implements MouseListener {
    //index of the entrance student it represents from the game, starts at 0
    protected int index;
    //reference to the client, used for both choosing when to draw and when to handle events
    protected Client client;
    //reference to the game panel this element is in
    protected GUIGamePanel gui_panel;

    //dim is both width and height, since they are equal
    /**
     * Instantiates a new entrance student with the provided coordinates and dimensions, if the constructed entrance student's {@link GUIEntranceStudent#show} method
     * is then called at each repaint, the entrance student will always be displayed on the GUI correctly.
     *
     * @param client reference client for the UI
     * @param gui_panel reference {@link GUIGamePanel} for this {@link GUIElement}
     * @param x to left corner X coordinate for this entrance student
     * @param y to left corner Y coordinate for this entrance student
     * @param dim width and height for this entrance student
     * @param index index of the entrance student represented out of those available in the model
     */
    public GUIEntranceStudent(Client client, GUIGamePanel gui_panel, int x, int y, int dim, int index) {
        this.client = client;
        this.gui_panel = gui_panel;
        this.x = x;
        this.y = y;
        this.width = dim;
        this.height = dim;
        this.index = index;
    }

    /**
     * {@inheritDoc}
     * @param g instance of {@link Graphics} handling the current repaint
     */
    public void show(Graphics g) {
        GameState gs = client.getGameState();
        Graphics2D g2d = (Graphics2D) g;
        List<Colors> entrance = gs.myPlayer(client).getDashboard().getEntrance();

        if(index < entrance.size()) {

            //in the case that an NPC is being activated, that takes priority
            if(client.getLobby().expert_mode && gs.isMyTurn(client.getClientID()) && (gui_panel.getActivatedNpcId() == 10 || gui_panel.getActivatedNpcId() == 7)) {
                g.setColor(Color.decode("#ccffff"));
                g.fillOval((int) (x * GUIGamePanel.SCREEN_WIDTH_SCALE) - 8, (int) (y * GUIGamePanel.SCREEN_HEIGHT_SCALE) - 8,
                        (int) (width * GUIGamePanel.SCREEN_SCALE) + 16, (int) (height * GUIGamePanel.SCREEN_SCALE) + 16);

                drawHover(g2d);
            } else if(gs.isMyTurn(client.getClientID()) && gs.getPhase() == 1 && gs.getStep() == 0 && gui_panel.getActivatedNpc() == -1) {
                //if a student still has to be selected, draw an aura
                if(gui_panel.getSelectedStudentIndex() == -1) {
                    g.setColor(Color.decode("#ccffff"));
                    g.fillOval((int) (x * GUIGamePanel.SCREEN_WIDTH_SCALE) - 8, (int) (y * GUIGamePanel.SCREEN_HEIGHT_SCALE) - 8,
                            (int) (width * GUIGamePanel.SCREEN_SCALE) + 16, (int) (height * GUIGamePanel.SCREEN_SCALE) + 16);
                }

                drawHover(g2d);
            }

            //shadow to indicate the selected element
            if((gui_panel.getSelectedStudentIndex() == index && gui_panel.getActivatedNpcId() != 10) || gui_panel.getSelectedEntranceStudents().contains(index)) {
                g.setColor(Color.black);
                g.fillOval((int) (x * GUIGamePanel.SCREEN_WIDTH_SCALE) - 5, (int) (y * GUIGamePanel.SCREEN_HEIGHT_SCALE) -5,
                        (int) (width * GUIGamePanel.SCREEN_SCALE) + 10, (int) (height * GUIGamePanel.SCREEN_SCALE) + 10);
            }

            g.drawImage(GUIGamePanel.student_images.get(entrance.get(index)), (int) (x * GUIGamePanel.SCREEN_WIDTH_SCALE), (int) (y * GUIGamePanel.SCREEN_HEIGHT_SCALE),
                    (int) (width * GUIGamePanel.SCREEN_SCALE), (int) (height * GUIGamePanel.SCREEN_SCALE), null);
        }
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

        if(inBounds(e.getX(), e.getY())) {
            //in the case that an NPC is being activated, that takes priority
            if(client.getLobby().expert_mode && gs.isMyTurn(client.getClientID()) && (gui_panel.getActivatedNpcId() == 10 || gui_panel.getActivatedNpcId() == 7)) {
                if(gui_panel.getActivatedNpcId() == 10) {
                    //only one student has to be selected in this case
                    for(int i : gui_panel.getSelectedEntranceStudents())
                        gui_panel.unselectEntranceStudent(i);
                    gui_panel.selectEntranceStudent(index);
                } else {
                    //at most 3 student can be selected in this case
                    if(gui_panel.getSelectedEntranceStudents().contains(index))
                        gui_panel.unselectEntranceStudent(index);
                    else
                    if(gui_panel.getSelectedEntranceStudents().size() < 3)
                        gui_panel.selectEntranceStudent(index);
                }
            } else if(gs.isMyTurn(client.getClientID()) && gui_panel.getActivatedNpc() == -1 && index < gs.myPlayer(client).getDashboard().getEntrance().size() &&
                    gs.getPhase() == 1 && gs.getStep() == 0) {

                gui_panel.setSelectedStudentIndex(index);
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
