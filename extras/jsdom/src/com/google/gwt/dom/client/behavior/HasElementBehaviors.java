package com.google.gwt.dom.client.behavior;

import java.util.List;

import cc.alcina.framework.common.client.reflection.Property;

/**
 * A model interface - when bound, the behavior (classes) will be added to the
 * element
 */
public interface HasElementBehaviors {
	@Property.Not
	List<Class<? extends ElementBehavior>> getBehaviors();
}
