package com.japs.core.utils;

import org.springframework.beans.BeansException;
import org.springframework.context.ApplicationContext;

import java.util.Objects;

public final class SpringBeanUtils {

    private static final SpringBeanUtils INSTANCE = new SpringBeanUtils();

    private static ApplicationContext applicationContext = null;

    public static SpringBeanUtils getInstance() {
        return INSTANCE;
    }

    public <T> T getBean(final Class<T> type) {
        Objects.requireNonNull(type);
        return applicationContext.getBean(type);
    }

    public void setApplicationContext(ApplicationContext applicationContext) throws BeansException {
        SpringBeanUtils.applicationContext = applicationContext;
    }

    public ApplicationContext getApplicationContext() {
        return SpringBeanUtils.applicationContext;
    }
}
