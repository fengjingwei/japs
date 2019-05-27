package com.japs.server;

import com.google.common.collect.Maps;
import com.japs.annotation.RpcService;
import com.japs.core.codec.coder.RpcDecoder;
import com.japs.core.codec.coder.RpcEncoder;
import com.japs.core.codec.serialization.impl.ProtobufSerializer;
import com.japs.core.common.RpcRequest;
import com.japs.core.common.RpcResponse;
import com.japs.core.common.ServiceAddress;
import com.japs.registry.ServiceRegistry;
import com.japs.server.handler.RpcServerHandler;
import io.netty.bootstrap.ServerBootstrap;
import io.netty.channel.*;
import io.netty.channel.nio.NioEventLoopGroup;
import io.netty.channel.socket.SocketChannel;
import io.netty.channel.socket.nio.NioServerSocketChannel;
import lombok.NonNull;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.aop.support.AopUtils;
import org.springframework.beans.BeansException;
import org.springframework.beans.factory.InitializingBean;
import org.springframework.context.ApplicationContext;
import org.springframework.context.ApplicationContextAware;
import org.springframework.util.CollectionUtils;

import java.lang.annotation.Annotation;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.stream.Collectors;

@Slf4j
@RequiredArgsConstructor
public class RpcServer implements ApplicationContextAware, InitializingBean {

    @NonNull
    private String serverIp;

    @NonNull
    private int serverPort;

    @NonNull
    private ServiceRegistry serviceRegistry;

    private Map<String, Object> handlerMap = Maps.newConcurrentMap();

    @Override
    public void setApplicationContext(ApplicationContext ctx) throws BeansException {
        log.info("Putting handler");
        // Register handler
        getServiceInterfaces(ctx).forEach(interfaceClazz -> {
            String serviceName = interfaceClazz.getAnnotation(RpcService.class).value().getName();
            Object serviceBean = ctx.getBean(interfaceClazz);
            handlerMap.put(serviceName, serviceBean);
            log.debug("Put handler: {}, {}", serviceName, serviceBean);
        });
    }

    @Override
    public void afterPropertiesSet() {
        startServer();
    }

    private void startServer() {
        // Get ip and port
        log.debug("Starting server on port: {}", serverPort);
        EventLoopGroup bossGroup = new NioEventLoopGroup();
        EventLoopGroup workerGroup = new NioEventLoopGroup();
        try {
            ServerBootstrap bootstrap = new ServerBootstrap();
            bootstrap.group(bossGroup, workerGroup)
                    .channel(NioServerSocketChannel.class)
                    .childHandler(new ChannelInitializer<SocketChannel>() {
                        @Override
                        public void initChannel(SocketChannel channel) {
                            ChannelPipeline pipeline = channel.pipeline();
                            pipeline.addLast(new RpcDecoder(RpcRequest.class, new ProtobufSerializer()))
                                    .addLast(new RpcEncoder(RpcResponse.class, new ProtobufSerializer()))
                                    .addLast(new RpcServerHandler(handlerMap));
                        }
                    }).option(ChannelOption.SO_BACKLOG, 1024).childOption(ChannelOption.SO_KEEPALIVE, true);
            ChannelFuture future = bootstrap.bind(serverIp, serverPort).sync();
            registerServices();
            log.info("Server started");
            future.channel().closeFuture().sync();
        } catch (InterruptedException e) {
            throw new RuntimeException("Server shutdown", e);
        } finally {
            workerGroup.shutdownGracefully();
            bossGroup.shutdownGracefully();
        }
    }

    private void registerServices() {
        if (serviceRegistry != null) {
            if (CollectionUtils.isEmpty(handlerMap.values())) {
                log.info("No discovery service");
            } else {
                handlerMap.keySet().forEach(interfaceName -> {
                    serviceRegistry.register(interfaceName, new ServiceAddress(serverIp, serverPort));
                    log.info("Registering service: {} with address: {}:{}", interfaceName, serverIp, serverPort);
                });
            }
        }
    }

    private List<Class<?>> getServiceInterfaces(ApplicationContext ctx) {
        Class<? extends Annotation> clazz = RpcService.class;
        return ctx.getBeansWithAnnotation(clazz)
                .values().stream()
                .map(AopUtils::getTargetClass)
                .map(cls -> Arrays.asList(cls.getInterfaces()))
                .flatMap(List::stream)
                .filter(cls -> Objects.nonNull(cls.getAnnotation(clazz)))
                .collect(Collectors.toList());
    }
}
