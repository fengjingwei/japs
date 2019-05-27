package com.japs.server.handler;

import com.japs.core.common.RpcRequest;
import com.japs.core.common.RpcResponse;
import io.netty.channel.ChannelFutureListener;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.SimpleChannelInboundHandler;
import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;

import java.lang.reflect.Method;
import java.util.Map;

@Slf4j
@AllArgsConstructor
public class RpcServerHandler extends SimpleChannelInboundHandler<RpcRequest> {

    private Map<String, Object> handlerMap;

    @Override
    protected void channelRead0(ChannelHandlerContext ctx, RpcRequest request) {
        log.debug("Get request: {}", request);
        RpcResponse response = new RpcResponse();
        response.setRequestId(request.getRequestId());
        try {
            Object result = handleRequest(request);
            response.setResult(result);
        } catch (Exception e) {
            log.warn("Get exception when handing request, exception: {}", e);
            response.setException(e);
        }
        ctx.writeAndFlush(response).addListener((ChannelFutureListener) channelFuture -> log.debug("Sent response for request: {}", request.getRequestId()));
    }

    private Object handleRequest(RpcRequest request) throws Exception {
        // Get service bean
        String serviceName = request.getInterfaceName();
        Object serviceBean = handlerMap.get(serviceName);
        if (serviceBean == null) {
            throw new RuntimeException(String.format("No service bean available: %s", serviceName));
        }
        // Invoke by reflect
        Class<?> serviceClass = serviceBean.getClass();
        String methodName = request.getMethodName();
        Class<?>[] parameterTypes = request.getParameterTypes();
        Object[] parameters = request.getParameters();
        Method method = serviceClass.getMethod(methodName, parameterTypes);
        method.setAccessible(true);
        return method.invoke(serviceBean, parameters);
    }

    @Override
    public void exceptionCaught(ChannelHandlerContext ctx, Throwable cause) {
        log.error("Server caught exception", cause);
        ctx.close();
    }
}
