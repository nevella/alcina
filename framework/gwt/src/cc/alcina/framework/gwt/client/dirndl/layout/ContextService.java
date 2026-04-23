package cc.alcina.framework.gwt.client.dirndl.layout;

import cc.alcina.framework.gwt.client.dirndl.event.ModelEvent;
import cc.alcina.framework.gwt.client.dirndl.event.NodeEvent;
import cc.alcina.framework.gwt.client.dirndl.model.Model;

/**
 * <p>
 * Half-baked (but promising) implementation of composable resolvers -
 * essentially a child can choose to only override/implement specific resolver
 * services (ContextService) and optionally delegate to a parent via
 * ancestorService
 * <p>
 * A while later...actually, this looks like being the third limb of inter-model
 * communication, as well as simplifying ContextResolver itself no end
 * <p>
 * Models can register services (if they have a defined ContextResolver - say by
 * annotating with DelegatingContextResolver) that descendant models can consume
 * - basically to provide cross-layer 'props' - aka immutable state.
 * <p>
 * A more general and sometimes better analaogy is an RPC service - the
 * (directed) descendants interact with each other and the ancestor via rpc-like
 * invocations of the ancestor service(s)
 * 
 * <p>
 * Services - if reachable - will always be registered with some node/model - so
 * the ContextService.Source can also be used to emit events
 */
public interface ContextService {
	/**
	 * <p>
	 * Marker interface, {@link Model} instances which register services can
	 * implement subtypes to describe their ContextService emission at
	 * compile-time.
	 * 
	 * <p>
	 * Originally, this was linked to runtime kit which performed service
	 * creation - but since the Model needs to retain a reference to the
	 * services anyway, manual registration is better (and simpler). It is not
	 * required, but is a useful indicator of service scope, particularly if
	 * different models register the same service interface (say
	 * SequenceArea.Service)
	 * 
	 * <p>
	 * Note - don't name subtypes "Provider" - rather "ServiceProvider"
	 */
	public interface Provider {
	}

	public interface Source {
		/**
		 * not 'getService' because it's _so_ common
		 * 
		 * @param <T>
		 * @param serviceType
		 * @return
		 */
		<T extends ContextService> T service(Class<T> serviceType);

		void emitEvent(Class<? extends ModelEvent> clazz);

		void emitEvent(Class<? extends ModelEvent> clazz, Object value);

		default void reemitEvent(NodeEvent<?> nodeEvent,
				Class<? extends ModelEvent> clazz) {
			reemitEvent(nodeEvent, clazz, null);
		}

		void reemitEvent(NodeEvent<?> nodeEvent,
				Class<? extends ModelEvent> clazz, Object value);

		<CS extends ContextService> void registerService(Class<CS> service,
				CS implementation);
	}
}