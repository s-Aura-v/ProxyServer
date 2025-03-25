package org.network;

import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.channels.SelectionKey;
import java.nio.channels.Selector;
import java.nio.channels.SocketChannel;

import static org.network.Config.*;

public class Handler implements Runnable {
    int MAX_IN = 500;
    int MAX_OUT = 500;

    final SocketChannel socket;
    final SelectionKey sk;
    ByteBuffer input = ByteBuffer.allocate(MAX_IN);
    ByteBuffer output = ByteBuffer.allocate(MAX_OUT);
    static final int READING = 0, SENDING = 1;
    int state = READING;

    public Handler(Selector sel, SocketChannel c) throws IOException {
        socket = c;
        c.configureBlocking(false);

        // Optionally try first read now
        sk = socket.register(sel, 0);
        sk.attach(this);
        sk.interestOps(SelectionKey.OP_READ);
        sel.wakeup();
    }

    @Override
    public void run() {
        try {
            if (state == READING) {
                read();
            } else if (state == SENDING) {
                write();
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    void read() throws IOException {
        int bytesRead = socket.read(input);
        if (bytesRead == -1) {
            sk.cancel();
            socket.close();
            return;
        }

        input.flip();
        byte[] receivedData = new byte[input.remaining()];
        input.get(receivedData);
        System.out.println(BLUE + "Received: " + RESET + new String(receivedData));

        // Prepare ACK response
        output = ByteBuffer.wrap(Config.createACKPacket(0));

        // Switch to sending mode
        state = SENDING;
        sk.interestOps(SelectionKey.OP_WRITE);
    }



    // once input is read, set to reading mode so that we can send an ack back
    void write() throws IOException {
        // number of bytes read
        int bytesRead = socket.read(input);
        input.flip(); // Prepare to read the buffer
        byte[] receivedData = new byte[input.remaining()];
        input.get(receivedData);
        System.out.println("As string: " + new String(receivedData));
        output = ByteBuffer.wrap(Config.createACKPacket(0));
        send();
    }

    // once output is sent, then cancel the key and let the new iteration run
    void send() throws IOException {
        socket.write(output);
        if (inputIsComplete()) {
            sk.cancel(); // Close connection after sending ACK
            socket.close();
        } else {
            state = READING;
            sk.interestOps(SelectionKey.OP_READ);
        }
    }


    private boolean inputIsComplete() {

        return true;
    }
}
