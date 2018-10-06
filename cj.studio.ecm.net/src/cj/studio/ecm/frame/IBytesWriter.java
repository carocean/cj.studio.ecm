package cj.studio.ecm.frame;
/**
 * 用于netFrame向外输出
 * @author cj
 *
 */
public interface IBytesWriter {
	public void write(byte[] b, int i, int length);
	long length();
	void setLength(long l);
}
