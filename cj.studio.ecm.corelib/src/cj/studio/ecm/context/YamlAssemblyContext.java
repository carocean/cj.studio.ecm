package cj.studio.ecm.context;

import cj.studio.ecm.CJSystem;
import cj.studio.ecm.EcmException;
import cj.studio.ecm.Environment;
import cj.studio.ecm.IAssemblyContext;
import cj.studio.ecm.domain.ChipDomain;
import cj.studio.ecm.global.ILanguage;
import cj.studio.ecm.global.Language;
import cj.studio.ecm.logging.ILogging;
import cj.studio.ecm.logging.LoggingConfigurator;
import cj.studio.ecm.resource.IResource;
import cj.studio.ecm.script.JssModuleNodeParser;
import cj.studio.ecm.util.ObjectHelper;
import cj.studio.ecm.util.ObjectmMedium;
import cj.ultimate.util.StringUtil;

import java.io.File;
import java.io.InputStream;
import java.util.*;

public class YamlAssemblyContext implements IAssemblyContext {
    private IResource resource;
    private IElement element;
    protected ObjectmMedium domainPropsRef;
    protected Properties props;

    public YamlAssemblyContext(IResource resource, Map<String, Object> node, Properties properties) {
        this.resource = resource;
        this.props = properties;
        try {
            domainPropsRef = (ObjectmMedium) ObjectHelper.get(resource,
                    "domainPropsRef");
        } catch (Exception e) {
            throw new EcmException(e);
        }
        this.parse(node);
    }

    private void parse(Map<String, Object> node) {
        if (node == null)
            throw new RuntimeException("格式错误");
        element = new Element("assemblyContext");
        this.parseAssemblyInfo((Map<String, Object>) node.get("assemblyInfo"));
        this.parseEntryPoint((Map<String, Object>) node.get("entryPoint"));
        this.parseServiceContainer((Map<String, Object>) node.get("serviceContainer"));
        this.parseGlobal((Map<String, Object>) node.get("global"));
        this.parseLogging();
    }

    private void parseAssemblyInfo(Map<String, Object> node) {
        if (node == null) {
            throw new EcmException("缺少assemblyInfo定义");
        }
        IElement infoObj = new Element("assemblyInfo");
        infoObj.addNode(new Property("assemblyResource", new Node(node.get(
                "assemblyResource") == null ? "" : (String) node.get(
                "assemblyResource"))));
        infoObj.addNode(new Property("assemblyTitle", new Node(node.get(
                "assemblyTitle") == null ? "" : (String) node.get(
                "assemblyTitle"))));
        infoObj.addNode(new Property("assemblyDescription", new Node(node.get(
                "assemblyDescription") == null ? "" : (String) node.get(
                "assemblyDescription"))));
        infoObj.addNode(new Property("assemblyConfiguration", new Node(node.get(
                "assemblyConfiguration") == null ? "" : (String) node.get(
                "assemblyConfiguration"))));
        infoObj.addNode(new Property("assemblyCompany", new Node(node.get(
                "assemblyCompany") == null ? "" : (String) node.get(
                "assemblyCompany"))));
        infoObj.addNode(new Property("assemblyProduct", new Node(node.get(
                "assemblyProduct") == null ? "" : (String) node.get(
                "assemblyProduct"))));
        infoObj.addNode(new Property("assemblyCopyright", new Node(node.get(
                "assemblyCopyright") == null ? "" : (String) node.get(
                "assemblyCopyright"))));
        infoObj.addNode(new Property("assemblyTrademark", new Node(node.get(
                "assemblyTrademark") == null ? "" : (String) node.get(
                "assemblyTrademark"))));
        infoObj.addNode(new Property("assemblyCulture", new Node(node.get(
                "assemblyCulture") == null ? "" : (String) node.get(
                "assemblyCulture"))));
        infoObj.addNode(new Property("guid", new Node(node.get(
                "guid") == null ? "" : (String) node.get(
                "guid"))));
        infoObj.addNode(new Property("assemblyIcon", new Node(node.get(
                "assemblyIcon") == null ? "" : (String) node.get(
                "assemblyIcon"))));
        infoObj.addNode(new Property("assemblyVersion", new Node(node.get(
                "assemblyVersion") == null ? "" : (String) node.get(
                "assemblyVersion"))));
        infoObj.addNode(new Property("assemblyFileVersion", new Node(node.get(
                "assemblyFileVersion") == null ? "" : (String) node.get(
                "assemblyFileVersion"))));
        infoObj.addNode(new Property("assemblyDeveloperHome", new Node(node.get(
                "assemblyDeveloperHome") == null ? "" : (String) node.get(
                "assemblyDeveloperHome"))));

        this.element.addNode(infoObj);
        this.domainPropsRef.set(ChipDomain.CHIP_INFO_KEY, infoObj);
    }

    private void parseEntryPoint(Map<String, Object> node) {
        if (node == null)
            throw new RuntimeException("入口点未定义。");
        IElement entrypoint = new Element("entryPoint");
        //plugins
        List<Map<String, Object>> pluginsList = node.get("plugins") == null ? new ArrayList<>() : (List<Map<String, Object>>) node.get(
                "plugins");
        IElement plugins = new Element("plugins");
        for (int i = 0; i < pluginsList.size(); i++) {
            Map<String, Object> plugin = pluginsList.get(i);
            IElement pluginObj = new Element("plugin_" + i);
            plugins.addNode(pluginObj);
            IProperty a = new Property("name", plugin.get("name") == null ? "" :
                    (String) plugin.get("name"));
            pluginObj.addNode(a);
            IProperty b = new Property("class", plugin.get("class") == null ? "" :
                    (String) plugin.get("class"));
            pluginObj.addNode(b);
            if (plugin.get("parameters") == null) {
                throw new EcmException(String.format("parameters of the activator %s  is null.", plugin.get("class")));
            }
            IElement c = new Element("parameters");
            Map<String, String> paramJo = (Map<String, String>) plugin.get("parameters");
            Iterator<Map.Entry<String, String>> it = paramJo.entrySet()
                    .iterator();
            while (it.hasNext()) {
                Map.Entry<String, String> entry = it.next();
                String value = paramJo.get(entry.getKey());
                INode param = new Property(entry.getKey(), value);
                c.addNode(param);
            }
            pluginObj.addNode(c);

        }
        entrypoint.addNode(plugins);
        // activators
        List<Map<String, Object>> jarr = node.get("activators") == null ? new ArrayList<>() : (List<Map<String, Object>>) node.get(
                "activators");
        IElement activators = new Element("activators");
        for (int i = 0; i < jarr.size(); i++) {
            Map<String, Object> activator = jarr.get(i);
            IElement activObj = new Element("activator_" + i);
            activators.addNode(activObj);
            IProperty a = new Property("name", activator.get("name") == null ? "" : (String) activator.get("name"));
            activObj.addNode(a);
            IProperty b = new Property("class", activator.get("class") == null ? "" : (String) activator.get("class"));
            activObj.addNode(b);
            if (activator.get("parameters") == null) {
                throw new EcmException(String.format("parameters of the activator %s  is null.", activator.get("class")));
            }
            IElement c = new Element("parameters");
            Map<String, String> paramList = (Map<String, String>) activator.get("parameters");
            Iterator<Map.Entry<String, String>> it = paramList.entrySet()
                    .iterator();
            while (it.hasNext()) {
                Map.Entry<String, String> entry = it.next();
                String value = paramList.get(entry.getKey());
                INode param = new Property(entry.getKey(), value);
                c.addNode(param);
            }
            activObj.addNode(c);

        }
        entrypoint.addNode(activators);
        element.addNode(entrypoint);
    }


    private void parseServiceContainer(Map<String, Object> node) {
        if (node == null)
            throw new RuntimeException("服务容器未定义。");
        IElement containerObj = new Element("serviceContainer");
        containerObj.addNode(new Property("name", node.get("name") == null ? "" : (String) node.get("name")));
        // containerObj.addNode(new Property("classloader", ElementGet
        // .getJsonProp(container.get("classloader"))));
        Object swithchFilter = node.get("switchFilter");
        if (swithchFilter == null) {
            swithchFilter = "off";
        } else {
            if (swithchFilter instanceof Boolean) {
                swithchFilter = (boolean) swithchFilter ? "on" : "off";
            } else {
                swithchFilter = (String) swithchFilter;
            }
        }
        containerObj.addNode(new Property("switchFilter", (String) swithchFilter));
        String monitors = "";
        if (node
                .get("monitors") == null) {
            monitors = node
                    .get("monitor") == null ? "" : (String) node
                    .get("monitor");
        } else {
            monitors = (String) node.get(
                    "monitors");
        }
        containerObj.addNode(new Property("monitors", monitors));

        List<Map<String, Object>> jss = node.get("jss") == null ? new ArrayList<>() : (List<Map<String, Object>>) node.get(
                "jss");
        IElement jssArr = new Element("jss");
        IElementParser parser = new JssModuleNodeParser();
        jssArr.parse(jss, parser);
        containerObj.addNode(jssArr);

        List<Map<String, Object>> scans = (List<Map<String, Object>>) node.get("scans");
        IElement scansArr = new Element("scans");
        containerObj.addNode(scansArr);
        for (int i = 0; i < scans.size(); i++) {
            Map<String, Object> obj = scans.get(i);
            IElement scanObj = new Element("scan_" + i);
            scansArr.addNode(scanObj);
            scanObj.addNode(new Property("package", obj.get("package") == null ? "" : (String) obj.get("package")));
            scanObj.addNode(new Property("extName", obj.get("extName") == null ? "" : (String) obj.get("extName")));
            scanObj.addNode(new Property("exoterical", obj.get("exoterical") == null ? "false" : obj.get(
                    "exoterical")+""));
        }
        this.element.addNode(containerObj);
    }

    private void parseGlobal(Map<String, Object> node) {
        if (node == null) {
            throw new RuntimeException("global未定义。");
        }
        IElement global = new Element("global");
        String defaultLanguage = (node.get("default") == null) ? "" : (String) node.get(
                "default");
        global.addNode(new Property("default", defaultLanguage));
        String rp = CJSystem.current().environment()
                .getProperty("chip-repository");
        global.addNode(new Property("chip-repository", rp));
        global.addNode(new Property("desc", (node.get("desc") == null) ? ""
                : (String) node.get("desc")));
        ILanguage lang = new Language(rp, defaultLanguage);
        String dc = CJSystem.current().environment().language().getLocaleCode();
        lang.scan(dc, resource.getClassLoader());
        domainPropsRef.set(ILanguage.class.getName(), lang);

        element.addNode(global);
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

}
