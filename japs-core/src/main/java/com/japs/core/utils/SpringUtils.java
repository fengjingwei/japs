package com.japs.core.utils;

import org.springframework.beans.BeansException;
import org.springframework.context.ApplicationContext;

import java.util.Objects;

public final class SpringUtils {

    private static ApplicationContext applicationContext = null;

    public <T> T getBean(final Class<T> type) {
        Objects.requireNonNull(type);
        return applicationContext.getBean(type);
    }

    public ApplicationContext getApplicationContext() {
        return applicationContext;
    }

    public void setApplicationContext(ApplicationContext applicationContext) throws BeansException {
        SpringUtils.applicationContext = applicationContext;
    }
}
