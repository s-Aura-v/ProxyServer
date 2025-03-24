package org.network;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;

/*
For now, I'll keep all my constants in here.
Once the project works, I'll move them to the appropriate class.
 */
public final class Config {
    // TCP SLIDING WINDOW
    public static final int WINDOW_SIZE = 0;

    // PACKET
    // OPCODE: 1 = READ | 2 = WRITE | 3 = DATA | 4 = ACK | 5 = ERROR | 6 = OACK
    static String MODE = "octet";
    static byte[] END = new byte[]{0};


    // MISC
    double DROP_RATE = 0.01;
    long seed = 123123123;


    private Config() {
    }

    static byte[] createRequestPacket(boolean reader, String title) throws IOException {
        // RRQ/WRQ = opcode + string  + null terminator + mode + null terminator;

        // reminder: OpCode takes 2 bytes of space.
        byte[] opcodeBytes = {0x00, 0x00};
        if (reader) {
           opcodeBytes = new byte[]{0x00, 0x01};  // binary: 00000000 00000001
        } else {
            opcodeBytes = new byte[]{0x00, 0x02};  // binary: 00000000 00000001
        }

        ByteArrayOutputStream output = new ByteArrayOutputStream();
        output.write(opcodeBytes);
        output.write(title.getBytes());
        output.write(0);
        output.write(MODE.getBytes());
        output.write(0);
        byte[] requestPacket = output.toByteArray();

        // Debug for formula
        int totalSize = (2) + ( title.getBytes().length + 1) + (MODE.getBytes().length + 1);
        System.out.print("Created Packet: " + totalSize + " bytes: ");
        System.out.println(Arrays.toString(requestPacket));

        return requestPacket;
    }

    //byte b = (byte)0xC8;
    //int v1 = b;       // v1 is -56 (0xFFFFFFC8)
    //int v2 = b & 0xFF // v2 is 200 (0x000000C8)
    // there's a reason v2 is better

    static byte[] createDataPacket(String data, int blockNum) throws IOException {
        // Data Packets: opcode + block # + data

        ByteArrayOutputStream output = new ByteArrayOutputStream();
        output.write(new byte[]{0x00, 0x03});
        // If the size is too big to fit inside a 8 byte code, then you have to split it into low and high bytes.
        // 16 bytes = 128 bits = 2^128 amount of bits
        output.write((byte) (blockNum >> 8)); // High byte
        output.write((byte) (blockNum & 0xFF)); // Low byte
        output.write(data.getBytes());
        byte[] dataPacket = output.toByteArray();

        // Debug for formula
        System.out.print(dataPacket.length + " bytes: ");
        System.out.println(Arrays.toString(dataPacket));

        return dataPacket;
    }

    // the ack packet is used to slide the window over
    static byte[] createACKPacket(int blockNum) throws IOException {
        // ACK Packets: opcode (4) + blockNum
        // both are 2 bytes long

        ByteArrayOutputStream output = new ByteArrayOutputStream();
        output.write(new byte[]{0x00, 0x04});
        output.write((byte) (blockNum >> 8));
        output.write((byte) (blockNum & 0xFF));
        byte[] ackPacket = output.toByteArray();

        // Debug
        System.out.print(ackPacket.length + " bytes: ");
        System.out.println(Arrays.toString(ackPacket));

        return ackPacket;
    }

    // The TCP Sliding Window is an arraylist that stores data packets
    // the data packets are byte[]
    static byte[] createTCPSlidingWindow() {
        ArrayList<byte[]> window = new ArrayList<>();

        return new byte[]{0x00, 0x00};
    }

    /**
     * Changes the initial key for encoding and decoding purposes
     * @param r - initial key
     * @return r - new key
     */
    static long xorShift(long r) {
        r ^= r << 13;
        r ^= r >>> 7;
        r ^= r << 17;
        return r;
    }

    public static void main(String[] args) throws IOException {
        System.out.println("RRQ/WRQ Packet Debug");
        createRequestPacket(false, "TEST");
        createRequestPacket(true, "TESTING");

        System.out.println("Data Packet Debug");
        createDataPacket("this is the real data", 1);
        createDataPacket("there's less data", 99999999);

        System.out.println("ACK Packet Debug");
        createACKPacket(10);

        // Index:  0    1    2    3
        // Value:  0x00 0x04 0x02 0x00
        // ackPacket[2] = 0x02 (high byte of block number 512
        // ackPacket[3] = 0x00 (low byte of block number 512)
        // | (bitwise OR): Combines the two bytes into a single int
        // MAX BLOCK NUMBER IS 65536 (2^16) [unsigned]
        byte[] ackPacket = createACKPacket(65534);
        int receivedBlockNum = ((ackPacket[2] & 0xFF) << 8) | (ackPacket[3] & 0xFF);
        System.out.println(receivedBlockNum);
    }
}
