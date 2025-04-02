package org.network;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.nio.channels.SelectionKey;
import java.nio.channels.Selector;
import java.nio.channels.ServerSocketChannel;
import java.nio.channels.SocketChannel;
import java.util.Iterator;
import java.util.Set;

public class Reactor implements Runnable {
    private static final int PORT = 26880;
    final Selector selector;
    final ServerSocketChannel serverSocket;

    public Reactor(int port) throws IOException {
        selector = Selector.open();
        serverSocket = ServerSocketChannel.open();
        serverSocket.socket().bind(new InetSocketAddress(port));
        serverSocket.configureBlocking(false);

        // register what to do when I get a connection, whenever it happens, do it please
        SelectionKey key = serverSocket.register(selector, SelectionKey.OP_ACCEPT);
        // Create the Acceptor which in turns is used to make the handler
        key.attach(new Acceptor());
    }


    // Dispatch Loop

    // this is the main bread and butter
    // The server is running a selector loop, waiting for new events.
    @Override
    public void run() {
        try {
            while (!Thread.interrupted()) {
                selector.select();
                Set<SelectionKey> selected = selector.selectedKeys();
                Iterator<SelectionKey> it = selected.iterator();
                while (it.hasNext()) {
                    SelectionKey key = it.next();
                    it.remove(); //(should i remove the key?)
                    dispatch(key);
                }
                selected.clear();
            }
        } catch (IOException e) {
            e.printStackTrace();
        } finally {
            try {
                selector.close();
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
    }

    // creates the handler using the Acceptor
    void dispatch(SelectionKey key) {
        Runnable r = (Runnable) key.attachment();
        if (r != null) {
            r.run();
        }
    }

    // create a handler that either reads or writes
    class Acceptor implements Runnable {
        @Override
        public void run() {
            try {
                SocketChannel c = serverSocket.accept();
                if (c != null)
                    new Handler(selector, c);
            }
            catch(IOException ex) { /* ... */ }
        }
    }
    // End of Dispatch Loop

    public static void main(String[] args) {
        Thread reactorThread = null;
        try {
            reactorThread = new Thread(new Reactor(PORT));
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
        reactorThread.start();
    }

}
