package cc.alcina.framework.gwt.client.dirndl.model;

import com.google.gwt.dom.client.Element;

public interface HasElement {
	Element provideElement();

	boolean provideIsBound();

	boolean provideIsUnbound();
}
