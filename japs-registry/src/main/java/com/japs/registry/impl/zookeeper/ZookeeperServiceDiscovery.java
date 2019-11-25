package com.japs.registry.impl.zookeeper;

import com.google.common.collect.Maps;
import com.japs.core.common.ServiceAddress;
import com.japs.core.utils.StringUtilsX;
import com.japs.loadbalancing.LoadBalancer;
import com.japs.loadbalancing.impl.RandomLoadBalancer;
import com.japs.registry.ServiceConstant;
import com.japs.registry.ServiceDiscovery;
import lombok.extern.slf4j.Slf4j;
import org.apache.zookeeper.KeeperException;
import org.apache.zookeeper.Watcher;
import org.apache.zookeeper.ZooKeeper;
import org.apache.zookeeper.data.Stat;
import org.springframework.util.CollectionUtils;

import java.util.List;
import java.util.Map;
import java.util.concurrent.CountDownLatch;
import java.util.stream.Collectors;

@Slf4j
public class ZookeeperServiceDiscovery implements ServiceDiscovery, ServiceConstant {

    private static final CountDownLatch LATCH = new CountDownLatch(1);
    private static volatile ZooKeeper zooKeeper;
    private Map<String, LoadBalancer<ServiceAddress>> loadBalancerMap = Maps.newConcurrentMap();

    public ZookeeperServiceDiscovery(String zookeeperAddress) {
        try {
            zooKeeper = new ZooKeeper(zookeeperAddress, SESSION_TIMEOUT, watchedEvent -> {
                if (watchedEvent.getState() == Watcher.Event.KeeperState.SyncConnected) {
                    LATCH.countDown();
                }
            });
            LATCH.await();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    @Override
    public String discover(String serviceName) {
        if (!loadBalancerMap.containsKey(serviceName)) {
            String servicePath = StringUtilsX.join(REGISTRY_PATH, serviceName);
            try {
                List<String> zNodePaths = zooKeeper.getChildren(servicePath, true);
                if (!CollectionUtils.isEmpty(zNodePaths)) {
                    List<String> servers = zNodePaths.stream()
                            .filter(StringUtilsX::isNoneBlank)
                            .map(zNodePath -> {
                                try {
                                    byte[] content = zooKeeper.getData(StringUtilsX.join(servicePath, zNodePath), true, new Stat());
                                    return new String(content);
                                } catch (KeeperException | InterruptedException e) {
                                    e.printStackTrace();
                                }
                                return null;
                            }).collect(Collectors.toList());

                    loadBalancerMap.put(serviceName, buildLoadBalancer(servers));
                }
            } catch (Exception e) {
                log.debug("Get zookeeper data failure : {}", e);
            }
        }

        ServiceAddress address = loadBalancerMap.get(serviceName).next();
        if (address == null) {
            throw new RuntimeException(String.format("No service instance for %s", serviceName));
        }
        return address.toString();
    }

    private LoadBalancer<ServiceAddress> buildLoadBalancer(List<String> servers) {
        return new RandomLoadBalancer(servers.stream().map(server -> {
            String[] serverArr = StringUtilsX.split(server, ":");
            return new ServiceAddress(serverArr[0], Integer.valueOf(serverArr[1]));
        }).collect(Collectors.toList()));
    }
}
