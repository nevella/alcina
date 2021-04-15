package cc.alcina.framework.gwt.client.dirndl.annotation;

import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;
import java.util.Arrays;
import java.util.function.Function;

import cc.alcina.framework.common.client.logic.reflection.ClientVisible;
import cc.alcina.framework.common.client.util.IdentityFunction;
import cc.alcina.framework.gwt.client.dirndl.annotation.Behaviour.TopicBehaviour.TopicBehaviourType;
import cc.alcina.framework.gwt.client.dirndl.behaviour.NodeEvent;
import cc.alcina.framework.gwt.client.dirndl.behaviour.NodeTopic;
import cc.alcina.framework.gwt.client.dirndl.handler.EmitTopicHandler;

@Retention(RetentionPolicy.RUNTIME)
@Documented
// in fact, should only be an inner annotation for @Directed
@Target(ElementType.TYPE_USE)
@ClientVisible
public @interface Behaviour {
	Class<? extends NodeEvent> event();

	boolean fireOnce() default false;

	Class<? extends NodeEvent.Handler> handler() default NodeEvent.Handler.Self.class;

	TopicBehaviour[] topics() default {};

	@Retention(RetentionPolicy.RUNTIME)
	@Documented
	// inner annotation for @Behaviour; type annotation for @ActionRef
	@Target({ ElementType.TYPE_USE, ElementType.TYPE })
	@ClientVisible
	public @interface TopicBehaviour {
		Class<? extends Function> payloadTransformer() default IdentityFunction.class;

		Class<? extends NodeTopic> topic();

		TopicBehaviourType type();

		public static enum TopicBehaviourType {
			EMIT, RECEIVE, ACTIVATION;

			boolean isListenerTopic() {
				switch (this) {
				case RECEIVE:
				case ACTIVATION:
					return true;
				default:
					return false;
				}
			}
		}
	}

	public static class Util {
		public static Behaviour getEmitter(Directed directed,
				Class<? extends NodeTopic> topic) {
			if (directed.behaviours().length == 0) {
				return null;
			}
			return Arrays.stream(directed.behaviours())
					.filter(b -> b.handler() == EmitTopicHandler.class
							&& hasEmitTopicBehaviour(b, topic))
					.findFirst().orElse(null);
		}

		public static TopicBehaviour
				getEmitTopicBehaviour(Behaviour behaviour) {
			return Arrays.stream(behaviour.topics())
					.filter(tb -> tb.type() == TopicBehaviourType.EMIT)
					.findFirst().get();
		}

		public static boolean hasActivationTopic(Behaviour behaviour) {
			return behaviour.topics().length > 0
					&& Arrays.stream(behaviour.topics()).anyMatch(
							tb -> tb.type() == TopicBehaviourType.ACTIVATION);
		}

		public static boolean hasActivationTopic(Behaviour behaviour,
				Class<? extends NodeTopic> topic) {
			return behaviour.topics().length > 0
					&& Arrays.stream(behaviour.topics()).anyMatch(
							tb -> tb.type() == TopicBehaviourType.ACTIVATION
									&& tb.topic() == topic);
		}

		public static boolean hasEmitTopicBehaviour(Behaviour behaviour,
				Class<? extends NodeTopic> topic) {
			return behaviour.topics().length > 0
					&& Arrays.stream(behaviour.topics())
							.anyMatch(tb -> tb.type() == TopicBehaviourType.EMIT
									&& tb.topic() == topic);
		}

		public static boolean hasListenerTopic(Behaviour behaviour,
				Class topic) {
			return behaviour.topics().length > 0
					&& Arrays.stream(behaviour.topics()).anyMatch(
							tb -> tb.type() == TopicBehaviourType.RECEIVE
									&& tb.topic() == topic);
		}

		public static boolean isListenerTopic(TopicBehaviour topicBehaviour) {
			return topicBehaviour.type().isListenerTopic();
		}
	}
}
