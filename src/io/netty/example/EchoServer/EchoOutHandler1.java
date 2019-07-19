package io.netty.example.EchoServer;

import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.ChannelOutboundHandlerAdapter;
import io.netty.channel.ChannelPromise;

public class EchoOutHandler1 extends ChannelOutboundHandlerAdapter {

    @Override
    // 向客户端发送消息
    public void write(ChannelHandlerContext ctx, Object msg, ChannelPromise promise) throws Exception {
        System.out.println("正在输出处理：【" + EchoOutHandler1.class.getName() + "】");
        ctx.write(msg);
        ctx.flush();
    }
}
