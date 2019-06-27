package com.japs.client;

import com.japs.core.common.RpcResponse;
import lombok.Data;
import lombok.NonNull;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

import java.util.concurrent.CountDownLatch;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

@Slf4j
@Data
@RequiredArgsConstructor
public class RpcResponseFuture implements Future<Object> {

    private final CountDownLatch LATCH = new CountDownLatch(1);
    @NonNull
    private String requestId;
    private RpcResponse response;

    public void done(RpcResponse response) {
        this.response = response;
        LATCH.countDown();
    }

    @Override
    public RpcResponse get() {
        try {
            LATCH.await();
        } catch (InterruptedException e) {
            log.error(e.getMessage());
        }
        return response;
    }

    @Override
    public RpcResponse get(long timeout, TimeUnit unit) throws TimeoutException {
        try {
            if (!LATCH.await(timeout, unit)) {
                throw new TimeoutException("RPC Request timeout");
            }
        } catch (InterruptedException e) {
            log.error(e.getMessage());
        }
        return response;
    }

    @Override
    public boolean isDone() {
        return LATCH.getCount() == 0;
    }

    @Override
    public boolean cancel(boolean mayInterruptIfRunning) {
        throw new UnsupportedOperationException();
    }

    @Override
    public boolean isCancelled() {
        throw new UnsupportedOperationException();
    }
}
