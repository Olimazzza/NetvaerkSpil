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
            String player = message[1];

            if(event.equals("MOVE")){
                int x = Integer.parseInt(message[2]);
                int y = Integer.parseInt(message[3]);

                String direction = message[4];

                System.out.println("Player " + player + " moved to " + x + ", " + y + " in direction " + direction);
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
            
            for (Socket s : server.getSockets()) {
                if (s.isClosed()) {
                    //TODO: remove player from game
                    //send a message to other clients here
                    DataOutputStream writer = null;
                    try {
                        writer = new DataOutputStream(s.getOutputStream());
                    } catch (IOException e) {
                        throw new RuntimeException(e);
                    }

                    //writer.writeBytes(nameServiceClient.getUser(socket) + ": " + receivedMessage + '\n');
                    server.removePlayer(s);
                    continue;
                }
                if (s != socket) {
                    DataOutputStream writer = null;
                    try {
                        writer = new DataOutputStream(s.getOutputStream());
                        //send a message to other clients here
                        writer.writeBytes("Jeg modtog din besked, og sendte dem rundt");
                        writer.flush();
                    } catch (IOException e) {
                        throw new RuntimeException(e);
                    }
                }
            }
        }
    }
}