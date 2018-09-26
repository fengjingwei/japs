package com.japs.demo.api.hello;

import com.japs.annotation.RpcService;

import java.util.List;

@RpcService(HelloService.class)
public interface HelloService {

    /**
     * hello
     *
     * @param name
     * @return
     */
    String hello(String name);

    /**
     * list
     *
     * @return
     */
    List<String> list();
}
