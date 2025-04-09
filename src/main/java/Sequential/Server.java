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

import static Sequential.Workers.*;

public class Server {
    static final int PORT = 26880;

    static Cache cache = new Cache();
    static ArrayList<byte[]> tcpSlidingWindow = new ArrayList<>();
    static Set<Integer> acks = new HashSet<>();
    static int leftPointer = 0;
    static int rightPointer = 0;
    static double lastAckTime = 0;

    static final int WAITING = 0, SENDING = 1, RECEIVING = 2, TERMINATING = 3;
    static int state = WAITING;
    static byte[] encryptionKey;

    static DataOutputStream out;
    static final int TIMEOUT_MS = 1000;
    public static int sendWindowSize = 8;



    public static void main(String[] args) throws SocketTimeoutException {
        Scanner scanner = new Scanner(System.in);
        System.out.println("Please enter window size: [1, 8, 64]" );
        sendWindowSize = scanner.nextInt();
        scanner.close();
        System.out.println("Waiting for Connection");

        try (ServerSocket serverSocket = new ServerSocket(PORT)) {
            for (; ; ) {
                Socket client = serverSocket.accept();
                System.out.println("Server Connected");

                try (DataInputStream in = new DataInputStream(client.getInputStream())) {
                    out = new DataOutputStream(client.getOutputStream());

                    for (; ; ) {
                        try {
                            /* SETUP: GET DATA */
                            int length = in.readInt();
                            byte[] receivedData = new byte[length];
                            in.readFully(receivedData);
                            System.out.println(Arrays.toString(receivedData));


                            if (state == WAITING) {
                                // GOOD TEST IMAGE: https://i.ytimg.com/vi/2DjGg77iz-A/sddefault.jpg
                                /* CASE 2: READ URL AND DOWNLOAD DATA */
                                // URL PACKET = OPCODE + KEY + DATA
                                System.out.println(receivedData.length);
                                byte[] packetData = Arrays.copyOfRange(receivedData, OPCODE_SIZE + KEY_SIZE, receivedData.length);
                                encryptionKey = (Arrays.copyOfRange(receivedData, OPCODE_SIZE, receivedData.length - packetData.length));
                                String url = new String(packetData, StandardCharsets.UTF_8);
                                String safeUrl = url.replaceAll("/", "__");
                                System.out.println(url);

                                byte[] imageBytes;
                                if (cache.hasKey(safeUrl)) {
                                    imageBytes = cache.get(safeUrl);
                                } else {
                                    Workers.downloadImage(safeUrl);
                                    imageBytes = Workers.imageToBytes(CACHE_PATH + safeUrl);
                                    cache.addToCache(safeUrl, imageBytes);
                                }

                                tcpSlidingWindow = Workers.createTCPSlidingWindow(imageBytes);
                                leftPointer = 0;
                                rightPointer = 0;
                                state = SENDING;
                            }

                            if (state == SENDING) {
                                while (leftPointer + sendWindowSize > rightPointer
                                        && rightPointer < tcpSlidingWindow.size()) {
                                    byte[] packet = tcpSlidingWindow.get(rightPointer);
                                    byte[] encrypted = Workers.encryptionCodec(packet, encryptionKey);
                                    out.writeInt(encrypted.length);
                                    out.write(encrypted);
                                    rightPointer++;
                                }

                                state = RECEIVING;
                                lastAckTime = System.currentTimeMillis();
                            }

                            if (state == RECEIVING) {
                                if (receivedData.length >= 4) {
                                    int ackBlockNum = ((receivedData[2] & 0xff) << 8) | (receivedData[3] & 0xff);
                                    acks.add(ackBlockNum);
                                    lastAckTime = System.currentTimeMillis();

                                    while (acks.contains(leftPointer)) {
                                        leftPointer++;
                                    }

                                    System.out.println("Pointer Value: " + leftPointer);
                                    if (leftPointer >= tcpSlidingWindow.size()) {
                                        state = TERMINATING;
                                    } else {
                                        state = SENDING;
                                    }
                                }
                            }

                            if (state == TERMINATING) {
                                tcpSlidingWindow.clear();
                                acks.clear();
                                state = WAITING;
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
                    in.close();
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

    boolean checkForTimeout() {
        return System.currentTimeMillis() - lastAckTime > TIMEOUT_MS;
    }
}
