package cc.alcina.framework.entity.gwt.reflection;

import cc.alcina.framework.entity.gwt.reflection.ReachabilityData.AppReflectableTypes;
import cc.alcina.framework.entity.gwt.reflection.ReachabilityData.Type;

public abstract class ReachabilityLinkerPeer {
	public static class Default extends ReachabilityLinkerPeer {
		@Override
		public boolean permit(Type type) {
			return true;
		}
	}

	protected AppReflectableTypes reflectableTypes;
	public abstract boolean permit(Type type);
}
