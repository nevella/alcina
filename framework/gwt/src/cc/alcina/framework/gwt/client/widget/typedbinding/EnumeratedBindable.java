package cc.alcina.framework.gwt.client.widget.typedbinding;

import java.beans.PropertyChangeListener;

import cc.alcina.framework.common.client.csobjects.BaseBindable;

public abstract class EnumeratedBindable extends BaseBindable
		implements HasEnumeratedBindings {
	protected EnumeratedBindingSupport enumeratedBindingSupport;

	@Override
	public void addPropertyChangeListener(String propertyName,
			PropertyChangeListener listener) {
		super.addPropertyChangeListener(propertyName, listener);
		this.enumeratedBindingSupport.addPropertyChangeListener(propertyName,
				listener);
	}

	@Override
	public EnumeratedBindingSupport getEnumeratedBindingSupport() {
		return enumeratedBindingSupport;
	}

	@Override
	public void removePropertyChangeListener(String propertyName,
			PropertyChangeListener listener) {
		super.removePropertyChangeListener(propertyName, listener);
		this.enumeratedBindingSupport.removePropertyChangeListener(propertyName,
				listener);
	}
}
