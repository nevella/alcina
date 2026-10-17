package cc.alcina.framework.gwt.client.dirndl.cmp.command;

import java.util.Collection;
import java.util.LinkedHashSet;
import java.util.Set;

import cc.alcina.framework.gwt.client.dirndl.event.ModelEvents;
import cc.alcina.framework.gwt.client.dirndl.event.ModelEvents.CommandContextRegistration;
import cc.alcina.framework.gwt.client.dirndl.event.ModelEvents.CommandContextRegistration.Data;

public interface CommandContext {
	public interface Provider {
		/*
		 * FIXME - registry - once there are per-environment (context)
		 * registries, reinstate
		 */
		// public static Provider get() {
		// return Registry.impl(Provider.class);
		// }
		Set<Class<? extends CommandContext>> getContexts();

		public static class Default implements Provider {
			@Override
			public Set<Class<? extends CommandContext>> getContexts() {
				return Set.of();
			}
		}
	}

	public static class ContextChangeRegistry
			implements ModelEvents.CommandContextRegistration.Handler {
		Set<Class<? extends CommandContext>> contexts = new LinkedHashSet<>();

		@Override
		public void
				onCommandContextRegistration(CommandContextRegistration event) {
			Data model = event.getModel();
			if (model.attached) {
				contexts.addAll(model.contexts);
			} else {
				contexts.removeAll(model.contexts);
			}
		}

		public Collection<? extends Class<? extends CommandContext>>
				getContexts() {
			return contexts;
		}
	}
}
