package com.japs.loadbalancing.impl;

import com.japs.core.common.ServiceAddress;
import com.japs.loadbalancing.LoadBalancer;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;

import java.util.List;
import java.util.Optional;
import java.util.concurrent.ThreadLocalRandom;

@Data
@Builder
@AllArgsConstructor
public class RandomLoadBalancer implements LoadBalancer<ServiceAddress> {

    private List<ServiceAddress> serviceAddresses;

    @Override
    public ServiceAddress next() {
        return Optional.ofNullable(serviceAddresses).map(value -> serviceAddresses.get(ThreadLocalRandom.current().nextInt(serviceAddresses.size()))).orElse(null);
    }
}
