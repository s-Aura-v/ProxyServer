package Sequential;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;

/**
 * A utility class for creating TFTP based packets.
 * <p>
 * This class includes methods for creation of data packets, ack packets, data extraction,
 * and other custom packets.
 */
public class Packets {
    public static final int MAX_PACKET_SIZE = 512;
    public static final int OPCODE_SIZE = 2;
    public static final int BLOCK_SIZE = 2;
    public static final int KEY_SIZE = 64;

    /**
     * Creates a byte[] that contains: op-code, encryption-key, and url-data.
     * +-----------+----------------+------------+
     * | OP-Code   | ENCRYPTION-KEY | URL-DATA   |
     * | [2 BYTES] | [64 BYTES]     | [VARIABLE] |
     * +-----------+----------------+------------+
     * <p>
     * @param data - image source url as byte[]
     * @param key - encryption key used for encrypting packet communication
     * @return url byte[] packet used to write image to the server and establish communication.
     * @throws IOException cannot usually be thrown, but is kept as safety measure.
     */
    public static byte[] createURLPacket(byte[] data, byte[] key) throws IOException {
        ByteArrayOutputStream output = new ByteArrayOutputStream();
        output.write(new byte[]{0x00, 0x02});
        output.write(key); // Key
        output.write(data);
        return output.toByteArray();
    }

    /**
     * Creates a byte[] that contains: op-code, block-number, and data.
     * +-----------+-----------+---------------------+
     * |  OP-CODE  | BLOCK-NUM |      IMAGE-DATA     |
     * +-----------+-----------+---------------------+
     * | [2 BYTES] | [2 BYTES] | [AT MOST 508 BYTES] |
     * +-----------+-----------+---------------------+
     * <p>
     * @param data - the partitioned/full bytes for an image
     * @param blockNum - unique number used to identify packet data
     * @return data byte[] packet used to send partitioned/whole image data.
     * @throws IOException cannot usually be thrown, but is kept as safety measure.
     */
    public static byte[] createDataPacket(byte[] data, int blockNum) throws IOException {
        ByteArrayOutputStream output = new ByteArrayOutputStream();
        output.write(new byte[]{0x00, 0x03});
        /*
            int v1 = b;       // v1 is -56 (0xFFFFFFC8)
            int v2 = b & 0xFF // v2 is 200 (0x000000C8)
         */
        output.write((byte) (blockNum >> 8)); // High byte
        output.write((byte) (blockNum & 0xFF)); // Low byte
        output.write(data);
        return output.toByteArray();
    }

    /**
     * Creates an ack packet that contains: op-code and block-number.
     * +-----------+-----------+
     * |  OP-CODE  | BLOCK-NUM |
     * +-----------+-----------+
     * | [2 BYTES] | [2 BYTES] |
     * +-----------+-----------+
     * <p>
     * @param blockNum - the unique identifier of a packet
     * @return acknowledgment byte[] packet used by Client to confirm packet to Server.
     * It is also needed for the sliding window protocol.
     * @throws IOException cannot usually be thrown, but is kept as safety measure.
     */
    public static byte[] createACKPacket(int blockNum) throws IOException {
        ByteArrayOutputStream output = new ByteArrayOutputStream();
        output.write(new byte[]{0x00, 0x04});
        output.write((byte) (blockNum >> 8));
        output.write((byte) (blockNum & 0xFF));
        return output.toByteArray();
    }

    /**
     * Copy the bytes of the partitioned image bytes[] ignoring byte and opcode.
     * <p>
     * @param dataPacket - data packet used for sending image bytes[]
     * @return extracted byte[] containing only the image data
     */
    public static byte[] extractPacketData(byte[] dataPacket) {
        byte[] data = new byte[dataPacket.length - OPCODE_SIZE - BLOCK_SIZE];
        System.arraycopy(dataPacket, OPCODE_SIZE + BLOCK_SIZE, data, 0, dataPacket.length - OPCODE_SIZE - BLOCK_SIZE);
        return data;
    }

    /**
     * Splits imageData into partitions of PACKET_SIZE.
     * <p>
     * @param imageData - the full image data as a byte[]
     * @return a list of bytes[] that split the imageData for tcp communication
     * @throws IOException cannot usually be thrown, but is kept as safety measure.
     */
    static ArrayList<byte[]> createTCPSlidingWindow(byte[] imageData) throws IOException {
        ArrayList<byte[]> window = new ArrayList<>();
        int blockNum = 0;
        int packetSize = MAX_PACKET_SIZE - OPCODE_SIZE - BLOCK_SIZE;
        for (int i = 0; i < imageData.length; i += packetSize) {
            byte[] partition = Arrays.copyOfRange(imageData, i, Math.min(imageData.length, i + packetSize));
            byte[] packet = createDataPacket(partition, blockNum);
            window.add(packet);
            blockNum++;
        }
        return window;
    }
}
