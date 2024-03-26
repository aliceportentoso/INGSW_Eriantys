package View.GUI;

import Controller.ClientSide.Client;
import Model.Colors;
import Model.GameState;

import java.awt.*;
import java.awt.event.MouseEvent;
import java.awt.event.MouseListener;
import java.util.ArrayList;
import java.util.List;

/**
 * {@link GUIElement} representing a cloud of the game.<br>
 * A click on a cloud implies an attempt to play it, which only happens if allowed by the client's model instance.
 * <br><br>
 * Every cloud maps 1:1 via the provided index to a cloud in the model.
 */
public class GUICloud extends GUIElement implements MouseListener {
    //index of the cloud it represents from the game, starts at 0
    protected int index;
    //reference to the client, used for both choosing when to draw and when to handle events
    protected Client client;
    //reference to the game panel this element is in
    protected GUIGamePanel gui_panel;

    /**
     * Instantiates a new cloud with the provided coordinates and dimensions, if the constructed cloud's {@link GUICloud#show} method
     * is then called at each repaint, the cloud will always be displayed on the GUI correctly.
     *
     * @param client reference client for the UI
     * @param gui_panel reference {@link GUIGamePanel} for this {@link GUIElement}
     * @param x to left corner X coordinate for this cloud
     * @param y to left corner Y coordinate for this cloud
     * @param width width for this cloud
     * @param height height for this cloud
     * @param index index of the cloud represented out of the 2/3 potentially available in the model
     */
    public GUICloud(Client client, GUIGamePanel gui_panel, Image image, int x, int y, int width, int height, int index) {
        this.client = client;
        this.gui_panel = gui_panel;
        this.image = image;
        this.x = x;
        this.y = y;
        this.width = width;
        this.height = height;
        this.index = index;
    }

    /**
     * {@inheritDoc}
     * @param g instance of {@link Graphics} handling the current repaint
     */
    public void show(Graphics g) {
        if(index < client.getGameState().getClouds().length) {
            Graphics2D g2d = (Graphics2D) g;
            GameState gs = client.getGameState();

            List<Colors> students = gs.getClouds()[index];
            if(students != null && students.size() != 0) {
                if(gs.isMyTurn(client.getClientID()) && gs.getPhase() == 1 && gs.getStep() == 2 && gui_panel.getActivatedNpc() == -1) {
                    drawHover(g2d);

                    g.setColor(Color.decode("#ccffff"));
                    g.fillOval((int) ((x - 5) * GUIGamePanel.SCREEN_WIDTH_SCALE), (int) ((y - 5) * GUIGamePanel.SCREEN_HEIGHT_SCALE),
                            (int) ((width + 10) * GUIGamePanel.SCREEN_SCALE), (int) ((height + 10) * GUIGamePanel.SCREEN_SCALE));
                }
            }

            if(index < gs.getPlayers().size())
                super.show(g);

            //draw students on cloud
            if(students != null && students.size() != 0) {
                for (int i = 0; i < students.size(); i++) {
                    g.drawImage(GUIGamePanel.student_images.get(students.get(i)),
                            (int) (gs.otherPlayers(client).size() > 1 ? (x * GUIGamePanel.SCREEN_WIDTH_SCALE + (15 + 30 * (i%2)) * GUIGamePanel.SCREEN_SCALE) :
                                    (i == 2 ? (x * GUIGamePanel.SCREEN_WIDTH_SCALE + 30 * GUIGamePanel.SCREEN_SCALE) : (x * GUIGamePanel.SCREEN_WIDTH_SCALE + (15 + 30 * (i % 2)) * GUIGamePanel.SCREEN_SCALE))),
                            (int) (y * GUIGamePanel.SCREEN_HEIGHT_SCALE + (20 + 25 * (i / 2)) * GUIGamePanel.SCREEN_SCALE),
                            (int) (width * GUIGamePanel.SCREEN_SCALE / 4),
                            (int) (height * GUIGamePanel.SCREEN_SCALE / 4), null);
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
        GameState gs = client.getGameState();

        //choose cloud on click
        if (gs.getClouds()[index] != null) {
            if (inBounds(e.getX(), e.getY()) && gui_panel.getActivatedNpc() == -1 &&
                    gs.isMyTurn(client.getClientID()) && index < gs.getClouds().length && gs.getClouds()[index].size() != 0 &&
                    gs.getPhase() == 1 && gs.getStep() == 2) {
                client.chooseCloud(index);

                //create particles
                ArrayList<GUIParticle> particles = new ArrayList<GUIParticle>();
                for (int i = 0; i < 6; i++)
                    particles.add(new GUIParticle(GUIGamePanel.cloud_particle, x + width / 2, y + height / 2,
                            GUIGamePanel.PARTICLE_SIZE, GUIGamePanel.PARTICLE_SIZE, 0.90f, false));
                gui_panel.addParticles(particles);
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
