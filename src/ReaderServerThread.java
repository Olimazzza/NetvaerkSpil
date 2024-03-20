import javafx.application.Platform;

import java.io.BufferedReader;
import java.io.DataOutputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.Socket;
import java.util.HashSet;
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
        for (Socket s : new HashSet<>(server.getSockets())) {
            if (ignoreSelf && s == socket) {
                continue;
            }
            sendToSocket(s, message);
        }
    }

    private void sendToSocket(Socket s, String message) {
        DataOutputStream writer = null;
        try {
            writer = new DataOutputStream(s.getOutputStream());
            writer.writeBytes(message + '\n');
            writer.flush();
        } catch (IOException e) {
            System.out.println("Failed to send message to socket " + s);
            System.out.println("DEBUG: " + server.getSocketsAndPlayers().get(s));
            //server.removePlayer(s);
            try {
                s.close();
                //sendToAll("DISCONNECT," + server.getPlayers().stream().map(Player::getName).toList(), true);
            } catch (IOException ex) {
                ex.printStackTrace();
            }
        }
    }

    @Override
    public void run() {
        Player you = null;
        while (socket.isConnected()) {
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

                int p1Points = message.length > 5 ? Integer.parseInt(message[5]) : 0;
                String targetName = message.length > 6 ? message[6] : null;
                you.addPoints(p1Points);
                if (targetName != null) {
                    Player target = server.getPlayers().stream().filter(p -> p.getName().equals(targetName)).findFirst().orElse(null);
//                    if (target == null) {
//                        sendToAll("DISCONNECT," + targetName, false);
//                    } else {
                        int p2Points = message.length > 7 ? Integer.parseInt(message[7]) : 0;
                        if (p2Points != 0) {
                            target.addPoints(p2Points);
                        }
//                    }
                }
                //System.out.println("Player " + player + " moved to " + x + ", " + y + " in direction " + direction);
                //System.out.println(receivedMessage);
                //System.out.println(server.getPlayers());
                sendToAll(receivedMessage, false);
                StringBuilder sb = new StringBuilder();
                for (Player p : server.getPlayers()) {
                    sb.append(p.name).append(":").append(p.point).append(",");
                }
                sb.deleteCharAt(sb.length() - 1);
                sendToAll("POINTS," + sb.toString(), false);
            }

            else if (event.equals("REGISTER_ALL_PLAYERS")) {
                for (Socket socket1 : server.getSockets()) {
                    if (socket1 == socket) {
                        continue;
                    }
                    Player p = server.getSocketsAndPlayers().get(socket1);
//                    if (!socket1.isConnected()) {
//                        sendToAll("DISCONNECT," + p.getName(), false);
//                    }
                    System.out.println("sending " + you.name + " with data about " + p.name);
                    sendToSocket(socket, "REGISTER," + p.getName() + "," + p.getXpos() + "," + p.getYpos() + "," + p.getDirection());
                }
            }

            else if (event.equals("NAME_SEARCH")) {
                List<String> playerNames = server.getPlayers().stream().map(Player::getName).toList();
                if (playerNames.contains(player)) {
                    sendToSocket(socket, "NAME_TAKEN");
                } else {
                    sendToSocket(socket, "NAME_AVAILABLE");
                }
            }

            else if (event.equals("REGISTER")) {
                // check for om navnet er optaget.
                List<String> playerNames = server.getPlayers().stream().map(Player::getName).toList();
//                if (playerNames.contains(player)) {
//                    sendToSocket(socket, "NAME_TAKEN");
//                } else {
                System.out.println("Player " + player + " has joined the game");
                int x = Integer.parseInt(message[2]);
                int y = Integer.parseInt(message[3]);
                you = server.addPlayer(socket, player, x, y);
                System.out.println(you);
                sendToAll(receivedMessage, false); // send to all and yourself
//                }
            }

            else if (event.equals("DISCONNECT")) {
                server.removePlayer(socket);
                sendToAll(receivedMessage, true);
                try {
                    socket.close();
                } catch (IOException e) {
                    e.printStackTrace();
                }
                break;
            }
        }
        // Player disconnected
        server.removeSocketByPlayer(you);
        System.out.println("Player " + you.getName() + " has disconnected");
        sendToAll("DISCONNECT," + you.getName(), true);
    }
}