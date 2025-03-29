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
        // IMPORTANT: Clear buffer before reading
        input.clear();
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

        // Check if this is a WRQ (first packet)
        if (receivedData.length >= 2 && receivedData[0] == 0 && receivedData[1] == 2) {
            output = ByteBuffer.wrap(Config.createACKPacket(0));
        } else {
            // For data packets, parse block number and ACK it
            int blockNumber = ((receivedData[2] & 0xff) << 8) | (receivedData[3] & 0xff);
            output = ByteBuffer.wrap(Config.createACKPacket(blockNumber));
            // Save it to cache
        }

        // Switch to sending mode
        state = SENDING;
        sk.interestOps(SelectionKey.OP_WRITE);
    }

    void write() throws IOException {
        socket.write(output);
        if (output.hasRemaining()) {
            // Not all bytes were written, keep trying
            return;
        }

        // Switch back to reading mode for next packet
        state = READING;
        sk.interestOps(SelectionKey.OP_READ);
    }
}
