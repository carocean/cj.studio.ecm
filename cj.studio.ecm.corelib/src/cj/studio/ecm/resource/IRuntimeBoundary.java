package cj.studio.ecm.resource;

import cj.studio.ecm.domain.IDomain;

/**
 * 运行时边界，为资源提供域
 * @author Administrator
 *
 */
public interface IRuntimeBoundary {
	IDomain getDomain();
}
