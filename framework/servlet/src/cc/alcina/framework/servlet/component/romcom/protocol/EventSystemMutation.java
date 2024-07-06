package cc.alcina.framework.servlet.component.romcom.protocol;

import com.google.gwt.dom.client.Refid;

import cc.alcina.framework.common.client.logic.reflection.reachability.Bean;
import cc.alcina.framework.common.client.logic.reflection.reachability.Bean.PropertySource;
import cc.alcina.framework.common.client.util.FormatBuilder;

@Bean(PropertySource.FIELDS)
public final class EventSystemMutation {
	public Refid path;

	public int eventBits = -1;

	public String eventTypeName;

	public EventSystemMutation() {
	}

	public EventSystemMutation(Refid path, int eventBits) {
		this.path = path;
		this.eventBits = eventBits;
	}

	public EventSystemMutation(Refid path, String eventTypeName) {
		this.path = path;
		this.eventTypeName = eventTypeName;
	}

	@Override
	public String toString() {
		return FormatBuilder.keyValues("bits", eventBits, "type", eventTypeName,
				"path", path);
	}
}
