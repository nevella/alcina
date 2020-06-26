package cc.alcina.framework.gwt.client.widget.typedbinding;

import cc.alcina.framework.common.client.Reflections;

public interface EnumeratedBinding {
	default <T> T get(HasEnumeratedBindings source) {
		return (T) source.getEnumeratedBindingSupport().get(this);
	}

	Class getBoundClass();

	default String getBoundPath() {
		return toString();
	}

	default Class getBoundPropertyType() {
		return Reflections.propertyAccessor().getPropertyType(getBoundClass(),
				getPath());
	}

	default String getPath() {
		return toString();
	}

	default void set(HasEnumeratedBindings source, Object o) {
		source.getEnumeratedBindingSupport().set(this, o);
	}
}
