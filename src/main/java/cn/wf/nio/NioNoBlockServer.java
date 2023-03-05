package cn.wf.nio;

import lombok.extern.slf4j.Slf4j;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.nio.ByteBuffer;
import java.nio.channels.SelectionKey;
import java.nio.channels.Selector;
import java.nio.channels.ServerSocketChannel;
import java.nio.channels.SocketChannel;
import java.nio.charset.Charset;
import java.util.Iterator;
import java.util.Set;

import static cn.wf.utils.ByteBufferUtil.debugAll;

@Slf4j
public class NioNoBlockServer {

    private static void split(ByteBuffer source) {
        source.flip();
        for (int i = 0; i < source.limit(); i++) {
            // 找到一条完整消息
            if (source.get(i) == '\n') {
                int length = i + 1 - source.position();
                // 把这条完整消息存入新的 ByteBuffer
                ByteBuffer target = ByteBuffer.allocate(length);
                // 从 source 读，向 target 写
                for (int j = 0; j < length; j++) {
                    target.put(source.get());
                }
                debugAll(target);
            }
        }
        source.compact(); // 0123456789abcdef  position 16 limit 16
    }

    public static void main(String[] args) throws IOException {
        ServerSocketChannel ssc = ServerSocketChannel.open();
        // 设置非阻塞
        ssc.configureBlocking(false);

        ssc.bind(new InetSocketAddress(8080));

        Selector selector = Selector.open();
        ssc.register(selector, SelectionKey.OP_ACCEPT);

        while (true) {
            selector.select();
            Set<SelectionKey> keys = selector.selectedKeys();

            Iterator<SelectionKey> it = keys.iterator();

            while (it.hasNext()) {
                SelectionKey selectionKey = it.next();

                if (selectionKey.isAcceptable()) {
                    SocketChannel sc = ssc.accept();
                    log.debug("connecting...{}", sc);
                    // 注册一个可读事件
                    sc.configureBlocking(false);
                    SelectionKey scKey = sc.register(selector, SelectionKey.OP_READ, ByteBuffer.allocate(16));


                    // 向客户端发送信息
                    StringBuilder sb = new StringBuilder();
                    for (int i = 0; i < 300000; i++) {
                        sb.append("abc");
                    }
                    ByteBuffer byteBuffer = Charset.defaultCharset().encode(sb.toString());
                    int write = sc.write(byteBuffer);
                    log.debug("写入客户端字节数量:{}", write);
                    if (byteBuffer.hasRemaining()){
                        sc.register(selector, scKey.interestOps() + SelectionKey.OP_WRITE, byteBuffer);
                    }

                } else if (selectionKey.isReadable()) {
                    try {
                        SocketChannel sc = (SocketChannel) selectionKey.channel();
                        ByteBuffer buffer = (ByteBuffer) selectionKey.attachment();
                        if (sc.read(buffer) == -1) {
                            selectionKey.cancel();
                        } else {
                            split(buffer);
                            // 需要扩容
                            if (buffer.position() == buffer.limit()) {
                                ByteBuffer newBuffer = ByteBuffer.allocate(buffer.capacity() * 2);
                                buffer.flip();
                                newBuffer.put(buffer); // 0123456789abcdef3333\n
                                selectionKey.attach(newBuffer);
                            }
                        }
                    } catch (IOException e) {
                        selectionKey.cancel();
                        e.printStackTrace();
                    }
                }else if(selectionKey.isWritable()){
                    SocketChannel sc = (SocketChannel) selectionKey.channel();
                    ByteBuffer byteBuffer = (ByteBuffer)selectionKey.attachment();
                    int write = sc.write(byteBuffer);
                    log.debug("写入客户端字节数量:{}", write);
                    if (!byteBuffer.hasRemaining()){
                        selectionKey.interestOps(selectionKey.interestOps() - SelectionKey.OP_WRITE);
                        selectionKey.attach(null);
                    }

                }

                // 必须移除
                it.remove();
            }
        }
    }
}
