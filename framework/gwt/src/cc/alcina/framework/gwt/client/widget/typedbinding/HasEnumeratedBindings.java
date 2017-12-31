package cc.alcina.framework.gwt.client.widget.typedbinding;

public interface HasEnumeratedBindings {
	EnumeratedBindingSupport getEnumeratedBindingSupport();

	Object provideRelatedObject(Class boundClass);
}
