package com.japs.core.codec.serialization;

public interface Serializer {

    /**
     * serialize
     *
     * @param obj
     * @param <T>
     * @return
     */
    <T> byte[] serialize(T obj);

    /**
     * deSerialize
     *
     * @param data
     * @param cls
     * @param <T>
     * @return
     */
    <T> T deSerialize(byte[] data, Class<T> cls);
}
