package cc.alcina.framework.gwt.client.dirndl.layout;

import cc.alcina.framework.common.client.logic.reflection.Registration;
import cc.alcina.framework.common.client.logic.reflection.reachability.Reflected;

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
 */
public interface ContextService {
	/*
	 * Marker interface, subtypes will have explicit getSomeService methods,
	 * that will be invoked reflectivly via the corresponding ProviderInvoker
	 */
	/*
	 * It'd be nice for this to be generic, but given we want to have a
	 * coordinator model potentially interface several Provider subtypes, we
	 * can't. So both the provider intf and the provided service go on the
	 * ProviderInvoker
	 */
	public interface Provider {
	}

	@Registration.NonGenericSubtypes(ProviderInvoker.class)
	@Reflected
	public interface ProviderInvoker<P extends ContextService.Provider>
			extends Registration.AllSubtypesClient {
		ContextService get(P provider);

		Class<? extends ContextService> getServiceClass();
	}
}