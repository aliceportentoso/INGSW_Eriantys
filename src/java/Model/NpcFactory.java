package Model;

import Exceptions.InvalidMoveException;
import org.jetbrains.annotations.TestOnly;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Random;
import java.util.stream.Collectors;

/**
 * Factory class for the specific classes that extend {@link Npc}, those being the 12 distinct npcs in the game.
 * This exists mainly to allow the 12 specific npc classes to remain private.
 */
public class NpcFactory {
    private static final Random ran = new Random();

    /**
     * Static factory method that provides an array of 3 {@link Npc Npcs} ready for an expert instance of {@link Game}.
     * The Npcs are chosen randomly from the pool of 12, and none is chosen more than once.
     *
     * @param game instance of {@link Game} which will receive the Npcs, used to provide the students for the Npcs that need them
     * @return an array of 3 randomly selected {@link Npc Npcs}
     */
    protected static Npc[] factoryMethod(Game game) {
        Npc[] result = new Npc[3];
        List<Integer> already_extracted = new ArrayList<Integer>();

        for(int i = 0; i < result.length; i++) {
            int temp;
            do temp = ran.nextInt(1, 13);
            while(already_extracted.contains(temp));
            already_extracted.add(temp);
            result[i] = switch (temp) {
                case 1 -> new Npc1(game.extractStudents(4));
                case 2 -> new Npc2();
                case 3 -> new Npc3();
                case 4 -> new Npc4();
                case 5 -> new Npc5();
                case 6 -> new Npc6();
                case 7 -> new Npc7(game.extractStudents(6));
                case 8 -> new Npc8();
                case 9 -> new Npc9();
                case 10 -> new Npc10();
                case 11 -> new Npc11(game.extractStudents(4));
                /*case 12,*/ default -> new Npc12();
            };
        }
        return result;
    }

    /**
     * Returns the desired {@link Npc}, eventually consuming students from the specified list (if necessary for the specific npc).
     * @param students list of {@link Colors} representing the students eventually to be used to construct the npc
     * @param index index of the desired npc
     * @return the desired npc
     * @implNote This constructor is meant to be used for testing purposes only.
     */
    @TestOnly
    public static Npc factoryTestMethod(List<Colors> students, int index) {
        return switch (index) {
            case 1 -> {
                List<Colors> temp = new ArrayList<>();
                for (int i = 0; i < 4; i++) temp.add(students.remove(0));
                yield new Npc1(temp);
            }
            case 2 -> new Npc2();
            case 3 -> new Npc3();
            case 4 -> new Npc4();
            case 5 -> new Npc5();
            case 6 -> new Npc6();
            case 7 -> {
                List<Colors> temp = new ArrayList<>();
                for (int i = 0; i < 6; i++) temp.add(students.remove(0));
                yield new Npc7(temp);
            }
            case 8 -> new Npc8();
            case 9 -> new Npc9();
            case 10 -> new Npc10();
            case 11 -> {
                List<Colors> temp = new ArrayList<>();
                for (int i = 0; i < 4; i++) temp.add(students.remove(0));
                yield new Npc11(temp);
            }
            /*case 12,*/ default -> new Npc12();
        };
    }
}

/**
 * Npc number 1:<br>
 * Select 1 student from this card and an island for the student to be placed on, then add 1 student from the pouch to this card.
 * <br><br>
 * See {@link EffectParameters} for the parameter format of {@link Npc1#activateEffect}
 */
class Npc1 extends Npc implements Serializable {
    public static final int uid = 1;
    protected final int args_num = 2;
    public static final int base_cost = 1;
    private int cost;
    private List<Colors> students;

    /**
     * Constructor used by {@link NpcFactory#factoryMethod}
     *
     * @param students students that will end up available on the Npc
     */
    public Npc1(List<Colors> students) {
        this.students = students;
        this.cost = base_cost;
    }

    //1 -> Take 1 student from this card and place it on an island, then add 1 student from the pouch to this card
    //    args.get(0) is the target student index, args.get(1) is the target island
    /**
     * {@inheritDoc}
     */
    public void activateEffect(Game game, EffectParameters effect_parameters) throws InvalidMoveException {
        if(effect_parameters.args.size() < 2 || effect_parameters.args.get(0) >= students.size() || effect_parameters.args.get(0) < 0 || effect_parameters.args.get(1) >= game.islands.size() || effect_parameters.args.get(1) < 0)
            throw new InvalidMoveException("The effect could not be activated due to bad parameters.");

        game.getPlayers().get(game.playerIndexFromID(game.currentlyPlayingPlayer())).pay(this.cost);
        game.bank += this.cost - 1;

        game.islands.get(effect_parameters.args.get(1)).addStudent(students.remove(effect_parameters.args.get(0).intValue()));
        students.addAll(game.extractStudents(1));

        game.npc_effect = uid;
        cost++;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public int getId() {
        return uid;
    }

    /**
     * {@inheritDoc}
     */
    public int getCost() {
        return cost;
    }

    /**
     * {@inheritDoc}
     */
    public List<Integer> getExtraProperty() {
        return students.stream().map(x -> x.index).collect(Collectors.toList());
    }

    /**
     * {@inheritDoc}
     */
    public void setExtraProperty(List<Colors> prop) {
        this.students = new ArrayList<Colors>(prop);
    }

    /**
     * {@inheritDoc}
     */
    public int getArgsNum() {
        return args_num;
    }

    /**
     * Returns the Npc's effect description.
     */
    @Override
    public String toString() {
        return "Take 1 student from this card and place it on an island, then add 1 student from the pouch to this card. Arg 0 is the target student index, Arg 1 is the target island\n" +
                "Args needed: " + args_num;
    }
}

/**
 * Npc number 2:<br>
 * During this turn take control of professors even if you are tied with their current owner.
 * <br><br>
 * See {@link EffectParameters} for the parameter format of {@link Npc2#activateEffect}
 */
class Npc2 extends Npc implements Serializable {
    public static final int uid = 2;
    //private static final boolean resolves_on_activation = false;
    protected final int args_num = 0;
    public static final int base_cost = 2;
    private int cost;

    /**
     * Constructor used by {@link NpcFactory#factoryMethod}
     */
    public Npc2() {
        this.cost = base_cost;
    }

    //2 -> During this turn take control of professors even if you are tied with their current owner
    //    simply set npc_effect in game to this card's UID
    /**
     * {@inheritDoc}
     */
    public void activateEffect(Game game, EffectParameters effect_parameters) throws InvalidMoveException {
        game.getPlayers().get(game.playerIndexFromID(game.currentlyPlayingPlayer())).pay(this.cost);
        game.bank += this.cost - 1;

        game.npc_effect = uid;
        cost++;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public int getId() {
        return uid;
    }

    /**
     * {@inheritDoc}
     */
    public int getCost() {
        return cost;
    }

    /**
     * {@inheritDoc}
     */
    public int getArgsNum() {
        return args_num;
    }

    /**
     * Returns the Npc's effect description.
     */
    @Override
    public String toString() {
        return "During this turn take control of professors even if you are tied with their current owner\n" +
                "Args needed: " + args_num;
    }
}

/**
 * Npc number 3:<br>
 * Select and island and compute it's dispute as if mother nature landed there.
 * <br><br>
 * See {@link EffectParameters} for the parameter format of {@link Npc3#activateEffect}
 */
class Npc3 extends Npc implements Serializable {
    public static final int uid = 3;
    //private static final boolean resolves_on_activation = true;
    protected final int args_num = 1;
    public static final int base_cost = 3;
    private int cost;

    /**
     * Constructor used by {@link NpcFactory#factoryMethod}
     */
    public Npc3() {
        this.cost = base_cost;
    }

    //3 -> Calculate the dispute on a given island as if mother nature landed there (here it is assumed that this triggers any interdiction)
    //    args.get(0) is the island index, the passed to game.disputeIsland(args.get(0)) via the game instance in activateEffect
    //IMPORTANT: it would make sense to force the activation of this effect only before the actual dispute calculation, so that players do not mistakenly activate it before placing students!!!
    /**
     * {@inheritDoc}
     */
    public void activateEffect(Game game, EffectParameters effect_parameters) throws InvalidMoveException {
        if(effect_parameters.args.size() < 1 || effect_parameters.args.get(0) >= game.islands.size() || effect_parameters.args.get(0) < 0)
            throw new InvalidMoveException("The effect could not be activated due to bad parameters.");

        game.getPlayers().get(game.playerIndexFromID(game.currentlyPlayingPlayer())).pay(this.cost);
        game.bank += this.cost - 1;

        game.disputeIsland(effect_parameters.args.get(0));

        game.npc_effect = uid;
        cost++;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public int getId() {
        return uid;
    }

    /**
     * {@inheritDoc}
     */
    public int getCost() {
        return cost;
    }

    /**
     * {@inheritDoc}
     */
    public int getArgsNum() {
        return args_num;
    }

    /**
     * Returns the Npc's effect description.
     */
    @Override
    public String toString() {
        return "Calculate the dispute on a given island as if mother nature landed there, Arg 0 is the island index\n" +
                "Args needed: " + args_num;
    }
}

/**
 * Npc number 4:<br>
 * You can move mother nature by 2 additional islands this turn.
 * <br><br>
 * See {@link EffectParameters} for the parameter format of {@link Npc4#activateEffect}
 */
class Npc4 extends Npc implements Serializable {
    public static final int uid = 4;
    //private static final boolean resolves_on_activation = false;
    protected final int args_num = 0;
    public static final int base_cost = 1;
    private int cost;

    /**
     * Constructor used by {@link NpcFactory#factoryMethod}
     */
    public Npc4() {
        this.cost = base_cost;
    }

    //4 -> You can move mother nature by 2 additional islands
    //        simply set npc_effect in game to this card's UID
    /**
     * {@inheritDoc}
     */
    public void activateEffect(Game game, EffectParameters effect_parameters) throws InvalidMoveException {
        game.getPlayers().get(game.playerIndexFromID(game.currentlyPlayingPlayer())).pay(this.cost);
        game.bank += this.cost - 1;

        game.npc_effect = uid;
        cost++;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public int getId() {
        return uid;
    }

    /**
     * {@inheritDoc}
     */
    public int getCost() {
        return cost;
    }

    /**
     * {@inheritDoc}
     */
    public int getArgsNum() {
        return args_num;
    }

    /**
     * Returns the Npc's effect description.
     */
    @Override
    public String toString() {
        return "You can move mother nature by 2 additional islands\n" +
                "Args needed: " + args_num;
    }
}

/**
 * Npc number 5:<br>
 * Place an interdiction card on the selected island, the first time mother nature lands there you do not dispute the island.
 * <br><br>
 * See {@link EffectParameters} for the parameter format of {@link Npc5#activateEffect}
 */
class Npc5 extends Npc implements Serializable {
    public static final int uid = 5;
    //private static final boolean resolves_on_activation = true;
    protected final int args_num = 1;
    public static final int base_cost = 1;
    private int cost;
    private int interdictions_count;

    /**
     * Constructor used by {@link NpcFactory#factoryMethod}
     */
    public Npc5() {
        this.cost = base_cost;
        this.interdictions_count = 4;
    }

    //5 -> Place an interdiction card on an island, the first time mother nature lands there you do not dispute the island
    //    args.get(0) is the index of the island where to put the interdiction
    /**
     * {@inheritDoc}
     */
    public void activateEffect(Game game, EffectParameters effect_parameters) throws InvalidMoveException {
        if(effect_parameters.args.size() < 1 || effect_parameters.args.get(0) >= game.islands.size() || effect_parameters.args.get(0) < 0)
            throw new InvalidMoveException("The effect could not be activated due to bad parameters.");

        game.getPlayers().get(game.playerIndexFromID(game.currentlyPlayingPlayer())).pay(this.cost);
        game.bank += this.cost - 1;

        game.islands.get(effect_parameters.args.get(0)).setInterdiction(true);
        interdictions_count--;

        game.npc_effect = uid;
        cost++;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public int getId() {
        return uid;
    }

    /**
     * {@inheritDoc}
     */
    public int getCost() {
        return cost;
    }

    /**
     * {@inheritDoc}
     */
    public void extraEffect() {
        interdictions_count++;
    }

    /**
     * {@inheritDoc}
     */
    public List<Integer> getExtraProperty() {
        return new ArrayList<>(List.of(interdictions_count));
    }

    /**
     * {@inheritDoc}
     */
    public int getArgsNum() {
        return args_num;
    }

    /**
     * Returns the Npc's effect description.
     */
    @Override
    public String toString() {
        return "Place an interdiction card on an island, the first time mother nature lands there you do not dispute the island, Arg 0 is the index of the island where to put the interdiction\n" +
                "Args needed: " + args_num;
    }
}

/**
 * Npc number 6:<br>
 * During this turn's island dispute the number of towers is not considered.
 * <br><br>
 * See {@link EffectParameters} for the parameter format of {@link Npc6#activateEffect}
 */
class Npc6 extends Npc implements Serializable {
    public static final int uid = 6;
    //private static final boolean resolves_on_activation = false;
    protected final int args_num = 0;
    public static final int base_cost = 3;
    private int cost;

    /**
     * Constructor used by {@link NpcFactory#factoryMethod}
     */
    public Npc6() {
        this.cost = base_cost;
    }

    //6 -> During this turn's dispute the number of towers is not considered
    //    simply set npc_effect in game to this card's UID
    /**
     * {@inheritDoc}
     */
    public void activateEffect(Game game, EffectParameters effect_parameters) throws InvalidMoveException {
        game.getPlayers().get(game.playerIndexFromID(game.currentlyPlayingPlayer())).pay(this.cost);
        game.bank += this.cost - 1;

        game.npc_effect = uid;
        cost++;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public int getId() {
        return uid;
    }

    /**
     * {@inheritDoc}
     */
    public int getCost() {
        return cost;
    }

    /**
     * {@inheritDoc}
     */
    public int getArgsNum() {
        return args_num;
    }

    /**
     * Returns the Npc's effect description.
     */
    @Override
    public String toString() {
        return "During this turn's dispute the number of towers is not considered\n" +
                "Args needed: " + args_num;
    }
}

/**
 * Npc number 7:<br>
 * Select up to 3 students to take from this card and swap them for the same number of selected students from your dashboard's entrance.
 * <br><br>
 * See {@link EffectParameters} for the parameter format of {@link Npc7#activateEffect}
 */
class Npc7 extends Npc implements Serializable {
    public static final int uid = 7;
    //private static final boolean resolves_on_activation = true;
    protected final int args_num = 3;
    public static final int base_cost = 1;
    private int cost;
    private List<Colors> students;

    /**
     * Constructor used by {@link NpcFactory#factoryMethod}
     *
     * @param students students that will end up available on the Npc
     */
    public Npc7(List<Colors> students) {
        this.cost = base_cost;
        this.students = students;
    }

    //7 -> Take up to 3 students from this card and swap them for the same number from your dashboard's entrance
    //    args.get(0 ... 6), index 0 is the number of students to swap, then from index 1 to number_of_students are the indexes of the students to take from the card,
    //    then from number_of_students + 1 to number_of_students*2 it contains the students in the entrance to replace
    /**
     * {@inheritDoc}
     */
    public void activateEffect(Game game, EffectParameters effect_parameters) throws InvalidMoveException {
        if(effect_parameters.args.size() < 3 || effect_parameters.args.get(0) < 1 || effect_parameters.args.get(0) > 3 || effect_parameters.args.size() < effect_parameters.args.get(0)*2 + 1)
            throw new InvalidMoveException("The effect could not be activated due to bad parameters.");
        for(int i = 0; i < effect_parameters.args.get(0); i++) {
            if(effect_parameters.args.get(i + 1) < 0 || effect_parameters.args.get(i + 1) >= students.size() ||
                    effect_parameters.args.get(i + 1 + effect_parameters.args.get(0)) < 0 ||
                    effect_parameters.args.get(i + 1 + effect_parameters.args.get(0)) >= game.getPlayers().get(game.playerIndexFromID(game.currentlyPlayingPlayer())).getDashboard().getEntrance().size())
                throw new InvalidMoveException("The effect could not be activated due to bad parameters, invalid student indexes.");
            for(int j = 0; j < i; j++) {
                if(effect_parameters.args.get(i + 1) == effect_parameters.args.get(j + 1) || effect_parameters.args.get(i + 1 + effect_parameters.args.get(0)) == effect_parameters.args.get(j + 1 + effect_parameters.args.get(0)))
                    throw new InvalidMoveException("The effect could not be activated due to bad parameters, duplicated index provided.");
            }
        }

        game.getPlayers().get(game.playerIndexFromID(game.currentlyPlayingPlayer())).pay(this.cost);
        game.bank += this.cost - 1;

        List<Colors> swapped_students = new ArrayList<Colors>();

        List<Integer> students_from_card = new ArrayList<Integer>(effect_parameters.args.subList(1, effect_parameters.args.get(0) + 1));
        List<Integer> students_from_entrance = new ArrayList<Integer>(effect_parameters.args.subList(effect_parameters.args.get(0) + 1, effect_parameters.args.get(0)*2 + 1));
        if(effect_parameters.args.get(0) != 1) {
            students_from_card.sort(Collections.reverseOrder());
            students_from_entrance.sort(Collections.reverseOrder());
        }
        for(int i = 0; i < effect_parameters.args.get(0); i++) {
            swapped_students.add(
                    game.getPlayers().get(game.playerIndexFromID(game.currentlyPlayingPlayer())).getDashboard().removeFromEntrance(students_from_entrance.get(i))
            );
            game.getPlayers().get(game.playerIndexFromID(game.currentlyPlayingPlayer())).getDashboard().addToEntrance(students.get(students_from_card.get(i)));
            students.remove(students_from_card.get(i).intValue());
        }

        students.addAll(swapped_students);

        game.npc_effect = uid;
        cost++;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public int getId() {
        return uid;
    }

    /**
     * {@inheritDoc}
     */
    public int getCost() {
        return cost;
    }

    /**
     * {@inheritDoc}
     */
    public List<Integer> getExtraProperty() {
        return students.stream().map(x -> x.index).collect(Collectors.toList());
    }

    /**
     * {@inheritDoc}
     */
    public void setExtraProperty(List<Colors> prop) {
        students = new ArrayList<Colors>(prop);
    }

    /**
     * {@inheritDoc}
     */
    public int getArgsNum() {
        return args_num;
    }

    /**
     * Returns the Npc's effect description.
     */
    @Override
    public String toString() {
        return "Take up to 3 students from this card and swap them for the same number from your dashboard's entrance\n" +
                "Args 0...6, Arg 0 is the number of students to swap, then from 1 to the value of Arg 0 are the indexes of the students to take from the card,\n" +
                "then from the value of Arg 0 + 1 to value of Arg 0 * 2 it contains the students in the entrance to replace\n" +
                "Minimum args needed: " + args_num;
    }
}

/**
 * Npc number 8:<br>
 * During this turn's island dispute you have 2 extra points in your favour.
 * <br><br>
 * See {@link EffectParameters} for the parameter format of {@link Npc8#activateEffect}
 */
class Npc8 extends Npc implements Serializable {
    public static final int uid = 8;
    //private static final boolean resolves_on_activation = false;
    protected final int args_num = 0;
    public static final int base_cost = 2;
    private int cost;

    /**
     * Constructor used by {@link NpcFactory#factoryMethod}
     */
    public Npc8() {
        this.cost = base_cost;
    }

    //8 -> During this turn's dispute you have 2 extra points in your favour
    /**
     * {@inheritDoc}
     */
    public void activateEffect(Game game, EffectParameters effect_parameters) throws InvalidMoveException {
        game.getPlayers().get(game.playerIndexFromID(game.currentlyPlayingPlayer())).pay(this.cost);
        game.bank += this.cost - 1;

        game.npc_effect = uid;
        cost++;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public int getId() {
        return uid;
    }

    /**
     * {@inheritDoc}
     */
    public int getCost() {
        return cost;
    }

    /**
     * {@inheritDoc}
     */
    public int getArgsNum() {
        return args_num;
    }

    /**
     * Returns the Npc's effect description.
     */
    @Override
    public String toString() {
        return "During this turn's dispute you have 2 extra points in your favour\n" +
                "Args needed: " + args_num;
    }
}

/**
 * Npc number 9:<br>
 * Choose a color by selecting its corresponding dashboard row, during this turn's island dispute that color doesn't count towards the total score of any player.
 * <br><br>
 * See {@link EffectParameters} for the parameter format of {@link Npc9#activateEffect}
 */
class Npc9 extends Npc implements Serializable {
    public static final int uid = 9;
    //private static final boolean resolves_on_activation = false;
    protected final int args_num = 1;
    public static final int base_cost = 3;
    private int cost;
    private Colors blocked_color;

    /**
     * Constructor used by {@link NpcFactory#factoryMethod}
     */
    public Npc9() {
        this.cost = base_cost;
        this.blocked_color = null;
    }

    //9 -> Choose a color (args.get(0) from 0 to 4), during this turn's dispute that color doesn't count
    //    towards the total score of any player
    /**
     * {@inheritDoc}
     */
    public void activateEffect(Game game, EffectParameters effect_parameters) throws InvalidMoveException {
        if(effect_parameters.args.size() < 1 || effect_parameters.args.get(0) < 0 || effect_parameters.args.get(0) > 4)
            throw new InvalidMoveException("The effect could not be activated due to bad parameters.");

        game.getPlayers().get(game.playerIndexFromID(game.currentlyPlayingPlayer())).pay(this.cost);
        game.bank += this.cost - 1;

        this.blocked_color = Colors.fromColorIndex(effect_parameters.args.get(0));

        game.npc_effect = uid;
        cost++;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public int getId() {
        return uid;
    }

    /**
     * {@inheritDoc}
     */
    public int getCost() {
        return cost;
    }

    /**
     * {@inheritDoc}
     */
    public List<Integer> getExtraProperty() {
        return new ArrayList<>(blocked_color != null ? List.of(blocked_color.index) : List.of());
    }

    /**
     * {@inheritDoc}
     */
    public int getArgsNum() {
        return args_num;
    }

    /**
     * Returns the Npc's effect description.
     */
    @Override
    public String toString() {
        return "Choose a color via Arg 0 (with a value from 0 to 4 extremes included), during this turn's dispute that color doesn't count towards the total score of any player\n" +
                "Args needed: " + args_num;
    }
}

/**
 * Npc number 10:<br>
 * You can swap between them 2 students, one selected from your hall and one selected from your dashboard's entrance.
 * (Doesn't accredit coins)
 * <br><br>
 * See {@link EffectParameters} for the parameter format of {@link Npc10#activateEffect}
 */
class Npc10 extends Npc implements Serializable {
    public static final int uid = 10;
    //private static final boolean resolves_on_activation = true;
    protected final int args_num = 2;
    public static final int base_cost = 1;
    private int cost;

    /**
     * Constructor used by {@link NpcFactory#factoryMethod}
     */
    public Npc10() {
        this.cost = base_cost;
    }

    //10 -> You can swap between them 2 students, one from your hall and one from your entrance on your dashboard
    //    args.get(0) is the entrance's student index and args.get(1) is the hall's student color
    /**
     * {@inheritDoc}
     */
    public void activateEffect(Game game, EffectParameters effect_parameters) throws InvalidMoveException {
        if(effect_parameters.args.size() < 2 || effect_parameters.args.get(0) < 0 ||
                effect_parameters.args.get(0) >= game.getPlayers().get(game.playerIndexFromID(game.currentlyPlayingPlayer())).getDashboard().getEntrance().size() ||
                effect_parameters.args.get(1) < 0 || effect_parameters.args.get(1) > 4 || game.getPlayers().get(game.playerIndexFromID(game.currentlyPlayingPlayer())).getDashboard().getHallRow(effect_parameters.args.get(1)) == 0)
            throw new InvalidMoveException("The effect could not be activated due to bad parameters.");

        game.getPlayers().get(game.playerIndexFromID(game.currentlyPlayingPlayer())).pay(this.cost);
        game.bank += this.cost - 1;

        game.getPlayers().get(game.playerIndexFromID(game.currentlyPlayingPlayer())).getDashboard().addStudentToHall(
                game.getPlayers().get(game.playerIndexFromID(game.currentlyPlayingPlayer())).getDashboard().removeFromEntrance(effect_parameters.args.get(0))
        );
        game.getPlayers().get(game.playerIndexFromID(game.currentlyPlayingPlayer())).getDashboard().addToEntrance(Colors.fromColorIndex(effect_parameters.args.get(1)));
        game.getPlayers().get(game.playerIndexFromID(game.currentlyPlayingPlayer())).getDashboard().removeStudentFromHall(Colors.fromColorIndex(effect_parameters.args.get(1)));

        game.updateProfessors();

        game.npc_effect = uid;
        cost++;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public int getId() {
        return uid;
    }

    /**
     * {@inheritDoc}
     */
    public int getCost() {
        return cost;
    }

    /**
     * {@inheritDoc}
     */
    public int getArgsNum() {
        return args_num;
    }

    /**
     * Returns the Npc's effect description.
     */
    @Override
    public String toString() {
        return "You can swap between them 2 students, one from your hall and one from your entrance on your dashboard Arg 0 is the entrance's student index and Arg 1 is the hall's student color\n" +
                "Args needed: " + args_num;
    }
}

/**
 * Npc number 11:<br>
 * Select 1 student from this card, and place it in your hall, then place one student from the punch back on this card.<br>
 * (Doesn't accredit coins)
 * <br><br>
 * See {@link EffectParameters} for the parameter format of {@link Npc11#activateEffect}
 */
class Npc11 extends Npc implements Serializable {
    public static final int uid = 11;
    //private static final boolean resolves_on_activation = true;
    protected final int args_num = 1;
    public static final int base_cost = 2;
    private int cost;
    private List<Colors> students;

    /**
     * Constructor used by {@link NpcFactory#factoryMethod}
     */
    public Npc11(List<Colors> students) {
        this.cost = base_cost;
        this.students = students;
    }

    //11 -> Take 1 student from this card (args.get(0)) and place it in your hall, then place one student from the punch
    //    back on this card, args.get(0) is the student index
    /**
     * {@inheritDoc}
     */
    public void activateEffect(Game game, EffectParameters effect_parameters) throws InvalidMoveException {
        if(effect_parameters.args.size() < 1 || effect_parameters.args.get(0) >= students.size() || effect_parameters.args.get(0) < 0)
            throw new InvalidMoveException("The effect could not be activated due to bad parameters.");

        game.getPlayers().get(game.playerIndexFromID(game.currentlyPlayingPlayer())).pay(this.cost);
        game.bank += this.cost - 1;

        int index = effect_parameters.args.get(0);
        game.getPlayers().get(game.playerIndexFromID(game.currentlyPlayingPlayer())).getDashboard().addStudentToHall(students.remove(index));
        students.addAll(game.extractStudents(1));

        game.updateProfessors();

        game.npc_effect = uid;
        cost++;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public int getId() {
        return uid;
    }

    /**
     * {@inheritDoc}
     */
    public int getCost() {
        return cost;
    }

    /**
     * {@inheritDoc}
     */
    public List<Integer> getExtraProperty() {
        return students.stream().map(x -> x.index).collect(Collectors.toList());
    }

    /**
     * {@inheritDoc}
     */
    public void setExtraProperty(List<Colors> prop) {
        students = new ArrayList<Colors>(prop);
    }

    /**
     * {@inheritDoc}
     */
    public int getArgsNum() {
        return args_num;
    }

    /**
     * Returns the Npc's effect description.
     */
    @Override
    public String toString() {
        return "Take 1 student from this card, indexed by Arg 0, and place it in your hall, then place one student from the punch back on this card\n" +
                "Args needed: " + args_num;
    }
}

/**
 * Npc number 12:<br>
 * Choose a color by selecting its corresponding dashboard row, each player, you included, has to put back in the pouch 3 students of that color from his dashboard's hall, if he has less than 3, he just puts back as many as he has.
 * <br><br>
 * See {@link EffectParameters} for the parameter format of {@link Npc12#activateEffect}
 */
class Npc12 extends Npc implements Serializable {
    public static final int uid = 12;
    //private static final boolean resolves_on_activation = true;
    protected final int args_num = 1;
    public static final int base_cost = 3;
    private int cost;

    /**
     * Constructor used by {@link NpcFactory#factoryMethod}
     */
    public Npc12() {
        this.cost = base_cost;
    }

    //Choose a color (args.get(0) from 0 to 4), each player, you included, has to put back in the pouch 3
    //    students of that color from his hall on his dashboard, if there are less, he only puts back as many as possible
    /**
     * {@inheritDoc}
     */
    public void activateEffect(Game game, EffectParameters effect_parameters) throws InvalidMoveException {
        if(effect_parameters.args.size() < 1 || effect_parameters.args.get(0) < 0 || effect_parameters.args.get(0) > 4)
            throw new InvalidMoveException("The effect could not be activated due to bad parameters.");

        game.getPlayers().get(game.playerIndexFromID(game.currentlyPlayingPlayer())).pay(this.cost);
        game.bank += this.cost - 1;

        //Important: this is intended NOT to undo an eventual true last_game_turn, since it would
        //be impossible to determine where to put each refurbished student!

        for(Player player : game.getPlayers()) {
            for(int i = 0; i < 3; i++) {
                if(player.getDashboard().removeStudentFromHall(Colors.fromColorIndex(effect_parameters.args.get(0))))
                    game.random_students.add(Colors.fromColorIndex(effect_parameters.args.get(0)));
            }
            Collections.shuffle(game.random_students);
        }

        game.updateProfessors();

        game.npc_effect = uid;
        cost++;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public int getId() {
        return uid;
    }

    /**
     * {@inheritDoc}
     */
    public int getCost() {
        return cost;
    }

    /**
     * {@inheritDoc}
     */
    public int getArgsNum() {
        return args_num;
    }

    /**
     * Returns the Npc's effect description.
     */
    @Override
    public String toString() {
        return "Choose a color via Arg 0 (with a value from 0 to 4 extremes included), each player, you included, has to put back in the pouch 3 students of that color from his hall on his dashboard, if there are less, he only puts back as many as possible\n" +
                "Args needed: " + args_num;
    }
}

/*
class NpcTemplate extends Npc implements Serializable {
    public static final int uid = 0;
    //private static final boolean resolves_on_activation = true;
    protected final int args_num = 0;
    public static final int base_cost = 0;
    private int cost;

    public NpcTemplate() {
        this.cost = base_cost;
    }

    //Effect description
    public void activateEffect(Game game, EffectParameters effect_parameters) throws InvalidMoveException {
        if(//check parameters//)
            throw new InvalidMoveException("The effect could not be activated due to bad parameters.");
        //additional checks

        game.getPlayers().get(game.playerIndexFromID(game.currentlyPlayingPlayer())).pay(this.cost);
        game.bank += this.cost - 1;

        //logic

        //set npc_effect even if the effect resolves instantly, to prevent further activations
        game.npc_effect = uid;
        cost++;
    }

    public int getId() {
        return uid;
    }

    public int getCost() {
        return cost;
    }

    public void extraEffect() {
        return;
    }

    public int getArgsNum() {
        return args_num;
    }
    
    public List<Integer> getExtraProperty() {
        return null;
    }
}
*/