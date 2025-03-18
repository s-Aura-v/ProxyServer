package org.network;

import java.io.IOException;
import java.net.ServerSocket;
import java.nio.channels.Selector;
import java.nio.channels.ServerSocketChannel;

public class Client {
    private static final String host = "localhost";
    private static final int PORT = 26880;

    public static void main(String[] args) {
        try (ServerSocket socket = new ServerSocket(PORT)) {



        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }
}
