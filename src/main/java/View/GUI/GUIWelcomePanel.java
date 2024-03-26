package View.GUI;

import Controller.ClientSide.Client;

import javax.swing.*;
import javax.swing.border.EtchedBorder;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.util.Objects;

/**
 * Panel intended to make the user choose a nickname and consequently login.<br>
 * The panel displays a simple set of elements: an input field, a label and a button to accomplish its goal.
 */
public class GUIWelcomePanel extends JPanel {
    private static final int FONT_SIZE = 16;

    private final Client client;
    private final JPanel inner_panel;
    private final JLabel enter_nick_label;
    private final JTextField text_field;
    private final JButton login_button;
    private final JLabel error_label;
    private final JLabel image_label;

    private final ImageIcon image;

    /**
     * Constructs the panel and all its components.
     *
     * @param client {@link Client} which is using the UI
     */
    public GUIWelcomePanel(Client client) {
        this.client = client;

        this.setLayout(null);

        inner_panel = new JPanel();

        inner_panel.setLayout(new GridLayout(0, 1));
        inner_panel.setLocation(GUIFrame.WIDTH/2 - GUIFrame.WIDTH/6, 2*GUIFrame.HEIGHT/3);
        inner_panel.setSize(new Dimension(GUIFrame.WIDTH/3, GUIFrame.HEIGHT/4));
        inner_panel.setVisible(true);
        inner_panel.setBackground(new Color(236,204,191,100));
        this.add(inner_panel);

        /*innerPanel = new JPanel();
        innerPanel.setLayout(new CardLayout());
        centeredPanel.add(innerPanel);*/

        enter_nick_label = new JLabel("Enter your nickname");
        enter_nick_label.setFont(new Font("Segoe UI", Font.BOLD, FONT_SIZE));
        enter_nick_label.setForeground(Color.white);
        enter_nick_label.setHorizontalAlignment(SwingConstants.CENTER);
        inner_panel.add(enter_nick_label);

        text_field = new JTextField(10);
        text_field.setHorizontalAlignment(JTextField.CENTER);
        text_field.setFont(new Font("Segoe UI", Font.BOLD, FONT_SIZE));
        text_field.setBackground(Color.white);
        text_field.setForeground(Color.darkGray);

        text_field.setBorder(new EtchedBorder());
        text_field.addActionListener(this::nicknameInserted);
        inner_panel.add(text_field);

        login_button = new JButton("LOG IN");
        login_button.setBackground(new Color (94, 78, 71));
        login_button.setFont(new Font("Segoe UI", Font.PLAIN, FONT_SIZE));
        login_button.addActionListener(this::nicknameInserted);
        inner_panel.add(login_button);

        error_label = new JLabel(" ");
        error_label.setBackground(new Color(0, 0, 0, 0));
        error_label.setFont(new Font("Segoe UI", Font.PLAIN, FONT_SIZE));
        error_label.setHorizontalAlignment(SwingConstants.CENTER);

        inner_panel.add(error_label);

        //image background
        ClassLoader loader = ClassLoader.getSystemClassLoader();
        image = new ImageIcon (Objects.requireNonNull(loader.getResource("Welcome.png")));
        image_label = new JLabel(new ImageIcon(image.getImage().getScaledInstance(GUIFrame.WIDTH, GUIFrame.HEIGHT, Image.SCALE_SMOOTH)));
        this.add(image_label);
        image_label.setLocation(0, 0);
        image_label.setSize(new Dimension(GUIFrame.WIDTH, GUIFrame.HEIGHT));
    }

    /**
     * Handler for when the user tries to choose a nickname, which gets consequently validated against a regex,
     * to allow only for nickname of at least 4 character between number, letter and eventually underscores.
     *
     * @param evt click or enter event triggering the handler
     */
    private void nicknameInserted(ActionEvent evt) {
        String text = text_field.getText();
        if(text.matches("^[A-Za-z][A-Za-z0-9_]{3,29}$")) {
            client.setNickname(text);
            error_label.setText(" ");
        } else {
            showError("<html>Invalid format, use a minimum of 4 letters,<br>numbers or underscores, " +
                    "nothing else allowed...</html>", new Color(99, 0, 0));
        }
        text_field.selectAll();
    }

    /**
     * Displays an error on the panel's dedicated error label, in the specified color.
     *
     * @param error text of the error
     * @param color color for the error text
     */
    public void showError(String error, Color color) {
        error_label.setForeground(new Color(color.getRed() - 100 > 0 ? color.getRed() - 100 : color.getRed(), color.getGreen(), color.getBlue()));
        error_label.setText(error);
        this.repaint();
    }

    /**
     * Method which signals this component that a resize has happened, consequently adjusting the font size and location of the elements inside
     * this panel.
     *
     * @param width new component width
     * @param height new component height
     */
    public void resized(int width, int height) {
        if(inner_panel != null) {
            inner_panel.setLocation(width/2 - Math.max(width, GUIFrame.BASE_WIDTH)/6, 2*height/3);
            inner_panel.setSize(new Dimension(Math.max(width, GUIFrame.BASE_WIDTH)/3, Math.max(height, GUIFrame.BASE_HEIGHT)/4));
            image_label.setIcon(new ImageIcon(image.getImage().getScaledInstance(width, height, Image.SCALE_SMOOTH)));

            enter_nick_label.setFont(new Font("Segoe UI", Font.BOLD, (int) (FONT_SIZE * Math.max(Math.min((((float) height)/GUIFrame.HEIGHT), (((float) width)/GUIFrame.WIDTH)), 1))));
            text_field.setFont(new Font("Segoe UI", Font.PLAIN, (int) (FONT_SIZE * Math.max(Math.min((((float) height)/GUIFrame.HEIGHT), (((float) width)/GUIFrame.WIDTH)), 1))));
            login_button.setFont(new Font("Segoe UI", Font.PLAIN, (int) (FONT_SIZE * Math.max(Math.min((((float) height)/GUIFrame.HEIGHT), (((float) width)/GUIFrame.WIDTH)), 1))));
            error_label.setFont(new Font("Segoe UI", Font.PLAIN, (int) (FONT_SIZE * Math.max(Math.min((((float) height)/GUIFrame.HEIGHT), (((float) width)/GUIFrame.WIDTH)), 1))));
        }
        if(image_label != null)
            image_label.setSize(new Dimension(width, height));

        if(this.isVisible())
            this.repaint();
    }
}
