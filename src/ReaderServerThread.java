import java.io.BufferedReader;
import java.io.DataOutputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.Socket;
import java.util.List;

public class ReaderServerThread extends Thread {

    private final Socket socket;
    private final Server server;
    private BufferedReader reader;

    public ReaderServerThread(Socket socket, Server server) throws IOException {
        this.socket = socket;
        this.server = server;
        this.reader = new BufferedReader(new InputStreamReader(socket.getInputStream()));
    }

    private void sendToAll(String message) {
        for (Socket s : server.getSockets()) {
            sendToSocket(s, message);
        }
    }

    private void sendToSocket(Socket s, String message) {
        if (s.isClosed()) {
            server.removePlayer(s);
        }
        if (s != socket) {
            DataOutputStream writer = null;
            try {
                writer = new DataOutputStream(s.getOutputStream());
                writer.writeBytes(message);
                writer.flush();
            } catch (IOException e) {
                throw new RuntimeException(e);
            }
        }
    }

    @Override
    public void run() {
        while (true) {
            String receivedMessage;
            try {
                receivedMessage = reader.readLine();
            } catch (IOException e) {
                continue;
            }
            if (receivedMessage == null) {
                continue;
            }
            String[] message = receivedMessage.split(",");
            String event = message[0];
            String player = message.length > 1 ? message[1] : null;

            if (event.equals("MOVE")) {
                int x = Integer.parseInt(message[2]);
                int y = Integer.parseInt(message[3]);

                String direction = message[4];

                System.out.println("Player " + player + " moved to " + x + ", " + y + " in direction " + direction);
            }

            if (event.equals("REGISTER_ALL_PLAYERS")) {
                for (Player p : server.getPlayers()) {
                    sendToSocket(socket, "REGISTER," + p.getName() + "," + p.getXpos() + "," + p.getYpos() + "," + p.getDirection() + '\n');
                }
            }

            if (event.equals("REGISTER")) {
                // check for om navnet er optaget.
                List<String> playerNames = server.getPlayers().stream().map(Player::getName).toList();
                if (playerNames.contains(player)) {
                    //TODO: name is taken
                    try {
                        DataOutputStream writer = null;
                        writer = new DataOutputStream(socket.getOutputStream());
                        writer.writeBytes("Name is taken");
                        writer.flush();
                    } catch (IOException e) {
                        throw new RuntimeException(e);
                    } finally {
                        try {
                            socket.close();
                        } catch (IOException e) {
                            throw new RuntimeException(e);
                        }
                    }
                }
                server.addPlayer(socket, player);
                System.out.println("Player " + player + " has joined the game");
            }
        }
    }
}