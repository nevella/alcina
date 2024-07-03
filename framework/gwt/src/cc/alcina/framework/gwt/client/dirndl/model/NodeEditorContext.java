package cc.alcina.framework.gwt.client.dirndl.model;

import cc.alcina.framework.common.client.reflection.Property;

public interface NodeEditorContext {
	public static NodeEditorContext get(Object o) {
		return ((NodeEditorContext.Has) o).getNodeEditorContext();
	}

	default Property getEditingProperty() {
		throw new UnsupportedOperationException();
	}

	boolean isEditable();

	default boolean isDetached() {
		return false;
	}

	boolean isRenderAsNodeEditors();

	public interface Has {
		NodeEditorContext getNodeEditorContext();
	}
}
