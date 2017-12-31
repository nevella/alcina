package cc.alcina.framework.gwt.client.widget.typedbinding;

public interface EnumeratedBinding {
	default <T> T get(HasEnumeratedBindings source) {
		return (T) source.getEnumeratedBindingSupport().get(this);
	}

	Class getBoundClass();

	String getBoundPath();

	Class getBoundPropertyType();

	default String getPath() {
		return toString();
	}

	default void set(HasEnumeratedBindings source, Object o) {
		source.getEnumeratedBindingSupport().set(this, o);
	}
}
