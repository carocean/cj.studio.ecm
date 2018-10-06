package cj.studio.ecm.container.resolver;

import java.lang.reflect.Field;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.Vector;

import cj.studio.ecm.resource.SystemResource;
import cj.ultimate.IDisposable;

//用于在注解被解析时装入类型，该加载器是临时的，在注解解析完成后当释放
public class AnnotationResource extends SystemResource implements IDisposable {

	public AnnotationResource() {
		// TODO Auto-generated constructor stub
	}

	public AnnotationResource(ClassLoader parent) {
		super(parent);
	}

	@Override
	public void dispose() {
		this.dispose(true);
	}

	@Override
	protected void finalize() throws Throwable {
		this.dispose(true);
		super.finalize();
	}
	private Field findProperty(String propName, Class<?> c) {
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
	@Override
	protected void dispose(boolean disposing) {
		if (disposing) {
			Class<?> clazz = SystemResource.class;
			try {
				Field parent = findProperty("parent",clazz);
				Field classes = findProperty("classes",clazz);
				Field packages = findProperty("packages",clazz);
				Field jdkLoader = findProperty("jdkLoader",clazz);
				Field pipeline = findProperty("pipeline",clazz);
				Field resourceNames = findProperty("resourceNames",clazz);
				Field lstJarFile = findProperty("lstJarFile",clazz);
				Field hsNativeFile = findProperty("hsNativeFile",clazz);
				Field hmClass = findProperty("hmClass",clazz);

				if (parent != null) {
					parent.setAccessible(true);
					parent.set(this, null);
				}
				if (classes != null) {
					classes.setAccessible(true);
					Vector<?> v = (Vector<?>) classes.get(this);
					if (v != null)
						v.clear();
				}
				if (packages != null) {
					packages.setAccessible(true);
					Map<?, ?> map = (Map<?, ?>) packages.get(this);
					if (map != null)
						map.clear();
				}
				if (jdkLoader != null) {
					jdkLoader.setAccessible(true);
					jdkLoader.set(this, null);
				}
				if (pipeline != null) {
					pipeline.setAccessible(true);
					pipeline.set(this, null);
				}
				if (resourceNames != null) {
					resourceNames.setAccessible(true);
					List<?> list = (List<?>) resourceNames.get(this);
					if (list != null) {
						list.clear();
					}
				}
				if (lstJarFile != null) {
					lstJarFile.setAccessible(true);
					List<?> list = (List<?>) lstJarFile.get(this);
					if (list != null) {
						list.clear();
					}
				}
				if (hsNativeFile != null) {
					hsNativeFile.setAccessible(true);
					Set<?> list = (Set<?>) hsNativeFile.get(this);
					if (list != null) {
						list.clear();
					}
				}
				if (hmClass != null) {
					hmClass.setAccessible(true);
					Map<?, ?> map = (Map<?, ?>) hmClass.get(this);
					if (map != null)
						map.clear();
				}
			} catch (Throwable e) {
				throw new RuntimeException(e);
			}
		}
	}
}
