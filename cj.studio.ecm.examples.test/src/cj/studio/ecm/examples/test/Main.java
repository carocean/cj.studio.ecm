package cj.studio.ecm.examples.test;

import java.util.List;

import cj.studio.ecm.Assembly;
import cj.studio.ecm.IAssembly;
import cj.studio.ecm.IChipInfo;
import cj.studio.ecm.IServiceAfter;
import cj.studio.ecm.IServiceProvider;
import cj.studio.ecm.ServiceCollection;
import cj.studio.ecm.adapter.IActuator;
import cj.studio.ecm.adapter.IAdaptable;

public class Main {
	static String examplesDir=String.format("%s/examples", System.getProperty("user.dir"));
	static String version="2.4.0";
	public class MyMain extends Main{
		
	}
	public static void main(String... strings) {
		test5();
		// 测jss
	}
	//
	private static void test6() {
		// TODO Auto-generated method stub
		String fn1 = String.format("/%s/cj.studio.ecm.examples.chip1/cj.studio.ecm.examples.chip1-%s.jar", examplesDir, version);
		IAssembly chip1 = Assembly.loadAssembly(fn1);
		chip1.start();
		chip1.stop();
	}
	private static void test5() {
		// TODO Auto-generated method stub
		String fn1 = String.format("/%s/cj.studio.ecm.examples.chip1/cj.studio.ecm.examples.chip1-%s.jar", examplesDir, version);
		IAssembly chip1 = Assembly.loadAssembly(fn1);
		chip1.start();
		Object obj=chip1.workbin().part("main");
		IAdaptable a=(IAdaptable)obj;
		IActuator t=a.getAdapter(IActuator.class);
		t.exactCommand("test", new Class<?>[] {String.class}, "tttt");
		chip1.stop();
	}

	// 测json/xml注解方式
	public static void test3() {
		String fn1 = String.format("/%s/cj.studio.ecm.examples.chip1/cj.studio.ecm.examples.chip1-%s.jar", examplesDir, version);
		IAssembly chip1 = Assembly.loadAssembly(fn1);
		chip1.start();
		Object obj=chip1.workbin().part("$.cj.studio.gateway.app");
		System.out.println("--这是获取服务："+obj);
	}

	// 测程序集类型依赖
	// 内部调用在另一程序集中的实现
	public static void test4() {
		String fn1 = String.format("/%s/cj.studio.ecm.examples.chip1/cj.studio.ecm.examples.chip1-%s.jar", examplesDir, version);
		IAssembly chip1 = Assembly.loadAssembly(fn1);
		chip1.start();
		String fn2 = String.format("/%s/cj.studio.ecm.examples.chip2/cj.studio.ecm.examples.chip2-%s.jar", examplesDir, version);
		IAssembly chip2 = Assembly.loadAssembly(fn2);
		chip2.dependency(chip1);
		chip2.start();
		Object on=chip1.workbin().part("singleOn");
		IAdaptable a=(IAdaptable)on;
		IActuator exe=a.getAdapter(IActuator.class);
		exe.exeCommand("test", "xxx");
	}

	// 测程序集类型依赖
	// 测以包路径开放服务
	public static void test2() {
		String fn1 = String.format("/%s/cj.studio.ecm.examples.chip1/cj.studio.ecm.examples.chip1-%s.jar", examplesDir, version);
		IAssembly chip1 = Assembly.loadAssembly(fn1);
		chip1.start();
		String fn2 = String.format("/%s/cj.studio.ecm.examples.chip2/cj.studio.ecm.examples.chip2-%s.jar", examplesDir, version);
		IAssembly chip2 = Assembly.loadAssembly(fn2);
		chip2.dependency(chip1);
		chip2.start();
		List<Class<?>> list = chip1.workbin().exotericalType("exoType");// 从chip1中取类型
		for (Class<?> clazz : list) {
			ServiceCollection<?> col =chip1.workbin().part(clazz);
			for (Object obj : col) {
				System.out.println("++++++这是搜到的chip1中的实现：" + obj);
			}
			col = chip2.workbin().part(clazz);// 在chip2中搜索实现类
			for (Object obj : col) {
				System.out.println("++++++这是搜到的chip2中的实现：" + obj);
			}
		}
		System.out.println(list.size());
		chip2.stop();
	}

	// 测外部提供服务
	public static void test1() {
		String fn1 = String.format("/%s/cj.studio.ecm.examples.chip1/cj.studio.ecm.examples.chip1-%s.jar", examplesDir, version);
		IAssembly a = Assembly.loadAssembly(fn1);
		a.parent(new IServiceProvider() {

			@Override
			public <T> ServiceCollection<T> getServices(Class<T> serviceClazz) {
				// TODO Auto-generated method stub
				return null;
			}

			@Override
			public Object getService(String serviceId) {
				if ("$.object".equals(serviceId)) {
					return new Object();
				}
				return null;
			}
		});
		a.start();
		IChipInfo ci = a.workbin().chipInfo();
		String aa = a.workbin().getProperty("site.session.expire");
		System.out.println(aa);
		Object obj1 = a.workbin().part("singleOn");
		System.out.println(obj1);
		ServiceCollection<IServiceAfter> col = a.workbin().part(IServiceAfter.class);
		System.out.println(col.size());
		List<Class<?>> list = a.workbin().exotericalType("aa");
		System.out.println(list.size());
	}
}
