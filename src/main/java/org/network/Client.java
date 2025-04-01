package org.network;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.net.InetSocketAddress;
import java.nio.ByteBuffer;
import java.nio.channels.SocketChannel;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Scanner;

import static org.network.Config.*;

public class Client implements Runnable {
    private int urlNum = 1;

    @Override
    public void run() {
        try {
            SocketChannel clientChannel = SocketChannel.open(new InetSocketAddress("localhost", 26880));
            clientChannel.configureBlocking(false);
            long seed = generateSessionKey();
            System.out.println(seed);

            // Sending the data
            System.out.println("Please enter the image address or enter 'exit' to terminate program: ");
            Scanner scanner = new Scanner(System.in);
            String url = scanner.nextLine();
            while (!url.equals("exit")) {
                String safeURL = url.replaceAll("/", "__");

                long key = xorShift(seed);
                byte[] keyBytes = longToBytes(key);
                System.out.println(key);
                byte[] urlData = url.getBytes();
                byte[] urlPacket = createURLPacket(urlData, urlNum, keyBytes);
                System.out.println(Arrays.toString(urlPacket));
                System.out.println("Client: " + "Sending url " + urlNum);
                urlNum++;

                long startTime = System.nanoTime();
                System.out.println(startTime);
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

                    int blockNumber = ((data[2] & 0xff) << 8) | (data[3] & 0xff);
                    byte[] ack = createACKPacket(blockNumber);
                    clientChannel.write(ByteBuffer.wrap(ack));

                    packets.add(data);
                    dataBuffer.clear();
                }
                long endTime = System.nanoTime();
                System.out.println(endTime);

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
}