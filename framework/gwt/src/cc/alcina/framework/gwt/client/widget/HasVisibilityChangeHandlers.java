package cc.alcina.framework.gwt.client.widget;

import com.google.gwt.event.shared.HandlerRegistration;
import com.google.gwt.event.shared.HasHandlers;

public interface HasVisibilityChangeHandlers extends HasHandlers{
	HandlerRegistration addVisibilityChangeHandler(VisibilityChangeEvent.Handler handler);
}
