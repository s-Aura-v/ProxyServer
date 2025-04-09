package Sequential;

import java.io.ByteArrayOutputStream;
import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.net.Socket;
import java.net.UnknownHostException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Scanner;
import java.util.concurrent.ThreadLocalRandom;

public class Client {
    private static int urlNum = 1;
    private static final int PORT = 26880;
    private static boolean enableDropEmulation = false;
    private static final int DROP_PERCENTAGE = 1;

    private static final String SERVER = "localhost";
    private static ArrayList<Integer> throughputData = new ArrayList<>();


    public static void main(String[] args) {
        // Initial Setup
        Scanner scanner = new Scanner(System.in);
        System.out.println("Emulate packet loss? [Yes or No]");
        if (scanner.nextLine().equalsIgnoreCase("yes")) {
            enableDropEmulation = true;
        } else {
            enableDropEmulation = false;
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


//                System.out.println("Client: " + "Sending url " + urlNum);
                urlNum++;

                ArrayList<byte[]> packets = new ArrayList<>();
                long startTime = System.nanoTime();
                while (true) {
                    int length = in.readInt();
                    byte[] encryptedPacket = new byte[length];
                    in.readFully(encryptedPacket);
                    if (encryptedPacket.length == 0) {
                        break;
                    }

                    byte[] decryptedPacket = Workers.encryptionCodec(encryptedPacket, encryptionKey);
                    int blockNumber = ((decryptedPacket[2] & 0xff) << 8) | (decryptedPacket[3] & 0xff);
                    byte[] ack = Workers.createACKPacket(blockNumber);
                    if (enableDropEmulation && shouldDropPacket()) {
//                        System.out.println("Packet Dropped");
                    } else {
                        out.writeInt(ack.length);
                        out.write(ack);
                    }
//                    System.out.println("Blocknumber: " + blockNumber + ", Bytes: " + Arrays.toString(decryptedPacket));

                    packets.add(decryptedPacket);

//                    System.out.println(packets.size());
                }
                long endTime = System.nanoTime();

                // Creating Image
                ByteArrayOutputStream output = new ByteArrayOutputStream();
                for (byte[] imagePacket : packets) {
                    byte[] extracted = Workers.extractPacketData(imagePacket);
                    output.write(extracted);
                }
                byte[] finalImageFrame = output.toByteArray();
                Workers.bytesToImage(finalImageFrame, safeURL);
                output.close();

                // Calculating Throughput
                double imageSizeInMB = (finalImageFrame.length * 8) / 1e6;
                double timeInSeconds = (endTime - startTime) / 1e9;
                double throughput = imageSizeInMB / timeInSeconds;
                System.out.println("Image Size in MB: " + imageSizeInMB + " \n" +
                        "Time: " + timeInSeconds + " \n" +
                        " Throughput: " + throughput);


                System.out.println("Please enter the image address or enter 'exit' to terminate program: ");
                url = scanner.nextLine();
            } catch (IOException e) {
                System.out.println("Client " + urlNum + " failed to connect");
            } finally {
                // graph
            }
        }
    }

    static boolean shouldDropPacket() {
        int rand = ThreadLocalRandom.current().nextInt(100);
        return rand < DROP_PERCENTAGE;
    }
}
