package cc.alcina.framework.gwt.client.dirndl.annotation;

import java.util.ArrayList;
import java.util.List;

import com.google.common.base.Preconditions;

import cc.alcina.framework.common.client.logic.reflection.resolution.AbstractMergeStrategy;
import cc.alcina.framework.common.client.logic.reflection.resolution.AnnotationLocation.Resolver;
import cc.alcina.framework.common.client.reflection.ClassReflector;
import cc.alcina.framework.common.client.reflection.HasAnnotations;
import cc.alcina.framework.common.client.reflection.Property;
import cc.alcina.framework.common.client.reflection.Reflections;
import cc.alcina.framework.common.client.util.Ax;
import cc.alcina.framework.gwt.client.dirndl.annotation.Directed.AllProperties;
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
			ClassReflector<?> reflector, ClassReflector<?> resolvingReflector,
			Resolver resolver) {
		return atHasAnnotations(reflector, resolver);
	}

	protected List<Directed> atHasAnnotations(HasAnnotations reflector,
			Resolver resolver) {
		List<Directed> result = new ArrayList<>();
		Directed directed = resolver.contextAnnotation(reflector,
				Directed.class, Resolver.ResolutionContext.Strategy);
		Directed.Multiple multiple = resolver.contextAnnotation(reflector,
				Directed.Multiple.class, Resolver.ResolutionContext.Strategy);
		Directed.Wrap wrap = resolver.contextAnnotation(reflector,
				Directed.Wrap.class, Resolver.ResolutionContext.Strategy);
		Directed.Delegating delegating = resolver.contextAnnotation(reflector,
				Directed.Delegating.class, Resolver.ResolutionContext.Strategy);
		Directed.Transform transform = resolver.contextAnnotation(reflector,
				Directed.Transform.class, Resolver.ResolutionContext.Strategy);
		Directed.TransformElements transformElements = resolver
				.contextAnnotation(reflector, Directed.TransformElements.class,
						Resolver.ResolutionContext.Strategy);
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
		if (transform != null) {
			if (result.isEmpty()) {
				Directed.Impl impl = new Directed.Impl();
				impl.setRenderer(DirectedRenderer.TransformRenderer.class);
				result.add(impl);
			}
		}
		if (transformElements != null) {
			if (result.isEmpty()) {
				Directed.Impl impl = new Directed.Impl();
				result.add(impl);
			}
		}
		if (result.isEmpty() && reflector instanceof Property) {
			Class declaringType = ((Property) reflector).getDeclaringType();
			ClassReflector typeReflector = Reflections.at(declaringType);
			AllProperties allProperties = resolver.contextAnnotation(
					typeReflector, Directed.AllProperties.class,
					Resolver.ResolutionContext.Strategy);
			if (allProperties != null) {
				Directed.Exclude exclude = resolver.contextAnnotation(reflector,
						Directed.Exclude.class,
						Resolver.ResolutionContext.Strategy);
				if (exclude == null) {
					result.add(new Directed.Impl());
				}
			}
		}
		return result;
	}

	@Override
	protected List<Directed> atProperty(Class<Directed> annotationClass,
			Property property, Resolver resolver) {
		return atHasAnnotations(property, resolver);
	}

	public static class Delegating extends Directed.Impl {
		@Override
		public Class<? extends DirectedRenderer> renderer() {
			return DirectedRenderer.Delegating.class;
		}
	}
}