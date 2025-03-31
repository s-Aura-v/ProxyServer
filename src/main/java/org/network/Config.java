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
    // OPCODE: 1 = READ | 2 = WRITE | 3 = DATA | 4 = ACK | 5 = ERROR | 6 = OACK | 7 = DATA PACKET COMPLETE (custom)
    public static final int SEND_WINDOW_SIZE = 4;
    public static final int MAX_PACKET_SIZE = 512;
    public static final int OPCODE_SIZE = 2;
    public static final int BLOCK_SIZE = 2;
    public static final String CACHE_PATH = "src/main/resources/img-cache/";

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
//        byte[] finalPacketHead = new byte[MAX_PACKET_SIZE];
//        Arrays.fill(finalPacketHead, (byte) 7);
//        byte[] finalPacket = window.getLast();
//        finalPacket[0] = 7;
//        finalPacketHead = Arrays.copyOfRange(finalPacket, 0, finalPacket.length);
//        System.out.println(finalPacket.length);
//        System.out.println(finalPacketHead.length);
//        window.set(window.size() - 1, finalPacketHead);
        byte[] testPacket = window.get(window.size() - 5);
        testPacket[0] = 7;
        window.set(window.size() - 5, testPacket);

        System.out.println(window);
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

        return output.toByteArray();
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

    // Download URL into machine
    static void downloadImage(String safeURL) throws IOException {
        String fileURL = safeURL.replaceAll("__", "/");
        System.out.println(fileURL);
        InputStream in = new URL(fileURL).openStream();
        Files.copy(in, Paths.get("src/main/resources/img-cache/" + safeURL), StandardCopyOption.REPLACE_EXISTING);
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
        if (imageBytes != null) {
            BufferedImage img = ImageIO.read(new ByteArrayInputStream(imageBytes));
            File outputfile = new File("src/main/resources/img-cache/test-case.jpg");
            ImageIO.write(img, "jpg", outputfile);
        }
    }

    public static void main(String[] args) throws IOException {

    }

}
