package com.japs.demo.consumer;

import com.japs.demo.api.hello.HelloService;
import com.japs.demo.api.order.OrderService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.support.ClassPathXmlApplicationContext;

import java.util.concurrent.TimeUnit;

@Slf4j
public class Consumer {

    public static void main(String[] args) {
        ClassPathXmlApplicationContext context = new ClassPathXmlApplicationContext("applicationContext.xml");
        context.start();
        HelloService helloService = context.getBean(HelloService.class);
        OrderService orderService = context.getBean(OrderService.class);
        while (true) {
            try {
                TimeUnit.MILLISECONDS.sleep(1000);
                log.info("{}", helloService.hello("我来了，这世界"));

                log.info("{}", helloService.list());

                log.info("{}", orderService.list());
            } catch (Throwable throwable) {
                throwable.printStackTrace();
            }
        }
    }
}
