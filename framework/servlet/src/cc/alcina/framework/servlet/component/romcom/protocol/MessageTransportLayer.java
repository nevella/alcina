package cc.alcina.framework.servlet.component.romcom.protocol;

import cc.alcina.framework.common.client.logic.domaintransform.SequentialIdGenerator;

/**
 * This (simple) class provides the infrastructure to handle lost requests, by
 * numbering and queueing message receipt/dispatch
 */
public class MessageTransportLayer {
	SequentialIdGenerator idGenerator = new SequentialIdGenerator();

	public int nextId() {
		return (int) idGenerator.incrementAndGet();
	}
}
