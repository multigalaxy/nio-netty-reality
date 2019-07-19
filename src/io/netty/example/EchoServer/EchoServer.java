package io.netty.example.EchoServer;

import io.netty.bootstrap.ServerBootstrap;
import io.netty.channel.Channel;
import io.netty.channel.ChannelFuture;
import io.netty.channel.ChannelInitializer;
import io.netty.channel.EventLoopGroup;
import io.netty.channel.nio.NioEventLoopGroup;
import io.netty.channel.socket.nio.NioServerSocketChannel;
import io.netty.handler.codec.http.HttpObjectAggregator;
import io.netty.handler.codec.http.HttpServerCodec;
import io.netty.handler.stream.ChunkedWriteHandler;

public class EchoServer {

    private final String host;
    private final int port;

    public EchoServer(String host, int port) {
        this.host = host;
        this.port = port;
    }

    public void start() throws Exception {
        EventLoopGroup bossGroup = new NioEventLoopGroup();
        EventLoopGroup workerGroup = new NioEventLoopGroup();
        try {
            ServerBootstrap serverBootstrap = new ServerBootstrap();
            serverBootstrap.group(bossGroup, workerGroup)
                    .channel(NioServerSocketChannel.class)
                    .localAddress(host, port)
                    .childHandler(new ChannelInitializer<Channel>() {
                        // 每个连接过来时才触发此方法
                        @Override
                        protected void initChannel(Channel ch) throws Exception {
                            // 注册两个outbound
//                            ch.pipeline().addLast(new HttpResponseEncoder());
//                            ch.pipeline().addLast(new EchoOutHandler1());
//                            ch.pipeline().addLast(new EchoOutHandler2());
                            // 注册2个inbound
//                            ch.pipeline().addLast(new EchoInHandler1());  // inbound
//                            ch.pipeline().addLast(new EchoInHandler2());  // inbound

                            // 解码 握手 处理消息等
                            ch.pipeline().addLast(new HttpServerCodec());  // inbound
                            ch.pipeline().addLast(new HttpObjectAggregator(65536));  // inbound
                            ch.pipeline().addLast(new ChunkedWriteHandler());  // inbound
                            ch.pipeline().addLast(new WebsocketServerHandler());  // inbound
                        }
                    });

            // 则塞到绑定服务器操作完成
            ChannelFuture channelFuture = serverBootstrap.bind().sync();

            System.out.println("开始监听：【" + channelFuture.channel().localAddress() + "】");

            // 则塞到服务器关闭操作为止
            channelFuture.channel().closeFuture().sync();
        }catch (Exception e) {
            e.printStackTrace();
        } finally {
            bossGroup.shutdownGracefully();
            workerGroup.shutdownGracefully();
        }
    }

    public static void main(String[] args) throws Exception{
        new EchoServer("localhost", 20000).start();
    }
}
