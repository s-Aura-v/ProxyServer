package Sequential;

import java.io.ByteArrayOutputStream;
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

                ArrayList<byte[]> packets = new ArrayList<>();
                boolean finalPacket = false;
                while (true) {
                    int length = in.readInt();
                    byte[] encryptedPacket = new byte[length];
                    in.readFully(encryptedPacket);

                    byte[] decryptedPacket = Workers.encryptionCodec(encryptedPacket, encryptionKey);
                    int blockNumber = ((decryptedPacket[2] & 0xff) << 8) | (decryptedPacket[3] & 0xff);
                    byte[] ack = Workers.createACKPacket(blockNumber);
                    out.writeInt(ack.length);
                    out.write(ack);

                    packets.add(decryptedPacket);

                    System.out.println(packets.size());
                    if (encryptedPacket.length == 0) {
                        break;
                    }
                }

                ByteArrayOutputStream output = new ByteArrayOutputStream();
                for (byte[] imagePacket : packets) {
                    byte[] extracted = Workers.extractPacketData(imagePacket);
                    output.write(extracted);
                }
                byte[] finalImageFrame = output.toByteArray();
                Workers.bytesToImage(finalImageFrame, safeURL);
                output.close();

                double imageSizeInMB = (finalImageFrame.length * 8) / 1e6;
                System.out.println("image size: " + imageSizeInMB);
//                System.out.println("Image Size in MB: " + imageSizeInMB);

                url = scanner.nextLine();
            } catch (IOException e) {
                System.out.println("Client " + urlNum + " failed to connect");
            } finally {
                // graph
            }
        }
    }
}
