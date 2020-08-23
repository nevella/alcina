package cc.alcina.framework.gwt.client.dirndl.annotation;

import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

import cc.alcina.framework.common.client.logic.reflection.ClientVisible;
import cc.alcina.framework.gwt.client.dirndl.behaviour.NodeEvent;
import cc.alcina.framework.gwt.client.dirndl.behaviour.NodeTopic;
import cc.alcina.framework.gwt.client.dirndl.behaviour.NodeTopic.VoidTopic;

@Retention(RetentionPolicy.RUNTIME)
@Documented
// in fact, should only be an inner annotation for @Directed
@Target(ElementType.TYPE_USE)
@ClientVisible
public @interface Behaviour {
	Class<? extends NodeEvent.Handler> handler();

	Class<? extends NodeEvent> event();

	String eventSourcePath() default ".";

	String handlerTargetPath() default ".";

	Class<? extends NodeTopic> activationTopic() default VoidTopic.class;

	boolean fireOnce() default false;
}
