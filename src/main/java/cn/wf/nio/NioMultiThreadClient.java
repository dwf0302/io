package cn.wf.nio;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.nio.channels.SocketChannel;
import java.nio.charset.Charset;

public class NioMultiThreadClient {

    public static void main(String[] args) throws IOException {

        SocketChannel sc = SocketChannel.open();
        sc.connect(new InetSocketAddress("127.0.0.1", 8080));
        // sc.configureBlocking(false);
        sc.write(Charset.defaultCharset().encode("nihaoa"));
        System.in.read();
    }
}
