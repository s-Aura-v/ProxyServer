package org.network;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.net.ServerSocket;
import java.nio.ByteBuffer;
import java.nio.channels.Selector;
import java.nio.channels.ServerSocketChannel;
import java.nio.channels.SocketChannel;
import java.util.Arrays;

public class Client implements Runnable {
    @Override
    public void run() {
        try {
            // Create a SocketChannel and connect to the server at localhost:26880
            SocketChannel clientChannel = SocketChannel.open(new InetSocketAddress("localhost", 26880));
            clientChannel.configureBlocking(false);  // Non-blocking mode

            // Send some data to the server
            byte[] requestPacket = Config.createRequestPacket(true, "read-test");
            ByteBuffer buffer = ByteBuffer.wrap(requestPacket);
            clientChannel.write(buffer);
            System.out.println("Sent message: " + Arrays.toString(requestPacket));

            // Close the channel after sending data
            clientChannel.close();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public static void start() {

    }
}
