import java.io.IOException;
import java.io.Reader;
import java.io.Writer;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.*;

public class Server extends Thread {

    // martin's ip: 172.31.80.1

    private final Map<Socket, Player> players = new HashMap<>();
    private ServerSocket serverSocket;

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

    public Set<Socket> getSockets() {
        return players.keySet();
    }

    public void removePlayer(Socket socket) {
        players.remove(socket);
    }

    public void addPlayer(Socket socket) {
        players.put(socket, new Player("test", 1, 1, "UP"));
    }
}