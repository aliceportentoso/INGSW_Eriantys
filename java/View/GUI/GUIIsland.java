package View.GUI;

import Controller.ClientSide.Client;
import Model.Colors;
import Model.GameState;
import Model.Island;

import java.awt.*;
import java.awt.event.MouseEvent;
import java.awt.event.MouseListener;
import java.util.ArrayList;

/**
 * {@link GUIElement} representing an island of the game.<br>
 * A click on a island implies, if a {@link GUIEntranceStudent} is selected, moves it to it, if allowed by the client's model instance.
 * <br><br>
 * Every island doesn't represent a specific instance of {@link Model.Island Island} of the model, but it represents the island in the provided
 * position (index) among those in the model, as such the specific instance of {@link Model.Island Island} referenced can change during a game.
 */
public class GUIIsland extends GUIElement implements MouseListener {
    //index of the island it represents from the game, starts at 0
    protected int index;
    //reference to the client, used for both choosing when to draw and when to handle events
    protected Client client;
    //reference to the game panel this element is in
    protected GUIGamePanel gui_panel;
    //coordinates for students
    private static final int[][] coordinates = {{18,5},{34,5},{50,5},{8,20},{24,20},{40,20},{56,20},{2,35},{18,35},{34,35},{50,35},{66,35}};

    /**
     * Instantiates a new island with the provided coordinates and dimensions, if the constructed island's {@link GUIIsland#show} method
     * is then called at each repaint, the island will always be displayed on the GUI correctly.
     *
     * @param client reference client for the UI
     * @param gui_panel reference {@link GUIGamePanel} for this {@link GUIElement}
     * @param x to left corner X coordinate for this island
     * @param y to left corner Y coordinate for this island
     * @param width width for this island
     * @param height height for this island
     * @param index index of the island represented out of the 10 potentially available in the model
     */
    public GUIIsland(Client client, GUIGamePanel gui_panel, Image image, int x, int y, int width, int height, int index) {
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
        Graphics2D g2d = (Graphics2D) g;
        GameState gs = client.getGameState();

        //index of the island in the array provided by gamestate
        int actual_index;
        int islands_total = 0;
        if(gs.getIslands().get(0).index > index) {
            actual_index = gs.getIslands().size() - 1;
        } else {
            for(actual_index = 0; actual_index < gs.getIslands().size(); actual_index++) {
                islands_total += gs.getIslands().get(actual_index).getNumOfMergedIslands();
                if(islands_total >= index + 1 - gs.getIslands().get(0).index) {
                    break;
                }
            }
        }

        if(gs.getPhase() == 1 && gs.isMyTurn(client.getClientID())) {

            //if an NPC that requires an island is being activated, ignore everything else and go along with the NPC island selection
            if(client.getLobby().expert_mode && (gui_panel.getActivatedNpcId() == 1 ||
                    gui_panel.getActivatedNpcId() == 3 || gui_panel.getActivatedNpcId() == 5)) {

                //if selected, draw an aura
                if(gui_panel.getSelectedNpcIsland() == actual_index) {
                    g2d.setColor(Color.decode("#ff9999"));
                    g2d.fillOval((int) ((x - 5) * GUIGamePanel.SCREEN_WIDTH_SCALE), (int) ((y - 5) * GUIGamePanel.SCREEN_HEIGHT_SCALE),
                            (int) ((width + 10) * GUIGamePanel.SCREEN_SCALE), (int) ((height + 10) * GUIGamePanel.SCREEN_SCALE));
                } else {
                    g2d.setColor(Color.decode("#ccffff"));
                    g2d.fillOval((int) ((x - 5) * GUIGamePanel.SCREEN_WIDTH_SCALE), (int) ((y - 5) * GUIGamePanel.SCREEN_HEIGHT_SCALE),
                            (int) ((width + 10) * GUIGamePanel.SCREEN_SCALE), (int) ((height + 10) * GUIGamePanel.SCREEN_SCALE));
                }

                drawHover(g2d);
            } else {

                //SET STUDENT
                if(gs.getStep() == 0 && actual_index < gs.getIslands().size() && gui_panel.getSelectedStudentIndex() != -1 && gui_panel.getActivatedNpc() == -1) {

                    //draw aura for when a student can be placed on the island
                    g2d.setColor(Color.decode("#ccffff"));
                    g2d.fillOval((int) ((x - 5) * GUIGamePanel.SCREEN_WIDTH_SCALE), (int) ((y - 5) * GUIGamePanel.SCREEN_HEIGHT_SCALE),
                            (int) ((width + 10) * GUIGamePanel.SCREEN_SCALE), (int) ((height + 10) * GUIGamePanel.SCREEN_SCALE));

                    drawHover(g2d);
                }

                //MOVE MOTHER NATURE
                //handle NPC 4's extra mother nature steps
                int allowed_steps = gs.getNpcEffect() == 4 ? 2 + gs.myPlayer(client).getLastCardPlayed().movements_value : gs.myPlayer(client).getLastCardPlayed().movements_value;
                if(gs.getStep() == 1 && gui_panel.getActivatedNpcId() == -1 && ((actual_index <= (gs.getMotherNature() + allowed_steps) % gs.getIslands().size() &&
                        (gs.getMotherNature() + allowed_steps) >= gs.getIslands().size()) || (actual_index > gs.getMotherNature() &&
                        actual_index <= (gs.getMotherNature() + allowed_steps)))) {

                    //draw aura for when mother nature ca be moved
                    g2d.setColor(Color.decode("#ffffe6"));
                    g2d.fillOval((int) ((x - 5) * GUIGamePanel.SCREEN_WIDTH_SCALE), (int) ((y - 5) * GUIGamePanel.SCREEN_HEIGHT_SCALE),
                            (int) ((width + 10) * GUIGamePanel.SCREEN_SCALE), (int) ((height + 10) * GUIGamePanel.SCREEN_SCALE));

                    drawHover(g2d);
                }
            }
        }


        super.show(g);
    }

    //method used to separate the drawing of the island and its backgrounds from that the elements on top of it, to allow bridges to be drawn in between
    /**
     * Secondary show method intended to show some of this island's features at the end the {@link GUIGamePanel}'s show method, hence on top of everything else.
     */
    public void showDataOnTop(Graphics g) {
        GameState gs = client.getGameState();

        //index of the island in the array provided by gamestate
        int actual_index;
        int islands_total = 0;
        if(gs.getIslands().get(0).index > index) {
            actual_index = gs.getIslands().size() - 1;
        } else {
            for(actual_index = 0; actual_index < gs.getIslands().size(); actual_index++) {
                islands_total += gs.getIslands().get(actual_index).getNumOfMergedIslands();
                if(islands_total >= index + 1 - gs.getIslands().get(0).index) {
                    break;
                }
            }
        }

        Island island = gs.getIslands().get(actual_index);

        //draw owner (the one who has the rook on the island)
        if(island.getOwnerIndex() != null) {
            g.drawImage(GUIGamePanel.rook_images[island.getOwnerIndex()],(int) ((x+5) * GUIGamePanel.SCREEN_WIDTH_SCALE) + ((int) (width * GUIGamePanel.SCREEN_SCALE / 5)),
                    (int) ((y+10) * GUIGamePanel.SCREEN_HEIGHT_SCALE) + ((int) (height * GUIGamePanel.SCREEN_SCALE / 1.9)),
                    (int) (52/3.5 * GUIGamePanel.SCREEN_SCALE), (int) ( 99/3.5 * GUIGamePanel.SCREEN_SCALE), null);
        }

        //draw students, mother nature and interdiction only on one island in a group of merged islands
        if(index == island.index) {

            int countStudents = 0;
            for (int i = 0; i < 5; i++) {    //for each color
                for (int j = 0; j < island.getStudents(i); j++)
                    countStudents++;
            }

            //draw students
            if (countStudents < 6){
                int k = 0;
                for (int i = 0; i < 5; i++) {    //for each color
                    for (int j = 0; j < island.getStudents(i); j++) {   //for each student
                        if (k < 2) g.drawImage(GUIGamePanel.student_images.get(Colors.fromColorIndex(i)),
                                (int) (x * GUIGamePanel.SCREEN_WIDTH_SCALE + (23 + 23 * (k % 3)) * GUIGamePanel.SCREEN_SCALE),
                                (int) (y  * GUIGamePanel.SCREEN_HEIGHT_SCALE + 5 * GUIGamePanel.SCREEN_SCALE),
                                (int) (width * GUIGamePanel.SCREEN_SCALE / 5.5),
                                (int) (height * GUIGamePanel.SCREEN_SCALE / 5.5), null);
                        else if (k < 5) g.drawImage(GUIGamePanel.student_images.get(Colors.fromColorIndex(i)),
                                (int) (x * GUIGamePanel.SCREEN_WIDTH_SCALE + (10 + 23 * (k % 3)) * GUIGamePanel.SCREEN_SCALE),
                                (int) (y * GUIGamePanel.SCREEN_HEIGHT_SCALE + 25 * GUIGamePanel.SCREEN_SCALE),
                                (int) (width * GUIGamePanel.SCREEN_SCALE / 5.5),
                                (int) (height * GUIGamePanel.SCREEN_SCALE / 5.5), null);
                        k++;
                    }
                }
            //draw compacted students
            } else if (countStudents < 13){
                int k = 0;
                for (int i = 0; i < 5; i++) {    //for each color
                    for (int j = 0; j < island.getStudents(i); j++) {   //for each student
                        g.drawImage(GUIGamePanel.student_images.get(Colors.fromColorIndex(i)),
                                (int) (x * GUIGamePanel.SCREEN_WIDTH_SCALE + coordinates[k][0] * GUIGamePanel.SCREEN_SCALE),
                                (int) (y * GUIGamePanel.SCREEN_HEIGHT_SCALE + coordinates[k][1] * GUIGamePanel.SCREEN_SCALE),
                                (int) (width * GUIGamePanel.SCREEN_SCALE / 5.8),
                                (int) (height * GUIGamePanel.SCREEN_SCALE / 5.8), null);
                        k++;
                    }
                }
            //draw the number of students on the island
            } else {
                for (int i = 0; i < 5; i++) {
                    g.drawImage(GUIGamePanel.student_images.get(Colors.fromColorIndex(i)),
                            (int) (x * GUIGamePanel.SCREEN_WIDTH_SCALE + (15 * i) * GUIGamePanel.SCREEN_SCALE),
                            (int) (y * GUIGamePanel.SCREEN_HEIGHT_SCALE + 25 * GUIGamePanel.SCREEN_SCALE),
                            (int) (width * GUIGamePanel.SCREEN_SCALE / 5.5),
                            (int) (height * GUIGamePanel.SCREEN_SCALE / 5.5), null);
                    g.setColor(Color.black);
                    g.drawString(String.valueOf(island.getStudents(i)),
                            (int) (x * GUIGamePanel.SCREEN_WIDTH_SCALE + 15 * i * GUIGamePanel.SCREEN_SCALE),
                            (int) (y * GUIGamePanel.SCREEN_HEIGHT_SCALE + 25 * GUIGamePanel.SCREEN_SCALE));
                }
            }

            //draw mother nature, if present
            if (gs.getMotherNature() == actual_index) {
                g.drawImage(GUIGamePanel.mother_nature_image, (int) (x * GUIGamePanel.SCREEN_WIDTH_SCALE) + 2 * ((int) (width * GUIGamePanel.SCREEN_SCALE / 5)),
                        (int) ((y + 20) * GUIGamePanel.SCREEN_HEIGHT_SCALE) + 3 * ((int) (height * GUIGamePanel.SCREEN_SCALE / 4)) - (int) (30 * GUIGamePanel.SCREEN_SCALE),
                        (int) (width * GUIGamePanel.SCREEN_SCALE / 2.5), (int) (height * GUIGamePanel.SCREEN_SCALE / 2.5), null);
            }

            //draw interdiction, if any
            if (island.getInterdiction()) {
                g.setColor(Color.black);
                g.drawString("Int.",
                        (int) (x * GUIGamePanel.SCREEN_WIDTH_SCALE + ((int) (width * GUIGamePanel.SCREEN_SCALE / 10))),
                        (int) (y * GUIGamePanel.SCREEN_HEIGHT_SCALE) + ((int) (4 * height * GUIGamePanel.SCREEN_SCALE / 5)));
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

        //index of the island in the array provided by gamestate
        int actual_index;
        int islands_total = 0;
        if(gs.getIslands().get(0).index > index) {
            actual_index = gs.getIslands().size() - 1;
        } else {
            for(actual_index = 0; actual_index < gs.getIslands().size(); actual_index++) {
                islands_total += gs.getIslands().get(actual_index).getNumOfMergedIslands();
                if(islands_total >= index + 1 - gs.getIslands().get(0).index) {
                    break;
                }
            }
        }

        //if an NPC that requires an island is being activated, ignore everything else and go along with the NPC island selection
        if(client.getLobby().expert_mode && (gui_panel.getActivatedNpcId() == 1 ||
                gui_panel.getActivatedNpcId() == 3 || gui_panel.getActivatedNpcId() == 5)) {

            if(inBounds(e.getX(), e.getY()))
                gui_panel.setSelectedNpcIsland(actual_index);
        } else if(gs.getPhase() == 1 && inBounds(e.getX(), e.getY()) && gs.isMyTurn(client.getClientID())) {

            //set student on this island on click
            if(gui_panel.getActivatedNpc() == -1 &&
                    actual_index < gs.getIslands().size() &&
                    gs.getStep() == 0 && gui_panel.getSelectedStudentIndex() != -1) {

                client.setStudentToIsland(gui_panel.getSelectedStudentIndex(), actual_index);
                gui_panel.setSelectedStudentIndex(-1);

                //create particles
                ArrayList<GUIParticle> particles = new ArrayList<GUIParticle>();
                for(int i = 0; i < 6; i++)
                    particles.add(new GUIParticle(GUIGamePanel.start_particle, x + width / 2, y + height / 2,
                            GUIGamePanel.PARTICLE_SIZE, GUIGamePanel.PARTICLE_SIZE, 0.92f, true));
                gui_panel.addParticles(particles);
            }

            //move mother nature on click
            if(gs.getStep() == 1 && gui_panel.getActivatedNpc() == -1) {
                //handle NPC 4's extra mother nature steps
                int allowed_steps = gs.getNpcEffect() == 4 ? 2 + gs.myPlayer(client).getLastCardPlayed().movements_value : gs.myPlayer(client).getLastCardPlayed().movements_value;
                if((actual_index <= (gs.getMotherNature() + allowed_steps) % gs.getIslands().size() &&
                        (gs.getMotherNature() + allowed_steps) >= gs.getIslands().size()) || (actual_index > gs.getMotherNature() &&
                        actual_index <= (gs.getMotherNature() + allowed_steps))) {
                    client.moveMotherNature((actual_index - gs.getMotherNature() + gs.getIslands().size()) % gs.getIslands().size());

                    //create particles
                    ArrayList<GUIParticle> particles = new ArrayList<GUIParticle>();
                    for(int i = 0; i < 6; i++)
                        particles.add(new GUIParticle(GUIGamePanel.dust_particle, x + width / 2, y + height / 2,
                                GUIGamePanel.PARTICLE_SIZE, GUIGamePanel.PARTICLE_SIZE, 0.80f, false));
                    gui_panel.addParticles(particles);
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
