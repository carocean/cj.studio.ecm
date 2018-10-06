package cj.studio.ecm.memory;
//指针,指针地址是对象的哈唏，即：哈唏作为对象的句柄
public class Point {
	private int address;
	private int offset;
	public Point(int address) {
		this.address=address;
	}
	public int getAddress() {
		return address;
	}
	public int getOffset() {
		return offset;
	}
	public void setOffset(int offset) {
		this.offset = offset;
	}
}
