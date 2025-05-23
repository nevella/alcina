package cc.alcina.framework.gwt.client.dirndl.model.edit;

import cc.alcina.framework.common.client.flight.FlightEventWrappable;
import cc.alcina.framework.common.client.logic.reflection.reachability.Bean;
import cc.alcina.framework.common.client.logic.reflection.reachability.Bean.PropertySource;
import cc.alcina.framework.common.client.process.ProcessObservable;
import cc.alcina.framework.common.client.util.Ax;

@Bean(PropertySource.FIELDS)
public class DecoratorEvent implements ProcessObservable, FlightEventWrappable {
	public String sessionId;

	public String message;

	public Type type;

	public String subtype;

	public MutationStrings mutationStrings;

	@Bean(PropertySource.FIELDS)
	public static class MutationStrings {
		public String mutationRecords;

		public String editorDom;
	}

	public DecoratorEvent withType(Type type) {
		this.type = type;
		return this;
	}

	public DecoratorEvent withSubtype(String subtype) {
		this.subtype = subtype;
		return this;
	}

	public DecoratorEvent withMessage(String message) {
		this.message = message;
		return this;
	}

	public enum Type {
		node_bound, node_unbound, zws_refreshed, editable_attr_changed,
		selection_changed, editor_transforms_applied, overlay_opened,
		overlay_closed
	}

	public String getSessionId() {
		return sessionId;
	}

	@Override
	public String provideDetail() {
		return message;
	}

	@Override
	public String provideSubcategory() {
		return Ax.format("%s :: %s", type, subtype);
	}
}
