package cc.alcina.framework.gwt.client.dirndl.widget;

import com.google.gwt.dom.client.Document;
import com.google.gwt.user.client.ui.Widget;

public class SimpleWidget extends Widget {
	public SimpleWidget(String tag) {
		setElement(Document.get().createElement(tag));
	}
}
