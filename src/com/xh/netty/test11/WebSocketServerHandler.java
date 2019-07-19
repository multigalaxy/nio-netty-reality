package com.xh.netty.test11;

import io.netty.buffer.ByteBuf;
import io.netty.buffer.Unpooled;
import io.netty.channel.ChannelFuture;
import io.netty.channel.ChannelFutureListener;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.SimpleChannelInboundHandler;
import io.netty.handler.codec.http.*;
import io.netty.handler.codec.http.websocketx.*;
import io.netty.util.CharsetUtil;


import java.util.Date;

public class WebSocketServerHandler extends SimpleChannelInboundHandler {
    private WebSocketServerHandshaker handshaker;

    @Override
    public void channelRead0(ChannelHandlerContext ctx, Object msg) throws Exception {
        System.out.println(msg.toString());
        //http
        if (msg instanceof FullHttpRequest) {
            handleHttpRequest(ctx, (FullHttpRequest) msg);
        } else if (msg instanceof WebSocketFrame) {//websocket
            handleWebsocketFrame(ctx, (WebSocketFrame) msg);
        }
    }

    @Override
    public void channelReadComplete(ChannelHandlerContext ctx) throws Exception {
        ctx.flush();
    }

    @Override
    public void exceptionCaught(ChannelHandlerContext ctx, Throwable cause) throws Exception {
        cause.printStackTrace();
        ctx.close();
    }

    private void handleWebsocketFrame(ChannelHandlerContext ctx, WebSocketFrame msg) {
        //关闭链路指令
        if (msg instanceof CloseWebSocketFrame) {
            handshaker.close(ctx.channel(), (CloseWebSocketFrame) msg.retain());
            return;
        }

        //PING 消息
        if (msg instanceof PingWebSocketFrame) {
            ctx.write(new PongWebSocketFrame(msg.content().retain()));
            return;
        }

        //非文本
        if (!(msg instanceof TextWebSocketFrame)) {
            throw new UnsupportedOperationException(String.format("%s frame type not support", msg.getClass().getName()));

        }

        //应答消息
        String requset = ((TextWebSocketFrame) msg).text();
        ctx.channel().write(new TextWebSocketFrame(requset + " >>>>Now is " + new Date().toString()));
    }

    private void handleHttpRequest(ChannelHandlerContext ctx, FullHttpRequest msg) {

        //HTTP 请异常
        if (!msg.decoderResult().isSuccess() || !"websocket".equals(msg.headers().get("Upgrade"))) {
            System.out.println(msg.decoderResult().isSuccess());
            System.out.println(msg.headers().get("Upgrade"));
            sendHttpResponse(ctx, msg, new DefaultFullHttpResponse(HttpVersion.HTTP_1_1, HttpResponseStatus.BAD_REQUEST));
            return;
        }

        //握手
        WebSocketServerHandshakerFactory wsFactory = new WebSocketServerHandshakerFactory("ws://localhost:8080/websocket", null, false);
        handshaker = wsFactory.newHandshaker(msg);
        if (handshaker == null) {
            WebSocketServerHandshakerFactory.sendUnsupportedVersionResponse(ctx.channel());

        } else {
            handshaker.handshake(ctx.channel(), msg);
        }
    }

    private void sendHttpResponse(ChannelHandlerContext ctx, FullHttpRequest msg, FullHttpResponse resp) {

        //响应
        if (resp.status().code() != 200) {
            ByteBuf buf = Unpooled.copiedBuffer(resp.status().toString(), CharsetUtil.UTF_8);
            resp.content().writeBytes(buf);
            buf.release();
            HttpUtil.setContentLength(resp, resp.content().readableBytes());
        }

        //非Keep-Alive,关闭链接
        ChannelFuture future = ctx.channel().writeAndFlush(resp);
        if (!HttpUtil.isKeepAlive(resp) || resp.status().code() != 200) {
            future.addListener(ChannelFutureListener.CLOSE);
        }


    }


}