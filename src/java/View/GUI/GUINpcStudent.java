package View.GUI;

import Controller.ClientSide.Client;
import Model.Colors;
import Model.GameState;
import Model.Npc;

import java.awt.*;
import java.util.List;
import java.awt.event.MouseEvent;
import java.awt.event.MouseListener;
import java.util.stream.Collectors;

public class GUINpcStudent extends GUIElement implements MouseListener {
    //index of this student among the ones above a npc
    protected int index;
    //index of the npc this student is on
    protected int npc_index;
    //reference to the client, used for both choosing when to draw and when to handle events
    protected Client client;
    //reference to the game panel this element is in
    protected GUIGamePanel gui_panel;

    //dim is both width and height, since they are equal
    public GUINpcStudent(Client client, GUIGamePanel gui_panel, int x, int y, int dim, int index, int npc_index) {
        this.client = client;
        this.gui_panel = gui_panel;
        this.x = x;
        this.y = y;
        this.width = dim;
        this.height = dim;
        this.index = index;
        this.npc_index = npc_index;
    }

    public void show(Graphics g) {
        if(client.getLobby().expert_mode) {
            Graphics2D g2d = (Graphics2D) g;
            GameState gs = client.getGameState();
            Npc npc = gs.getNpcs()[npc_index];

            //the related NPC must be one of those with students on it
            if(npc.getId() == 1 ||
                    npc.getId() == 7 ||
                    npc.getId() == 11) {

                List<Colors> students = npc.getExtraProperty().stream().map(Colors::fromColorIndex).toList();

                if(index < npc.getExtraProperty().size()) {
                    //if you npc is getting activated and this student can be selected, draw an aura
                    if(gs.isMyTurn(client.getClientID()) && gui_panel.getActivatedNpc() == npc_index) {
                        g.setColor(Color.decode("#ccffff"));
                        g.fillOval((int) ((x - 5) * GUIGamePanel.SCREEN_WIDTH_SCALE), (int) ((y - 5) * GUIGamePanel.SCREEN_HEIGHT_SCALE),
                                (int) ((width + 10) * GUIGamePanel.SCREEN_SCALE), (int) ((height + 10) * GUIGamePanel.SCREEN_SCALE));

                        drawHover(g2d);
                    }

                    //TODO: mouse overlay

                    //if you are among the already selected ones, draw an aura
                    if(gui_panel.getSelectedNpcStudents().contains(index) && gui_panel.getActivatedNpcId() == gs.getNpcs()[npc_index].getId()) {
                        g.setColor(Color.black);
                        g.fillOval((int) ((x - 4) * GUIGamePanel.SCREEN_WIDTH_SCALE), (int) ((y - 4) * GUIGamePanel.SCREEN_HEIGHT_SCALE),
                                (int) ((width + 8) * GUIGamePanel.SCREEN_SCALE), (int) ((height + 8) * GUIGamePanel.SCREEN_SCALE));
                    }

                    g.drawImage(GUIGamePanel.student_images.get(students.get(index)), (int) (x * GUIGamePanel.SCREEN_WIDTH_SCALE), (int) (y * GUIGamePanel.SCREEN_HEIGHT_SCALE),
                            (int) (width * GUIGamePanel.SCREEN_SCALE), (int) (height * GUIGamePanel.SCREEN_SCALE), null);
                }
            }
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
        if(client.getLobby().expert_mode) {
            GameState gs = client.getGameState();
            Npc npc = gs.getNpcs()[npc_index];

            if (inBounds(e.getX(), e.getY()) && client.getLobby().expert_mode &&
                    gs.isMyTurn(client.getClientID()) && gui_panel.getActivatedNpc() == npc_index &&
                    (npc.getId() == 1 || npc.getId() == 7 || npc.getId() == 11) &&
                    index < npc.getExtraProperty().size()) {

                if (gui_panel.getActivatedNpcId() == 1 || gui_panel.getActivatedNpcId() == 11) {
                    //only one student has to be selected in this case
                    for (int i : gui_panel.getSelectedNpcStudents())
                        gui_panel.unselectNpcStudent(i);
                    gui_panel.selectNpcStudent(index);
                } else {
                    //at most 3 student can be selected in this case
                    if (gui_panel.getSelectedNpcStudents().contains(index))
                        gui_panel.unselectNpcStudent(index);
                    else if (gui_panel.getSelectedNpcStudents().size() < 3)
                        gui_panel.selectNpcStudent(index);
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
