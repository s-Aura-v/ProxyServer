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

import static org.network.Config.*;

public class Client implements Runnable {
    @Override
    public void run() {
        try {
            ArrayList<byte[]> window = new ArrayList<>();
            createTestCases(window);

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

//            long startTime = System.currentTimeMillis();
//            // Sending Normally
//            for (int i = 0; i < window.size(); i++) {
//                clientChannel.write(ByteBuffer.wrap(window.get(i)));
//                System.out.println(YELLOW + "Client: " + RESET + "Sent DATA block " + (i + 1));
//
//                // Wait for ACK
//                ackBuffer.clear();
//                while (ackBuffer.position() < 4) {
//                    clientChannel.read(ackBuffer);
//                }
//
//                ackBuffer.flip();
//                byte[] buffered = Arrays.copyOf(ackBuffer.array(), ackBuffer.limit());
//                System.out.println(YELLOW + "Client: " + RESET + "Received Ack: " + Arrays.toString(buffered));
//            }

//            int leftPointer = 0;
//            int rightPointer = Math.min(SEND_WINDOW_SIZE, window.size());
//
//            while (leftPointer < window.size()) {
//                // Send all packets in the current window
//                for (int i = leftPointer; i < rightPointer; i++) {
//                    clientChannel.write(ByteBuffer.wrap(window.get(i)));
//                }
//
//                // Wait for an acknowledgment for each packet
//                for (int i = 0; i < (rightPointer - leftPointer); i++) {
//                    ackBuffer = ByteBuffer.allocate(4);
//                    while (ackBuffer.position() < 4) {  // Ensure full ACK read
//                        clientChannel.read(ackBuffer);
//                    }
//                    ackBuffer.flip();
//                    byte[] ack = Arrays.copyOf(ackBuffer.array(), ackBuffer.limit());
//                    System.out.println("Client: Received Ack: " + Arrays.toString(ack));
//
//                    // Move the sliding window forward
//                    if (rightPointer < window.size()) {
//                        rightPointer++;
//                    }
//                    leftPointer++;
//                }
//            }

            int leftPointer = 0;
            int rightPointer = Math.min(SEND_WINDOW_SIZE, window.size());

            while (leftPointer < window.size()) {
                // Send all packets in the current window
                for (int i = leftPointer; i < rightPointer; i++) {
                    clientChannel.write(ByteBuffer.wrap(window.get(i)));
                    System.out.println("Sent packet: " + i);  // For debugging
                }

                // Wait for acknowledgments
                int acksReceived = 0;
                while (acksReceived < (rightPointer - leftPointer) && leftPointer < window.size()) {
                    ackBuffer = ByteBuffer.allocate(4);
                    while (ackBuffer.position() < 4) {  // Ensure full ACK read
                        clientChannel.read(ackBuffer);
                    }
                    ackBuffer.flip();
                    byte[] ack = Arrays.copyOf(ackBuffer.array(), ackBuffer.limit());
                    System.out.println("Client: Received Ack: " + Arrays.toString(ack));

                    acksReceived++;
                    leftPointer++;

                    // Slide the window forward if possible
                    if (rightPointer < window.size()) {
                        rightPointer++;
                    }
                }
            }

//
//
//            // Step 1: Send the send-window
//            for (int i = 0; i < WINDOW_SIZE; i++) {
//                clientChannel.write(ByteBuffer.wrap(window.get(i)));
//                ackBuffer.clear();
//            }
//
//            // Step 2: Receive and continue sliding the window
//            for (int i = WINDOW_SIZE; i < window.size(); i++) {
//                while (ackBuffer.position() < 4) {
//                    clientChannel.read(ackBuffer);
//                    ackBuffer.flip();
//                    byte[] buffered = Arrays.copyOf(ackBuffer.array(), ackBuffer.limit());
//                    System.out.println(YELLOW + "Client: " + RESET + "Received Ack: " + Arrays.toString(buffered));
//                    clientChannel.write(ByteBuffer.wrap(window.get(i)));
//                    ackBuffer.clear();
//
//                }
//            }
//
//            // Send some data to the server
//            int leftPointer = 0;
//            int rightPointer = 0;
//
//            while (leftPointer < window.size()) {
//                // Step 1: Send all the packets inside the send-window
//                while ((rightPointer < leftPointer + WINDOW_SIZE)
//                        && (rightPointer < window.size())) {
//                    clientChannel.write(ByteBuffer.wrap(window.get(leftPointer)));
//                    System.out.println(YELLOW + "Client: " + RESET + "Sent DATA block " + (leftPointer + 1));
//
//                    // Wait for ACK
//                    ackBuffer.clear();
//                    while (ackBuffer.position() < 4) {
//                        clientChannel.read(ackBuffer);
//                    }
//
//                    ackBuffer.flip();
//                    byte[] buffered = Arrays.copyOf(ackBuffer.array(), ackBuffer.limit());
//                    System.out.println(YELLOW + "Client: " + RESET + "Received Ack: " + Arrays.toString(buffered));
//                }
//                leftPointer++;
//            }

//            final int WINDOW_SIZE = 4;
//            int base = 0; // Tracks the oldest un-ACKed packet
//            int nextSeqNum = 0; // Tracks next packet to send
//
//            long timestamp = System.currentTimeMillis();
//            while (base < window.size()) {
//                // Send all packets in the current window
//                while (nextSeqNum < base + WINDOW_SIZE && nextSeqNum < window.size()) {
//                    clientChannel.write(ByteBuffer.wrap(window.get(nextSeqNum)));
//                    System.out.println(YELLOW + "Client: " + RESET + "Sent DATA block " + (nextSeqNum + 1));
//                    nextSeqNum++;
//                }
//
//                // Wait for ACKs
//                ackBuffer.clear();
//                while (ackBuffer.position() < 4) {
//                    int bytesRead = clientChannel.read(ackBuffer);
//                    if (bytesRead == -1) throw new IOException("Connection closed");
//                }
//                ackBuffer.flip();
//
//                // Process ACK
//                byte[] ackData = Arrays.copyOf(ackBuffer.array(), ackBuffer.limit());
//                int ackBlockNum = ((ackData[2] & 0xff) << 8 | (ackData[3] & 0xff));
//                System.out.println(YELLOW + "Client: " + RESET + "Received ACK: " + ackBlockNum);
//
//                // Slide window
//                if (ackBlockNum >= base) {
//                    base = ackBlockNum + 1;
//                }
//
//                // If we've sent all packets but not all are ACKed
//                if (nextSeqNum == window.size() && base < window.size()) {
//                    // Handle timeout and retransmission here if needed
//                }
//            }
//            long endTime = System.currentTimeMillis();
//            System.out.println(endTime - timestamp);


            clientChannel.close();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
}