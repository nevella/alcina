package cc.alcina.framework.servlet.component.romcom.client.common.logic;

import java.util.List;
import java.util.Set;

import com.google.gwt.dom.client.AttachId;
import com.google.gwt.dom.client.Document;
import com.google.gwt.dom.client.DomEventContext;
import com.google.gwt.dom.client.DomEventContext.NodeUiState;
import com.google.gwt.dom.client.Element;
import com.google.gwt.dom.client.NodeAttachId;

import cc.alcina.framework.common.client.util.AlcinaCollections;

class DomEventContextGenerator {
	DomEventContext result = new DomEventContext();

	DomEventContext generate() {
		Document doc = Document.get();
		addRect(doc.getDocumentElement());
		Element activeElement = doc.getActiveElement();
		if (activeElement != null) {
			result.focussedElement = AttachId.forNode(activeElement);
			Element cursor = activeElement;
			while (cursor != doc.getDocumentElement()) {
				addRect(cursor);
				cursor = cursor.getParentElement();
			}
		}
		List<Element> withStateAttributes = doc.querySelectorAll(
				NodeAttachId.ATTR_NAME_TRANSMIT_STATE_SELECTOR);
		withStateAttributes.forEach(this::addRect);
		return result;
	}

	Set<Element> addedUiStates = AlcinaCollections.newUniqueSet();

	void addRect(Element elem) {
		if (!addedUiStates.add(elem)) {
			return;
		}
		NodeUiState uiState = new NodeUiState();
		uiState.element = AttachId.forNode(elem);
		uiState.boundingClientRect = elem.getBoundingClientRect();
		uiState.scrollPos = elem.getScrollPosition();
		result.nodeUiStates.add(uiState);
	}
}
