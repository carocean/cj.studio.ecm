package cj.studio.ecm.net.layer;

public interface ITokenDistributionStrategy {
	/**
	 * 在整个netsite中唯一，且具有签名信息。
	 * <pre>
	 *
	 * </pre>
	 * @param info
	 * @return
	 */
	String genToken(SessionInfo info);

}
