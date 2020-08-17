package cc.alcina.framework.gwt.client.dirndl.behaviour;

import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

import cc.alcina.framework.common.client.logic.reflection.ClientVisible;

public class NodeTopicHandler extends NodeEventHandler {
	@Override
	public void onEvent(NodeEvent.Context eventContext) {
		eventContext.node.publishTopic(
				eventContext.annotation(NodeTopicHandlerArgs.class).topic());
	}

	@ClientVisible
	@Retention(RetentionPolicy.RUNTIME)
	@Documented
	@Target({ ElementType.TYPE, ElementType.METHOD })
	public @interface NodeTopicHandlerArgs {
		Class<? extends NodeTopic> topic();
	}
}
