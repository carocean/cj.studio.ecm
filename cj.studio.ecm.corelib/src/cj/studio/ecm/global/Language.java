package cj.studio.ecm.global;

import java.io.IOException;
import java.io.InputStream;
import java.util.HashMap;
import java.util.Map;
import java.util.PropertyResourceBundle;
import java.util.ResourceBundle;
import java.util.Set;

import cj.studio.ecm.Environment;

public class Language implements ILanguage {
	private Map<String, String> index;
	private String lang_code;
	private String repository;
	private String defaultCode;
	public Language(String repositoryPath,String defaultCode) {
		this.repository=repositoryPath;
		this.defaultCode=defaultCode;
	}
	@Override
	public void scan(String lang_code,ClassLoader loader) {
		index=new HashMap<String, String>();
		this.lang_code = lang_code;
		String gr = repository;
		String propFile = "";
		if(loader==null){
			 loader = Environment.class.getClassLoader();
		}
		propFile = gr + "/global_" + lang_code + ".properties";
		InputStream in = loader.getResourceAsStream(propFile.replace("//",
				"/"));
		if (in == null) {
			String defaultLang = defaultCode;
			this.lang_code = defaultLang;
			propFile = gr + "/global_" + defaultLang + ".properties";
			in = loader.getResourceAsStream(propFile.replace("//", "/"));
		}
		if (in == null)
			return;
		ResourceBundle rb;
		try {
			rb = new PropertyResourceBundle(in);
			Set<String> set = rb.keySet();
			for (String key : set) {
				String value = rb.getString(key);
				index.put(key, value);
			}
//			Properties p = new Properties();
//			p.load(in);
//			 Enumeration<?> e=p.propertyNames();
//			while(e.hasMoreElements()){
//				String key=(String)e.nextElement();
//				 Object value=p.getProperty(key);
//			 System.out.println(key+"="+value);
//			 }
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}

	}

	@Override
	public String getDefaultLanguageCode() {
		return defaultCode;
	}

	@Override
	public String getLocaleCode() {
		// TODO Auto-generated method stub
		return lang_code;
	}

	@Override
	public String lookup(String prop,String...args) {
		Object[] arr=(String[]) args;
		String str=String.format(index.get(prop), arr);
		return str;
	}

}
