package io.netty.example.EchoServer;

import io.netty.buffer.ByteBuf;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.ChannelOutboundHandlerAdapter;
import io.netty.channel.ChannelPromise;

public class EchoOutHandler2 extends ChannelOutboundHandlerAdapter {

    @Override
    public void write(ChannelHandlerContext ctx, Object msg, ChannelPromise promise) throws Exception {
        System.out.println("正在输出处理：【" + EchoOutHandler2.class.getName() + "】");
        ByteBuf buf = (ByteBuf)msg;
        byte[] bytes = new byte[buf.readableBytes()];
        buf.getBytes(buf.readerIndex(), bytes);
        System.out.println("接收inhandler的数据【" + (new String(bytes, 0, buf.readableBytes())) + "】");
        // 执行下一个outhandler
        super.write(ctx, msg, promise);
    }
}
