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

    private void sendToAll(String message, boolean ignoreSelf) {
        for (Socket s : server.getSockets()) {
            if (ignoreSelf && s == socket) {
                continue;
            }
            sendToSocket(s, message);
        }
    }

    private void sendToSocket(Socket s, String message) {
        if (s.isClosed()) {
            System.out.println("Socket " + s + " is closed or not connected");
            server.removePlayer(s);
            return;
        }
        DataOutputStream writer = null;
        try {
            writer = new DataOutputStream(s.getOutputStream());
            writer.writeBytes(message + '\n');
            writer.flush();
        } catch (IOException e) {
            System.out.println("Failed to send message to socket " + s);
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
                System.out.println(receivedMessage);
                System.out.println("Sending to all players");
                System.out.println(server.getPlayers());
                sendToAll(receivedMessage, false);
            }

            else if (event.equals("REGISTER_ALL_PLAYERS")) {
                for (Player p : server.getPlayers()) {
                    sendToSocket(socket, "REGISTER," + p.getName() + "," + p.getXpos() + "," + p.getYpos() + "," + p.getDirection() + '\n');
                }
            }

            else if (event.equals("REGISTER")) {
                // check for om navnet er optaget.
                List<String> playerNames = server.getPlayers().stream().map(Player::getName).toList();
                if (playerNames.contains(player)) {
                    sendToSocket(socket, "NAME_TAKEN");
                } else {
                    System.out.println("Player " + player + " has joined the game");
                    server.addPlayer(socket, player);
                    sendToAll(receivedMessage, false); // send to all and yourself
                }
            }
        }
    }
}