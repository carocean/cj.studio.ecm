package cj.studio.ecm.net.site;
/**
 * 
 * 网络站点入口。
 * <pre>
 * netsite搜索主芯片中的该服务，并主动加载。
 * 开发者在开发主新片是需实现该类
 * 
 * 该接口提供组装四类graph的功能:
 * 1.netgraph,即服务源
 * 2.embedgraph,/embed目录下的芯片
 * 3.localgraph,/local目录下的芯片
 * 3.remotegraph,远程芯片客户端芯片
 * </pre>
 * @author carocean
 *
 */
public interface INetSiteEntrance {

}
