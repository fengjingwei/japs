package com.japs.loadbalancing.impl.consistenthash;

@FunctionalInterface
public interface HashFunction<T> {

    /**
     * Get hash value
     *
     * @param t
     * @return
     */
    int hash(T t);
}
