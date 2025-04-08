package Sequential;

import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.net.Socket;
import java.net.UnknownHostException;
import java.util.ArrayList;
import java.util.Scanner;


public class Client {
    private static int urlNum = 1;
    private static final int PORT = 26880;
    private static final String SERVER = "localhost";
    private static boolean implementDropEmulation = false;

    public static void main(String[] args) {
        // Initial Setup
        Scanner scanner = new Scanner(System.in);
        System.out.println("Emulate packet loss? [Yes or No]");
        if (scanner.nextLine().equalsIgnoreCase("yes")) {
            implementDropEmulation = true;
        } else {
            implementDropEmulation = false;
        }

        System.out.println("Please enter the image address or enter 'exit' to terminate program: ");
        String url = scanner.nextLine();

        while (!url.equals("exit")) {
            try (Socket clientSocket = new Socket(SERVER, PORT);
                 DataOutputStream out = new DataOutputStream(clientSocket.getOutputStream());
                 DataInputStream in = new DataInputStream(clientSocket.getInputStream())) {

                String safeURL = url.replaceAll("/", "__");
                byte[] encryptionKey = Workers.generateSessionKey();
                byte[] urlData = url.getBytes();
                byte[] urlPacket = Workers.createURLPacket(urlData, encryptionKey);
                out.writeInt(urlPacket.length);
                out.write(urlPacket);

                System.out.println("Client: " + "Sending url " + urlNum);
                urlNum++;


                int length = in.readInt();
                byte[] packet = new byte[length];
                in.readFully(packet);
                double imageSizeInMB = (packet.length * 8) / 1e6;
                System.out.println("Image Size in MB: " + imageSizeInMB);



                url = scanner.nextLine();
            } catch (IOException e) {
                System.out.println("Client " + urlNum + " failed to connect");
            }

            ArrayList<Double> tcpLatencyData = new ArrayList<>();
            try (Socket echoSocket = new Socket(SERVER, PORT);
                 DataOutputStream out = new DataOutputStream(echoSocket.getOutputStream());
                 DataInputStream in = new DataInputStream(echoSocket.getInputStream())) {

                echoSocket.setSoTimeout(30000);


//                double packetsTimeInSeconds = (endTime - startTime) / 1e9;
//                System.out.println("Packet Time in Seconds: " + packetsTimeInSeconds);
//                double throughput = (imageSizeInMB/packetsTimeInSeconds);
//                System.out.println("The throughput of this operation was " + throughput + " Mb/s");





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
}
