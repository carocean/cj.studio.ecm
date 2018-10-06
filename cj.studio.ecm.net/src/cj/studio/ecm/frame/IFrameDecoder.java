package cj.studio.ecm.frame;

import io.netty.buffer.ByteBuf;

/**
 * 用于从流中得到侦
 * <pre>
 *
 * </pre>
 * @author carocean
 *
 */
public interface IFrameDecoder {
	void write(ByteBuf d,Object... attachArgs);
	void setDecoder(IFrameDecoderCallback e);
}
