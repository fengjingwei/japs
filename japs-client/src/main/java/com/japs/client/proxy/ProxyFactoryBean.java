package com.japs.client.proxy;

import com.japs.client.ChannelManager;
import com.japs.client.RpcResponseFuture;
import com.japs.client.RpcResponseFutureManager;
import com.japs.core.common.RpcRequest;
import com.japs.core.common.RpcResponse;
import com.japs.core.utils.StringUtilsX;
import com.japs.registry.ServiceDiscovery;
import io.netty.channel.Channel;
import io.netty.channel.ChannelFutureListener;
import lombok.Data;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.FactoryBean;

import java.lang.reflect.Method;
import java.lang.reflect.Proxy;
import java.net.InetSocketAddress;
import java.util.UUID;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;

@Slf4j
@Data
public class ProxyFactoryBean implements FactoryBean<Object> {

    private Class<?> type;

    private ServiceDiscovery serviceDiscovery;

    @Override
    public Object getObject() {
        return Proxy.newProxyInstance(type.getClassLoader(), new Class<?>[]{type}, this::doInvoke);
    }

    @Override
    public Class<?> getObjectType() {
        return this.type;
    }

    @Override
    public boolean isSingleton() {
        return true;
    }

    private Object doInvoke(Object proxy, Method method, Object[] args) throws Throwable {
        String targetServiceName = type.getName();
        // Create request
        RpcRequest request = RpcRequest.builder()
                .requestId(generateRequestId(targetServiceName))
                .interfaceName(method.getDeclaringClass().getName())
                .methodName(method.getName())
                .parameters(args)
                .parameterTypes(method.getParameterTypes()).build();
        // Get service address
        InetSocketAddress serviceAddress = getServiceAddress(targetServiceName);
        // Get channel by service address
        Channel channel = ChannelManager.getInstance().getChannel(serviceAddress);
        if (channel == null) {
            throw new RuntimeException("Can't get channel for address " + serviceAddress);
        }
        // Send request
        RpcResponse response = sendRequest(channel, request);
        if (response == null) {
            throw new RuntimeException("Response is null");
        }
        if (response.hasException()) {
            throw response.getException();
        } else {
            return response.getResult();
        }
    }

    private String generateRequestId(String targetServiceName) {
        return String.format("%s-%s", targetServiceName, UUID.randomUUID().toString());
    }

    private InetSocketAddress getServiceAddress(String targetServiceName) {
        String serviceAddress = StringUtilsX.EMPTY;
        if (serviceDiscovery != null) {
            serviceAddress = serviceDiscovery.discover(targetServiceName);
            log.debug("Get address: {} for service: {}", serviceAddress, targetServiceName);
        }
        if (StringUtilsX.isEmpty(serviceAddress)) {
            throw new RuntimeException(String.format("Address of target service %s is empty", targetServiceName));
        }
        String[] array = StringUtilsX.split(serviceAddress, ":");
        return new InetSocketAddress(array[0], Integer.parseInt(array[1]));
    }

    private RpcResponse sendRequest(Channel channel, RpcRequest request) {
        CountDownLatch latch = new CountDownLatch(1);
        RpcResponseFuture rpcResponseFuture = new RpcResponseFuture(request.getRequestId());
        RpcResponseFutureManager.getInstance().registerFuture(rpcResponseFuture);
        channel.writeAndFlush(request).addListener((ChannelFutureListener) future -> latch.countDown());
        try {
            latch.await();
        } catch (InterruptedException e) {
            log.error(e.getMessage());
        }
        try {
            return rpcResponseFuture.get(1, TimeUnit.SECONDS);
        } catch (Exception e) {
            log.warn("Exception:", e);
            return null;
        }
    }
}
