package cc.alcina.framework.gwt.client.dirndl.model.edit;

import cc.alcina.framework.common.client.flight.FlightEventWrappable;
import cc.alcina.framework.common.client.logic.reflection.reachability.Bean;
import cc.alcina.framework.common.client.logic.reflection.reachability.Bean.PropertySource;
import cc.alcina.framework.common.client.process.ProcessObservable;

@Bean(PropertySource.FIELDS)
public class DecoratorEvent implements ProcessObservable, FlightEventWrappable {
	public String sessionId;

	public String message;

	public Type type;

	public DecoratorEvent withType(Type type) {
		this.type = type;
		return this;
	}

	public DecoratorEvent withMessage(String message) {
		this.message = message;
		return this;
	}

	public enum Type {
		node_bound, node_unbound
	}

	public String getSessionId() {
		return sessionId;
	}
}
