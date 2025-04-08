package Sequential;

import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.net.Socket;
import java.net.UnknownHostException;
import java.util.ArrayList;

public class Client {
    private int urlNum = 1;
    private static final int PORT = 26880;
    private static final String SERVER = "localhost";

    public static void main(String[] args) {
        ArrayList<Double> tcpLatencyData = new ArrayList<>();
        try (Socket echoSocket = new Socket(SERVER, PORT);
             DataOutputStream out = new DataOutputStream(echoSocket.getOutputStream());
             DataInputStream in = new DataInputStream(echoSocket.getInputStream())) {

            echoSocket.setSoTimeout(30000);

            // Figure Send out
//            for (int i = 0; i < encryptedPackets.size(); i++) {
//                long sendTime = System.nanoTime();
//                out.writeInt(encryptedPackets.get(i).length);
//                out.write(encryptedPackets.get(i));
//                out.flush();
//
//
//                int length = in.readInt();
//                byte[] byteArray = new byte[length];
//                in.readFully(byteArray);
//
//                long receiveTime = System.nanoTime();
//                double diffInSeconds = (receiveTime - sendTime) * 1e-9;
//                tcpLatencyData.add(diffInSeconds);
//
//                System.out.println(Helpers.msgSize + " byte packet " + (i + 1) + " sent and received in " + diffInSeconds + " seconds");
//            }

            System.out.println("All packets sent and received successfully.");
            in.close();
            out.close();
            echoSocket.close();
        } catch (UnknownHostException e) {
            System.err.println("Don't know about host " + SERVER);
            e.printStackTrace();
            System.exit(1);
        } catch (IOException e) {
            System.err.println("IO failure.");
            e.printStackTrace();
        } finally {
            // Graph it at the end

        }
    }
}
