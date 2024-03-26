package View.CLI;

import Controller.ClientSide.Client;
import Controller.ServerSide.ClientData;
import Controller.ServerSide.LobbyData;
import Model.EffectParameters;
import Model.GameState;
import View.*;

import org.fusesource.jansi.AnsiConsole;
import static org.fusesource.jansi.Ansi.*;

import java.io.IOException;
import java.util.ArrayList;
import java.util.NoSuchElementException;
import java.util.Scanner;

/**
 * Command line interface, allows the user to interact with the program via a terminal and nothing more than text.
 * <br><br>
 * The CLI functions by always expecting a command from the users in the general format "command [parameter 1] [parameter 2] ...",
 * with the parameters being numbers, with their meaning explained by the "help" command. After receiving a valid command
 * the UI proceeds to execute it and then return control to the user for the next command.
 * <br><br>
 * The GLI has 5 states:
 * <ul>
 *     <li> Before a proper nickname has been chosen, still in login
 *     <li> After login, in the lobby selection menu
 *     <li> In a lobby, before a game starts
 *     <li> In a game
 *     <li> After a game, before the users goes back to lobby selection
 * </ul>
 */
public class CLI implements UI {
    private Client client;
    private boolean inLogin;
    private boolean inLobby;
    private boolean inGame;
    private boolean afterGame;
    private int winnerId;

    private static final int lobbies_per_page = 4;
    private int lobbies_page;

    private final String os = System.getProperty("os.name");

    /**
     * Construct and initializes the {@link CLI} but doesn't start it.<br>
     * The CLI is only started by a later call to {@link CLI#start}.
     */
    public CLI() {
        AnsiConsole.systemInstall();

        this.inLogin = true;
        this.inLobby = false;
        this.inGame = false;
        this.afterGame = false;
        this.winnerId = 0;

        this.lobbies_page = 0;
    }

    /**
     * {@inheritDoc}
     * @param client reference {@link Client} for this user interface
     */
    public void start(Client client) {
        this.client = client;

        String input;
        String[] parsed_input;
        Scanner console = new Scanner(System.in);

        try {
            while (true) {

                if (inLogin) {
                    clearScreen();

                    System.out.println("Welcome to ERIANTYS!\n");
                    System.out.print("Choose a nickname:\n>");
                    while (inLogin) {
                        input = console.nextLine();

                        if (input.matches("^[A-Za-z][A-Za-z0-9_]{3,29}$")) {
                            client.setNickname(input);
                            try {
                                synchronized (this) {
                                    this.wait();
                                }
                            } catch (InterruptedException e) {
                                e.printStackTrace();
                            }
                        } else {
                            System.out.print("\nInvalid nickname, try again:\n>");
                        }
                    }

                    refresh();

                    //client.getLobbies();

                } else {

                    System.out.print("\b\b\b \n>");
                    input = console.nextLine();
                    parsed_input = input.split(" ");

                    if (afterGame) {
                        afterGame = false;
                        inGame = false;
                        winnerId = 0;
                        refresh();
                        continue;
                    }

                    refresh();
                    System.out.print("\b\b\b");

                    try {
                        if (!inLobby) {
                            switch (parsed_input[0]) {
                                case "createlobby", "cl" -> client.createLobby(Integer.parseInt(parsed_input[1]), Integer.parseInt(parsed_input[2]) != 0);
                                case "joinlobby", "jl" -> {
                                    if (Integer.parseInt(parsed_input[1]) < client.getLobbiesList().size())
                                        client.joinLobby(client.getLobbiesList().get(Integer.parseInt(parsed_input[1])).lobbyID);
                                    else
                                        System.out.println("Invalid index...\nConsider refreshing!");
                                }
                                case "getlobbies", "refresh", "gl", "r" -> client.getLobbies();
                                case "nextpage", "np" -> {
                                    if (client.getLobbiesList().size() - lobbies_per_page * lobbies_page > lobbies_per_page) {
                                        lobbies_page++;
                                        refresh();
                                    } else
                                        System.out.println("This is the last page...");
                                }
                                case "prevpage", "pp" -> {
                                    if (lobbies_page > 0) {
                                        lobbies_page--;
                                        refresh();
                                    } else
                                        System.out.println("This is the first page...");
                                }
                                case "search", "s" -> {
                                    if (client.getLobbiesList() != null) {
                                        String search_string = parsed_input[1];

                                        StringBuilder output = new StringBuilder("Results for user \"" + search_string + "\":");
                                        for (LobbyData lobbyData : client.getLobbiesList()) {
                                            if (lobbyData.clients.stream().map(cd -> cd.nickname)
                                                    .anyMatch(nickname -> nickname.toLowerCase()
                                                            .contains(search_string.toLowerCase()))) {
                                                output.append("\n@|green Index: ").append(client.getLobbiesList().indexOf(lobbyData)).append("|@");
                                                //output += "\n@|cyan Id: " + lobbyData.lobbyID + " Expert mode: " + lobbyData.expert_mode + " Size: " + lobbyData.size + "|@";
                                                output.append("\n@|cyan Size: ").append(lobbyData.size).append(", Expert mode: ").append(lobbyData.expert_mode).append("|@");
                                                output.append("\n@|yellow Participants:");
                                                for (ClientData cd : lobbyData.clients)
                                                    output.append("\n").append(cd.nickname);
                                                output.append("|@\n");
                                            }
                                        }

                                        System.out.println(ansi().render(output.toString()));
                                    } else
                                        System.out.println("Loading lobbies...");
                                }
                                case "quit", "exit" -> {
                                    client.deleteLocalStorage();
                                    client.stop();
                                }
                                //case "whoami" -> System.out.println("Logged in with nickname: " + client.getNickname() + ", and clientID: " + client.getClientID());

                                case "help", "h" -> help();
                                default -> System.out.println("\bKeyword not recognized, use \"help\" for the list of commands.");
                            }
                        } else if (!inGame) {
                            switch (parsed_input[0]) {
                                case "leavelobby", "ll" -> client.leaveLobby();
                                case "toggleready", "tr" -> client.toggleReady();
                        /*case "readiness" -> {
                            System.out.println("Players readiness status:");
                            for (int i = 0; i < client.getLobby().clients.size(); i++)
                                System.out.println(client.getLobby().clients.get(i).nickname + ": " + (client.getReadyFlags()[i] ? "true" : "false"));
                        }*/
                        /*case "lobby" -> {
                            System.out.println("Current lobby:\nId: " + client.getLobby().lobbyID + " Expert mode: " + client.getLobby().expert_mode + " Size: " + client.getLobby().size);
                            System.out.println("Participants:");
                            for (ClientData clientData : client.getLobby().clients)
                                System.out.println(clientData.nickname);
                        }*/
                                //case "whoami" -> System.out.println("Logged in with nickname: " + client.getNickname() + ", and clientID: " + client.getClientID());

                                case "help", "h" -> help();
                                default -> System.out.println("\bKeyword not recognized, use \"help\" for the list of commands.");
                            }
                        } else {
                            switch (parsed_input[0]) {
                                case "leavelobby", "ll" -> client.leaveLobby();
                                case "playcard", "pc" -> client.playCard(client.getGameState().myPlayer(client).getCards()
                                        .stream().map(card -> card.order_value).toList().indexOf(Integer.parseInt(parsed_input[1])));
                                case "sstohall", "sh" -> client.setStudentToHall(Integer.parseInt(parsed_input[1]));
                                case "sstoisland", "si" -> client.setStudentToIsland(Integer.parseInt(parsed_input[1]), Integer.parseInt(parsed_input[2]));
                                case "movemn", "mm" -> client.moveMotherNature(Integer.parseInt(parsed_input[1]));
                                case "choosecloud", "cc" -> client.chooseCloud(Integer.parseInt(parsed_input[1]));
                                case "activateeffect", "ae" -> {
                                    if(client.getLobby().expert_mode) {
                                        if (client.getGameState().getNpcWithId(Integer.parseInt(parsed_input[1])) != null) {
                                            ArrayList<Integer> args = new ArrayList<Integer>();
                                            for (int i = 0; i < client.getGameState().getNpcWithId(Integer.parseInt(parsed_input[1])).getArgsNum(); i++)
                                                args.add(Integer.parseInt(parsed_input[2 + i]));
                                            client.activateEffect(java.util.Arrays.asList(client.getGameState().getNpcs()).indexOf(client.getGameState().getNpcWithId(Integer.parseInt(parsed_input[1]))),
                                                    new EffectParameters(args));
                                        } else
                                            System.out.println(ansi().render("@|red invalid NPC id... |@"));
                                    } else
                                        System.out.println(ansi().render("@|red expert mode is disabled... |@"));
                                }
                                case "npcinfo", "ni" -> {
                                    if(client.getLobby().expert_mode) {
                                        if (client.getGameState().getNpcWithId(Integer.parseInt(parsed_input[1])) != null)
                                        System.out.println(client.getGameState().getNpcWithId(Integer.parseInt(parsed_input[1])).toString() +
                                                "\nCost: " + client.getGameState().getNpcWithId(Integer.parseInt(parsed_input[1])).getCost());
                                        else
                                            System.out.println(ansi().render("@|red invalid NPC id... |@"));
                                    } else
                                        System.out.println(ansi().render("@|red expert mode is disabled... |@"));
                                }
                                //case "gamestate" -> System.out.println(client.getGameState().toString(client));
                                case "readiness", "r" -> {
                                    System.out.println("Players readiness status:");
                                    for (int i = 0; i < client.getLobby().clients.size(); i++)
                                        System.out.println(client.getLobby().clients.get(i).nickname + ": " + (client.getReadyFlags()[i] ? "true" : "false"));
                                }
                                case "lobby", "l" -> {
                                    System.out.println("Current lobby:\nId: " + client.getLobby().lobbyID + " Expert mode: " + client.getLobby().expert_mode + " Size: " + client.getLobby().size);
                                    System.out.println("Participants:");
                                    for (ClientData clientData : client.getLobby().clients)
                                        System.out.println(clientData.nickname);
                                }
                                case "whoami", "i" -> System.out.println("Logged in with nickname: " + client.getNickname() + ", and clientID: " + client.getClientID());

                                case "help", "h" -> help();
                                default -> System.out.println("\bKeyword not recognized, use \"help\" for the list of commands.");
                            }
                        }
                    } catch (NumberFormatException | ArrayIndexOutOfBoundsException e) {
                        System.out.println("\bInvalid keyword parameters...");
                    }
                }
            }
        } catch(NoSuchElementException e) {
            System.out.println("\nCLI interrupted, quitting the program forcefully...");
            client.stop();
        }
    }

    /**
     * {@inheritDoc}
     */
    public void nicknameConfirmed() {
        inLogin = false;
        synchronized(this) {
            this.notifyAll();
        }

        client.getLobbies();
    }

    /**
     * {@inheritDoc}
     */
    public void inLobby() {
        inLobby = true;
    }

    /**
     * {@inheritDoc}
     */
    public void noLobby() {
        inLobby = false;
        lobbies_page = 0;
    }

    /**
     * {@inheritDoc}
     */
    public void gameStart() {
        inGame = true;
        afterGame = false;
        winnerId = 0;
    }

    /**
     * {@inheritDoc}
     * @param winnerId id of the player who won the last game
     */
    public void gameEnd(int winnerId) {
        afterGame = true;
        this.winnerId = winnerId;
        refresh();
    }

    /**
     * {@inheritDoc}
     */
    public void resetState() {
        inLogin = client.getNickname() == null;

        inLobby = false;
        inGame = false;
        afterGame = false;
    }

    /***
     * {@inheritDoc}
     * @param message string to show to the user
     * @param color color for the displayed string
     */
    public void showMessage(String message, UIColors color) {
        refresh();
        System.out.print("\b\b\b \b");
        System.out.println(ansi().fg(color.ansiColor).a(message).reset());
        System.out.print("\n>");
        if(inLogin) {
            synchronized(this) {
                this.notifyAll();
            }
        }
    }

    /**
     * {@inheritDoc}
     */
    public synchronized void refresh() {
        clearScreen();

        System.out.println(ansi().render(
                "\n@|bold Nickname: " + client.getNickname() + "\nClientID: " + client.getClientID() + "|@\n"
        ));

        if(!inLobby) { //you are not in any lobby
            if(client.getLobbiesList() != null) {
                System.out.println("Lobbies, page " + (lobbies_page + 1) + " of " + (client.getLobbiesList().size() / lobbies_per_page + 1));

                StringBuilder output = new StringBuilder();
                for(int i = lobbies_page * lobbies_per_page; i < (lobbies_page + 1) * lobbies_per_page && i < client.getLobbiesList().size(); i++) {
                    LobbyData lobbyData = client.getLobbiesList().get(i);
                    output.append("\n@|green Index: ").append(i).append("|@");
                    //output += "\n@|cyan Id: " + lobbyData.lobbyID + " Expert mode: " + lobbyData.expert_mode + " Size: " + lobbyData.size + "|@";
                    output.append("\n@|cyan Size: ").append(lobbyData.size).append(", Expert mode: ").append(lobbyData.expert_mode).append("|@");
                    output.append("\n@|yellow Participants:");
                    for(ClientData cd : lobbyData.clients)
                        output.append("\n").append(cd.nickname);
                    output.append("|@\n");
                }

                System.out.println(ansi().render(output.toString()));
            } else
                System.out.println("Loading lobbies (force with \"r\") ...");
        } else if(!inGame) { //in a lobby, before the game starts
            System.out.println(ansi().render("@|cyan Current lobby:|@\n@|blue " + //Id: " + client.getLobby().lobbyID +
                    "\nExpert mode: " + client.getLobby().expert_mode +
                    "\nSize: " + client.getLobby().size + "|@"));
            System.out.println(ansi().render("@|yellow Participants (readiness):|@"));
            for(int i = 0; i < client.getLobby().clients.size() && i < client.getReadyFlags().length; i++)
                System.out.println(ansi().render("@|yellow " + client.getLobby().clients.get(i).nickname + " (" + (client.getReadyFlags()[i] ? "true" : "false") + ")" + "|@"));
            System.out.println(" ");
        } else { //in a lobby, with a game going on
            System.out.println(ansi().render(client.getGameState().toString(client)));
            if(afterGame)
                System.out.println(ansi().render((winnerId == client.getClientID() ? "@|green " : "@|red ") + "--> game ended, winner: " +
                        client.clientIDToNickname(winnerId) + "\n\nPRESS ENTER TO CONTINUE...|@"));
        }

        if(inLogin) {
            this.notifyAll();
        }

        System.out.print("\n>");
    }

    public void help() {
        if(!inLobby) { //you are not in any lobby
            System.out.println("""
                    List of available commands:
                    createlobby, cl [size] [expert mode] - creates a new lobby
                    joinlobby, jl [lobby index] - lets you join the specified lobby
                    getlobbies, refresh, gl, r - reloads the list of available lobbies
                    nextpage, np - prints the next page of available lobbies
                    prevpage, pp - prints the previous page of available lobbies
                    search, s [string] - prints only the lobbies where a player's name matches the searched string
                    quit, exit - closes the game
                    """);
                    //"whoami - prints your current nickname and client id");
        } else if(!inGame) { //in a lobby, before the game starts
            System.out.println("""
                    List of available commands:
                    leavelobby, ll - makes you leave your current lobby
                    toggleready, tr - toggles your readiness state inside a lobby
                    """);
                    //"readiness - prints the readiness state of the players in the lobby\n" +
                    //"lobby - prints you current lobby\n" +
                    //"whoami - prints your current nickname and client id");
        } else { //in a lobby, with a game going on
            GameState gs = client.getGameState();
            System.out.println(("""
                    List of available commands:
                    """) + (gs.isMyTurn(client.getClientID()) ? ((gs.getPhase() == 0 ? """
                    playcard, pc [card order value] - allows you to play the given card
                    """ : "") + (gs.getPhase() == 1 && gs.getStep() == 0 ? """
                    sstohall, sh [student index] - allows you to set the selected student to your hall
                    sstoisland, si [student index] [island index] - allows you to set the selected student on the selected island
                    """ : "") + (gs.getPhase() == 1 && gs.getStep() == 1 ? """
                    movemn, mm [steps] - allows you to move mother nature of the given amount of steps
                    """ : "") + ( gs.getPhase() == 1 && gs.getStep() == 2 ? """
                    choosecloud, cc [cloud index] - allows you to pick the students from the selected cloud
                    """ : "")) : "none -> wait for your turn\n") + (client.getLobby().expert_mode ? (("""
                            
                            Expert mode commands:
                            npcinfo, ni [npc id] - prints the effect and the arguments needed for the selected npc
                            """) + (gs.getPhase() == 1 ? """
                            activateeffect, ae [npc id] [npc args ...] - activates the selected npc's effect
                            """ : "")) : "") + ("""
                    
                    Lobby related commands:
                    leavelobby, ll - makes you leave your current lobby
                    readiness, r - prints the readiness state of the players in the lobby
                    lobby, l - prints you current lobby
                    whoami, i - prints your current nickname and client id"""));
        }
    }

    /**
     * Utility method that clears the console's screen calling the platform-specific command:
     * <ul>
     *     <li> "clear" on unix systems
     *     <li> "cls" on windows systems
     * </ul>
     */
    public void clearScreen() {
        if(os.contains("Windows")) {
            try {
                new ProcessBuilder("cmd", "/c", "cls").inheritIO().start().waitFor();
            } catch (InterruptedException e) {
                e.printStackTrace();
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
        else {
            try {
                Runtime.getRuntime().exec("clear");
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
        //System.out.println("\\033[H\\033[2J");

        //potentially: ansi().eraseScreen().etc...
    }
}
