package Sequential;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.security.SecureRandom;

public class Workers {
    public static final int OPCODE_SIZE = 2;
    public static final int BLOCK_SIZE = 2;
    public static final int KEY_SIZE = 64;
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

    public static byte[] generateSessionKey() {
        SecureRandom secureRandom = new SecureRandom();
        byte[] byteArray = new byte[KEY_SIZE];
        secureRandom.nextBytes(byteArray);
        return byteArray;
    }

}
