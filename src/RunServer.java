public class RunServer {

    public static void main(String[] args) {
        Server server = new Server(6750);
        server.start();
    }
}