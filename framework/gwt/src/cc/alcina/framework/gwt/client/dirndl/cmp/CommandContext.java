package cc.alcina.framework.gwt.client.dirndl.cmp;

import java.util.Set;

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
}
