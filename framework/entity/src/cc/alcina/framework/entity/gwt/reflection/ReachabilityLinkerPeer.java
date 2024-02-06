package cc.alcina.framework.entity.gwt.reflection;

import java.util.Optional;

import cc.alcina.framework.entity.gwt.reflection.ReachabilityData.AppReflectableTypes;
import cc.alcina.framework.entity.gwt.reflection.ReachabilityData.Type;

public abstract class ReachabilityLinkerPeer {
	protected AppReflectableTypes reflectableTypes;

	public abstract Optional<String> explain(Type type);

	protected boolean hasExplicitTypePermission(Type type) {
		return false;
	}

	protected abstract void init(AppReflectableTypes reflectableTypes2);

	public abstract boolean permit(Type type);

	public static class Default extends ReachabilityLinkerPeer {
		@Override
		public Optional<String> explain(Type type) {
			return Optional.empty();
		}

		@Override
		protected void init(AppReflectableTypes reflectableTypes2) {
		}

		@Override
		public boolean permit(Type type) {
			return true;
		}
	}
}
