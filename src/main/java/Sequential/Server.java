package Sequential;

import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.EOFException;
import java.io.IOException;
import java.net.ServerSocket;
import java.net.Socket;
import java.nio.charset.StandardCharsets;
import java.util.Arrays;

import static org.network.Config.*;

public class Server {
    static final int PORT = 26880;

    public static void main(String[] args) {
        System.out.println("Waiting for Connection");
        try (ServerSocket serverSocket = new ServerSocket(PORT)) {
            for (; ; ) {
                Socket client = serverSocket.accept();
                System.out.println("Server Connected");
                client.setSoTimeout(30000);

                try (DataOutputStream out = new DataOutputStream(client.getOutputStream());
                     DataInputStream in = new DataInputStream(client.getInputStream())) {

                    for (; ; ) {
                        try {

                            // CASE 2: READ URL AND DOWNLOAD DATA
                            int length = in.readInt();
                            byte[] receivedData = new byte[length];
                            in.readFully(receivedData);

                            // URL PACKET = OPCODE + KEY + DATA
                            byte[] packetData = Arrays.copyOfRange(receivedData,OPCODE_SIZE + KEY_SIZE, receivedData.length);
                            byte[] key = (Arrays.copyOfRange(receivedData,  OPCODE_SIZE, receivedData.length - packetData.length));
                            String url = new String(packetData, StandardCharsets.UTF_8);
                            String safeUrl = url.replaceAll("/", "__");
                            System.out.println(url);



//                            String message = new String(Helpers.xorEncode(byteArray, Helpers.key));
//                            System.out.println("Decoded byte array: " + message);
//                            out.writeInt(packetData.length);
//                            out.write(byteArray);

                            out.flush();

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
                    System.err.println("Error handling client connection: " + e.getMessage());
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
}
