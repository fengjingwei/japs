package com.japs.loadbalancing;

public interface LoadBalancer<T> {

    /**
     * Get next server address
     *
     * @return
     */
    T next();
}
