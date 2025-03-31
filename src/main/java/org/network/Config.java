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
        for (int i = 0; i < imageData.length; i += packetSize) {
            byte[] partition = Arrays.copyOfRange(imageData, i, Math.min(imageData.length, i + packetSize));
            byte[] packet = createDataPacket(partition, i);
            window.add(packet);
        }
        // 7 indicates final packet - tells client that it can stop reading.
        byte[] testPacket = window.getLast();
        testPacket[0] = 7;
        window.set(window.size() - 1, testPacket);

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
        InputStream in = new URL(fileURL).openStream();
        Files.copy(in, Paths.get("src/main/resources/img-cache/" + safeURL), StandardCopyOption.REPLACE_EXISTING);
    }

    // convert image to bytes
    static byte[] imageToBytes(String filePath) throws IOException {
        File file = new File(filePath);
        return Files.readAllBytes(file.toPath());
    }

    // convert bytes to images
    static void bytesToImage(byte[] imageBytes, String safeURL) throws IOException {
        try {
            BufferedImage img = ImageIO.read(new ByteArrayInputStream(imageBytes));
            File outputfile = new File("src/main/resources/img-cache/" + safeURL);
            ImageIO.write(img, "png", outputfile);
        } catch (IllegalArgumentException e) {
            System.out.println("Packet information incomplete. Unable to generate image.");
        }
    }

    /*
    [0, 3, 0, 0, -119, 80, 78, 71, 13, 10, 26, 10, 0, 0, 0, 13, 73, 72, 68, 82, 0, 0, 0, 100, 0, 0, 0, 100, 8, 6, 0, 0, 0, 112, -30, -107, 84, 0, 0, 0, 9, 112, 72, 89, 115, 0, 0, 15, 97, 0, 0, 15, 97, 1, -88, 63, -89, 105, 0, 0, 4, 59, 73, 68, 65, 84, 120, -38, -19, -100, 91, 79, -22, 76, 24, -123, 87, -127, 114, 16, -112, 66, 64, 68, 78, 26, 3, 23, -58, -124, -1, -1, 31, 76, -12, -50, 104, -126, 17, 16, 40, 84, -96, 8, -107, 83, -23, -20, -117, 47, 109, 84, -86, 22, -74, -60, -99, 47, -21, -71, -21, -52, -126, -52, -12, -103, 121, 59, -27, 2, -23, -22, -22, 74, -128, -4, 51, -8, 120, 11, 40, -124, 80, 8, -123, 16, 10, -95, 16, 66, 33, 20, 66, 40, -124, 66, 8, -123, 16, 10, -95, 16, 66, 33, 20, 66, 40, -124, 66, 8, -123, 80, 8, -95, 16, 66, 33, 20, 66, 40, -124, 66, 8, -123, 80, 8, -95, 16, 10, 33, 20, 66, 40, -124, 66, 8, -123, 80, 8, -95, 16, 10, 33, 20, 66, 33, -124, 66, 8, -123, 80, 8, -95, 16, 10, 33, 63, 68, -32, 111, 62, -84, -21, 58, 38, -109, 9, -118, -59, -94, 107, -1, 96, 48, -128, -90, 105, -104, -49, -25, -120, 68, 34, -56, 102, -77, 80, 20, 101, -25, -36, 62, -24, 116, 58, 8, 6, -125, 72, -89, -45, 27, 125, 66, 8, 116, 58, 29, -24, -70, 14, -45, 52, 17, -115, 70, 81, 40, 20, 16, 14, -121, 119, -54, -19, 117, -121, 8, 33, -96, -86, 42, 102, -77, -103, 107, 127, -81, -41, -61, -29, -29, 35, -4, 126, 63, -78, -39, 44, 0, -96, 94, -81, 99, 48, 24, -20, -108, -37, 7, -53, -27, 18, -67, 94, 15, -53, -27, -46, -75, -1, -31, -31, 1, -86, -86, 34, 22, -117, 33, -109, -55, 96, 62, -97, -29, -10, -10, 22, -13, -7, 124, -89, -36, 94, -124, -52, 102, 51, 104, -102, -122, -5, -5, 123, 24, -122, -31, -102, -79, 44, 11, -86, -86, 34, -111, 72, -96, 82, -87, 32, -105, -53, -95, 90, -83, 34, 26, -115, -94, -37, -19, 66, 8, -79, 85, -18, -89, -103, 76, 38, -24, 118, -69, -72, -69, -69, -125, 101, 89, -82, 25, -61, 48, -96, -21, 58, -14, -7, 60, -54, -27, 50, 78, 78, 78, 80, -83, 86, 33, 73, 18, 84, 85, -35, 58, -73, 55, 33, -3, 126, 31, -99, 78, -25, 75, -5, -10, -42, -51, 100, 50, 78, -101, 36, 73, -56, 100, 50, 88, 44, 22, -104, 76, 38, 91, -27, -36, -88, -41, -21, -72, -71, -71, -127, 105, -102, -17, 86, -4, -11, -11, 53, 26, -115, -58, -105, 115, 104, -75, 90, -24, -9, -5, 88, -81, -41, -97, 102, 52, 77, 115, -58, 98, 19, 12, 6, -95, 40, 10, -122, -61]
    [7, 3, 1, -4, -95, -13, 89, -81, -71, -67, 9, 41, -105, -53, -88, -43, 106, -88, -43, 106, -120, 68, 34, -82, 25, 91, 86, 44, 22, 123, -41, 110, 95, -37, 59, -53, 107, -50, -115, 82, -87, -28, -36, 92, -101, 102, -77, -119, 64, 32, -128, 66, -95, -16, -27, 28, 46, 46, 46, 80, -85, -43, 112, 121, 121, -7, 105, 102, -79, 88, 32, 18, -119, -64, -17, -9, -65, 107, -113, -57, -29, 16, 66, 56, -91, -38, 107, 110, -17, -49, -112, -17, 106, -77, 36, 73, 27, -125, 12, 4, -2, 59, 67, -84, 86, -85, -83, 114, 110, -56, -78, -116, 124, 62, -113, -31, 112, -120, -105, -105, 23, -116, 70, 35, -116, -57, 99, -108, 74, -91, -115, -17, -37, -123, -43, 106, -27, -116, -29, -85, -79, 121, -51, 121, 37, -128, 61, 96, -102, -90, -21, 77, -79, -37, -20, -70, -19, 53, -9, 25, -103, 76, 6, -93, -47, 8, -51, 102, 19, -106, 101, 33, -107, 74, 33, -111, 72, -4, -40, 28, -36, 42, -128, -49, -25, -37, -104, -125, -105, -36, -81, -18, -112, 64, 32, -32, 58, 16, -69, -98, -38, -85, -57, 107, -18, -69, 18, -70, 92, 46, 97, 89, -106, 83, -58, -10, 57, 7, -69, -19, -69, 57, 124, -52, -3, -86, 16, 89, -106, 97, 89, -42, -58, 3, -51, -66, -106, 101, 121, -85, -36, 119, -27, 81, 8, -127, -11, 122, -115, -59, 98, -15, -93, 66, -34, 30, 24, -34, -106, -78, -73, 99, -13, -102, -5, 85, 33, -10, 22, -2, -8, 80, -98, 78, -89, 0, -32, -68, 48, 121, -51, 125, -122, 101, 89, 104, 52, 26, 56, 60, 60, 68, 40, 20, -6, -10, 116, -75, 13, -31, 112, 24, -77, -39, 108, 99, -79, 24, -122, 1, 73, -110, 16, 10, -123, -74, -54, -3, -86, 16, 69, 81, -32, -9, -5, 55, 94, -18, -98, -97, -97, 33, -53, -78, 83, -25, -67, -26, 62, -93, -35, 110, 99, -75, 90, -95, 92, 46, -93, 84, 42, -31, -11, -11, 21, -67, 94, -17, 71, -26, -112, 78, -89, 33, -124, -64, 112, 56, 116, -38, 76, -45, -60, 120, 60, 70, 50, -103, 116, -98, 115, 94, 115, -98, 119, -26, 62, -124, -8, 124, 62, -28, 114, 57, 60, 61, 61, 65, -110, 36, -60, -29, 113, -24, -70, -114, -23, 116, -118, 114, -71, -68, 117, -50, 13, -61, 48, -96, 105, 26, -14, -7, 60, -126, -63, 32, -126, -63, 32, 82, -87, 20, 58, -99, 14, 20, 69, -39, 122, 101, 126, 36, 22, -117, 33, -111, 72, -96, -43, 106, 97, -75, 90, 65, -106, 101, 60, 63, 63, 67, 8, -127, -29, -29, -29, -83, 115, -65, 42, 4, 0, -78, -39, 44, 124, 62, 31, 6, -125, 1, 116, 93, -57, -63, -63, 1, -50, -50, -50, -112, 76, 38, 119, -54, -67, 69, 8, -127, 70, -93, -127, 112, 56, -116, -93, -93, 35, -89, -67, 88, 44, 98]
    [0, 3, 3, -8, 60, 30, -93, -39, 108, -94, 82, -87, -4, -11, 28, -50, -49, -49, -47, 110, -73, 49, 26, -115, 96, -102, 38, 98, -79, 24, 78, 79, 79, 55, 78, 85, 94, 115, 94, -112, -8, -65, -67, -1, 22, -4, -7, -99, 66, 8, -123, 80, 8, -95, 16, 10, 33, 20, 66, 33, -124, 66, 40, -124, 80, 8, -95, 16, 10, 33, 20, 66, 33, -124, 66, 40, -124, 80, 8, -123, 16, 10, 33, 20, 66, 33, -124, 66, 40, -124, 80, 8, -123, 16, 10, -95, 16, 66, 33, -124, 66, 40, -124, 80, 8, -123, 16, 10, -95, 16, 66, 33, 20, 66, 40, -124, 80, 8, -123, 16, 10, -95, 16, 66, 33, -1, 83, -2, 0, -52, 108, 119, -65, 95, 26, -113, 99, 0, 0, 0, 0, 73, 69, 78, 68, -82, 66, 96, -126]
     */
    public static void main(String[] args) throws IOException {
//        String url = "https://placehold.jp/100x100.png";
//        String safeUrl = url.replaceAll("/", "__");
//        downloadImage(safeUrl);
//        System.out.println("Download Complete");
//        byte[] imageBytes = (imageToBytes(CACHE_PATH + safeUrl));
//        ArrayList<byte[]> window = createTCPSlidingWindow(imageBytes);
//        for (byte[] bytes : window) {
//            System.out.println(Arrays.toString(bytes));
//        }

        // TESTING PACKETS TO IMAGE
        byte[] image2 = imageToBytes("src/main/resources/test-cases/qr-code.jpeg");
        ArrayList<byte[]> imagePackets = createTCPSlidingWindow(image2);
        ByteArrayOutputStream output = new ByteArrayOutputStream();
        for (byte[] imagePacket : imagePackets) {
            byte[] extracted = extractPacketData(imagePacket);
            output.write(extracted);
        }
        byte[] finalImageFrame = output.toByteArray();
        bytesToImage(finalImageFrame, "url");

    }

}
