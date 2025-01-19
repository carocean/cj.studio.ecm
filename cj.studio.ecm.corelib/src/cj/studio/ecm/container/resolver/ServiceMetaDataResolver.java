package cj.studio.ecm.container.resolver;

import java.lang.reflect.Constructor;
import java.lang.reflect.Executable;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.util.List;

import cj.studio.ecm.EcmException;
import cj.studio.ecm.IServiceDefinition;
import cj.studio.ecm.IServiceMetaData;
import cj.studio.ecm.IServiceMetaDataResolver;
import cj.studio.ecm.annotation.CjExotericalType;
import cj.studio.ecm.container.describer.*;
import cj.studio.ecm.container.registry.ServiceMetaData;
import cj.studio.ecm.resource.IResource;
import cj.ultimate.net.sf.cglib.proxy.MethodInterceptor;
import cj.ultimate.net.sf.cglib.proxy.MethodProxy;
import cj.ultimate.util.PrimitiveType;
import cj.ultimate.util.StringUtil;

public abstract class ServiceMetaDataResolver implements
        IServiceMetaDataResolver {
    // 检查当前服务类型如果声明了开放服务描述，那么其接口中是否包括有开放类型的声明,可以是类也可以是接口
    private boolean hasExoticType(IServiceDefinition def, Class<?> clazz) {
        Class<?>[] interArr = clazz.getInterfaces();
        Class<?> superC = clazz.getSuperclass();
        boolean found = false;
        for (Class<?> c : interArr) {
            CjExotericalType et = c.getAnnotation(CjExotericalType.class);
            if (et != null) {
                found = true;
                break;
            }
        }
        if (!found) {
            CjExotericalType et = superC.getAnnotation(CjExotericalType.class);
            if (et != null) {
                found = true;
            }
        }
        if (!found && superC != Object.class) {
            return hasExoticType(def, superC);
        } else {
            return found;
        }
    }

    @Override
    public IServiceMetaData resolve(IServiceDefinition def, IResource resource) {
        ServiceMetaData meta = null;
        String className = def.getServiceDescriber().getClassName();
        try {
            Class<?> clazz = resource.loadClass(className);
//			if (def.getServiceDescriber().isExoteric()) {
//				if (!hasExoticType(def, clazz)) {
//					throw new RuntimeException(String.format(
//							"服务%s声明为开放类型，但其实现接口或超类中不存在依赖的开放类型声明", def
//									.getServiceDescriber().getServiceId()));
//				}
//			}
            meta = new ServiceMetaData(clazz);
            List<ServiceProperty> props = def.getProperties();
            for (ServiceProperty p : props) {
                String name = p.getPropName();
                Field f = this.findProperty(name, clazz);
                if (f != null) {
                    meta.addServicePropMeta(p, f);
                    // meta的用法是先增加字段，再取出包装字段
                    FieldWrapper fw = meta.getServicePropMeta(p);
                    // 求引用的类型
                    if ((p.getPropDescribeForm() & ServiceProperty.SERVICEREF_DESCRIBEFORM) == ServiceProperty.SERVICEREF_DESCRIBEFORM) {
                        try {
                            for (PropertyDescriber pd : p
                                    .getPropertyDescribers()) {
                                if (pd instanceof ServiceRefDescriber) {
                                    ServiceRefDescriber srd = (ServiceRefDescriber) pd;
                                    String type = srd.getRefByType();
                                    if (!StringUtil.isEmpty(type) && !UncertainObject.class.getName().equals(type)) {
                                        Class<?> refType = Class.forName(type,
                                                false,
                                                resource.getClassLoader());
                                        fw.setRefType(refType);
                                        break;
                                    }
                                    if (StringUtil.isEmpty(srd.getRefByName())
                                            && (StringUtil.isEmpty(type) || UncertainObject.class.getName().equals(type))) {
                                        fw.setRefType(fw.getField().getType());
                                        break;
                                    }
                                    break;
                                }
                            }

                        } catch (ClassNotFoundException e) {
                            throw new RuntimeException(e);
                        }
                    }

                }
            }
            List<ServiceMethod> methods = def.getMethods();
            for (ServiceMethod sm : methods) {
                String name = sm.getBind();
                String[] args = sm.getParameterTypeNames();
                Executable m = null;
                if ("<init>".equals(name)) {
                    m = this.findContructor(args, clazz,
                            resource.getClassLoader());
                } else {
                    m = this.findMethod(name, args, clazz,
                            resource.getClassLoader());
                }
                if (m == null) {
                    throw new EcmException(String.format("定义的方法未找到，请检查类型是否匹配。方法：%s.%s", def.getServiceDescriber().getServiceId(), sm.getAlias()));
                }
                meta.addServiceMethodMeta(sm, m);
            }
            return meta;
        } catch (ClassNotFoundException e) {
            throw new RuntimeException(e);
        }
    }

    protected Field findProperty(String propName, Class<?> c) {
        Field f = null;
        try {
            f = c.getDeclaredField(propName);
        } catch (SecurityException e) {
        } catch (NoSuchFieldException e) {
            if (!c.equals(Object.class))
                f = findProperty(propName, c.getSuperclass());
        }
        return f;
    }

    protected Constructor<?> findContructor(String[] argTypes, Class<?> clazz,
                                            ClassLoader loader) {
        Constructor<?> m = null;
        Class<?>[] arr = new Class<?>[argTypes == null ? 0 : argTypes.length];
        try {
            for (int i = 0; i < arr.length; i++) {
                String type = argTypes[i];
                Class<?> c = PrimitiveType.convert(type, loader);
                arr[i] = c;
            }
            m = clazz.getDeclaredConstructor(arr);

        } catch (NoSuchMethodException e) {
            Class<?> superC = clazz.getSuperclass();
            if (superC != Object.class) {
                m = findContructor(argTypes, superC, loader);
            }
        } catch (ClassNotFoundException e) {
            throw new RuntimeException(e);
        }
        return m;
    }

    protected Method findMethod(String name, String[] argTypes, Class<?> clazz,
                                ClassLoader loader) {
        Method m = null;
        Class<?>[] arr = new Class<?>[argTypes == null ? 0 : argTypes.length];
        try {
            for (int i = 0; i < arr.length; i++) {
                String type = argTypes[i];
                // Class<?> c = Class.forName(type, true, loader);
                Class<?> c = PrimitiveType.convert(type, loader);
                arr[i] = c;
            }
            m = clazz.getDeclaredMethod(name, arr);

        } catch (NoSuchMethodException e) {
            Class<?> superC = clazz.getSuperclass();
            if (superC != Object.class) {
                m = findMethod(name, argTypes, superC, loader);
            }
        } catch (ClassNotFoundException e) {
            throw new RuntimeException(e);
        }
        return m;
    }

    private class ServiceMethodInterruptor implements MethodInterceptor {

        // 该类主要是用来在方法调用后实现服务注入
        @Override
        public Object intercept(Object obj, Method method, Object[] args,
                                MethodProxy proxy) throws Throwable {
            return proxy.invokeSuper(obj, args);
        }
    }

}
