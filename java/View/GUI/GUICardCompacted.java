package View.GUI;

import Controller.ClientSide.Client;

import java.awt.*;
import java.awt.event.MouseEvent;
import java.awt.event.MouseListener;

//Class to create only once, when it is clicked, all the other cards are shown on the screen
/**
 * {@link GUIElement} representing one of the cards in the compacted cards pile.<br>
 * Those are displayed by default instead of the {@link GUICard GUICards}, which are instead shown after a click on <strong>the top compacted card</strong>
 * of the constituted pile.<br>
 * A click everywhere switches then back from showing the Cards to showing those as a pile of cards.
 * <br><br>
 * Every compacted card doesn't represent a specific instance of {@link Model.Card Card} of the model, but it represents the card in the provided
 * position (index) among those in the client's hand, as such the specific instance of {@link Model.Card Card} referenced can change during a game.
 */
public class GUICardCompacted extends GUICard implements MouseListener {

    /**
     * Instantiates a new compacted card with the provided coordinates and dimensions, if the constructed compacted card's {@link GUICardCompacted#show} method
     * is then called at each repaint, the compacted card will always be displayed on the GUI correctly.
     *
     * @param client reference client for the UI
     * @param gui_panel reference {@link GUIGamePanel} for this {@link GUIElement}
     * @param x to left corner X coordinate for this compacted card
     * @param y to left corner Y coordinate for this compacted card
     * @param width width for this compacted card
     * @param height height for this compacted card
     * @param index index of the compacted card represented out of the 10 potentially available in the model
     */
    public GUICardCompacted(Client client, GUIGamePanel gui_panel, int x, int y, int width, int height, int index) {
        super(client, gui_panel, x, y, width, height, index);
    }

    /**
     * {@inheritDoc}
     * @return true if this card has to be drawn
     */
    @Override
    protected boolean checkCompact() {
        return gui_panel.getCompactCards();
    }

    /**
     * {@inheritDoc}
     * @param g instance of {@link Graphics} handling the current repaint
     */
    @Override
    public void show(Graphics g) {
        Graphics2D g2d = (Graphics2D) g;
        if (client.getGameState().myPlayer(client).getCards() != null) {
            if (checkCompact() && index < client.getGameState().myPlayer(client).getCards().size()) {
                if (client.getGameState().myPlayer(client).getCards().size() - 1 == index)
                    drawHover(g2d);

                Image image = GUIGamePanel.card_images[client.getGameState().myPlayer(client).getCards().get(index).order_value - 1];

                g2d.drawImage(image, (int) (x * GUIGamePanel.SCREEN_WIDTH_SCALE), (int) (y * GUIGamePanel.SCREEN_HEIGHT_SCALE),
                        (int) (width * GUIGamePanel.SCREEN_SCALE), (int) (height * GUIGamePanel.SCREEN_SCALE), null);

            }
        }
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void mousePressed(MouseEvent e) {
        if(client.getGameState().myPlayer(client).getCards().size() - 1 == index) {
            gui_panel.setCompactCards(!inBounds(e.getX(), e.getY()) || !gui_panel.getCompactCards());
        }
    }
}
