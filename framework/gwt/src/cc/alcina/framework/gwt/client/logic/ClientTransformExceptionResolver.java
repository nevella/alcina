package cc.alcina.framework.gwt.client.logic;

import java.util.HashSet;
import java.util.Set;

import cc.alcina.framework.common.client.logic.domaintransform.DomainTransformRequestException;
import cc.alcina.framework.common.client.logic.reflection.RegistryLocation;
import cc.alcina.framework.common.client.logic.reflection.RegistryLocation.ImplementationType;
import cc.alcina.framework.common.client.util.Callback;
/*
 * REVISIT: At the moment, there's a mismatch between local and remote 
 * in terms of exception resolution - i.e. if some transforms are skipped
 * for remote commit due to resolution, they're still stored locally.
 * 
 * This is actually a little tricky to decide - particularly without
 * local undo. Probably best leave for now (the transforms will be
 * marked as all_committed locally, so they won't be retried and shouldn't
 * cause inconsistency...I think). 
 */

public interface ClientTransformExceptionResolver {
	public void resolve(DomainTransformRequestException dtre,
			Callback<ClientTransformExceptionResolutionToken> callback);

	public enum ClientTransformExceptionResolverAction {
		THROW, RESUBMIT
	}

	public static class ClientTransformExceptionResolutionToken {
		private ClientTransformExceptionResolverAction resolverAction = ClientTransformExceptionResolverAction.THROW;

		private Set<Long> eventIdsToIgnore = new HashSet<Long>();

		private boolean reloadRequired = false;

		public ClientTransformExceptionResolverAction getResolverAction() {
			return this.resolverAction;
		}

		public void setResolverAction(
				ClientTransformExceptionResolverAction resolverAction) {
			this.resolverAction = resolverAction;
		}

		public Set<Long> getEventIdsToIgnore() {
			return this.eventIdsToIgnore;
		}

		public void setEventIdsToIgnore(Set<Long> eventIdsToIgnore) {
			this.eventIdsToIgnore = eventIdsToIgnore;
		}

		public void setReloadRequired(boolean reloadRequired) {
			this.reloadRequired = reloadRequired;
		}

		public boolean isReloadRequired() {
			return reloadRequired;
		}
	}

	public static class ClientTransformExceptionResolverThrow implements
			ClientTransformExceptionResolver {
		public void resolve(DomainTransformRequestException dtre,
				Callback<ClientTransformExceptionResolutionToken> callback) {
			callback.apply(new ClientTransformExceptionResolutionToken());
		}
	}
}
