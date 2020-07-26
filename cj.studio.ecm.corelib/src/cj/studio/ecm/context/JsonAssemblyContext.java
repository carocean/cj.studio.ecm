package cj.studio.ecm.context;

import java.io.File;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.Map.Entry;
import java.util.Properties;

import cj.studio.ecm.CJSystem;
import cj.studio.ecm.EcmException;
import cj.studio.ecm.Environment;
import cj.studio.ecm.IAssemblyContext;
import cj.studio.ecm.domain.ChipDomain;
import cj.studio.ecm.global.ILanguage;
import cj.studio.ecm.global.Language;
import cj.studio.ecm.json.JsonReflector;
import cj.studio.ecm.logging.ILogging;
import cj.studio.ecm.logging.LoggingConfigurator;
import cj.studio.ecm.resource.IResource;
import cj.studio.ecm.script.JssModuleJsonParser;
import cj.studio.ecm.util.ObjectHelper;
import cj.studio.ecm.util.ObjectmMedium;
import cj.ultimate.gson2.com.google.gson.JsonArray;
import cj.ultimate.gson2.com.google.gson.JsonElement;
import cj.ultimate.gson2.com.google.gson.JsonObject;
import cj.ultimate.util.StringUtil;

public class JsonAssemblyContext implements IAssemblyContext {
    private IResource resource;
    private IElement element;
    protected ObjectmMedium domainPropsRef;
    protected Properties props;

    public JsonAssemblyContext(IResource resource, InputStream stream, Properties properties) {
        this.resource = resource;
        this.props = properties;
        try {
            domainPropsRef = (ObjectmMedium) ObjectHelper.get(resource,
                    "domainPropsRef");
        } catch (Exception e) {
            throw new EcmException(e);
        }
        JsonReflector refl = new JsonReflector(stream);
        JsonElement element = refl.getContext();
        this.parse(element);
    }

    public String[] enumProperty() {
        return props.keySet().toArray(new String[0]);
    }

    public String getProperty(String key) {
        if ("home.dir".equals(key)) {
            String f = resource.getResourcefile();
            File file = new File(f);
            return file.getParent();
        }
        if ("assembly.fileName".equals(key)) {
            String f = resource.getResourcefile();
            File file = new File(f);
            return file.getName();
        }
        return props.getProperty(key);
    }

    private void parseEntryPoint(JsonObject e) {
        JsonElement ep = e.get("entryPoint");
        if (!ep.isJsonObject())
            throw new RuntimeException("入口点需是对象格式。");
        JsonObject epObj = ep.getAsJsonObject();
        IElement entrypoint = new Element("entryPoint");
        //plugins
        JsonArray pluginsArr = epObj.get("plugins") == null ? null : epObj.get(
                "plugins").getAsJsonArray();
        if (pluginsArr != null) {
            IElement plugins = new Element("plugins");
            for (int i = 0; i < pluginsArr.size(); i++) {
                JsonObject plugin = pluginsArr.get(i).getAsJsonObject();
                IElement pluginObj = new Element("plugin_" + i);
                plugins.addNode(pluginObj);
                IProperty a = new Property("name", plugin.get("name")
                        .getAsString());
                pluginObj.addNode(a);
                IProperty b = new Property("class", plugin.get("class")
                        .getAsString());
                pluginObj.addNode(b);
                if (plugin.get("parameters") == null) {
                    throw new EcmException(String.format("parameters of the activator %s  is null.", plugin.get("class")));
                }
                IElement c = new Element("parameters");
                JsonObject paramJo = plugin.get("parameters")
                        .getAsJsonObject();
                Iterator<Entry<String, JsonElement>> it = paramJo.entrySet()
                        .iterator();
                while (it.hasNext()) {
                    Entry<String, JsonElement> entry = it.next();
                    String value = paramJo.get(entry.getKey()).getAsString();
                    INode param = new Property(entry.getKey(), value);
                    c.addNode(param);
                }
                pluginObj.addNode(c);

            }
            entrypoint.addNode(plugins);
        }
        // activators
        JsonArray jarr = epObj.get("activators") == null ? null : epObj.get(
                "activators").getAsJsonArray();
        if (jarr != null) {
            IElement activators = new Element("activators");
            for (int i = 0; i < jarr.size(); i++) {
                JsonObject activator = jarr.get(i).getAsJsonObject();
                IElement activObj = new Element("activator_" + i);
                activators.addNode(activObj);
                IProperty a = new Property("name", activator.get("name")
                        .getAsString());
                activObj.addNode(a);
                IProperty b = new Property("class", activator.get("class")
                        .getAsString());
                activObj.addNode(b);
                if (activator.get("parameters") == null) {
                    throw new EcmException(String.format("parameters of the activator %s  is null.", activator.get("class")));
                }
                IElement c = new Element("parameters");
                JsonObject paramJo = activator.get("parameters")
                        .getAsJsonObject();
                Iterator<Entry<String, JsonElement>> it = paramJo.entrySet()
                        .iterator();
                while (it.hasNext()) {
                    Entry<String, JsonElement> entry = it.next();
                    String value = paramJo.get(entry.getKey()).getAsString();
                    INode param = new Property(entry.getKey(), value);
                    c.addNode(param);
                }
                activObj.addNode(c);

            }
            entrypoint.addNode(activators);
        }
        element.addNode(entrypoint);
        // System.out.println(element.toString());
    }

    private void parseAssemblyInfo(JsonObject e) {
        JsonElement ep = e.get("assemblyInfo");
        if (!ep.isJsonObject())
            throw new RuntimeException("入口点需是对象格式。");
        JsonObject info = ep.getAsJsonObject();
        IElement infoObj = new Element("assemblyInfo");
        if (info.get("assemblyResource") != null)
            infoObj.addNode(new Property("assemblyResource", new Node(info.get(
                    "assemblyResource").getAsString())));
        infoObj.addNode(new Property("assemblyTitle", new Node(info.get(
                "assemblyTitle").getAsString())));
        infoObj.addNode(new Property("assemblyDescription", new Node(info.get(
                "assemblyDescription").getAsString())));
        infoObj.addNode(new Property("assemblyConfiguration", new Node(info
                .get("assemblyConfiguration").getAsString())));
        infoObj.addNode(new Property("assemblyCompany", new Node(info.get(
                "assemblyCompany").getAsString())));
        infoObj.addNode(new Property("assemblyProduct", new Node(info.get(
                "assemblyProduct").getAsString())));
        infoObj.addNode(new Property("assemblyCopyright", new Node(info.get(
                "assemblyCopyright").getAsString())));
        infoObj.addNode(new Property("assemblyTrademark", new Node(info.get(
                "assemblyTrademark").getAsString())));
        infoObj.addNode(new Property("assemblyCulture", new Node(info.get(
                "assemblyCulture").getAsString())));
        infoObj.addNode(new Property("guid", new Node(info.get("guid")
                .getAsString())));
        infoObj.addNode(new Property("assemblyIcon", new Node(info
                .get("assemblyIcon") == null ? "" : info.get("assemblyIcon")
                .getAsString())));
        infoObj.addNode(new Property("assemblyVersion", new Node(info.get(
                "assemblyVersion").getAsString())));
        infoObj.addNode(new Property("assemblyFileVersion", new Node(info.get(
                "assemblyFileVersion").getAsString())));
        infoObj.addNode(new Property("assemblyDeveloperHome", new Node(
                ElementGet.getJsonProp(info.get("assemblyDeveloperHome")))));

        this.element.addNode(infoObj);
        this.domainPropsRef.set(ChipDomain.CHIP_INFO_KEY, infoObj);
        // System.out.println(element.toString());
    }

    private void parseServiceContainer(JsonObject e) {
        JsonElement ep = e.get("serviceContainer");
        if (!ep.isJsonObject())
            throw new RuntimeException("服务容器需是对象格式。");
        JsonObject container = ep.getAsJsonObject();
        IElement containerObj = new Element("serviceContainer");
        containerObj.addNode(new Property("name", container.get("name")
                .getAsString()));
        // containerObj.addNode(new Property("classloader", ElementGet
        // .getJsonProp(container.get("classloader"))));
        containerObj.addNode(new Property("switchFilter", container
                .get("switchFilter") == null ? "off" : container.get(
                "switchFilter").getAsString()));
        String monitors = "";
        if (container
                .get("monitors") == null) {
            monitors = container
                    .get("monitor") == null ? "" : container
                    .get("monitor").getAsString();
        } else {
            monitors = container.get(
                    "monitors").getAsString();
        }
        containerObj.addNode(new Property("monitors", monitors));
        JsonArray jss = container.get("jss") == null ? null : container.get(
                "jss").getAsJsonArray();
        if (jss != null) {
            IElement jssArr = new Element("jss");
            IElementParser parser = new JssModuleJsonParser();
            jssArr.parse(jss, parser);
            containerObj.addNode(jssArr);
        }
        JsonArray scans = container.get("scans").getAsJsonArray();
        IElement scansArr = new Element("scans");
        containerObj.addNode(scansArr);
        for (int i = 0; i < scans.size(); i++) {
            JsonObject obj = scans.get(i).getAsJsonObject();
            IElement scanObj = new Element("scan_" + i);
            scansArr.addNode(scanObj);
            scanObj.addNode(new Property("package", obj.get("package")
                    .getAsString()));
            scanObj.addNode(new Property("extName", obj.get("extName")
                    .getAsString()));
            scanObj.addNode(new Property("exoterical",
                    obj.get("exoterical") == null ? "false" : obj.get(
                            "exoterical").getAsString()));
        }
        this.element.addNode(containerObj);
        // System.out.println(element.toString());
    }

    protected void parse(JsonElement root) {

        if (root.isJsonNull() || (!root.isJsonObject()))
            throw new RuntimeException("格式错误");
        element = new Element("assemblyContext");
        JsonObject ctx = root.getAsJsonObject();
        this.parseEntryPoint(ctx);
        this.parseAssemblyInfo(ctx);
        this.parseServiceContainer(ctx);
        this.parseGlobal(ctx);
        this.parseLogging();
    }

    private void parseLogging() {
        IElement logging = new Element("logging");
        Environment en = CJSystem.current().environment();
        String chipconfile = en.getProperty("chip-config-file");
        IElement infoObj = (IElement) domainPropsRef
                .get(ChipDomain.CHIP_INFO_KEY);
        IProperty node = (IProperty) infoObj.getNode("assemblyTitle");
        String name = node.getValue().getName();
        IProperty vernode = (IProperty) infoObj.getNode("assemblyVersion");
        String ver = vernode.getValue().getName();
        String fullName = name + (StringUtil.isEmpty(ver) ? "" : "_" + ver);
        LoggingConfigurator conf = new LoggingConfigurator(fullName);
        InputStream in = resource.getResourceAsStream(chipconfile);
        conf.config(in, resource.getClassLoader());
        ILogging log = conf.getLogging();
        domainPropsRef.set(ILogging.class.getName(), log);

        logging.addNode(new Property("logName", fullName));
        logging.addNode(new Property("chip-config-file", chipconfile));
        element.addNode(logging);
    }

    private void parseGlobal(JsonObject e) {
        JsonElement g = e.get("global");
        if (g == null)
            return;
        IElement global = new Element("global");
        JsonObject jobj = g.getAsJsonObject();
        String defaultLanguage = (jobj.get("default") == null) ? "" : jobj.get(
                "default").getAsString();
        global.addNode(new Property("default", defaultLanguage));
        String rp = CJSystem.current().environment()
                .getProperty("chip-repository");
        global.addNode(new Property("chip-repository", rp));
        global.addNode(new Property("desc", (jobj.get("desc") == null) ? ""
                : jobj.get("desc").getAsString()));
        ILanguage lang = new Language(rp, defaultLanguage);
        String dc = CJSystem.current().environment().language().getLocaleCode();
        lang.scan(dc, resource.getClassLoader());
        domainPropsRef.set(ILanguage.class.getName(), lang);

        element.addNode(global);
    }

    /*
     * (non-Javadoc)
     *
     * @see cj.studio.ecm.IAssemblyContext#getResource()
     */
    @Override
    public IResource getResource() {
        return this.resource;
    }

    /*
     * (non-Javadoc)
     *
     * @see cj.studio.ecm.IAssemblyContext#getElement()
     */
    @Override
    public IElement getElement() {
        return element;
    }

    @Override
    public IElement getJss() {
        IElement scNode = ((IElement) element.getNode("serviceContainer"));
        INode sNode = ((IElement) scNode).getNode("jss");
        return (IElement) sNode;
    }

    @Override
    public String serviceContainerMonitors() {
        IElement scNode = ((IElement) element.getNode("serviceContainer"));
        Property sNode = (Property) scNode.getNode("monitors");
        if (sNode == null) {
            return null;
        }
        return sNode.getValue().getName();
    }

    @Override
    public String switchFilter() {
        IElement scNode = ((IElement) element.getNode("serviceContainer"));
        Property sNode = (Property) scNode.getNode("switchFilter");

        return sNode.getValue().getName();
    }

    /*
     * (non-Javadoc)
     *
     * @see cj.studio.ecm.IAssemblyContext#getScans()
     */
    @Override
    public IElement[] getScans() {
        IElement scNode = ((IElement) element.getNode("serviceContainer"));
        INode sNode = ((IElement) scNode).getNode("scans");
        List<IElement> list = new ArrayList<IElement>();
        IElement sEle = ((IElement) sNode);
        String[] names = sEle.enumNodeNames();
        for (String name : names) {
            IElement obje = (IElement) sEle.getNode(name);
            list.add(obje);
        }
        IElement[] arr = new IElement[0];
        return list.toArray(arr);
    }
    // public String getClassLoaderName() {
    // IElement scNode = ((IElement) element.getNode("serviceContainer"));
    // Property sNode = (Property) scNode.getNode("classloader");
    // return sNode.getValue().getName();
    // }
}
