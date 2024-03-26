package Model;

import Controller.ClientSide.Client;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

/**
 * Read only <strong>PROXY</strong> for the {@link Game} class.<br>
 * Provides all the getters present in Game, while adding a few extra convenient ones.
 * <br><br>
 * Intended for use by the UI.
 */
public class GameState {
    private final Game game;

    public GameState(Game game) {
        this.game = game;
    }

    //returns the Player instance associated to the calling client
    /**
     * Provides the {@link Player player} which in the game corresponds to the provided client.
     *
     * @param client {@link Client client} who's associated player will be fetched
     * @return the {@link Player player} representing to the provided client
     */
    public Player myPlayer(Client client) {
        for(Player player : game.getPlayers())
            if(player.clientID == client.getClientID())
                return player;
        return null;
    }

    //returns only the players that are not the client
    /**
     * Provides all the {@link Player player} which in the game do not correspond to the provided client.
     *
     * @param client {@link Client client} used to return every player except his one
     * @return the list of {@link Player players} representing to opponents to the provided client.
     */
    public List<Player> otherPlayers(Client client) {
        List<Player> result = new ArrayList<Player>();
        for(Player player : game.getPlayers())
            if(player.clientID != client.getClientID())
                result.add(player);
        return result;
    }

    /**
     * See: {@link Game#isMyTurn(int)}
     */
    public boolean isMyTurn(int clientID) {
        return game.isMyTurn(clientID);
    }

    /**
     * See: {@link Game#currentlyPlayingPlayer()}
     */
    public int currentlyPlayingPlayer() {
        return game.currentlyPlayingPlayer();
    }

    /**
     * See: {@link Game#currentPlayersTurnOrder()}
     */
    public List<Integer> currentPlayersTurnOrder() {
        return game.currentPlayersTurnOrder();
    }

    /**
     * See: {@link Game#getPlayers()}
     */
    public List<Player> getPlayers() {
        return game.getPlayers();
    }

    /**
     * See: {@link Game#getGameTurn()}
     */
    public int getGameTurn() {
        return game.getGameTurn();
    }

    /**
     * See: {@link Game#getPhase()}
     */
    public int getPhase() {
        return game.getPhase();
    }

    /**
     * See: {@link Game#getPlayerTurn()}
     */
    public int getPlayerTurn() {
        return game.getPlayerTurn();
    }

    /**
     * See: {@link Game#getStep()}
     */
    public int getStep() {
        return game.getStep();
    }

    /**
     * See: {@link Game#getMovedStudents()}
     */
    public int getMovedStudents() {
        return game.getMovedStudents();
    }

    /**
     * See: {@link Game#getClouds()}
     */
    public List<Colors>[] getClouds() {
        return game.getClouds();
    }

    /**
     * See: {@link Game#getMotherNature()}
     */
    public int getMotherNature() {
        return game.getMotherNature();
    }

    /**
     * See: {@link Game#getIslands()}
     */
    public List<Island> getIslands() {
        return game.getIslands();
    }

    /**
     * See: {@link Game#getUnclaimedProfessors()}
     */
    public List<Colors> getUnclaimedProfessors() {
        return game.getUnclaimedProfessors();
    }

    /**
     * See: {@link Game#getNpcs()}
     */
    public Npc[] getNpcs() {
        return game.getNpcs();
    }

    /**
     * Returns the {@link Npc} with the specified id if present in the current game, otherwise returns null.
     *
     * @param id id of the {@link Npc} to search for
     * @return the {@link Npc} with the provided id, if present, otherwise null
     */
    public Npc getNpcWithId(int id) {
        for(Npc npc : game.getNpcs())
            if(npc.getId() == id)
                return npc;
        return null;
    }

    /**
     * See: {@link Game#getNpcEffect()}
     */
    public int getNpcEffect() {
        return game.getNpcEffect();
    }

    /**
     * See: {@link Game#getBank()}
     */
    public int getBank() {
        return game.getBank();
    }

    /**
     * See: {@link Game#getRemainingStudentsNum()}
     */
    public int getRemainingStudentsNum() {
        return game.getRemainingStudentsNum();
    }

    /**
     * See: {@link Game#isLastGameTurn()}
     */
    public boolean isLastGameTurn() {
        return game.isLastGameTurn();
    }

    /**
     * See: {@link Game#isGameEnded()}
     */
    public boolean isGameEnded() {
        return game.isGameEnded();
    }

    /**
     * See: {@link Game#getWinnerID()}
     */
    public int getWinnerID() {
        return game.getWinnerID();
    }

    /**
     * Returns a string describing entirely the current game state.
     * This is the main source of data for the CLI during a game.
     *
     * @param client {@link Client client} used to make his corresponding player
     * @return string describing the entirety of the game state
     */
    public String toString(Client client) {
        StringBuilder result = new StringBuilder();

        result.append("@|bold Game turn: ").append(game.getGameTurn()).append("\n");
        result.append("Phase: ").append(game.getPhase()).append("\n");
        result.append("Expert mode: ").append(game.expert_mode).append("\n");
        result.append("Remaining students (pouch): ").append(game.getRemainingStudentsNum()).append("\n");
        if(game.currentlyPlayingPlayer() == client.getClientID())
            result.append("Currently playing player:|@ @|blue ").append(client.clientIDToNickname(game.currentlyPlayingPlayer())).append("|@\n");
        else
            result.append("Currently playing player:|@ @|red ").append(client.clientIDToNickname(game.currentlyPlayingPlayer())).append("|@\n");
        /*result += "@|bold Players nicknames:|@ ";
        for(Player player : game.getPlayers()) {
            if(player.clientID == client.getClientID())
                result += "@|blue " + client.clientIDToNickname(player.clientID) + "|@ ";
            else
                result += "@|red " + client.clientIDToNickname(player.clientID) + "|@ ";

        }*/
        result.append("\n@|bold Turn order (first to last):|@ ");
        for(int id : game.currentPlayersTurnOrder()) {
            if(id == client.getClientID())
                result.append("@|blue ").append(client.clientIDToNickname(id)).append("|@ ");
            else
                result.append("@|red ").append(client.clientIDToNickname(id)).append("|@ ");
        }

        result.append("\n\n");
        result.append("Clouds: " + "\n");
        for(int i = 0; i < game.getClouds().length; i++) {
            if(game.getClouds()[i] != null) {
                for (Colors color : game.getClouds()[i]) {
                    switch(color) {
                        case YELLOW -> result.append("@|bg_yellow  ").append(i).append(" |@ ");
                        case BLUE -> result.append("@|bg_blue  ").append(i).append(" |@ ");
                        case GREEN -> result.append("@|bg_green  ").append(i).append(" |@ ");
                        case RED -> result.append("@|bg_red  ").append(i).append(" |@ ");
                        case MAGENTA -> result.append("@|bg_magenta  ").append(i).append(" |@ ");
                    }
                }
                result.append("\n");
            }
        }
        result.append("\n@|bold Mother nature is on island:|@ @|italic ").append(game.getMotherNature()).append("|@\n");
        result.append("@|bold Islands:|@" + "\n\n");
        result.append(game.expert_mode ? "@|faint index\t\tstudents\tmerges\t\tinterdicted\towner|@\n" : "@|faint index\t\tstudents\tmerges\t\towner|@\n");
        for(int i = 0; i < game.getIslands().size(); i++) {
            String row = i + "\t\t";

            Island island = game.getIslands().get(i);

            row += "@|fg_yellow " + island.getStudents(0) +
                    "|@@|fg_blue " + island.getStudents(1) +
                    "|@@|fg_green " + island.getStudents(2) +
                    "|@@|fg_red " + island.getStudents(3) +
                    "|@@|fg_magenta " + island.getStudents(4) + "|@\t\t";
            row += island.getNumOfMergedIslands() + "\t\t";
            if(game.expert_mode)
                row += island.getInterdiction() ? "yes" : "no" + "\t\t";

            if(island.getOwnerIndex() != null) {
                if(game.getPlayers().get(island.getOwnerIndex()).clientID == client.getClientID())
                    row += "@|blue " + client.clientIDToNickname(game.getPlayers().get(island.getOwnerIndex()).clientID) + "|@";
                else
                    row += "@|red " + client.clientIDToNickname(game.getPlayers().get(island.getOwnerIndex()).clientID) + "|@";
            } else
                row += "@|faint none|@";

            if(game.getMotherNature() == i)
                row += " <- @|bold MN|@";

            result.append(row).append("\n");
        }
        result.append("\n");
        result.append("Unclaimed professors: ");
        for(Colors color : game.getUnclaimedProfessors()) {
            switch(color) {
                case YELLOW -> result.append("@|fg_yellow P|@ ");
                case BLUE -> result.append("@|fg_blue P|@ ");
                case GREEN -> result.append("@|fg_green P|@ ");
                case RED -> result.append("@|fg_red P|@ ");
                case MAGENTA -> result.append("@|fg_magenta P|@ ");
            }
        }
        result.append("\n");

        if(game.expert_mode) {
            result.append("NPCs id list: ");
            for(Npc npc : game.getNpcs()) {
                result.append("@|cyan ").append(npc.getId()).append("|@ ");
                switch(npc.getId()) {
                    case 1, 7, 11 -> result.append("(").append(npc.getExtraProperty().stream().map(x -> switch (Colors.fromColorIndex(x)) {
                        case YELLOW -> "@|fg_yellow S|@";
                        case BLUE -> "@|fg_blue S|@";
                        case GREEN -> "@|fg_green S|@";
                        case RED -> "@|fg_red S|@";
                        case MAGENTA -> "@|fg_magenta S|@";
                    }).collect(Collectors.joining(", "))).append(") ");
                    case 5 -> result.append("(").append(npc.getExtraProperty().get(0)).append(") ");
                    case 9 -> {
                        if(npc.getExtraProperty().size() > 0) {
                            result.append("(");
                            switch(Colors.fromColorIndex(npc.getExtraProperty().get(0))) {
                                case YELLOW -> result.append("@|fg_yellow blocked|@");
                                case BLUE -> result.append("@|fg_blue blocked|@");
                                case GREEN -> result.append("@|fg_green blocked|@");
                                case RED -> result.append("@|fg_red blocked|@");
                                case MAGENTA -> result.append("@|fg_magenta blocked|@");
                            }
                            result.append(") ");
                        }
                    }
                }
            }
            result.append("\n");
            result.append("NPCs costs: ");
            for(Npc npc : game.getNpcs())
                result.append("@|cyan ").append(npc.getCost()).append("|@ ");
            result.append("\n");
            result.append("Active NPC: @|cyan ").append(game.getNpcEffect()).append("|@\n");
            result.append("Bank: @|yellow ").append(game.getBank()).append("|@\n");
        }

        if(getWinnerID() != 0)
            result.append("Winner: ").append(client.clientIDToNickname(game.getWinnerID())).append("\n\n");

        result.append("\n@|bold Players:|@" + "\n\n");
        for(Player player : game.getPlayers()) {
            if(player.clientID == client.getClientID())
                result.append("@|blink_fast YOU|@: @|blue ").append(client.clientIDToNickname(player.clientID)).append("|@\n");
            else
                result.append("@|faint player:|@ @|red ").append(client.clientIDToNickname(player.clientID)).append("|@\n");

            result.append("Cards in hand:\nOrder values ->   ");
            for(Card card : player.getCards())
                result.append("\t").append(card.order_value);
            result.append("\nMovement values ->");
            for(Card card : player.getCards())
                result.append("\t").append(card.movements_value);
            result.append("\n");

            if(player.getLastCardPlayed() != null)
                result.append("Last card played: (" + "Order value: ").append(player.getLastCardPlayed().order_value).append(", Movement value: ").append(player.getLastCardPlayed().movements_value).append(")\n");
            if(game.expert_mode)
                result.append("Coins: ").append(player.getCoins()).append("\n");
            Dashboard dashboard = player.getDashboard();
            result.append("Entrance: ");
            for(int i = 0; i < dashboard.getEntrance().size(); i++) {
                switch(dashboard.getEntrance().get(i)) {
                    case YELLOW -> result.append("@|bg_yellow  ").append(i).append(" |@ ");
                    case BLUE -> result.append("@|bg_blue  ").append(i).append(" |@ ");
                    case GREEN -> result.append("@|bg_green  ").append(i).append(" |@ ");
                    case RED -> result.append("@|bg_red  ").append(i).append(" |@ ");
                    case MAGENTA -> result.append("@|bg_magenta  ").append(i).append(" |@ ");
                }
            }
            result.append("\nHall: ");
            /*for(Colors color : Colors.values())
                result += color.name() + " - " + dashboard.getHallRow(color) + ", ";*/
            result.append("@|fg_yellow ").append(dashboard.getHallRow(Colors.YELLOW)).append("|@").append("@|fg_blue ").append(dashboard.getHallRow(Colors.BLUE)).append("|@").append("@|fg_green ").append(dashboard.getHallRow(Colors.GREEN)).append("|@").append("@|fg_red ").append(dashboard.getHallRow(Colors.RED)).append("|@").append("@|fg_magenta ").append(dashboard.getHallRow(Colors.MAGENTA)).append("|@");
            result.append("\nProfessors: ");
            /*for(Colors color : Colors.values())
                result += color.name() + " - " + dashboard.getProfessor(color) + ", ";*/
            result.append(dashboard.getProfessor(Colors.YELLOW) ? "@|fg_yellow P|@ " : "").append(dashboard.getProfessor(Colors.BLUE) ? "@|fg_blue P|@ " : "").append(dashboard.getProfessor(Colors.GREEN) ? "@|fg_green P|@ " : "").append(dashboard.getProfessor(Colors.RED) ? "@|fg_red P|@ " : "").append(dashboard.getProfessor(Colors.MAGENTA) ? "@|fg_magenta P|@ " : "");
            result.append("\nRooks: ").append(dashboard.getRooks());
            result.append("\n\n\n");
        }

        result.append("@|underline_double ");
        if(game.isMyTurn(client.getClientID())){
            result.append("It's your turn: ");
            if(game.getPhase()==0)
                result.append("play a card");
            else{
                result.append(switch (game.getStep()) {
                    case 0 -> "set students, " + (3 - game.getMovedStudents()) + " left";
                    case 1 -> "move mother nature";
                    case 2 -> "choose a cloud";
                    default -> "";
                });
            }
        }else{
            result.append(client.getGameState() == null ? "<->" :
                    client.clientIDToNickname(client.getGameState().currentlyPlayingPlayer()));
            if(game.getPhase()==0)
                result.append(" is choosing the card");
            else{
                result.append(switch (game.getStep()) {
                    case 0 -> " is moving students";
                    case 1 -> " is moving mother nature";
                    default -> " is choosing the cloud";
                });
            }
        }
        result.append("|@\n\n");

        if(game.isLastGameTurn())
            result.append("@|red !! LAST GAME TURN !!|@\n\n");

        return result.toString();
    }
}
