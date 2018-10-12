package com.japs.client;

import com.japs.core.common.RpcResponse;

import java.util.concurrent.ConcurrentHashMap;

public class RpcResponseFutureManager {

    private static RpcResponseFutureManager rpcResponseFutureManager;

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

    private ConcurrentHashMap<String, RpcResponseFuture> rpcFutureMap = new ConcurrentHashMap<>();

    public void registerFuture(RpcResponseFuture rpcResponseFuture) {
        rpcFutureMap.put(rpcResponseFuture.getRequestId(), rpcResponseFuture);
    }

    public void futureDone(RpcResponse response) {
        // Mark the responseFuture as done
        rpcFutureMap.remove(response.getRequestId()).done(response);
    }
}
