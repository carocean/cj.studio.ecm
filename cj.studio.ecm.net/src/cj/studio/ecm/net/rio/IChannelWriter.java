package cj.studio.ecm.net.rio;

import java.io.IOException;

import cj.studio.ecm.frame.Frame;
import cj.ultimate.IDisposable;

public interface IChannelWriter extends IDisposable {

	void write(Frame frame)throws IOException;

}
