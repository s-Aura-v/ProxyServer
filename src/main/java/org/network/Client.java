package org.network;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.net.ServerSocket;
import java.nio.ByteBuffer;
import java.nio.channels.Selector;
import java.nio.channels.ServerSocketChannel;
import java.nio.channels.SocketChannel;
import java.util.ArrayList;
import java.util.Arrays;

import static org.network.Config.WINDOW_SIZE;
import static org.network.Config.createTestCases;

public class Client implements Runnable {
    @Override
    public void run() {
        try {
            ArrayList<byte[]> window = new ArrayList<>();
            createTestCases(window);

            SocketChannel clientChannel = SocketChannel.open(new InetSocketAddress("localhost", 26880));
            clientChannel.configureBlocking(false);

//            // 1. Send WRQ first
//            byte[] writeRequest = Config.createRequestPacket(false, "test-file");
//            clientChannel.write(ByteBuffer.wrap(writeRequest));
//            System.out.println("Sent WRQ");

            // Wait for ACK (block 0)
            ByteBuffer ackBuffer = ByteBuffer.allocate(4);
            while (ackBuffer.position() < 4) {
                clientChannel.read(ackBuffer);
            }
            ackBuffer.flip();
            System.out.println("Received initial ACK");

            // 2. Send data packets
            for (int i = 0; i < window.size(); i++) {
                clientChannel.write(ByteBuffer.wrap(window.get(i)));
                System.out.println("Sent DATA block " + (i+1));

                // Wait for ACK
                ackBuffer.clear();
                while (ackBuffer.position() < 4) {
                    clientChannel.read(ackBuffer);
                }
                ackBuffer.flip();
                System.out.println("Received ACK for block " + (i+1));
            }

//            // 3. Send RRQ to switch to read mode
//            byte[] readRequest = Config.createRequestPacket(true, "test-file");
//            clientChannel.write(ByteBuffer.wrap(readRequest));
//            System.out.println("Sent RRQ");

            clientChannel.close();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
}