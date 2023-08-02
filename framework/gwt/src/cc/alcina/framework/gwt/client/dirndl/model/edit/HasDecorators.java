package cc.alcina.framework.gwt.client.dirndl.model.edit;

import cc.alcina.framework.gwt.client.dirndl.model.edit.DecoratorChooser.BeforeChooserClosed;

public interface HasDecorators extends DecoratorChooser.BeforeChooserClosed.Handler {
	@Override
	default void onChooserClosed(BeforeChooserClosed event) {
		validateDecorators();
	}

	void validateDecorators();
}
