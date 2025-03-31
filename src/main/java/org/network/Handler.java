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
        System.out.println("Entering READ state");
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
        System.out.println("Received: " + new String(receivedData));

        // Process packet
        if (receivedData.length >= 2 && receivedData[0] == 0 && receivedData[1] == 2) {
            output = ByteBuffer.wrap(Config.createACKPacket(0));
            System.out.println("WRQ received, sending ACK");
        } else {
            int blockNumber = ((receivedData[2] & 0xff) << 8) | (receivedData[3] & 0xff);
            // data manipulation

            byte[] packetData = Arrays.copyOfRange(receivedData, 4, receivedData.length);
            String url = new String(packetData, StandardCharsets.UTF_8);
            String safeUrl = url.replaceAll("/","__");
            System.out.println(safeUrl);


//            if (cache.hasKey(url)) {
//                // get cached data and send
//            } else {
//               // add actual image bytes to cache
//            }

            downloadImage(safeUrl);
            byte[] imageBytes = imageToBytes(CACHE_PATH + safeUrl);
            ArrayList<byte[]> tcpSlidingWindow = createTCPSlidingWindow(imageBytes);
            System.out.println(tcpSlidingWindow);

            int leftPointer = 0;
            int rightPointer = leftPointer + 1;
            while (leftPointer < tcpSlidingWindow.size()) {
                while ((rightPointer < leftPointer + SEND_WINDOW_SIZE) && (rightPointer < tcpSlidingWindow.size())) {
                    output = ByteBuffer.wrap(tcpSlidingWindow.get(leftPointer));
                    socket.write(output);
                    output.clear();
                    rightPointer++;
                }
                leftPointer++;
            }

            System.out.println("Write Complete" + blockNumber);
        }

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

        System.out.println("Write completed, switching to READ");
        output.clear();
        state = READING;
        sk.interestOps(SelectionKey.OP_READ);
        sk.selector().wakeup();
    }
}

