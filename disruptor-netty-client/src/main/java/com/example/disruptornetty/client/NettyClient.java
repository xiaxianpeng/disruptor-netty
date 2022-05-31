package com.example.disruptornetty.client;

import com.example.disruptornetty.common.entity.TranslatorData;
import com.example.disruptornetty.common.entity.codec.MarshallingCodecFactory;
import io.netty.bootstrap.Bootstrap;
import io.netty.buffer.PooledByteBufAllocator;
import io.netty.channel.AdaptiveRecvByteBufAllocator;
import io.netty.channel.Channel;
import io.netty.channel.ChannelFuture;
import io.netty.channel.ChannelInitializer;
import io.netty.channel.ChannelOption;
import io.netty.channel.EventLoopGroup;
import io.netty.channel.nio.NioEventLoopGroup;
import io.netty.channel.socket.SocketChannel;
import io.netty.channel.socket.nio.NioSocketChannel;
import lombok.extern.slf4j.Slf4j;

@Slf4j
public class NettyClient {
    public static final String HOST = "127.0.0.1";
    public static final int PORT = 8765;
    // 可优化，池化 Map<String,Channel> channelMap= new ConcurrentHashMap<>();
    private Channel channel;


    public NettyClient() {
        this.connect(HOST, PORT);
    }

    private void connect(String host, int port) {
        EventLoopGroup workGroup = new NioEventLoopGroup();
        Bootstrap bootstrap = new Bootstrap();
        try {
            bootstrap.group(workGroup)
                    .channel(NioSocketChannel.class)
                    // 缓存区动态调配（自适应）
                    .option(ChannelOption.RCVBUF_ALLOCATOR, AdaptiveRecvByteBufAllocator.DEFAULT)
                    // 缓存区 池化操作
                    .option(ChannelOption.ALLOCATOR, PooledByteBufAllocator.DEFAULT)
                    .handler(new ChannelInitializer<SocketChannel>() {
                        @Override
                        protected void initChannel(SocketChannel sc) throws Exception {
                            sc.pipeline().addLast(MarshallingCodecFactory.buildMarshallingDecoder());
                            sc.pipeline().addLast(MarshallingCodecFactory.buildMarshallingEncoder());
                            sc.pipeline().addLast(new ClientHandler());
                        }
                    });

            ChannelFuture channelFuture = bootstrap.connect(host, port);
            log.info("Client connected...");

            this.channel = channelFuture.channel();
            channelFuture.channel().closeFuture().sync();

        } catch (Exception e) {
            log.error("connect error:", e);
        } finally {
            workGroup.shutdownGracefully();
            log.info("Client ShutDown...");
        }
    }

    public void sendData() {
        for (int i = 0; i < 10; i++) {
            TranslatorData data = new TranslatorData()
                    .setId("" + i)
                    .setName("req: " + i)
                    .setMessage("req : " + i);

            this.channel.writeAndFlush(data);
        }
    }
}
