package org.network;

import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.channels.SelectionKey;
import java.nio.channels.Selector;
import java.nio.channels.SocketChannel;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Arrays;

import static org.network.Config.*;

public class Handler implements Runnable {
    int MAX_IN = 512;
    int MAX_OUT = 512;

    final SocketChannel socket;
    final SelectionKey sk;
    ByteBuffer input = ByteBuffer.allocate(MAX_IN);
    ByteBuffer output = ByteBuffer.allocate(MAX_OUT);
    static final int READING = 0, SENDING = 1;
    int state = READING;

    static Cache cache;
    static ArrayList<Integer> acks = new ArrayList<>();

    public Handler(Selector sel, SocketChannel c) throws IOException {
        cache = new Cache();
        socket = c;
        c.configureBlocking(false);
        // 0 vs 1? causing issue.
        sk = socket.register(sel, SelectionKey.OP_READ);
        sk.attach(this);
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
            try {
                socket.close();
            } catch (IOException ex) {
                // ignore
            }
            sk.cancel();
            e.printStackTrace();
        }
    }

    void read() throws IOException {
        input.clear();
        int bytesRead = socket.read(input);

        if (bytesRead == -1) {
            System.out.println("Connection closed by client");
            sk.cancel();
            socket.close();
            return;
        } else if (bytesRead == 0) {
            System.out.println("Read 0 bytes, waiting for more data");
            return;
        }

        input.flip();
        byte[] receivedData = new byte[input.remaining()];
        input.get(receivedData);

        int blockNumber = ((receivedData[2] & 0xff) << 8) | (receivedData[3] & 0xff);
        // data manipulation

        byte[] packetData = Arrays.copyOfRange(receivedData, BLOCK_SIZE + OPCODE_SIZE, receivedData.length);
        String url = new String(packetData, StandardCharsets.UTF_8);
        String safeUrl = url.replaceAll("/", "__");


        byte[] imageBytes;
        if (cache.hasKey(safeUrl)) {
            imageBytes = cache.get(safeUrl);
        } else {
            downloadImage(safeUrl);
            imageBytes = imageToBytes(CACHE_PATH + safeUrl);
            cache.addToCache(safeUrl, imageBytes);
        }
        ArrayList<byte[]> tcpSlidingWindow = createTCPSlidingWindow(imageBytes);
        int leftPointer = 0;
        int rightPointer = leftPointer;

        while (leftPointer < tcpSlidingWindow.size()) {
            while ((rightPointer < leftPointer + SEND_WINDOW_SIZE) && (rightPointer < tcpSlidingWindow.size())) {
                output = ByteBuffer.wrap(tcpSlidingWindow.get(rightPointer));
                socket.write(output);
                output.clear();
                rightPointer++;
            }

            leftPointer++;
            // REMINDER:
            // PURPOSE: SEE IF WE RECEIVED AN ACK, IF SO, MOVE LEFT POINTER UP
            // WHERE
//            ByteBuffer ackBuffer = ByteBuffer.allocate(4);
//            byte[] receivedAck = new byte[input.remaining()];
//            while (ackBuffer.hasRemaining()) {
//                ackBuffer.get(receivedAck);
//            }
//            int ackBlockNum = ((receivedAck[2] & 0xff) << 8) | (receivedAck[3] & 0xff);
//            System.out.println(ackBlockNum);
//            acks.add(ackBlockNum);
//            if (acks.contains(leftPointer)) {
//                leftPointer++;
//            }

//            ByteBuffer ackBuffer = ByteBuffer.allocate(4);
//            int ackBytes = socket.read(ackBuffer);
//            while (ackBytes < 4) {
//                System.out.println("ACK packet incomplete, waiting for more data");
//                return;
//            }
//            System.out.println("Ack acknowledged");
//            ackBuffer.flip();
//            byte[] receivedAck = new byte[ackBuffer.remaining()];
//            while (ackBuffer.hasRemaining()) {
//                ackBuffer.get(receivedAck);
//            }
//            System.out.println("acked");
//
//            int ackBlockNum = ((receivedAck[2] & 0xff) << 8) | (receivedAck[3] & 0xff);
//            System.out.println(ackBlockNum);
//            acks.add(ackBlockNum);
//            while (acks.contains(leftPointer)) {
//                leftPointer++;
//            }
        }

        acks.clear();
        state = SENDING;
        sk.interestOps(SelectionKey.OP_WRITE);
        sk.selector().wakeup();
    }

    void write() throws IOException {
        System.out.println("Entering WRITE state");
        socket.write(output);

        if (output.hasRemaining()) {
            System.out.println("Output buffer not empty, will continue writing");
            return;
        }

        output.clear();
        state = READING;
        sk.interestOps(SelectionKey.OP_READ);
        sk.selector().wakeup();
    }
}

