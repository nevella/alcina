package cc.alcina.framework.servlet.component.romcom.protocol;

import com.google.gwt.dom.client.Pathref;

import cc.alcina.framework.common.client.logic.reflection.reachability.Bean;
import cc.alcina.framework.common.client.logic.reflection.reachability.Bean.PropertySource;

@Bean(PropertySource.FIELDS)
public class EventSystemMutation {
	public Pathref path;

	public int eventBits = -1;

	public String eventTypeName;

	public EventSystemMutation() {
	}

	public EventSystemMutation(Pathref path, int eventBits) {
		this.path = path;
		this.eventBits = eventBits;
	}

	public EventSystemMutation(Pathref path, String eventTypeName) {
		this.path = path;
		this.eventTypeName = eventTypeName;
	}
}
