package cj.studio.ecm.util;

import java.lang.reflect.Field;
import java.util.List;

//对象存取器
public class ObjectHelper {
	
	public static void fetchAllInterface(Class<?> clazz, List<Class<?>> list) {

		Class<?>[] arr = clazz.getInterfaces();
		for (Class<?> c : arr) {
			if (list.contains(c))
				continue;
			list.add(c);
		}
		Class<?> superc = clazz.getSuperclass();
		if (!Object.class.equals(superc)) {
			fetchAllInterface(superc, list);
		}
	}

	public static Object get(Object obj, String fieldName)
			throws NoSuchFieldException, IllegalArgumentException,
			IllegalAccessException {
		Field f = findField(obj.getClass(), fieldName);
		if (f == null)
			throw new NoSuchFieldException();
		f.setAccessible(true);
		return f.get(obj);
	}

	public static Field findField(Class<?> clazz, String fieldName) {
		try {
			return clazz.getDeclaredField(fieldName);
		} catch (SecurityException e) {
			throw new RuntimeException(e);
		} catch (NoSuchFieldException e) {
			if (Object.class != clazz.getSuperclass())
				return findField(clazz.getSuperclass(), fieldName);
		}
		return null;
	}

	public static Field findProperty(String propName, Class<?> c) {
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
}
