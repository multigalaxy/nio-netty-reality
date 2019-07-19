package io.netty.example.EchoServer;

import java.util.Date;

import io.netty.buffer.ByteBuf;
import io.netty.buffer.Unpooled;
import io.netty.channel.*;
import io.netty.handler.codec.http.*;
import io.netty.handler.codec.http.websocketx.*;
import io.netty.util.CharsetUtil;

public class WebsocketServerHandler extends SimpleChannelInboundHandler {

    private WebSocketServerHandshaker handshaker;

    @Override
    public void channelReadComplete(ChannelHandlerContext ctx) throws Exception {
        ctx.flush();
    }

    @Override
    public void exceptionCaught(ChannelHandlerContext ctx, Throwable cause) throws Exception {
        cause.printStackTrace();
        ctx.close();
    }

    // 处理接收的消息
    @Override
    protected void channelRead0(ChannelHandlerContext ctx, Object msg) throws Exception {
        System.out.println("开始处理read0消息：【" + Thread.currentThread().getStackTrace()[1].getMethodName() + "】");
        // http 处理握手信息等
        if(msg instanceof FullHttpRequest) {
            handleHttpRequest(ctx, (FullHttpRequest)msg);
        }else if(msg instanceof WebSocketFrame) {
            // websocket 消息处理
            handleWebsocketFrame(ctx, (WebSocketFrame)msg);
        }
    }

    // 处理http请求
    private void handleHttpRequest(ChannelHandlerContext ctx, FullHttpRequest req) {
        System.out.println("开始处理http消息：【" + Thread.currentThread().getStackTrace()[1].getMethodName() + "】");
        // 解码失败或非websocket请求，返回400
        if(req.decoderResult().isFailure() || !req.headers().get("Upgrade").equals("websocket")) {
            sendHttpResponse(ctx, req, new DefaultFullHttpResponse(HttpVersion.HTTP_1_1, HttpResponseStatus.BAD_REQUEST));
            return;
        }
        // 构造websocket握手对象
        WebSocketServerHandshakerFactory handshakerFactory = new WebSocketServerHandshakerFactory("ws://localhost:8080/websocket", null, false);
        this.handshaker = handshakerFactory.newHandshaker(req);
        if(this.handshaker == null) {
            System.out.println("处理http消息 => 握手失败：【" + Thread.currentThread().getStackTrace()[1].getMethodName() + "】");
            WebSocketServerHandshakerFactory.sendUnsupportedVersionResponse(ctx.channel());
        }else {
            System.out.println("处理http消息 => 握手成功：【" + Thread.currentThread().getStackTrace()[1].getMethodName() + "】");
            this.handshaker.handshake(ctx.channel(), req);
        }
    }

    // 处理websocket请求
    private void handleWebsocketFrame(ChannelHandlerContext ctx, WebSocketFrame frame) {
        System.out.println("开始处理websocket消息：【" + Thread.currentThread().getStackTrace()[1].getMethodName() + "】");

        // 关闭链接
        if(frame instanceof CloseWebSocketFrame) {
            System.out.println("处理websocket消息 => 关闭链接：【" + Thread.currentThread().getStackTrace()[1].getMethodName() + "】");
            this.handshaker.close(ctx.channel(), ((CloseWebSocketFrame) frame).retain());
            return;
        }

        // ping指令
        if(frame instanceof PingWebSocketFrame) {
            System.out.println("处理websocket消息 => 回复ping消息：【" + Thread.currentThread().getStackTrace()[1].getMethodName() + "】");
            ctx.write(new PongWebSocketFrame(frame.content().retain()));
            return;
        }

        // 非文本消息
        if(!(frame instanceof TextWebSocketFrame)) {
            System.out.println("处理websocket消息 => 非法消息：【" + Thread.currentThread().getStackTrace()[1].getMethodName() + "】");
            throw new UnsupportedOperationException(String.format("%s frame type is not supported.", frame.getClass().getName()));
        }

        // 业务逻辑
        handleData();

        // 应答消息
        String msg = ((TextWebSocketFrame) frame).text();
        String data = msg + " >>> server_date:" + new Date().toString();
        ctx.channel().write(new TextWebSocketFrame(data));
        System.out.println("处理websocket消息 => 应答消息：【" + Thread.currentThread().getStackTrace()[1].getMethodName() + "】data: 【" + data + "】" );
    }

    // 发送http响应内容
    private void sendHttpResponse(ChannelHandlerContext ctx, FullHttpRequest req, FullHttpResponse resp) {
        // 发送消息
        if(resp.status().code() != 200) {
            ByteBuf buf = Unpooled.copiedBuffer(resp.toString(), CharsetUtil.UTF_8);
            resp.content().writeBytes(buf);
            buf.release();
            HttpUtil.setContentLength(resp, resp.content().readableBytes());
        }
        // 非keep-alive，直接关链接
        ChannelFuture future = ctx.channel().writeAndFlush(resp);
        if(!HttpUtil.isKeepAlive(resp) || resp.status().code() != 200) {
            future.addListener(ChannelFutureListener.CLOSE);
        }
    }

    // 业务逻辑
    private void handleData() {
        System.out.println("处理websocket消息 => 处理业务逻辑：【" + Thread.currentThread().getStackTrace()[1].getMethodName() + "】");
    }

}
