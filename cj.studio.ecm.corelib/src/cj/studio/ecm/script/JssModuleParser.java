package cj.studio.ecm.script;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import cj.studio.ecm.EcmException;
import cj.studio.ecm.context.Element;
import cj.studio.ecm.context.IElement;
import cj.studio.ecm.context.IElementParser;
import cj.studio.ecm.context.IProperty;
import cj.studio.ecm.context.Property;
import cj.ultimate.gson2.com.google.gson.JsonArray;
import cj.ultimate.gson2.com.google.gson.JsonElement;
import cj.ultimate.gson2.com.google.gson.JsonObject;
import cj.ultimate.util.StringUtil;

public class JssModuleParser implements IElementParser {
	private IElement ele;

	public JssModuleParser() {
	}

	public Object getEntity() {
		List<IJssModule> modules = new ArrayList<IJssModule>();
		String names[] = ele.enumNodeNames();
		for (String name : names) {
			JssModule entity = new JssModule();
			modules.add(entity);

			entity.name = name;
			IElement module = (IElement) ele.getNode(name);
			IProperty pack = (IProperty) module.getNode("package");
			if (pack != null)
				entity.pack = pack.getValue().getName();
			IProperty ext = (IProperty) module.getNode("extName");
			if (ext != null)
				entity.ext = ext.getValue().getName();
			IProperty unzip = (IProperty) module.getNode("unzip");
			if (unzip != null)
				entity.unzip = "true".equals(unzip.getValue().getName()) ? true
						: false;
			IProperty runtimeHome = (IProperty) module.getNode("runtimeHome");
			if (runtimeHome != null)
				entity.jssmodruntimeHome = runtimeHome.getValue()==null ? null
						: runtimeHome.getValue().getName();
			IProperty searchMode = (IProperty) module.getNode("searchMode");
			if (searchMode != null)
				entity.searchMode(searchMode.getValue().getName());
		}
		return modules;
	}

	private boolean invalidJssPack(Map<String, String> map, String pack) {
		if (map.containsKey("http.jss") || map.containsKey("ws.jss")) {
			throw new EcmException(String.format("jss模块名冲突，不能使用固定模块名%s", pack));
		}
		for (String v : map.values()) {
			if (StringUtil.isPackageConflict(v, pack)) {
				return true;
			}
		}
		return false;
	}

	@Override
	public void parse(IElement e, JsonElement je) {
		this.ele = e;
		JsonArray jss = (JsonArray) je;
		Map<String, String> map = new HashMap<String, String>();
		for (int i = 0; i < jss.size(); i++) {
			JsonObject module = jss.get(i).getAsJsonObject();

			String moduleName = module.get("module") == null ? "" : module.get(
					"module").getAsString();
			if (StringUtil.isEmpty(moduleName)) {
				throw new EcmException("jss模块名为空");
			}
			if (moduleName.contains(".")) {
				throw new EcmException(String.format("jss模块%s名称含有.号",
						moduleName));
			}
			if (map.containsKey(moduleName)) {
				throw new EcmException(String.format("jss模块%s已存在", moduleName));
			}
			IElement moduleE = new Element(moduleName);
			e.addNode(moduleE);

			moduleE.addNode(new Property("module", moduleName));
			String pack = module.get("package") == null ? "" : module.get(
					"package").getAsString();
			if (invalidJssPack(map, pack)) {
				throw new EcmException(String.format("jss模块%s的包已存在或是子包，冲突：%s",
						moduleName, pack));
			}
			moduleE.addNode(new Property("package", pack));
			map.put(moduleName, pack);

			moduleE.addNode(new Property("unzip",
					module.get("unzip") == null ? "false" : module.get("unzip")
							.getAsString()));
			
			moduleE.addNode(new Property("runtimeHome",
					module.get("runtimeHome") == null ? null: module.get("runtimeHome")
							.getAsString()));
			
			String extName = module.get("extName") == null ? ".jss.js" : module.get(
					"extName").getAsString();
			moduleE.addNode(new Property("extName", extName));
			if (!extName.startsWith(".")) {
				throw new EcmException(String.format("jss模块%s扩展名前必须包括有.号",
						moduleName));
			}
			String searchMode = module.get("searchMode") == null ? "both" : module.get(
					"searchMode").getAsString();
			moduleE.addNode(new Property("searchMode", searchMode));
		}

	}

}
