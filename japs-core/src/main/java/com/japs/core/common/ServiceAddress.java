package com.japs.core.common;

import lombok.AllArgsConstructor;
import lombok.Data;

@Data
@AllArgsConstructor
public class ServiceAddress {

    private String ip;

    private int port;

    @Override
    public String toString() {
        return String.format("%s:%s", ip, port);
    }
}