package cc.alcina.framework.entity.gwt.headless;

import com.google.gwt.dom.client.Element;
import com.google.gwt.user.client.Event;
import com.google.gwt.user.client.impl.DOMImpl;

import cc.alcina.framework.common.client.logic.reflection.Registration;
import cc.alcina.framework.common.client.logic.reflection.RegistryLocation;
import cc.alcina.framework.common.client.logic.reflection.RegistryLocation.ImplementationType;

@RegistryLocation(registryPoint = DOMImpl.class, implementationType = ImplementationType.SINGLETON)
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
	}

	@Override
	public void sinkEvents(Element elem, int eventBits) {
	}

	@Override
	protected void initEventSystem() {
	}
}
