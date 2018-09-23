package com.japs.core.utils;

import org.springframework.context.ConfigurableApplicationContext;

import java.util.Objects;

public final class SpringBeanUtils {

    private static final SpringBeanUtils INSTANCE = new SpringBeanUtils();

    private ConfigurableApplicationContext cfgContext;

    public static SpringBeanUtils getInstance() {
        return INSTANCE;
    }

    public <T> T getBean(final Class<T> type) {
        Objects.requireNonNull(type);
        return cfgContext.getBean(type);
    }

    public void setBean(final String beanName, final Object obj) {
        Objects.requireNonNull(beanName);
        Objects.requireNonNull(obj);
        cfgContext.getBeanFactory().registerSingleton(beanName, obj);
    }

    public void setCfgContext(final ConfigurableApplicationContext cfgContext) {
        this.cfgContext = cfgContext;
    }
}
