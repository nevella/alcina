package cc.alcina.framework.gwt.client.gwittir.widget;

import com.totsp.gwittir.client.ui.Label;

import cc.alcina.framework.gwt.client.gwittir.customiser.MultilineWidget;

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
