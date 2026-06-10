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

/**
 * Currently this is all or nothing (otherwise it's hard to avoid main-app-init
 * dependency on behaviors). It's probably better to manipulate the registry to
 * explicitly remove any handlers you don't want, rather than have imperative
 * custom initialization.
 */
@Registration.Singleton
public class BehaviorRegistry
		implements NativePreviewHandler, ElementBehavior.RegisterAllEvents {
	static class ElementRegistration {
		Element elem;

		ElementBehavior behavior;

		ElementRegistration(Element elem, ElementBehavior behavior) {
			this.elem = elem;
			this.behavior = behavior;
		}

		@Override
		public boolean equals(Object obj) {
			if (obj instanceof ElementRegistration) {
				ElementRegistration o = (ElementRegistration) obj;
				return elem == o.elem && behavior == o.behavior;
			}
			return super.equals(obj);
		}

		@Override
		public int hashCode() {
			return Objects.hash(elem, behavior);
		}
	}

	public static BehaviorRegistry get() {
		return Registry.impl(BehaviorRegistry.class);
	}

	public static boolean isInitialised() {
		return get().initialised;
	}

	Set<String> eventTypes = AlcinaCollections.newUniqueSet();

	boolean initialised = false;

	Set<ElementRegistration> allEventRegistrations = AlcinaCollections
			.newUniqueSet();

	/**
	 * In a romcom context, the server does *not* need to populate the
	 * eventTypes (the browser client will)
	 * 
	 * @param registerHandlers
	 */
	public void init(boolean registerHandlers) {
		if (initialised) {
			return;
		}
		initialised = true;
		if (registerHandlers) {
			Event.addNativePreviewHandler(this);
		}
		/*
		 * compute the set of all event types we need to preview (hopefully not
		 * too many...)
		 */
		List<ElementBehavior> behaviors = registerHandlers
				? Registry.query(ElementBehavior.class).implementations()
						.filter(ElementBehavior::isEventHandler).collect(
								Collectors.toList())
				: List.of();
		behaviors.stream().map(ElementBehavior::getEventType)
				.filter(Objects::nonNull).forEach(eventTypes::add);
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
		Element cursor = targetElement;
		while (cursor != null) {
			if (cursor.getBehaviors() != null) {
				Element registeredElement = cursor;
				cursor.getBehaviors().forEach(bh -> {
					if (Objects.equals(bh.getEventType(), eventType)) {
						bh.onNativeEvent(event, registeredElement, this);
					}
				});
			}
			cursor = cursor.getParentElement();
		}
		allEventRegistrations.stream()
				.filter(registration -> registration.behavior.getEventType()
						.equals(eventType))
				.toList().forEach(registration -> registration.behavior
						.onNativeEvent(event, registration.elem, this));
	}

	@Override
	public void registerAllEvents(Element elem, ElementBehavior behavior,
			boolean register) {
		ElementRegistration registration = new ElementRegistration(elem,
				behavior);
		if (register) {
			allEventRegistrations.add(registration);
		} else {
			allEventRegistrations.remove(registration);
		}
	}
}