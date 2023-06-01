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
				result.pathref = Pathref.forNode(target.asElement());
			}
			return result;
		}
	}

	transient JavaScriptObject nativeTarget;

	transient Node pathrefTarget;

	Pathref pathref;

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
			return (T) LocalDom.nodeFor(nativeTarget);
		}
		ensurePathrefTarget();
		if (pathrefTarget instanceof ClientDomElement) {
			return (T) pathrefTarget;
		}
		throw new FixmeUnsupportedOperationException();
	}

	public boolean is(Class<? extends JavascriptObjectEquivalent> clazz) {
		if (clazz == Element.class && ElementJso.is(nativeTarget)) {
			return true;
		}
		ensurePathrefTarget();
		if (clazz == Element.class
				&& pathrefTarget instanceof ClientDomElement) {
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

	void ensurePathrefTarget() {
		if (pathrefTarget == null && pathref != null
				&& Document.get().remoteType == RemoteType.PATHREF) {
			pathrefTarget = pathref.node();
		}
	}
}
