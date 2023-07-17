package cc.alcina.framework.servlet.component.romcom.protocol;

import cc.alcina.framework.common.client.logic.reflection.reachability.Bean;
import cc.alcina.framework.common.client.logic.reflection.reachability.Bean.PropertySource;
import cc.alcina.framework.servlet.component.romcom.protocol.RemoteComponentProtocol.Message;

@Bean(PropertySource.FIELDS)
public class RemoteComponentResponse {
	public RemoteComponentProtocol.Session session;

	public int requestId;

	public Message protocolMessage;
}
