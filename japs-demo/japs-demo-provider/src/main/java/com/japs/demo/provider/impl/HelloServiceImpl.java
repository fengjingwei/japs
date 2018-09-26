package com.japs.demo.provider.impl;

import com.japs.demo.api.hello.HelloService;
import org.springframework.stereotype.Component;

import java.util.Arrays;
import java.util.List;

@Component
public class HelloServiceImpl implements HelloService {

    @Override
    public String hello(String name) {
        return "Hello! " + name;
    }

    @Override
    public List<String> list() {
        return Arrays.asList("a", "b", "c");
    }

}
