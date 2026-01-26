package cc.alcina.framework.servlet.component.romcom.client.common.logic;

import java.util.Iterator;
import java.util.List;
import java.util.Set;
import java.util.stream.Stream;

import com.google.gwt.dom.client.AttachId;
import com.google.gwt.dom.client.Document;
import com.google.gwt.dom.client.Element;
import com.google.gwt.dom.client.NodeAttachId;
import com.google.gwt.dom.client.WindowState;
import com.google.gwt.dom.client.WindowState.NodeUiState;
import com.google.gwt.dom.client.WindowState.OffsetsDelta.ElementOffsets;
import com.google.gwt.user.client.Window;

import cc.alcina.framework.common.client.meta.Feature;
import cc.alcina.framework.common.client.util.AlcinaCollections;
import cc.alcina.framework.servlet.component.romcom.Feature_Romcom_Impl;
import cc.alcina.framework.servlet.component.romcom.protocol.OffsetProtocol.OffsetRegistry;

@Feature.Ref(Feature_Romcom_Impl._WindowState.class)
class WindowStateGenerator {
	WindowState result = new WindowState();

	List<Element> offsetObservedElements;

	Set<Element> addedUiStates = AlcinaCollections.newUniqueSet();

	OffsetRegistry offsetRegistry;

	WindowStateGenerator(List<Element> offsetObservedElements,
			OffsetRegistry offsetRegistry) {
		this.offsetObservedElements = offsetObservedElements;
		this.offsetRegistry = offsetRegistry;
	}

	WindowState generate() {
		Document doc = Document.get();
		addElement(doc.getDocumentElement());
		addElement(doc.getActiveElement());
		Iterator<Element> itr = offsetObservedElements.iterator();
		while (itr.hasNext()) {
			Element elem = itr.next();
			if (!elem.isAttached()) {
				itr.remove();
			} else {
				addElement(elem);
			}
		}
		/*
		 * wip - localdom.offsetprotocol - should just be offsetobservedelements
		 */
		List<Element> withStateAttributes = doc.querySelectorAll(
				NodeAttachId.ATTR_NAME_TRANSMIT_STATE_SELECTOR);
		withStateAttributes.forEach(this::addElement);
		List<ElementOffsets> elementOffsets = result.nodeUiStates.stream()
				.map(nus -> nus.nodeId.node()).map(ElementOffsets::of).toList();
		result.offsetsDelta = offsetRegistry
				.computeOffsetsDelta(elementOffsets);
		result.clientHeight = Window.getClientHeight();
		result.clientWidth = Window.getClientWidth();
		/*
		 * for the protocol, this is optimised to result.offsetsDelta
		 */
		result.nodeUiStates.clear();
		return result;
	}

	void addElement(Element elem) {
		if (elem == null) {
			return;
		}
		if (!addedUiStates.add(elem)) {
			return;
		}
		NodeUiState uiState = new NodeUiState();
		uiState.nodeId = AttachId.forNode(elem);
		uiState.boundingClientRect = elem.getBoundingClientRect();
		uiState.scrollPos = elem.getScrollPosition();
		result.nodeUiStates.add(uiState);
		Element parentElement = elem.getParentElement();
		addElement(parentElement);
	}
}
