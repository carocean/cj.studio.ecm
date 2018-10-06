package cj.studio.ecm;

import java.io.InputStream;

//程序集信息，读取在assembly.json中的描述
public interface IAssemblyInfo {
	String getName();
	String getGuid();
	String getDescription();
	String getVersion();
	String getFileVersion();
	String getCompany();
	String getProduct();
	String getCopyright();
	InputStream getIconStream();
	String getProperty(String name);
}
