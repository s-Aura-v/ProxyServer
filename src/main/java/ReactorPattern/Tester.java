package ReactorPattern;

import java.io.IOException;

public class Tester {
    public static void main(String[] args) throws IOException {
        // Start the Reactor in its own thread
        Thread reactorThread = new Thread(new Reactor(26880));
        reactorThread.start();

        // Start the client in its own thread to send data to the Reactor server
        new Thread(new Client()).start();
    }
}
