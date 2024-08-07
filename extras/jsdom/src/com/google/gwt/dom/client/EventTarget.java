/*
 * Copyright 2009 Google Inc.
 *
 * Licensed under the Apache License, Version 2.0 (the "License"); you may not
 * use this file except in compliance with the License. You may obtain a copy of
 * the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS, WITHOUT
 * WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. See the
 * License for the specific language governing permissions and limitations under
 * the License.
 */
package com.google.gwt.dom.client;

import com.google.common.base.Preconditions;
import com.google.gwt.core.client.JavaScriptObject;
import com.google.gwt.core.client.JavascriptObjectEquivalent;
import com.google.gwt.dom.client.Document.RemoteType;

import cc.alcina.framework.common.client.logic.reflection.reachability.Bean;
import cc.alcina.framework.common.client.logic.reflection.reachability.Bean.PropertySource;
import cc.alcina.framework.common.client.logic.reflection.reachability.Reflected;

/**
 * Represents the target of a JavaScript event.
 *
 * <p>
 * This type is returned from methods such as
 * {@link NativeEvent#getEventTarget()}, and must usually be cast to another
 * type using methods such as {@link Element#is(JavaScriptObject)} and
 * {@link Element#as(JavaScriptObject)}.
 * </p>
 *
 * <p>
 * This class intentionally does <em>not</em> specify the methods from the DOM
 * IDL (dispatchEvent, addEventListener, and removeEventListener).
 * </p>
 */
@Bean(PropertySource.FIELDS)
public class EventTarget implements JavascriptObjectEquivalent {
	public static EventTarget serializableForm(EventTarget target) {
		if (target == null) {
			return null;
		} else {
			EventTarget result = new EventTarget();
			if (target.isElement()) {
				result.attachId = AttachId.forNode(target.asElement());
				result.type = Type.element;
			} else {
				result.type = Type.valueOf(target.getNativeTargetType());
			}
			return result;
		}
	}

	final native String getNativeTargetType()/*-{
		var nativeTarget = this.@com.google.gwt.dom.client.EventTarget::nativeTarget;
		if(nativeTarget==$wnd){
		return 'window';
		}else if(nativeTarget==$doc){
		return 'document';
		}else{
		return 'other';
		}
		}-*/;

	transient JavaScriptObject nativeTarget;

	transient Node attachIdTarget;

	AttachId attachId;

	Type type;

	@Reflected
	public enum Type {
		element, window, document, other
	}

	public EventTarget() {
	}

	EventTarget(JavaScriptObject nativeTarget) {
		this.nativeTarget = nativeTarget;
	}

	public Element asElement() {
		Preconditions.checkState(isElement());
		return Element.as(this);
	}

	@Override
	public <T extends JavascriptObjectEquivalent> T cast() {
		if (ElementJso.is(nativeTarget)) {
			ElementJso remote = ElementJso.asRemote(nativeTarget);
			boolean connected = remote.isConnected();
			if (remote.getAttachId() == 0) {
				// return null;
				if (!connected) {
					// removed from local/browser dom - say a click, removed on
					// mousedown
					return null;
				} else {
					throw new IllegalStateException();
				}
			}
			return (T) LocalDom.nodeFor(nativeTarget);
		}
		ensureAttachIdTarget();
		if (attachIdTarget instanceof ClientDomElement) {
			return (T) attachIdTarget;
		}
		// Note that is() only confirme Element.class for the moment
		throw new IllegalStateException(
				"Should oonly be called after is() confirms the type");
	}

	void ensureAttachIdTarget() {
		if (attachIdTarget == null && attachId != null
				&& Document.get().remoteType == RemoteType.REF_ID) {
			attachIdTarget = attachId.node();
		}
	}

	public boolean is(Class<? extends JavascriptObjectEquivalent> clazz) {
		// this does *not* support checking if the clazz is Window.class - if
		// needed,
		// implement
		Preconditions.checkArgument(clazz == Element.class);
		if (clazz == Element.class && ElementJso.is(nativeTarget)) {
			return true;
		}
		ensureAttachIdTarget();
		if (clazz == Element.class
				&& attachIdTarget instanceof ClientDomElement) {
			return true;
		}
		return false;
	}

	public boolean isElement() {
		return is(Element.class);
	}

	@Override
	public String toString() {
		return super.toString() + ":" + nativeTarget;
	}

	/*
	 * If the (element) eventtarget was removed from the dom - say between
	 * mousedown + mouseup (remotedom)
	 */
	public boolean wasRemoved() {
		if (ElementJso.is(nativeTarget)) {
			return LocalDom.wasRemoved((ElementJso) nativeTarget);
		}
		return false;
	}
}
