package cc.alcina.framework.common.client.search;

import cc.alcina.framework.common.client.serializer.PropertySerialization;

public abstract class BaseEnumCriterion<E extends Enum>
		extends EnumCriterion<E> {
	private E value;

	@Override
	@PropertySerialization(defaultProperty = true)
	public E getValue() {
		return this.value;
	}

	@Override
	public void setValue(E value) {
		E old_value = this.value;
		this.value = value;
		propertyChangeSupport().firePropertyChange("value", old_value, value);
	}
}
