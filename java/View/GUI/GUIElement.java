package View.GUI;

import java.awt.*;

/**
 * Abstract class for an element of the {@link GUI}'s {@link View.GUI.GUIGamePanel GamePanel}.<br>
 * An element of this class is intended to be drawn by a {@link Graphics} instance onto the relative {@link View.GUI.GUIGamePanel GamePanel}'s screen,
 * implementing the procedures to handle the resizing of the GUI's window and the basic methods for the mouse HID, hence it has utility methods to:
 * <ul>
 *     <li> Draw the element on the screen, by default drawning its image
 *     <li> Draw an aura around the element, by default a circular one
 *     <li> Detect if something is inside the bounds of this element
 *     <li> Procuring the mouse's location
 * </ul>
 */
public abstract class GUIElement {
    //the following attributes could have been made private, with getters and setters

    protected Image image;
    //position at 1280x720, scaled via screen scale
    protected int x, y;
    //actual dimensions at 1280x720, scaled via screen scale
    protected int width, height;

    //draws the elements on the panel
    /**
     * Draws this element on the related {@link View.GUI.GUIGamePanel GamePanel}, in the coordinates and dimensions provided during construction,
     * taking into account the current size of the GamePanel, resizing this element accordingly.
     *
     * @param g instance of {@link Graphics} handling the current repaint
     */
    public void show(Graphics g) {
        g.drawImage(image, (int) (x * GUIGamePanel.SCREEN_WIDTH_SCALE), (int) (y * GUIGamePanel.SCREEN_HEIGHT_SCALE),
                (int) (width * GUIGamePanel.SCREEN_SCALE), (int) (height * GUIGamePanel.SCREEN_SCALE), null);
    }

    //if the mouse hovers on the element, draw an oval contour
    /**
     * Draws a contour around this element, an aura, only when the mouse is inside the element's area.<br>
     * By default, the contour is round.
     *
     * @param g2d instance of {@link Graphics2D} handling the current repaint
     */
    public void drawHover(Graphics2D g2d) {
        Point mouse_location = mouseLocation();

        if(inBounds((int) mouse_location.getX(), (int) mouse_location.getY())) {
            g2d.setColor(Color.gray);
            g2d.setStroke(new BasicStroke((float) (3 * GUIGamePanel.SCREEN_SCALE)));
            g2d.drawOval((int) ((x - 5) * GUIGamePanel.SCREEN_WIDTH_SCALE), (int) ((y - 5) * GUIGamePanel.SCREEN_HEIGHT_SCALE),
                    (int) ((width + 10) * GUIGamePanel.SCREEN_SCALE), (int) ((height + 10) * GUIGamePanel.SCREEN_SCALE));
            g2d.setStroke(new BasicStroke());
        }
    }

    //checks if a pair of coordinates is inside this element's boundaries
    /**
     * Checks if a pair of coordinates is inside this element's boundaries, taking into account eventual resizes of the window.
     *
     * @param test_x X coordinate to test
     * @param test_y Y coordinate to test
     * @return true if the coordinates provided are over/inside the element
     */
    public boolean inBounds(int test_x, int test_y) {
        return test_x > x * GUIGamePanel.SCREEN_WIDTH_SCALE && test_x < x * GUIGamePanel.SCREEN_WIDTH_SCALE + (width * GUIGamePanel.SCREEN_SCALE) &&
                test_y > y * GUIGamePanel.SCREEN_HEIGHT_SCALE && test_y < y * GUIGamePanel.SCREEN_HEIGHT_SCALE + (height * GUIGamePanel.SCREEN_SCALE);
    }

    //returns the mouse position relative to the game's JPanel
    /**
     * Provides the current mouse position relative to the game's JPanel
     *
     * @return the current mouse position
     */
    public Point mouseLocation() {
        Point mouse_location = GUI.frame.getGamePanel().getMousePosition();
        if(mouse_location != null)
            return mouse_location;
        else
            return new Point(0, 0);
    }
}
