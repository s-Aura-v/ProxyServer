package org.network;

/*
For now, I'll keep all my constants in here.
Once the project works, I'll move them to the appropriate class.
 */
public final class Config {
    // TCP SLIDING WINDOW
    public static final int WINDOW_SIZE = 0;


    // PACKET
    int READ_OPCODE = 1;
    int WRITE_OPCODE = 2;
    String MODE = "octet";
    static byte[] END = new byte[]{0};



    // DROP RATE
    double DROP_RATE = 0.01;



    private Config() {

    }
}
