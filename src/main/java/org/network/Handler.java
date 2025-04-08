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
    ArrayList<Integer> acks = new ArrayList<>();
    ArrayList<byte[]> tcpSlidingWindow = new ArrayList<>();
    int leftPointer = 0;
    int rightPointer = 0;
    boolean sendingImage = false; // new state to differentiate url vs ack
    byte[] key = new byte[KEY_SIZE];

    long lastAckTime = System.currentTimeMillis();
    boolean packetLost = false;


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
            return;
        }

        input.flip();
        byte[] receivedData = new byte[input.remaining()];
        input.get(receivedData);

        // Case 1: ACK Received
        if (receivedData.length >= 4 && receivedData[1] == 4) {
            for (int i = 0; i + 3 < receivedData.length; i += 4) {
                int ackBlockNum = ((receivedData[i + 2] & 0xff) << 8) | (receivedData[i + 3] & 0xff);
                if (!acks.contains(ackBlockNum)) {
                    acks.add(ackBlockNum);
                }
                lastAckTime = System.currentTimeMillis();
            }

// Try to slide the window forward
            while (acks.contains(leftPointer)) {
                leftPointer++;
                if (leftPointer < tcpSlidingWindow.size()) {
                    state = SENDING;
                    sk.interestOps(SelectionKey.OP_WRITE);
                    sk.selector().wakeup();
                }
            }

            return;
        }

        // Case 2: New Image Request
        byte[] packetData = Arrays.copyOfRange(receivedData, BLOCK_SIZE + OPCODE_SIZE + KEY_SIZE, receivedData.length);
        key = (Arrays.copyOfRange(receivedData, BLOCK_SIZE + OPCODE_SIZE, receivedData.length - packetData.length));
        String url = new String(packetData, StandardCharsets.UTF_8);
        String safeUrl = url.replaceAll("/", "__");
        System.out.println(url);

        byte[] imageBytes;
        if (cache.hasKey(safeUrl)) {
            imageBytes = cache.get(safeUrl);
        } else {
            downloadImage(safeUrl);
            imageBytes = imageToBytes(CACHE_PATH + safeUrl);
            cache.addToCache(safeUrl, imageBytes);
        }

        tcpSlidingWindow = createTCPSlidingWindow(imageBytes);
        leftPointer = 0;
        rightPointer = 0;
        sendingImage = true;

        state = SENDING;
        sk.interestOps(SelectionKey.OP_WRITE);
        sk.selector().wakeup();
    }

    void write() throws IOException {
        if (!sendingImage || tcpSlidingWindow.isEmpty()) return;

        while ((rightPointer < leftPointer + SEND_WINDOW_SIZE) &&
                (rightPointer < tcpSlidingWindow.size())) {
            byte[] packet = tcpSlidingWindow.get(rightPointer);
            byte[] encrypted = encryptionCodec(packet, key);
            socket.write(ByteBuffer.wrap(encrypted));
            rightPointer++;
        }

        if (leftPointer >= tcpSlidingWindow.size()) {
            System.out.println("All packets sent, waiting for final ACKs");
            sendingImage = false;
        }

        state = READING;
        sk.interestOps(SelectionKey.OP_READ);
        sk.selector().wakeup();
    }

//    void handleTimeout() {
//        if (System.currentTimeMillis() - lastAckTime > TIMEOUT) {
//            packetLost = true;
//        }
//    }
    public void checkAckTimeout(long now) {
        if (!sendingImage || leftPointer >= tcpSlidingWindow.size()) return;

        if ((now - lastAckTime) > TIMEOUT) {
            System.out.println("Timeout for block " + leftPointer + " — resending");

            try {
                byte[] packet = tcpSlidingWindow.get(leftPointer);
                byte[] encrypted = encryptionCodec(packet, key);
                socket.write(ByteBuffer.wrap(encrypted));
                lastAckTime = now; // reset after resend
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
    }

}


