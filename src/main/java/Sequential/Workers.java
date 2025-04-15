package Sequential;

import java.io.*;
import java.net.URL;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.security.SecureRandom;

import static Sequential.Packets.*;

public class Workers {
    public static final String CACHE_PATH = "src/main/resources/img-cache/";

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
