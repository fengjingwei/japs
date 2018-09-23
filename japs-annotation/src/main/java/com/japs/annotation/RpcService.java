package com.japs.annotation;

import org.springframework.context.annotation.Configuration;

import java.lang.annotation.*;

@Target(ElementType.TYPE)
@Retention(RetentionPolicy.RUNTIME)
@Configuration
@Inherited
public @interface RpcService {

    Class<?> value();
}
