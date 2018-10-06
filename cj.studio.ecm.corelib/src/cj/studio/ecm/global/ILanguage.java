package cj.studio.ecm.global;

//语言.管理系统和芯片的语言，芯片的语言由Global中获取
public interface ILanguage {
	void scan(String lang_code,ClassLoader loader);
	String getDefaultLanguageCode();

	// 本地编码，如zh-CN,en等
	String getLocaleCode();

	String lookup(String prop,String...args);
}
