package cc.alcina.framework.servlet.component.romcom.client.common.logic;

import java.util.Iterator;
import java.util.List;
import java.util.Set;

import com.google.gwt.dom.client.Document;
import com.google.gwt.dom.client.Element;
import com.google.gwt.dom.client.WindowState;
import com.google.gwt.user.client.Window;

import cc.alcina.framework.common.client.meta.Feature;
import cc.alcina.framework.common.client.util.AlcinaCollections;
import cc.alcina.framework.servlet.component.romcom.Feature_Romcom_Impl;
import cc.alcina.framework.servlet.component.romcom.protocol.OffsetProtocol.OffsetRegistry;

@Feature.Ref(Feature_Romcom_Impl._WindowState.class)
class WindowStateGenerator {
	WindowState result = new WindowState();

	List<Element> offsetObservedElements;

	Set<Element> observedElementTree = AlcinaCollections.newUniqueSet();

	OffsetRegistry offsetRegistry;

	WindowStateGenerator(List<Element> offsetObservedElements,
			OffsetRegistry offsetRegistry) {
		this.offsetObservedElements = offsetObservedElements;
		this.offsetRegistry = offsetRegistry;
	}

	WindowState generate() {
		Document doc = Document.get();
		addTreeElement(doc.getDocumentElement());
		addTreeElement(doc.getActiveElement());
		Iterator<Element> itr = offsetObservedElements.iterator();
		observedElementTree.removeIf(elem -> !elem.isAttached());
		while (itr.hasNext()) {
			Element elem = itr.next();
			if (!elem.isAttached()) {
				itr.remove();
			} else {
				addTreeElement(elem);
			}
		}
		result.offsetsDelta = offsetRegistry
				.computeOffsetsDelta(observedElementTree);
		result.clientHeight = Window.getClientHeight();
		result.clientWidth = Window.getClientWidth();
		result.title = Window.getTitle();
		return result;
	}

	void addTreeElement(Element elem) {
		if (elem == null) {
			return;
		}
		if (!observedElementTree.add(elem)) {
			return;
		}
		Element parentElement = elem.getParentElement();
		addTreeElement(parentElement);
	}
}
