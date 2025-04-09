package Sequential;

import java.io.*;
import java.net.URL;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.security.SecureRandom;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.concurrent.ThreadLocalRandom;

public class Workers {
    public static final int MAX_PACKET_SIZE = 512;
    public static final int OPCODE_SIZE = 2;
    public static final int BLOCK_SIZE = 2;
    public static final int KEY_SIZE = 64;
    public static final String CACHE_PATH = "src/main/resources/img-cache/";

    // 1 - READ / 2 - WRITE

    // URL Packet = opcode + encryption-key + url
    // Bytes: 2 + -key-size- + -url-size-
    public static byte[] createURLPacket(byte[] data, byte[] key) throws IOException {
        // Data Packets: opcode + block # + key + data
        // the key is 64 bytes long

        ByteArrayOutputStream output = new ByteArrayOutputStream();
        output.write(new byte[]{0x00, 0x02});
        output.write(key); // Key
        output.write(data);

        return output.toByteArray();
    }

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

    public static byte[] generateSessionKey() {
        SecureRandom secureRandom = new SecureRandom();
        byte[] byteArray = new byte[KEY_SIZE];
        secureRandom.nextBytes(byteArray);
        return byteArray;
    }

    public static byte[] encryptionCodec(byte[] packetData, byte[] key) {
        byte[] encrypted = new byte[packetData.length];
        // Keep the op-code and block number the same
        encrypted[0] = packetData[0];
        encrypted[1] = packetData[1];
        encrypted[2] = packetData[2];
        encrypted[3] = packetData[3];
        // start the encryption for the data
        for (int i = BLOCK_SIZE + OPCODE_SIZE; i < packetData.length; i++) {
            encrypted[i] = (byte) (packetData[i] ^ key[i % key.length]);
        }
        return encrypted;
    }

    public static byte[] extractPacketData(byte[] dataPacket) {
        byte[] data = new byte[dataPacket.length - OPCODE_SIZE - BLOCK_SIZE];
        System.arraycopy(dataPacket, OPCODE_SIZE + BLOCK_SIZE, data, 0, dataPacket.length - OPCODE_SIZE - BLOCK_SIZE);
        return data;
    }

    // the ack packet is used to slide the window over
    public static byte[] createACKPacket(int blockNum) throws IOException {
        // ACK Packets: opcode (4) + blockNum
        // both are 2 bytes long = 4 bytes long
        ByteArrayOutputStream output = new ByteArrayOutputStream();
        output.write(new byte[]{0x00, 0x04});
        output.write((byte) (blockNum >> 8));
        output.write((byte) (blockNum & 0xFF));
        return output.toByteArray();
    }


    public static void downloadImage(String safeURL) throws IOException {
        String fileURL = safeURL.replaceAll("__", "/");
        InputStream in = new URL(fileURL).openStream();
        Files.copy(in, Paths.get("src/main/resources/img-cache/" + safeURL), StandardCopyOption.REPLACE_EXISTING);
    }

    static byte[] imageToBytes(String filePath) throws IOException {
        File file = new File(filePath);
        return Files.readAllBytes(file.toPath());
    }

    static void bytesToImage(byte[] imageBytes, String safeURL) {
        File file = new File(CACHE_PATH, safeURL);
        try (FileOutputStream fos = new FileOutputStream(file)) {
            fos.write(imageBytes);
        } catch (IOException e) {
            System.out.println("Packet incomplete; Unable to save file ");
        }
    }






}
