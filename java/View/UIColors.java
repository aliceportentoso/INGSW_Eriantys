package View;

/**
 * Enum of colors used in {@link UI#showMessage}.<br>
 * Allows the {@link Client} to reference a color regardless of if it's using the {@link View.CLI.CLI CLI} or {@link View.GUI.GUI GUI},
 * since the colors in this enum have associated to them both their {@link org.fusesource.jansi.Ansi.Color} equivalent and their {@link java.awt.Color} equivalent.
 */
public enum UIColors {
    RED(java.awt.Color.red, org.fusesource.jansi.Ansi.Color.RED),
    GREEN(java.awt.Color.green, org.fusesource.jansi.Ansi.Color.GREEN),
    BLUE(java.awt.Color.blue, org.fusesource.jansi.Ansi.Color.BLUE),
    CYAN(java.awt.Color.cyan, org.fusesource.jansi.Ansi.Color.CYAN),
    YELLOW(java.awt.Color.yellow, org.fusesource.jansi.Ansi.Color.YELLOW),
    GRAY(java.awt.Color.gray, org.fusesource.jansi.Ansi.Color.WHITE),
    WHITE(java.awt.Color.white, org.fusesource.jansi.Ansi.Color.WHITE);

    public final java.awt.Color awtColor;
    public final org.fusesource.jansi.Ansi.Color ansiColor;

    /**
     * Enum constructor for its colors, it associates at each color its {@link org.fusesource.jansi.Ansi.Color} and its {@link java.awt.Color} equivalent.
     *
     * @param awtColor {@link java.awt.Color} equivalent
     * @param ansiColor {@link org.fusesource.jansi.Ansi.Color} equivalent
     */
    UIColors(java.awt.Color awtColor, org.fusesource.jansi.Ansi.Color ansiColor) {
        this.awtColor = awtColor;
        this.ansiColor = ansiColor;
    }
}