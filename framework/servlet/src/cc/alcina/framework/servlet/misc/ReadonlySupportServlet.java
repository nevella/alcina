package cc.alcina.framework.servlet.misc;

import java.util.List;

import cc.alcina.framework.common.client.logic.reflection.RegistryLocation;
import cc.alcina.framework.common.client.logic.reflection.RegistryLocation.ImplementationType;
import cc.alcina.framework.common.client.logic.reflection.registry.Registry;
import cc.alcina.framework.common.client.util.TopicPublisher.TopicListener;
import cc.alcina.framework.entity.persistence.AppPersistenceBase;
import cc.alcina.framework.gwt.client.rpc.OutOfBandMessage;
import cc.alcina.framework.servlet.servlet.CommonRemoteServiceServlet;

@RegistryLocation(registryPoint = ReadonlySupportServlet.class, implementationType = ImplementationType.SINGLETON)
// FIXME - mvcc.4 - add registrableservice
public class ReadonlySupportServlet {
	public static ReadonlySupportServlet get() {
		return Registry.impl(ReadonlySupportServlet.class);
	}

	private String clientInstanceMessage;

	private String notPerformedBecauseReadonlyMessage;

	private TopicListener<List<OutOfBandMessage>> appendMessageListener = (k,
			list) -> {
		if (AppPersistenceBase.isInstanceReadOnly()) {
			{
				OutOfBandMessage.ClientInstanceMessage message = new OutOfBandMessage.ClientInstanceMessage();
				message.setMessageHtml(getClientInstanceMessage());
				list.add(message);
			}
			{
				OutOfBandMessage.ReadonlyInstanceMessage message = new OutOfBandMessage.ReadonlyInstanceMessage();
				message.setReadonly(true);
				list.add(message);
			}
		}
	};

	public ReadonlySupportServlet() {
		CommonRemoteServiceServlet.OutOfBandMessages.topicAppendMessages
				.add(appendMessageListener);
	}

	public String getClientInstanceMessage() {
		return this.clientInstanceMessage;
	}

	public String getNotPerformedBecauseReadonlyMessage() {
		return this.notPerformedBecauseReadonlyMessage;
	}

	public void setClientInstanceMessage(String clientInstanceMessage) {
		this.clientInstanceMessage = clientInstanceMessage;
	}

	public void setNotPerformedBecauseReadonlyMessage(
			String notPerformedBecauseReadonlyMessage) {
		this.notPerformedBecauseReadonlyMessage = notPerformedBecauseReadonlyMessage;
	}
}
