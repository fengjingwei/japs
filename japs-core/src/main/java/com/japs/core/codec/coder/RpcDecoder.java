package com.japs.core.codec.coder;

import com.japs.core.codec.serialization.Serializer;
import io.netty.buffer.ByteBuf;
import io.netty.channel.ChannelHandlerContext;
import io.netty.handler.codec.ByteToMessageDecoder;
import lombok.AllArgsConstructor;

import java.util.List;

@AllArgsConstructor
public class RpcDecoder extends ByteToMessageDecoder {

    private static final Integer LENGTH = 4;
    private Class<?> genericClass;
    private Serializer serializer;

    @Override
    public void decode(ChannelHandlerContext ctx, ByteBuf in, List<Object> out) {
        if (in.readableBytes() < LENGTH) {
            return;
        }
        in.markReaderIndex();
        int dataLength = in.readInt();
        if (in.readableBytes() < dataLength) {
            in.resetReaderIndex();
            return;
        }
        byte[] data = new byte[dataLength];
        in.readBytes(data);
        out.add(serializer.deSerialize(data, genericClass));
    }
}
