package org.network;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.net.InetSocketAddress;
import java.nio.ByteBuffer;
import java.nio.channels.SocketChannel;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Scanner;

import static org.network.Config.*;

public class Client implements Runnable {
    private int urlNum = 1;
    private static final int PORT = 26880;
    private static final String SERVER = "localhost";

    @Override
    public void run() {
        Scanner scanner = new Scanner(System.in);
        System.out.println("Emulate packet loss? [Yes or No]");
        if (scanner.nextLine().equalsIgnoreCase("yes")) {
            shouldDrop = true;
        } else {
            shouldDrop = false;
        }

        try {
            SocketChannel clientChannel = SocketChannel.open(new InetSocketAddress(SERVER, PORT));
            clientChannel.configureBlocking(false);

            // Sending the data
            System.out.println("Please enter the image address or enter 'exit' to terminate program: ");
            String url = scanner.nextLine();
            while (!url.equals("exit")) {
                String safeURL = url.replaceAll("/", "__");

                byte[] encryptionKey = generateSessionKey();
                byte[] urlData = url.getBytes();
                byte[] urlPacket = createURLPacket(urlData, urlNum, encryptionKey);
                System.out.println("Client: " + "Sending url " + urlNum);
                urlNum++;

                long startTime = System.nanoTime();
                clientChannel.write(ByteBuffer.wrap(urlPacket));
                ByteBuffer dataBuffer = ByteBuffer.allocate(MAX_PACKET_SIZE);
                ArrayList<byte[]> packets = new ArrayList<>();
                boolean finalPacket = false;

                while (!finalPacket) {
                    while (dataBuffer.position() < MAX_PACKET_SIZE) {
                        clientChannel.read(dataBuffer);
                        if (dataBuffer.get(0) == (byte) 7) {
                            finalPacket = true;
                            break;
                        }
                    }
                    dataBuffer.flip();

                    byte[] data = new byte[dataBuffer.limit()];
                    dataBuffer.get(data);
                    data = encryptionCodec(data, encryptionKey);

                    int blockNumber = ((data[2] & 0xff) << 8) | (data[3] & 0xff);
                    byte[] ack = createACKPacket(blockNumber);
                    if (shouldDrop && shouldDropPacket()) {
                        // don't send the packet to simulate packet loss
                        System.out.println("Packet Dropped");
                    } else {
                        clientChannel.write(ByteBuffer.wrap(ack));
                    }

                    packets.add(data);
                    dataBuffer.clear();
                }
                long endTime = System.nanoTime();

                ByteArrayOutputStream output = new ByteArrayOutputStream();
                for (byte[] imagePacket : packets) {
                    byte[] extracted = extractPacketData(imagePacket);
                    output.write(extracted);
                }
                byte[] finalImageFrame = output.toByteArray();
                bytesToImage(finalImageFrame, safeURL);
                output.close();

                // Measuring Throughput
                double imageSizeInMB = (finalImageFrame.length * 8) / 1e6;
                System.out.println("Image Size in MB: " + imageSizeInMB);
                double packetsTimeInSeconds = (endTime - startTime) / 1e9;
                System.out.println("Packet Time in Seconds: " + packetsTimeInSeconds);
                double throughput = (imageSizeInMB/packetsTimeInSeconds);
                System.out.println("The throughput of this operation was " + throughput + " Mb/s");

                System.out.println("Please enter the image address or enter 'exit' to terminate program: ");
                url = scanner.nextLine();
            }
            scanner.close();
            clientChannel.close();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public static void main(String[] args) {
        new Thread(new Client()).start();
    }
}