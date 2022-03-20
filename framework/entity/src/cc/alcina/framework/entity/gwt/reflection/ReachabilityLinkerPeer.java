package cc.alcina.framework.entity.gwt.reflection;

import java.util.Optional;

import cc.alcina.framework.entity.gwt.reflection.ReachabilityData.AppReflectableTypes;
import cc.alcina.framework.entity.gwt.reflection.ReachabilityData.Type;

public abstract class ReachabilityLinkerPeer {
	public static class Default extends ReachabilityLinkerPeer {
		@Override
		public boolean permit(Type type) {
			return true;
		}

		@Override
		public Optional<String> explain(Type type) {
			return Optional.empty();
		}
	}

	protected AppReflectableTypes reflectableTypes;
	public abstract boolean permit(Type type);
	
	public abstract Optional<String> explain(Type type);
}
