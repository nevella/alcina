package cc.alcina.framework.gwt.client.dirndl.annotation;

import java.util.ArrayList;
import java.util.List;

import com.google.common.base.Preconditions;

import cc.alcina.framework.common.client.logic.reflection.resolution.AbstractMergeStrategy;
import cc.alcina.framework.common.client.reflection.ClassReflector;
import cc.alcina.framework.common.client.reflection.HasAnnotations;
import cc.alcina.framework.common.client.reflection.Property;
import cc.alcina.framework.common.client.util.Ax;
import cc.alcina.framework.gwt.client.dirndl.annotation.Directed.Impl;
import cc.alcina.framework.gwt.client.dirndl.layout.DirectedRenderer;

public class DirectedMergeStrategy extends AbstractMergeStrategy<Directed> {
	/*
	 * as per implemented method, 'lower' is lower in the resolution stack,
	 * higher precedence
	 * 
	 * FIXME - dirndl 1x1e - reverse lower + higher (top of stack -- most
	 * specific frame - is 'higher', not lower)
	 */
	@Override
	public List<Directed> merge(List<Directed> higher, List<Directed> lower) {
		if (lower == null || lower.isEmpty()) {
			return higher;
		}
		if (higher == null || higher.isEmpty()) {
			return lower;
		}
		// require lower.length==1 || higher.length ==1
		//
		// if lower.length==1, merge lower[0] with higher[0], then add remaining
		// higher
		//
		// if higher.length==1, add lower[0..last-1], merge lower[last] with
		// higher[0]
		//
		// merge via Directed.Impl
		Directed lowest = Ax.last(lower);
		if (!lowest.merge()) {
			return lower;
		}
		Preconditions.checkArgument(higher.size() == 1 || lower.size() == 1);
		Directed.Impl lowestImpl = Directed.Impl.wrap(lowest);
		List<Directed> result = new ArrayList<>();
		lower.stream().limit(lower.size() - 1).forEach(result::add);
		Impl merged = lowestImpl.mergeParent(higher.get(0));
		result.add(merged);
		higher.stream().skip(1).forEach(result::add);
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
		Directed.Transform transform = reflector
				.annotation(Directed.Transform.class);
		if (directed != null) {
			Preconditions.checkState(
					wrap == null && multiple == null && delegating == null);
			result.add(directed);
		}
		if (wrap != null) {
			Preconditions.checkState(multiple == null && delegating == null);
			Directed.Impl impl = new Directed.Impl();
			impl.setTag(wrap.value());
			// Only Container is permitted (or logical) for wrapping
			impl.setRenderer(DirectedRenderer.Container.class);
			result.add(impl);
			result.add(new Directed.Impl());
		}
		if (multiple != null) {
			Preconditions.checkState(delegating == null);
			int length = multiple.value().length;
			for (int idx = 0; idx < length; idx++) {
				Directed element = multiple.value()[idx];
				if (idx < length - 1) {
					Preconditions.checkArgument(element
							.renderer() == DirectedRenderer.Container.class
							|| element
									.renderer() == DirectedRenderer.ModelClass.class);
					Directed.Impl impl = Directed.Impl.wrap(element);
					// Only Container is permitted (or logical) for wrapping
					impl.setRenderer(DirectedRenderer.Container.class);
					result.add(impl);
				} else {
					result.add(element);
				}
			}
		}
		if (delegating != null) {
			result.add(new Delegating());
		}
		if (transform != null && result.isEmpty()) {
			result.add(new Directed.Impl());
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
		public Class<? extends DirectedRenderer> renderer() {
			return DirectedRenderer.Delegating.class;
		}
	}
}