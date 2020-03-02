package com.japs.registry.impl.consul;

import com.ecwid.consul.v1.ConsulClient;
import com.ecwid.consul.v1.ConsulRawClient;
import com.ecwid.consul.v1.agent.model.NewService;
import com.japs.core.common.ServiceAddress;
import com.japs.registry.ServiceRegistry;

import java.util.ArrayList;

public class ConsulServiceRegistry implements ServiceRegistry {

    private final ConsulClient consulClient;

    public ConsulServiceRegistry(String consulAddress) {
        String[] address = consulAddress.split(":");
        ConsulRawClient rawClient = new ConsulRawClient(address[0], Integer.valueOf(address[1]));
        consulClient = new ConsulClient(rawClient);
    }

    @Override
    public void register(String serviceName, ServiceAddress serviceAddress) {
        NewService newService = new NewService();
        newService.setId(generateNewIdForService(serviceName, serviceAddress));
        newService.setName(serviceName);
        newService.setTags(new ArrayList<>());
        newService.setAddress(serviceAddress.getIp());
        newService.setPort(serviceAddress.getPort());

        NewService.Check check = new NewService.Check();
        check.setTcp(serviceAddress.toString());
        check.setInterval("1s");
        newService.setCheck(check);
        consulClient.agentServiceRegister(newService);
    }

    private String generateNewIdForService(String serviceName, ServiceAddress serviceAddress) {
        // serviceName + ip + port
        return String.format("%s-%s-%s", serviceName, serviceAddress.getIp(), serviceAddress.getPort());
    }
}