package org.network;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.util.Arrays;

/*
For now, I'll keep all my constants in here.
Once the project works, I'll move them to the appropriate class.
 */
public final class Config {
    // TCP SLIDING WINDOW
    public static final int WINDOW_SIZE = 0;


    // PACKET
    static int READ_OPCODE = 1;
    static int WRITE_OPCODE = 2;
    static String MODE = "octet";
    static byte[] END = new byte[]{0};



    // DROP RATE
    double DROP_RATE = 0.01;


    private Config() {
    }

    static byte[] createRequestPacket(boolean reader, String title) throws IOException {
        // RRQ/WRQ = opcode + string  + null terminator + mode + null terminator;
        byte[] opcodeBytes = {0x00, 0x00};

        // reminder: OpCode takes 2 bytes of space.
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
        int totalSize = (1 + READ_OPCODE) + ( title.getBytes().length + 1) + (MODE.getBytes().length + 1);
        System.out.print(totalSize + " bytes : ");
        System.out.println(Arrays.toString(requestPacket));

        return requestPacket;
    }

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
        System.out.print(dataPacket.length + " bytes : ");
        System.out.print(Arrays.toString(dataPacket));

        return dataPacket;
    }

    public static void main(String[] args) throws IOException {
        System.out.println("RRQ/WRQ Packet Debug");
        createRequestPacket(false, "TEST");
        createRequestPacket(true, "TESTING");

        System.out.println("Data Packet Debug");
        createDataPacket("this is the real data", 1);
        createDataPacket("this is the fake data, but really, there's more", 99999999);
    }
}
