package cc.alcina.framework.gwt.client.dirndl.model;

import cc.alcina.framework.common.client.csobjects.Bindable;
import cc.alcina.framework.common.client.serializer.TypeSerialization;
import cc.alcina.framework.gwt.client.dirndl.annotation.Directed;

/**
 * Contains either a standard layout model (such as HeaderContent) (for normal
 * UI) or a model which is essentially a modal in-page
 *
 * Renders by delegating to the model field
 */
@Directed.Delegating
@TypeSerialization(reflectiveSerializable = false)
public class TopModel extends Bindable {
	private Object model;

	@Directed
	public Object getModel() {
		return this.model;
	}

	public void setModel(Object model) {
		Object old_model = this.model;
		this.model = model;
		propertyChangeSupport().firePropertyChange("model", old_model, model);
	}
}
