package com.japs.client;

import com.google.common.collect.Maps;
import com.japs.core.common.RpcResponse;

import java.util.Map;

public class RpcResponseFutureManager {

    private static RpcResponseFutureManager rpcResponseFutureManager;
    private Map<String, RpcResponseFuture> rpcFutureMap = Maps.newConcurrentMap();

    private RpcResponseFutureManager() {
    }

    public static RpcResponseFutureManager getInstance() {
        if (rpcResponseFutureManager == null) {
            synchronized (RpcResponseFutureManager.class) {
                if (rpcResponseFutureManager == null) {
                    rpcResponseFutureManager = new RpcResponseFutureManager();
                }
            }
        }
        return rpcResponseFutureManager;
    }

    public void registerFuture(RpcResponseFuture rpcResponseFuture) {
        rpcFutureMap.put(rpcResponseFuture.getRequestId(), rpcResponseFuture);
    }

    public void futureDone(RpcResponse response) {
        // Mark the responseFuture as done
        rpcFutureMap.remove(response.getRequestId()).done(response);
    }
}
