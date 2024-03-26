package View.GUI;

import Controller.ClientSide.Client;
import Model.Colors;
import Model.GameState;

import javax.swing.*;
import java.awt.*;
import java.awt.event.MouseEvent;
import java.awt.event.MouseListener;

/**
 * {@link GUIElement} representing an NPc in the game, if expert mode is enabled<br>
 * A click on an Npc shows a dialog to activate it.<br>
 * Moving the mouse over an Npc shows a dialog with its effect's description.
 */
public class GUINpc extends GUIElement implements MouseListener {
    //index of this npc in the game
    protected int index;
    //reference to the client, used for both choosing when to draw and when to handle events
    protected Client client;
    //reference to the game panel this element is in
    protected GUIGamePanel gui_panel;

    /**
     * Instantiates a new Npc with the provided coordinates and dimensions, if the constructed Npc's {@link GUINpc#show} method
     * is then called at each repaint, the Npc will always be displayed on the GUI correctly.
     *
     * @param client reference client for the UI
     * @param gui_panel reference {@link GUIGamePanel} for this {@link GUIElement}
     * @param x to left corner X coordinate for this Npc
     * @param y to left corner Y coordinate for this Npc
     * @param width width for this Npc
     * @param height height for this Npc
     * @param index index of the Npc represented out of the 10 potentially available in the model
     */
    public GUINpc(Client client, GUIGamePanel gui_panel, int x, int y, int width, int height, int index) {
        this.client = client;
        this.gui_panel = gui_panel;
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
        if(client.getLobby().expert_mode) {
            Graphics2D g2d = (Graphics2D) g;

            //hover -> border
            Point mouse_location = mouseLocation();
            if((inBounds((int) mouse_location.getX(), (int) mouse_location.getY()) && client.getGameState().getPhase() == 1
                    && client.getGameState().isMyTurn(client.getClientID()) && gui_panel.getActivatedNpc() == -1) ||
                    gui_panel.getActivatedNpc() == index) {
                if (gui_panel.getActivatedNpc() == index)
                    g2d.setColor(Color.yellow);
                else
                    g2d.setColor(Color.gray);

                g2d.setStroke(new BasicStroke((float) (3 * GUIGamePanel.SCREEN_SCALE)));
                g2d.drawRect((int) ((x - 5) * GUIGamePanel.SCREEN_WIDTH_SCALE), (int) ((y - 5) * GUIGamePanel.SCREEN_HEIGHT_SCALE),
                        (int) ((width + 10) * GUIGamePanel.SCREEN_SCALE), (int) ((height + 10) * GUIGamePanel.SCREEN_SCALE));
                g2d.setStroke(new BasicStroke());
            }

            //aura in case the npc has been activated
            if(client.getGameState().getNpcs()[index].getId() == client.getGameState().getNpcEffect()) {
                g2d.setColor(Color.decode("#ffffe6"));
                g2d.fillRect((int) ((x - 5) * GUIGamePanel.SCREEN_WIDTH_SCALE), (int) ((y - 5) * GUIGamePanel.SCREEN_HEIGHT_SCALE),
                        (int) ((width + 10) * GUIGamePanel.SCREEN_SCALE), (int) ((height + 10) * GUIGamePanel.SCREEN_SCALE));
            }

            //draw the actual card, taking the image from the preloaded ones
            Image image = GUIGamePanel.npc_images[client.getGameState().getNpcs()[index].getId() - 1];

            g2d.drawImage(image, (int) (x * GUIGamePanel.SCREEN_WIDTH_SCALE), (int) (y * GUIGamePanel.SCREEN_HEIGHT_SCALE),
                    (int) (width * GUIGamePanel.SCREEN_SCALE), (int) (height * GUIGamePanel.SCREEN_SCALE), null);

            //draw the remaining interdictions, if any
            if(client.getGameState().getNpcs()[index].getId() == 5) {
                g.setColor(Color.black);
                g.drawString(String.valueOf(client.getGameState().getNpcs()[index].getExtraProperty().get(0)),
                        (int) (x * GUIGamePanel.SCREEN_WIDTH_SCALE + ((int) (width * GUIGamePanel.SCREEN_SCALE / 10))),
                        (int) (y * GUIGamePanel.SCREEN_HEIGHT_SCALE) + ((int) (4* height * GUIGamePanel.SCREEN_SCALE / 5)));
            }

            //draw the blocked color, if any
            if(client.getGameState().getNpcs()[index].getId() == 9 && client.getGameState().getNpcEffect() == 9 && client.getGameState().getNpcs()[index].getExtraProperty().size() > 0) {
                g.drawImage(GUIGamePanel.student_images.get(Colors.fromColorIndex(client.getGameState().getNpcs()[index].getExtraProperty().get(0))),
                        (int) (x * GUIGamePanel.SCREEN_WIDTH_SCALE + (width/2 - 15) * GUIGamePanel.SCREEN_SCALE),
                        (int) (y * GUIGamePanel.SCREEN_HEIGHT_SCALE + (height/2 - 15) * GUIGamePanel.SCREEN_SCALE),
                        (int) ((30) * GUIGamePanel.SCREEN_SCALE), (int) ((30) * GUIGamePanel.SCREEN_SCALE), null);
            }
        }
    }

    //dedicate method to show the overlay after everything else has been drawn
    /**
     * Secondary show method intended to show this {@link GUINpc}'s description box at the end the {@link GUIGamePanel}'s show method, hence on top of everything else.
     */
    public void showOverlay(Graphics g) {
        //hover -> description
        Point mouse_location = mouseLocation();
        if(inBounds((int) mouse_location.getX(), (int) mouse_location.getY()) && gui_panel.getActivatedNpcId() == -1) {
            JTextArea overlay = gui_panel.getNpcOverlayTextArea();
            overlay.setSize((int) ((width * 6.5) * GUIGamePanel.SCREEN_SCALE), (int) ((height) * GUIGamePanel.SCREEN_SCALE));

            if(!overlay.isVisible())
                overlay.setVisible(true);

            overlay.setText(gui_panel.getGUINpcString(index));

            if(mouse_location.getX() != 0 && mouse_location.getY() != 0) {
                overlay.setLocation((int) mouse_location.getX() + 1, (int) (mouse_location.getY() - ((height) * GUIGamePanel.SCREEN_SCALE) - 1));
            }

            overlay.update(g.create(overlay.getLocation().x, overlay.getLocation().y,
                    (int) ((width * 6.5) * GUIGamePanel.SCREEN_SCALE), (int) ((height) * GUIGamePanel.SCREEN_SCALE)));
        } else {
            gui_panel.requestInvisibleNpcOverlayTextArea();
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

        if(client.getLobby().expert_mode && inBounds(e.getX(), e.getY()) && gui_panel.getActivatedNpc() == -1 && gs.getPhase() == 1 &&
                gs.isMyTurn(client.getClientID()) && gs.myPlayer(client).getCoins() >= client.getGameState().getNpcs()[index].getCost() &&
                gs.getNpcEffect() == 0) {

            gui_panel.activateNpc(index);
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
