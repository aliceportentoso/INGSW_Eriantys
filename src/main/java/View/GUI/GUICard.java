package View.GUI;

import Controller.ClientSide.Client;
import Model.GameState;

import java.awt.*;
import java.awt.event.MouseEvent;
import java.awt.event.MouseListener;

/**
 * {@link GUIElement} representing a card of the game in the hand of this client's player.<br>
 * By default, cards are not shown, they are only displayed after the user clicks on {@link GUICardCompacted}, and are quickly
 * hidden after a click everywhere else.<br>
 * A click on a card implies an attempt to play it, which only happens if allowed by the client's model instance.
 * <br><br>
 * Every card doesn't represent a specific instance of {@link Model.Card Card} of the model, but it represents the card in the provided
 * position (index) among those in the client's hand, as such the specific instance of {@link Model.Card Card} referenced can change during a game.
 */
public class GUICard extends GUIElement implements MouseListener {
    //index of the card it represents from the game, starts at 0
    protected int index;
    //reference to the client, used for both choosing when to draw and when to handle events
    protected Client client;
    //reference to the game panel this element is in
    protected GUIGamePanel gui_panel;

    /**
     * Instantiates a new card with the provided coordinates and dimensions, if the constructed card's {@link GUICard#show} method
     * is then called at each repaint, the card will always be displayed on the GUI correctly.
     *
     * @param client reference client for the UI
     * @param gui_panel reference {@link GUIGamePanel} for this {@link GUIElement}
     * @param x to left corner X coordinate for this card
     * @param y to left corner Y coordinate for this card
     * @param width width for this card
     * @param height height for this card
     * @param index index of the card represented out of the 10 potentially available in the model
     */
    public GUICard(Client client, GUIGamePanel gui_panel, int x, int y, int width, int height, int index) {
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
        Graphics2D g2d = (Graphics2D) g;

        if(checkCompact() && index < client.getGameState().myPlayer(client).getCards().size()) {
            if(client.getGameState().isMyTurn(client.getClientID()) && client.getGameState().getPhase() == 0)
                drawHover(g2d);

            //draw the actual card, taking the image from the preloaded ones
            Image image = GUIGamePanel.card_images[client.getGameState().myPlayer(client).getCards().get(index).order_value - 1];

            g2d.drawImage(image, (int) (x * GUIGamePanel.SCREEN_WIDTH_SCALE), (int) (y * GUIGamePanel.SCREEN_HEIGHT_SCALE),
                    (int) (width * GUIGamePanel.SCREEN_SCALE), (int) (height * GUIGamePanel.SCREEN_SCALE), null);
        }
    }

    //override for a square contour
    /**
     * {@inheritDoc}<br><br>
     * Override for a square contour.
     */
    @Override
    public void drawHover(Graphics2D g2d) {
        Point mouse_location = mouseLocation();

        g2d.setColor(new Color(177, 206, 255, 140));
        g2d.fillRect((int)((x - 12) * GUIGamePanel.SCREEN_WIDTH_SCALE), (int)((y - 12)*GUIGamePanel.SCREEN_HEIGHT_SCALE),
                (int)((width + 24) * GUIGamePanel.SCREEN_SCALE), (int)((height + 24) * GUIGamePanel.SCREEN_SCALE));

        if(inBounds((int) mouse_location.getX(), (int) mouse_location.getY())) {
            g2d.setColor(Color.gray);
            g2d.setStroke(new BasicStroke((float) (3 * GUIGamePanel.SCREEN_SCALE)));
            g2d.drawRect((int) ((x - 5) * GUIGamePanel.SCREEN_WIDTH_SCALE), (int) ((y - 5) * GUIGamePanel.SCREEN_HEIGHT_SCALE),
                    (int) ((width + 10) * GUIGamePanel.SCREEN_SCALE), (int) ((height + 10) * GUIGamePanel.SCREEN_SCALE));
            g2d.setStroke(new BasicStroke());
        }
    }

    //handles the check for when to draw only the top card or draw them all
    /**
     * Handles the check for when to draw only the {@link GUICardCompacted GUICardCompacteds} or the {@link GUICard GUICards} instead
     * @return true if this card has to be drawn
     */
    protected boolean checkCompact() {
        return !gui_panel.getCompactCards();
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
        //System.out.println(e.getX() + " " + e.getY());

        //play card on click
        if(inBounds(e.getX(), e.getY()) && checkCompact() &&
                gs.isMyTurn(client.getClientID()) && index < gs.myPlayer(client).getCards().size() &&
                gs.getPhase() == 0) {

            client.playCard(index);
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
