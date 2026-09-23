package tech.allydoes;

import io.netty.bootstrap.ServerBootstrap;
import io.netty.channel.*;
import io.netty.channel.nio.NioEventLoopGroup;
import io.netty.channel.socket.SocketChannel;
import io.netty.channel.socket.nio.NioServerSocketChannel;
import io.netty.handler.codec.http.HttpObjectAggregator;
import io.netty.handler.codec.http.HttpRequestDecoder;
import io.netty.handler.codec.http.HttpResponseEncoder;
import io.netty.handler.logging.LogLevel;
import io.netty.handler.logging.LoggingHandler;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import tech.allydoes.discord.DiscordManager;
import tech.allydoes.web.HttpServerHandler;

public class Main {
    static Logger logger = LogManager.getLogger(Main.class);

    private static DiscordManager discordManager;

    public final int port;
    public Main(int port) {
        this.port = port;
    }

    public static void main(String[] args) throws InterruptedException {
        discordManager = new DiscordManager();

        new Main(6123).start();
    }

    public void start() {
        EventLoopGroup bossGroup = new NioEventLoopGroup(1);
        EventLoopGroup workerGroup = new NioEventLoopGroup();
        try {
            ServerBootstrap serverBootstrap = new ServerBootstrap();
            serverBootstrap.option(ChannelOption.SO_BACKLOG, 1024);
            serverBootstrap.group(bossGroup, workerGroup)
                    .channel(NioServerSocketChannel.class)
                    .handler(new LoggingHandler(LogLevel.DEBUG))
                    .childHandler(new ChannelInitializer<SocketChannel>() {
                        @Override
                        protected void initChannel(SocketChannel socketChannel) {
                            ChannelPipeline channelPipeline = socketChannel.pipeline();
                            channelPipeline.addLast(new HttpResponseEncoder());
                            channelPipeline.addLast(new HttpRequestDecoder());
                            channelPipeline.addLast(new HttpObjectAggregator(1048576));
                            channelPipeline.addLast(new HttpServerHandler());
                        }
                    });
            Channel ch = serverBootstrap.bind(this.port).sync().channel();
            ch.closeFuture().sync();
        } catch (InterruptedException e) {
            throw new RuntimeException(e);
        } finally {
            bossGroup.shutdownGracefully();
            workerGroup.shutdownGracefully();
        }
    }

    public static DiscordManager getDiscordManager() {
        return discordManager;
    }
}
