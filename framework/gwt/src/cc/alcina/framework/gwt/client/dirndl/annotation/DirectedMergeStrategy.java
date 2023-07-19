package cc.alcina.framework.gwt.client.dirndl.annotation;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

import com.google.common.base.Preconditions;

import cc.alcina.framework.common.client.logic.reflection.resolution.AbstractMergeStrategy;
import cc.alcina.framework.common.client.reflection.ClassReflector;
import cc.alcina.framework.common.client.reflection.HasAnnotations;
import cc.alcina.framework.common.client.reflection.Property;
import cc.alcina.framework.common.client.reflection.Reflections;
import cc.alcina.framework.common.client.util.Ax;
import cc.alcina.framework.gwt.client.dirndl.annotation.Directed.Impl;
import cc.alcina.framework.gwt.client.dirndl.layout.DirectedRenderer;

public class DirectedMergeStrategy extends AbstractMergeStrategy<Directed> {
	@Override
	public List<Directed> merge(List<Directed> lessSpecific,
			List<Directed> moreSpecific) {
		if (moreSpecific == null || moreSpecific.isEmpty()) {
			return lessSpecific;
		}
		if (lessSpecific == null || lessSpecific.isEmpty()) {
			return moreSpecific;
		}
		// require moreSpecific.length==1 || lessSpecific.length ==1
		//
		// if moreSpecific.length==1, merge moreSpecific[0] with
		// lessSpecific[0], then add remaining
		// lessSpecific
		//
		// if lessSpecific.length==1, add moreSpecific[0..last-1], merge
		// moreSpecific[last] with
		// lessSpecific[0]
		//
		// merge via Directed.Impl
		Directed mostSpecific = Ax.last(moreSpecific);
		if (!mostSpecific.merge()) {
			return moreSpecific;
		}
		Preconditions.checkArgument(
				lessSpecific.size() == 1 || moreSpecific.size() == 1);
		Directed.Impl lowestImpl = Directed.Impl.wrap(mostSpecific);
		List<Directed> result = new ArrayList<>();
		moreSpecific.stream().limit(moreSpecific.size() - 1)
				.forEach(result::add);
		Impl merged = lowestImpl.mergeParent(lessSpecific.get(0));
		result.add(merged);
		lessSpecific.stream().skip(1).forEach(result::add);
		return result;
	}

	@Override
	protected List<Directed> atClass(Class<Directed> annotationClass,
			ClassReflector<?> reflector, ClassReflector<?> resolvingReflector) {
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
			Directed.Impl impl = new Directed.Impl();
			/*
			 * if collection property, render the collection normally (the
			 * transform will be applied to the collection elements), otherwise
			 * use the transform renderer)
			 */
			boolean isCollection = reflector instanceof Property
					&& Reflections.isAssignableFrom(Collection.class,
							((Property) reflector).getType());
			if (isCollection) {
				impl.setRenderer(DirectedRenderer.ModelClass.class);
			} else {
				impl.setRenderer(DirectedRenderer.TransformRenderer.class);
			}
			result.add(impl);
		}
		if (result.isEmpty() && reflector instanceof Property) {
			Class declaringType = ((Property) reflector).getDeclaringType();
			if (Reflections.at(declaringType)
					.has(Directed.AllProperties.class)) {
				result.add(new Directed.Impl());
			}
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