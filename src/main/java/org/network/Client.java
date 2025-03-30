package org.network;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.nio.ByteBuffer;
import java.nio.channels.SocketChannel;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Arrays;

import static org.network.Config.*;

public class Client implements Runnable {
    private int urlNum = 1;

    @Override
    public void run() {
        try {
            ArrayList<byte[]> window = new ArrayList<>();
//            createTestCases(window);

            SocketChannel clientChannel = SocketChannel.open(new InetSocketAddress("localhost", 26880));
            clientChannel.configureBlocking(false);

            // Initial Setup:
            // 1. Send WRQ
            // 2. Wait for ACK
            // 3. Start sending Data
            byte[] writeRequest = Config.createRequestPacket(false, "test-file");
            clientChannel.write(ByteBuffer.wrap(writeRequest));
            System.out.println(YELLOW + "Client: " + RESET + "Sent WRQ");
            // Wait for ACK
            ByteBuffer ackBuffer = ByteBuffer.allocate(4);
            while (ackBuffer.position() < 4) {
                clientChannel.read(ackBuffer);
            }
            ackBuffer.flip();
            System.out.println(YELLOW + "Client: " + RESET + "Received initial ACK");

            // Sending the data
//            Scanner scanner = new Scanner(System.in);
//            String url = scanner.nextLine();
//            scanner.close();

            String url = "https://m.media-amazon.com/images/M/MV5BNTc4ODVkMmMtZWY3NS00OWI4LWE1YmYtN2NkNDA3ZjcyNTkxXkEyXkFqcGc@._V1_.jpg";

            byte[] urlData = url.getBytes();
            byte[] urlPacket = createDataPacket(urlData, urlNum);
            System.out.println(YELLOW + "Client: " + RESET + "Sending url " + urlNum);
            urlNum++;
            clientChannel.write(ByteBuffer.wrap(urlPacket));

            ByteBuffer dataBuffer = ByteBuffer.allocate(MAX_PACKET_SIZE);
            ArrayList<byte[]> packets = new ArrayList<>();
            boolean finalPacket = false;

            while (!finalPacket) {
                while (dataBuffer.position() < MAX_PACKET_SIZE) {
                    clientChannel.read(dataBuffer);
                }
                dataBuffer.flip();

                byte[] data = new byte[dataBuffer.limit()];
                dataBuffer.get(data);
                if (data[0] == 0 && data[1] == 2) {
                    finalPacket = true;
                } else {
                    packets.add(data);
                    dataBuffer.clear();
                }
            }
            System.out.println( "all packets collected: " + packets.size());


//            ackBuffer.clear();
//            clientChannel.read(ackBuffer);
//            ackBuffer.flip();
//            System.out.println(YELLOW + "Client: " + RESET + "Received ACK " + urlNum);
////            System.out.println(YELLOW + "Client: " + RESET + ackBuffer.);

//            byte[] bytes = new byte[ackBuffer.remaining()];
//            ackBuffer.get(bytes);  // Copy data into byte array
//            System.out.println("Received ACK: " + new String(bytes, StandardCharsets.UTF_8));







//            String url2 = "https://s-aura-v.com/assets/F24_P1-C98K-uUV.png";
//
//            byte[] urlData2 = url2.getBytes();
//            byte[] urlPacket2 = createDataPacket(urlData2, urlNum);
//            System.out.println(YELLOW + "Client: " + RESET + "Sending url " + urlNum);
//            urlNum++;
//            clientChannel.write(ByteBuffer.wrap(urlPacket2));
//
//            ackBuffer.clear();
//            clientChannel.read(ackBuffer);
//            ackBuffer.flip();
//            System.out.println(YELLOW + "Client: " + RESET + "Received ACK " + urlNum);
////            System.out.println(YELLOW + "Client: " + RESET + ackBuffer.);
//
//            byte[] bytes2 = new byte[ackBuffer.remaining()];
//            ackBuffer.get(bytes2);  // Copy data into byte array
//            System.out.println("Received ACK: " + new String(bytes2, StandardCharsets.UTF_8));


            clientChannel.close();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
}