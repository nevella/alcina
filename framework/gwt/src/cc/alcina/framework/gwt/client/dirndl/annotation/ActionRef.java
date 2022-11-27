package cc.alcina.framework.gwt.client.dirndl.annotation;

import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

import com.google.gwt.event.shared.GwtEvent;

import cc.alcina.framework.common.client.logic.reflection.Registration;
import cc.alcina.framework.common.client.logic.reflection.reachability.ClientVisible;
import cc.alcina.framework.common.client.logic.reflection.reachability.Reflected;
import cc.alcina.framework.gwt.client.dirndl.layout.DirectedLayout.Node;
import cc.alcina.framework.gwt.client.entity.place.ActionRefPlace;

@Registration(ActionRef.class)
/**
 * FIXME - dirndl 1x1d - remove (and
 * cc.alcina.framework.gwt.client.dirndl.annotation.Reference), just use
 * ModelEvent, and Link.withDefaultEventHandler() - which is looked up by the
 * root node
 *
 * Also optionally add a handler to the link
 *
 * @author nick@alcina.cc
 *
 */
public abstract class ActionRef extends Reference {
	public static Class<? extends ActionRef> forId(String token) {
		return Reference.forId(ActionRef.class, token);
	}

	@Reflected
	public static abstract class ActionHandler {
		public abstract void handleAction(Node node, GwtEvent event,
				ActionRefPlace place);
	}

	@ClientVisible
	@Retention(RetentionPolicy.RUNTIME)
	@Documented
	@Target({ ElementType.TYPE, ElementType.METHOD })
	public static @interface ActionRefHandler {
		Class<? extends ActionHandler> value();
	}
}
