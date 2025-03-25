package org.network;

import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.channels.SelectionKey;
import java.nio.channels.Selector;
import java.nio.channels.SocketChannel;

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

    // write the code for reading here
    boolean inputIsComplete() { /* ... */
        System.out.println("handled!");
        return true;
    }

    // write the code for writing here
    // if writing failed, return false and continues the loop going (maybe); but client needs to send packet again
    // if writing passed, complete and finishes the current iteration
    boolean outputIsComplete() { /* ... */
        System.out.println("not handled!");
        return true;
    }

    void process() {
    }

    @Override
    public void run() {
        try {
            if (state == READING) {
                read();
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    // once input is read, set to reading mode so that we can send an ack back
    void read() throws IOException {
        // number of bytes read
        int bytesRead = socket.read(input);
        input.flip(); // Prepare to read the buffer
        byte[] receivedData = new byte[input.remaining()];
        input.get(receivedData);
        System.out.println("As string: " + new String(receivedData));


        if (inputIsComplete()) {
            process();
            state = SENDING;
            // Normally also do first write now
            sk.interestOps(SelectionKey.OP_WRITE);
        }
    }


    // once output is sent, then cancel the key and let the new iteration run
    void send() throws IOException {
        socket.write(output);
        if (outputIsComplete()) sk.cancel();
    }
}
