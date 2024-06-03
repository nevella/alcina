package cc.alcina.framework.servlet.component.romcom.protocol;

import cc.alcina.framework.common.client.logic.reflection.reachability.Bean;
import cc.alcina.framework.common.client.logic.reflection.reachability.Bean.PropertySource;
import cc.alcina.framework.common.client.util.CommonUtils;
import cc.alcina.framework.servlet.component.romcom.protocol.RemoteComponentProtocol.Message;
import cc.alcina.framework.servlet.component.romcom.protocol.RemoteComponentProtocol.ProtocolException;

@Bean(PropertySource.FIELDS)
public class RemoteComponentResponse {
	public RemoteComponentProtocol.Session session;

	public int requestId;

	public Message protocolMessage;

	public void putException(Exception e) {
		Message.ProcessingException processingException = new Message.ProcessingException();
		processingException.exceptionClassName = e.getClass().getName();
		processingException.exceptionMessage = CommonUtils
				.toSimpleExceptionMessage(e);
		if (e instanceof ProtocolException) {
			processingException.protocolException = (ProtocolException) e;
		}
		protocolMessage = processingException;
	}
}
