package cj.studio.ecm;
/**
 * 服务事件。在服务容器加载完所有服务后执行
 * <pre>
 *用于初始化服务，因为在一个服务构造前，比如根据类型取得其它服务，
 *因为不像是单个服务的引用一样会导致引用的服务初始化，
 *因此欲取得完整的符合类型的集合，就必须在服务后获取。
 * </pre>
 * @author carocean
 *
 */
public interface IServiceAfter {
 void onAfter(IServiceSite site);
}
