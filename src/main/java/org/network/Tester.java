package org.network;

import java.io.IOException;
import java.nio.CharBuffer;
import java.nio.channels.ServerSocketChannel;
import java.nio.channels.SocketChannel;
import java.nio.charset.Charset;
import java.nio.charset.CharsetEncoder;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class Tester {

    public static void main(String[] args) throws Exception{


        ExecutorService service = Executors.newFixedThreadPool(1);
        service.execute( new Reactor(26880));
        service.shutdown();



    }
}
