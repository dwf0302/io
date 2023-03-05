package cn.wf.nio;

import lombok.extern.slf4j.Slf4j;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.nio.ByteBuffer;
import java.nio.channels.*;
import java.util.Iterator;
import java.util.Set;
import java.util.concurrent.ConcurrentLinkedDeque;

import static cn.wf.utils.ByteBufferUtil.debugAll;
import static cn.wf.utils.ByteBufferUtil.debugRead;

@Slf4j
public class NioMultiThreadServer {

    public static void main(String[] args) throws IOException {
        ServerSocketChannel ssc = ServerSocketChannel.open();
        ssc.bind(new InetSocketAddress(8080));
        ssc.configureBlocking(false);
        Thread.currentThread().setName("Boss");

        Selector boss = Selector.open();
        ssc.register(boss, SelectionKey.OP_ACCEPT);

        Worker worker = new Worker("Thread-0");

        while (true) {
            boss.select();
            Set<SelectionKey> keys = boss.selectedKeys();
            Iterator<SelectionKey> it = keys.iterator();
            while (it.hasNext()) {
                SelectionKey key = it.next();
                if (key.isAcceptable()) {
                    ServerSocketChannel serverSocketChannel = (ServerSocketChannel) key.channel();
                    SocketChannel sc = serverSocketChannel.accept();
                    sc.configureBlocking(false);
                    worker.register(sc);
                }
                it.remove();
            }
        }


    }


    static class Worker implements Runnable {

        private String name;

        private Thread thread;

        private Selector selector;

        private volatile boolean isCreate;

        private ConcurrentLinkedDeque<Runnable> queue = new ConcurrentLinkedDeque<>();

        public Worker(String name) {
            this.name = name;
        }

        public void register(SocketChannel sc) throws IOException {
            if (!isCreate) {
                selector = Selector.open();
                thread = new Thread(this, name);
                thread.start();
                isCreate = Boolean.TRUE;
            }

            queue.add(() -> {
                try {
                    sc.register(selector, SelectionKey.OP_READ, null);
                } catch (ClosedChannelException e) {
                    e.printStackTrace();
                }
            });

            selector.wakeup();
        }

        @Override
        public void run() {
            while (true) {
                try {
                    selector.select();
                    Runnable task = queue.poll();
                    if (task != null) {
                        task.run();
                    }
                    Set<SelectionKey> keys = selector.selectedKeys();
                    Iterator<SelectionKey> it = keys.iterator();
                    while (it.hasNext()) {
                        SelectionKey selectionKey = it.next();
                        if (selectionKey.isReadable()) {
                            SocketChannel sc = (SocketChannel) selectionKey.channel();
                            log.debug(">>>>>>客户端<<<<<<：{}", sc);
                            try {
                                ByteBuffer buffer = ByteBuffer.allocate(16);
                                if (sc.read(buffer) == -1) {
                                    selectionKey.cancel();
                                } else {
                                    buffer.flip();
                                    debugRead(buffer);
                                }
                            } catch (IOException e) {
                                selectionKey.cancel();
                                e.printStackTrace();
                            }
                        }
                        // 必须移除
                        it.remove();
                    }

                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
        }
    }

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
}
