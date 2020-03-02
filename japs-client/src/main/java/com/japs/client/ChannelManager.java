package com.japs.client;

import com.google.common.collect.Maps;
import com.japs.core.codec.coder.RpcDecoder;
import com.japs.core.codec.coder.RpcEncoder;
import com.japs.core.codec.serialization.impl.ProtobufSerializer;
import com.japs.core.common.RpcRequest;
import com.japs.core.common.RpcResponse;
import io.netty.bootstrap.Bootstrap;
import io.netty.channel.*;
import io.netty.channel.nio.NioEventLoopGroup;
import io.netty.channel.socket.SocketChannel;
import io.netty.channel.socket.nio.NioSocketChannel;
import lombok.extern.slf4j.Slf4j;

import java.net.InetSocketAddress;
import java.util.Map;

@Slf4j
public class ChannelManager {

    private static volatile ChannelManager channelManager;

    private Map<InetSocketAddress, Channel> channels = Maps.newConcurrentMap();

    private ChannelManager() {
    }

    public static ChannelManager getInstance() {
        if (channelManager == null) {
            synchronized (ChannelManager.class) {
                if (channelManager == null) {
                    channelManager = new ChannelManager();
                }
            }
        }
        return channelManager;
    }

    public Channel getChannel(InetSocketAddress inetSocketAddress) {
        Channel channel = channels.get(inetSocketAddress);
        if (channel == null) {
            EventLoopGroup group = new NioEventLoopGroup();
            try {
                Bootstrap bootstrap = new Bootstrap();
                bootstrap
                        .group(group)
                        .channel(NioSocketChannel.class)
                        .handler(new RpcChannelInitializer())
                        .option(ChannelOption.SO_KEEPALIVE, true);

                channel = bootstrap.connect(inetSocketAddress.getHostName(), inetSocketAddress.getPort()).sync().channel();
                registerChannel(inetSocketAddress, channel);
                // Remove the channel for map when it's closed
                channel.closeFuture().addListener((ChannelFutureListener) future -> removeChannel(inetSocketAddress));
            } catch (Exception e) {
                log.warn("Fail to get channel for address: {}", inetSocketAddress);
            }
        }
        return channel;
    }

    private void registerChannel(InetSocketAddress inetSocketAddress, Channel channel) {
        channels.put(inetSocketAddress, channel);
    }

    private void removeChannel(InetSocketAddress inetSocketAddress) {
        channels.remove(inetSocketAddress);
    }

    private class RpcChannelInitializer extends ChannelInitializer<SocketChannel> {

        @Override
        protected void initChannel(SocketChannel ch) {
            ChannelPipeline pipeline = ch.pipeline();
            pipeline.addLast(new RpcEncoder(RpcRequest.class, new ProtobufSerializer()))
                    .addLast(new RpcDecoder(RpcResponse.class, new ProtobufSerializer()))
                    .addLast(new RpcResponseHandler());
        }
    }

    private class RpcResponseHandler extends SimpleChannelInboundHandler<RpcResponse> {

        @Override
        protected void channelRead0(ChannelHandlerContext channelHandlerContext, RpcResponse rpcResponse) {
            log.debug("Get response: {}", rpcResponse);
            RpcResponseFutureManager.getInstance().futureDone(rpcResponse);
        }

        @Override
        public void exceptionCaught(ChannelHandlerContext ctx, Throwable cause) {
            log.warn("RPC request exception: {}", cause);
        }
    }
}
