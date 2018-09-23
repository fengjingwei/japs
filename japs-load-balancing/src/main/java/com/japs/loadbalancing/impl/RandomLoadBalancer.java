package com.japs.loadbalancing.impl;

import com.japs.core.common.ServiceAddress;
import com.japs.loadbalancing.LoadBalancer;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import org.springframework.util.CollectionUtils;

import java.util.List;
import java.util.concurrent.ThreadLocalRandom;

@Data
@Builder
@AllArgsConstructor
public class RandomLoadBalancer implements LoadBalancer<ServiceAddress> {

    List<ServiceAddress> serviceAddresses;

    @Override
    public ServiceAddress next() {
        if (CollectionUtils.isEmpty(serviceAddresses)) {
            return null;
        }
        return serviceAddresses.get(ThreadLocalRandom.current().nextInt(serviceAddresses.size()));
    }
}
