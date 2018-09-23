package com.japs.demo.provider;

import com.japs.demo.api.OrderService;
import org.springframework.stereotype.Component;

import java.util.Arrays;
import java.util.List;

@Component
public class OrderServiceImpl implements OrderService {

    @Override
    public List<String> list() {
        return Arrays.asList("order-id-1", "order-id-2", "order-id-3");
    }

}
