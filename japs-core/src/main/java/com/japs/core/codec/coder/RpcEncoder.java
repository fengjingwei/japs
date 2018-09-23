package com.japs.core.codec.coder;

import com.japs.core.codec.serialization.Serializer;
import io.netty.buffer.ByteBuf;
import io.netty.channel.ChannelHandlerContext;
import io.netty.handler.codec.MessageToByteEncoder;
import lombok.AllArgsConstructor;

@AllArgsConstructor
public class RpcEncoder extends MessageToByteEncoder {

    private Class<?> genericClass;

    private Serializer serializer;

    @Override
    public void encode(ChannelHandlerContext ctx, Object in, ByteBuf out) {
        if (genericClass.isInstance(in)) {
            byte[] data = serializer.serialize(in);
            out.writeInt(data.length);
            out.writeBytes(data);
        }
    }
}
