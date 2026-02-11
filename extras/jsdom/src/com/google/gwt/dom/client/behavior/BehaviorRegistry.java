package com.google.gwt.dom.client.behavior;

import java.util.List;
import java.util.Objects;
import java.util.Set;
import java.util.stream.Collectors;

import com.google.gwt.dom.client.Document;
import com.google.gwt.dom.client.Element;
import com.google.gwt.dom.client.EventTarget;
import com.google.gwt.dom.client.Selection;
import com.google.gwt.user.client.Event;
import com.google.gwt.user.client.Event.NativePreviewEvent;
import com.google.gwt.user.client.Event.NativePreviewHandler;

import cc.alcina.framework.common.client.dom.DomNode;
import cc.alcina.framework.common.client.dom.Location;
import cc.alcina.framework.common.client.logic.reflection.Registration;
import cc.alcina.framework.common.client.logic.reflection.registry.Registry;
import cc.alcina.framework.common.client.util.AlcinaCollections;
import cc.alcina.framework.common.client.util.Ax;

/**
 * Currently this is all or nothing (otherwise it's hard to avoid main-app-init
 * dependency on behaviors). It's probably better to manipulate the registry to
 * explicitly remove any handlers you don't want, rather than have imperative
 * custom initialization.
 */
@Registration.Singleton
public class BehaviorRegistry implements NativePreviewHandler {
	public static BehaviorRegistry get() {
		return Registry.impl(BehaviorRegistry.class);
	}

	List<ElementBehavior> handlers;

	Set<String> eventTypes = AlcinaCollections.newUniqueSet();

	boolean initialised = false;

	/**
	 * In a romcom context, the server does *not* need to populate the handlers
	 * (the browser client will)
	 * 
	 * @param registerHandlers
	 */
	public void init(boolean registerHandlers) {
		if (initialised) {
			return;
		}
		initialised = true;
		handlers = registerHandlers ? Registry.query(ElementBehavior.class)
				.implementations().filter(ElementBehavior::isEventHandler)
				.collect(Collectors.toList()) : List.of();
		handlers.stream().map(ElementBehavior::getEventType)
				.filter(Objects::nonNull).forEach(eventTypes::add);
		if (registerHandlers) {
			Event.addNativePreviewHandler(this);
		}
	}

	public static boolean isInitialised() {
		return get().handlers != null;
	}

	@Override
	public void onPreviewNativeEvent(NativePreviewEvent event) {
		if (event.isCanceled()) {
			return;
		}
		Event nativeEvent = (Event) event.getNativeEvent();
		String eventType = nativeEvent.getType();
		Ax.out(eventType);
		if (!eventTypes.contains(eventType)) {
			return;
		}
		EventTarget eventTarget = nativeEvent.getEventTarget();
		Element targetElement = null;
		if (eventTarget.isElement()) {
			targetElement = eventTarget.asElement();
		}
		switch (eventType) {
		case "selectionchange":
		case "keydown":
			Selection selection = Document.get().getSelection();
			Location focusLocation = selection.getFocusLocation();
			if (focusLocation != null) {
				DomNode node = focusLocation.getContainingNode();
				if (node.isElement()) {
					targetElement = node.gwtElement();
				} else if (node.isText()) {
					targetElement = node.parent().gwtElement();
				}
			}
			break;
		}
		if (targetElement == null) {
			return;
		}
		if (handlers.isEmpty()) {
			return;
		}
		Element cursor = targetElement;
		while (cursor != null) {
			if (cursor.hasAttributes() || cursor.getBehaviors() != null) {
				Element registeredElement = cursor;
				handlers.stream()
						.filter(h -> h.matches(registeredElement)
								&& h.getEventType().equals(eventType))
						.forEach(
								h -> h.onNativeEvent(event, registeredElement));
			}
			cursor = cursor.getParentElement();
		}
	}
}