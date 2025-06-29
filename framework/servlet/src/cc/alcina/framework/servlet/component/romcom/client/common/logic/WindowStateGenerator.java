package cc.alcina.framework.servlet.component.romcom.client.common.logic;

import java.util.Iterator;
import java.util.List;
import java.util.Set;

import com.google.gwt.dom.client.AttachId;
import com.google.gwt.dom.client.Document;
import com.google.gwt.dom.client.Element;
import com.google.gwt.dom.client.NodeAttachId;
import com.google.gwt.dom.client.WindowState;
import com.google.gwt.dom.client.WindowState.NodeUiState;
import com.google.gwt.user.client.Window;

import cc.alcina.framework.common.client.meta.Feature;
import cc.alcina.framework.common.client.util.AlcinaCollections;
import cc.alcina.framework.servlet.component.romcom.Feature_Romcom_Impl;

@Feature.Ref(Feature_Romcom_Impl._WindowState.class)
class WindowStateGenerator {
	WindowState result = new WindowState();

	List<Element> offsetObservedElements;

	Set<Element> addedUiStates = AlcinaCollections.newUniqueSet();

	WindowStateGenerator(List<Element> offsetObservedElements) {
		this.offsetObservedElements = offsetObservedElements;
	}

	WindowState generate() {
		Document doc = Document.get();
		addRect(doc.getDocumentElement());
		Element activeElement = doc.getActiveElement();
		if (activeElement != null) {
			result.activeElement = AttachId.forNode(activeElement);
			Element cursor = activeElement;
			while (cursor != doc.getDocumentElement()) {
				addRect(cursor);
				cursor = cursor.getParentElement();
			}
		}
		Iterator<Element> itr = offsetObservedElements.iterator();
		while (itr.hasNext()) {
			Element elem = itr.next();
			if (!elem.isAttached()) {
				itr.remove();
			} else {
				addRect(elem);
			}
		}
		List<Element> withStateAttributes = doc.querySelectorAll(
				NodeAttachId.ATTR_NAME_TRANSMIT_STATE_SELECTOR);
		withStateAttributes.forEach(this::addRect);
		result.clientHeight = Window.getClientHeight();
		result.clientWidth = Window.getClientWidth();
		result.scrollTop = Window.getScrollTop();
		result.scrollLeft = Window.getScrollLeft();
		return result;
	}

	void addRect(Element elem) {
		if (!addedUiStates.add(elem)) {
			return;
		}
		NodeUiState uiState = new NodeUiState();
		uiState.element = AttachId.forNode(elem);
		uiState.boundingClientRect = elem.getBoundingClientRect();
		uiState.scrollPos = elem.getScrollPosition();
		result.nodeUiStates.add(uiState);
		if (uiState.boundingClientRect.isZeroDimensions()) {
			/*
			 * A pattern - if using an empty element for positioning, it will
			 * have zero dimensions - so will use its parent for positioning
			 */
			addRect(elem.getParentElement());
		}
	}
}
