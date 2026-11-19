package cc.alcina.framework.gwt.client.dirndl.event;

import cc.alcina.framework.common.client.reflection.Reflections;

/**
 * <p>
 * The event should be routed to descendant-or-self nodes, rather than
 * ancestors-or-self. The original source of the event may be the Emitter, or a
 * descendant of the Emitter (the event 'ascends' to the emitter and is handled
 * by 0,n descendants of the emitter - i.e. members of the node subtree rooted
 * in the emitter - hence 'reflected')
 * 
 * <p>
 * Note that the emitter of a reflected event can itself be a handler
 */
public abstract class ReflectedEvent<T, H extends NodeEvent.Handler, E extends ModelEvent.Reflector>
		extends ModelEvent<T, H> {
	public Class<E> getEmitterClass() {
		return Reflections.at(getClass()).getGenericBounds().bounds.get(2);
	}

	/*
	 * don't dispatch this on attach of subsequent listeners
	 */
	public interface NotStored {
	}
}