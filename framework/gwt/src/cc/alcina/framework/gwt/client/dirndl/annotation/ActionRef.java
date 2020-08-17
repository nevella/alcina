package cc.alcina.framework.gwt.client.dirndl.annotation;

import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

import com.google.gwt.event.shared.GwtEvent;

import cc.alcina.framework.common.client.logic.reflection.ClientInstantiable;
import cc.alcina.framework.common.client.logic.reflection.ClientVisible;
import cc.alcina.framework.common.client.logic.reflection.RegistryLocation;
import cc.alcina.framework.gwt.client.entity.place.ActionRefPlace;

@RegistryLocation(registryPoint = ActionRef.class)
public abstract class ActionRef extends Reference {
	public static Class<? extends ActionRef> forId(String token) {
		return Reference.forId(ActionRef.class, token);
	}

	@ClientInstantiable
	public static abstract class ActionHandler {
		public abstract void handleAction(GwtEvent event, ActionRefPlace place);
	}

	@ClientVisible
	@Retention(RetentionPolicy.RUNTIME)
	@Documented
	@Target({ ElementType.TYPE, ElementType.METHOD })
	public static @interface ActionRefHandler {
		Class<? extends ActionHandler> value();
	}
}