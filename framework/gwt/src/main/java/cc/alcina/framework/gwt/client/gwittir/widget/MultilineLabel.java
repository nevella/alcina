package cc.alcina.framework.gwt.client.gwittir.widget;

import cc.alcina.framework.common.client.logic.reflection.reachability.Reflected;
import cc.alcina.framework.gwt.client.gwittir.customiser.MultilineWidget;
import cc.alcina.framework.gwt.shadow.com.totsp.gwittir.client.ui.Label;

@Reflected
public class MultilineLabel extends Label implements MultilineWidget {
	public MultilineLabel() {
		super(null);
	}

	public MultilineLabel(String text) {
		super(text);
	}

	@Override
	public boolean isMultiline() {
		return true;
	}
}
