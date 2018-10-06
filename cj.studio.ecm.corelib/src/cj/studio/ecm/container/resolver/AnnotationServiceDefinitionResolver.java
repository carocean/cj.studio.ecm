package cj.studio.ecm.container.resolver;

import java.lang.annotation.Annotation;
import java.lang.reflect.Constructor;
import java.lang.reflect.Field;
import java.lang.reflect.Method;

import cj.studio.ecm.IServiceDefinition;
import cj.studio.ecm.annotation.CjBridge;
import cj.studio.ecm.annotation.CjExotericalType;
import cj.studio.ecm.annotation.CjJoinpoint;
import cj.studio.ecm.annotation.CjMethod;
import cj.studio.ecm.annotation.CjMethodArg;
import cj.studio.ecm.annotation.CjPropertyValue;
import cj.studio.ecm.annotation.CjService;
import cj.studio.ecm.annotation.CjServiceInvertInjection;
import cj.studio.ecm.annotation.CjServiceRef;
import cj.studio.ecm.annotation.CjServiceSite;
import cj.studio.ecm.annotation.MethodMode;
import cj.studio.ecm.bridge.UseBridgeMode;
import cj.studio.ecm.container.describer.BridgeDescriber;
import cj.studio.ecm.container.describer.BridgeJoinpoint;
import cj.studio.ecm.container.describer.ExotericalTypeDescriber;
import cj.studio.ecm.container.describer.ParametersMehtodDescriber;
import cj.studio.ecm.container.describer.ReturnMehtodDescriber;
import cj.studio.ecm.container.describer.ServiceDescriber;
import cj.studio.ecm.container.describer.ServiceInvertInjectionDescriber;
import cj.studio.ecm.container.describer.ServiceMethod;
import cj.studio.ecm.container.describer.ServiceProperty;
import cj.studio.ecm.container.describer.ServicePropertyValueDescriber;
import cj.studio.ecm.container.describer.ServiceRefDescriber;
import cj.studio.ecm.container.describer.ServiceSiteDescriber;
import cj.studio.ecm.container.registry.AnnotationServiceDefinition;
import cj.studio.ecm.resource.IResource;
import cj.studio.ecm.resource.SystemResource;
import cj.ultimate.IDisposable;
import cj.ultimate.util.StringUtil;

public class AnnotationServiceDefinitionResolver extends
		AbstractServiceDefinitionResolver implements IDisposable {
	// 此为解析的镜像的定义，镜像为接口，解析接口。
	// 采用懒算法，如果在解析的接口中不存在定义，则在解析服务类型时解析其实现的镜像接口，并将之放到此集合，以留给后续程序使用。
	private AnnotationResource annoResource;

	public AnnotationServiceDefinitionResolver() {
	}

	@Override
	public IServiceDefinition resolve(String resItem, IResource resource) {
		if (!resItem.endsWith(".class"))
			return null;
		if (annoResource == null) {
			if (resource instanceof SystemResource) {
				//此处替换当前资源，为注解提供临时的资源
				SystemResource sr = (SystemResource) resource;
				annoResource = new AnnotationResource(sr.getParent());
				sr.copyTo(annoResource);
			}
		}
		String fullName = resItem.substring(0, resItem.indexOf(".class"));
		fullName = fullName.replace("/", ".");
		try {
			Class<?> clazz = annoResource.loadClass(fullName);
			if(clazz==null)throw new ClassNotFoundException(fullName);
			IServiceDefinition def = this.resolve(clazz);
			return def;
		} catch (ClassNotFoundException e) {
			throw new RuntimeException(e);
		}
	}

	/**
	 * 解析服务注解的核心实现。
	 * 
	 * @param clazz
	 * @return
	 */
	protected IServiceDefinition resolve(Class<?> clazz) {
		IServiceDefinition def = new AnnotationServiceDefinition();
		// 如果是开放类型则记录
		CjExotericalType et = clazz.getAnnotation(CjExotericalType.class);
		if (et != null) {
			ExotericalTypeDescriber etd = new ExotericalTypeDescriber();
			String name = clazz.getName();
			etd.setExotericalTypeName(name);
			if (clazz.isInterface() && name.endsWith(".package-info")) {
				etd.setPackage(true);
				etd.setExotericalTypeName(name.replace(".package-info", ""));
			}
			def.getExtraDescribers().add(etd);
			int form = def.getServiceDescribeForm()
					| IServiceDefinition.EXOTERICALTYPE_DESCRIBEFORM;
			def.setServiceDescribeForm((byte) form);
		}
		if (clazz.isInterface()) {
			// mirror,mirroring的.class的解析结果不返回，即不被注入到注册表。
			// 它只是用于组装服务定义
			return def;
		} else {
			CjService cjservice = clazz.getAnnotation(CjService.class);
			if (cjservice != null)
				this.resolveService(clazz, def);
			CjBridge bridge=clazz.getAnnotation(CjBridge.class);
			if(bridge!=null){
				this.resolveBridge(clazz, def);
			}
			return def;
		}
	}

	private void resolveService(Class<?> clazz, IServiceDefinition def) {
		CjService cjservice = clazz.getAnnotation(CjService.class);
		ServiceDescriber sd = new ServiceDescriber();
		def.setServiceDescriber(sd);
		// 采用按位累加的计算方式，增加一种组合注解类型，可以通过“按位或”的方式为服务增加注解类型
		int form = def.getServiceDescribeForm()
				| IServiceDefinition.SERVICE_DESCRIBEFORM;
		def.setServiceDescribeForm((byte) form);
		sd.setClassName(clazz.getName());
		sd.setScope(cjservice.scope());
		sd.setServiceId(cjservice.name());
		sd.setConstructor(cjservice.constructor());
		sd.setExoteric(cjservice.isExoteric());
		this.resolveFields(clazz, def);
		this.resolveMethods(clazz, def);
		this.resolveConstructors(clazz, def);
	}

	private void resolveFields(Class<?> clazz, IServiceDefinition def) {
		Field[] fields = clazz.getDeclaredFields();
		for (Field f : fields) {
			ServiceProperty sp = new ServiceProperty();
			// 有几类属性的修饰就解析几个
			CjServiceRef sr = f.getAnnotation(CjServiceRef.class);
			if (sr != null) {
				int form = sp.getPropDescribeForm()
						| ServiceProperty.SERVICEREF_DESCRIBEFORM;
				sp.setPropDescribeForm((byte) form);
				sp.setPropName(f.getName());
				ServiceRefDescriber srd = new ServiceRefDescriber();
				srd.setRefByName(sr.refByName());
				srd.setRefByMethod(sr.refByMethod());
				srd.setUseBridge(sr.useBridge());
				srd.setRefByType(sr.refByType() == null ? null : sr.refByType()
						.getName());
				if (sr.useBridge()!=UseBridgeMode.forbidden) {
					CjJoinpoint jp=f.getAnnotation(CjJoinpoint.class);
					if( jp!= null ) {
						String aspects = jp.aspects();
						BridgeJoinpoint bjp = new BridgeJoinpoint();
						bjp.setAspects(aspects);
						srd.setBridgeJoinpoint(bjp);
					}

				}
				sp.getPropertyDescribers().add(srd);
			}
			CjPropertyValue value=f.getAnnotation(CjPropertyValue.class);
			if(value!=null){
				if(StringUtil.isEmpty(value.value()))
					throw new RuntimeException(String.format("属性%s的PropertyValue注解的value不能为空",f.getName()));
				int form = sp.getPropDescribeForm()
						| ServiceProperty.VALUE_DESCRIBEFORM;
				sp.setPropDescribeForm((byte) form);
				sp.setPropName(f.getName());
				String parser = (value.parser());
				ServicePropertyValueDescriber pvd = new ServicePropertyValueDescriber();
				pvd.setValue(value.value());
				pvd.setParser(parser);
				sp.getPropertyDescribers().add(pvd);
			}
			CjServiceInvertInjection sii = f
					.getAnnotation(CjServiceInvertInjection.class);
			if (sii != null) {
				int form = sp.getPropDescribeForm()
						| ServiceProperty.SERVICEII_DESCRIBEFORM;
				sp.setPropDescribeForm((byte) form);
				sp.setPropName(f.getName());
				ServiceInvertInjectionDescriber srd = new ServiceInvertInjectionDescriber();
				srd.setForce(sii.isForce());
				sp.getPropertyDescribers().add(srd);
			}
			CjServiceSite ss = f.getAnnotation(CjServiceSite.class);
			if (ss != null) {
				int form = sp.getPropDescribeForm()
						| ServiceProperty.SERVICESITE_DESCRIBEFORM;
				sp.setPropDescribeForm((byte) form);
				sp.setPropName(f.getName());
				ServiceSiteDescriber srd = new ServiceSiteDescriber();
				sp.getPropertyDescribers().add(srd);
			}
			if (!StringUtil.isEmpty(sp.getPropName()))
				def.getProperties().add(sp);
		}
		Class<?> superClass = clazz.getSuperclass();
		if (superClass != Object.class) {
			this.resolveFields(superClass, def);
		}
	}
	private void resolveBridge(Class<?> clazz, IServiceDefinition def) {
		CjBridge bridge=clazz.getAnnotation(CjBridge.class);
		String aspects =bridge.aspects();
			BridgeDescriber bd = new BridgeDescriber();
			bd.setAspects(aspects);
			//只要bridge不为空就支持桥
			bd.setValid(true);
			def.getExtraDescribers().add(bd);
			int form = def.getServiceDescribeForm()
					| IServiceDefinition.BRIDGE_DESCRIBEFORM;
			def.setServiceDescribeForm((byte) form);
	}
	private void resolveConstructors(Class<?> clazz, IServiceDefinition def) {
		Constructor<?>[] arr= clazz.getConstructors();
		for (Constructor<?> m : arr) {
			CjMethod cjM=m.getAnnotation(CjMethod.class);
			if(cjM==null)continue;
			String alias = cjM.alias();
			String bind = "<init>";
			ServiceMethod sm = new ServiceMethod();
			ParametersMehtodDescriber md = new ParametersMehtodDescriber();
			sm.getMethodDescribers().add(md);
			int form = sm.getMethodDescribeForm()
					| ServiceMethod.PARAMETERS_METHOD_DESCRIBEFORM;
			sm.setMethodDescribeForm((byte) form);
			def.getMethods().add(sm);
			sm.setAlias(alias);
			sm.setBind(bind);
//			MethodMode mm=cjM.callMode()==null?MethodMode.ref:cjM.callMode();
//			if(mm!=MethodMode.ref){
//				throw new RuntimeException("构建函数必须声明为MethodMode.ref,因为它总是会被实例化，即可看成被其它类调用");
//			}
//			sm.setCallMode(mm);
			sm.setCallMode(MethodMode.ref);//构造只能被引用
			Class<?>[] argsType=m.getParameterTypes();
			String[] argsTypeStr=new String[argsType.length];
			for(int i=0;i<argsType.length;i++){
				argsTypeStr[i]=argsType[i].getName();
			}
			sm.setParameterTypeNames(argsTypeStr);
			Annotation[][] argAnnotations= m.getParameterAnnotations();
			for (int j = 0; j < argAnnotations.length; j++) {
				Annotation[] argAnnos=argAnnotations[j];
				if(argAnnos.length<1)continue;
				//方法参数的注解目前只支持一个
				CjMethodArg arg=null;
				for(Annotation a:argAnnos){
					if(a instanceof CjMethodArg)
						arg=(CjMethodArg)a;
				}
				if(arg==null)continue;
				String ref = arg.ref();
				String value = arg.value();
				String injectMode = "";
				String argConf = "";
				if (!StringUtil.isEmpty(value)) {
					argConf = value;
					injectMode = "value";
				}
				if (!StringUtil.isEmpty(ref)) {
					injectMode = "ref";
					argConf = ref;
				}
				md.put(new String[] { argConf }, new String[] { injectMode });
			}
		}
		Class<?> superClass = clazz.getSuperclass();
		if (superClass != Object.class) {
			this.resolveConstructors(superClass, def);
		}
	}
	private void resolveMethods(Class<?> clazz, IServiceDefinition def) {
		Method[] arr = clazz.getDeclaredMethods();
		for (Method m : arr) {
			CjMethod cjM=m.getAnnotation(CjMethod.class);
			if(cjM==null)continue;
			String alias = cjM.alias();
			String bind = m.getName();
			ServiceMethod sm = new ServiceMethod();
			ParametersMehtodDescriber md = new ParametersMehtodDescriber();
			sm.getMethodDescribers().add(md);
			int form = sm.getMethodDescribeForm()
					| ServiceMethod.PARAMETERS_METHOD_DESCRIBEFORM;
			sm.setMethodDescribeForm((byte) form);
			def.getMethods().add(sm);
			if(StringUtil.isEmpty(alias)){
				alias=bind;
			}
			sm.setAlias(alias);
			sm.setBind(bind);
//			sm.setCallMode(cjM.callMode()==null?MethodMode.both:cjM.callMode());//原因：此属性对开发者来讲容易造成混淆。且不必区分自调和远调
			sm.setCallMode(MethodMode.both);
			if((!StringUtil.isEmpty(cjM.returnDefinitionId())||(!StringUtil.isEmpty(cjM.returnDefinitionType())))){
				ReturnMehtodDescriber rmd = new ReturnMehtodDescriber();
				int f = sm.getMethodDescribeForm()
						| ServiceMethod.RETURN_METHOD_DESCRIBEFORM;
				sm.setMethodDescribeForm((byte) f);
				sm.getMethodDescribers().add(rmd);
				String id = cjM.returnDefinitionId();
				String type =".".equals(cjM.returnDefinitionType())?m.getReturnType().getName():cjM.returnDefinitionType();
				rmd.setByDefinitionId(id);
				rmd.setByDefinitionType(type);
			}
			Class<?>[] argsType=m.getParameterTypes();
			String[] argsTypeStr=new String[argsType.length];
			for(int i=0;i<argsType.length;i++){
				argsTypeStr[i]=argsType[i].getName();
			}
			sm.setParameterTypeNames(argsTypeStr);
			Annotation[][] argAnnotations= m.getParameterAnnotations();
			for (int j = 0; j < argAnnotations.length; j++) {
				Annotation[] argAnnos=argAnnotations[j];
				if(argAnnos.length<1)continue;
				//方法参数的注解目前只支持一个
				CjMethodArg arg=null;
				for(Annotation a:argAnnos){
					if(a instanceof CjMethodArg)
						arg=(CjMethodArg)a;
				}
				if(arg==null)continue;
				String ref = arg.ref();
				String value = arg.value();
				String injectMode = "";
				String argConf = "";
				if (!StringUtil.isEmpty(value)) {
					argConf = value;
					injectMode = "value";
				}
				if (!StringUtil.isEmpty(ref)) {
					injectMode = "ref";
					argConf = ref;
				}
				md.put(new String[] { argConf }, new String[] { injectMode });
			}
		}
		Class<?> superClass = clazz.getSuperclass();
		if (superClass != Object.class) {
			this.resolveMethods(superClass, def);
		}
	}
/*	private void resolveMethods(Class<?> clazz, IServiceDefinition def) {
		// 怎么与解析镜相中的不可见注解方法的解析综合考虑？
		Method[] arr = clazz.getDeclaredMethods();
		for (Method m : arr) {
			List<ServiceMethod> arrMeth = def.getMethods();
			ServiceMethod sm = null;
			for (ServiceMethod smeth : arrMeth) {
				if (smeth.matched(m)) {
					sm = smeth;
					break;
				}
			}
			// if(sm==null){
			// sm=new ServiceMethod();
			// def.getMethods().add(sm);
			// }
			// 以下用于方法修饰的解析。由于目前仅有CjUnEyeable注解用于方法，而此注解是通过镜相间接的被解析了
			// 所以就不必再解析此注解。但如果有新的方法注解类型，则需要在下面的代码中添加解析。
			// 上面是得到服务的方法，下面是解析方法除CjUnEyeable之外的注解
			// CjUnEyeable ue=m.getAnnotation(CjUnEyeable.class);
			// if(ue!=null){
			//
			// }
		}
		Class<?> superClass = clazz.getSuperclass();
		if (superClass != Object.class) {
			this.resolveMethods(superClass, def);
		}
	}*/





	@Override
	public void dispose() {
		if (this.annoResource != null) {
			this.annoResource.dispose();
			this.annoResource = null;
		}
	}

	@Override
	protected void finalize() throws Throwable {
		this.dispose();
		super.finalize();
	}
}
