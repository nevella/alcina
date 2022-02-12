package cc.alcina.framework.gwt.client.dirndl.behaviour;

import com.google.gwt.user.client.ui.impl.FocusImpl;

import cc.alcina.framework.gwt.client.dirndl.behaviour.GwtEvents.Attach;

public interface FocusOnAttach extends GwtEvents.Attach.Handler {
	@Override
	default void onAttach(Attach event) {
		if (isFocusOnAttach()) {
			FocusImpl.getFocusImplForWidget()
					.focus(event.getContext().node.getWidget().getElement());
		}
	}

	boolean isFocusOnAttach();
}