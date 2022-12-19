package com.japs.registry.impl.nacos;

import com.alibaba.nacos.api.NacosFactory;
import com.alibaba.nacos.api.PropertyKeyConst;
import com.alibaba.nacos.api.common.Constants;
import com.alibaba.nacos.api.exception.NacosException;
import com.alibaba.nacos.api.naming.NamingService;
import com.alibaba.nacos.api.naming.pojo.Instance;
import com.google.common.collect.Maps;
import com.japs.core.common.ServiceAddress;
import com.japs.core.utils.StringUtilsX;
import com.japs.loadbalancing.LoadBalancer;
import com.japs.loadbalancing.impl.RandomLoadBalancer;
import com.japs.registry.ServiceConstant;
import com.japs.registry.ServiceDiscovery;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.NonNull;
import lombok.extern.slf4j.Slf4j;

import java.util.List;
import java.util.Map;
import java.util.Properties;
import java.util.stream.Collectors;

@Slf4j
@Data
@NoArgsConstructor
public class NacosServiceDiscovery implements ServiceDiscovery, ServiceConstant {

    private final Map<String, LoadBalancer<ServiceAddress>> loadBalancerMap = Maps.newConcurrentMap();

    @NonNull
    private String serverAddress;

    private String namespace;

    private String group;

    @Override
    public String discover(String serviceName) {
        try {
            Properties properties = new Properties();
            properties.put(PropertyKeyConst.SERVER_ADDR, serverAddress);
            properties.put(PropertyKeyConst.NAMESPACE, StringUtilsX.isNotBlank(namespace) ? namespace : Constants.DEFAULT_NAMESPACE_ID);
            properties.put("group", StringUtilsX.isNotBlank(group) ? group : Constants.GROUP);
            NamingService namingService = NacosFactory.createNamingService(properties);
            List<String> servers = namingService.getAllInstances(serviceName, true).stream().map(Instance::toInetAddr).collect(Collectors.toList());
            loadBalancerMap.put(serviceName, buildLoadBalancer(servers));
        } catch (NacosException e) {
            log.error("Get nacos data failure : {}", e);
        }
        ServiceAddress address = loadBalancerMap.get(serviceName).next();
        if (address == null) {
            throw new RuntimeException(String.format("No service instance for %s", serviceName));
        }
        return address.toString();
    }

    private LoadBalancer<ServiceAddress> buildLoadBalancer(List<String> servers) {
        return new RandomLoadBalancer(servers.stream()
                .map(server ->
                        {
                            String[] serverArr = StringUtilsX.split(server, ":");
                            return new ServiceAddress(serverArr[0], Integer.valueOf(serverArr[1]));
                        }
                )
                .collect(Collectors.toList()));
    }
}
