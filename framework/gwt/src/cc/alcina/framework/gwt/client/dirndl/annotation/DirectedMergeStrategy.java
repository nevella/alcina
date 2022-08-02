package cc.alcina.framework.gwt.client.dirndl.annotation;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import cc.alcina.framework.common.client.logic.reflection.resolution.AbstractMergeStrategy;
import cc.alcina.framework.common.client.reflection.ClassReflector;
import cc.alcina.framework.common.client.reflection.Property;
import cc.alcina.framework.gwt.client.dirndl.layout.DelegatingNodeRenderer;
import cc.alcina.framework.gwt.client.dirndl.layout.DirectedNodeRenderer;

public class DirectedMergeStrategy extends AbstractMergeStrategy<Directed> {
	@Override
	public List<Directed> merge(List<Directed> higher, List<Directed> lower) {
		// if lower.length >1, require higher.length max 1 (merge multiple up)
		// and only merge last
		// merge via Directed.Default
		throw new UnsupportedOperationException();
		// return Shared.merge(higher, lower,
		// (t1, t2) -> Reflections.isAssignableFrom(t1, t2));
	}

	@Override
	protected List<Directed> atClass(Class<Directed> annotationClass,
			ClassReflector<?> reflector) {
		List<Directed> result = new ArrayList<>();
		Directed directed = reflector.annotation(Directed.class);
		Directed.Multiple multiple = reflector
				.annotation(Directed.Multiple.class);
		Directed.Delegating delegating = reflector
				.annotation(Directed.Delegating.class);
		if (directed != null) {
			result.add(directed);
		}
		if (multiple != null) {
			Arrays.stream(multiple.value()).forEach(result::add);
		}
		if (delegating != null) {
			result.add(new Delegating());
		}
		return result;
	}

	@Override
	protected List<Directed> atProperty(Class<Directed> annotationClass,
			Property property) {
		// TODO Auto-generated method stub
		return null;
	}

	public static class Delegating extends Directed.Default {
		@Override
		public Class<? extends DirectedNodeRenderer> renderer() {
			return DelegatingNodeRenderer.class;
		}
	}
}