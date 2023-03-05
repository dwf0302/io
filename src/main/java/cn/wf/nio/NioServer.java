package cn.wf.nio;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.nio.ByteBuffer;
import java.nio.channels.ServerSocketChannel;
import java.nio.channels.SocketChannel;
import java.util.LinkedList;

/**
 * nio 服务器
 *
 * @author dwf
 */
public class NioServer {

    public static void main(String[] args) throws IOException {
        // 存储客户端列表
        LinkedList<SocketChannel> clients = new LinkedList<>();

        // server服务channel
        ServerSocketChannel ss = ServerSocketChannel.open();
        ss.bind(new InetSocketAddress(6666));
        // 设置非阻塞
        ss.configureBlocking(Boolean.FALSE);


        while (true) {
            // 不再阻塞
            SocketChannel client = ss.accept();

            if (client == null) {
                System.out.println("无客户端");
            } else {
                // 客户端设置非阻塞
                client.configureBlocking(Boolean.FALSE);
                clients.add(client);
            }
            ByteBuffer buffer = ByteBuffer.allocate(4096);

            for (SocketChannel sc : clients) {
                if (sc.read(buffer) > 0) {
                    // 其他操作
                }
            }
        }
    }
}
