package View.GUI;

import java.awt.*;
import java.awt.geom.AffineTransform;
import java.awt.image.BufferedImage;
import java.util.Random;

/**
 * Class which creates a particle on the {@link GUIGamePanel}.<br>
 * A particle is an image with a given position, speed and rotation (optionally acceleration, angular velocity and growth ratio), which moves accordingly
 * to those physical properties like if it were a 2D body in the 2D plane constituted by the {@link GUIGamePanel}.<br>
 * Particles are used to show events and action int the GUI, and this class allows for complete customization of the particles' behaviour.
 * <br><br>
 * By default, particles are not animated, that happens if {@link GUIParticle#update} is called every frame, to allow the animation to take place.
 * It is recommended to dispose of a particle as soon as its {@link GUIParticle#toDelete} method returns true, since at that point teh particle
 * would be out of the screen or too small to be visible.
 */
public class GUIParticle extends GUIElement {

    private int velX, velY;
    private final float accX, accY;
    private float rotation;
    private final float angular_vel;
    private final float growth;

    /**
     * Fully customizable constructor without acceleration.
     *
     * @param image the particle's image
     * @param x starting x for the particle's center
     * @param y starting y for the particle's center
     * @param width starting particle width
     * @param height starting particle height
     * @param velX initial particle x velocity
     * @param velY initial particle y velocity
     * @param rotation initial rotation of the particle
     * @param angular_vel delta rotation of the particle each updated
     * @param growth size multiplier applied to width and height at each update
     * @param gravity flag for whether the particle has to be affected by gravity (hence being accelerated towards the bottom of the screen)
     */
    public GUIParticle(Image image, int x, int y, int width, int height, int velX, int velY, float rotation, float angular_vel, float growth, boolean gravity) {
        this.image = image;
        this.x = x;
        this.y = y;
        this.width = width;
        this.height = height;
        this.velX = velX;
        this.velY = velY;
        this.rotation = rotation;
        this.angular_vel = angular_vel;
        this.growth = growth;
        if (gravity)
            this.accY = 10;
        else
            this.accY = 0;
        this.accX = 0;
    }

    /**
     * Fully customizable constructor.
     *
     * @param image the particle's image
     * @param x starting x for the particle's center
     * @param y starting y for the particle's center
     * @param width starting particle width
     * @param height starting particle height
     * @param velX initial particle x velocity
     * @param velY initial particle y velocity
     * @param rotation initial rotation of the particle
     * @param angular_vel delta rotation of the particle each updated
     * @param accX delta velocity in the X direction
     * @param accY delta velocity in the Y direction
     * @param growth size multiplier applied to width and height at each update
     */
    public GUIParticle(Image image, int x, int y, int width, int height, int velX, int velY, float rotation, float angular_vel, float accX, float accY, float growth) {
        this.image = image;
        this.x = x;
        this.y = y;
        this.width = width;
        this.height = height;
        this.velX = velX;
        this.velY = velY;
        this.rotation = rotation;
        this.angular_vel = angular_vel;
        this.growth = growth;
        this.accX = accX;
        this.accY = accY;
    }

    //randomized constructor
    /**
     * Randomized constructor, the absent parameters are randomly initialized.<br>
     * The acceleration is always 0, except for gravity, if enabled.
     *
     * @param image the particle's image
     * @param x starting x for the particle's center
     * @param y starting y for the particle's center
     * @param width starting particle width
     * @param height starting particle height
     * @param growth size multiplier applied to width and height at each update
     * @param gravity flag for whether the particle has to be affected by gravity (hence being accelerated towards the bottom of the screen)
     */
    public GUIParticle(Image image, int x, int y, int width, int height, float growth, boolean gravity) {
        Random ran = new Random();

        this.image = image;
        this.x = x;
        this.y = y;
        this.width = width;
        this.height = height;
        this.velX = ran.nextInt(-20, 20);
        this.velY = ran.nextInt(-20, 20);
        this.rotation = ran.nextFloat(0f, (float) (2 * Math.PI));
        this.angular_vel = ran.nextFloat(-0.8f, 0.8f);
        this.growth = growth;
        if (gravity)
            this.accY = 5;
        else
            this.accY = 0;
        this.accX = 0;
    }

    /**
     * {@inheritDoc}
     *
     * @param g instance of {@link Graphics} handling the current repaint
     */
    public void show(Graphics g) {
        if (!toDelete()) {
            BufferedImage transformed_image = new BufferedImage((int) (width * GUIGamePanel.SCREEN_SCALE), (int) (height * GUIGamePanel.SCREEN_SCALE), BufferedImage.TYPE_INT_ARGB);
            Graphics2D image_graphics = (Graphics2D) transformed_image.getGraphics();

            AffineTransform at = new AffineTransform();
            //at.translate(width * GUIGamePanel.SCREEN_SCALE / 2, height * GUIGamePanel.SCREEN_SCALE / 2);
            at.rotate(rotation, width * GUIGamePanel.SCREEN_SCALE / 2, height * GUIGamePanel.SCREEN_SCALE / 2);
            //at.setToScale(GUIGamePanel.SCREEN_SCALE, GUIGamePanel.SCREEN_SCALE);
            image_graphics.setTransform(at);

            image_graphics.drawImage(image, 0, 0, (int) (width * GUIGamePanel.SCREEN_SCALE), (int) (height * GUIGamePanel.SCREEN_SCALE), null);

            g.drawImage(transformed_image, (int) (x * GUIGamePanel.SCREEN_WIDTH_SCALE) - (int) (width * GUIGamePanel.SCREEN_SCALE) / 2,
                    (int) (y * GUIGamePanel.SCREEN_HEIGHT_SCALE) - (int) (height * GUIGamePanel.SCREEN_SCALE) / 2, null);
        }
    }

    //physics are tied to framerate...bad idea usually, but fine in this case!
    /**
     * Updates the particle's position, rotation, velocity and size in function of its acceleration, growth and previous velocity.<br>
     * It has to be called each frame to correctly create a seemingly continuous motion of the particle.
     */
    public void update() {
        x += velX;
        y += velY;
        velX += accX;
        velY += accY;
        rotation += angular_vel;
        if (rotation > (2 * Math.PI))
            rotation -= (2 * Math.PI);
        width *= growth;
        height *= growth;
    }

    /**
     * Check if the particle is still visible, if not then returns true and the particle can be deleted without visible consequences.
     *
     * @return true if the particle can be deleted without the user noticing
     */
    public boolean toDelete() {
        return x < 0 || y < 0 || x > GUIFrame.BASE_WIDTH || y > GUIFrame.BASE_HEIGHT ||
                width * GUIGamePanel.SCREEN_SCALE < 1 || height * GUIGamePanel.SCREEN_SCALE < 1;
    }
}
