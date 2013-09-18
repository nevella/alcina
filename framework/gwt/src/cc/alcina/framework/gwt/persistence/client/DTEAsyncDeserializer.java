package cc.alcina.framework.gwt.persistence.client;

import java.util.ArrayList;
import java.util.List;

import cc.alcina.framework.common.client.logic.domaintransform.DeltaApplicationRecord;
import cc.alcina.framework.common.client.logic.domaintransform.DomainTransformEvent;
import cc.alcina.framework.common.client.logic.domaintransform.protocolhandlers.DTRProtocolHandler;
import cc.alcina.framework.common.client.logic.domaintransform.protocolhandlers.DTRProtocolSerializer;

import com.google.gwt.core.client.Scheduler.RepeatingCommand;

public class DTEAsyncDeserializer implements RepeatingCommand {
	private List<DomainTransformEvent> items = new ArrayList<DomainTransformEvent>();

	private DTRProtocolHandler protocolHandler;

	private DeltaApplicationRecord wrapper;

	public DTEAsyncDeserializer(DeltaApplicationRecord wrapper) {
		this.wrapper = wrapper;
		protocolHandler = new DTRProtocolSerializer().getHandler(wrapper
				.getProtocolVersion());
	}

	@Override
	public boolean execute() {
		List<DomainTransformEvent> events = new ArrayList<DomainTransformEvent>();
		String s = protocolHandler.deserialize(wrapper.getText(), events,
				100);
		items.addAll(events);
		return s != null;
	}

	public List<DomainTransformEvent> getItems() {
		return this.items;
	}
}