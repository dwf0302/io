package cn.wf.netty;

import io.netty.bootstrap.Bootstrap;
import io.netty.buffer.ByteBuf;
import io.netty.channel.ChannelFuture;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.ChannelInboundHandlerAdapter;
import io.netty.channel.ChannelInitializer;
import io.netty.channel.nio.NioEventLoopGroup;
import io.netty.channel.socket.SocketChannel;
import io.netty.channel.socket.nio.NioSocketChannel;
import io.netty.handler.codec.LengthFieldBasedFrameDecoder;
import io.netty.handler.logging.LoggingHandler;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Random;

public class HelloClient {
    static final Logger log = LoggerFactory.getLogger(HelloClient.class);

    public static void main(String[] args) {
        NioEventLoopGroup clientGroup = new NioEventLoopGroup();

        try {
            ChannelFuture channelFuture = new Bootstrap()
                    .group(clientGroup)
                    .channel(NioSocketChannel.class)
                    .handler(new ChannelInitializer<SocketChannel>() {
                        @Override
                        protected void initChannel(SocketChannel socketChannel) {
                            socketChannel.pipeline().addLast(new LoggingHandler());
                            socketChannel.pipeline().addLast(new ChannelInboundHandlerAdapter() {
                                @Override
                                public void channelActive(ChannelHandlerContext ctx) throws Exception {
                                    log.debug("sending...");
                                    Random r = new Random();
                                    char c = 'a';
                                    ByteBuf buffer = ctx.alloc().buffer();
                                    for (int i = 0; i < 10; i++) {
                                        byte length = (byte) (r.nextInt(16) + 1);
                                        // 先写入长度
                                        buffer.writeByte(length);
                                        // 再
                                        for (int j = 1; j <= length; j++) {
                                            buffer.writeByte((byte) c);
                                        }
                                        c++;
                                    }
                                    ctx.writeAndFlush(buffer);
                                }
                            });
                        }
                    })
                    .connect("127.0.0.1", 8080);
            channelFuture.sync();
            channelFuture.channel().closeFuture().sync();
        } catch (InterruptedException e) {
            e.printStackTrace();
        } finally {
            clientGroup.shutdownGracefully();
        }
    }
}
