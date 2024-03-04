public class RunServer {

    public static void main(String[] args) {
        Server server = new Server(6788);
        server.start();
    }
}
