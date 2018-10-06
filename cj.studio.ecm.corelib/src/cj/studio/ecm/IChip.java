package cj.studio.ecm;


/**
 * 芯片，程序集的逻辑名。
 * <pre>
 * 一个程序集即是一个芯片
 * 在程序集内可见，即为程序集开发者使用。
 * </pre>
 * @author carocean
 *
 */
public interface IChip {
	IChipInfo info();
	IServiceSite site();
}
