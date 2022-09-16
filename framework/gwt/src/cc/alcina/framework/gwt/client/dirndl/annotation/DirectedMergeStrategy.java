package cc.alcina.framework.gwt.client.dirndl.annotation;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;

import com.google.common.base.Preconditions;

import cc.alcina.framework.common.client.logic.reflection.resolution.AbstractMergeStrategy;
import cc.alcina.framework.common.client.reflection.ClassReflector;
import cc.alcina.framework.common.client.reflection.HasAnnotations;
import cc.alcina.framework.common.client.reflection.Property;
import cc.alcina.framework.common.client.util.Ax;
import cc.alcina.framework.gwt.client.dirndl.annotation.Directed.Impl;
import cc.alcina.framework.gwt.client.dirndl.layout.DelegatingNodeRenderer;
import cc.alcina.framework.gwt.client.dirndl.layout.DirectedNodeRenderer;

public class DirectedMergeStrategy extends AbstractMergeStrategy<Directed> {
	@Override
	public List<Directed> merge(List<Directed> higher, List<Directed> lower) {
		// if lower.length >1, require higher.length max 1 (merge multiple up)
		// and only merge last
		// merge via Directed.Impl
		if (lower == null || lower.isEmpty()) {
			return higher;
		}
		if (higher == null || higher.isEmpty()) {
			return lower;
		}
		Preconditions.checkArgument(higher.size() == 1);
		Directed lowest = Ax.last(lower);
		Directed.Impl lowestImpl = Directed.Impl.wrap(lowest);
		List<Directed> result = lower.stream().limit(lower.size() - 1)
				.collect(Collectors.toList());
		Impl merged = lowestImpl.mergeParent(higher.get(0));
		result.add(merged);
		return result;
	}

	@Override
	protected List<Directed> atClass(Class<Directed> annotationClass,
			ClassReflector<?> reflector) {
		return atHasAnnotations(reflector);
	}

	protected List<Directed> atHasAnnotations(HasAnnotations reflector) {
		List<Directed> result = new ArrayList<>();
		Directed directed = reflector.annotation(Directed.class);
		Directed.Multiple multiple = reflector
				.annotation(Directed.Multiple.class);
		Directed.Wrap wrap = reflector.annotation(Directed.Wrap.class);
		Directed.Delegating delegating = reflector
				.annotation(Directed.Delegating.class);
		if (directed != null) {
			result.add(directed);
		}
		if (wrap != null) {
			result.add(new Directed.Impl().withTag(wrap.value()));
			result.add(new Directed.Impl());
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
		return atHasAnnotations(property);
	}

	public static class Delegating extends Directed.Impl {
		@Override
		public Class<? extends DirectedNodeRenderer> renderer() {
			return DelegatingNodeRenderer.class;
		}
	}
}