package cj.studio.ecm;

/**
 * 程序集依赖器
 * 
 * <pre>
 * 用于将指定的子程序集依赖到当前程序集中。
 * </pre>
 * 
 * @author carocean
 *
 */
public interface IAssemblyDependency {
	IChipInfo current();

	/**
	 * 当前程序集的id
	 * 
	 * <pre>
	 *
	 * </pre>
	 * 
	 * @return
	 */
	String assemblyGuid();

	String assemblyHome();

	/**
	 * 将指定的子程序集依赖于当前程序集
	 * 
	 * <pre>
	 *
	 * </pre>
	 * 
	 * @param child
	 */
	void dependency(Assembly child);

	void undependency(Assembly child);

	IAssembly assembly();
}
