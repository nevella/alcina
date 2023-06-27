package cc.alcina.framework.gwt.client.dirndl.layout;

import com.google.gwt.dom.client.Document;

import cc.alcina.framework.common.client.logic.reflection.Registration;
import cc.alcina.framework.gwt.client.dirndl.layout.DirectedLayout.Rendered;

@Registration(RootModifier.class)
public class RootModifier {
	public void appendToRoot(Rendered rendered) {
		Document.get().getBody().appendChild(rendered.getNode());
	}

	public void replaceRoot(Rendered rendered) {
		appendToRoot(rendered);
	}
}