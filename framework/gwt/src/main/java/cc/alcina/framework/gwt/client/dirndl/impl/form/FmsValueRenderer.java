package cc.alcina.framework.gwt.client.dirndl.impl.form;

import com.google.gwt.user.client.ui.Widget;

import cc.alcina.framework.gwt.client.dirndl.layout.BridgingValueRenderer;

public class FmsValueRenderer extends BridgingValueRenderer {
	@Override
	protected void customizeWidget(Widget widget) {
		widget = FmsUtil.cleanupStyles(widget);
	}
}