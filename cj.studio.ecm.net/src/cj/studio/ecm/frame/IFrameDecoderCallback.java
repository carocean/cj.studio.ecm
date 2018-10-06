package cj.studio.ecm.frame;

public interface IFrameDecoderCallback {

	Frame onNewFrame(Frame frame,Object...attachArgs);

}
