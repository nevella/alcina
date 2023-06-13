package cc.alcina.framework.entity.gwt.headless;

import com.google.gwt.dom.client.ClientDomElement;
import com.google.gwt.dom.client.Element;
import com.google.gwt.user.client.Event;
import com.google.gwt.user.client.impl.DOMImpl;

import cc.alcina.framework.common.client.logic.reflection.Registration;

@Registration.Singleton(DOMImpl.class)
public class DOMImplHeadless extends DOMImpl {
	@Override
	public Element eventGetFromElement(Event evt) {
		return null;
	}

	@Override
	public Element eventGetToElement(Event evt) {
		return null;
	}

	@Override
	public Element getChild(Element elem, int index) {
		return null;
	}

	@Override
	public int getChildCount(Element elem) {
		return 0;
	}

	@Override
	public int getChildIndex(Element parent, Element child) {
		return 0;
	}

	@Override
	public void insertChild(Element parent, Element child, int index) {
	}

	@Override
	public void releaseCapture(Element elem) {
	}

	@Override
	public void setCapture(Element elem) {
	}

	@Override
	public void sinkBitlessEvent(Element elem, String eventTypeName) {
		ClientDomElement remote = elem.implAccess().remote();
		elem.implAccess().emitSinkBitlessEvent(eventTypeName);
	}

	@Override
	public void sinkEvents(Element elem, int eventBits) {
		ClientDomElement remote = elem.implAccess().remote();
		elem.implAccess().emitSinkEvents(eventBits);
	}

	@Override
	protected void initEventSystem() {
	}
}
