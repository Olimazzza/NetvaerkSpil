import java.io.IOException;
import java.io.Reader;
import java.io.Writer;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.*;

public class Server extends Thread {

    private final Map<Socket, Player> players = new HashMap<>();
    private ServerSocket serverSocket;

    public static void main(String[] args) {
        Server server = new Server(6750);
        server.start();
    }

    public Server(int port) {
        try {
            serverSocket = new ServerSocket(port);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    @Override
    public void run() {
        while (true) {
            try {
                Socket connection = serverSocket.accept();
                if (connection.isConnected()) {
                    System.out.println(connection);
                    ReaderServerThread readerThread = new ReaderServerThread(connection, this);
                    readerThread.start();
                }
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
    }

    public Map<Socket, Player> getSocketsAndPlayers() {
        return players;
    }

    public Set<Socket> getSockets() {
        return players.keySet();
    }

    public Collection<Player> getPlayers() {
        return players.values();
    }

    public void removePlayer(Socket socket) {
        players.remove(socket);
    }

    public Player addPlayer(Socket socket, String playerName, int x, int y) {
        Player newPlayer = new Player(playerName, x, y, "up");
        players.put(socket, newPlayer);
        return newPlayer;
    }

    public void removeSocketByPlayer(Player player) {
        for (Socket s : players.keySet()) {
            if (players.get(s).equals(player)) {
                players.remove(s);
                return;
            }
        }
    }
}