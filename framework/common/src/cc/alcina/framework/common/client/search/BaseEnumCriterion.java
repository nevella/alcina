package cc.alcina.framework.common.client.search;

public abstract class BaseEnumCriterion<E extends Enum>
		extends EnumCriterion<E> {
	private E value;

	@Override
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
