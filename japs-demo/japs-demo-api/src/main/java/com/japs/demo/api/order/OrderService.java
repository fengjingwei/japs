package com.japs.demo.api.order;

import com.japs.annotation.RpcService;

import java.util.List;

@RpcService(OrderService.class)
public interface OrderService {

    /**
     * list
     *
     * @return
     */
    List<String> list();
}
