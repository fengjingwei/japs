package com.japs.core.common;

import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class RpcRequest {

    private String requestId;

    private String interfaceName;

    private String methodName;

    private Class<?>[] parameterTypes;

    private Object[] parameters;
}
