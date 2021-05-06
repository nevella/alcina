package cc.alcina.framework.gwt.client.dirndl.handler;

import cc.alcina.framework.common.client.logic.reflection.ClientInstantiable;
import cc.alcina.framework.gwt.client.dirndl.annotation.Behaviour;
import cc.alcina.framework.gwt.client.dirndl.annotation.Behaviour.TopicBehaviour;
import cc.alcina.framework.gwt.client.dirndl.behaviour.NodeEvent;
import cc.alcina.framework.gwt.client.dirndl.layout.TopicEvent;

@ClientInstantiable
public class EmitTopicHandler implements NodeEvent.Handler {
	@Override
	public void onEvent(NodeEvent.Context context) {
		TopicBehaviour behaviour = Behaviour.Util
				.getEmitTopicBehaviour(context.behaviour);
		TopicEvent.fire(context, behaviour.topic(),
				behaviour.payloadTransformer(), null, false);
	}
}