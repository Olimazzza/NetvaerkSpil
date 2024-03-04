import java.io.BufferedReader;
import java.io.DataOutputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.Socket;

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


            for (Socket s : server.getSockets()) {
                if (s.isClosed()) {
                    //TODO: remove player from game
                    continue;
                }
                if (s != socket) {
                    DataOutputStream writer = null;
                    try {
                        writer = new DataOutputStream(s.getOutputStream());
                        //send a message to other clients here
                        //writer.writeBytes(nameServiceClient.getUser(socket) + ": " + receivedMessage + '\n');
                        writer.flush();
                    } catch (IOException e) {
                        throw new RuntimeException(e);
                    }
                }
            }
        }
    }
}
