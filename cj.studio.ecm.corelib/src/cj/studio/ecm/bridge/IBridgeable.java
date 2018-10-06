package cj.studio.ecm.bridge;
/**
 * 一个声明为桥的服务均可转换为此接口
 * @author carocean
 *
 */
public interface IBridgeable {
	public <T> T  getBridge(String aspects);
}
