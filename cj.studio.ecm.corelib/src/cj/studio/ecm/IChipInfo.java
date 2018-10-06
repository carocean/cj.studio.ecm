package cj.studio.ecm;

import java.io.InputStream;


/**
 * 表示芯片的信息。芯片在一个程序集内是虚拟的且唯一的，一个程序集包括一个模块一个模块就是一个芯片<br>
 * @author c.j
 *
 */

public interface IChipInfo{
	String CHIP_DEFAULT_ICON_KEY="cj/studio/ecm/assemlby-default.icon";
	String getId();
	String getName();
	String getDescription();
	String getVersion();
	String getResource();
	String getProperty(String name);
	String[] enumProperty();
	boolean isWebChip();
	InputStream getIconStream();
	String getIconFileName();
	public String getResourceProp(String key);
	public String[] enumeResourceProp();
	String getCompany();
	String getCopyright();
	String getProduct();
	/**
	 * 开发者个人主页网址
	 * <pre>
	 *
	 * </pre>
	 * @return
	 */
	String getDeveloperHome();
}
