package org.network;

import java.io.ByteArrayOutputStream;
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
            SocketChannel clientChannel = SocketChannel.open(new InetSocketAddress("localhost", 26880));
            clientChannel.configureBlocking(false);

            // Sending the data
//            Scanner scanner = new Scanner(System.in);
//            String url = scanner.nextLine();
//            scanner.close();

//            String url = "https://placehold.jp/100x100.png";

            String url = "https://m.media-amazon.com/images/M/MV5BNTc4ODVkMmMtZWY3NS00OWI4LWE1YmYtN2NkNDA3ZjcyNTkxXkEyXkFqcGc@._V1_.jpg";
            String safeURL = url.replaceAll("/", "__");

            byte[] urlData = url.getBytes();
            byte[] urlPacket = createDataPacket(urlData, urlNum);
            System.out.println("Client: " + "Sending url " + urlNum);
            urlNum++;
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
                packets.add(data);
                dataBuffer.clear();
            }

            ByteArrayOutputStream output = new ByteArrayOutputStream();
            for (byte[] imagePacket : packets) {
                byte[] extracted = extractPacketData(imagePacket);
                output.write(extracted);
            }
            byte[] finalImageFrame = output.toByteArray();
            bytesToImage(finalImageFrame, safeURL);

            clientChannel.close();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
}