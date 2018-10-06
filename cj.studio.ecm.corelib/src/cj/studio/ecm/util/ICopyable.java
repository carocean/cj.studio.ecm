package cj.studio.ecm.util;
/**
 * 可拷贝的。它用于将一个封闭的不开放的对象，附加一些行为
 * 如类A的属性在开放给开发者的接口中只有只读不能写，在核心设计中需要改变类A的属性，那么就用到此接口
 * 可设类B，派生于类A，在核心类使用B，B可不开发给开发者。通过copyFrom方法将B的属性赋予A
 * <br><br>
 * 类实现该接口可以隐藏写入方法，虽然爆漏了copyFrom，但可以通过只读属性拦载此方法的访问权限
 * <br>
 * <br>
 * 注意事项：在扩展类中重载get方法，使得基类的copyFrom从派生类中取得，这样可以封闭基类的字段
 * @author Administrator
 *
 */
public interface ICopyable {
	void copyFrom(Object obj);
}
