package cc.alcina.framework.common.client.reflection;

import java.util.List;

public class TypeBounds {
	public final List<Class> bounds;

	public TypeBounds(List<Class> bounds) {
		this.bounds = bounds;
	}

	@Override
	public String toString() {
		return bounds.toString();
	}
}
