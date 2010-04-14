package cc.alcina.framework.common.client.logic.domaintransform.protocolhandlers;

import java.util.List;

import cc.alcina.framework.common.client.logic.domaintransform.DomainTransformEvent;
/**
 * implementing classes must be @clientinstantiable
 * @author nick@alcina.cc
 *
 */
public interface DTRProtocolHandler {
	public String handlesVersion();

	public String serialize(List<DomainTransformEvent> events);

	public List<DomainTransformEvent> deserialize(String serializedEvents);

	// this should throw an unsupported op for, say, GWTRPC-encoded handlers
	public void appendTo(DomainTransformEvent domainTransformEvent,
			StringBuffer sb);
	// this should throw an unsupported op for, say, GWTRPC-encoded handlers
	/**
	 * @return last parsed string (null if finished)
	 */
	public String deserialize(String serializedEvents,
			List<DomainTransformEvent> events, int maxCount);
	// this should throw an unsupported op for, say, GWTRPC-encoded handlers (offset of text processed)
	public int getOffset();
}
