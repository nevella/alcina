package cc.alcina.framework.gwt.client.dirndl.behaviour;

import cc.alcina.framework.common.client.logic.reflection.ClientInstantiable;

@ClientInstantiable
public abstract class NodeEventHandler {
	public abstract void onEvent(NodeEvent.Context eventContext);
}
