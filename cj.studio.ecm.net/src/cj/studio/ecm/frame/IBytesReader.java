package cj.studio.ecm.frame;
/**
 * 用于为netFrame读取数据
 * @author cj
 *
 */
public interface IBytesReader {
	public int read(byte[] b, int i, long len);
	public void readFully(byte[] frame, int i, long len);
	long length();
	void setLength(long l);
}
