package com.google.gwt.dom.client.behavior;

import java.util.LinkedHashSet;
import java.util.List;
import java.util.Objects;
import java.util.Set;
import java.util.stream.Collectors;

import com.google.common.base.Preconditions;
import com.google.gwt.dom.client.Document;
import com.google.gwt.dom.client.Element;
import com.google.gwt.dom.client.EventTarget;
import com.google.gwt.dom.client.NodeAttachId;
import com.google.gwt.dom.client.Selection;
import com.google.gwt.user.client.Event;
import com.google.gwt.user.client.Event.NativePreviewEvent;
import com.google.gwt.user.client.Event.NativePreviewHandler;

import cc.alcina.framework.common.client.dom.DomNode;
import cc.alcina.framework.common.client.dom.Location;
import cc.alcina.framework.common.client.logic.reflection.Registration;
import cc.alcina.framework.common.client.logic.reflection.registry.Registry;
import cc.alcina.framework.common.client.util.AlcinaCollections;

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

	/*
	 * Used to test uniqueness of magic names
	 */
	Set<String> magicAttributeNames;

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
		magicAttributeNames = new LinkedHashSet<>();
		addMagicName(NodeAttachId.ATTR_NAME_TRANSMIT_STATE);
		handlers.stream().map(ElementBehavior::getMagicAttributeName)
				.filter(Objects::nonNull).forEach(this::addMagicName);
		handlers.stream().map(ElementBehavior::getEventType)
				.filter(Objects::nonNull).forEach(eventTypes::add);
		if (registerHandlers) {
			Event.addNativePreviewHandler(this);
		}
	}

	void addMagicName(String name) {
		Preconditions.checkArgument(magicAttributeNames.add(name));
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