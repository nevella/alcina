package cc.alcina.framework.gwt.client.dirndl.model;

import cc.alcina.framework.common.client.reflection.Property;
import cc.alcina.framework.gwt.client.dirndl.layout.ContextService;

public interface NodeEditorContextService extends ContextService {
	default Property getEditingProperty() {
		throw new UnsupportedOperationException();
	}

	boolean isEditable();

	default boolean isDetached() {
		return false;
	}

	boolean isRenderAsNodeEditors();

	public static class Editable implements NodeEditorContextService {
		public static Editable INSTANCE = new Editable();

		@Override
		public boolean isEditable() {
			return true;
		}

		@Override
		public boolean isRenderAsNodeEditors() {
			return true;
		}
	}
}
