/*
 * Copyright 2012 The Netty Project
 *
 * The Netty Project licenses this file to you under the Apache License,
 * version 2.0 (the "License"); you may not use this file except in compliance
 * with the License. You may obtain a copy of the License at:
 *
 *   http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS, WITHOUT
 * WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. See the
 * License for the specific language governing permissions and limitations
 * under the License.
 */
package cj.studio.ecm.net.nio;

import io.netty.buffer.ByteBuf;
import io.netty.channel.ChannelHandlerAdapter;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.ChannelPromise;
import io.netty.util.internal.TypeParameterMatcher;

import java.util.List;

/**
 * A Codec for on-the-fly encoding/decoding of bytes to messages and vise-versa.
 *
 * This can be thought of as a combination of {@link ByteToMessageDecoder2} and {@link MessageToByteEncoder2}.
 *
 * Be aware that sub-classes of {@link ByteToMessageCodec2} <strong>MUST NOT</strong>
 * annotated with {@link @Sharable}.
 */
public abstract class ByteToMessageCodec2<I> extends ChannelHandlerAdapter {

    private final TypeParameterMatcher outboundMsgMatcher;
    private final MessageToByteEncoder2<I> encoder;

    private final ByteToMessageDecoder2 decoder = new ByteToMessageDecoder2() {
        @Override
        public void decode(ChannelHandlerContext ctx, ByteBuf in, List<Object> out) throws Exception {
            ByteToMessageCodec2.this.decode(ctx, in, out);
        }

        @Override
        protected void decodeLast(ChannelHandlerContext ctx, ByteBuf in, List<Object> out) throws Exception {
            ByteToMessageCodec2.this.decodeLast(ctx, in, out);
        }
    };

    /**
     * @see {@link #ByteToMessageCodec(boolean)} with {@code true} as boolean parameter.
     */
    protected ByteToMessageCodec2() {
        this(true);
    }

    /**
     * @see {@link #ByteToMessageCodec(Class, boolean)} with {@code true} as boolean value.
     */
    protected ByteToMessageCodec2(Class<? extends I> outboundMessageType) {
        this(outboundMessageType, true);
    }

    /**
     * Create a new instance which will try to detect the types to match out of the type parameter of the class.
     *
     * @param preferDirect          {@code true} if a direct {@link ByteBuf} should be tried to be used as target for
     *                              the encoded messages. If {@code false} is used it will allocate a heap
     *                              {@link ByteBuf}, which is backed by an byte array.
     */
    protected ByteToMessageCodec2(boolean preferDirect) {
        outboundMsgMatcher = TypeParameterMatcher.find(this, ByteToMessageCodec2.class, "I");
        encoder = new Encoder(preferDirect);
    }

    /**
     * Create a new instance
     *
     * @param outboundMessageType   The type of messages to match
     * @param preferDirect          {@code true} if a direct {@link ByteBuf} should be tried to be used as target for
     *                              the encoded messages. If {@code false} is used it will allocate a heap
     *                              {@link ByteBuf}, which is backed by an byte array.
     */
    protected ByteToMessageCodec2(Class<? extends I> outboundMessageType, boolean preferDirect) {
        checkForSharableAnnotation();
        outboundMsgMatcher = TypeParameterMatcher.get(outboundMessageType);
        encoder = new Encoder(preferDirect);
    }

    private void checkForSharableAnnotation() {
        if (getClass().isAnnotationPresent(Sharable.class)) {
            throw new IllegalStateException("@Sharable annotation is not allowed");
        }
    }

    /**
     * Returns {@code true} if and only if the specified message can be encoded by this codec.
     *
     * @param msg the message
     */
    public boolean acceptOutboundMessage(Object msg) throws Exception {
        return outboundMsgMatcher.match(msg);
    }

    @Override
    public void channelRead(ChannelHandlerContext ctx, Object msg) throws Exception {
        decoder.channelRead(ctx, msg);
    }

    @Override
    public void write(ChannelHandlerContext ctx, Object msg, ChannelPromise promise) throws Exception {
        encoder.write(ctx, msg, promise);
    }

//    /**
//     * @see MessageToByteEncoder2#encode(ChannelHandlerContext, Object, ByteBuf)
//     */
//    protected abstract void encode(ChannelHandlerContext ctx, I msg, ByteBuf out) throws Exception;
    
    /**
     * Encode a message into a {@link ByteBuf}. This method will be called for each written message that can be handled
     * by this encoder.
     */
     protected abstract void encode(ChannelHandlerContext ctx, I cast,
			ChannelPromise promise, boolean preferDirect2)throws Exception;
    /**
     * @see ByteToMessageDecoder2#decode(ChannelHandlerContext, ByteBuf, List)
     */
    protected abstract void decode(ChannelHandlerContext ctx, ByteBuf in, List<Object> out) throws Exception;

    /**
     * @see ByteToMessageDecoder2#decodeLast(ChannelHandlerContext, ByteBuf, List)
     */
    protected void decodeLast(ChannelHandlerContext ctx, ByteBuf in, List<Object> out) throws Exception {
        decode(ctx, in, out);
    }

    private final class Encoder extends MessageToByteEncoder2<I> {
        Encoder(boolean preferDirect) {
            super(preferDirect);
        }

        @Override
        public boolean acceptOutboundMessage(Object msg) throws Exception {
            return ByteToMessageCodec2.this.acceptOutboundMessage(msg);
        }
        @Override
        protected void encode(ChannelHandlerContext ctx, I cast,
        		ChannelPromise promise, boolean preferDirect) throws Exception {
        	 ByteToMessageCodec2.this.encode(ctx, cast, promise, preferDirect);
        }
//        @Override
//        protected void encode(ChannelHandlerContext ctx, I msg, ByteBuf out) throws Exception {
//            ByteToMessageCodec2.this.encode(ctx, msg, out);
//        }
    }
}