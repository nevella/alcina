package cc.alcina.framework.gwt.client.dirndl.behaviour;

import com.google.gwt.user.client.ui.impl.FocusImpl;

import cc.alcina.framework.gwt.client.dirndl.behaviour.LayoutEvents.Bind;

public interface FocusOnAttach extends LayoutEvents.Bind.Handler {
	boolean isFocusOnAttach();

	@Override
	default void onBind(Bind event) {
		if (event.isBound() && isFocusOnAttach()) {
			FocusImpl.getFocusImplForWidget()
					.focus(event.getContext().node.getWidget().getElement());
		}
	}
}