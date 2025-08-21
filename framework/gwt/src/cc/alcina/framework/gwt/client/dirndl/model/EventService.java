package cc.alcina.framework.gwt.client.dirndl.model;

import java.util.List;
import java.util.function.Consumer;

import cc.alcina.framework.common.client.logic.ListenerBinding;
import cc.alcina.framework.common.client.util.Multimap;
import cc.alcina.framework.gwt.client.dirndl.event.ModelEvent;
import cc.alcina.framework.gwt.client.dirndl.layout.ContextService;

/**
 * <p>
 * This service emits events (they must be ModelEvent.DescendantEvent subtypes)
 * to the containing DecoratedContentArea - so provides the main intra-component
 * communication channel between DecoratedContentArea components
 * 
 * <p>
 * Note though that although the *effect* of this service is intra-component
 * communication, it's explicitly not modelled as such - rather as a broadcast
 * service (like say UDP) with typed listeners
 * 
 * <p>
 * Note the special handling by DirectedLayout descendant event emission
 * (they're routed to the EventService as well, if it exists)
 */
public interface EventService extends ContextService {
	public interface Provider extends ContextService.Provider {
		EventService getEventService();
	}

	default void emitEvent(Class<? extends ModelEvent> clazz) {
		emitEvent(clazz, null);
	}

	void emitEvent(Class<? extends ModelEvent> clazz, Object value);

	<M extends ModelEvent> StreamBinding<M> on(Class<M> clazz);

	void publishEvent(ModelEvent event);

	static class ProviderInvoker
			implements ContextService.ProviderInvoker<EventService.Provider> {
		@Override
		public ContextService get(Provider provider) {
			return ((EventService.Provider) provider).getEventService();
		}

		@Override
		public Class<? extends ContextService> getServiceClass() {
			return EventService.class;
		}
	}

	public static abstract class Base implements EventService {
		class ServiceBinding<M extends ModelEvent>
				implements Consumer<M>, ListenerBinding {
			Class<M> clazz;

			StreamBinding<M> streamBinding;

			public ServiceBinding(Class<M> clazz) {
				this.clazz = clazz;
				/*
				 * Note that - since this stream binding only accepts events
				 * (not property changes) - we require that any events are
				 * *fired* on the dispatch thread, so there's no need to pass a
				 * dispatchref for stream event dispatch
				 */
				streamBinding = new StreamBinding<>(null);
			}

			@Override
			public void accept(ModelEvent t) {
				streamBinding.acceptStreamElement(t);
			}

			@Override
			public void bind() {
				// NOOP
			}

			@Override
			public void unbind() {
				streamBinding.unbind();
				bindings.remove(clazz, this);
			}
		}

		Multimap<Class<? extends ModelEvent>, List<ServiceBinding>> bindings = new Multimap<>();

		@Override
		public <M extends ModelEvent> StreamBinding<M> on(Class<M> clazz) {
			ServiceBinding serviceBinding = new ServiceBinding((Class) clazz);
			bindings.add(clazz, serviceBinding);
			return serviceBinding.streamBinding;
		}

		public void publishEvent(ModelEvent event) {
			List<ServiceBinding> classBindings = bindings.get(event.getClass());
			if (classBindings != null) {
				classBindings.forEach(binding -> binding.accept(event));
			}
		}
	}
}
