package View.GUI;

import javax.swing.*;
import javax.swing.border.LineBorder;
import javax.swing.text.SimpleAttributeSet;
import javax.swing.text.StyleConstants;
import javax.swing.text.StyledDocument;
import java.awt.*;

/**
 * {@link GUIElement} intended for displaying text in the center of a specified box.<br>
 */
public class GUIText extends GUIElement {
    
    private final JTextPane pane;
    private final int font_size; //22 is good enough usually

    /**
     * Sets up a text box to be displayed on the GUI.<br>
     * The text is always displayed in the middle.
     *
     * @param x upper left corner X coordinate of the new text box
     * @param y upper left corner Y coordinate of the new text box
     * @param width width of the text box
     * @param height height of the text box
     * @param font_size font size for the text box
     * @param border optional border for the text box, null if not wanted
     * @param color color for the text inside the text box
     */
    public GUIText(int x, int y, int width, int height, int font_size, boolean border, Color color) {
        this.x = x;
        this.y = y;
        this.width = width;
        this.height = height;
        this.font_size = font_size;

        pane = new JTextPane();
        pane.setVisible(true);
        pane.setEditable(false);
        pane.setBackground(new Color(0, 0, 0, 0));
        if(border)
            pane.setBorder(new LineBorder(Color.black));

        StyledDocument doc = pane.getStyledDocument();
        SimpleAttributeSet center = new SimpleAttributeSet();
        StyleConstants.setBold(center, true);
        StyleConstants.setForeground(center, color);
        StyleConstants.setAlignment(center, StyleConstants.ALIGN_CENTER);
        doc.setParagraphAttributes(0, doc.getLength(), center, false);
    }

    /**
     * {@inheritDoc}
     * @param g instance of {@link Graphics} handling the current repaint
     */
    @Override
    public void show(Graphics g) {
        pane.setSize((int) ((width) * GUIGamePanel.SCREEN_SCALE), (int) ((height) * GUIGamePanel.SCREEN_SCALE));
        pane.setFont(new Font("Segoe UI", Font.PLAIN, (int) (font_size * GUIGamePanel.SCREEN_SCALE)));

        pane.update(g.create((int) (x * GUIGamePanel.SCREEN_WIDTH_SCALE), (int) (y * GUIGamePanel.SCREEN_HEIGHT_SCALE),
                (int) ((width) * GUIGamePanel.SCREEN_SCALE), (int) ((height) * GUIGamePanel.SCREEN_SCALE)));
    }

    /**
     * Updates the text inside the text box.
     *
     * @param text new text for the box
     */
    public void setText(String text) {
        pane.setText(text);
    }
}
