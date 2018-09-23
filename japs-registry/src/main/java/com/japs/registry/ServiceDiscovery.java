package com.japs.registry;

public interface ServiceDiscovery {

    /**
     * discover service
     *
     * @param serviceName
     * @return
     */
    String discover(String serviceName);
}
