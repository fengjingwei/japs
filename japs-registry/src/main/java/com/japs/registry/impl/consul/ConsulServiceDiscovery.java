package com.japs.registry.impl.consul;

import com.ecwid.consul.v1.ConsulClient;
import com.ecwid.consul.v1.ConsulRawClient;
import com.ecwid.consul.v1.QueryParams;
import com.ecwid.consul.v1.Response;
import com.ecwid.consul.v1.health.model.HealthService;
import com.google.common.collect.Maps;
import com.google.common.util.concurrent.ThreadFactoryBuilder;
import com.japs.core.common.ServiceAddress;
import com.japs.loadbalancing.LoadBalancer;
import com.japs.loadbalancing.impl.RandomLoadBalancer;
import com.japs.registry.ServiceDiscovery;
import lombok.extern.slf4j.Slf4j;

import java.util.List;
import java.util.Map;
import java.util.concurrent.*;
import java.util.stream.Collectors;

@Slf4j
public class ConsulServiceDiscovery implements ServiceDiscovery {

    private static final int MAX_THREAD = Runtime.getRuntime().availableProcessors() << 1;
    private ConsulClient consulClient;
    private Map<String, LoadBalancer<ServiceAddress>> loadBalancerMap = Maps.newConcurrentMap();

    public ConsulServiceDiscovery(String consulAddress) {
        log.debug("Use consul to do service discovery: {}", consulAddress);
        String[] address = consulAddress.split(":");
        ConsulRawClient rawClient = new ConsulRawClient(address[0], Integer.valueOf(address[1]));
        consulClient = new ConsulClient(rawClient);
    }

    @Override
    public String discover(String serviceName) {
        List<HealthService> healthServices;
        if (!loadBalancerMap.containsKey(serviceName)) {
            healthServices = consulClient.getHealthServices(serviceName, true, QueryParams.DEFAULT).getValue();
            loadBalancerMap.put(serviceName, buildLoadBalancer(healthServices));
            // Watch consul
            longPolling(serviceName);
        }
        ServiceAddress address = loadBalancerMap.get(serviceName).next();
        if (address == null) {
            throw new RuntimeException(String.format("No service instance for %s", serviceName));
        }
        return address.toString();
    }

    private void longPolling(String serviceName) {
        ThreadFactory namedThreadFactory = new ThreadFactoryBuilder().setNameFormat("japs-rpc-pool-%d").build();
        ExecutorService pool = new ThreadPoolExecutor(MAX_THREAD, MAX_THREAD, 0L, TimeUnit.MILLISECONDS, new LinkedBlockingQueue<>(1024), namedThreadFactory, new ThreadPoolExecutor.AbortPolicy());
        pool.execute(() -> {
            long consulIndex = -1;
            do {
                QueryParams param = QueryParams.Builder.builder().setIndex(consulIndex).build();
                Response<List<HealthService>> healthyServices = consulClient.getHealthServices(serviceName, true, param);
                consulIndex = healthyServices.getConsulIndex();
                log.debug("Consul index for {} is: {}", serviceName, consulIndex);

                List<HealthService> healthServices = healthyServices.getValue();
                log.debug("Service addresses of {} is: {}", serviceName, healthServices);

                loadBalancerMap.put(serviceName, buildLoadBalancer(healthServices));
            } while (true);
        });
        pool.shutdown();
    }

    private LoadBalancer buildLoadBalancer(List<HealthService> healthServices) {
        return new RandomLoadBalancer(healthServices.stream().map(healthService -> {
            HealthService.Service service = healthService.getService();
            return new ServiceAddress(service.getAddress(), service.getPort());
        }).collect(Collectors.toList()));
    }
}
