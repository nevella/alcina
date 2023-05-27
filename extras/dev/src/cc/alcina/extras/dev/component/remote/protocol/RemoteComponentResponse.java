package cc.alcina.extras.dev.component.remote.protocol;

import cc.alcina.framework.common.client.logic.reflection.reachability.Bean;
import cc.alcina.framework.common.client.logic.reflection.reachability.Bean.PropertySource;

@Bean(PropertySource.FIELDS)
public class RemoteComponentResponse {
	public RemoteComponentRequest.Session session;

	public int requestId;

	public ProtocolMessage protocolMessage;
}
