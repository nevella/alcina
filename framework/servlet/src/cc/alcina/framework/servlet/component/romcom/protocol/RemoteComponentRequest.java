package cc.alcina.framework.servlet.component.romcom.protocol;

import cc.alcina.framework.common.client.logic.reflection.reachability.Bean;
import cc.alcina.framework.common.client.logic.reflection.reachability.Bean.PropertySource;
import cc.alcina.framework.servlet.component.romcom.protocol.MessageTransportLayer2.MessageEnvelope;

@Bean(PropertySource.FIELDS)
public class RemoteComponentRequest {
	public RemoteComponentProtocol.Session session;

	public MessageEnvelope messageEnvelope;
}
