package com.japs.registry.impl.zookeeper;

import com.japs.core.common.ServiceAddress;
import com.japs.core.utils.BaseStringUtils;
import com.japs.registry.ServiceConstant;
import com.japs.registry.ServiceRegistry;
import lombok.extern.slf4j.Slf4j;
import org.apache.zookeeper.CreateMode;
import org.apache.zookeeper.Watcher;
import org.apache.zookeeper.ZooDefs;
import org.apache.zookeeper.ZooKeeper;

import java.util.concurrent.CountDownLatch;

@Slf4j
public class ZookeeperServiceRegistry implements ServiceRegistry, ServiceConstant {

    private static volatile ZooKeeper zooKeeper;

    private static final CountDownLatch LATCH = new CountDownLatch(1);

    private ZookeeperServiceRegistry(String zookeeperAddress) {
        try {
            zooKeeper = new ZooKeeper(zookeeperAddress, SESSION_TIMEOUT, watchedEvent -> {
                if (watchedEvent.getState() == Watcher.Event.KeeperState.SyncConnected) {
                    LATCH.countDown();
                }
            });
            LATCH.await();
            log.info("Connected to zookeeper");
        } catch (Exception e) {
            log.error("Create zookeeper client failure", e);
        }
    }

    @Override
    public void register(String serviceName, ServiceAddress serviceAddress) {
        try {
            String registryPath = REGISTRY_PATH;

            if (zooKeeper.exists(registryPath, false) == null) {
                zooKeeper.create(registryPath, registryPath.getBytes(), ZooDefs.Ids.OPEN_ACL_UNSAFE, CreateMode.PERSISTENT);
                log.debug("Create registry node : {}", registryPath);
            }

            // Create service node
            String servicePath = BaseStringUtils.join(registryPath, serviceName);
            if (zooKeeper.exists(servicePath, false) == null) {
                zooKeeper.create(servicePath, servicePath.getBytes(), ZooDefs.Ids.OPEN_ACL_UNSAFE, CreateMode.PERSISTENT);
                log.debug("Create service node : {}", servicePath);
            }

            // Create address node
            String addressPath = BaseStringUtils.join(servicePath, "address-");
            String addressNode = zooKeeper.create(addressPath, serviceAddress.toString().getBytes(), ZooDefs.Ids.OPEN_ACL_UNSAFE, CreateMode.EPHEMERAL_SEQUENTIAL);
            log.debug("Create address node : {} => {}", addressNode, serviceAddress);
        } catch (Exception e) {
            log.debug("Create node failure : {}", e);
        }
    }
}
