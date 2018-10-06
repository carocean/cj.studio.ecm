package cj.studio.ecm.logging;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;

import cj.ultimate.util.StringUtil;

public class FileOutter implements IOutter {
	private FileWriter fw;
	private Map<String, String> propMap;
	private String owner;

	public FileOutter() {
		// TODO Auto-generated constructor stub
	}

	@Override
	public IOutter copy() {
		FileOutter out = new FileOutter();
		Map<String, String> map = new HashMap<String, String>();
		for (String key : propMap.keySet()) {
			map.put(key, propMap.get(key));
		}
		out.propMap = map;
		out.owner = "";
		out.fw = null;
		return out;
	}

	@Override
	public void setOwner(String owner) {
		this.owner = owner;
	}

	@Override
	public void loadProperties(Map<String, String> propMap) {
		this.propMap = propMap;
		fw = null;
	}

	private void createFile() {
		String createType = propMap.get("type");
		if (!propMap.containsKey("type")) {
			createType = "autocreatepath";
		}
		try {
			String exten = getLogFileName();
			if (!StringUtil.isEmpty(exten)) {
				String sp = System.getProperty("file.separator");
				String curDir = System.getProperty("user.dir")
						+ String.format("%slogs%s%s.log", sp, sp, exten);
				File f = new File(curDir);
				if (!f.exists())
					f.createNewFile();
				fw = new FileWriter(f, true);
				return;
			}
			if ("autocreatepath".equals(createType)) {
				String sp = System.getProperty("file.separator");
				String curDir = System.getProperty("user.dir")
						+ String.format("%slogs%s%s.log", sp, sp, owner);
				File f = new File(curDir);
				if (!f.getParentFile().exists())
					f.getParentFile().mkdir();
				if (!f.exists())
					f.createNewFile();
				fw = new FileWriter(f, true);
			}
			if ("designatedspot".equals(createType)) {
				String curDir = propMap.get("file");
				fw = new FileWriter(curDir);
			}
		} catch (Exception e) {
			throw new RuntimeException(e);
		}
	}

	/**
	 * 获取日志文件名
	 * 
	 * @return
	 */
	protected String getLogFileName() {
		return "";
	}

	@Override
	public void print(Object message) {
		if (fw == null)
			createFile();
		boolean isContainsDate = true;
		if (message instanceof String) {
			String msg = (String) message;
			isContainsDate = !msg.startsWith("\t");
		}
		String str = "";
		if (isContainsDate) {
			SimpleDateFormat formatter = new SimpleDateFormat(
					"yyyy年MM月dd日 HH:mm:ss ");
			Date curDate = new Date(System.currentTimeMillis());// 获取当前时间
			String date = formatter.format(curDate);
			str = String.format("%s %s" , date, message);
		} else {
			str = String.format("%s", message);
			System.out.println(str);
		}
		try {
			fw.append(str + "\r\n");
			fw.flush();
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
	}

	@Override
	public void print(Object message, Throwable t) {
		if (fw == null)
			createFile();
		boolean isContainsDate = true;
		if (message instanceof String) {
			String msg = (String) message;
			isContainsDate = !msg.startsWith("\t");
		}
		String str = "";
		if (isContainsDate) {
			SimpleDateFormat formatter = new SimpleDateFormat(
					"yyyy年MM月dd日 HH:mm:ss ");
			Date curDate = new Date(System.currentTimeMillis());// 获取当前时间
			String date = formatter.format(curDate);
			str = String.format("%s" + message + "  原因：%s", date,
					t.getMessage());
		} else {
			str = String.format("%s", message);
			System.out.println(str);
		}
		try {
			fw.append(str + "\r\n");
			fw.flush();
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
	}

}
