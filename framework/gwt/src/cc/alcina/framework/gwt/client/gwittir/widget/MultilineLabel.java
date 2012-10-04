package cc.alcina.framework.gwt.client.gwittir.widget;

import cc.alcina.framework.gwt.client.gwittir.customiser.MultilineWidget;

import com.totsp.gwittir.client.ui.Label;

public class MultilineLabel extends Label implements MultilineWidget {
	@Override
	public boolean isMultiline() {
		return true;
	}
	public MultilineLabel() {
        super(null);
    }
    public MultilineLabel(String text){
        super(text);
    }
}
