package cc.alcina.framework.entity.gwt.headless;

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
	protected void initEventSystem() {
	}

	@Override
	public void insertChild(Element parent, Element child, int index) {
	}

	@Override
	public void releaseCapture(Element elem) {
		throw new UnsupportedOperationException();
	}

	@Override
	public void setCapture(Element elem) {
		throw new UnsupportedOperationException(
				"Requires js client (performance)");
	}

	@Override
	public void sinkBitlessEvent(Element elem, String eventTypeName) {
		elem.implAccess().emitSinkBitlessEvent(eventTypeName);
	}

	@Override
	public void sinkEvents(Element elem, int eventBits) {
		elem.implAccess().emitSinkEvents(eventBits);
	}
}
