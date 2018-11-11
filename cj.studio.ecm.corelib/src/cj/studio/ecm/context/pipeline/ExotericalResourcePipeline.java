package cj.studio.ecm.context.pipeline;

import java.net.URL;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

import cj.studio.ecm.EcmException;
import cj.studio.ecm.IExotericalResourcePipeline;
import cj.studio.ecm.resource.IResource;

// 开放类型管道，它用于芯片间依赖的开放类型的查询
public class ExotericalResourcePipeline implements IExotericalResourcePipeline {
	// 扫描出来的外部类型名称集合,对应所依赖,当value=true时表示为开放的包路径
	private Map<String, Boolean> exotericalTypeNames;
	private List<IResource> referenceResources;
	private IResource owner;

	public ExotericalResourcePipeline(IResource owner) {
		owner.setPipeline(this);
		this.owner = owner;
		this.referenceResources = new ArrayList<IResource>();
		exotericalTypeNames = new HashMap<String, Boolean>();
	}

	@Override
	public void dispose() {
		// TODO Auto-generated method stub
		exotericalTypeNames.clear();
		referenceResources.clear();
		owner = null;
	}

	@Override
	public IResource getOwner() {
		// TODO Auto-generated method stub
		return owner;
	}

	@Override
	public void addExotericalTypeName(String name, boolean isPackagePath) {
		this.exotericalTypeNames.put(name, isPackagePath);

	}
	@Override
	public List<Class<?>> enumExotericalType() {
		List<Class<?>> list = new ArrayList<Class<?>>();
		ClassLoader cl = (ClassLoader) owner;
		try {
			Set<String> set=exotericalTypeNames.keySet();
			for (String key : set) {
				if (true != exotericalTypeNames.get(key)) {
					Class<?> c;
					c = Class.forName(key, true, cl);
					list.add(c);
				}
			}
			return list;
		} catch (ClassNotFoundException e) {
			throw new EcmException(e);
		}

	}

	@Override
	public void removeExotericalTypeName(String name) {
		this.exotericalTypeNames.remove(name);
	}

	@Override
	public boolean isContainsExotericalTypeName(String name) {
		Set<String> set = exotericalTypeNames.keySet();
		for (String key : set) {
			boolean isPackage = this.exotericalTypeNames.get(key);
			if ((isPackage) && name.startsWith(key)) {
				return true;
			}
			if (key.equals(name))
				return true;
		}
		return false;
	}

	@Override
	public boolean contains(IResource resource) {
		return referenceResources.contains(resource);
	}

	@Override
	public void addReference(IResource resource) {
		this.referenceResources.add(resource);
	}

	@Override
	public void removeReference(IResource resource) {
		this.referenceResources.remove(resource);
	}

	// @Override
	// public synchronized Class<?> searchReferenceType(String className)
	// throws ClassNotFoundException {
	// for (IResource r : this.referenceResources) {
	// if (!r.isContainsExotericalTypeName(className))
	// continue;
	// // 与Sysresource中的loadClass对照调用。它只能实现直接上级查寻
	// Class<?> c = r.loadClass(className);
	// if (c != null)
	// return c;
	// }
	// return null;
	// }
	@Override
	public synchronized Class<?> searchReferenceType(String className)
			throws ClassNotFoundException {
		return searchReferenceType(className, this);
	}

	// 前序搜索
	private synchronized Class<?> searchReferenceType(String className,
			ExotericalResourcePipeline pl) throws ClassNotFoundException {
		Class<?> c = null;
		for (IResource r : pl.referenceResources) {
			// 限定为只加载开放类型
			if (r.isContainsExotericalTypeName(className))
				c = r.loadClass(className);
			if (c != null)
				return c;
			else {
				return searchReferenceType(className,
						(ExotericalResourcePipeline) r.getPipeline());
			}
		}
		return c;
	}

	@Override
	public URL searchResource(String sName) {
		for (IResource r : this.referenceResources) {
			// 与Sysresource中的loadClass形成递归调用
			URL c = r.getResource(sName);
			if (c != null)
				return c;
		}
		return null;
	}

}