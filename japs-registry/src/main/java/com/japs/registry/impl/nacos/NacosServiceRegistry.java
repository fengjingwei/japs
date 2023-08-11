package com.japs.registry.impl.nacos;

import com.alibaba.nacos.api.PropertyKeyConst;
import com.alibaba.nacos.api.common.Constants;
import com.alibaba.nacos.api.exception.NacosException;
import com.alibaba.nacos.api.naming.NamingFactory;
import com.alibaba.nacos.api.naming.NamingService;
import com.japs.core.common.ServiceAddress;
import com.japs.core.utils.StringUtilsX;
import com.japs.registry.ServiceConstant;
import com.japs.registry.ServiceRegistry;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.NonNull;
import lombok.extern.slf4j.Slf4j;

import java.util.Properties;

@Slf4j
@Data
@NoArgsConstructor
public class NacosServiceRegistry implements ServiceRegistry, ServiceConstant {

    @NonNull
    private String serverAddress;

    private String namespace;

    private String group;

    @Override
    public void register(String serviceName, ServiceAddress serviceAddress) {
        Properties properties = new Properties();
        properties.put(PropertyKeyConst.SERVER_ADDR, serverAddress);
        properties.put(PropertyKeyConst.NAMESPACE, StringUtilsX.isNotBlank(namespace) ? namespace : Constants.DEFAULT_NAMESPACE_ID);
        properties.put("group", StringUtilsX.isNotBlank(group) ? group : Constants.GROUP);
        try {
            NamingService namingService = NamingFactory.createNamingService(properties);
            namingService.registerInstance(serviceName, serviceAddress.getIp(), serviceAddress.getPort());
        } catch (NacosException e) {
            log.error("Register nacos service failure", e);
        }
    }
}
