package cc.alcina.framework.gwt.client.dirndl.behaviour;

import cc.alcina.framework.common.client.logic.reflection.ClientInstantiable;

@ClientInstantiable
/*
 * FIXME - dirndl.1.a
 * 
 * see if everything can be expressed via topic bubbling and model changes
 */
public abstract class NodeTopic {
	public static class VoidTopic extends NodeTopic {
	}
}
