package cj.studio.ecm.script;

import java.io.File;
import java.io.IOException;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.regex.Pattern;

import org.apache.log4j.Logger;

import cj.studio.ecm.EcmException;
import cj.studio.ecm.IAssemblyContext;
import cj.studio.ecm.IChip;
import cj.studio.ecm.IChipInfo;
import cj.studio.ecm.IServiceInstanceFactory;
import cj.studio.ecm.IServiceNameGenerator;
import cj.studio.ecm.IServiceProvider;
import cj.studio.ecm.Scope;
import cj.studio.ecm.ServiceCollection;
import cj.studio.ecm.adapter.AdapterFactory;
import cj.studio.ecm.adapter.IAdaptable;
import cj.studio.ecm.adapter.ICommand;
import cj.studio.ecm.container.factory.FactoryType;
import cj.studio.ecm.context.IElement;
import cj.studio.ecm.context.IModuleContext;
import cj.ultimate.util.StringUtil;
import cj.ultimate.util.UnzipUtil;
import jdk.nashorn.api.scripting.ScriptObjectMirror;
import jdk.nashorn.internal.runtime.Undefined;

/**
 * jss服务工厂
 *
 * <pre>
 * jss服务不支持name属性，而是采用文件相对路径名作为其名(格式：xxx.xxx.xxx)
 * ，原因是采用java类文件与名的结构（同一性），因此更容易找到服务文件。
 *
 * 1.如果指定为预装载，则在刷新时扫描
 * 2.在获取服务时，如果服务在缓冲，且其scope不是runtime，则返回singlon实例，由muti实例生成新实例。
 * 3.如果服务不在缓冲，则装载它。
 * 默认为runtime模式
 * 1.如果是singleon模式，则由容器缓冲，js文件只在容器加载时加载并执行一次
 * 2.multi例模式，每次由容器创建jss服务实例，js文件只在容器加载时加载并执行一次
 * 3.runtime模式，每次jss服务实例的创建，均加载并执行一遍js文件。
 *
 *
 * 域：
 * 1.芯片域：设site,chipinfo(简为chip)
 * 2.imports域：一个jss对象内可见，用于输入.
 *   imports域与exports对象配对形成对一个jss对象的输入输出。
 *
 * </pre>
 * <p>
 * 技术限制：由于一个eval只能绑定一个Bindings，因此无法实现模块域和imports域，要共享就不能私有，要私有就共享不了，
 * 而exports概念上是导出之意， 不希望通过它输入。因此去掉了模块域概念。
 *
 * @author carocean
 */
// 两个固定的模块为http.jss,ws.jss，其它的是自定义服务.
// 可以根据serviceid的通配符返回所有jss模块的服务
public class JssServiceInstanceFactory implements IServiceInstanceFactory {

    private Map<String, IJssDefinition> registry;// key=jss服务名,$.cj.jss.services1.dept.Test
    private Map<String, IJssModule> modules;// key=moduleName，包括固定模块http.jss,ws.jss
    // services只缓冲singleon的服务，多例和运行时服务不缓冲。
    private Map<String, Object> services;// key=jss服务名,$.cj.jss.services1.dept.Test
    // private Map<String,Bindings>
    // moduleDomain;//key=moduleName，由于一个eval只能绑定一个Bindings，因此无法实现模块域和imports域，因此去掉了模块域概念。
    private IScriptContainer container;
    private String chipHome;
    // 之前的注释： 为了防止在刷新前请求jss服务，因此0表示初始化，1表示由getService刷新了工厂，2表示refresh方法被执行过了。
    // 改后的注释：如果为true表示正在刷新或已刷新实例工厂
    private boolean refreshCtr;
    private IServiceProvider parent;
    private boolean switchFilter;// 开关
    private static Logger log = Logger.getLogger(JssServiceInstanceFactory.class);

    public JssServiceInstanceFactory() {
    }

    @Override
    public void dispose() {
        if (registry != null) {
            registry.clear();
        }
        container = null;
        if (modules != null)
            modules.clear();
        if (services != null)
            services.clear();
    }

    @Override
    public FactoryType getType() {
        return FactoryType.jss;
    }

    @Override
    public String getDefinitionNameOfInstance(String instanceName) {
        return registry.get(instanceName).selectName();
    }

    // 当jss过滤器配置了通过类型拦载的方式时
    @SuppressWarnings("unchecked")
    @Override
    public <T> ServiceCollection<T> getServices(Class<T> serviceClazz) {
        if (!refreshCtr) {
            refresh();// 由于实例工厂的加类jss工厂在最后，在其它实例工厂请求注入时，该工厂可能还没刷新，因此在此判断并刷新。
        }
        if (serviceClazz.isAssignableFrom(ScriptObjectMirror.class)) {
            List<T> list = new ArrayList<T>();
            list.addAll((Collection<? extends T>) services.values());
            return new ServiceCollection<T>(list);
        }
        try {
            List<T> list = getJssInterface(serviceClazz.getName());
//			list.addAll((Collection<? extends T>) services.values());
            return new ServiceCollection<T>(list);
        } catch (ClassNotFoundException e) {
            throw new EcmException(e);
        }
    }

    private <T> List<T> getJssInterface(String className) throws ClassNotFoundException {
        List<T> list = new ArrayList<T>();
        Set<String> set = this.registry.keySet();
        for (String key : set) {
            IJssDefinition def = registry.get(key);
            String type = def.getDecriber().extendsType;
            if (!className.equals(type))
                continue;

            @SuppressWarnings("unchecked")
            Class<T> clazz = (Class<T>) container.classloader().loadClass(type);
            if (clazz == null)
                return list;
            if (!clazz.isInterface())
                throw new EcmException(String.format("不是接口。%s", type));
            @SuppressWarnings("unchecked")
            T inst = (T) getService(key);
            if (inst == null)
                return list;
//			ScriptObjectMirror m=(ScriptObjectMirror)inst;
//			m.put("head", def.getHead());
            if (!(inst instanceof ScriptObjectMirror)) {
                list.add(inst);
                continue;
            }
            T obj = container.getInterface(inst, clazz);
            if (obj != null)
                list.add(obj);
        }
        return list;
    }

    private String[] matchFilter(String serviceId) {
        Set<String> set = registry.keySet();

        for (String key : set) {
            IJssDefinition def = registry.get(key);
            JssDecriber dec = def.getDecriber();
            if (dec.filterInvalid || StringUtil.isEmpty(dec.interrupter))
                continue;
            Pattern p = Pattern.compile(dec.filterMatchId);
            if (p.matcher(serviceId).matches()) {
                return new String[]{def.selectName(), dec.interrupter};
            }
        }
        return null;
    }

    public final static String JSS_REQUEST_JAVA_SERVICEKEY = "$.cj.jss.B89D14E135D64707A047AE093FA7F34E";

    // 在此实现对java服务的拦截，因为jss服务优先，见：serviceInstanceFactory实现。
    // 请求id格式：$.cj.jss.模块名.相对jss服务名（如：$.cj.jss.service1.test.dept，test.dept即是相对于模块service1的相对服务名)
    @Override
    public Object getService(String serviceId) {
        // 由于下面将请求java服务会导致死循环，故此以该id约定为下面代码请求转发java服务的id
        if (serviceId.startsWith(JSS_REQUEST_JAVA_SERVICEKEY)) {
            return null;
        }

        if (!refreshCtr) {
            refresh();// 由于实例工厂的加类jss工厂在最后，在其它实例工厂请求注入时，该工厂可能还没刷新，因此在此判断并刷新。
        }
        /*以下采用排除法处理请求。*/
        //如果是jss请求则直接getJssService
        if (serviceId.startsWith("$.cj.jss.")) {
            return getJssService(serviceId);
        }
        //下面的就剩是java服务请求了，如果系统没有开启jss过滤器模式则直接返回null,因为这是jss工厂，返回null会被java服务工厂处理
        if (!switchFilter) {
            return null;
        }
        String[] filter = matchFilter(serviceId);
        //如果针对该java服务请求没有拦截器则返回null
        if (filter == null || filter.length == 0) {
            return null;
        }
        //处理有java拦截器的服务请求。
        String javaReqId = String.format("%s.%s", JSS_REQUEST_JAVA_SERVICEKEY, serviceId);
        Object proxy = parent.getService(javaReqId);// java服务
        if (proxy == null)
            return null;
        Object jss = getJssService(filter[0]);// jss执行器
        if (jss == null)
            return null;

        ICommand cmd = new ProxyCommand(proxy, (ScriptObjectMirror) jss, filter[1]);
        IAdaptable adapater = AdapterFactory.createAdaptable(cmd);
        Object a = adapater.getAdapter(proxy.getClass());
        return a;
    }

    protected Object getJssService(String serviceId) {
        if (services.containsKey(serviceId)) {
            return services.get(serviceId);
        }
        String[] uri = IJssDefinition.parseReqServiceId(serviceId);
        if (!modules.containsKey(uri[0])) {
            return null;
        }
        IJssModule mod = modules.get(uri[0]);
        if (StringUtil.isEmpty(uri[1])) {// 如果请求的是模块

            return mod.moduleScriptObj(container);
        }

        if (mod.unzip() && !mod.isUnziped()) {
            mod.doUnzip();
        }
        if (registry.containsKey(serviceId)) {
            IJssDefinition jssDef = registry.get(serviceId);
            if (!jssDef.ownerModule().equals(uri[0])) {
                throw new RuntimeException(String.format("jss服务定义中模块不匹配。%s!=%s", uri[0], jssDef.ownerModule()));
            }
            Object som = null;
            switch (jssDef.getDecriber().scope()) {
                case singleon:
                    som = mod.loadJss(jssDef, this.container);
                    if (som == null)
                        return null;
                    services.put(jssDef.selectName(), som);
                    break;
                case multiton:
                    som = mod.newJssInstance(jssDef, this.container);
                    if (som == null)
                        return null;
                    break;
                case runtime:
                    som = mod.loadJss(jssDef, this.container);
                    if (som == null)
                        return null;
                    break;
            }
            return som;
        } else {
            IJssModuleCallback cb = new JssModuleCallback();
            Object[] jss = mod.loadJss(uri[1], container, cb, true);
            if (jss.length == 0)
                return null;
            IJssDefinition def = (IJssDefinition) jss[0];
            Object som = jss[1];
            if (som != null && def.getDecriber().scope() == Scope.singleon) {
                services.put(def.selectName(), som);
            }
            return jss[1];
        }
    }

    @Override
    public void initialize(IModuleContext ctx, IServiceNameGenerator serviceNameGenerator) {
        this.parent = ctx;
        IAssemblyContext context = ctx.getAssemblyContext();
        this.container = ctx.getScriptContainer();
        this.container.init();
        // ＊＊＊＊＊＊构建芯片域
        IChip chip = (IChip) ctx.getService(IChip.class.getName());
        IChipInfo chipinfo = chip.info();
        container.global().put("chip", chip);
        // ****end
        switchFilter = "on".equals(context.switchFilter());
        chipHome = context.getProperty("home.dir");
        this.modules = new HashMap<String, IJssModule>(4);
        this.registry = new HashMap<String, IJssDefinition>(4);
        this.services = new HashMap<String, Object>(4);

        IElement jss = context.getJss();
        IJssModule httpModule = null;
        IJssModule wsModule = null;
        if (chipinfo.isWebChip()) {
            // 加固定模块
            String sitejss_pages_home = chipinfo.getResourceProp("http.jss");
            String sitejss_widgets_home = chipinfo.getResourceProp("ws.jss");
            String jssPagesHome = sitejss_pages_home.startsWith(File.separator)
                    ? sitejss_pages_home.substring(1, sitejss_pages_home.length())
                    : sitejss_pages_home;
            String jssWsHome = sitejss_widgets_home.startsWith(File.separator)
                    ? sitejss_widgets_home.substring(1, sitejss_widgets_home.length())
                    : sitejss_widgets_home;
            httpModule = IJssModule.create(parent, context.getResource(), IJssModule.FIXED_MODULENAME_HTTP_JSS,
                    ".jss.js", jssPagesHome.replace(File.separator, "."), true, "link", chipHome, true);
            wsModule = IJssModule.create(parent, context.getResource(), IJssModule.FIXED_MODULENAME_WS_JSS, ".jss.js",
                    jssWsHome.replace(File.separator, "."), true, "link", chipHome, true);
            this.modules.put(httpModule.name(), httpModule);
            this.modules.put(wsModule.name(), wsModule);
            // 解压site
            String site = chipinfo.getResourceProp("site");
            if (!sitejss_pages_home.startsWith(site)) {// http一定是解压、link方式
                httpModule.doUnzip();
            }
            if (!sitejss_widgets_home.startsWith(site)) {
                wsModule.doUnzip();
            }
            log.info(String.format("发现web芯片%s-%s，尝试解压", chipinfo.getName(), chipinfo.getVersion()));
            String outFolder = String.format("%s/%s/%s", chipHome, IJssModule.RUNTIME_SITE_DIR, site);
            outFolder = outFolder.replace("///", File.separator).replace("//", File.separator).replace("/",
                    File.separator);
            String sitepath = site.replace(File.separator, "/");
            if (sitepath.startsWith("/") || sitepath.startsWith("\\")) {
                sitepath = sitepath.substring(1, sitepath.length());
            }
            try {
                UnzipUtil.unzip(context.getResource().getResourcefile(), sitepath, outFolder);
                httpModule.setUnziped(true);
                wsModule.setUnziped(true);
                log.info(String.format("解压web芯片%s-%s，到：\r\n%s", chipinfo.getName(), chipinfo.getVersion(), outFolder));
            } catch (IOException e) {
                throw new EcmException(e);
            }
        }
        if (jss != null) {
            @SuppressWarnings("unchecked")
            List<IJssModule> cust = (List<IJssModule>) jss.parser().getEntity();
            // 装载自定义模块
            for (IJssModule m : cust) {
                if (httpModule != null && StringUtil.isPackageConflict(httpModule.pack(), m.pack())) {
                    throw new EcmException(String.format("jss与系统模块包充突.module%s:%s", m.name(), m.pack()));
                }
                if (wsModule != null && StringUtil.isPackageConflict(wsModule.pack(), m.pack())) {
                    throw new EcmException(String.format("jss与系统模块包充突.module%s:%s", m.name(), m.pack()));
                }
                if (context.getResource().getResourceAsStream(m.pack().replace(".", "/")) == null) {
                    log.info(String.format("模块%s的包%s不存在，或为空包。", m.name(), m.pack()));
                    continue;
                }
                m.chipHome(chipHome);
                m.resource(context.getResource());
                m.serviceProvider(parent);
                if (m.unzip() && !m.isUnziped()) {
                    m.doUnzip();
                }
                modules.put(m.name(), m);

            }
        }
    }

    @Override
    public void refresh() {
        if (refreshCtr)
            return;
        refreshCtr = true;
        registry.clear();
        services.clear();
        IJssModuleCallback cb = new JssModuleCallback();
        for (IJssModule m : modules.values()) {
            if (m.unzip() && !m.isUnziped()) {
                m.doUnzip();
            }

            m.scans(container, cb);
        }
    }

    class JssModuleCallback implements IJssModuleCallback {
        @Override
        public void newJss(IJssDefinition def, ScriptObjectMirror m) {
//			if (registry.containsKey(def.selectName())) {
//				throw new EcmException(String.format("jjs注册表中已存在服务%s定义。",
//						def.selectName()));
//			}
            registry.put(def.selectName(), def);
            if (m != null && def.getDecriber() != null && def.getDecriber().scope() == Scope.singleon) {
                services.put(def.selectName(), m);
            }
        }
    }

    class ProxyCommand implements ICommand {
        Object proxy;
        ScriptObjectMirror jssCommand;
        String filter;

        public ProxyCommand(Object proxy, ScriptObjectMirror jssCommand, String filter) {
            this.proxy = proxy;
            this.jssCommand = jssCommand;
            this.filter = filter;
        }

        @Override
        public Object exeCommand(Object adapter, String method, Class<?>[] argtypes, Object[] args) {
            if (jssCommand.hasMember(filter)) {
                Method m = findMethod(method, argtypes, proxy.getClass());
                if (m == null) {
                    throw new EcmException(String.format("NoSuchMethod %s.%s", adapter.getClass(), method));
                }
                Object ret = jssCommand.callMember(filter, proxy, m, args);
                if (ret instanceof Undefined) {
                    ret = null;
                }
                return ret;
            }
            return null;
        }

        private Method findMethod(String name, Class<?>[] argTypes, Class<?> clazz) {
            Method m = null;
            try {
                m = clazz.getDeclaredMethod(name, argTypes);
            } catch (NoSuchMethodException e) {
                if (!Object.class.equals(clazz)) {
                    Class<?> superC = clazz.getSuperclass();
                    m = findMethod(name, argTypes, superC);
                }
            }
            return m;
        }
    }
}
