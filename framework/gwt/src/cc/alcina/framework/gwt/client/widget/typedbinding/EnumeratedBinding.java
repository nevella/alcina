package cc.alcina.framework.gwt.client.widget.typedbinding;

public interface EnumeratedBinding {
	default <T> T get(HasEnumeratedBindings source) {
		return (T) source.getEnumeratedBindingSupport().get(this);
	}

	default void set(HasEnumeratedBindings source, Object o) {
		source.getEnumeratedBindingSupport().set(this, o);
	}

	default String getPath() {
		return toString();
	}

	Class getBoundClass();

	Class getBoundPropertyType();

	String getBoundPath();
}
