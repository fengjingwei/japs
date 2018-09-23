package com.japs.registry;

import com.japs.core.common.ServiceAddress;

public interface ServiceRegistry {

    /**
     * register service
     *
     * @param serviceName
     * @param serviceAddress
     */
    void register(String serviceName, ServiceAddress serviceAddress);
}