package Sequential;

import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.EOFException;
import java.io.IOException;
import java.net.ServerSocket;
import java.net.Socket;
import java.net.SocketTimeoutException;
import java.nio.charset.StandardCharsets;
import java.util.*;

import static Sequential.Packets.KEY_SIZE;
import static Sequential.Packets.OPCODE_SIZE;
import static Sequential.Workers.*;

/**
 * A class responsible for communicating with client to download, store, cache, and send image data.
 * <p>
 * The class includes SlidingWindowProtocol, TFIP Packets, TCP Retransmission Timeout and more.
 */
public class Server {
    static final int PORT = 26880;

    static Cache cache = new Cache();
    static ArrayList<byte[]> tcpSlidingWindow = new ArrayList<>();
    static Set<Integer> acks = new HashSet<>();
    static int leftPointer = 0;
    static int rightPointer = 0;
    static double lastAckTime = 0;

    static final int WAITING = 0, SENDING = 1, TERMINATING = 3;
    static int state = WAITING;
    static byte[] encryptionKey;

    static DataOutputStream out;
    static final int TIMEOUT_MS = 1000;
    public static int sendWindowSize = 8;

    public static void main(String[] args) {
        Scanner scanner = new Scanner(System.in);
        System.out.println("Please enter window size: [1, 8, 64]");
        sendWindowSize = scanner.nextInt();
        scanner.close();
        System.out.println("Waiting for Connection");

        try (ServerSocket serverSocket = new ServerSocket(PORT)) {
            for (; ; ) {
                Socket client = serverSocket.accept();
                System.out.println("Server Connected");

                try (DataInputStream in = new DataInputStream(client.getInputStream())) {
                    out = new DataOutputStream(client.getOutputStream());
                    client.setSoTimeout(TIMEOUT_MS);

                    for (; ; ) {
                        try {
                            /* SETUP: GET DATA */
                            int length = 0;
                            try {
                                length = in.readInt();
                            } catch (SocketTimeoutException e) {
                                System.out.println("Packet Drop Error: Resending Packet");
                                writeLostPacket();
                            }
                            byte[] receivedData = new byte[length];
                            in.readFully(receivedData);

                            // CASE 1: ACK
                            if (receivedData.length >= 4 && receivedData[1] == 4) {
                                readAcks(receivedData);
                            }

                            // CASE 2: URL DATA
                            if (state == WAITING) {
                                urlRead(receivedData);
                            }

                            // CASE 3: SLIDING-WINDOW COMMUNICATION
                            if (state == SENDING) {
                                slidingWindowProtocol();
                            }

                            // CASE 4: CLEAR ALL DATA AND GET READY FOR NEXT IMAGE
                            if (state == TERMINATING) {
                                tcpSlidingWindow.clear();
                                acks.clear();
                                state = WAITING;
                                client.setSoTimeout(Integer.MAX_VALUE);
                                out.writeInt(0);
                                System.out.println("Image Complete. Awaiting further images. ");
                            }

                        } catch (EOFException e) {
                            System.out.println("Client disconnected.");
                            break;
                        } catch (IOException e) {
                            System.err.println("Error reading from client: " + e.getMessage());

                            break;
                        }
                    }
                    out.close();
                } catch (IOException e) {
                    System.err.println("Error reading from client: " + e.getMessage());
                } finally {
                    try {
                        client.close();
                    } catch (IOException e) {
                        System.err.println("Error closing client socket: " + e.getMessage());
                    }
                }
            }
        } catch (IOException ex) {
            ex.printStackTrace();
            System.exit(-1);
        }
    }

    /**
     * STATE = WAITING
     * Reads the url packet, uses cache functions, and creates the sliding window starting the communication between server-client.
     * @param receivedData - url packet read by the server
     * @throws IOException cannot usually be thrown, but is kept as safety measure.
     */
    static void urlRead(byte[] receivedData) throws IOException {
        byte[] packetData = Arrays.copyOfRange(receivedData, OPCODE_SIZE + KEY_SIZE, receivedData.length);
        encryptionKey = (Arrays.copyOfRange(receivedData, OPCODE_SIZE, receivedData.length - packetData.length));
        String url = new String(packetData, StandardCharsets.UTF_8);
        String safeUrl = url.replaceAll("/", "__");

        byte[] imageBytes;
        if (cache.hasKey(safeUrl)) {
            imageBytes = cache.get(safeUrl);
        } else {
            Workers.downloadImage(safeUrl);
            imageBytes = Workers.imageToBytes(CACHE_PATH + safeUrl);
            cache.addToCache(safeUrl, imageBytes);
        }

        tcpSlidingWindow = Packets.createTCPSlidingWindow(imageBytes);
        leftPointer = 0;
        rightPointer = 0;

        state = SENDING;
    }

    /**
     * STATE = SENDING; PART 1 OF SLIDING WINDOW PROTOCOL
     * Sends all the data in the send-window.
     * <p>
     * @throws IOException cannot usually be thrown, but is kept as safety measure.
     */
    static void slidingWindowProtocol() throws IOException {
        while (leftPointer + sendWindowSize > rightPointer
                && rightPointer < tcpSlidingWindow.size()) {
            byte[] packet = tcpSlidingWindow.get(rightPointer);
            byte[] encrypted = Workers.encryptionCodec(packet, encryptionKey);
            out.writeInt(encrypted.length);
            out.write(encrypted);
            rightPointer++;
        }
    }

    /**
     * STATE = SENDING; PART 2 OF THE SLIDING WINDOW PROTOCOL:
     * After sliding window has sent data, await acknowledgment from the client and move the window accordingly.
     * <p>
     * @param receivedData - the 4 byte data read by the server
     * @throws IOException cannot usually be thrown, but is kept as safety measure.
     */
    static void readAcks(byte[] receivedData) throws IOException {
        int ackBlockNum = ((receivedData[2] & 0xff) << 8) | (receivedData[3] & 0xff);
        acks.add(ackBlockNum);
        lastAckTime = System.currentTimeMillis();

        while (acks.contains(leftPointer)) {
            leftPointer++;
        }

        if (leftPointer >= tcpSlidingWindow.size()) {
            state = TERMINATING;
        } else {
            state = SENDING;
        }
    }

    /**
     * Writes the left-most data in the sliding window if server does not detect activity in a while.
     * <p>
     * @throws IOException cannot usually be thrown, but is kept as safety measure.
     */
    static void writeLostPacket() throws IOException {
        byte[] packet = tcpSlidingWindow.get(leftPointer);
        out.writeInt(packet.length);
        out.write(packet);
    }
}
