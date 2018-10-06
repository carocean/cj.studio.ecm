package cj.studio.ecm.container.describer;

import java.lang.reflect.AccessibleObject;
import java.lang.reflect.Constructor;
import java.lang.reflect.Executable;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;

import cj.studio.ecm.EcmException;

//此包装目的是为了将来扩展需要。
public class MethodWrapper {
	private AccessibleObject accessibleObject;

	public AccessibleObject getMethod() {
		return accessibleObject;
	}

	public void setMethod(AccessibleObject method) {
		this.accessibleObject = method;
	}

	@Override
	public boolean equals(Object obj) {
		if (obj instanceof MethodWrapper) {
			MethodWrapper w = (MethodWrapper) obj;
			return w.accessibleObject.equals(this.accessibleObject);
		}
		return super.equals(obj);
	}

	public Object invoke(Object obj, Object[] args) {
		if (accessibleObject == null)
			throw new EcmException("没有可执行的方法");
		Object s = null;
		try {
			if (this.accessibleObject instanceof Method) {
				Method m = (Method) this.accessibleObject;
				Class<?> onClass=m.getDeclaringClass();
				if(Modifier.isStatic(m.getModifiers()) ||onClass.isAssignableFrom(obj.getClass())){
					s = m.invoke(obj, args);
				}else{
					Method method = findMethod(m.getName(),
							m.getParameterTypes(),
							obj.getClass());
					if (method == null){
						throw new EcmException(String.format(
								"no such method in %s.%s",
								m.getDeclaringClass(), m.getName()));
					}
					return method.invoke(obj, args);
				}
				
			} else if (accessibleObject instanceof Constructor<?>) {
				Constructor<?> c = (Constructor<?>) this.accessibleObject;
				s = c.newInstance(args);
			}
			return s;
		} catch (Exception e) {
			if (accessibleObject != null) {
				throw new EcmException(
						String.format("方法：%s 在类： %s, 原因：%s",accessibleObject,( (Executable)accessibleObject).getDeclaringClass(),e));
			} else {
				throw new EcmException(e);
			}
		}
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
