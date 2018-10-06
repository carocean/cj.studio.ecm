package cj.studio.ecm.logging;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;

public class ConsoleOutter implements IOutter {
	// 一些格式化定义
	private Map<String, String> props;
	private String owner;

	public ConsoleOutter() {

	}

	@Override
	public void setOwner(String owner) {
		this.owner = owner;
	}

	@Override
	public void loadProperties(Map<String, String> propMap) {
		props = propMap;
	}

	@Override
	public IOutter copy() {
		ConsoleOutter out = new ConsoleOutter();
		Map<String, String> map = new HashMap<String, String>();
		for (String key : props.keySet()) {
			map.put(key, props.get(key));
		}
		out.props = map;
		out.owner = "";
		return out;
	}

	@Override
	public void print(Object message) {
		boolean isContainsDate = true;
		if (message instanceof String) {
			String msg = (String) message;
			isContainsDate = !msg.startsWith("\t");
		}
		if (isContainsDate) {
			SimpleDateFormat formatter = new SimpleDateFormat(
					"yyyy年MM月dd日 HH:mm:ss ");
			Date curDate = new Date(System.currentTimeMillis());// 获取当前时间
			String date = formatter.format(curDate);
			String str = String.format("%s %s", date, message);
			System.out.println(str);
		} else {
			String str = String.format("%s", message);
			System.out.println(str);
		}
	}

	@Override
	public void print(Object message, Throwable t) {
		boolean isContainsDate = true;
		if (message instanceof String) {
			String msg = (String) message;
			isContainsDate = !msg.startsWith("\t");
		}
		if (isContainsDate) {
			SimpleDateFormat formatter = new SimpleDateFormat(
					"yyyy年MM月dd日 HH:mm:ss ");
			Date curDate = new Date(System.currentTimeMillis());// 获取当前时间
			String date = formatter.format(curDate);
			String str = String.format("%s %s 原因：%s", date, message,
					t.getMessage());
			System.out.println(str);
		} else {
			String str = String.format("%s", message);
			System.out.println(str);
		}
	}

}
