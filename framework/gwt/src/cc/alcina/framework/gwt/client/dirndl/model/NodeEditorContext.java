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

	boolean isRenderAsNodeEditors();

	public interface Has {
		NodeEditorContext getNodeEditorContext();
	}
}
