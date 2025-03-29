package org.network;

import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.io.*;
import java.net.URL;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.util.ArrayList;
import java.util.Arrays;

/*
For now, I'll keep all my constants in here.
Once the project works, I'll move them to the appropriate class.
 */
public final class Config {
    // TCP SLIDING WINDOW
    public static final int SEND_WINDOW_SIZE = 4;
    public static final int MAX_PACKET_SIZE = 512;
    public static final int OPCODE_SIZE = 2;
    public static final int BLOCK_SIZE = 2;


    // PACKET
    // OPCODE: 1 = READ | 2 = WRITE | 3 = DATA | 4 = ACK | 5 = ERROR | 6 = OACK

    // MISC
    double DROP_RATE = 0.01;
    long seed = 123123123;


    private Config() {
    }

    // The TCP Sliding Window is an arraylist that stores data packets
    // the data packets are byte[]
    static ArrayList<byte[]> createTCPSlidingWindow(byte[] imageData) throws IOException {
        ArrayList<byte[]> window = new ArrayList<>();
        int packetSize = MAX_PACKET_SIZE - OPCODE_SIZE - BLOCK_SIZE;
        for (int i = 0; i < imageData.length; i+=packetSize) {
            byte[] partition = Arrays.copyOfRange(imageData, i, Math.min(imageData.length, i + packetSize));
            byte[] packet = createDataPacket(partition, i);
            window.add(packet);
        }
        System.out.println(window.size());
        return window;
    }


    //byte b = (byte)0xC8;
    //int v1 = b;       // v1 is -56 (0xFFFFFFC8)
    //int v2 = b & 0xFF // v2 is 200 (0x000000C8)
    // there's a reason v2 is better

    static byte[] createDataPacket(byte[] data, int blockNum) throws IOException {
        // Data Packets: opcode + block # + data

        ByteArrayOutputStream output = new ByteArrayOutputStream();
        output.write(new byte[]{0x00, 0x03});
        // If the size is too big to fit inside a 8 byte code, then you have to split it into low and high bytes.
        // 16 bytes = 128 bits = 2^128 amount of bits
        output.write((byte) (blockNum >> 8)); // High byte
        output.write((byte) (blockNum & 0xFF)); // Low byte
        output.write(data);
        byte[] dataPacket = output.toByteArray();

        // Debug for formula
        System.out.print(dataPacket.length + " bytes: ");
        System.out.println(Arrays.toString(dataPacket));

        return dataPacket;
    }

    static byte[] extractPacketData(byte[] dataPacket) {
        byte[] data = new byte[dataPacket.length - OPCODE_SIZE - BLOCK_SIZE];
        System.arraycopy(dataPacket, OPCODE_SIZE + BLOCK_SIZE, data, 0, dataPacket.length - OPCODE_SIZE - BLOCK_SIZE);
        return data;
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
        System.out.print("Created ACK: " + ackPacket.length + " bytes: ");
        System.out.println(Arrays.toString(ackPacket));

        return ackPacket;
    }

    // Creating the test cases to see if the window sliding mechanic works
//    static void createTestCases(ArrayList<byte[]> window) throws IOException {
//        window.add(0, createDataPacket("https://s-aura-v.com/assets/F24_P3-BlldWeW3.png", 1));
//        window.add(1, createDataPacket("https://s-aura-v.com/assets/F24_P2-CxOsZN5F.png", 2));
//        window.add(2, createDataPacket("https://s-aura-v.com/assets/F24_P1-C98K-uUV.png", 3));
//        window.add(3, createDataPacket("https://cdn.gamerbraves.com/2022/01/kirby-1.jpg", 4));
//        window.add(4, createDataPacket("https://miro.medium.com/v2/resize:fit:401/1*FkSpGx7vW0irUrDSjc0X-Q.jpeg", 5));
//        window.add(5, createDataPacket("https://static.wikia.nocookie.net/severance-series/images/6/62/Promo-Severance.jpg", 6));
//    }

    // Download URL into machine
    static void downloadImage(String fileName, String fileURL) throws IOException {
        InputStream in = new URL(fileURL).openStream();
        Files.copy(in, Paths.get("src/main/resources/img-cache/" + fileName), StandardCopyOption.REPLACE_EXISTING);
    }

    // convert image to bytes
    static byte[] imageToBytes(String filePath) throws IOException {
        File file = new File(filePath);
        byte[] imageBytes = Files.readAllBytes(file.toPath());
        System.out.println(imageBytes.length);
        return imageBytes;
    }

    // convert bytes to images
    static void bytesToImage(byte[] imageBytes) throws IOException {
        BufferedImage img = ImageIO.read(new ByteArrayInputStream(imageBytes));
        File outputfile = new File("src/main/resources/img-cache/test-case.jpg");
        ImageIO.write(img, "jpg", outputfile);
    }

    /**
     * Changes the initial key for encoding and decoding purposes
     *
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
        System.out.println("Data Packet Debug");
//        createDataPacket("this is the real data", 1);
//        createDataPacket("there's less data", 99999999);
//
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


        // image download
//        byte[] image = imageToBytes("src/main/resources/test-cases/hunter.jpg");
//        bytesToImage(image);
//
        byte[] image2 = imageToBytes("src/main/resources/test-cases/qr-code.jpeg");
//        bytesToImage(image2);


        // TESTING PACKETS TO IMAGE
        ArrayList<byte[]> imagePackets = createTCPSlidingWindow(image2);
        ByteArrayOutputStream output = new ByteArrayOutputStream();
        for (byte[] imagePacket : imagePackets) {
            byte[] extracted = extractPacketData(imagePacket);
            output.write(extracted);
        }
        byte[] finalImageFrame = output.toByteArray();
        bytesToImage(finalImageFrame);

    }

    // Deprecated, but still worth understanding.
    static String MODE = "octet";
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
        int totalSize = (2) + (title.getBytes().length + 1) + (MODE.getBytes().length + 1);
        System.out.print("Created Packet: " + totalSize + " bytes: ");
        System.out.println(Arrays.toString(requestPacket));

        return requestPacket;
    }

    public static final String RESET = "\033[0m";  // Text Reset
    public static final String BLACK = "\033[0;30m";   // BLACK
    public static final String RED = "\033[0;31m";     // RED
    public static final String GREEN = "\033[0;32m";   // GREEN
    public static final String YELLOW = "\033[0;33m";  // YELLOW
    public static final String BLUE = "\033[0;34m";    // BLUE
    public static final String PURPLE = "\033[0;35m";  // PURPLE
    public static final String CYAN = "\033[0;36m";    // CYAN
    public static final String WHITE = "\033[0;37m";   // WHITE

}
