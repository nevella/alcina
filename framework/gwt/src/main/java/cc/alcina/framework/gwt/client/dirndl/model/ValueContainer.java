package cc.alcina.framework.gwt.client.dirndl.model;

import cc.alcina.framework.common.client.reflection.Property;
import cc.alcina.framework.gwt.client.dirndl.annotation.Directed;
import cc.alcina.framework.gwt.client.dirndl.event.LayoutEvents.Bind;
import cc.alcina.framework.gwt.client.dirndl.layout.ModelTransform.AbstractContextSensitiveModelTransform;
import cc.alcina.framework.gwt.client.dirndl.model.FormModel.ValueModel;

@Directed.Delegating
public class ValueContainer extends Model.Fields {
	@Directed.Transform(ContainerTransform.class)
	final ValueModel valueModel;

	public ValueContainer(ValueModel valueModel) {
		this.valueModel = valueModel;
	}

	@Override
	public void onBind(Bind event) {
		super.onBind(event);
	}

	public static class ContainerTransform
			extends AbstractContextSensitiveModelTransform<ValueModel, Object> {
		@Override
		public Object apply(ValueModel t) {
			Property property = t.getField().getProperty();
			return null;
		}
	}
}
